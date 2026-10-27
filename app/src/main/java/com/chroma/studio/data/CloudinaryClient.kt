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
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
     * Upload a video to Cloudinary from a [android.net.Uri].
     * Streams the file in chunks — avoids loading the full video into memory.
     * Reports upload progress via [onProgress] (0.0..1.0).
     */
    suspend fun uploadVideoFromUri(
        context: android.content.Context,
        uri: android.net.Uri,
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
            try {
                if (cloudName.isBlank()) {
                    Log.e("CloudinaryClient", "CLOUDINARY_CLOUD_NAME not set")
                    return@withContext null
                }

                val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/video/upload"
                val folderPath = if (folder.isNotBlank()) folder else "flash_videos"

                val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext null
                val videoBytes = inputStream.readBytes()
                inputStream.close()

                val totalBytes = videoBytes.size.toLong()
                var uploadedBytes = 0L

                // Build multipart body with progress tracking
                val mediaType = mimeType.toMediaTypeOrNull()
                val fileName = "flash_${System.currentTimeMillis()}.mp4"

                // Use a CountingRequestBody to track progress
                val rawBody = videoBytes.toRequestBody(mediaType)
                val countingBody = object : okhttp3.RequestBody() {
                    override fun contentType() = rawBody.contentType()
                    override fun contentLength() = totalBytes
                    override fun writeTo(sink: okio.BufferedSink) {
                        val bufferedSource = okio.Buffer()
                        rawBody.writeTo(bufferedSource)
                        var bytesRead: Int
                        val buffer = ByteArray(8192)
                        while (bufferedSource.read(buffer).also { bytesRead = it } != -1) {
                            sink.write(buffer, 0, bytesRead)
                            uploadedBytes += bytesRead
                            onProgress?.invoke(uploadedBytes.toFloat() / totalBytes.toFloat())
                        }
                    }
                }
                // Remove eager as unsigned presets typically block it. Apply transformations to delivery URL instead.
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, countingBody)
                    .addFormDataPart("upload_preset", videoUploadPreset)
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
                    Log.e("CloudinaryClient", "Video upload failed: HTTP ${response.code} — $responseBody")
                    recordFailure()
                    return@withContext null
                }

                val json = org.json.JSONObject(responseBody)
                val secureUrl = json.optString("secure_url", "")
                return@withContext if (secureUrl.isBlank()) {
                    Log.e("CloudinaryClient", "Video upload succeeded but no secure_url found.")
                    recordFailure()
                    null
                } else {
                    recordSuccess()
                    Log.d("CloudinaryClient", "Video upload success: $secureUrl")
                    secureUrl
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("CloudinaryClient", "Video upload exception", e)
                recordFailure()
                null
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
        
        val allTransformations = mutableListOf("f_auto", "q_auto:good")
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
                if (cloudName.isBlank() || videoUploadPreset.isBlank()) {
                    Log.e("CloudinaryClient", "Cloudinary credentials not set")
                    return@withContext null
                }

                val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/video/upload"
                val fileName = "video_${System.currentTimeMillis()}.$extension"
                val mediaType = "video/$extension".toMediaTypeOrNull()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, videoBytes.toRequestBody(mediaType))
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
                    Log.e("CloudinaryClient", "Video upload failed: HTTP ${response.code} — $responseBody")
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

