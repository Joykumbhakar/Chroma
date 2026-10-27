package com.chroma.studio.sticker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object StickerRepository {

    private const val STICKERS_DIR = "chroma_stickers"

    /**
     * Saves a transparent sticker bitmap (with 5px white border) to app internal storage.
     */
    suspend fun saveCustomSticker(
        context: Context,
        bitmap: Bitmap,
        name: String = "Custom Sticker"
    ): StickerItem = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, STICKERS_DIR)
        if (!dir.exists()) dir.mkdirs()

        val id = UUID.randomUUID().toString()
        val file = File(dir, "sticker_${id}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        StickerItem(
            id = id,
            uri = Uri.fromFile(file).toString(),
            name = name,
            isCustom = true,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Returns all saved custom stickers in reverse chronological order.
     */
    suspend fun loadCustomStickers(context: Context): List<StickerItem> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, STICKERS_DIR)
        if (!dir.exists()) return@withContext emptyList()

        val files = dir.listFiles { f -> f.extension.equals("png", ignoreCase = true) } ?: return@withContext emptyList()
        files.sortedByDescending { it.lastModified() }.map { f ->
            StickerItem(
                id = f.nameWithoutExtension.removePrefix("sticker_"),
                uri = Uri.fromFile(f).toString(),
                name = "Custom Sticker",
                isCustom = true,
                timestamp = f.lastModified()
            )
        }
    }

    /**
     * Deletes a custom sticker by ID.
     */
    suspend fun deleteSticker(context: Context, stickerId: String): Boolean = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, STICKERS_DIR)
        val file = File(dir, "sticker_${stickerId}.png")
        if (file.exists()) file.delete() else false
    }

    /**
     * Generates a sample / built-in sticker if custom list is empty,
     * ensuring user always has beautiful 5px-bordered stickers ready to test.
     */
    suspend fun getOrGenerateDefaultStickers(context: Context): List<StickerItem> = withContext(Dispatchers.IO) {
        val existing = loadCustomStickers(context)
        if (existing.isNotEmpty()) return@withContext existing

        // Generate 3 stylish default 5px-bordered stickers (Spark, Verified, Heart)
        val defaultItems = mutableListOf<StickerItem>()

        // 1. Chroma Spark
        val sparkBmp = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        val c1 = Canvas(sparkBmp)
        val p1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#38BDF8") }
        val pCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#818CF8") }
        c1.drawCircle(80f, 80f, 50f, p1)
        c1.drawCircle(80f, 80f, 32f, pCenter)
        val sparkSticker = StickerStudioEngine.apply5pxWhiteBorder(sparkBmp, borderPx = 5)
        defaultItems.add(saveCustomSticker(context, sparkSticker, "Chroma Spark"))
        sparkBmp.recycle()

        // 2. Verified Star Badge
        val starBmp = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        val c2 = Canvas(starBmp)
        val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10B981") }
        c2.drawRoundRect(25f, 25f, 135f, 135f, 36f, 36f, p2)
        val starSticker = StickerStudioEngine.apply5pxWhiteBorder(starBmp, borderPx = 5)
        defaultItems.add(saveCustomSticker(context, starSticker, "Verified Badge"))
        starBmp.recycle()

        // 3. Neon Heart
        val heartBmp = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        val c3 = Canvas(heartBmp)
        val p3 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F43F5E") }
        c3.drawCircle(58f, 65f, 32f, p3)
        c3.drawCircle(102f, 65f, 32f, p3)
        val path = android.graphics.Path().apply {
            moveTo(30f, 75f)
            lineTo(80f, 130f)
            lineTo(130f, 75f)
            close()
        }
        c3.drawPath(path, p3)
        val heartSticker = StickerStudioEngine.apply5pxWhiteBorder(heartBmp, borderPx = 5)
        defaultItems.add(saveCustomSticker(context, heartSticker, "Neon Heart"))
        heartBmp.recycle()

        defaultItems
    }
}
