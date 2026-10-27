package com.chroma.studio.chat.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class GalleryMediaItem(
    val uri: Uri,
    val name: String,
    val dateAdded: Long,
    val isVideo: Boolean = false,
    val durationMs: Long = 0L,
    val bucketName: String = "All",
    val sizeBytes: Long = 0L
)

object MediaStoreHelper {
    private const val TAG = "MediaStoreHelper"

    /**
     * Creates a temporary image file and returns its FileProvider content URI for camera capture
     */
    fun createTempImageUri(context: Context): Pair<File, Uri> {
        val picturesDir = File(context.cacheDir, "camera_photos").apply {
            if (!exists()) mkdirs()
        }
        val file = File(picturesDir, "photo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        return Pair(file, uri)
    }

    /**
     * Loads images and videos from device MediaStore asynchronously
     */
    suspend fun loadDeviceMedia(context: Context, maxCount: Int = 1000): List<GalleryMediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<GalleryMediaItem>()

        // 1. Fetch Images
        try {
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.SIZE
            )
            val imageSortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                imageSortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)

                if (idCol != -1) {
                    var count = 0
                    while (cursor.moveToNext() && count < maxCount) {
                        val id = cursor.getLong(idCol)
                        val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Image" else "Image"
                        val dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else System.currentTimeMillis()
                        val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "Photos" else "Photos"
                        val sizeBytes = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                        mediaList.add(
                            GalleryMediaItem(
                                uri = contentUri,
                                name = name,
                                dateAdded = dateAdded,
                                isVideo = false,
                                bucketName = bucket,
                                sizeBytes = sizeBytes
                            )
                        )
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed querying device images: ${e.message}", e)
        }

        // 2. Fetch Videos
        try {
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.SIZE
            )
            val videoSortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                videoSortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)

                if (idCol != -1) {
                    var count = 0
                    while (cursor.moveToNext() && count < (maxCount / 2)) {
                        val id = cursor.getLong(idCol)
                        val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Video" else "Video"
                        val dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else System.currentTimeMillis()
                        val duration = if (durCol != -1) cursor.getLong(durCol) else 0L
                        val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "Videos" else "Videos"
                        val sizeBytes = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                        mediaList.add(
                            GalleryMediaItem(
                                uri = contentUri,
                                name = name,
                                dateAdded = dateAdded,
                                isVideo = true,
                                durationMs = duration,
                                bucketName = bucket,
                                sizeBytes = sizeBytes
                            )
                        )
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed querying device videos: ${e.message}", e)
        }

        // Sort all media by date added descending
        mediaList.sortedByDescending { it.dateAdded }
    }

    fun getFileExtension(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        return android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }

    /**
     * Saves media (image or video from remote URL, content URI, or local file) to the device MediaStore / Gallery.
     */
    suspend fun saveMediaToGallery(context: Context, mediaUrlOrUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val isVideo = mediaUrlOrUri.contains(".mp4", ignoreCase = true) ||
                    mediaUrlOrUri.contains(".mov", ignoreCase = true) ||
                    mediaUrlOrUri.contains("video", ignoreCase = true)
            val extension = if (isVideo) "mp4" else "jpg"
            val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
            val fileName = "Chroma_${if (isVideo) "VID" else "IMG"}_${System.currentTimeMillis()}.$extension"

            val rawInput = if (mediaUrlOrUri.startsWith("http://") || mediaUrlOrUri.startsWith("https://")) {
                java.net.URL(mediaUrlOrUri).openStream()
            } else if (mediaUrlOrUri.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(mediaUrlOrUri))
            } else if (mediaUrlOrUri.startsWith("file://")) {
                java.io.FileInputStream(File(Uri.parse(mediaUrlOrUri).path ?: mediaUrlOrUri))
            } else {
                java.io.FileInputStream(File(mediaUrlOrUri))
            }
            val inputStream = rawInput ?: return@withContext false

            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativePath = if (isVideo) {
                        android.os.Environment.DIRECTORY_MOVIES + "/ChromaStudio/"
                    } else {
                        android.os.Environment.DIRECTORY_PICTURES + "/ChromaStudio/"
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val targetCollection = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = resolver.insert(targetCollection, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    inputStream.use { input ->
                        input.copyTo(outputStream)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Saved to Photos & Gallery", android.widget.Toast.LENGTH_SHORT).show()
                }
                return@withContext true
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving media to gallery: ${e.message}", e)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Failed to save: ${e.localizedMessage ?: "Unknown error"}", android.widget.Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    /**
     * Shares media via the Android system Share Sheet (Intent.ACTION_SEND).
     * Downloads or caches to a FileProvider-accessible file first.
     */
    suspend fun shareMedia(context: Context, mediaUrlOrUri: String) = withContext(Dispatchers.IO) {
        try {
            val isVideo = mediaUrlOrUri.contains(".mp4", ignoreCase = true) ||
                    mediaUrlOrUri.contains(".mov", ignoreCase = true) ||
                    mediaUrlOrUri.contains("video", ignoreCase = true)
            val extension = if (isVideo) "mp4" else "jpg"
            val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

            val shareCacheDir = File(context.cacheDir, "shared_media").apply { if (!exists()) mkdirs() }
            val tempFile = File(shareCacheDir, "share_${System.currentTimeMillis()}.$extension")

            val inputStream = if (mediaUrlOrUri.startsWith("http://") || mediaUrlOrUri.startsWith("https://")) {
                java.net.URL(mediaUrlOrUri).openStream()
            } else if (mediaUrlOrUri.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(mediaUrlOrUri))
            } else if (mediaUrlOrUri.startsWith("file://")) {
                java.io.FileInputStream(File(Uri.parse(mediaUrlOrUri).path ?: mediaUrlOrUri))
            } else {
                java.io.FileInputStream(File(mediaUrlOrUri))
            }

            if (inputStream != null) {
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    tempFile
                )
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Media"))
                }
            } else {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, mediaUrlOrUri)
                }
                withContext(Dispatchers.Main) {
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Media"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed sharing media: ${e.message}", e)
            withContext(Dispatchers.Main) {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, mediaUrlOrUri)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Media"))
            }
        }
    }
}



