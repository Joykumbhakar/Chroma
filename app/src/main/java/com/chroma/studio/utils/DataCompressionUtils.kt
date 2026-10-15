package com.chroma.studio.utils

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.json.JSONObject
import org.json.JSONArray

/**
 * High-efficiency text and JSON compression utilities for Firebase Firestore and Realtime Database.
 * Minimizes storage quota consumption by stripping whitespace and applying GZIP compression.
 */
object DataCompressionUtils {

    private const val GZIP_PREFIX = "gz:"
    private const val COMPRESSION_THRESHOLD_BYTES = 160

    /**
     * Minifies a JSON string by parsing and compacting without whitespaces.
     */
    fun minifyJson(rawJson: String): String {
        if (rawJson.isBlank()) return rawJson
        val trimmed = rawJson.trim()
        return try {
            if (trimmed.startsWith("{")) {
                JSONObject(trimmed).toString()
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed).toString()
            } else {
                trimmed
            }
        } catch (_: Exception) {
            // Fallback: strip excessive whitespace and newlines
            trimmed.replace(Regex("\\s+"), " ")
        }
    }

    /**
     * Compresses text or JSON payload with GZIP + Base64 if size exceeds threshold.
     */
    fun compressText(text: String): String {
        if (text.isBlank()) return text
        val minified = if (text.startsWith("{") || text.startsWith("[")) minifyJson(text) else text.trim()
        val bytes = minified.toByteArray(StandardCharsets.UTF_8)
        
        // Only compress if the data is large enough to benefit from compression
        if (bytes.size < COMPRESSION_THRESHOLD_BYTES) {
            return minified
        }

        return try {
            val byteOut = ByteArrayOutputStream()
            GZIPOutputStream(byteOut).use { gzip ->
                gzip.write(bytes)
            }
            val compressedBytes = byteOut.toByteArray()
            if (compressedBytes.size < bytes.size) {
                GZIP_PREFIX + Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
            } else {
                minified
            }
        } catch (_: Exception) {
            minified
        }
    }

    /**
     * Decompresses text or JSON payload. Transparently handles both compressed (gz:...) and raw text.
     */
    fun decompressText(compressedOrRaw: String?): String {
        if (compressedOrRaw.isNullOrBlank()) return ""
        if (!compressedOrRaw.startsWith(GZIP_PREFIX)) return compressedOrRaw

        return try {
            val base64Data = compressedOrRaw.substring(GZIP_PREFIX.length)
            val compressedBytes = Base64.decode(base64Data, Base64.NO_WRAP)
            val byteIn = ByteArrayInputStream(compressedBytes)
            val byteOut = ByteArrayOutputStream()
            GZIPInputStream(byteIn).use { gzip ->
                val buffer = ByteArray(512)
                var len: Int
                while (gzip.read(buffer).also { len = it } != -1) {
                    byteOut.write(buffer, 0, len)
                }
            }
            byteOut.toString(StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            compressedOrRaw
        }
    }
}
