package com.chroma.studio.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.OutputStream

object ExportHelper {

    private const val TAG = "ExportHelper"

    fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String, isPng: Boolean): Uri? {
        val mimeType = if (isPng) "image/png" else "image/jpeg"
        val ext = if (isPng) ".png" else ".jpg"
        val fullFileName = if (fileName.endsWith(ext)) fileName else "$fileName$ext"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Oscellion")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { stream ->
                    val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    bitmap.compress(format, 100, stream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            } catch (e: Exception) {
                resolver.delete(it, null, null)
                return null
            }
        }
        return uri
    }

    fun saveText(context: Context, content: String, fileName: String, ext: String = ".svg"): Uri? {
        val fullFileName = if (fileName.endsWith(ext)) fileName else "$fileName$ext"
        
        val mimeType = when (ext) {
            ".svg" -> "image/svg+xml"
            ".json" -> "application/json"
            else -> "text/plain"
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Oscellion")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        
        val resolver = context.contentResolver
        // Use Downloads for generic files like SVG since they aren't native Gallery images
        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }
        
        val uri = resolver.insert(collectionUri, contentValues)
        
        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            } catch (e: Exception) {
                resolver.delete(it, null, null)
                return null
            }
        }
        return uri
    }

    data class ZipAsset(
        val name: String,
        val content: ByteArray
    )

    fun saveZip(context: Context, fileName: String, assets: List<ZipAsset>): Uri? {
        val fullFileName = if (fileName.endsWith(".zip")) fileName else "$fileName.zip"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Oscellion")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        
        val resolver = context.contentResolver
        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }
        
        val uri = resolver.insert(collectionUri, contentValues)
        
        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { stream ->
                    java.util.zip.ZipOutputStream(stream).use { zos ->
                        for (asset in assets) {
                            val entry = java.util.zip.ZipEntry(asset.name)
                            zos.putNextEntry(entry)
                            zos.write(asset.content)
                            zos.closeEntry()
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            } catch (e: Exception) {
                resolver.delete(it, null, null)
                return null
            }
        }
        return uri
    }

    /**
     * Saves a video [java.io.File] into the device's public Gallery.
     * Automatically registers with MediaStore so it appears immediately in Google Photos / Gallery.
     * Uses DCIM/ChromaStudio/ (with fallback to Movies/ChromaStudio/) and scans the file.
     */
    fun saveVideoToGallery(
        context: Context,
        sourceFile: java.io.File,
        title: String = "ChromaStudio_${System.currentTimeMillis()}"
    ): Uri? {
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            Log.e(TAG, "saveVideoToGallery: sourceFile does not exist or is empty ($sourceFile)")
            return null
        }

        val fullFileName = if (title.endsWith(".mp4")) title else "$title.mp4"
        val currentTimeMs = System.currentTimeMillis()
        val currentTimeSec = currentTimeMs / 1000

        // Extract metadata for optimal Gallery indexing
        var durMs: Long? = null
        var width: Int? = null
        var height: Int? = null
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(sourceFile.absolutePath)
            durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            retriever.release()
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract metadata from $sourceFile", e)
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fullFileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.TITLE, title.removeSuffix(".mp4"))
            put(MediaStore.Video.Media.DATE_ADDED, currentTimeSec)
            put(MediaStore.Video.Media.DATE_MODIFIED, currentTimeSec)
            put(MediaStore.Video.Media.DATE_TAKEN, currentTimeMs)
            put(MediaStore.Video.Media.SIZE, sourceFile.length())
            if (durMs != null && durMs > 0L) {
                put(MediaStore.Video.Media.DURATION, durMs)
            }
            if (width != null && width > 0 && height != null && height > 0) {
                put(MediaStore.Video.Media.WIDTH, width)
                put(MediaStore.Video.Media.HEIGHT, height)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // DCIM/ChromaStudio/ indexes both in the main camera roll and its own album
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/ChromaStudio/")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        var uri: Uri? = null

        // Try inserting into DCIM/ChromaStudio/ first; fallback to Movies/ChromaStudio/ if needed
        try {
            uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to insert into DCIM/ChromaStudio/, falling back to Movies/ChromaStudio/", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ChromaStudio/")
                try {
                    uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed fallback insert into Movies/ChromaStudio/", e2)
                }
            }
        }

        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outStream ->
                    sourceFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                // Query the saved file path and trigger MediaScanner so it instantly appears in gallery
                var savedFilePath: String? = null
                try {
                    resolver.query(uri, arrayOf(MediaStore.Video.Media.DATA), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val dataIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                            if (dataIdx >= 0) savedFilePath = cursor.getString(dataIdx)
                        }
                    }
                } catch (_: Exception) {}

                if (savedFilePath != null) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(savedFilePath),
                        arrayOf("video/mp4")
                    ) { path, scannedUri ->
                        Log.i(TAG, "MediaScanner finished scanning $path -> $scannedUri")
                    }
                } else {
                    try {
                        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
                        context.sendBroadcast(mediaScanIntent)
                    } catch (_: Exception) {}
                }

                Log.i(TAG, "Successfully saved video to gallery: $uri (path: $savedFilePath)")
                return uri
            } catch (e: Exception) {
                Log.e(TAG, "Failed writing video bytes to $uri", e)
                try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            }
        }

        // Direct file fallback for older Android or restricted scoped storage
        try {
            val dcimDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "ChromaStudio")
            if (!dcimDir.exists()) dcimDir.mkdirs()
            val destFile = File(dcimDir, fullFileName)
            sourceFile.copyTo(destFile, overwrite = true)

            var scannedUri: Uri? = null
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("video/mp4")
            ) { _, u ->
                scannedUri = u
            }
            Log.i(TAG, "Saved video via direct file fallback: ${destFile.absolutePath}")
            return scannedUri ?: Uri.fromFile(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Direct file copy fallback failed", e)
        }

        return null
    }

    /**
     * Saves a video from an input content [Uri] into the device's public Gallery.
     */
    fun saveVideoUriToGallery(
        context: Context,
        sourceUri: Uri,
        title: String = "ChromaStudio_${System.currentTimeMillis()}"
    ): Uri? {
        val fullFileName = if (title.endsWith(".mp4")) title else "$title.mp4"
        val currentTimeMs = System.currentTimeMillis()
        val currentTimeSec = currentTimeMs / 1000

        var durMs: Long? = null
        var width: Int? = null
        var height: Int? = null
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, sourceUri)
            durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            retriever.release()
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract metadata from $sourceUri", e)
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fullFileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.TITLE, title.removeSuffix(".mp4"))
            put(MediaStore.Video.Media.DATE_ADDED, currentTimeSec)
            put(MediaStore.Video.Media.DATE_MODIFIED, currentTimeSec)
            put(MediaStore.Video.Media.DATE_TAKEN, currentTimeMs)
            if (durMs != null && durMs > 0L) {
                put(MediaStore.Video.Media.DURATION, durMs)
            }
            if (width != null && width > 0 && height != null && height > 0) {
                put(MediaStore.Video.Media.WIDTH, width)
                put(MediaStore.Video.Media.HEIGHT, height)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/ChromaStudio/")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        var uri: Uri? = null

        try {
            uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        } catch (e: Exception) {
            Log.w(TAG, "Failed insert into DCIM/ChromaStudio/, falling back to Movies/ChromaStudio/", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ChromaStudio/")
                try {
                    uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed fallback insert into Movies/ChromaStudio/", e2)
                }
            }
        }

        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outStream ->
                    resolver.openInputStream(sourceUri)?.use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                var savedFilePath: String? = null
                try {
                    resolver.query(uri, arrayOf(MediaStore.Video.Media.DATA), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val dataIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                            if (dataIdx >= 0) savedFilePath = cursor.getString(dataIdx)
                        }
                    }
                } catch (_: Exception) {}

                if (savedFilePath != null) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(savedFilePath),
                        arrayOf("video/mp4")
                    ) { path, scannedUri ->
                        Log.i(TAG, "MediaScanner finished scanning $path -> $scannedUri")
                    }
                } else {
                    try {
                        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
                        context.sendBroadcast(mediaScanIntent)
                    } catch (_: Exception) {}
                }
                return uri
            } catch (e: Exception) {
                Log.e(TAG, "Failed writing video bytes from $sourceUri to $uri", e)
                try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            }
        }
        return null
    }
}
