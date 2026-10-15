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
    val bucketName: String = "All"
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
            "${context.packageName}.fileprovider",
            file
        )
        return Pair(file, uri)
    }

    /**
     * Loads images and videos from device MediaStore asynchronously
     */
    suspend fun loadDeviceMedia(context: Context): List<GalleryMediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<GalleryMediaItem>()

        // 1. Fetch Images
        try {
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
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

                if (idCol != -1) {
                    var count = 0
                    while (cursor.moveToNext() && count < 150) {
                        val id = cursor.getLong(idCol)
                        val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Image" else "Image"
                        val dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else System.currentTimeMillis()
                        val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "Photos" else "Photos"
                        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                        mediaList.add(
                            GalleryMediaItem(
                                uri = contentUri,
                                name = name,
                                dateAdded = dateAdded,
                                isVideo = false,
                                bucketName = bucket
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
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
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

                if (idCol != -1) {
                    var count = 0
                    while (cursor.moveToNext() && count < 50) {
                        val id = cursor.getLong(idCol)
                        val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Video" else "Video"
                        val dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else System.currentTimeMillis()
                        val duration = if (durCol != -1) cursor.getLong(durCol) else 0L
                        val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "Videos" else "Videos"
                        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                        mediaList.add(
                            GalleryMediaItem(
                                uri = contentUri,
                                name = name,
                                dateAdded = dateAdded,
                                isVideo = true,
                                durationMs = duration,
                                bucketName = bucket
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
}



