package com.chroma.studio.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.toArgb
import com.chroma.studio.ui.screens.FcpAspectRatio
import com.chroma.studio.ui.screens.FcpCanvasBackground
import com.chroma.studio.ui.screens.FcpCanvasBgType
import com.chroma.studio.ui.screens.FcpClip
import com.chroma.studio.ui.screens.FcpClipAnimation
import com.chroma.studio.ui.screens.FcpCustomAudio
import com.chroma.studio.ui.screens.FcpLutPreset
import com.chroma.studio.ui.screens.FcpTextLayer
import com.chroma.studio.ui.screens.FcpTitlePresetType
import com.chroma.studio.ui.screens.FcpTransitionType
import com.chroma.studio.ui.screens.FcpVisualEffect
import com.chroma.studio.ui.screens.TextInAnimation
import com.chroma.studio.ui.screens.TextOutAnimation
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Timeline Exporter for the Video Editor.
 *
 * Handles:
 * 1. Multi-clip timelines (video + photo slideshows with EXIF orientation & OOM-safe downscaling)
 * 2. Precision in/out trimming and playback speed adjustment per clip
 * 3. Zoom (in and out) & Pan framing matched to the live preview viewport
 * 4. Full canvas backgrounds (Blur, Solid Color, Custom Image, Studio Gradient Backdrops + Dim)
 * 5. All 12 Title & Text presets with per-layer timeline windows & entrance/exit animations
 * 6. Color grading (exposure, contrast, saturation, WB, vignette, sharpen, 7 LUTs, 9 VFX)
 * 7. Camera motion animations and all 25 transitions with strictly clamped FFmpeg 4.x expressions
 * 8. Custom soundtrack mixing (trim, loop, fade-in/out, mute original, auto-ducking)
 * 9. Safe 2-pass FFmpeg compatibility fallback before native MediaMuxer fallback
 */
object FcpTimelineExporter {

