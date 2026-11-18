package com.chroma.studio.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * OOM-safe image compressor using two-pass inSampleSize decoding and deterministic bitmap recycling.
 * Avatars: max 320x320 (< 25 KB).
 * Banners: max 1080x450 (< 45 KB).
 * Chat images: max 1280px (< 180 KB).
 * Community posts: max 1440px (< 600 KB).
 */
object ImageUtils {

    private const val MAX_AVATAR_DIMENSION = 320
    private const val MAX_AVATAR_BYTES = 25_000

    private const val MAX_BANNER_WIDTH = 1080
    private const val MAX_BANNER_HEIGHT = 450
    private const val MAX_BANNER_BYTES = 45_000

    private const val MAX_CHAT_IMAGE_DIMENSION = 1280
    private const val MAX_CHAT_IMAGE_BYTES = 180_000

    private const val MAX_POST_IMAGE_DIMENSION = 1440
    private const val MAX_POST_IMAGE_BYTES = 600_000

    private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (srcH > reqH || srcW > reqW) {
            val halfH = srcH / 2
            val halfW = srcW / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun decodeSampledFromUri(context: Context, uri: Uri, reqW: Int, reqH: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqW, reqH)
        }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun decodeSampledFromBytes(imageBytes: ByteArray, reqW: Int, reqH: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqW, reqH)
        }
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, opts)
    }

    private fun safeRecycle(vararg bitmaps: Bitmap?, keep: Bitmap? = null) {
        val seen = mutableSetOf<Bitmap>()
        if (keep != null) seen.add(keep)
        for (b in bitmaps) {
            if (b != null && !b.isRecycled && seen.add(b)) {
                try { b.recycle() } catch (_: Throwable) {}
            }
        }
    }

    fun compressAvatarForSupabase(context: Context, uri: Uri): ByteArray? {
        var original: Bitmap? = null
        var rotated: Bitmap? = null
        var scaled: Bitmap? = null
        return try {
            val rotation = getOrientationDegrees(context, uri)
            original = decodeSampledFromUri(context, uri, MAX_AVATAR_DIMENSION * 2, MAX_AVATAR_DIMENSION * 2) ?: return null
            rotated = rotateBitmap(original, rotation)
            scaled = scaleBitmap(rotated, MAX_AVATAR_DIMENSION, MAX_AVATAR_DIMENSION, squareCrop = true)
            compressBitmapUnderLimit(scaled, MAX_AVATAR_BYTES, initialQuality = 80)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress avatar from uri", e)
            null
        } finally {
            safeRecycle(original, rotated, scaled)
        }
    }

    fun compressAvatarBytesForSupabase(imageBytes: ByteArray): ByteArray? {
        var original: Bitmap? = null
        var scaled: Bitmap? = null
        return try {
            original = decodeSampledFromBytes(imageBytes, MAX_AVATAR_DIMENSION * 2, MAX_AVATAR_DIMENSION * 2) ?: return null
            scaled = scaleBitmap(original, MAX_AVATAR_DIMENSION, MAX_AVATAR_DIMENSION, squareCrop = true)
            compressBitmapUnderLimit(scaled, MAX_AVATAR_BYTES, initialQuality = 80)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress avatar bytes", e)
            null
        } finally {
            safeRecycle(original, scaled)
        }
    }

    fun compressAvatarBitmapForSupabase(bitmap: Bitmap): ByteArray? {
        var scaled: Bitmap? = null
        return try {
            scaled = scaleBitmap(bitmap, MAX_AVATAR_DIMENSION, MAX_AVATAR_DIMENSION, squareCrop = true)
            compressBitmapUnderLimit(scaled, MAX_AVATAR_BYTES, initialQuality = 80)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress avatar bitmap", e)
            null
        } finally {
            safeRecycle(scaled, keep = bitmap)
        }
    }

    fun compressBannerForSupabase(context: Context, uri: Uri): ByteArray? {
        var original: Bitmap? = null
        var rotated: Bitmap? = null
        var scaled: Bitmap? = null
        return try {
            val rotation = getOrientationDegrees(context, uri)
            original = decodeSampledFromUri(context, uri, MAX_BANNER_WIDTH * 2, MAX_BANNER_HEIGHT * 2) ?: return null
            rotated = rotateBitmap(original, rotation)
            scaled = scaleBitmap(rotated, MAX_BANNER_WIDTH, MAX_BANNER_HEIGHT, squareCrop = false)
            compressBitmapUnderLimit(scaled, MAX_BANNER_BYTES, initialQuality = 82)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress banner from uri", e)
            null
        } finally {
            safeRecycle(original, rotated, scaled)
        }
    }

    fun compressBannerBytesForSupabase(imageBytes: ByteArray): ByteArray? {
        var original: Bitmap? = null
        var scaled: Bitmap? = null
        return try {
            original = decodeSampledFromBytes(imageBytes, MAX_BANNER_WIDTH * 2, MAX_BANNER_HEIGHT * 2) ?: return null
            scaled = scaleBitmap(original, MAX_BANNER_WIDTH, MAX_BANNER_HEIGHT, squareCrop = false)
            compressBitmapUnderLimit(scaled, MAX_BANNER_BYTES, initialQuality = 82)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress banner bytes", e)
            null
        } finally {
            safeRecycle(original, scaled)
        }
    }

    fun compressBannerBitmapForSupabase(bitmap: Bitmap): ByteArray? {
        var scaled: Bitmap? = null
        return try {
            scaled = scaleBitmap(bitmap, MAX_BANNER_WIDTH, MAX_BANNER_HEIGHT, squareCrop = false)
            compressBitmapUnderLimit(scaled, MAX_BANNER_BYTES, initialQuality = 82)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress banner bitmap", e)
            null
        } finally {
            safeRecycle(scaled, keep = bitmap)
        }
    }

    fun compressChatImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_CHAT_IMAGE_DIMENSION,
        maxBytes: Int = MAX_CHAT_IMAGE_BYTES
    ): ByteArray? {
        var original: Bitmap? = null
        var rotated: Bitmap? = null
        var scaled: Bitmap? = null
        return try {
            val rotation = getOrientationDegrees(context, uri)
            original = decodeSampledFromUri(context, uri, maxDimension, maxDimension) ?: return null
            rotated = rotateBitmap(original, rotation)
            scaled = scaleBitmap(rotated, maxDimension, maxDimension, squareCrop = false)
            compressBitmapUnderLimit(scaled, maxBytes, initialQuality = 85)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress chat image from uri", e)
            null
        } finally {
            safeRecycle(original, rotated, scaled)
        }
    }

    fun compressChatImageBytes(
        imageBytes: ByteArray,
        maxDimension: Int = MAX_CHAT_IMAGE_DIMENSION,
        maxBytes: Int = MAX_CHAT_IMAGE_BYTES
    ): ByteArray? {
        var original: Bitmap? = null
        var scaled: Bitmap? = null
        return try {
            original = decodeSampledFromBytes(imageBytes, maxDimension, maxDimension) ?: return null
            scaled = scaleBitmap(original, maxDimension, maxDimension, squareCrop = false)
            compressBitmapUnderLimit(scaled, maxBytes, initialQuality = 85)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress chat image bytes", e)
            null
        } finally {
            safeRecycle(original, scaled)
        }
    }

    fun compressChatImageBitmap(
        bitmap: Bitmap,
        maxDimension: Int = MAX_CHAT_IMAGE_DIMENSION,
        maxBytes: Int = MAX_CHAT_IMAGE_BYTES
    ): ByteArray? {
        var scaled: Bitmap? = null
        return try {
            scaled = scaleBitmap(bitmap, maxDimension, maxDimension, squareCrop = false)
            compressBitmapUnderLimit(scaled, maxBytes, initialQuality = 85)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress chat image bitmap", e)
            null
        } finally {
            safeRecycle(scaled, keep = bitmap)
        }
    }

    fun compressPostImage(context: Context, uri: Uri): ByteArray? {
        var original: Bitmap? = null
        var rotated: Bitmap? = null
        var scaled: Bitmap? = null
        return try {
            val rotation = getOrientationDegrees(context, uri)
            original = decodeSampledFromUri(context, uri, MAX_POST_IMAGE_DIMENSION, MAX_POST_IMAGE_DIMENSION) ?: return null
            rotated = rotateBitmap(original, rotation)
            scaled = scaleBitmap(rotated, MAX_POST_IMAGE_DIMENSION, MAX_POST_IMAGE_DIMENSION, squareCrop = false)
            compressPostBitmapUnderLimit(scaled)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress post image from uri", e)
            null
        } finally {
            safeRecycle(original, rotated, scaled)
        }
    }

    fun compressPostBitmap(bitmap: Bitmap): ByteArray? {
        var scaled: Bitmap? = null
        return try {
            scaled = scaleBitmap(bitmap, MAX_POST_IMAGE_DIMENSION, MAX_POST_IMAGE_DIMENSION, squareCrop = false)
            compressPostBitmapUnderLimit(scaled)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress post bitmap", e)
            null
        } finally {
            safeRecycle(scaled, keep = bitmap)
        }
    }

    fun compressPostImageBytes(imageBytes: ByteArray): ByteArray? {
        var original: Bitmap? = null
        var scaled: Bitmap? = null
        return try {
            original = decodeSampledFromBytes(imageBytes, MAX_POST_IMAGE_DIMENSION, MAX_POST_IMAGE_DIMENSION) ?: return null
            scaled = scaleBitmap(original, MAX_POST_IMAGE_DIMENSION, MAX_POST_IMAGE_DIMENSION, squareCrop = false)
            compressPostBitmapUnderLimit(scaled)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Failed to compress post image bytes", e)
            null
        } finally {
            safeRecycle(original, scaled)
        }
    }

    private fun compressPostBitmapUnderLimit(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var quality = 88
            do {
                stream.reset()
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, stream)
                quality -= 5
            } while (stream.size() > MAX_POST_IMAGE_BYTES && quality >= 55)
            return stream.toByteArray()
        }
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

    private fun scaleBitmap(
        src: Bitmap,
        maxWidth: Int,
        maxHeight: Int,
        squareCrop: Boolean
    ): Bitmap {
        var cropped: Bitmap? = null
        val bmp = if (squareCrop && src.width != src.height) {
            val minDim = min(src.width, src.height)
            val xOffset = (src.width - minDim) / 2
            val yOffset = (src.height - minDim) / 2
            Bitmap.createBitmap(src, xOffset, yOffset, minDim, minDim).also { cropped = it }
        } else {
            src
        }

        val widthRatio = bmp.width.toFloat() / maxWidth.toFloat()
        val heightRatio = bmp.height.toFloat() / maxHeight.toFloat()
        val maxRatio = max(widthRatio, heightRatio)

        return if (maxRatio > 1f) {
            val targetWidth = (bmp.width / maxRatio).toInt().coerceAtLeast(1)
            val targetHeight = (bmp.height / maxRatio).toInt().coerceAtLeast(1)
            val res = Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true)
            if (cropped != null && cropped !== res) {
                try { cropped?.recycle() } catch (_: Throwable) {}
            }
            res
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

// Backward compatibility top-level helpers
fun compressImageToUnder50KB(context: Context, uri: Uri): ByteArray? =
    ImageUtils.compressPostImage(context, uri)

fun compressBannerToUnder50KB(context: Context, uri: Uri): ByteArray? =
    ImageUtils.compressBannerForSupabase(context, uri)
