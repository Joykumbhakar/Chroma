package com.chroma.studio.data

import android.util.Log
import com.chroma.studio.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Cloudinary image uploader using unsigned upload preset.
 * Includes GZIP compression negotiation and a Circuit Breaker pattern.
 */
object CloudinaryClient {

    private val cloudName: String get() = BuildConfig.CLOUDINARY_CLOUD_NAME
    private val uploadPreset: String get() = BuildConfig.CLOUDINARY_UPLOAD_PRESET

    // Circuit Breaker State
    private val failureCount = AtomicInteger(0)
    private val circuitOpenTime = AtomicLong(0)
    private const val MAX_FAILURES = 3
    private const val COOLDOWN_MS = 60_000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    suspend fun uploadImage(imageBytes: ByteArray, folder: String = ""): String? {
        return withContext(Dispatchers.IO) {
            // Circuit Breaker Check
            if (failureCount.get() >= MAX_FAILURES) {
                if (System.currentTimeMillis() - circuitOpenTime.get() < COOLDOWN_MS) {
                    Log.e("CloudinaryClient", "Circuit breaker OPEN. Fast failing upload to avoid hanging.")
                    return@withContext null
                }
            }

            try {
                if (cloudName.isBlank()) {
                    Log.e("CloudinaryClient", "CLOUDINARY_CLOUD_NAME not set in local.properties")
                    return@withContext null
                }
                if (uploadPreset.isBlank()) {
                    Log.e("CloudinaryClient", "CLOUDINARY_UPLOAD_PRESET not set in local.properties")
                    return@withContext null
                }

                val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
                val folderPath = if (folder.isNotBlank()) "posts/$folder" else "posts"

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        "post_${System.currentTimeMillis()}.jpg",
                        imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    .addFormDataPart("upload_preset", uploadPreset)
                    .addFormDataPart("folder", folderPath)
                    .build()

                val request = Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    Log.e("CloudinaryClient", "Upload failed: HTTP ${response.code} — $responseBody")
                    recordFailure()
                    return@withContext null
                }

                val json = JSONObject(responseBody)
                val secureUrl = json.optString("secure_url", "")
                if (secureUrl.isBlank()) {
                    Log.e("CloudinaryClient", "Upload succeeded but no secure_url in response")
                    recordFailure()
                    null
                } else {
                    Log.d("CloudinaryClient", "Upload success: $secureUrl")
                    recordSuccess()
                    secureUrl
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("CloudinaryClient", "Upload exception", e)
                recordFailure()
                null
            }
        }
    }

    fun resetCircuitBreaker() {
        failureCount.set(0)
        circuitOpenTime.set(0)
    }

    private fun recordFailure() {
        val currentFails = failureCount.incrementAndGet()
        if (currentFails >= MAX_FAILURES) {
            circuitOpenTime.set(System.currentTimeMillis())
        }
    }

    private fun recordSuccess() {
        failureCount.set(0)
        circuitOpenTime.set(0)
    }

    // ── Video Upload (Flash Feed) ─────────────────────────────────────────

    private val videoUploadPreset: String get() = BuildConfig.CLOUDINARY_VIDEO_UPLOAD_PRESET

    /**
     * Upload a video to Cloudinary directly from a [java.io.File].
     * Streams the file in 32KB chunks directly from disk — avoids loading the video into JVM heap.
     * Tries the primary video preset first, then falls back to the posts preset.
     * Reports upload progress via [onProgress] (0.0..1.0).
     */
    suspend fun uploadVideoFile(
        file: java.io.File,
        folder: String = "flash_videos",
        onProgress: ((Float) -> Unit)? = null
    ): String? {
        return withContext(Dispatchers.IO) {
            // Circuit Breaker Check
            if (failureCount.get() >= MAX_FAILURES) {
                if (System.currentTimeMillis() - circuitOpenTime.get() < COOLDOWN_MS) {
                    Log.e("CloudinaryClient", "Circuit breaker OPEN — video upload fast-failed.")
                    return@withContext null
                }
            }
            if (!file.exists() || file.length() == 0L) {
                Log.e("CloudinaryClient", "Video file does not exist or is empty: ${file.absolutePath}")
                return@withContext null
            }
            if (cloudName.isBlank()) {
                Log.e("CloudinaryClient", "CLOUDINARY_CLOUD_NAME not set")
                return@withContext null
            }

            // Attempt with primary video preset first; fallback to main post preset if needed
            val presetsToTry = listOfNotNull(
                videoUploadPreset.takeIf { it.isNotBlank() },
                uploadPreset.takeIf { it.isNotBlank() && it != videoUploadPreset }
            ).ifEmpty { listOf("flash_videos", "oscellion_posts") }

            for (preset in presetsToTry) {
                val url = performVideoUpload(file, folder, preset, onProgress)
                if (url != null) {
                    recordSuccess()
                    return@withContext url
                }
                Log.w("CloudinaryClient", "Video upload failed with preset '$preset', trying fallback if available...")
            }

            recordFailure()
            null
        }
    }