    private const val TAG = "FcpTimelineExporter"

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "gif")

    private val typefaceCache = mutableMapOf<String, Typeface>()
    private val exportMutex = Mutex()

    @Volatile
    private var isCancelled = false

    fun cancelExport() {
        isCancelled = true
        try {
            RxFFmpegInvoke.getInstance().exit()
        } catch (e: Exception) {
            Log.w(TAG, "cancelExport error", e)
        }
    }

    /** Reliable image detection supporting both content:// and file:// URIs. */
    fun isImageClip(context: Context, clip: FcpClip): Boolean {
        return isImageUri(context, clip.uri, clip.name)
    }

    private fun isImageUri(context: Context, uri: Uri, fallbackName: String = ""): Boolean {
        try {
            val mime = context.contentResolver.getType(uri)
            if (mime != null) {
                if (mime.startsWith("image/")) return true
                if (mime.startsWith("video/") || mime.startsWith("audio/")) return false
            }
        } catch (_: Throwable) {}

        val pathExt = (uri.lastPathSegment ?: uri.path ?: "")
            .substringAfterLast('.', "")
            .substringBefore('?')
            .lowercase(Locale.US)
        if (pathExt in IMAGE_EXTENSIONS) return true

        val nameExt = fallbackName.substringAfterLast('.', "").lowercase(Locale.US)
        if (nameExt in IMAGE_EXTENSIONS) return true

        return false
    }

    private fun getTypeface(context: Context, fontAsset: String): Typeface {
        if (fontAsset.isBlank()) return Typeface.DEFAULT_BOLD
        return typefaceCache.getOrPut(fontAsset) {
            val candidates = listOf(
                fontAsset,
                fontAsset.replace(" ", ""),
                fontAsset.replace(".otf", ".ttf"),
                fontAsset.replace(".ttf", ".otf"),
                fontAsset.replace(" ", "").replace(".otf", ".ttf"),
                fontAsset.replace(" ", "").replace(".ttf", ".otf")
            ).distinct()
            for (candidate in candidates) {
                try {
                    return@getOrPut Typeface.createFromAsset(context.assets, "fonts/$candidate")
                } catch (_: Exception) {}
            }
            Typeface.DEFAULT_BOLD
        }
    }

    /**
     * Decodes a photo safely with inSampleSize + EXIF rotation baked in and saves it as a clean JPEG.
     * Prevents 48MP OOMs, fixes sideways EXIF-rotated phone photos, and guarantees FFmpeg image2 compatibility.
     */
    private fun prepareImageForFfmpeg(
        context: Context,
        uri: Uri,
        maxDim: Int,
        tempFiles: MutableList<File>
    ): File? {
        var rawCopy: File? = null
        var decoded: Bitmap? = null
        var oriented: Bitmap? = null
        return try {
            // Copy to a temp file first so ExifInterface & BitmapFactory can both read reliably
            val srcFile = if (uri.scheme == "file" && !uri.path.isNullOrBlank() && File(uri.path!!).exists()) {
                File(uri.path!!)
            } else {
                val tmp = File(context.cacheDir, "fcp_input_raw_${UUID.randomUUID().toString().take(8)}.img")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tmp).use { output -> input.copyTo(output) }
                }
                if (!tmp.exists() || tmp.length() == 0L) {
                    tmp.delete()
                    return null
                }
                rawCopy = tmp
                tmp
            }

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(srcFile.absolutePath, boundsOpts)
            val srcW = boundsOpts.outWidth
            val srcH = boundsOpts.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            var inSample = 1
            val targetCap = maxDim.coerceIn(720, 2560)
            while ((srcW / (inSample * 2)) >= targetCap || (srcH / (inSample * 2)) >= targetCap) {
                inSample *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = inSample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            decoded = BitmapFactory.decodeFile(srcFile.absolutePath, decodeOpts) ?: return null

            val orientation = try {
                ExifInterface(srcFile.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } catch (_: Throwable) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            }

            // Also cap longest edge if still slightly above targetCap
            val curMax = max(decoded.width, decoded.height)
            if (curMax > targetCap) {
                val s = targetCap.toFloat() / curMax.toFloat()
                matrix.postScale(s, s)
            }

            oriented = if (!matrix.isIdentity) {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            } else {
                decoded
            }

            val outFile = File(context.cacheDir, "fcp_input_img_${UUID.randomUUID().toString().take(8)}.jpg")
            FileOutputStream(outFile).use { out ->
                oriented.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (outFile.exists() && outFile.length() > 0L) {
                tempFiles.add(outFile)
                outFile
            } else {
                outFile.delete()
                null
            }
        } catch (t: Throwable) {
            Log.e(TAG, "prepareImageForFfmpeg failed for $uri", t)
            null
        } finally {
            if (oriented != null && oriented !== decoded) {
                try { oriented.recycle() } catch (_: Throwable) {}
            }
            try { decoded?.recycle() } catch (_: Throwable) {}
            try { rawCopy?.delete() } catch (_: Throwable) {}
        }
    }

    /**
     * Renders an FcpCanvasBgType.IMAGE background (either custom user image or studio gradient backdrop)
     * with backdropDim applied into an exact (width x height) JPEG for FFmpeg.
     */
    private fun prepareCanvasBackdropImage(
        context: Context,
        canvasBackground: FcpCanvasBackground,
        width: Int,
        height: Int,
        tempFiles: MutableList<File>
    ): File? {
        var canvasBmp: Bitmap? = null
        var srcBmp: Bitmap? = null
        return try {
            canvasBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(canvasBmp)

            val customUri = canvasBackground.customImageUri
            if (customUri != null) {
                val prepared = prepareImageForFfmpeg(context, customUri, max(width, height), tempFiles)
                if (prepared != null) {
                    srcBmp = BitmapFactory.decodeFile(prepared.absolutePath)
                }
            }

            if (srcBmp != null) {
                // Center-crop fill into (width x height)
                val scale = max(width.toFloat() / srcBmp.width, height.toFloat() / srcBmp.height)
                val drawW = srcBmp.width * scale
                val drawH = srcBmp.height * scale
                val left = (width - drawW) / 2f
                val top = (height - drawH) / 2f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(srcBmp, null, RectF(left, top, left + drawW, top + drawH), paint)
            } else {
                // Render matching studio backdrop gradient
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                when (canvasBackground.studioBackdropId) {
                    "cyber" -> {
                        paint.shader = LinearGradient(
                            0f, 0f, 0f, height.toFloat(),
                            intArrayOf(0xFF3B0764.toInt(), 0xFF1E1B4B.toInt(), 0xFF09090B.toInt()),
                            null, Shader.TileMode.CLAMP
                        )
                    }
                    "sunset" -> {
                        paint.shader = LinearGradient(
                            0f, 0f, 0f, height.toFloat(),
                            intArrayOf(0xFF7C2D12.toInt(), 0xFF4C0519.toInt(), 0xFF0A0A0A.toInt()),
                            null, Shader.TileMode.CLAMP
                        )
                    }
                    "ocean" -> {
                        paint.shader = LinearGradient(
                            0f, 0f, 0f, height.toFloat(),
                            intArrayOf(0xFF083344.toInt(), 0xFF022C22.toInt(), 0xFF020617.toInt()),
                            null, Shader.TileMode.CLAMP
                        )
                    }
                    "velvet" -> {
                        paint.shader = LinearGradient(
                            0f, 0f, 0f, height.toFloat(),
                            intArrayOf(0xFF4A044E.toInt(), 0xFF18181B.toInt(), 0xFF000000.toInt()),
                            null, Shader.TileMode.CLAMP
                        )
                    }
                    "slate" -> {
                        paint.shader = LinearGradient(
                            0f, 0f, 0f, height.toFloat(),
                            intArrayOf(0xFF334155.toInt(), 0xFF1E293B.toInt(), 0xFF0F172A.toInt()),
                            null, Shader.TileMode.CLAMP
                        )
                    }
                    else -> { // obsidian
                        paint.shader = RadialGradient(
                            width / 2f, height / 2f, max(width, height) * 0.7f,
                            intArrayOf(0xFF2E2E38.toInt(), 0xFF14141A.toInt(), 0xFF000000.toInt()),
                            null, Shader.TileMode.CLAMP
                        )
                    }
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            if (canvasBackground.backdropDim > 0.01f) {
                val dimAlpha = (canvasBackground.backdropDim.coerceIn(0f, 0.85f) * 255f).roundToInt()
                canvas.drawColor(AndroidColor.argb(dimAlpha, 0, 0, 0))
            }

            val outFile = File(context.cacheDir, "fcp_bg_${UUID.randomUUID().toString().take(8)}.jpg")
            FileOutputStream(outFile).use { out ->
                canvasBmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            if (outFile.exists() && outFile.length() > 0L) {
                tempFiles.add(outFile)
                outFile
            } else {
                outFile.delete()
                null
            }
        } catch (t: Throwable) {
            Log.e(TAG, "prepareCanvasBackdropImage failed", t)
            null
        } finally {
            try { srcBmp?.recycle() } catch (_: Throwable) {}
            try { canvasBmp?.recycle() } catch (_: Throwable) {}
        }
    }

    /** Renders all visible title & text layers onto a single transparent bitmap (public helper). */
    fun renderTextLayersToBitmap(
        context: Context,
        textLayers: List<FcpTextLayer>,
        width: Int,
        height: Int
    ): Bitmap? {
        val visibleLayers = textLayers.filter {
            it.isVisible && (it.primaryText.isNotBlank() || it.scriptText.isNotBlank())
        }
        if (visibleLayers.isEmpty()) return null
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            visibleLayers.forEach { layer ->
                drawTextLayerOnCanvas(context, canvas, layer, width, height)
            }
            bitmap
        } catch (t: Throwable) {
            Log.e(TAG, "renderTextLayersToBitmap failed or out of memory", t)
            null
        }
    }

    private fun renderSingleTextLayerToPng(
        context: Context,
        layer: FcpTextLayer,
        width: Int,
        height: Int,
        tempFiles: MutableList<File>
    ): File? {
        var bitmap: Bitmap? = null
        return try {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawTextLayerOnCanvas(context, canvas, layer, width, height)
            val f = File(context.cacheDir, "fcp_text_${UUID.randomUUID().toString().take(8)}.png")
            FileOutputStream(f).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (f.exists() && f.length() > 0L) {
                tempFiles.add(f)
                f
            } else {
                f.delete()
                null
            }
        } catch (t: Throwable) {
            Log.e(TAG, "renderSingleTextLayerToPng failed", t)
            null
        } finally {
            try { bitmap?.recycle() } catch (_: Throwable) {}
        }
    }

    private fun drawTextLayerOnCanvas(
        context: Context,
        canvas: Canvas,
        layer: FcpTextLayer,
        width: Int,
        height: Int
    ) {
        val scale = width / 360f
        canvas.save()
        val cx = width / 2f + (layer.offsetX * scale)
        val cy = height / 2f + (layer.offsetY * scale)
        canvas.translate(cx, cy)
        canvas.rotate(layer.rotationDeg)

        val alphaInt = ((layer.opacity.coerceIn(0f, 1f)) * 255).toInt()
        val primaryTf = getTypeface(context, layer.primaryFontAsset)
        val scriptTf = getTypeface(context, layer.scriptFontAsset)

        val primarySizePx = layer.fontSizeSp * scale * 0.95f
        val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryTf
            textSize = primarySizePx
            textAlign = Paint.Align.CENTER
            alpha = alphaInt
            if (layer.hasDropShadow) {
                setShadowLayer(
                    6f * scale, 2f * scale, 3f * scale,
                    AndroidColor.argb((alphaInt * 0.85f).toInt(), 0, 0, 0)
                )
            }
            if (layer.fontSizeSp > 0f) {
                letterSpacing = (layer.letterSpacingSp / layer.fontSizeSp).coerceIn(0f, 0.5f)
            }
        }

        if (layer.isGradientText) {
            val bounds = Rect()
            primaryPaint.getTextBounds(layer.primaryText, 0, layer.primaryText.length, bounds)
            val halfW = max(bounds.width() / 2f, 1f)
            primaryPaint.shader = LinearGradient(
                -halfW, 0f, halfW, 0f,
                layer.textColor.toArgb(),
                layer.secondaryTextColor.toArgb(),
                Shader.TileMode.CLAMP
            )
        } else {
            primaryPaint.color = layer.textColor.toArgb()
            primaryPaint.alpha = alphaInt
        }

        val scriptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = scriptTf
            textSize = primarySizePx * 1.05f
            textAlign = Paint.Align.CENTER
            color = layer.accentColor.toArgb()
            alpha = alphaInt
            if (layer.hasDropShadow) {
                setShadowLayer(
                    5f * scale, 2f * scale, 2f * scale,
                    AndroidColor.argb((alphaInt * 0.85f).toInt(), 0, 0, 0)
                )
            }
        }

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryTf
            textSize = (layer.fontSizeSp * 0.32f).coerceAtLeast(9f) * scale
            textAlign = Paint.Align.CENTER
            color = AndroidColor.WHITE
            alpha = (alphaInt * 0.85f).toInt()
            letterSpacing = ((layer.letterSpacingSp * 2f + 1f) / 12f).coerceIn(0f, 0.4f)
        }

        if (layer.hasBackgroundPill) {
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.argb((alphaInt * 0.55f).toInt(), 0, 0, 0)
                style = Paint.Style.FILL
            }
            val pillStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.argb((alphaInt * 0.25f).toInt(), 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * scale
            }
            val bounds = Rect()
            primaryPaint.getTextBounds(layer.primaryText, 0, layer.primaryText.length, bounds)
            val padX = 24f * scale
            val padY = 16f * scale
            val halfW = (bounds.width() / 2f) + padX
            val halfH = (bounds.height() / 2f) + padY
            val rect = RectF(-halfW, -halfH, halfW, halfH)
            val corner = 14f * scale
            canvas.drawRoundRect(rect, corner, corner, pillPaint)
            canvas.drawRoundRect(rect, corner, corner, pillStroke)
        }

        val fm = primaryPaint.fontMetrics
        val baseLineY = -(fm.ascent + fm.descent) / 2f

        when (layer.presetType) {
            FcpTitlePresetType.SCRIPT_HEADLINE, FcpTitlePresetType.DESIGN_CONTRAST -> {
                canvas.drawText(layer.primaryText, 0f, baseLineY, primaryPaint)
                if (layer.scriptText.isNotBlank()) {
                    canvas.save()
                    val pBounds = Rect()
                    primaryPaint.getTextBounds(layer.primaryText, 0, layer.primaryText.length, pBounds)
                    val shiftX = if (layer.presetType == FcpTitlePresetType.SCRIPT_HEADLINE) {
                        -pBounds.width() * 0.22f - (8f * scale)
                    } else 0f
                    canvas.translate(shiftX, baseLineY - (12f * scale))
                    canvas.rotate(-7f)
                    canvas.drawText(layer.scriptText, 0f, 0f, scriptPaint)
                    canvas.restore()
                }
                if (layer.subtitleText.isNotBlank()) {
                    canvas.drawText(layer.subtitleText, 0f, baseLineY + primarySizePx * 0.55f, subPaint)
                }
            }

            FcpTitlePresetType.GOLD_3D -> {
                if (layer.badgeText.isNotBlank()) {
                    val badgeText = layer.badgeText.uppercase(Locale.US)
                    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = primaryTf
                        textSize = 9f * scale
                        textAlign = Paint.Align.CENTER
                        color = layer.accentColor.toArgb()
                        alpha = alphaInt
                    }
                    val badgePillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = AndroidColor.argb((alphaInt * 0.7f).toInt(), 0, 0, 0)
                        style = Paint.Style.FILL
                    }
                    val badgeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.accentColor.toArgb()
                        alpha = (alphaInt * 0.7f).toInt()
                        style = Paint.Style.STROKE
                        strokeWidth = 1f * scale
                    }
                    val badgeBounds = Rect()
                    badgePaint.getTextBounds(badgeText, 0, badgeText.length, badgeBounds)
                    val bw = badgeBounds.width() / 2f + 10f * scale
                    val bh = 8f * scale
                    val badgeRect = RectF(-bw, baseLineY - primarySizePx - bh * 2, bw, baseLineY - primarySizePx)
                    canvas.drawRoundRect(badgeRect, 50f, 50f, badgePillPaint)
                    canvas.drawRoundRect(badgeRect, 50f, 50f, badgeStroke)
                    canvas.drawText(badgeText, 0f, badgeRect.centerY() + 3f * scale, badgePaint)
                }
                val shadow1 = Paint(primaryPaint).apply {
                    color = 0xFF261204.toInt(); alpha = alphaInt; clearShadowLayer(); shader = null
                }
                canvas.drawText(layer.primaryText, 3f * scale, baseLineY + 3f * scale, shadow1)
                val shadow2 = Paint(primaryPaint).apply {
                    color = 0xFF78350F.toInt(); alpha = alphaInt; clearShadowLayer(); shader = null
                }
                canvas.drawText(layer.primaryText, 1.5f * scale, baseLineY + 1.5f * scale, shadow2)
                val goldPaint = Paint(primaryPaint).apply {
                    if (!layer.isGradientText) {
                        shader = LinearGradient(
                            0f, baseLineY - primarySizePx, 0f, baseLineY,
                            intArrayOf(0xFFFFF9C4.toInt(), layer.accentColor.toArgb(), 0xFFF59E0B.toInt(), 0xFFB45309.toInt()),
                            null, Shader.TileMode.CLAMP
                        )
                    }
                }
                canvas.drawText(layer.primaryText, 0f, baseLineY, goldPaint)
                if (layer.scriptText.isNotBlank()) {
                    canvas.save()
                    canvas.rotate(-9f)
                    val sPaint = Paint(scriptPaint).apply { color = AndroidColor.WHITE; alpha = alphaInt }
                    canvas.drawText(layer.scriptText, 0f, baseLineY + 2f * scale, sPaint)
                    canvas.restore()
                }
                if (layer.subtitleText.isNotBlank()) {
                    canvas.drawText(layer.subtitleText, 0f, baseLineY + primarySizePx * 0.55f, subPaint)
                }
            }

            FcpTitlePresetType.MODERN_FOLD -> {
                val upText = layer.primaryText.uppercase(Locale.US)
                val foldShadow = Paint(primaryPaint).apply {
                    color = AndroidColor.argb((alphaInt * 0.45f).toInt(), 0, 0, 0)
                    clearShadowLayer(); shader = null
                }
                canvas.drawText(upText, 4f * scale, baseLineY + 5f * scale, foldShadow)
                canvas.drawText(upText, 0f, baseLineY, primaryPaint)
                if (layer.subtitleText.isNotBlank()) {
                    canvas.drawText(layer.subtitleText.uppercase(Locale.US), 0f, baseLineY + primarySizePx * 0.55f, subPaint)
                }
            }

            FcpTitlePresetType.BOXED_FRAME -> {
                val upText = layer.primaryText.uppercase(Locale.US)
                val pBounds = Rect()
                primaryPaint.getTextBounds(upText, 0, upText.length, pBounds)
                val halfW = (pBounds.width() / 2f) + 20f * scale
                val halfH = (pBounds.height() / 2f) + 12f * scale
                val frameRect = RectF(-halfW, baseLineY - halfH * 1.2f, halfW, baseLineY + halfH * 0.8f)
                val boxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb((alphaInt * 0.35f).toInt(), 0, 0, 0); style = Paint.Style.FILL
                }
                val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.accentColor.toArgb(); alpha = alphaInt; style = Paint.Style.STROKE; strokeWidth = 2f * scale
                }
                canvas.drawRoundRect(frameRect, 4f * scale, 4f * scale, boxBg)
                canvas.drawRoundRect(frameRect, 4f * scale, 4f * scale, boxStroke)
                canvas.drawText(upText, 0f, baseLineY, primaryPaint)
                if (layer.subtitleText.isNotBlank()) {
                    val subUp = layer.subtitleText.uppercase(Locale.US)
                    val subY = frameRect.bottom + 16f * scale
                    canvas.drawText(subUp, 0f, subY, subPaint)
                    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.accentColor.toArgb(); alpha = (alphaInt * 0.7f).toInt(); strokeWidth = 1.2f * scale
                    }
                    val subBounds = Rect()
                    subPaint.getTextBounds(subUp, 0, subUp.length, subBounds)
                    val subHalfW = subBounds.width() / 2f
                    canvas.drawLine(-subHalfW - 24f * scale, subY - 4f * scale, -subHalfW - 8f * scale, subY - 4f * scale, linePaint)
                    canvas.drawLine(subHalfW + 8f * scale, subY - 4f * scale, subHalfW + 24f * scale, subY - 4f * scale, linePaint)
                }
            }

            FcpTitlePresetType.ABSTRACT_BADGE -> {
                val upText = layer.primaryText.uppercase(Locale.US)
                val pBounds = Rect()
                primaryPaint.getTextBounds(upText, 0, upText.length, pBounds)
                val halfW = (pBounds.width() / 2f) + 22f * scale
                val halfH = (pBounds.height() / 2f) + 12f * scale
                val badgeRect = RectF(-halfW, baseLineY - halfH * 1.2f, halfW, baseLineY + halfH * 0.8f)
                val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.accentColor.toArgb(); alpha = alphaInt; style = Paint.Style.FILL
                    setShadowLayer(8f * scale, 0f, 4f * scale, AndroidColor.argb((alphaInt * 0.6f).toInt(), 0, 0, 0))
                }
                canvas.drawRoundRect(badgeRect, 8f * scale, 8f * scale, badgeBg)
                canvas.drawText(upText, 0f, baseLineY, primaryPaint)
                if (layer.subtitleText.isNotBlank()) {
                    canvas.drawText(layer.subtitleText.uppercase(Locale.US), 0f, badgeRect.bottom + 16f * scale, subPaint)
                }
            }

            FcpTitlePresetType.CYBER_NEON -> {
                val upText = layer.primaryText.uppercase(Locale.US)
                val pBounds = Rect()
                primaryPaint.getTextBounds(upText, 0, upText.length, pBounds)
                val halfW = (pBounds.width() / 2f) + 20f * scale
                val halfH = (pBounds.height() / 2f) + 12f * scale
                val neonRect = RectF(-halfW, baseLineY - halfH * 1.2f, halfW, baseLineY + halfH * 0.8f)
                val neonBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb((alphaInt * 0.85f).toInt(), 0x0F, 0x17, 0x2A); style = Paint.Style.FILL
                }
                val neonStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(-halfW, 0f, halfW, 0f, layer.textColor.toArgb(), layer.accentColor.toArgb(), Shader.TileMode.CLAMP)
                    alpha = alphaInt; style = Paint.Style.STROKE; strokeWidth = 2f * scale
                }
                canvas.drawRoundRect(neonRect, 12f * scale, 12f * scale, neonBg)
                canvas.drawRoundRect(neonRect, 12f * scale, 12f * scale, neonStroke)
                val glowPaint = Paint(primaryPaint).apply { setShadowLayer(14f * scale, 0f, 0f, layer.textColor.toArgb()) }
                canvas.drawText(upText, 0f, baseLineY, glowPaint)
                if (layer.subtitleText.isNotBlank()) {
                    canvas.drawText(layer.subtitleText.uppercase(Locale.US), 0f, neonRect.bottom - 4f * scale, subPaint)
                }
            }

            FcpTitlePresetType.MINIMAL_CLEAN -> {
                val cleanPaint = Paint(primaryPaint).apply { textSize = primarySizePx * 0.8f; textAlign = Paint.Align.LEFT }
                val cleanSubPaint = Paint(subPaint).apply { textAlign = Paint.Align.LEFT }
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.accentColor.toArgb(); alpha = alphaInt; style = Paint.Style.FILL
                }
                val pBounds = Rect()
                cleanPaint.getTextBounds(layer.primaryText, 0, layer.primaryText.length, pBounds)
                val totalW = pBounds.width() + 20f * scale
                val startX = -totalW / 2f
                val barRect = RectF(startX, baseLineY - primarySizePx * 0.7f, startX + 4f * scale, baseLineY + 6f * scale)
                canvas.drawRoundRect(barRect, 2f * scale, 2f * scale, barPaint)
                canvas.drawText(layer.primaryText, startX + 12f * scale, baseLineY - 2f * scale, cleanPaint)
                if (layer.subtitleText.isNotBlank()) {
                    canvas.drawText(layer.subtitleText, startX + 12f * scale, baseLineY + primarySizePx * 0.42f, cleanSubPaint)
                }
            }

            FcpTitlePresetType.RETRO_VHS -> {
                val upText = layer.primaryText.uppercase(Locale.US)
                val cyanShift = Paint(primaryPaint).apply { color = 0xFF00F0FF.toInt(); alpha = (alphaInt * 0.75f).toInt(); shader = null; clearShadowLayer() }
                val magShift = Paint(primaryPaint).apply { color = 0xFFFF0080.toInt(); alpha = (alphaInt * 0.75f).toInt(); shader = null; clearShadowLayer() }
                canvas.drawText(upText, -2.5f * scale, baseLineY, cyanShift)
                canvas.drawText(upText, 2.5f * scale, baseLineY, magShift)
                canvas.drawText(upText, 0f, baseLineY, primaryPaint)
                if (layer.subtitleText.isNotBlank()) {
                    canvas.drawText(layer.subtitleText.uppercase(Locale.US), 0f, baseLineY + primarySizePx * 0.55f, subPaint)
                }
            }

            FcpTitlePresetType.LUXURY_SERIF -> {
                val serifPaint = Paint(primaryPaint).apply {
                    letterSpacing = max(primaryPaint.letterSpacing, 0.16f)
                }
                canvas.drawText(layer.primaryText.uppercase(Locale.US), 0f, baseLineY, serifPaint)
                val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.accentColor.toArgb(); alpha = (alphaInt * 0.8f).toInt(); strokeWidth = 1.2f * scale
                }
                canvas.drawLine(-48f * scale, baseLineY + 12f * scale, 48f * scale, baseLineY + 12f * scale, rulePaint)
                if (layer.subtitleText.isNotBlank()) {
                    canvas.drawText(layer.subtitleText.uppercase(Locale.US), 0f, baseLineY + primarySizePx * 0.6f, subPaint)
                }
            }

            FcpTitlePresetType.KINETIC_PUNCH -> {
                val upText = layer.primaryText.uppercase(Locale.US)
                val strokePaint = Paint(primaryPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4f * scale
                    color = AndroidColor.BLACK
                    alpha = alphaInt
                    shader = null
                    clearShadowLayer()
                }
                canvas.drawText(upText, 0f, baseLineY, strokePaint)
                canvas.drawText(upText, 0f, baseLineY, primaryPaint)
                if (layer.subtitleText.isNotBlank()) {
                    canvas.drawText(layer.subtitleText.uppercase(Locale.US), 0f, baseLineY + primarySizePx * 0.55f, subPaint)
                }
            }

            FcpTitlePresetType.SUBTITLE_CARD -> {
                val pBounds = Rect()
                primaryPaint.getTextBounds(layer.primaryText, 0, layer.primaryText.length, pBounds)
                val halfW = (pBounds.width() / 2f) + 20f * scale
                val halfH = (pBounds.height() / 2f) + 12f * scale
                val cardRect = RectF(-halfW, baseLineY - halfH * 1.15f, halfW, baseLineY + halfH * 0.85f)
                val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.argb((alphaInt * 0.68f).toInt(), 12, 14, 20); style = Paint.Style.FILL
                }
                canvas.drawRoundRect(cardRect, 14f * scale, 14f * scale, cardBg)
                canvas.drawText(layer.primaryText, 0f, baseLineY, primaryPaint)
            }
        }

        canvas.restore()
    }

    private data class ClipSpec(
        val clip: FcpClip,
        val file: File,
        val isImage: Boolean,
        val hasAudio: Boolean,
        val sTime: Float,
        val eTime: Float,
        val speed: Float,
        val effectiveDur: Float
    )

    private data class TextOverlaySpec(
        val layer: FcpTextLayer,
        val file: File,
        val startSec: Float,
        val endSec: Float,
        val needsLoopStream: Boolean
    )

    suspend fun exportTimeline(
        context: Context,
        clips: List<FcpClip>,
        customAudio: FcpCustomAudio?,
        textLayers: List<FcpTextLayer> = emptyList(),
        canvasBackground: FcpCanvasBackground = FcpCanvasBackground(),
        aspectRatio: FcpAspectRatio,
        res: String,
        fps: String,
        previewWidthPx: Int = 0,
        previewHeightPx: Int = 0,
        onProgress: (Float, String) -> Unit
    ): File? = exportMutex.withLock {
        withContext(Dispatchers.IO) {
            isCancelled = false
            if (clips.isEmpty()) {
                Log.e(TAG, "exportTimeline: clips list is empty")
                return@withContext null
            }

            val outputFile = File(context.cacheDir, "pro_export_${System.currentTimeMillis()}.mp4")
            val tempFiles = mutableListOf<File>()
            val localFileCache = mutableMapOf<String, File>()

            var highWaterProgress = 0.05f
            fun reportProgress(p: Float, stage: String) {
                val monotonic = max(highWaterProgress, p.coerceIn(0f, 0.99f))
                highWaterProgress = monotonic
                onProgress(monotonic, stage)
            }

            try {
                if (isCancelled || !isActive) return@withContext null
                reportProgress(0.08f, "Analyzing timeline & ${clips.size} clip(s)...")

                // 1. Target dimensions (always even) & FPS
                val baseDim = when (res.lowercase(Locale.US)) {
                    "720p" -> 720
                    "4k" -> 2160
                    else -> 1080
                }
                val (targetW, targetH) = when (aspectRatio) {
                    FcpAspectRatio.VERTICAL_9_16 -> Pair(baseDim, ((baseDim * 16f) / 9f).toInt())
                    FcpAspectRatio.WIDESCREEN_16_9 -> Pair(((baseDim * 16f) / 9f).toInt(), baseDim)
                    FcpAspectRatio.SQUARE_1_1 -> Pair(baseDim, baseDim)
                    FcpAspectRatio.CINEMATIC_21_9 -> Pair(((baseDim * 21f) / 9f).toInt(), baseDim)
                }
                val finalW = (targetW / 2) * 2
                val finalH = (targetH / 2) * 2
                val targetFps = when {
                    fps.contains("60") -> 60
                    fps.contains("24") -> 24
                    else -> 30
                }

                // 2. Resolve inputs cleanly (preserving audio/video extensions & EXIF-normalizing photos)
                reportProgress(0.12f, "Preparing media inputs...")

                fun resolveExtension(uri: Uri, isAudio: Boolean): String {
                    val mime = try { context.contentResolver.getType(uri) } catch (_: Throwable) { null }
                    if (!mime.isNullOrBlank()) {
                        val mapped = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                        if (!mapped.isNullOrBlank()) return ".$mapped"
                    }
                    val pathExt = (uri.lastPathSegment ?: uri.path ?: "")
                        .substringAfterLast('.', "")
                        .substringBefore('?')
                        .lowercase(Locale.US)
                    if (pathExt.length in 2..5 && pathExt.all { it.isLetterOrDigit() }) {
                        return ".$pathExt"
                    }
                    return if (isAudio) ".mp3" else ".mp4"
                }

                fun getLocalMediaFile(uri: Uri, isAudio: Boolean = false): File? {
                    val key = "${if (isAudio) "a" else "v"}:$uri"
                    localFileCache[key]?.let { return it }

                    if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                        val f = File(uri.path!!)
                        if (f.exists() && f.length() > 0L) {
                            localFileCache[key] = f
                            return f
                        }
                    }

                    val ext = resolveExtension(uri, isAudio)
                    val tempFile = File(context.cacheDir, "fcp_input_${UUID.randomUUID().toString().take(8)}$ext")
                    return try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                        }
                        if (tempFile.exists() && tempFile.length() > 0L) {
                            localFileCache[key] = tempFile
                            tempFiles.add(tempFile)
                            tempFile
                        } else {
                            tempFile.delete()
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to cache uri $uri", e)
                        tempFile.delete()
                        null
                    }
                }

                val clipSpecs = mutableListOf<ClipSpec>()
                val maxPhotoDim = DevicePerformanceManager.maxVideoPhotoInputDim(context, finalW, finalH)

                for ((idx, clip) in clips.withIndex()) {
                    if (isCancelled || !isActive) return@withContext null
                    val isImage = isImageClip(context, clip)
                    val file = if (isImage) {
                        val imgKey = "img:${clip.uri}"
                        localFileCache[imgKey] ?: prepareImageForFfmpeg(context, clip.uri, maxPhotoDim, tempFiles)?.also {
                            localFileCache[imgKey] = it
                        }
                    } else {
                        getLocalMediaFile(clip.uri, isAudio = false)
                    }

                    if (file == null || !file.exists() || file.length() == 0L) {
                        Log.e(TAG, "Clip $idx file could not be resolved: ${clip.uri}")
                        return@withContext null
                    }

                    var hasAudio = false
                    var probedDur = 0f
                    if (!isImage) {
                        val extractor = MediaExtractor()
                        try {
                            extractor.setDataSource(file.absolutePath)
                            for (t in 0 until extractor.trackCount) {
                                val mime = extractor.getTrackFormat(t).getString(MediaFormat.KEY_MIME) ?: ""
                                if (mime.startsWith("audio/")) {
                                    hasAudio = true
                                    break
                                }
                            }
                        } catch (_: Throwable) {
                        } finally {
                            try { extractor.release() } catch (_: Throwable) {}
                        }

                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(file.absolutePath)
                            val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                            if (ms > 0L) probedDur = ms / 1000f
                        } catch (_: Throwable) {
                        } finally {
                            try { retriever.release() } catch (_: Throwable) {}
                        }
                    }

                    val speed = clip.speed.coerceIn(0.25f, 4.0f)
                    val sTime: Float
                    val eTime: Float
                    val effectiveDur: Float
                    if (isImage) {
                        sTime = 0f
                        effectiveDur = clip.trimmedDurationSec.coerceAtLeast(0.2f)
                        eTime = effectiveDur
                    } else {
                        val maxSourceDur = when {
                            probedDur > 0.1f -> probedDur
                            clip.durationSec > 0.1f -> clip.durationSec
                            clip.trimEndSec > 0.1f -> clip.trimEndSec
                            else -> 10f
                        }
                        sTime = clip.trimStartSec.coerceIn(0f, (maxSourceDur - 0.1f).coerceAtLeast(0f))
                        val eRaw = if (clip.trimEndSec > sTime + 0.1f) clip.trimEndSec else maxSourceDur
                        eTime = eRaw.coerceIn(sTime + 0.1f, maxSourceDur)
                        effectiveDur = ((eTime - sTime) / speed).coerceAtLeast(0.2f)
                    }

                    clipSpecs.add(
                        ClipSpec(
                            clip = clip,
                            file = file,
                            isImage = isImage,
                            hasAudio = hasAudio,
                            sTime = sTime,
                            eTime = eTime,
                            speed = speed,
                            effectiveDur = effectiveDur
                        )
                    )
                }

                val customAudioFile = customAudio?.let { getLocalMediaFile(it.uri, isAudio = true) }
                val totalTimelineDurationSec = clipSpecs.sumOf { it.effectiveDur.toDouble() }.toFloat().coerceAtLeast(0.5f)
                val totalTimelineDurationUs = (totalTimelineDurationSec * 1_000_000.0).toLong().coerceAtLeast(500_000L)

                // 3. Prepare canvas backdrop image if FcpCanvasBgType.IMAGE
                val canvasBackdropFile = if (canvasBackground.type == FcpCanvasBgType.IMAGE) {
                    prepareCanvasBackdropImage(context, canvasBackground, finalW, finalH, tempFiles)
                } else null

                // 4. Render per-layer text overlays
                reportProgress(0.18f, "Rendering title layers...")
                val visibleTextLayers = textLayers.filter {
                    it.isVisible && (it.primaryText.isNotBlank() || it.scriptText.isNotBlank())
                }
                val textOverlaySpecs = visibleTextLayers.mapNotNull { layer ->
                    val png = renderSingleTextLayerToPng(context, layer, finalW, finalH, tempFiles)
                        ?: return@mapNotNull null
                    val st = layer.timelineStartSec.coerceIn(0f, totalTimelineDurationSec)
                    val en = if (layer.timelineEndSec > st + 0.05f) {
                        layer.timelineEndSec.coerceAtMost(totalTimelineDurationSec)
                    } else {
                        totalTimelineDurationSec
                    }
                    val needsFadeLoop = (layer.inAnimation == TextInAnimation.FADE_IN ||
                        layer.outAnimation == TextOutAnimation.FADE_OUT ||
                        layer.outAnimation == TextOutAnimation.BLUR_OUT)
                    TextOverlaySpec(
                        layer = layer,
                        file = png,
                        startSec = st,
                        endSec = en,
                        needsLoopStream = needsFadeLoop
                    )
                }

                // 5. Build full-fidelity or compatibility FFmpeg command
                val refPreviewW = if (previewWidthPx > 0) previewWidthPx.toFloat() else (360f * context.resources.displayMetrics.density)
                val refPreviewH = if (previewHeightPx > 0) previewHeightPx.toFloat() else (refPreviewW / aspectRatio.ratio)

                fun buildCommand(
                    compatibilityMode: Boolean,
                    preset: String,
                    crf: String
                ): Array<String> {
                    val cmdArgs = mutableListOf<String>()
                    cmdArgs.add("ffmpeg")
                    cmdArgs.add("-y")
                    val numCores = DevicePerformanceManager.ffmpegThreads(context)
                    cmdArgs.add("-threads")
                    cmdArgs.add(numCores.toString())
                    cmdArgs.add("-filter_threads")
                    cmdArgs.add(numCores.toString())

                    // Input indices
                    var nextInputIdx = 0
                    val clipInputIndices = IntArray(clipSpecs.size)
                    val bgInputIndices = IntArray(clipSpecs.size) { -1 }

                    for (i in clipSpecs.indices) {
                        val spec = clipSpecs[i]
                        if (spec.isImage) {
                            cmdArgs.add("-loop"); cmdArgs.add("1")
                            cmdArgs.add("-framerate"); cmdArgs.add(targetFps.toString())
                            cmdArgs.add("-t"); cmdArgs.add(String.format(Locale.US, "%.3f", spec.effectiveDur))
                        }
                        cmdArgs.add("-i")
                        cmdArgs.add(spec.file.absolutePath)
                        clipInputIndices[i] = nextInputIdx++

                        if (!compatibilityMode && canvasBackground.type == FcpCanvasBgType.IMAGE && canvasBackdropFile != null) {
                            cmdArgs.add("-loop"); cmdArgs.add("1")
                            cmdArgs.add("-framerate"); cmdArgs.add(targetFps.toString())
                            cmdArgs.add("-t"); cmdArgs.add(String.format(Locale.US, "%.3f", spec.effectiveDur))
                            cmdArgs.add("-i")
                            cmdArgs.add(canvasBackdropFile.absolutePath)
                            bgInputIndices[i] = nextInputIdx++
                        }
                    }

                    val customAudioInputIndex = if (customAudioFile != null && customAudio != null) {
                        cmdArgs.add("-i")
                        cmdArgs.add(customAudioFile.absolutePath)
                        nextInputIdx++
                    } else null

                    val textInputIndices = IntArray(textOverlaySpecs.size) { -1 }
                    for (tIdx in textOverlaySpecs.indices) {
                        val tSpec = textOverlaySpecs[tIdx]
                        if (!compatibilityMode && tSpec.needsLoopStream) {
                            cmdArgs.add("-loop"); cmdArgs.add("1")
                            cmdArgs.add("-framerate"); cmdArgs.add(targetFps.toString())
                            cmdArgs.add("-t"); cmdArgs.add(String.format(Locale.US, "%.3f", tSpec.endSec))
                        }
                        cmdArgs.add("-i")
                        cmdArgs.add(tSpec.file.absolutePath)
                        textInputIndices[tIdx] = nextInputIdx++
                    }

                    val filterGraph = StringBuilder()
                    val concatVideoLabels = mutableListOf<String>()
                    val concatAudioLabels = mutableListOf<String>()
                    val needOriginalAudio = (customAudioInputIndex == null || customAudio == null || !customAudio.muteOriginalVideo)

                    val padColorHex = if (canvasBackground.type == FcpCanvasBgType.COLOR &&
                        canvasBackground.solidColor != androidx.compose.ui.graphics.Color.Black
                    ) {
                        val argb = canvasBackground.solidColor.toArgb()
                        String.format(Locale.US, "0x%02X%02X%02X", (argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
                    } else {
                        "black"
                    }

                    for (i in clipSpecs.indices) {
                        val spec = clipSpecs[i]
                        val clip = spec.clip
                        val inIdx = clipInputIndices[i]
                        val effectiveDur = spec.effectiveDur

                        // Stage A: Base clip source (trim, speed, mirror, rotate, fps)
                        val baseChain = StringBuilder()
                        if (!spec.isImage) {
                            val ptsScale = 1.0f / spec.speed
                            baseChain.append(
                                String.format(
                                    Locale.US,
                                    "trim=start=%.3f:end=%.3f,setpts=%.4f*(PTS-STARTPTS),",
                                    spec.sTime, spec.eTime, ptsScale
                                )
                            )
                        } else {
                            baseChain.append("setpts=PTS-STARTPTS,")
                        }

                        if (clip.isMirrored) baseChain.append("hflip,")
                        when (((clip.rotationAngle.toInt() % 360) + 360) % 360) {
                            90 -> baseChain.append("transpose=1,")
                            180 -> baseChain.append("transpose=2,transpose=2,")
                            270 -> baseChain.append("transpose=2,")
                            else -> {}
                        }
                        baseChain.append("setsar=1,fps=$targetFps")

                        // Stage B: Canvas composition (Blur / Image / Color + Zoom In/Out + Pan)
                        val clipScale = clip.scale.coerceIn(0.5f, 5.0f)
                        val hasCustomFraming = kotlin.math.abs(clipScale - 1.0f) > 0.02f ||
                            kotlin.math.abs(clip.panX) > 1f ||
                            kotlin.math.abs(clip.panY) > 1f
                        val panDx = ((clip.panX / refPreviewW) * finalW).roundToInt()
                        val panDy = ((clip.panY / refPreviewH) * finalH).roundToInt()

                        val composedLabel = "[v_comp_$i]"

                        if (!compatibilityMode && canvasBackground.type == FcpCanvasBgType.BLUR) {
                            filterGraph.append("[$inIdx:v]$baseChain,split=2[bg_src_$i][fg_src_$i];")
                            val blurW = ((finalW / 4) / 2 * 2).coerceAtLeast(64)
                            val blurH = ((finalH / 4) / 2 * 2).coerceAtLeast(64)
                            val blurRadius = (canvasBackground.blurRadius / 4f).roundToInt().coerceIn(2, 16)
                            val dimFilter = if (canvasBackground.backdropDim > 0.01f) {
                                String.format(Locale.US, ",eq=brightness=-%.2f", canvasBackground.backdropDim.coerceIn(0f, 0.7f))
                            } else ""
                            filterGraph.append(
                                "[bg_src_$i]scale=$blurW:$blurH:force_original_aspect_ratio=increase:flags=fast_bilinear," +
                                    "crop=$blurW:$blurH,boxblur=$blurRadius:1,scale=$finalW:$finalH:flags=bilinear$dimFilter[bg_blur_$i];"
                            )
                            appendForegroundFraming(
                                filterGraph = filterGraph,
                                srcLabel = "[fg_src_$i]",
                                dstLabel = "[fg_ready_$i]",
                                finalW = finalW,
                                finalH = finalH,
                                clipScale = clipScale,
                                panDx = panDx,
                                panDy = panDy
                            )
                            val ovX = if (clipScale <= 2.0f) "(W-w)/2+($panDx)" else "(W-w)/2"
                            val ovY = if (clipScale <= 2.0f) "(H-h)/2+($panDy)" else "(H-h)/2"
                            filterGraph.append("[bg_blur_$i][fg_ready_$i]overlay=x=$ovX:y=$ovY:shortest=1$composedLabel;")
                        } else if (!compatibilityMode && canvasBackground.type == FcpCanvasBgType.IMAGE && bgInputIndices[i] >= 0) {
                            val bgIdx = bgInputIndices[i]
                            filterGraph.append("[$inIdx:v]$baseChain[fg_src_$i];")
                            appendForegroundFraming(
                                filterGraph = filterGraph,
                                srcLabel = "[fg_src_$i]",
                                dstLabel = "[fg_ready_$i]",
                                finalW = finalW,
                                finalH = finalH,
                                clipScale = clipScale,
                                panDx = panDx,
                                panDy = panDy
                            )
                            val ovX = if (clipScale <= 2.0f) "(W-w)/2+($panDx)" else "(W-w)/2"
                            val ovY = if (clipScale <= 2.0f) "(H-h)/2+($panDy)" else "(H-h)/2"
                            filterGraph.append("[$bgIdx:v][fg_ready_$i]overlay=x=$ovX:y=$ovY:shortest=1$composedLabel;")
                        } else if (!compatibilityMode && hasCustomFraming) {
                            // Solid color backdrop with custom zoom-in, zoom-out, or pan framing
                            filterGraph.append(
                                "color=c=$padColorHex:s=${finalW}x${finalH}:r=$targetFps:d=${String.format(Locale.US, "%.3f", effectiveDur)}[bg_col_$i];"
                            )
                            filterGraph.append("[$inIdx:v]$baseChain[fg_src_$i];")
                            appendForegroundFraming(
                                filterGraph = filterGraph,
                                srcLabel = "[fg_src_$i]",
                                dstLabel = "[fg_ready_$i]",
                                finalW = finalW,
                                finalH = finalH,
                                clipScale = clipScale,
                                panDx = panDx,
                                panDy = panDy
                            )
                            val ovX = if (clipScale <= 2.0f) "(W-w)/2+($panDx)" else "(W-w)/2"
                            val ovY = if (clipScale <= 2.0f) "(H-h)/2+($panDy)" else "(H-h)/2"
                            filterGraph.append("[bg_col_$i][fg_ready_$i]overlay=x=$ovX:y=$ovY:shortest=1$composedLabel;")
                        } else {
                            // Fast single-pass letterbox scale + pad (also used in Compatibility Mode)
                            filterGraph.append(
                                "[$inIdx:v]$baseChain," +
                                    "scale=$finalW:$finalH:force_original_aspect_ratio=decrease:flags=bilinear," +
                                    "pad=$finalW:$finalH:(ow-iw)/2:(oh-ih)/2:$padColorHex$composedLabel;"
                            )
                        }

                        // Stage C: Post-composition Color, LUT, VFX, Motion Animations & Transitions
                        val postFilter = StringBuilder("setsar=1,format=yuv420p")

                        // Color adjustments
                        if (clip.exposure != 0f || clip.contrast != 100f || clip.saturation != 100f) {
                            val b = String.format(Locale.US, "%.3f", (clip.exposure / 100f).coerceIn(-0.9f, 0.9f))
                            val c = String.format(Locale.US, "%.3f", (clip.contrast / 100f).coerceIn(0.1f, 3.0f))
                            val s = String.format(Locale.US, "%.3f", (clip.saturation / 100f).coerceIn(0f, 3.0f))
                            postFilter.append(",eq=brightness=$b:contrast=$c:saturation=$s")
                        }

                        // White balance
                        if (clip.temperature != 0f || clip.tint != 0f) {
                            val tempNorm = (clip.temperature / 50f).coerceIn(-1f, 1f)
                            val tintNorm = (clip.tint / 50f).coerceIn(-1f, 1f)
                            val rBal = String.format(Locale.US, "%.3f", (tempNorm * 0.20f + tintNorm * 0.10f).coerceIn(-0.8f, 0.8f))
                            val gBal = String.format(Locale.US, "%.3f", (-tintNorm * 0.20f).coerceIn(-0.8f, 0.8f))
                            val bBal = String.format(Locale.US, "%.3f", (-tempNorm * 0.20f + tintNorm * 0.10f).coerceIn(-0.8f, 0.8f))
                            postFilter.append(",colorbalance=rs=$rBal:gs=$gBal:bs=$bBal:rm=$rBal:gm=$gBal:bm=$bBal")
                        }

                        if (!compatibilityMode) {
                            // Vignette & Sharpen
                            if (clip.vignette > 0.01f) {
                                val angle = String.format(Locale.US, "%.3f", (clip.vignette / 100f) * (Math.PI.toFloat() / 3f))
                                postFilter.append(",vignette=angle=$angle")
                            }
                            if (clip.sharpen > 0.01f) {
                                val lAmount = String.format(Locale.US, "%.2f", (clip.sharpen / 100f) * 1.5f)
                                postFilter.append(",unsharp=5:5:$lAmount:5:5:0.0")
                            }

                            // LUTs
                            when (clip.lut) {
                                FcpLutPreset.TEAL_ORANGE -> postFilter.append(",colorbalance=rs=0.15:gs=-0.02:bs=-0.12:rm=0.10:bm=-0.10")
                                FcpLutPreset.CYBER -> postFilter.append(",colorbalance=rm=0.15:gm=-0.05:bm=0.25:rh=0.10:bh=0.20")
                                FcpLutPreset.NOIR -> postFilter.append(",hue=s=0,eq=contrast=1.35:brightness=-0.02")
                                FcpLutPreset.WARM_GOLD -> postFilter.append(",colorbalance=rh=0.20:gh=0.10:bh=-0.15:rm=0.10:gm=0.05")
                                FcpLutPreset.COOL_NORDIC -> postFilter.append(",colorbalance=rs=-0.10:bs=0.20:rm=-0.05:bm=0.15")
                                FcpLutPreset.VINTAGE_FADE -> postFilter.append(",colorbalance=rs=0.08:gs=0.04:bs=-0.05,eq=contrast=0.92:brightness=0.04")
                                FcpLutPreset.EMERALD -> postFilter.append(",colorbalance=gs=0.12:gm=0.08:bm=-0.05")
                                FcpLutPreset.NONE -> {}
                            }

                            // Visual Effects
                            val effInt = clip.effectIntensity.coerceIn(0.1f, 1.0f)
                            when (clip.effect) {
                                FcpVisualEffect.VIGNETTE_MOOD -> {
                                    val angle = String.format(Locale.US, "%.3f", (Math.PI / 4.0) * effInt)
                                    postFilter.append(",vignette=angle=$angle")
                                }
                                FcpVisualEffect.FILM_GRAIN -> {
                                    val noiseStr = (18 * effInt).toInt().coerceIn(6, 32)
                                    postFilter.append(",noise=alls=$noiseStr:allf=t")
                                }
                                FcpVisualEffect.VHS_RETRO -> {
                                    postFilter.append(",eq=contrast=1.15:saturation=1.25,noise=c0s=16:allf=t,colorbalance=rs=0.08:bs=-0.06")
                                }
                                FcpVisualEffect.GLITCH -> {
                                    postFilter.append(",noise=c0s=32:c1s=22:allf=t,hue=H=PI/16,eq=contrast=1.25:saturation=1.35")
                                }
                                FcpVisualEffect.CINEMA_BLOOM -> {
                                    postFilter.append(",eq=brightness=0.08:contrast=1.15:saturation=1.12")
                                }
                                FcpVisualEffect.PRISM_SPLIT -> {
                                    postFilter.append(",colorbalance=rs=0.15:gs=-0.05:bs=0.20:rm=0.10:bm=-0.10,eq=contrast=1.18:saturation=1.28")
                                }
                                FcpVisualEffect.NOIR_CONTRAST -> {
                                    postFilter.append(",hue=s=0,eq=contrast=1.38:brightness=-0.04")
                                }
                                FcpVisualEffect.ANAMORPHIC_FLARE -> {
                                    postFilter.append(",colorbalance=rh=-0.05:gh=0.02:bh=0.26:rm=-0.02:bm=0.16,eq=contrast=1.15:saturation=1.18")
                                }
                                FcpVisualEffect.CYBER_SCANLINE -> {
                                    postFilter.append(",colorbalance=gs=0.08:bs=0.15,noise=c0s=12:allf=t")
                                }
                                FcpVisualEffect.NONE -> {}
                            }

                            // Motion & Camera Animations (100% FFmpeg 4.0 compatible)
                            appendClipAnimation(postFilter, clip.animationEffect, clip.animationIntensity, effectiveDur, finalW, finalH, targetFps)

                            // Boundary Transitions (100% FFmpeg 4.0 compatible, constant finalW x finalH)
                            val transToNext = if (i < clipSpecs.size - 1) clip.transitionToNext else FcpTransitionType.NONE
                            val nextTransDur = clip.transitionDurationSec.coerceIn(0.15f, 2.0f)
                            val halfTrans = (nextTransDur / 2f).coerceAtMost(effectiveDur / 2.2f)
                            val fadeStart = (effectiveDur - halfTrans).coerceAtLeast(0f)

                            val prevTrans = if (i > 0) clipSpecs[i - 1].clip.transitionToNext else FcpTransitionType.NONE
                            val prevTransDur = if (i > 0) clipSpecs[i - 1].clip.transitionDurationSec.coerceIn(0.15f, 2.0f) else 0f
                            val prevHalfTrans = (prevTransDur / 2f).coerceAtMost(effectiveDur / 2.2f)

                            appendBoundaryTransitions(
                                vFilter = postFilter,
                                hasPrev = i > 0,
                                hasNext = i < clipSpecs.size - 1,
                                prevTrans = prevTrans,
                                prevHalfTrans = prevHalfTrans,
                                transToNext = transToNext,
                                fadeStart = fadeStart,
                                halfTrans = halfTrans,
                                finalW = finalW,
                                finalH = finalH,
                                targetFps = targetFps
                            )
                        }

                        // Guarantee final exact dimensions and duration before concat
                        postFilter.append(String.format(Locale.US, ",setsar=1,trim=start=0:end=%.3f,setpts=PTS-STARTPTS", effectiveDur))
                        filterGraph.append("$composedLabel$postFilter[v_clip_$i];")
                        concatVideoLabels.add("[v_clip_$i]")

                        // Audio chain
                        if (needOriginalAudio) {
                            if (spec.hasAudio) {
                                val aFilter = StringBuilder()
                                aFilter.append(String.format(Locale.US, "atrim=start=%.3f:end=%.3f,asetpts=PTS-STARTPTS", spec.sTime, spec.eTime))
                                if (spec.speed != 1.0f) {
                                    var cur = spec.speed.coerceIn(0.25f, 4.0f)
                                    while (cur > 2.0f) {
                                        aFilter.append(",atempo=2.0")
                                        cur /= 2.0f
                                    }
                                    while (cur < 0.5f) {
                                        aFilter.append(",atempo=0.5")
                                        cur /= 0.5f
                                    }
                                    aFilter.append(String.format(Locale.US, ",atempo=%.3f", cur))
                                }

                                val transToNext = if (i < clipSpecs.size - 1) clip.transitionToNext else FcpTransitionType.NONE
                                val halfTrans = (clip.transitionDurationSec.coerceIn(0.15f, 2.0f) / 2f).coerceAtMost(effectiveDur / 2.2f)
                                val audioFadeOut = if (i < clipSpecs.size - 1 && transToNext != FcpTransitionType.NONE) {
                                    halfTrans.coerceAtMost(0.25f)
                                } else 0.04f
                                val audioFadeIn = if (i > 0 && clipSpecs[i - 1].clip.transitionToNext != FcpTransitionType.NONE) {
                                    val prevDur = clipSpecs[i - 1].clip.transitionDurationSec.coerceIn(0.1f, 1.5f)
                                    (prevDur / 2f).coerceAtMost(effectiveDur / 2f).coerceAtMost(0.25f)
                                } else 0.04f

                                if (effectiveDur > 0.2f) {
                                    if (audioFadeIn > 0.01f) {
                                        aFilter.append(String.format(Locale.US, ",afade=t=in:st=0:d=%.3f", audioFadeIn))
                                    }
                                    if (audioFadeOut > 0.01f) {
                                        val aFadeStart = (effectiveDur - audioFadeOut).coerceAtLeast(0f)
                                        aFilter.append(String.format(Locale.US, ",afade=t=out:st=%.3f:d=%.3f", aFadeStart, audioFadeOut))
                                    }
                                }
                                aFilter.append(
                                    String.format(
                                        Locale.US,
                                        ",aformat=sample_rates=44100:channel_layouts=stereo,aresample=async=1,apad,atrim=start=0:end=%.3f,asetpts=PTS-STARTPTS",
                                        effectiveDur
                                    )
                                )
                                filterGraph.append("[$inIdx:a]$aFilter[a_clip_$i];")
                                concatAudioLabels.add("[a_clip_$i]")
                            } else {
                                filterGraph.append(
                                    String.format(
                                        Locale.US,
                                        "anullsrc=channel_layout=stereo:sample_rate=44100,atrim=start=0:end=%.3f,asetpts=PTS-STARTPTS[a_clip_$i];",
                                        effectiveDur
                                    )
                                )
                                concatAudioLabels.add("[a_clip_$i]")
                            }
                        }
                    }

                    // Concatenate clips
                    var finalVideoStream: String
                    var finalAudioStream: String
                    if (clipSpecs.size == 1) {
                        finalVideoStream = "[v_clip_0]"
                        finalAudioStream = if (needOriginalAudio) "[a_clip_0]" else ""
                    } else {
                        val nClips = clipSpecs.size
                        val concatBuilder = StringBuilder()
                        if (needOriginalAudio) {
                            for (i in 0 until nClips) {
                                concatBuilder.append(concatVideoLabels[i])
                                concatBuilder.append(concatAudioLabels[i])
                            }
                            concatBuilder.append("concat=n=$nClips:v=1:a=1[v_concat][a_concat];")
                            finalVideoStream = "[v_concat]"
                            finalAudioStream = "[a_concat]"
                        } else {
                            for (i in 0 until nClips) {
                                concatBuilder.append(concatVideoLabels[i])
                            }
                            concatBuilder.append("concat=n=$nClips:v=1:a=0[v_concat];")
                            finalVideoStream = "[v_concat]"
                            finalAudioStream = ""
                        }
                        filterGraph.append(concatBuilder.toString())
                    }

                    // Custom audio soundtrack
                    if (customAudioInputIndex != null && customAudio != null) {
                        val vol = customAudio.volume.coerceIn(0f, 1f)
                        val muteOrig = customAudio.muteOriginalVideo
                        val safeLoopSamples = ((totalTimelineDurationSec + 5f) * 44100).toInt().coerceIn(44100, 26_460_000)
                        val loopFilter = if (customAudio.loopAudio) "aloop=loop=-1:size=$safeLoopSamples," else ""

                        val trimStart = customAudio.trimStartSec.coerceAtLeast(0f)
                        val trimEnd = if (customAudio.trimEndSec > trimStart + 0.1f) {
                            customAudio.trimEndSec
                        } else {
                            trimStart + totalTimelineDurationSec
                        }
                        val trimFilter = if (trimStart > 0f || customAudio.trimEndSec > 0f) {
                            String.format(Locale.US, "atrim=start=%.3f:end=%.3f,asetpts=PTS-STARTPTS,", trimStart, trimEnd)
                        } else ""

                        val fadeInFilter = if (customAudio.fadeInSec > 0.01f) {
                            String.format(Locale.US, ",afade=t=in:st=0:d=%.3f", customAudio.fadeInSec.coerceAtMost(totalTimelineDurationSec / 2f))
                        } else ""
                        val fadeOutFilter = if (customAudio.fadeOutSec > 0.01f) {
                            val durOut = customAudio.fadeOutSec.coerceAtMost(totalTimelineDurationSec / 2f)
                            val fadeOutStart = (totalTimelineDurationSec - durOut).coerceAtLeast(0f)
                            String.format(Locale.US, ",afade=t=out:st=%.3f:d=%.3f", fadeOutStart, durOut)
                        } else ""

                        if (muteOrig || !needOriginalAudio) {
                            filterGraph.append(
                                String.format(
                                    Locale.US,
                                    "[$customAudioInputIndex:a]aformat=sample_rates=44100:channel_layouts=stereo,%s%satrim=start=0:end=%.3f,asetpts=PTS-STARTPTS,volume=%.3f%s%s,apad[a_custom];",
                                    trimFilter, loopFilter, totalTimelineDurationSec, vol, fadeInFilter, fadeOutFilter
                                )
                            )
                            finalAudioStream = "[a_custom]"
                        } else {
                            // Apply auto-ducking attenuation directly via volume (avoids FFmpeg 4.1+ amix weights option)
                            val effectiveVol = if (customAudio.autoDucking) (vol * 0.35f).coerceIn(0f, 1f) else vol
                            filterGraph.append(
                                String.format(
                                    Locale.US,
                                    "[$customAudioInputIndex:a]aformat=sample_rates=44100:channel_layouts=stereo,%s%satrim=start=0:end=%.3f,asetpts=PTS-STARTPTS,volume=%.3f%s%s[a_custom];",
                                    trimFilter, loopFilter, totalTimelineDurationSec, effectiveVol, fadeInFilter, fadeOutFilter
                                )
                            )
                            filterGraph.append("${finalAudioStream}[a_custom]amix=inputs=2:duration=first:dropout_transition=2[a_mixed];")
                            finalAudioStream = "[a_mixed]"
                        }
                    }

                    // Overlay each text layer individually with its own timing & entrance/exit animation
                    for (tIdx in textOverlaySpecs.indices) {
                        val tSpec = textOverlaySpecs[tIdx]
                        val tIn = textInputIndices[tIdx]
                        val layer = tSpec.layer
                        val st = tSpec.startSec
                        val en = tSpec.endSec
                        val animDur = layer.animationDurationSec.coerceIn(0.15f, ((en - st) / 2.2f).coerceAtLeast(0.15f))
                        val outLabel = "[v_txt_$tIdx]"
                        val enableExpr = String.format(Locale.US, "between(t,%.2f,%.2f)", st, en)

                        if (!compatibilityMode && tSpec.needsLoopStream) {
                            val txtStream = "[txt_anim_$tIdx]"
                            val fadeChain = StringBuilder("format=rgba")
                            if (layer.inAnimation == TextInAnimation.FADE_IN) {
                                fadeChain.append(String.format(Locale.US, ",fade=t=in:st=%.3f:d=%.3f:alpha=1", st, animDur))
                            }
                            if (layer.outAnimation == TextOutAnimation.FADE_OUT || layer.outAnimation == TextOutAnimation.BLUR_OUT) {
                                val outSt = (en - animDur).coerceAtLeast(st)
                                fadeChain.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f:alpha=1", outSt, animDur))
                            }
                            filterGraph.append("[$tIn:v]$fadeChain$txtStream;")
                            filterGraph.append(
                                "$finalVideoStream${txtStream}overlay=0:0:enable='$enableExpr':shortest=0:eof_action=pass$outLabel;"
                            )
                        } else if (!compatibilityMode && (
                                layer.inAnimation == TextInAnimation.SLIDE_UP ||
                                    layer.inAnimation == TextInAnimation.SLIDE_RIGHT ||
                                    layer.outAnimation == TextOutAnimation.SLIDE_DOWN ||
                                    layer.outAnimation == TextOutAnimation.SLIDE_LEFT
                                )
                        ) {
                            val outSt = (en - animDur).coerceAtLeast(st)
                            val xExpr = when {
                                layer.inAnimation == TextInAnimation.SLIDE_RIGHT && layer.outAnimation == TextOutAnimation.SLIDE_LEFT ->
                                    String.format(
                                        Locale.US,
                                        "if(lt(t,%.3f),-W*(1-(t-%.3f)/%.3f),if(gt(t,%.3f),-W*((t-%.3f)/%.3f),0))",
                                        st + animDur, st, animDur, outSt, outSt, animDur
                                    )
                                layer.inAnimation == TextInAnimation.SLIDE_RIGHT ->
                                    String.format(Locale.US, "if(lt(t,%.3f),-W*(1-(t-%.3f)/%.3f),0)", st + animDur, st, animDur)
                                layer.outAnimation == TextOutAnimation.SLIDE_LEFT ->
                                    String.format(Locale.US, "if(gt(t,%.3f),-W*((t-%.3f)/%.3f),0)", outSt, outSt, animDur)
                                else -> "0"
                            }
                            val yExpr = when {
                                layer.inAnimation == TextInAnimation.SLIDE_UP && layer.outAnimation == TextOutAnimation.SLIDE_DOWN ->
                                    String.format(
                                        Locale.US,
                                        "if(lt(t,%.3f),H*0.25*(1-(t-%.3f)/%.3f),if(gt(t,%.3f),H*0.25*((t-%.3f)/%.3f),0))",
                                        st + animDur, st, animDur, outSt, outSt, animDur
                                    )
                                layer.inAnimation == TextInAnimation.SLIDE_UP ->
                                    String.format(Locale.US, "if(lt(t,%.3f),H*0.25*(1-(t-%.3f)/%.3f),0)", st + animDur, st, animDur)
                                layer.outAnimation == TextOutAnimation.SLIDE_DOWN ->
                                    String.format(Locale.US, "if(gt(t,%.3f),H*0.25*((t-%.3f)/%.3f),0)", outSt, outSt, animDur)
                                else -> "0"
                            }
                            filterGraph.append(
                                "$finalVideoStream[$tIn:v]overlay=x='$xExpr':y='$yExpr':enable='$enableExpr':repeatlast=1$outLabel;"
                            )
                        } else {
                            filterGraph.append(
                                "$finalVideoStream[$tIn:v]overlay=0:0:enable='$enableExpr':repeatlast=1$outLabel;"
                            )
                        }
                        finalVideoStream = outLabel
                    }

                    val finalGraph = filterGraph.toString().trimEnd(';')
                    cmdArgs.add("-filter_complex")
                    cmdArgs.add(finalGraph)
                    cmdArgs.add("-map")
                    cmdArgs.add(finalVideoStream)
                    if (finalAudioStream.isNotEmpty()) {
                        cmdArgs.add("-map")
                        cmdArgs.add(finalAudioStream)
                    }
                    cmdArgs.add("-t")
                    cmdArgs.add(String.format(Locale.US, "%.3f", totalTimelineDurationSec))

                    val (maxBitrate, bufSize, h264Level) = when (res.lowercase(Locale.US)) {
                        "720p" -> Triple("6M", "10M", "4.0")
                        "4k" -> Triple("24M", "36M", "5.1")
                        else -> Triple("12M", "18M", "4.1")
                    }

                    // Capped-CRF H.264 encoding (no conflicting -b:v so -crf actually governs quality & size)
                    cmdArgs.add("-c:v"); cmdArgs.add("libx264")
                    cmdArgs.add("-preset"); cmdArgs.add(preset)
                    cmdArgs.add("-crf"); cmdArgs.add(crf)
                    cmdArgs.add("-maxrate"); cmdArgs.add(maxBitrate)
                    cmdArgs.add("-bufsize"); cmdArgs.add(bufSize)
                    cmdArgs.add("-profile:v"); cmdArgs.add("high")
                    cmdArgs.add("-level"); cmdArgs.add(h264Level)
                    cmdArgs.add("-pix_fmt"); cmdArgs.add("yuv420p")
                    if (finalAudioStream.isNotEmpty()) {
                        cmdArgs.add("-c:a"); cmdArgs.add("aac")
                        cmdArgs.add("-b:a"); cmdArgs.add("192k")
                    }
                    cmdArgs.add("-movflags"); cmdArgs.add("+faststart")
                    cmdArgs.add(outputFile.absolutePath)

                    return cmdArgs.toTypedArray()
                }

                try {
                    RxFFmpegInvoke.getInstance().setDebug(true)
                } catch (_: Throwable) {}

                var lastLoggedPct = -1
                var ffmpegErrorMsg: String? = null

                fun createListener(stagePrefix: String, minP: Float, maxP: Float) =
                    object : RxFFmpegInvoke.IFFmpegListener {
                        override fun onFinish() {
                            Log.d(TAG, "FFmpeg onFinish callback received")
                        }

                        override fun onProgress(progress: Int, progressTime: Long) {
                            // Prefer progressTime (microseconds) vs total timeline duration for accurate multi-clip progress
                            val frac = if (progressTime > 0L) {
                                (progressTime.toDouble() / totalTimelineDurationUs.toDouble()).toFloat().coerceIn(0f, 1f)
                            } else if (progress >= 0) {
                                (progress.coerceIn(0, 100) / 100f)
                            } else {
                                return
                            }
                            val mapped = (minP + frac * (maxP - minP)).coerceIn(minP, maxP)
                            val pct = (mapped * 100f).toInt()
                            if (pct != lastLoggedPct) {
                                lastLoggedPct = pct
                                reportProgress(mapped, "$stagePrefix ($pct%)...")
                            }
                        }

                        override fun onCancel() {
                            Log.w(TAG, "FFmpeg onCancel callback received")
                            isCancelled = true
                        }

                        override fun onError(message: String?) {
                            Log.e(TAG, "FFmpeg onError callback received: $message")
                            ffmpegErrorMsg = message
                        }
                    }

                if (isCancelled || !isActive) {
                    try { outputFile.delete() } catch (_: Throwable) {}
                    return@withContext null
                }

                // Pass 1: Full-fidelity timeline render
                try { outputFile.delete() } catch (_: Throwable) {}
                reportProgress(0.24f, "Rendering video timeline (24%)...")
                val primaryCmd = buildCommand(compatibilityMode = false, preset = "ultrafast", crf = "22")
                Log.d(TAG, "Executing Primary FFmpeg (${primaryCmd.size} args): ${primaryCmd.joinToString(" ")}")

                var returnCode = try {
                    RxFFmpegInvoke.getInstance().runCommand(
                        primaryCmd,
                        createListener("Rendering video", 0.24f, 0.86f)
                    )
                } catch (t: Throwable) {
                    Log.e(TAG, "RxFFmpeg primary invocation crashed", t)
                    -1
                }
                Log.d(TAG, "Primary FFmpeg returned code: $returnCode (ErrorMsg: $ffmpegErrorMsg)")

                // Pass 2: Safe compatibility graph (keeps trims, speed, music, color & static text; strips exotic transitions/animations)
                if ((returnCode != 0 || !outputFile.exists() || outputFile.length() == 0L) && !isCancelled && isActive) {
                    Log.w(TAG, "Primary FFmpeg failed ($returnCode: $ffmpegErrorMsg). Retrying with safe compatibility graph...")
                    reportProgress(0.40f, "Retrying in compatibility mode...")
                    try { outputFile.delete() } catch (_: Throwable) {}
                    ffmpegErrorMsg = null
                    lastLoggedPct = -1

                    val fallbackCmd = buildCommand(compatibilityMode = true, preset = "veryfast", crf = "25")
                    Log.d(TAG, "Executing Compatibility FFmpeg (${fallbackCmd.size} args): ${fallbackCmd.joinToString(" ")}")

                    returnCode = try {
                        RxFFmpegInvoke.getInstance().runCommand(
                            fallbackCmd,
                            createListener("Compatibility render", 0.40f, 0.86f)
                        )
                    } catch (t: Throwable) {
                        Log.e(TAG, "RxFFmpeg compatibility invocation crashed", t)
                        -1
                    }
                    Log.d(TAG, "Compatibility FFmpeg returned code: $returnCode (ErrorMsg: $ffmpegErrorMsg)")
                }

                if (isCancelled || !isActive) {
                    try { outputFile.delete() } catch (_: Throwable) {}
                    return@withContext null
                }

                if (returnCode == 0 && outputFile.exists() && outputFile.length() > 0L) {
                    reportProgress(0.88f, "Finishing up export...")
                    Log.i(TAG, "Export succeeded! Size: ${outputFile.length()} bytes, path: ${outputFile.absolutePath}")
                    return@withContext outputFile
                }

                // Pass 3: Native hardware MediaMuxer fallback (only viable when at least one video clip exists)
                val hasVideoClips = clipSpecs.any { !it.isImage }
                if (!isCancelled && isActive && hasVideoClips) {
                    Log.w(TAG, "FFmpeg failed ($returnCode: $ffmpegErrorMsg); attempting native MediaMuxer fallback")
                    reportProgress(0.65f, "Using direct muxer fallback...")
                    try {
                        outputFile.delete()
                        val videoOnlyClips = clipSpecs.filter { !it.isImage }.map { it.clip }
                        val success = VideoProcessor.concatVideoClips(context, videoOnlyClips, outputFile)
                        if (success && outputFile.exists() && outputFile.length() > 0L) {
                            reportProgress(0.88f, "Render complete!")
                            return@withContext outputFile
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Native fallback failed", e)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "exportTimeline encountered exception", e)
            } finally {
                if (isCancelled) {
                    try { outputFile.delete() } catch (_: Throwable) {}
                }
                for (f in tempFiles) {
                    try {
                        if (f.exists() && f.parentFile == context.cacheDir) {
                            f.delete()
                        }
                    } catch (_: Throwable) {}
                }
            }

            try { outputFile.delete() } catch (_: Throwable) {}
            null
        }
    }

    /**
     * Scales/crops a foreground clip for zoom-out (scale < 1), 1:1 fit, or zoom-in (scale > 1)
     * without allocating frames larger than 2x target resolution.
     */
    private fun appendForegroundFraming(
        filterGraph: StringBuilder,
        srcLabel: String,
        dstLabel: String,
        finalW: Int,
        finalH: Int,
        clipScale: Float,
        panDx: Int,
        panDy: Int
    ) {
        if (clipScale <= 2.0f) {
            val scaledW = ((finalW * clipScale).roundToInt() / 2 * 2).coerceAtLeast(2)
            val scaledH = ((finalH * clipScale).roundToInt() / 2 * 2).coerceAtLeast(2)
            filterGraph.append(
                "$srcLabel" +
                    "scale=$scaledW:$scaledH:force_original_aspect_ratio=decrease:flags=bilinear$dstLabel;"
            )
        } else {
            // High zoom (>2x): crop source proportionally first, then scale to final canvas to cap RAM
            val normPanX = (panDx.toFloat() / finalW.toFloat()).coerceIn(-0.6f, 0.6f)
            val normPanY = (panDy.toFloat() / finalH.toFloat()).coerceIn(-0.6f, 0.6f)
            val xExpr = String.format(Locale.US, "min(max(0,(iw-ow)/2-(%.4f*iw)),iw-ow)", normPanX)
            val yExpr = String.format(Locale.US, "min(max(0,(ih-oh)/2-(%.4f*ih)),ih-oh)", normPanY)
            filterGraph.append(
                String.format(
                    Locale.US,
                    "%scrop=w=max(2,iw/%.3f):h=max(2,ih/%.3f):x='%s':y='%s',scale=%d:%d:force_original_aspect_ratio=decrease:flags=bilinear%s;",
                    srcLabel, clipScale, clipScale, xExpr, yExpr, finalW, finalH, dstLabel
                )
            )
        }
    }

    /** Appends strictly clamped clip motion animations (100% FFmpeg 4.0 compatible — no scale eval=frame). */
    private fun appendClipAnimation(
        vFilter: StringBuilder,
        animation: FcpClipAnimation,
        intensity: Float,
        effectiveDur: Float,
        finalW: Int,
        finalH: Int,
        targetFps: Int
    ) {
        val animInt = intensity.coerceIn(0.1f, 1.0f)
        val pExpr = String.format(Locale.US, "min(1,max(0,t/%.3f))", effectiveDur.coerceAtLeast(0.1f))
        val totalFrames = (effectiveDur * targetFps).roundToInt().coerceAtLeast(1)
        val periodFrames = (0.6f * targetFps).roundToInt().coerceAtLeast(1)
        when (animation) {
            FcpClipAnimation.SHAKE -> {
                val s = 1.15f
                val xExpr = String.format(Locale.US, "min(iw-ow,max(0,(iw-ow)/2+(sin(t*38)*16+cos(t*55)*8)*%.2f))", animInt)
                val yExpr = String.format(Locale.US, "min(ih-oh,max(0,(ih-oh)/2+(cos(t*45)*14+sin(t*33)*7)*%.2f))", animInt)
                vFilter.append(
                    String.format(
                        Locale.US,
                        ",scale=w=trunc(iw*%.2f/2)*2:h=trunc(ih*%.2f/2)*2,crop=w=%d:h=%d:x='%s':y='%s'",
                        s, s, finalW, finalH, xExpr, yExpr
                    )
                )
            }
            FcpClipAnimation.BEAT_PULSE -> {
                val zExpr = String.format(
                    Locale.US,
                    "1.0+0.12*pow(1-mod(in,%d)/%d,2)*%.2f",
                    periodFrames, periodFrames, animInt
                )
                vFilter.append(
                    ",zoompan=z='$zExpr':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=${finalW}x${finalH}:fps=$targetFps"
                )
            }
            FcpClipAnimation.ZOOM_IN -> {
                val zExpr = String.format(Locale.US, "1.0+0.22*min(1,max(0,in/%d))*%.2f", totalFrames, animInt)
                vFilter.append(
                    ",zoompan=z='$zExpr':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=${finalW}x${finalH}:fps=$targetFps"
                )
            }
            FcpClipAnimation.ZOOM_OUT -> {
                val zExpr = String.format(Locale.US, "1.0+0.22*(1.0-min(1,max(0,in/%d)))*%.2f", totalFrames, animInt)
                vFilter.append(
                    ",zoompan=z='$zExpr':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=${finalW}x${finalH}:fps=$targetFps"
                )
            }
            FcpClipAnimation.CAMERA_JITTER -> {
                val s = 1.08f
                val xExpr = String.format(Locale.US, "min(iw-ow,max(0,(iw-ow)/2+(sin(t*3.4)*9+cos(t*5.1)*5)*%.2f))", animInt)
                val yExpr = String.format(Locale.US, "min(ih-oh,max(0,(ih-oh)/2+(cos(t*4.2)*8+sin(t*2.8)*4)*%.2f))", animInt)
                vFilter.append(
                    String.format(
                        Locale.US,
                        ",scale=w=trunc(iw*%.2f/2)*2:h=trunc(ih*%.2f/2)*2,crop=w=%d:h=%d:x='%s':y='%s'",
                        s, s, finalW, finalH, xExpr, yExpr
                    )
                )
            }
            FcpClipAnimation.PAN_LEFT -> {
                val s = 1.15f
                val xExpr = "min(iw-ow,max(0,(iw-ow)*(1.0-($pExpr))))"
                vFilter.append(
                    String.format(
                        Locale.US,
                        ",scale=w=trunc(iw*%.2f/2)*2:h=trunc(ih*%.2f/2)*2,crop=w=%d:h=%d:x='%s':y=(ih-oh)/2",
                        s, s, finalW, finalH, xExpr
                    )
                )
            }
            FcpClipAnimation.PAN_RIGHT -> {
                val s = 1.15f
                val xExpr = "min(iw-ow,max(0,(iw-ow)*($pExpr)))"
                vFilter.append(
                    String.format(
                        Locale.US,
                        ",scale=w=trunc(iw*%.2f/2)*2:h=trunc(ih*%.2f/2)*2,crop=w=%d:h=%d:x='%s':y=(ih-oh)/2",
                        s, s, finalW, finalH, xExpr
                    )
                )
            }
            FcpClipAnimation.FLASH_STROBE -> {
                val bright = String.format(Locale.US, "%.2f", (0.22f * animInt).coerceIn(0.05f, 0.35f))
                vFilter.append(",eq=brightness=$bright:enable='lt(mod(t,0.4),0.10)'")
            }
            FcpClipAnimation.WOBBLE -> {
                vFilter.append(String.format(Locale.US, ",rotate='sin(t*4.5)*0.04*%.2f':ow=iw:oh=ih:c=black", animInt))
            }
            FcpClipAnimation.SWAY_DRIFT -> {
                vFilter.append(String.format(Locale.US, ",rotate='sin(t*1.8)*0.025*%.2f':ow=iw:oh=ih:c=black", animInt))
            }
            FcpClipAnimation.NONE -> {}
        }
    }

    /**
     * Appends head & tail boundary transitions with strictly clamped progress [0, 1], constant
     * (finalW x finalH) output dimensions, and 100% FFmpeg 4.0 compatible filters.
     */
    private fun appendBoundaryTransitions(
        vFilter: StringBuilder,
        hasPrev: Boolean,
        hasNext: Boolean,
        prevTrans: FcpTransitionType,
        prevHalfTrans: Float,
        transToNext: FcpTransitionType,
        fadeStart: Float,
        halfTrans: Float,
        finalW: Int,
        finalH: Int,
        targetFps: Int
    ) {
        val outP = String.format(Locale.US, "min(1,max(0,(t-%.3f)/%.3f))", fadeStart, halfTrans.coerceAtLeast(0.05f))
        val inP = String.format(Locale.US, "min(1,max(0,t/%.3f))", prevHalfTrans.coerceAtLeast(0.05f))
        val outStartFrame = (fadeStart * targetFps).roundToInt().coerceAtLeast(0)
        val outTransFrames = (halfTrans * targetFps).roundToInt().coerceAtLeast(1)
        val inTransFrames = (prevHalfTrans * targetFps).roundToInt().coerceAtLeast(1)
        val outFrameP = "min(1,max(0,(in-$outStartFrame)/$outTransFrames))"
        val inFrameP = "min(1,max(0,in/$inTransFrames))"

        // Horizontal slide / push transitions
        val hasIncomingHorizSlide = hasPrev && prevTrans in listOf(
            FcpTransitionType.PUSH_LEFT, FcpTransitionType.SLIDE_LEFT,
            FcpTransitionType.PUSH_RIGHT, FcpTransitionType.WIPE_RIGHT,
            FcpTransitionType.COVER_SLIDE
        ) && prevHalfTrans > 0.05f

        val hasOutgoingHorizSlide = hasNext && transToNext in listOf(
            FcpTransitionType.PUSH_LEFT, FcpTransitionType.SLIDE_LEFT,
            FcpTransitionType.PUSH_RIGHT, FcpTransitionType.WIPE_RIGHT,
            FcpTransitionType.PARALLAX, FcpTransitionType.COVER_SLIDE
        ) && halfTrans > 0.05f

        if (hasIncomingHorizSlide || hasOutgoingHorizSlide) {
            val inExpr = when (prevTrans) {
                FcpTransitionType.PUSH_RIGHT, FcpTransitionType.WIPE_RIGHT ->
                    "min(2*$finalW,max(0,2*$finalW-$finalW*$inP))"
                FcpTransitionType.PUSH_LEFT, FcpTransitionType.SLIDE_LEFT, FcpTransitionType.COVER_SLIDE ->
                    "min(2*$finalW,max(0,$finalW*$inP))"
                else -> "$finalW"
            }
            val outExpr = when (transToNext) {
                FcpTransitionType.PUSH_RIGHT, FcpTransitionType.WIPE_RIGHT ->
                    "min(2*$finalW,max(0,$finalW-$finalW*$outP))"
                FcpTransitionType.PUSH_LEFT, FcpTransitionType.SLIDE_LEFT ->
                    "min(2*$finalW,max(0,$finalW+$finalW*$outP))"
                FcpTransitionType.COVER_SLIDE ->
                    "min(2*$finalW,max(0,$finalW-${(finalW * 0.25f).toInt()}*$outP))"
                FcpTransitionType.PARALLAX ->
                    "min(2*$finalW,max(0,$finalW-${(finalW * 0.8f).toInt()}*$outP))"
                else -> "$finalW"
            }
            val xExpr = when {
                hasIncomingHorizSlide && hasOutgoingHorizSlide ->
                    String.format(Locale.US, "if(lte(t,%.3f),%s,if(gte(t,%.3f),%s,%d))", prevHalfTrans, inExpr, fadeStart, outExpr, finalW)
                hasIncomingHorizSlide ->
                    String.format(Locale.US, "if(lte(t,%.3f),%s,%d)", prevHalfTrans, inExpr, finalW)
                else ->
                    String.format(Locale.US, "if(gte(t,%.3f),%s,%d)", fadeStart, outExpr, finalW)
            }
            vFilter.append(
                String.format(Locale.US, ",pad=3*%d:%d:%d:0:black,crop=w=%d:h=%d:x='%s':y=0", finalW, finalH, finalW, finalW, finalH, xExpr)
            )
        }

        // Vertical slide / push transitions
        val hasIncomingVertSlide = hasPrev && prevTrans in listOf(FcpTransitionType.PUSH_UP, FcpTransitionType.PUSH_DOWN) && prevHalfTrans > 0.05f
        val hasOutgoingVertSlide = hasNext && transToNext in listOf(FcpTransitionType.PUSH_UP, FcpTransitionType.PUSH_DOWN) && halfTrans > 0.05f

        if (hasIncomingVertSlide || hasOutgoingVertSlide) {
            val inYExpr = when (prevTrans) {
                FcpTransitionType.PUSH_DOWN -> "min(2*$finalH,max(0,2*$finalH-$finalH*$inP))"
                FcpTransitionType.PUSH_UP -> "min(2*$finalH,max(0,$finalH*$inP))"
                else -> "$finalH"
            }
            val outYExpr = when (transToNext) {
                FcpTransitionType.PUSH_DOWN -> "min(2*$finalH,max(0,$finalH-$finalH*$outP))"
                FcpTransitionType.PUSH_UP -> "min(2*$finalH,max(0,$finalH+$finalH*$outP))"
                else -> "$finalH"
            }
            val yExpr = when {
                hasIncomingVertSlide && hasOutgoingVertSlide ->
                    String.format(Locale.US, "if(lte(t,%.3f),%s,if(gte(t,%.3f),%s,%d))", prevHalfTrans, inYExpr, fadeStart, outYExpr, finalH)
                hasIncomingVertSlide ->
                    String.format(Locale.US, "if(lte(t,%.3f),%s,%d)", prevHalfTrans, inYExpr, finalH)
                else ->
                    String.format(Locale.US, "if(gte(t,%.3f),%s,%d)", fadeStart, outYExpr, finalH)
            }
            vFilter.append(
                String.format(Locale.US, ",pad=%d:3*%d:0:%d:black,crop=w=%d:h=%d:x=0:y='%s'", finalW, finalH, finalH, finalW, finalH, yExpr)
            )
        }

        // Outgoing tail transitions
        if (hasNext && transToNext != FcpTransitionType.NONE && halfTrans > 0.05f) {
            when (transToNext) {
                FcpTransitionType.DIP_TO_BLACK, FcpTransitionType.CROSS_DISSOLVE, FcpTransitionType.CROSSFADE -> {
                    vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f:c=black", fadeStart, halfTrans))
                }
                FcpTransitionType.DIP_TO_WHITE -> {
                    vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f:c=white", fadeStart, halfTrans))
                }
                FcpTransitionType.FILM_BLOOM -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",zoompan=z='1+0.12*%s':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=%dx%d:fps=%d,fade=t=out:st=%.3f:d=%.3f:c=white,eq=brightness=0.18:contrast=1.12:saturation=1.18:enable='gte(t,%.3f)'",
                            outFrameP, finalW, finalH, targetFps, fadeStart, halfTrans, fadeStart
                        )
                    )
                }
                FcpTransitionType.GLITCH -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",noise=c0s=42:c1s=32:allf=t:enable='gte(t,%.3f)',hue=H='PI*sin(20*t)':enable='gte(t,%.3f)',eq=contrast=1.35:brightness=0.06:enable='gte(t,%.3f)'",
                            fadeStart, fadeStart, fadeStart
                        )
                    )
                }
                FcpTransitionType.ZOOM_IN -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",zoompan=z='1+0.9*%s':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=%dx%d:fps=%d,fade=t=out:st=%.3f:d=%.3f:c=black",
                            outFrameP, finalW, finalH, targetFps, fadeStart, halfTrans
                        )
                    )
                }
                FcpTransitionType.ZOOM_OUT, FcpTransitionType.QUAD_SHUTTER, FcpTransitionType.DIAMOND_IRIS,
                FcpTransitionType.CUBE_3D, FcpTransitionType.PAGE_FLIP_3D -> {
                    // Pad 2x then zoompan from 2.0 down to 1.15 (always >= 1.0, exact 1:1 original when p=0)
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",pad=2*%d:2*%d:%d/2:%d/2:black,zoompan=z='2.0-0.85*%s':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=%dx%d:fps=%d,fade=t=out:st=%.3f:d=%.3f:c=black",
                            finalW, finalH, finalW, finalH, outFrameP, finalW, finalH, targetFps, fadeStart, halfTrans
                        )
                    )
                }
                FcpTransitionType.WHIP_SPIN, FcpTransitionType.CLOCK_SWEEP -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",rotate='2*PI*%s':ow=iw:oh=ih:c=black:enable='gte(t,%.3f)',fade=t=out:st=%.3f:d=%.3f:c=black",
                            outP, fadeStart, fadeStart, halfTrans
                        )
                    )
                }
                FcpTransitionType.WIPE_HORIZONTAL, FcpTransitionType.BARN_DOORS -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",pad=2*%d:%d:0:0:black,crop=w=%d:h=%d:x='min(%d,max(0,%d*%s))':y=0",
                            finalW, finalH, finalW, finalH, finalW, finalW, outP
                        )
                    )
                }
                FcpTransitionType.WIPE_LEFT -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",pad=2*%d:%d:%d:0:black,crop=w=%d:h=%d:x='min(%d,max(0,%d*(1-%s)))':y=0",
                            finalW, finalH, finalW, finalW, finalH, finalW, finalW, outP
                        )
                    )
                }
                FcpTransitionType.WIPE_DIAGONAL -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",pad=2*%d:2*%d:0:0:black,crop=w=%d:h=%d:x='min(%d,max(0,%d*%s))':y='min(%d,max(0,%d*%s))'",
                            finalW, finalH, finalW, finalH, finalW, finalW, outP, finalH, finalH, outP
                        )
                    )
                }
                FcpTransitionType.VENETIAN_BLINDS -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",pad=%d:2*%d:0:0:black,crop=w=%d:h=%d:x=0:y='min(%d,max(0,%d*%s))'",
                            finalW, finalH, finalW, finalH, finalH, finalH, outP
                        )
                    )
                }
                FcpTransitionType.CIRCLE_IRIS, FcpTransitionType.PINHOLE_SNAP -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",vignette=angle='max(0.05,(PI/2)*(1-%s))':eval=frame:enable='gte(t,%.3f)',fade=t=out:st=%.3f:d=%.3f:c=black",
                            outP, fadeStart, fadeStart, halfTrans
                        )
                    )
                }
                FcpTransitionType.PUSH_LEFT, FcpTransitionType.SLIDE_LEFT,
                FcpTransitionType.PUSH_RIGHT, FcpTransitionType.WIPE_RIGHT,
                FcpTransitionType.PARALLAX, FcpTransitionType.COVER_SLIDE,
                FcpTransitionType.PUSH_UP, FcpTransitionType.PUSH_DOWN,
                FcpTransitionType.NONE -> {}
            }
        }

        // Incoming head transitions
        if (hasPrev && prevTrans != FcpTransitionType.NONE && prevHalfTrans > 0.05f) {
            when (prevTrans) {
                FcpTransitionType.DIP_TO_BLACK, FcpTransitionType.CROSS_DISSOLVE, FcpTransitionType.CROSSFADE -> {
                    vFilter.append(String.format(Locale.US, ",fade=t=in:st=0:d=%.3f:c=black", prevHalfTrans))
                }
                FcpTransitionType.DIP_TO_WHITE -> {
                    vFilter.append(String.format(Locale.US, ",fade=t=in:st=0:d=%.3f:c=white", prevHalfTrans))
                }
                FcpTransitionType.FILM_BLOOM -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",zoompan=z='1+0.12*(1-%s)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=%dx%d:fps=%d,fade=t=in:st=0:d=%.3f:c=white,eq=brightness=0.18:contrast=1.12:saturation=1.18:enable='lte(t,%.3f)'",
                            inFrameP, finalW, finalH, targetFps, prevHalfTrans, prevHalfTrans
                        )
                    )
                }
                FcpTransitionType.GLITCH -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",noise=c0s=42:c1s=32:allf=t:enable='lte(t,%.3f)',hue=H='-PI*sin(20*t)':enable='lte(t,%.3f)',eq=contrast=1.35:brightness=0.06:enable='lte(t,%.3f)'",
                            prevHalfTrans, prevHalfTrans, prevHalfTrans
                        )
                    )
                }
                FcpTransitionType.ZOOM_IN -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",zoompan=z='1+0.9*(1-%s)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=%dx%d:fps=%d,fade=t=in:st=0:d=%.3f:c=black",
                            inFrameP, finalW, finalH, targetFps, prevHalfTrans
                        )
                    )
                }
                FcpTransitionType.ZOOM_OUT, FcpTransitionType.QUAD_SHUTTER, FcpTransitionType.DIAMOND_IRIS,
                FcpTransitionType.CUBE_3D, FcpTransitionType.PAGE_FLIP_3D -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",pad=2*%d:2*%d:%d/2:%d/2:black,zoompan=z='1.15+0.85*%s':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=%dx%d:fps=%d,fade=t=in:st=0:d=%.3f:c=black",
                            finalW, finalH, finalW, finalH, inFrameP, finalW, finalH, targetFps, prevHalfTrans
                        )
                    )
                }
                FcpTransitionType.WHIP_SPIN, FcpTransitionType.CLOCK_SWEEP -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",rotate='-2*PI*(1-%s)':ow=iw:oh=ih:c=black:enable='lte(t,%.3f)',fade=t=in:st=0:d=%.3f:c=black",
                            inP, prevHalfTrans, prevHalfTrans
                        )
                    )
                }
                FcpTransitionType.WIPE_HORIZONTAL, FcpTransitionType.BARN_DOORS -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",pad=2*%d:%d:%d:0:black,crop=w=%d:h=%d:x='min(%d,max(0,%d*%s))':y=0",
                            finalW, finalH, finalW, finalW, finalH, finalW, finalW, inP
                        )
                    )
                }
                FcpTransitionType.WIPE_LEFT -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",pad=2*%d:%d:0:0:black,crop=w=%d:h=%d:x='min(%d,max(0,%d*(1-%s)))':y=0",
                            finalW, finalH, finalW, finalH, finalW, finalW, inP
                        )
                    )
                }
                FcpTransitionType.WIPE_DIAGONAL -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",pad=2*%d:2*%d:%d:%d:black,crop=w=%d:h=%d:x='min(%d,max(0,%d*%s))':y='min(%d,max(0,%d*%s))'",
                            finalW, finalH, finalW, finalH, finalW, finalH, finalW, finalW, inP, finalH, finalH, inP
                        )
                    )
                }
                FcpTransitionType.VENETIAN_BLINDS -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",pad=%d:2*%d:0:%d:black,crop=w=%d:h=%d:x=0:y='min(%d,max(0,%d*%s))'",
                            finalW, finalH, finalH, finalW, finalH, finalH, finalH, inP
                        )
                    )
                }
                FcpTransitionType.CIRCLE_IRIS, FcpTransitionType.PINHOLE_SNAP -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",vignette=angle='max(0.05,(PI/2)*%s)':eval=frame:enable='lte(t,%.3f)',fade=t=in:st=0:d=%.3f:c=black",
                            inP, prevHalfTrans, prevHalfTrans
                        )
                    )
                }
                FcpTransitionType.PARALLAX -> {
                    vFilter.append(
                        String.format(
                            Locale.US,
                            ",zoompan=z='1+0.12*(1-%s)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=%dx%d:fps=%d,fade=t=in:st=0:d=%.3f:c=black",
                            inFrameP, finalW, finalH, targetFps, prevHalfTrans
                        )
                    )
                }
                FcpTransitionType.PUSH_LEFT, FcpTransitionType.SLIDE_LEFT,
                FcpTransitionType.PUSH_RIGHT, FcpTransitionType.WIPE_RIGHT,
                FcpTransitionType.COVER_SLIDE, FcpTransitionType.PUSH_UP,
                FcpTransitionType.PUSH_DOWN, FcpTransitionType.NONE -> {}
            }
        }
    }
}
