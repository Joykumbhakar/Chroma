package com.chroma.studio.utils

import android.content.Context
import android.os.Build
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Ultra-aggressive image compressor designed for Supabase Free Tier storage preservation.
 * Avatars are scaled to max 320x320 and compressed to < 25KB.
 * Banners are scaled to max 1080x450 and compressed to < 45KB.
 */
object ImageUtils {

    private const val MAX_AVATAR_DIMENSION = 320
    private const val MAX_AVATAR_BYTES = 25_000 // 25 KB target

    private const val MAX_BANNER_WIDTH = 1080
    private const val MAX_BANNER_HEIGHT = 450
    private const val MAX_BANNER_BYTES = 45_000 // 45 KB target

    /**
     * Aggressively compress a profile avatar from a Uri to under 25KB before uploading to Supabase,
     * applying EXIF orientation correction.
     */
    fun compressAvatarForSupabase(context: Context, uri: Uri): ByteArray? {
        return try {
            val rotation = getOrientationDegrees(context, uri)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null

            val rotated = rotateBitmap(original, rotation)
            val scaled = scaleBitmap(rotated, MAX_AVATAR_DIMENSION, MAX_AVATAR_DIMENSION, squareCrop = true)
            compressBitmapUnderLimit(scaled, MAX_AVATAR_BYTES, initialQuality = 80)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress avatar from uri", e)
            null
        }
    }

    /**
     * Aggressively compress a profile avatar from raw bytes to under 25KB.
     */
    fun compressAvatarBytesForSupabase(imageBytes: ByteArray): ByteArray? {
        return try {
            val inputStream = ByteArrayInputStream(imageBytes)
            compressAvatarStream(inputStream)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress avatar bytes", e)
            null
        }
    }

    /**
     * Aggressively compress a profile avatar Bitmap to under 25KB.
     */
    fun compressAvatarBitmapForSupabase(bitmap: Bitmap): ByteArray? {
        return try {
            val scaled = scaleBitmap(bitmap, MAX_AVATAR_DIMENSION, MAX_AVATAR_DIMENSION, squareCrop = true)
            compressBitmapUnderLimit(scaled, MAX_AVATAR_BYTES, initialQuality = 80)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress avatar bitmap", e)
            null
        }
    }

    /**
     * Aggressively compress a profile banner from a Uri to under 45KB before uploading to Supabase,
     * preserving landscape aspect ratio and applying EXIF orientation correction.
     */
    fun compressBannerForSupabase(context: Context, uri: Uri): ByteArray? {
        return try {
            val rotation = getOrientationDegrees(context, uri)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null

            val rotated = rotateBitmap(original, rotation)
            val scaled = scaleBitmap(rotated, MAX_BANNER_WIDTH, MAX_BANNER_HEIGHT, squareCrop = false)
            compressBitmapUnderLimit(scaled, MAX_BANNER_BYTES, initialQuality = 82)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress banner from uri", e)
            null
        }
    }

    /**
     * Aggressively compress a profile banner from raw bytes to under 45KB without cropping to square.
     */
    fun compressBannerBytesForSupabase(imageBytes: ByteArray): ByteArray? {
        return try {
            val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null
            val scaled = scaleBitmap(original, MAX_BANNER_WIDTH, MAX_BANNER_HEIGHT, squareCrop = false)
            compressBitmapUnderLimit(scaled, MAX_BANNER_BYTES, initialQuality = 82)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress banner bytes", e)
            null
        }
    }

    /**
     * Aggressively compress a profile banner Bitmap to under 45KB without cropping to square.
     */
    fun compressBannerBitmapForSupabase(bitmap: Bitmap): ByteArray? {
        return try {
            val scaled = scaleBitmap(bitmap, MAX_BANNER_WIDTH, MAX_BANNER_HEIGHT, squareCrop = false)
            compressBitmapUnderLimit(scaled, MAX_BANNER_BYTES, initialQuality = 82)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress banner bitmap", e)
            null
        }
    }

    private const val MAX_CHAT_IMAGE_DIMENSION = 1280
    private const val MAX_CHAT_IMAGE_BYTES = 180_000 // 180 KB target for high quality + low storage

    // ── Community Post Image (high-quality, lossless-looking) ──────────────
    private const val MAX_POST_IMAGE_DIMENSION = 1440   // 1440px long-edge — retina detail preserved
    private const val MAX_POST_IMAGE_BYTES = 600_000    // 600 KB ceiling — Cloudinary free-tier safe