    private fun performVideoUpload(
        file: java.io.File,
        folder: String,
        preset: String,
        onProgress: ((Float) -> Unit)?
    ): String? {
        return try {
            val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/video/upload"
            val folderPath = if (folder.isNotBlank()) folder else "flash_videos"
            val totalBytes = file.length()
            val fileName = "flash_${System.currentTimeMillis()}.mp4"

            val countingBody = object : okhttp3.RequestBody() {
                override fun contentType() = "video/mp4".toMediaTypeOrNull()
                override fun contentLength() = totalBytes
                override fun writeTo(sink: okio.BufferedSink) {
                    val buffer = ByteArray(32768)
                    var uploaded = 0L
                    file.inputStream().use { input ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            sink.write(buffer, 0, read)
                            uploaded += read
                            if (totalBytes > 0L) {
                                onProgress?.invoke(uploaded.toFloat() / totalBytes.toFloat())
                            }
                        }
                    }
                }
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, countingBody)
                .addFormDataPart("upload_preset", preset)
                .addFormDataPart("folder", folderPath)
                .addFormDataPart("resource_type", "video")
                .build()

            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e("CloudinaryClient", "Video upload failed (preset $preset): HTTP ${response.code} — $responseBody")
                return null
            }

            val json = JSONObject(responseBody)
            val secureUrl = json.optString("secure_url", "")
            if (secureUrl.isNotBlank()) {
                Log.d("CloudinaryClient", "Video upload success (preset $preset): $secureUrl")
                secureUrl
            } else {
                null
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("CloudinaryClient", "performVideoUpload exception with preset $preset", e)
            null
        }
    }

    /**
     * Upload a video to Cloudinary from a [android.net.Uri].
     * If uri is file://, uploads directly without memory duplication.
     * If uri is content://, streams through a temp cache file.
     * Reports upload progress via [onProgress] (0.0..1.0).
     */
    suspend fun uploadVideoFromUri(
        context: android.content.Context,
        uri: android.net.Uri,
        folder: String = "flash_videos",
        onProgress: ((Float) -> Unit)? = null
    ): String? {
        return withContext(Dispatchers.IO) {
            if (uri.scheme == "file") {
                val file = java.io.File(uri.path ?: "")
                return@withContext uploadVideoFile(file, folder, onProgress)
            }
            // For content:// scheme, stream to temporary cache file then upload
            val tempFile = java.io.File(context.cacheDir, "cloudinary_vid_${System.currentTimeMillis()}.mp4")
            try {
                val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (copied == null || copied == 0L || !tempFile.exists()) {
                    Log.e("CloudinaryClient", "Failed to read content URI: $uri")
                    return@withContext null
                }
                uploadVideoFile(tempFile, folder, onProgress)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("CloudinaryClient", "uploadVideoFromUri error", e)
                null
            } finally {
                try { tempFile.delete() } catch (_: Exception) {}
            }
        }
    }

    /** Helper to apply a Cloudinary transformation string to a raw delivery URL. */
    fun applyTransformation(
        url: String, 
        filterName: String, 
        brightness: Float = 0f, 
        contrast: Float = 1f, 
        saturation: Float = 1f, 
        hue: Float = 0f
    ): String {
        if (url.isBlank()) return url
        
        val isDefaultFilter = filterName.isBlank() || filterName.equals("None", ignoreCase = true)
        val isDefaultAdjustments = brightness == 0f && contrast == 1f && saturation == 1f && hue == 0f
        
        // Preserve clean direct video URL when no visual filter is requested to prevent Cloudinary audio stripping
        if (isDefaultFilter && isDefaultAdjustments) {
            return url
        }

        // Map local filter names to Cloudinary transformations
        val effect = when (filterName) {
            "B&W" -> "e_grayscale"
            "Cinematic" -> "e_art:incognito"
            "Warm" -> "e_art:sizzle"
            "Cool" -> "e_art:frost"
            "Fade" -> "e_art:peacock"
            "Vivid" -> "e_art:zorro"
            "Matte" -> "e_art:audrey"
            else -> ""
        }
        
        val mappedBrightness = brightness.coerceIn(-99f, 100f).toInt()
        val mappedContrast = ((contrast - 1f) * 100f).coerceIn(-100f, 100f).toInt()
        val mappedSaturation = ((saturation - 1f) * 100f).coerceIn(-100f, 100f).toInt()
        val mappedHue = ((hue / 180f) * 100f).coerceIn(-100f, 100f).toInt()

        val adjustments = mutableListOf<String>()
        if (mappedBrightness != 0) adjustments.add("e_brightness:$mappedBrightness")
        if (mappedContrast != 0) adjustments.add("e_contrast:$mappedContrast")
        if (mappedSaturation != 0) adjustments.add("e_saturation:$mappedSaturation")
        if (mappedHue != 0) adjustments.add("e_hue:$mappedHue")
        
        val adjustmentsStr = adjustments.joinToString(",")
        
        // Explicitly preserve the audio stream with ac_aac and avoid destructive f_auto conversions
        val allTransformations = mutableListOf("ac_aac", "q_auto:good")
        if (effect.isNotEmpty()) allTransformations.add(effect)
        if (adjustmentsStr.isNotEmpty()) allTransformations.add(adjustmentsStr)
        
        val transformation = allTransformations.joinToString(",")
        
        val uploadSegment = "/upload/"
        val idx = url.indexOf(uploadSegment)
        if (idx == -1) return url
        
        return url.substring(0, idx + uploadSegment.length) + transformation + "/" + url.substring(idx + uploadSegment.length)
    }
    /** Upload a thumbnail image for a flash video. Thin wrapper around [uploadImage]. */
    suspend fun uploadFlashThumbnail(imageBytes: ByteArray): String? =
        uploadImage(imageBytes, folder = "flash_thumbnails")

    /**
     * Upload an audio file (e.g. voice note) to Cloudinary.
     */
    /**
     * Upload a video file to Cloudinary.
     */
    suspend fun uploadVideo(
        videoBytes: ByteArray,
        extension: String = "mp4",
        folder: String = "chat_videos"
    ): String? {
        return withContext(Dispatchers.IO) {
            if (failureCount.get() >= MAX_FAILURES) {
                if (System.currentTimeMillis() - circuitOpenTime.get() < COOLDOWN_MS) {
                    Log.e("CloudinaryClient", "Circuit breaker OPEN — video upload fast-failed.")
                    return@withContext null
                }
            }
            try {
                if (cloudName.isBlank()) {
                    Log.e("CloudinaryClient", "Cloudinary credentials not set")
                    return@withContext null
                }

                val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/video/upload"
                val fileName = "video_${System.currentTimeMillis()}.$extension"
                val mediaType = "video/$extension".toMediaTypeOrNull()

                val presetsToTry = listOfNotNull(
                    videoUploadPreset.takeIf { it.isNotBlank() },
                    uploadPreset.takeIf { it.isNotBlank() && it != videoUploadPreset }
                ).ifEmpty { listOf("flash_videos", "oscellion_posts") }

                for (preset in presetsToTry) {
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName, videoBytes.toRequestBody(mediaType))
                        .addFormDataPart("upload_preset", preset)
                        .addFormDataPart("folder", folder)
                        .addFormDataPart("resource_type", "video")
                        .build()

                    val request = Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        val json = JSONObject(responseBody)
                        val secureUrl = json.optString("secure_url", "")
                        if (secureUrl.isNotBlank()) {
                            recordSuccess()
                            return@withContext secureUrl
                        }
                    }
                    Log.w("CloudinaryClient", "uploadVideo with preset '$preset' failed, trying next...")
                }
                recordFailure()
                null
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("CloudinaryClient", "Video upload exception", e)
                recordFailure()
                null
            }
        }
    }

    suspend fun uploadAudio(
        audioBytes: ByteArray,
        extension: String = "m4a",
        folder: String = "chat_audio"
    ): String? {
        return withContext(Dispatchers.IO) {
            if (failureCount.get() >= MAX_FAILURES) {
                if (System.currentTimeMillis() - circuitOpenTime.get() < COOLDOWN_MS) {
                    Log.e("CloudinaryClient", "Circuit breaker OPEN — audio upload fast-failed.")
                    return@withContext null
                }
            }
            try {
                if (cloudName.isBlank() || videoUploadPreset.isBlank()) {
                    Log.e("CloudinaryClient", "Cloudinary credentials not set")
                    return@withContext null
                }

                val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/video/upload"
                val fileName = "audio_${System.currentTimeMillis()}.$extension"
                val mediaType = "audio/$extension".toMediaTypeOrNull()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, audioBytes.toRequestBody(mediaType))
                    .addFormDataPart("upload_preset", videoUploadPreset)
                    .addFormDataPart("folder", folder)
                    .addFormDataPart("resource_type", "video")
                    .build()

                val request = Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    Log.e("CloudinaryClient", "Audio upload failed: HTTP ${response.code} — $responseBody")
                    recordFailure()
                    return@withContext null
                }

                val json = JSONObject(responseBody)
                val secureUrl = json.optString("secure_url", "")
                if (secureUrl.isBlank()) {
                    recordFailure()
                    null
                } else {
                    recordSuccess()
                    secureUrl
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("CloudinaryClient", "Audio upload exception", e)
                recordFailure()
                null
            }
        }
    }
}

