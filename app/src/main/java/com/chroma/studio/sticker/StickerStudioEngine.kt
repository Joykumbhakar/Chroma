package com.chroma.studio.sticker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.compose.ui.geometry.Offset
import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object StickerStudioEngine {

    /**
     * Automatically applies a crisp 5px die-cut white border around a transparent subject,
     * matching the requested 5px white outline specification with anti-aliasing and subtle drop shadow.
     */
    fun apply5pxWhiteBorder(
        source: Bitmap,
        borderPx: Int = 5,
        borderColor: Int = Color.WHITE,
        addDropShadow: Boolean = true
    ): Bitmap {
        val w = source.width
        val h = source.height
        val pad = borderPx + 8
        val outW = w + pad * 2
        val outH = h + pad * 2

        // Padded bitmap holding foreground
        val paddedBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val padCanvas = Canvas(paddedBmp)
        padCanvas.drawBitmap(source, pad.toFloat(), pad.toFloat(), null)

        // Read pixels to dilate alpha channel
        val pixels = IntArray(outW * outH)
        paddedBmp.getPixels(pixels, 0, outW, 0, 0, outW, outH)

        val alphaMask = ByteArray(outW * outH)
        for (i in pixels.indices) {
            val a = (pixels[i] ushr 24) and 0xFF
            alphaMask[i] = if (a > 35) 1 else 0
        }

        // Circular dilation kernel of radius = borderPx
        val r = borderPx
        val kernelOffsets = mutableListOf<Pair<Int, Int>>()
        for (dy in -r..r) {
            for (dx in -r..r) {
                if (dx * dx + dy * dy <= r * r + 1) {
                    kernelOffsets.add(Pair(dx, dy))
                }
            }
        }

        val dilated = ByteArray(outW * outH)
        for (y in 0 until outH) {
            val yOffset = y * outW
            for (x in 0 until outW) {
                if (alphaMask[yOffset + x].toInt() == 1) {
                    for ((dx, dy) in kernelOffsets) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until outW && ny in 0 until outH) {
                            dilated[ny * outW + nx] = 1
                        }
                    }
                }
            }
        }

        // Build output bitmap
        val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val outCanvas = Canvas(output)

        // 1. Subtle drop shadow so white border stands out on white backgrounds
        if (addDropShadow) {
            val shadowPixels = IntArray(outW * outH)
            val shadowColor = Color.argb(45, 0, 0, 0)
            for (i in dilated.indices) {
                if (dilated[i].toInt() == 1) {
                    shadowPixels[i] = shadowColor
                }
            }
            val shadowBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            shadowBmp.setPixels(shadowPixels, 0, outW, 0, 0, outW, outH)
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                maskFilter = android.graphics.BlurMaskFilter(4f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            outCanvas.drawBitmap(shadowBmp, 2f, 3f, shadowPaint)
            shadowBmp.recycle()
        }

        // 2. Crisp solid 5px white border
        val borderPixels = IntArray(outW * outH)
        for (i in dilated.indices) {
            if (dilated[i].toInt() == 1) {
                borderPixels[i] = borderColor
            }
        }
        val borderBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        borderBmp.setPixels(borderPixels, 0, outW, 0, 0, outW, outH)
        outCanvas.drawBitmap(borderBmp, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        borderBmp.recycle()

        // 3. Composite original foreground on top
        outCanvas.drawBitmap(paddedBmp, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        paddedBmp.recycle()

        // 4. Auto-crop transparent boundaries
        return cropTransparentPadding(output, margin = 4)
    }

    /**
     * Automatic subject segmentation and background removal using flood-fill color clustering
     * and edge preservation.
     */
    fun autoCutoutSubject(source: Bitmap, tolerance: Int = 32): Bitmap {
        // Downscale large images for responsive on-device processing
        val maxDim = 800
        val scale = if (source.width > maxDim || source.height > maxDim) {
            maxDim.toFloat() / max(source.width, source.height)
        } else 1.0f

        val workingBmp = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val w = workingBmp.width
        val h = workingBmp.height
        val pixels = IntArray(w * h)
        workingBmp.getPixels(pixels, 0, w, 0, 0, w, h)

        // Sample background reference colors from corners and border midpoints
        val sampleCoords = listOf(
            0,
            w - 1,
            (h - 1) * w,
            (h - 1) * w + (w - 1),
            w / 2,
            (h - 1) * w + (w / 2),
            (h / 2) * w,
            (h / 2) * w + (w - 1)
        )
        val bgColors = sampleCoords.map { pixels[it] }

        // BFS flood-fill from all 4 borders to remove contiguous background
        val visited = BooleanArray(w * h)
        val isBackground = BooleanArray(w * h)
        val queue = ArrayDeque<Int>(w * 4)

        // Enqueue border pixels
        for (x in 0 until w) {
            queue.add(x)
            queue.add((h - 1) * w + x)
            visited[x] = true
            visited[(h - 1) * w + x] = true
        }
        for (y in 0 until h) {
            val idxL = y * w
            val idxR = y * w + (w - 1)
            if (!visited[idxL]) { queue.add(idxL); visited[idxL] = true }
            if (!visited[idxR]) { queue.add(idxR); visited[idxR] = true }
        }

        val tolSq = (tolerance * tolerance * 3.5).toInt()

        while (!queue.isEmpty()) {
            val curr = queue.poll() ?: break
            val cx = curr % w
            val cy = curr / w
            val cColor = pixels[curr]
            val cr = (cColor ushr 16) and 0xFF
            val cg = (cColor ushr 8) and 0xFF
            val cb = cColor and 0xFF

            // Check if matches any sampled background color
            var matchesBg = false
            for (bg in bgColors) {
                val bgr = (bg ushr 16) and 0xFF
                val bgg = (bg ushr 8) and 0xFF
                val bgb = bg and 0xFF
                val distSq = (cr - bgr) * (cr - bgr) + (cg - bgg) * (cg - bgg) + (cb - bgb) * (cb - bgb)
                if (distSq <= tolSq) {
                    matchesBg = true
                    break
                }
            }

            if (matchesBg) {
                isBackground[curr] = true
                // Expand to 4 neighbors
                val neighbors = arrayOf(
                    if (cx > 0) curr - 1 else -1,
                    if (cx < w - 1) curr + 1 else -1,
                    if (cy > 0) curr - w else -1,
                    if (cy < h - 1) curr + w else -1
                )
                for (n in neighbors) {
                    if (n != -1 && !visited[n]) {
                        visited[n] = true
                        queue.add(n)
                    }
                }
            }
        }

        // Apply alpha mask
        for (i in pixels.indices) {
            if (isBackground[i]) {
                pixels[i] = 0 // Transparent
            }
        }

        val resultBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        resultBmp.setPixels(pixels, 0, w, 0, 0, w, h)
        if (workingBmp != source) workingBmp.recycle()

        return cropTransparentPadding(resultBmp, margin = 4)
    }

    /**
     * Erases manual touch strokes with a circular brush on the bitmap.
     */
    fun manualErase(
        source: Bitmap,
        pathPoints: List<Offset>,
        brushRadiusPx: Float
    ): Bitmap {
        if (pathPoints.isEmpty()) return source
        val mutable = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            style = Paint.Style.STROKE
            strokeWidth = brushRadiusPx * 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val path = Path()
        val first = pathPoints.first()
        path.moveTo(first.x, first.y)
        for (i in 1 until pathPoints.size) {
            val pt = pathPoints[i]
            path.lineTo(pt.x, pt.y)
        }
        canvas.drawPath(path, paint)
        return mutable
    }

    /**
     * Trims transparent border pixels from a bitmap with an optional margin.
     */
    fun cropTransparentPadding(src: Bitmap, margin: Int = 4): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        var minX = w
        var minY = h
        var maxX = 0
        var maxY = 0
        var hasVisiblePixel = false

        for (y in 0 until h) {
            val yOffset = y * w
            for (x in 0 until w) {
                val alpha = (pixels[yOffset + x] ushr 24) and 0xFF
                if (alpha > 15) {
                    hasVisiblePixel = true
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (!hasVisiblePixel) return src

        val cropX = max(0, minX - margin)
        val cropY = max(0, minY - margin)
        val cropW = min(w - cropX, (maxX - minX + 1) + margin * 2)
        val cropH = min(h - cropY, (maxY - minY + 1) + margin * 2)

        return Bitmap.createBitmap(src, cropX, cropY, cropW, cropH)
    }
}