    /**
     * Efficiently compresses a chat or post image from a Uri with orientation correction.
     */
    fun compressChatImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_CHAT_IMAGE_DIMENSION,
        maxBytes: Int = MAX_CHAT_IMAGE_BYTES
    ): ByteArray? {
        return try {
            val rotation = getOrientationDegrees(context, uri)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null

            val rotated = rotateBitmap(original, rotation)
            val scaled = scaleBitmap(rotated, maxDimension, maxDimension, squareCrop = false)
            compressBitmapUnderLimit(scaled, maxBytes, initialQuality = 85)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress chat image from uri", e)
            null
        }
    }

    /**
     * Efficiently compresses a chat image from raw bytes.
     */
    fun compressChatImageBytes(
        imageBytes: ByteArray,
        maxDimension: Int = MAX_CHAT_IMAGE_DIMENSION,
        maxBytes: Int = MAX_CHAT_IMAGE_BYTES
    ): ByteArray? {
        return try {
            val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null
            val scaled = scaleBitmap(original, maxDimension, maxDimension, squareCrop = false)
            compressBitmapUnderLimit(scaled, maxBytes, initialQuality = 85)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress chat image bytes", e)
            null
        }
    }

    /**
     * Efficiently compresses a chat image Bitmap.
     */
    fun compressChatImageBitmap(
        bitmap: Bitmap,
        maxDimension: Int = MAX_CHAT_IMAGE_DIMENSION,
        maxBytes: Int = MAX_CHAT_IMAGE_BYTES
    ): ByteArray? {
        return try {
            val scaled = scaleBitmap(bitmap, maxDimension, maxDimension, squareCrop = false)
            compressBitmapUnderLimit(scaled, maxBytes, initialQuality = 85)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress chat image bitmap", e)
            null
        }
    }


    /**
     * Smart high-quality community post image compressor.
     *
     * Strategy:
     *  1. Scale to 1440px long-edge maximum (preserves full detail on retina screens).
     *  2. Try WebP lossy at 88% quality first (Android 11+ native WebP encoder).
     *     WebP saves 25-35% over JPEG at the same visual quality.
     *  3. If WebP still exceeds the byte ceiling, progressively step down quality by 5%.
     *  4. Fall back to JPEG on older devices (pre-API 30) with the same step-down logic.
     *  5. Never drops below 60% JPEG / 55% WebP quality — image always looks crisp.
     *
     * Typical result: 3–8 MB camera JPEG → 250–500 KB without visible quality loss.
     */
    fun compressPostImage(context: Context, uri: Uri): ByteArray? {
        return try {
            val rotation = getOrientationDegrees(context, uri)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null
            val rotated = rotateBitmap(original, rotation)
            val scaled = scaleBitmap(rotated, MAX_POST_IMAGE_DIMENSION, MAX_POST_IMAGE_DIMENSION, squareCrop = false)
            compressPostBitmapUnderLimit(scaled)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress post image from uri", e)
            null
        }
    }

    /** Compress a post image directly from a [Bitmap] (e.g. from canvas filter rendering). */
    fun compressPostBitmap(bitmap: Bitmap): ByteArray? {
        return try {
            val scaled = scaleBitmap(bitmap, MAX_POST_IMAGE_DIMENSION, MAX_POST_IMAGE_DIMENSION, squareCrop = false)
            compressPostBitmapUnderLimit(scaled)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress post bitmap", e)
            null
        }
    }

    /** Compress a post image directly from raw bytes (e.g. from gallery picker). */
    fun compressPostImageBytes(imageBytes: ByteArray): ByteArray? {
        return try {
            val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null
            val scaled = scaleBitmap(original, MAX_POST_IMAGE_DIMENSION, MAX_POST_IMAGE_DIMENSION, squareCrop = false)
            compressPostBitmapUnderLimit(scaled)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress post image bytes", e)
            null
        }
    }

    private fun compressPostBitmapUnderLimit(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()

        // Android 11+ (API 30): use lossless-looking WebP lossy — 25-35% smaller than JPEG at same quality
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var quality = 88
            do {
                stream.reset()
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, stream)
                quality -= 5
            } while (stream.size() > MAX_POST_IMAGE_BYTES && quality >= 55)
            return stream.toByteArray()
        }

        // Fallback: JPEG for older devices
        var quality = 92
        do {
            stream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            quality -= 5
        } while (stream.size() > MAX_POST_IMAGE_BYTES && quality >= 60)

        return stream.toByteArray()
    }

    private fun getOrientationDegrees(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun compressAvatarStream(inputStream: InputStream): ByteArray? {
        val original = BitmapFactory.decodeStream(inputStream) ?: return null
        val scaled = scaleBitmap(original, MAX_AVATAR_DIMENSION, MAX_AVATAR_DIMENSION, squareCrop = true)
        return compressBitmapUnderLimit(scaled, MAX_AVATAR_BYTES, initialQuality = 80)
    }

    private fun scaleBitmap(
        src: Bitmap,
        maxWidth: Int,
        maxHeight: Int,
        squareCrop: Boolean
    ): Bitmap {
        var bmp = src
        if (squareCrop && bmp.width != bmp.height) {
            val minDim = min(bmp.width, bmp.height)
            val xOffset = (bmp.width - minDim) / 2
            val yOffset = (bmp.height - minDim) / 2
            bmp = Bitmap.createBitmap(bmp, xOffset, yOffset, minDim, minDim)
        }

        val widthRatio = bmp.width.toFloat() / maxWidth.toFloat()
        val heightRatio = bmp.height.toFloat() / maxHeight.toFloat()
        val maxRatio = max(widthRatio, heightRatio)

        return if (maxRatio > 1f) {
            val targetWidth = (bmp.width / maxRatio).toInt().coerceAtLeast(1)
            val targetHeight = (bmp.height / maxRatio).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true)
        } else {
            bmp
        }
    }

    private fun compressBitmapUnderLimit(
        bitmap: Bitmap,
        maxBytes: Int,
        initialQuality: Int = 80
    ): ByteArray {
        val stream = ByteArrayOutputStream()
        var quality = initialQuality
        do {
            stream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            quality -= 8
        } while (stream.size() > maxBytes && quality >= 35)

        return stream.toByteArray()
    }
}

// Backward compatibility extension / helper
// NOTE: Redirected to the high-quality post compressor (1440px / WebP 88% / 600 KB ceiling)
fun compressImageToUnder50KB(context: Context, uri: Uri): ByteArray? =
    ImageUtils.compressPostImage(context, uri)

fun compressBannerToUnder50KB(context: Context, uri: Uri): ByteArray? =
    ImageUtils.compressBannerForSupabase(context, uri)
