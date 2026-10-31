package com.chroma.studio.utils

import android.content.Context
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
fun compressImageToUnder50KB(context: Context, uri: Uri): ByteArray? =
    ImageUtils.compressAvatarForSupabase(context, uri)

fun compressBannerToUnder50KB(context: Context, uri: Uri): ByteArray? =
    ImageUtils.compressBannerForSupabase(context, uri)
