package com.chroma.studio.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

object ExportHelper {

    fun saveBitmap(context: Context, bitmap: Bitmap, fileName: String, isPng: Boolean): Uri? {
        val mimeType = if (isPng) "image/png" else "image/jpeg"
        val ext = if (isPng) ".png" else ".jpg"
        val fullFileName = if (fileName.endsWith(ext)) fileName else "$fileName$ext"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ChromaStudio")
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
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ChromaStudio")
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
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ChromaStudio")
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
}
