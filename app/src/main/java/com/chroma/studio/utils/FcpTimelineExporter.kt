package com.chroma.studio.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import com.chroma.studio.ui.screens.FcpAspectRatio
import com.chroma.studio.ui.screens.FcpCanvasBackground
import com.chroma.studio.ui.screens.FcpCanvasBgType
import com.chroma.studio.ui.screens.FcpClip
import com.chroma.studio.ui.screens.FcpCustomAudio
import com.chroma.studio.ui.screens.FcpLutPreset
import com.chroma.studio.ui.screens.FcpTextLayer
import com.chroma.studio.ui.screens.FcpTitlePresetType
import com.chroma.studio.ui.screens.FcpTransitionType
import com.chroma.studio.ui.screens.FcpVisualEffect
import com.chroma.studio.ui.screens.FcpClipAnimation
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

/**
 * High-performance Timeline Exporter for Final Cut Pro Video Editor.
 *
 * Handles:
 * 1. Multi-clip timelines (cut / split clips, newly imported clips, multiple media sources)
 * 2. Precision in/out trimming per clip ([FcpClip.trimStartSec] to [FcpClip.trimEndSec])
 * 3. Playback speed adjustment per clip ([FcpClip.speed])
 * 4. Precision Zoom & Crop from Preview Screen ([FcpClip.scale], [FcpClip.panX], [FcpClip.panY])
 * 5. Title & Text Layers overlay rendering ([FcpTextLayer])
 * 6. Hardware color adjustments (exposure, contrast, saturation, white balance)
 * 7. Seamless transitions between clips (Dip to Black, Dip to White, Dissolve, etc.)
 * 8. Uniform aspect ratio scaling and letterboxing (9:16, 16:9, 1:1, 21:9)
 * 9. Audio mixing for custom soundtracks / extracted audio
 * 10. Automatic fallback to hardware [VideoProcessor.concatVideoClips] if needed
 */
object FcpTimelineExporter {

    private const val TAG = "FcpTimelineExporter"

    private val typefaceCache = mutableMapOf<String, Typeface>()

    private fun getTypeface(context: Context, fontAsset: String): Typeface {
        if (fontAsset.isBlank()) return Typeface.DEFAULT_BOLD
        return typefaceCache.getOrPut(fontAsset) {
            try {
                Typeface.createFromAsset(context.assets, "fonts/$fontAsset")
            } catch (_: Exception) {
                try {
                    val clean = fontAsset.replace(" ", "")
                    Typeface.createFromAsset(context.assets, "fonts/$clean")
                } catch (_: Exception) {
                    Typeface.DEFAULT_BOLD
                }
            }
        }
    }

    /**
     * Renders all visible title & text layers onto a full-resolution transparent bitmap.
     */
    fun renderTextLayersToBitmap(
        context: Context,
        textLayers: List<FcpTextLayer>,
        width: Int,
        height: Int
    ): Bitmap? {
        val visibleLayers = textLayers.filter { it.isVisible && (it.primaryText.isNotBlank() || it.scriptText.isNotBlank()) }
        if (visibleLayers.isEmpty()) return null

        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Baseline scale factor relative to standard 360dp preview viewport
            val scale = width / 360f

            visibleLayers.forEach { layer ->
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
                    setShadowLayer(6f * scale, 2f * scale, 3f * scale, AndroidColor.argb((alphaInt * 0.85f).toInt(), 0, 0, 0))
                }
                if (layer.fontSizeSp > 0) {
                    letterSpacing = (layer.letterSpacingSp / layer.fontSizeSp).coerceIn(0f, 0.5f)
                }
            }

            if (layer.isGradientText) {
                val bounds = Rect()
                primaryPaint.getTextBounds(layer.primaryText, 0, layer.primaryText.length, bounds)
                val halfW = bounds.width() / 2f
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
                    setShadowLayer(5f * scale, 2f * scale, 2f * scale, AndroidColor.argb((alphaInt * 0.85f).toInt(), 0, 0, 0))
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

            // Optional pill background
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

            // Render preset styling
            when (layer.presetType) {
                FcpTitlePresetType.SCRIPT_HEADLINE -> {
                    val fm = primaryPaint.fontMetrics
                    val baseLineY = -(fm.ascent + fm.descent) / 2f
                    canvas.drawText(layer.primaryText, 0f, baseLineY, primaryPaint)

                    if (layer.scriptText.isNotBlank()) {
                        canvas.save()
                        val pBounds = Rect()
                        primaryPaint.getTextBounds(layer.primaryText, 0, layer.primaryText.length, pBounds)
                        canvas.translate(-pBounds.width() * 0.22f - (8f * scale), baseLineY - (12f * scale))
                        canvas.rotate(-7f)
                        canvas.drawText(layer.scriptText, 0f, 0f, scriptPaint)
                        canvas.restore()
                    }

                    if (layer.subtitleText.isNotBlank()) {
                        val subY = baseLineY + primarySizePx * 0.55f
                        canvas.drawText(layer.subtitleText, 0f, subY, subPaint)
                    }
                }

                FcpTitlePresetType.GOLD_3D -> {
                    val fm = primaryPaint.fontMetrics
                    val baseLineY = -(fm.ascent + fm.descent) / 2f

                    // Badge pill if set
                    if (layer.badgeText.isNotBlank()) {
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
                        badgePaint.getTextBounds(layer.badgeText.uppercase(), 0, layer.badgeText.length, badgeBounds)
                        val bw = badgeBounds.width() / 2f + 10f * scale
                        val bh = 8f * scale
                        val badgeRect = RectF(-bw, baseLineY - primarySizePx - bh * 2, bw, baseLineY - primarySizePx)
                        canvas.drawRoundRect(badgeRect, 50f, 50f, badgePillPaint)
                        canvas.drawRoundRect(badgeRect, 50f, 50f, badgeStroke)
                        canvas.drawText(layer.badgeText.uppercase(), 0f, badgeRect.centerY() + 3f * scale, badgePaint)
                    }

                    // 3D under-shadow 1
                    val shadow1 = Paint(primaryPaint).apply {
                        color = 0xFF261204.toInt()
                        alpha = alphaInt
                        clearShadowLayer()
                        shader = null
                    }
                    canvas.drawText(layer.primaryText, 3f * scale, baseLineY + 3f * scale, shadow1)

                    // 3D under-shadow 2
                    val shadow2 = Paint(primaryPaint).apply {
                        color = 0xFF78350F.toInt()
                        alpha = alphaInt
                        clearShadowLayer()
                        shader = null
                    }
                    canvas.drawText(layer.primaryText, 1.5f * scale, baseLineY + 1.5f * scale, shadow2)

                    // Front Metallic Gold text
                    val goldPaint = Paint(primaryPaint).apply {
                        if (!layer.isGradientText) {
                            shader = LinearGradient(
                                0f, baseLineY - primarySizePx, 0f, baseLineY,
                                intArrayOf(0xFFFFF9C4.toInt(), layer.accentColor.toArgb(), 0xFFF59E0B.toInt(), 0xFFB45309.toInt()),
                                null,
                                Shader.TileMode.CLAMP
                            )
                        }
                    }
                    canvas.drawText(layer.primaryText, 0f, baseLineY, goldPaint)

                    // Crossover script
                    if (layer.scriptText.isNotBlank()) {
                        canvas.save()
                        canvas.rotate(-9f)
                        val sPaint = Paint(scriptPaint).apply {
                            color = AndroidColor.WHITE
                            alpha = alphaInt
                        }
                        canvas.drawText(layer.scriptText, 0f, baseLineY + 2f * scale, sPaint)
                        canvas.restore()
                    }

                    if (layer.subtitleText.isNotBlank()) {
                        val subY = baseLineY + primarySizePx * 0.55f
                        canvas.drawText(layer.subtitleText, 0f, subY, subPaint)
                    }
                }

                FcpTitlePresetType.BOXED_FRAME -> {
                    val fm = primaryPaint.fontMetrics
                    val baseLineY = -(fm.ascent + fm.descent) / 2f
                    val pBounds = Rect()
                    primaryPaint.getTextBounds(layer.primaryText.uppercase(), 0, layer.primaryText.length, pBounds)
                    val padX = 20f * scale
                    val padY = 12f * scale
                    val halfW = (pBounds.width() / 2f) + padX
                    val halfH = (pBounds.height() / 2f) + padY
                    val frameRect = RectF(-halfW, baseLineY - halfH * 1.2f, halfW, baseLineY + halfH * 0.8f)

                    val boxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = AndroidColor.argb((alphaInt * 0.35f).toInt(), 0, 0, 0)
                        style = Paint.Style.FILL
                    }
                    val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.accentColor.toArgb()
                        alpha = alphaInt
                        style = Paint.Style.STROKE
                        strokeWidth = 2f * scale
                    }
                    canvas.drawRoundRect(frameRect, 4f * scale, 4f * scale, boxBg)
                    canvas.drawRoundRect(frameRect, 4f * scale, 4f * scale, boxStroke)
                    canvas.drawText(layer.primaryText.uppercase(), 0f, baseLineY, primaryPaint)

                    if (layer.subtitleText.isNotBlank()) {
                        val subY = frameRect.bottom + 16f * scale
                        canvas.drawText(layer.subtitleText.uppercase(), 0f, subY, subPaint)
                        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = layer.accentColor.toArgb()
                            alpha = (alphaInt * 0.7f).toInt()
                            strokeWidth = 1.2f * scale
                        }
                        val subBounds = Rect()
                        subPaint.getTextBounds(layer.subtitleText.uppercase(), 0, layer.subtitleText.length, subBounds)
                        val subHalfW = subBounds.width() / 2f
                        canvas.drawLine(-subHalfW - 24f * scale, subY - 4f * scale, -subHalfW - 8f * scale, subY - 4f * scale, linePaint)
                        canvas.drawLine(subHalfW + 8f * scale, subY - 4f * scale, subHalfW + 24f * scale, subY - 4f * scale, linePaint)
                    }
                }

                FcpTitlePresetType.ABSTRACT_BADGE -> {
                    val fm = primaryPaint.fontMetrics
                    val baseLineY = -(fm.ascent + fm.descent) / 2f
                    val pBounds = Rect()
                    primaryPaint.getTextBounds(layer.primaryText.uppercase(), 0, layer.primaryText.length, pBounds)
                    val padX = 22f * scale
                    val padY = 12f * scale
                    val halfW = (pBounds.width() / 2f) + padX
                    val halfH = (pBounds.height() / 2f) + padY
                    val badgeRect = RectF(-halfW, baseLineY - halfH * 1.2f, halfW, baseLineY + halfH * 0.8f)

                    val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.accentColor.toArgb()
                        alpha = alphaInt
                        style = Paint.Style.FILL
                        setShadowLayer(8f * scale, 0f, 4f * scale, AndroidColor.argb((alphaInt * 0.6f).toInt(), 0, 0, 0))
                    }
                    canvas.drawRoundRect(badgeRect, 8f * scale, 8f * scale, badgeBg)
                    canvas.drawText(layer.primaryText.uppercase(), 0f, baseLineY, primaryPaint)

                    if (layer.subtitleText.isNotBlank()) {
                        val subY = badgeRect.bottom + 16f * scale
                        canvas.drawText(layer.subtitleText.uppercase(), 0f, subY, subPaint)
                    }
                }

                FcpTitlePresetType.CYBER_NEON -> {
                    val fm = primaryPaint.fontMetrics
                    val baseLineY = -(fm.ascent + fm.descent) / 2f
                    val pBounds = Rect()
                    primaryPaint.getTextBounds(layer.primaryText.uppercase(), 0, layer.primaryText.length, pBounds)
                    val padX = 20f * scale
                    val padY = 12f * scale
                    val halfW = (pBounds.width() / 2f) + padX
                    val halfH = (pBounds.height() / 2f) + padY
                    val neonRect = RectF(-halfW, baseLineY - halfH * 1.2f, halfW, baseLineY + halfH * 0.8f)

                    val neonBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = AndroidColor.argb((alphaInt * 0.85f).toInt(), 0x0F, 0x17, 0x2A)
                        style = Paint.Style.FILL
                    }
                    val neonStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(-halfW, 0f, halfW, 0f, layer.textColor.toArgb(), layer.accentColor.toArgb(), Shader.TileMode.CLAMP)
                        alpha = alphaInt
                        style = Paint.Style.STROKE
                        strokeWidth = 2f * scale
                    }
                    canvas.drawRoundRect(neonRect, 12f * scale, 12f * scale, neonBg)
                    canvas.drawRoundRect(neonRect, 12f * scale, 12f * scale, neonStroke)

                    val glowPaint = Paint(primaryPaint).apply {
                        setShadowLayer(14f * scale, 0f, 0f, layer.textColor.toArgb())
                    }
                    canvas.drawText(layer.primaryText.uppercase(), 0f, baseLineY, glowPaint)

                    if (layer.subtitleText.isNotBlank()) {
                        val subY = neonRect.bottom - 4f * scale
                        canvas.drawText(layer.subtitleText.uppercase(), 0f, subY, subPaint)
                    }
                }

                FcpTitlePresetType.MINIMAL_CLEAN -> {
                    val fm = primaryPaint.fontMetrics
                    val baseLineY = -(fm.ascent + fm.descent) / 2f
                    val cleanPaint = Paint(primaryPaint).apply {
                        textSize = primarySizePx * 0.8f
                        textAlign = Paint.Align.LEFT
                    }
                    val cleanSubPaint = Paint(subPaint).apply {
                        textAlign = Paint.Align.LEFT
                    }
                    val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.accentColor.toArgb()
                        alpha = alphaInt
                        style = Paint.Style.FILL
                    }

                    val pBounds = Rect()
                    cleanPaint.getTextBounds(layer.primaryText, 0, layer.primaryText.length, pBounds)
                    val totalW = pBounds.width() + 20f * scale
                    val startX = -totalW / 2f

                    // Vertical accent indicator bar
                    val barRect = RectF(startX, baseLineY - primarySizePx * 0.7f, startX + 4f * scale, baseLineY + 6f * scale)
                    canvas.drawRoundRect(barRect, 2f * scale, 2f * scale, barPaint)

                    canvas.drawText(layer.primaryText, startX + 12f * scale, baseLineY - 2f * scale, cleanPaint)
                    if (layer.subtitleText.isNotBlank()) {
                        canvas.drawText(layer.subtitleText, startX + 12f * scale, baseLineY + primarySizePx * 0.42f, cleanSubPaint)
                    }
                }

                else -> {
                    // SCRIPT_HEADLINE, MODERN_FOLD, DESIGN_CONTRAST, RETRO_VHS, LUXURY_SERIF, KINETIC_PUNCH
                    val fm = primaryPaint.fontMetrics
                    val baseLineY = -(fm.ascent + fm.descent) / 2f
                    canvas.drawText(layer.primaryText, 0f, baseLineY, primaryPaint)

                    if (layer.scriptText.isNotBlank()) {
                        canvas.save()
                        canvas.translate(0f, -primarySizePx * 0.5f)
                        canvas.rotate(-5f)
                        canvas.drawText(layer.scriptText, 0f, 0f, scriptPaint)
                        canvas.restore()
                    }

                    if (layer.subtitleText.isNotBlank()) {
                        val subY = baseLineY + primarySizePx * 0.55f
                        canvas.drawText(layer.subtitleText, 0f, subY, subPaint)
                    }
                }
            }

            canvas.restore()
        }

        bitmap
    } catch (t: Throwable) {
        Log.e(TAG, "renderTextLayersToBitmap failed or out of memory", t)
        null
    }
}

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

    suspend fun exportTimeline(
        context: Context,
        clips: List<FcpClip>,
        customAudio: FcpCustomAudio?,
        textLayers: List<FcpTextLayer> = emptyList(),
        canvasBackground: FcpCanvasBackground = FcpCanvasBackground(),
        aspectRatio: FcpAspectRatio,
        res: String,
        fps: String,
        onProgress: (Float, String) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        isCancelled = false
        if (clips.isEmpty()) {
            Log.e(TAG, "exportTimeline: clips list is empty")
            return@withContext null
        }

        val outputFile = File(context.cacheDir, "pro_export_${System.currentTimeMillis()}.mp4")
        var overlayPngFile: File? = null
        val localFileCache = mutableMapOf<String, File>()

        try {
            if (isCancelled || !isActive) return@withContext null
            onProgress(0.10f, "Analyzing timeline & ${clips.size} clip(s)...")

            // 1. Calculate target dimensions based on resolution & aspect ratio
            val baseDim = when (res.lowercase()) {
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

            // 2. Resolve media files to local disk
            onProgress(0.18f, "Resolving video inputs...")

            fun getLocalFile(uri: Uri, isAudio: Boolean = false): File? {
                val key = uri.toString()
                localFileCache[key]?.let { return it }

                if (uri.scheme == "file") {
                    val f = File(uri.path ?: "")
                    if (f.exists()) {
                        localFileCache[key] = f
                        return f
                    }
                }

                val ext = if (isAudio) ".mp3" else ".mp4"
                val tempFile = File(context.cacheDir, "fcp_input_${UUID.randomUUID().toString().take(8)}$ext")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0L) {
                        localFileCache[key] = tempFile
                        return tempFile
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cache uri $uri", e)
                }
                return null
            }

            // Map each clip to a local file
            val clipFiles = clips.mapIndexed { idx, clip ->
                val f = getLocalFile(clip.uri)
                if (f == null) {
                    Log.e(TAG, "Clip $idx file could not be resolved: ${clip.uri}")
                    return@withContext null
                }
                f
            }

            val customAudioFile = customAudio?.let { getLocalFile(it.uri, isAudio = true) }

            // 3. Inspect audio presence & probe actual duration for each clip
            val videoMetadataList = clips.indices.map { i ->
                val clip = clips[i]
                val f = clipFiles[i]
                val isImage = try {
                    val type = context.contentResolver.getType(clip.uri)
                    type?.startsWith("image/") == true
                } catch (_: Throwable) { false }

                if (isImage) {
                    Pair(false, 0f)
                } else {
                    var hasAudio = false
                    val extractor = android.media.MediaExtractor()
                    try {
                        extractor.setDataSource(f.absolutePath)
                        for (t in 0 until extractor.trackCount) {
                            val format = extractor.getTrackFormat(t)
                            val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                            if (mime.startsWith("audio/")) {
                                hasAudio = true
                                break
                            }
                        }
                    } catch (_: Throwable) {
                    } finally {
                        try { extractor.release() } catch (_: Throwable) {}
                    }

                    var probedDurationSec = 0f
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(f.absolutePath)
                        val durMsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val ms = durMsStr?.toLongOrNull() ?: 0L
                        if (ms > 0L) probedDurationSec = ms / 1000f
                    } catch (_: Throwable) {
                    } finally {
                        try { retriever.release() } catch (_: Throwable) {}
                    }

                    Pair(hasAudio, probedDurationSec)
                }
            }

            // 4. Build FFmpeg command
            onProgress(0.30f, "Building timeline composition...")
            val cmdArgs = mutableListOf<String>()
            cmdArgs.add("ffmpeg")
            cmdArgs.add("-y")
            val numCores = Runtime.getRuntime().availableProcessors().coerceIn(4, 16)
            cmdArgs.add("-threads")
            cmdArgs.add(numCores.toString())
            cmdArgs.add("-filter_threads")
            cmdArgs.add(numCores.toString())

            // Add input arguments
            clips.indices.forEach { i ->
                val clip = clips[i]
                val f = clipFiles[i]
                val (hasAudio, _) = videoMetadataList[i]
                val isImage = try {
                    val type = context.contentResolver.getType(clip.uri)
                    type?.startsWith("image/") == true
                } catch (_: Throwable) { false }

                if (isImage) {
                    val dur = clip.trimmedDurationSec
                    cmdArgs.add("-loop")
                    cmdArgs.add("1")
                    cmdArgs.add("-t")
                    cmdArgs.add(String.format(Locale.US, "%.3f", dur))
                    cmdArgs.add("-framerate")
                    cmdArgs.add(targetFps.toString())
                }
                cmdArgs.add("-i")
                cmdArgs.add(f.absolutePath)
            }

            val customAudioInputIndex = if (customAudioFile != null) {
                cmdArgs.add("-i")
                cmdArgs.add(customAudioFile.absolutePath)
                clips.size
            } else null

            // Render visible Title & Text Layers into a transparent overlay PNG
            val overlayBitmap = renderTextLayersToBitmap(context, textLayers, finalW, finalH)
            overlayPngFile = if (overlayBitmap != null) {
                val f = File(context.cacheDir, "fcp_text_overlay_${UUID.randomUUID().toString().take(8)}.png")
                try {
                    FileOutputStream(f).use { out ->
                        overlayBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    overlayBitmap.recycle()
                    if (f.exists() && f.length() > 0L) f else null
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write text overlay PNG", e)
                    null
                }
            } else null

            val textOverlayInputIndex = if (overlayPngFile != null) {
                cmdArgs.add("-i")
                cmdArgs.add(overlayPngFile.absolutePath)
                clips.size + (if (customAudioInputIndex != null) 1 else 0)
            } else null

            // Build filter graph
            val filterGraph = StringBuilder()
            val concatVideoLabels = mutableListOf<String>()
            val concatAudioLabels = mutableListOf<String>()

            clips.indices.forEach { i ->
                val clip = clips[i]
                val (hasAudio, probedDur) = videoMetadataList[i]
                val maxSourceDur = maxOf(probedDur, clip.durationSec, clip.trimEndSec, 10f)
                val sTime = clip.trimStartSec.coerceIn(0f, (maxSourceDur - 0.1f).coerceAtLeast(0f))
                val eTimeRaw = if (clip.trimEndSec > sTime + 0.1f) clip.trimEndSec else maxSourceDur
                val eTime = eTimeRaw.coerceIn(sTime + 0.1f, maxSourceDur)
                val dur = (eTime - sTime).coerceAtLeast(0.1f)
                val speed = clip.speed.coerceIn(0.25f, 4.0f)
                val effectiveDur = dur / speed

                val isImage = try {
                    val type = context.contentResolver.getType(clip.uri)
                    type?.startsWith("image/") == true
                } catch (_: Throwable) { false }

                // Video filter: trim, speed, crop (zoom/pan), scale, pad, fps
                val vFilter = StringBuilder()
                if (!isImage) {
                    val ptsScale = 1.0f / speed
                    vFilter.append(String.format(Locale.US, "trim=start=%.3f:end=%.3f,setpts=%.4f*(PTS-STARTPTS),", sTime, eTime, ptsScale))
                }

                // Precision Zoom & Crop from Preview screen framing
                if (clip.scale > 1.02f) {
                    val s = clip.scale.coerceIn(1.02f, 5.0f)
                    val normPanX = (clip.panX / 360f).coerceIn(-0.6f, 0.6f)
                    val normPanY = (clip.panY / 480f).coerceIn(-0.6f, 0.6f)
                    val xExpr = String.format(Locale.US, "min(max(0,(iw-ow)/2-(%.4f*iw)),iw-ow)", normPanX)
                    val yExpr = String.format(Locale.US, "min(max(0,(ih-oh)/2-(%.4f*ih)),ih-oh)", normPanY)
                    vFilter.append(String.format(Locale.US, "crop=w=iw/%.3f:h=ih/%.3f:x='%s':y='%s',", s, s, xExpr, yExpr))
                }

                // Determine letterbox / canvas background color
                val padColorHex = if (canvasBackground.type == FcpCanvasBgType.COLOR && canvasBackground.solidColor != androidx.compose.ui.graphics.Color.Black) {
                    val argb = canvasBackground.solidColor.toArgb()
                    val r = (argb shr 16) and 0xFF
                    val g = (argb shr 8) and 0xFF
                    val b = argb and 0xFF
                    String.format(Locale.US, "0x%02X%02X%02X", r, g, b)
                } else {
                    "black"
                }

                // Uniform scale and letterboxing
                vFilter.append("scale=$finalW:$finalH:force_original_aspect_ratio=decrease:flags=bilinear,")
                vFilter.append("pad=$finalW:$finalH:(ow-iw)/2:(oh-ih)/2:$padColorHex,")
                vFilter.append("setsar=1,fps=$targetFps")

                // 1. Color adjustments (exposure, contrast, saturation)
                if (clip.exposure != 0f || clip.contrast != 100f || clip.saturation != 100f) {
                    val b = String.format(Locale.US, "%.3f", clip.exposure / 100f)
                    val c = String.format(Locale.US, "%.3f", clip.contrast / 100f)
                    val s = String.format(Locale.US, "%.3f", clip.saturation / 100f)
                    vFilter.append(",eq=brightness=$b:contrast=$c:saturation=$s")
                }

                // 2. White Balance (Temperature & Tint)
                if (clip.temperature != 0f || clip.tint != 0f) {
                    val tempNorm = (clip.temperature / 50f).coerceIn(-1f, 1f)
                    val tintNorm = (clip.tint / 50f).coerceIn(-1f, 1f)
                    val rBal = String.format(Locale.US, "%.3f", (tempNorm * 0.20f + tintNorm * 0.10f).coerceIn(-0.8f, 0.8f))
                    val gBal = String.format(Locale.US, "%.3f", (-tintNorm * 0.20f).coerceIn(-0.8f, 0.8f))
                    val bBal = String.format(Locale.US, "%.3f", (-tempNorm * 0.20f + tintNorm * 0.10f).coerceIn(-0.8f, 0.8f))
                    vFilter.append(",colorbalance=rs=$rBal:gs=$gBal:bs=$bBal:rm=$rBal:gm=$gBal:bm=$bBal")
                }

                // 3. Cinematic LUT Presets
                when (clip.lut) {
                    FcpLutPreset.TEAL_ORANGE -> vFilter.append(",colorbalance=rs=0.15:gs=-0.02:bs=-0.12:rm=0.10:bm=-0.10")
                    FcpLutPreset.CYBER -> vFilter.append(",colorbalance=rm=0.15:gm=-0.05:bm=0.25:rh=0.10:bh=0.20")
                    FcpLutPreset.NOIR -> vFilter.append(",hue=s=0,eq=contrast=1.35:brightness=-0.02")
                    FcpLutPreset.WARM_GOLD -> vFilter.append(",colorbalance=rh=0.20:gh=0.10:bh=-0.15:rm=0.10:gm=0.05")
                    FcpLutPreset.COOL_NORDIC -> vFilter.append(",colorbalance=rs=-0.10:bs=0.20:rm=-0.05:bm=0.15")
                    FcpLutPreset.VINTAGE_FADE -> vFilter.append(",colorbalance=rs=0.08:gs=0.04:bs=-0.05,eq=contrast=0.92:brightness=0.04")
                    FcpLutPreset.EMERALD -> vFilter.append(",colorbalance=gs=0.12:gm=0.08:bm=-0.05")
                    FcpLutPreset.NONE -> {}
                }

                // 4. Visual Effects
                val effInt = clip.effectIntensity.coerceIn(0.1f, 1.0f)
                when (clip.effect) {
                    FcpVisualEffect.VIGNETTE_MOOD -> {
                        val angle = String.format(Locale.US, "%.3f", (Math.PI / 4.0) * effInt)
                        vFilter.append(",vignette=angle=$angle")
                    }
                    FcpVisualEffect.FILM_GRAIN -> {
                        val noiseStr = (18 * effInt).toInt().coerceIn(6, 32)
                        vFilter.append(",noise=alls=$noiseStr:allf=t")
                    }
                    FcpVisualEffect.VHS_RETRO -> {
                        vFilter.append(",eq=contrast=1.15:saturation=1.25,noise=c0s=16:allf=t,colorbalance=rs=0.08:bs=-0.06")
                    }
                    FcpVisualEffect.GLITCH -> {
                        vFilter.append(",noise=c0s=32:c1s=22:allf=t,hue=H=PI/16,eq=contrast=1.25:saturation=1.35")
                    }
                    FcpVisualEffect.CINEMA_BLOOM -> {
                        vFilter.append(",eq=brightness=0.08:contrast=1.15:saturation=1.12")
                    }
                    FcpVisualEffect.PRISM_SPLIT -> {
                        vFilter.append(",colorbalance=rs=0.15:gs=-0.05:bs=0.20:rm=0.10:bm=-0.10,eq=contrast=1.18:saturation=1.28")
                    }
                    FcpVisualEffect.NOIR_CONTRAST -> {
                        vFilter.append(",hue=s=0,eq=contrast=1.38:brightness=-0.04")
                    }
                    FcpVisualEffect.ANAMORPHIC_FLARE -> {
                        vFilter.append(",colorbalance=rh=-0.05:gh=0.02:bh=0.26:rm=-0.02:bm=0.16,eq=contrast=1.15:saturation=1.18")
                    }
                    FcpVisualEffect.NONE -> {}
                }

                // 5. Dedicated Motion & Camera Animation Effects (Shake, Pulse, Zoom, Pan, Strobe, Wobble)
                val animInt = clip.animationIntensity.coerceIn(0.1f, 1.0f)
                when (clip.animationEffect) {
                    FcpClipAnimation.SHAKE -> {
                        val s = 1.15f
                        val xExpr = String.format(Locale.US, "(iw-ow)/2 + (sin(t*38)*16 + cos(t*55)*8)*%.2f", animInt)
                        val yExpr = String.format(Locale.US, "(ih-oh)/2 + (cos(t*45)*14 + sin(t*33)*7)*%.2f", animInt)
                        vFilter.append(String.format(Locale.US, ",scale=w='iw*%.2f':h='ih*%.2f':eval=frame,crop=w=iw/%.2f:h=ih/%.2f:x='%s':y='%s'", s, s, s, s, xExpr, yExpr))
                    }
                    FcpClipAnimation.BEAT_PULSE -> {
                        val durExpr = String.format(Locale.US, ",scale=w='iw*(1.0 + 0.12*pow(1-mod(t,0.6)/0.6, 2)*%.2f)':h='ih*(1.0 + 0.12*pow(1-mod(t,0.6)/0.6, 2)*%.2f)':eval=frame,crop=w=%d:h=%d", animInt, animInt, finalW, finalH)
                        vFilter.append(durExpr)
                    }
                    FcpClipAnimation.ZOOM_IN -> {
                        val durExpr = String.format(Locale.US, ",scale=w='iw*(1.0 + 0.22*(t/%.3f)*%.2f)':h='ih*(1.0 + 0.22*(t/%.3f)*%.2f)':eval=frame,crop=w=%d:h=%d", effectiveDur, animInt, effectiveDur, animInt, finalW, finalH)
                        vFilter.append(durExpr)
                    }
                    FcpClipAnimation.ZOOM_OUT -> {
                        val durExpr = String.format(Locale.US, ",scale=w='iw*(1.0 + 0.22*(1.0 - t/%.3f)*%.2f)':h='ih*(1.0 + 0.22*(1.0 - t/%.3f)*%.2f)':eval=frame,crop=w=%d:h=%d", effectiveDur, animInt, effectiveDur, animInt, finalW, finalH)
                        vFilter.append(durExpr)
                    }
                    FcpClipAnimation.CAMERA_JITTER -> {
                        val s = 1.08f
                        val xExpr = String.format(Locale.US, "(iw-ow)/2 + (sin(t*3.4)*9 + cos(t*5.1)*5)*%.2f", animInt)
                        val yExpr = String.format(Locale.US, "(ih-oh)/2 + (cos(t*4.2)*8 + sin(t*2.8)*4)*%.2f", animInt)
                        vFilter.append(String.format(Locale.US, ",scale=w='iw*%.2f':h='ih*%.2f':eval=frame,crop=w=iw/%.2f:h=ih/%.2f:x='%s':y='%s'", s, s, s, s, xExpr, yExpr))
                    }
                    FcpClipAnimation.PAN_LEFT -> {
                        val s = 1.15f
                        val xExpr = String.format(Locale.US, "((iw-ow)*(1.0 - t/%.3f))", effectiveDur)
                        vFilter.append(String.format(Locale.US, ",scale=w='iw*%.2f':h='ih*%.2f':eval=frame,crop=w=iw/%.2f:h=ih/%.2f:x='%s':y=(ih-oh)/2", s, s, s, s, xExpr))
                    }
                    FcpClipAnimation.PAN_RIGHT -> {
                        val s = 1.15f
                        val xExpr = String.format(Locale.US, "((iw-ow)*(t/%.3f))", effectiveDur)
                        vFilter.append(String.format(Locale.US, ",scale=w='iw*%.2f':h='ih*%.2f':eval=frame,crop=w=iw/%.2f:h=ih/%.2f:x='%s':y=(ih-oh)/2", s, s, s, s, xExpr))
                    }
                    FcpClipAnimation.FLASH_STROBE -> {
                        vFilter.append(String.format(Locale.US, ",eq=brightness='0.22*pow(1-mod(t,0.4)/0.4, 3)*%.2f':enable='gte(mod(t,0.4),0)'", animInt))
                    }
                    FcpClipAnimation.WOBBLE -> {
                        vFilter.append(String.format(Locale.US, ",rotate='(sin(t*4.5)*0.04*%.2f)':ow=iw:oh=ih:c=black", animInt))
                    }
                    FcpClipAnimation.NONE -> {}
                }

                // 6. Boundary Transitions Calculation
                val transToNext = if (i < clips.size - 1) clip.transitionToNext else FcpTransitionType.NONE
                val nextTransDur = clip.transitionDurationSec.coerceIn(0.15f, 2.0f)
                val halfTrans = (nextTransDur / 2f).coerceAtMost(effectiveDur / 2.2f)
                val fadeStart = (effectiveDur - halfTrans).coerceAtLeast(0f)

                val prevTrans = if (i > 0) clips[i - 1].transitionToNext else FcpTransitionType.NONE
                val prevTransDur = if (i > 0) clips[i - 1].transitionDurationSec.coerceIn(0.15f, 2.0f) else 0f
                val prevHalfTrans = (prevTransDur / 2f).coerceAtMost(effectiveDur / 2.2f)

                // 5a. Horizontal Slide & Push Transitions
                val hasIncomingHorizSlide = prevTrans in listOf(
                    FcpTransitionType.PUSH_LEFT, FcpTransitionType.SLIDE_LEFT,
                    FcpTransitionType.PUSH_RIGHT, FcpTransitionType.WIPE_RIGHT,
                    FcpTransitionType.COVER_SLIDE
                ) && prevHalfTrans > 0.05f

                val hasOutgoingHorizSlide = transToNext in listOf(
                    FcpTransitionType.PUSH_LEFT, FcpTransitionType.SLIDE_LEFT,
                    FcpTransitionType.PUSH_RIGHT, FcpTransitionType.WIPE_RIGHT,
                    FcpTransitionType.PARALLAX, FcpTransitionType.COVER_SLIDE
                ) && halfTrans > 0.05f

                if (hasIncomingHorizSlide || hasOutgoingHorizSlide) {
                    val inExpr = when (prevTrans) {
                        FcpTransitionType.PUSH_RIGHT, FcpTransitionType.WIPE_RIGHT ->
                            String.format(Locale.US, "2*%d-%d*t/%.3f", finalW, finalW, prevHalfTrans)
                        FcpTransitionType.PUSH_LEFT, FcpTransitionType.SLIDE_LEFT, FcpTransitionType.COVER_SLIDE ->
                            String.format(Locale.US, "%d*t/%.3f", finalW, prevHalfTrans)
                        else -> "$finalW"
                    }

                    val outExpr = when (transToNext) {
                        FcpTransitionType.PUSH_RIGHT, FcpTransitionType.WIPE_RIGHT ->
                            String.format(Locale.US, "%d-%d*(t-%.3f)/%.3f", finalW, finalW, fadeStart, halfTrans)
                        FcpTransitionType.PUSH_LEFT, FcpTransitionType.SLIDE_LEFT ->
                            String.format(Locale.US, "%d+%d*(t-%.3f)/%.3f", finalW, finalW, fadeStart, halfTrans)
                        FcpTransitionType.COVER_SLIDE ->
                            String.format(Locale.US, "%d-%.1f*(t-%.3f)/%.3f", finalW, finalW * 0.25f, fadeStart, halfTrans)
                        FcpTransitionType.PARALLAX ->
                            String.format(Locale.US, "%d-%.1f*(t-%.3f)/%.3f", finalW, finalW * 0.8f, fadeStart, halfTrans)
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

                    vFilter.append(String.format(Locale.US, ",pad=3*%d:%d:%d:0:black,crop=w=%d:h=%d:x='%s':y=0", finalW, finalH, finalW, finalW, finalH, xExpr))
                }

                // 5b. Vertical Slide & Push Transitions
                val hasIncomingVertSlide = prevTrans in listOf(FcpTransitionType.PUSH_UP, FcpTransitionType.PUSH_DOWN) && prevHalfTrans > 0.05f
                val hasOutgoingVertSlide = transToNext in listOf(FcpTransitionType.PUSH_UP, FcpTransitionType.PUSH_DOWN) && halfTrans > 0.05f

                if (hasIncomingVertSlide || hasOutgoingVertSlide) {
                    val inYExpr = when (prevTrans) {
                        FcpTransitionType.PUSH_DOWN ->
                            String.format(Locale.US, "2*%d-%d*t/%.3f", finalH, finalH, prevHalfTrans)
                        FcpTransitionType.PUSH_UP ->
                            String.format(Locale.US, "%d*t/%.3f", finalH, prevHalfTrans)
                        else -> "$finalH"
                    }

                    val outYExpr = when (transToNext) {
                        FcpTransitionType.PUSH_DOWN ->
                            String.format(Locale.US, "%d-%d*(t-%.3f)/%.3f", finalH, finalH, fadeStart, halfTrans)
                        FcpTransitionType.PUSH_UP ->
                            String.format(Locale.US, "%d+%d*(t-%.3f)/%.3f", finalH, finalH, fadeStart, halfTrans)
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

                    vFilter.append(String.format(Locale.US, ",pad=%d:3*%d:0:%d:black,crop=w=%d:h=%d:x=0:y='%s'", finalW, finalH, finalH, finalW, finalH, yExpr))
                }

                // 5c. Outgoing Transitions on tail of clip i (All 25 Transitions without xfade)
                if (i < clips.size - 1 && transToNext != FcpTransitionType.NONE && halfTrans > 0.05f) {
                    when (transToNext) {
                        FcpTransitionType.DIP_TO_BLACK -> {
                            vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f:c=black", fadeStart, halfTrans))
                        }
                        FcpTransitionType.DIP_TO_WHITE -> {
                            vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f:c=white", fadeStart, halfTrans))
                        }
                        FcpTransitionType.FILM_BLOOM -> {
                            vFilter.append(String.format(Locale.US, ",scale=w='iw*(1+0.06*(t-%.3f)/%.3f)':h='ih*(1+0.06*(t-%.3f)/%.3f)':eval=frame,crop=w=%d:h=%d,fade=t=out:st=%.3f:d=%.3f:c=white,eq=brightness=0.18:contrast=1.15:saturation=1.20:enable='gte(t,%.3f)',colorbalance=rs=0.12:gs=0.06:bs=-0.08:enable='gte(t,%.3f)'", fadeStart, halfTrans, fadeStart, halfTrans, finalW, finalH, fadeStart, halfTrans, fadeStart, fadeStart))
                        }
                        FcpTransitionType.CROSS_DISSOLVE, FcpTransitionType.CROSSFADE -> {
                            vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f:c=black,eq=contrast=0.96:brightness=0.03:enable='gte(t,%.3f)'", fadeStart, halfTrans, fadeStart))
                        }
                        FcpTransitionType.GLITCH -> {
                            vFilter.append(String.format(Locale.US, ",noise=c0s=45:c1s=35:allf=t:enable='gte(t,%.3f)',hue=H='PI*sin(20*t)':enable='gte(t,%.3f)',eq=contrast=1.40:brightness=0.08:enable='gte(t,%.3f)',colorbalance=rs=0.18:bs=-0.15:enable='gte(t,%.3f)'", fadeStart, fadeStart, fadeStart, fadeStart))
                        }
                        FcpTransitionType.ZOOM_IN -> {
                            vFilter.append(String.format(Locale.US, ",scale=w='iw*(1+1.8*(t-%.3f)/%.3f)':h='ih*(1+1.8*(t-%.3f)/%.3f)':eval=frame,crop=w=%d:h=%d,avgblur=sizeX=8:enable='gte(t,%.3f)',fade=t=out:st=%.3f:d=%.3f:c=black", fadeStart, halfTrans, fadeStart, halfTrans, finalW, finalH, fadeStart, fadeStart, halfTrans))
                        }
                        FcpTransitionType.ZOOM_OUT -> {
                            vFilter.append(String.format(Locale.US, ",scale=w='max(2,iw*(1-0.65*(t-%.3f)/%.3f))':h='max(2,ih*(1-0.65*(t-%.3f)/%.3f))':eval=frame,pad=%d:%d:'(%d-iw)/2':'(%d-ih)/2':black,fade=t=out:st=%.3f:d=%.3f:c=black", fadeStart, halfTrans, fadeStart, halfTrans, finalW, finalH, finalW, finalH, fadeStart, halfTrans))
                        }
                        FcpTransitionType.WHIP_SPIN -> {
                            vFilter.append(String.format(Locale.US, ",rotate='2*PI*(t-%.3f)/%.3f':ow=iw:oh=ih:c=black:enable='gte(t,%.3f)',avgblur=sizeX=10:enable='gte(t,%.3f)',scale=w='iw*(1+0.45*(t-%.3f)/%.3f)':h='ih*(1+0.45*(t-%.3f)/%.3f)':eval=frame,crop=w=%d:h=%d,fade=t=out:st=%.3f:d=%.3f:c=black", fadeStart, halfTrans, fadeStart, fadeStart, fadeStart, halfTrans, fadeStart, halfTrans, finalW, finalH, fadeStart, halfTrans))
                        }
                        FcpTransitionType.WIPE_HORIZONTAL -> {
                            vFilter.append(String.format(Locale.US, ",crop=w='max(2,%d*(1-(t-%.3f)/%.3f))':h=%d:x=0:y=0,pad=%d:%d:0:0:black", finalW, fadeStart, halfTrans, finalH, finalW, finalH))
                        }
                        FcpTransitionType.WIPE_DIAGONAL -> {
                            vFilter.append(String.format(Locale.US, ",pad=2*%d:2*%d:0:0:black,crop=w=%d:h=%d:x='min(%d,%d*(t-%.3f)/%.3f)':y='min(%d,%d*(t-%.3f)/%.3f)'", finalW, finalH, finalW, finalH, finalW, finalW, fadeStart, halfTrans, finalH, finalH, fadeStart, halfTrans))
                        }
                        FcpTransitionType.CLOCK_SWEEP -> {
                            vFilter.append(String.format(Locale.US, ",rotate='2*PI*(t-%.3f)/%.3f':ow=iw:oh=ih:c=black:enable='gte(t,%.3f)',vignette=angle='max(0.01,(PI/2.0)*(1-(t-%.3f)/%.3f))':enable='gte(t,%.3f)'", fadeStart, halfTrans, fadeStart, fadeStart, halfTrans, fadeStart))
                        }
                        FcpTransitionType.BARN_DOORS -> {
                            vFilter.append(String.format(Locale.US, ",crop=w='max(2,%d*(1-(t-%.3f)/%.3f))':h=%d:x='%d*0.5*(t-%.3f)/%.3f':y=0,pad=%d:%d:'(%d-iw)/2':0:black", finalW, fadeStart, halfTrans, finalH, finalW, fadeStart, halfTrans, finalW, finalH, finalW))
                        }
                        FcpTransitionType.VENETIAN_BLINDS -> {
                            vFilter.append(String.format(Locale.US, ",crop=w=%d:h='max(2,%d*(1-(t-%.3f)/%.3f))':x=0:y=0,pad=%d:%d:0:0:black,avgblur=sizeX=4:enable='gte(t,%.3f)'", finalW, finalH, fadeStart, halfTrans, finalW, finalH, fadeStart))
                        }
                        FcpTransitionType.CIRCLE_IRIS -> {
                            vFilter.append(String.format(Locale.US, ",vignette=angle='max(0.01,(PI/2.0)*(1-(t-%.3f)/%.3f))':x0='w/2':y0='h/2':enable='gte(t,%.3f)'", fadeStart, halfTrans, fadeStart))
                        }
                        FcpTransitionType.PINHOLE_SNAP -> {
                            vFilter.append(String.format(Locale.US, ",vignette=angle='max(0.005,(PI/2.0)*(1-(t-%.3f)/%.3f)*(1-(t-%.3f)/%.3f))':x0='w/2':y0='h/2':enable='gte(t,%.3f)'", fadeStart, halfTrans, fadeStart, halfTrans, fadeStart))
                        }
                        FcpTransitionType.DIAMOND_IRIS -> {
                            vFilter.append(String.format(Locale.US, ",vignette=angle='max(0.01,(PI/2.2)*(1-(t-%.3f)/%.3f))':aspect=1.0:enable='gte(t,%.3f)',crop=w='max(2,%d*(1-(t-%.3f)/%.3f))':h='max(2,%d*(1-(t-%.3f)/%.3f))':x='%d*0.5*(t-%.3f)/%.3f':y='%d*0.5*(t-%.3f)/%.3f',pad=%d:%d:'(%d-iw)/2':'(%d-ih)/2':black", fadeStart, halfTrans, fadeStart, finalW, fadeStart, halfTrans, finalH, fadeStart, halfTrans, finalW, fadeStart, halfTrans, finalH, fadeStart, halfTrans, finalW, finalH, finalW, finalH))
                        }
                        FcpTransitionType.QUAD_SHUTTER -> {
                            vFilter.append(String.format(Locale.US, ",crop=w='max(2,%d*(1-(t-%.3f)/%.3f))':h='max(2,%d*(1-(t-%.3f)/%.3f))':x='%d*0.5*(t-%.3f)/%.3f':y='%d*0.5*(t-%.3f)/%.3f',pad=%d:%d:'(%d-iw)/2':'(%d-ih)/2':black", finalW, fadeStart, halfTrans, finalH, fadeStart, halfTrans, finalW, fadeStart, halfTrans, finalH, fadeStart, halfTrans, finalW, finalH, finalW, finalH))
                        }
                        FcpTransitionType.CUBE_3D -> {
                            vFilter.append(String.format(Locale.US, ",scale=w='max(2,%d*cos((PI/2)*(t-%.3f)/%.3f))':h=%d:eval=frame,pad=%d:%d:'(%d-iw)/2':0:black,eq=brightness='-0.12*(t-%.3f)/%.3f':enable='gte(t,%.3f)'", finalW, fadeStart, halfTrans, finalH, finalW, finalH, finalW, fadeStart, halfTrans, fadeStart))
                        }
                        FcpTransitionType.PAGE_FLIP_3D -> {
                            vFilter.append(String.format(Locale.US, ",scale=w='max(2,%d*cos((PI/2)*(t-%.3f)/%.3f)*(1-0.12*(t-%.3f)/%.3f))':h='%d*(1-0.12*(t-%.3f)/%.3f)':eval=frame,pad=%d:%d:'(%d-iw)/2':'(%d-ih)/2':black,avgblur=sizeX=4:enable='gte(t,%.3f)'", finalW, fadeStart, halfTrans, fadeStart, halfTrans, finalH, fadeStart, halfTrans, finalW, finalH, finalW, finalH, fadeStart))
                        }
                        FcpTransitionType.WIPE_LEFT -> {
                            vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f:c=black", fadeStart, halfTrans))
                        }
                        FcpTransitionType.PUSH_LEFT, FcpTransitionType.SLIDE_LEFT, FcpTransitionType.PUSH_RIGHT, FcpTransitionType.WIPE_RIGHT, FcpTransitionType.PARALLAX, FcpTransitionType.COVER_SLIDE, FcpTransitionType.PUSH_UP, FcpTransitionType.PUSH_DOWN -> {
                            // Handled above via horizontal/vertical slide crop
                        }
                        FcpTransitionType.NONE -> {}
                    }
                }

                // 5d. Incoming Transitions on head of clip i (All 25 Transitions without xfade)
                if (i > 0 && prevTrans != FcpTransitionType.NONE && prevHalfTrans > 0.05f) {
                    when (prevTrans) {
                        FcpTransitionType.DIP_TO_BLACK -> {
                            vFilter.append(String.format(Locale.US, ",fade=t=in:st=0:d=%.3f:c=black", prevHalfTrans))
                        }
                        FcpTransitionType.DIP_TO_WHITE -> {
                            vFilter.append(String.format(Locale.US, ",fade=t=in:st=0:d=%.3f:c=white", prevHalfTrans))
                        }
                        FcpTransitionType.FILM_BLOOM -> {
                            vFilter.append(String.format(Locale.US, ",scale=w='iw*(1.06-0.06*t/%.3f)':h='ih*(1.06-0.06*t/%.3f)':eval=frame,crop=w=%d:h=%d,fade=t=in:st=0:d=%.3f:c=white,eq=brightness=0.18:contrast=1.15:saturation=1.20:enable='lte(t,%.3f)',colorbalance=rs=0.12:gs=0.06:bs=-0.08:enable='lte(t,%.3f)'", prevHalfTrans, prevHalfTrans, finalW, finalH, prevHalfTrans, prevHalfTrans, prevHalfTrans))
                        }
                        FcpTransitionType.CROSS_DISSOLVE, FcpTransitionType.CROSSFADE -> {
                            vFilter.append(String.format(Locale.US, ",fade=t=in:st=0:d=%.3f:c=black,eq=contrast=0.96:brightness=0.03:enable='lte(t,%.3f)'", prevHalfTrans, prevHalfTrans))
                        }
                        FcpTransitionType.GLITCH -> {
                            vFilter.append(String.format(Locale.US, ",noise=c0s=45:c1s=35:allf=t:enable='lte(t,%.3f)',hue=H='-PI*sin(20*t)':enable='lte(t,%.3f)',eq=contrast=1.40:brightness=0.08:enable='lte(t,%.3f)',colorbalance=rs=-0.15:bs=0.18:enable='lte(t,%.3f)'", prevHalfTrans, prevHalfTrans, prevHalfTrans, prevHalfTrans))
                        }
                        FcpTransitionType.ZOOM_IN -> {
                            vFilter.append(String.format(Locale.US, ",scale=w='iw*(1.8-0.8*t/%.3f)':h='ih*(1.8-0.8*t/%.3f)':eval=frame,crop=w=%d:h=%d,avgblur=sizeX=10:enable='lte(t,%.3f)',fade=t=in:st=0:d=%.3f:c=black", prevHalfTrans, prevHalfTrans, finalW, finalH, prevHalfTrans, prevHalfTrans))
                        }
                        FcpTransitionType.ZOOM_OUT -> {
                            vFilter.append(String.format(Locale.US, ",scale=w='iw*(1.75-0.75*t/%.3f)':h='ih*(1.75-0.75*t/%.3f)':eval=frame,crop=w=%d:h=%d,fade=t=in:st=0:d=%.3f:c=black", prevHalfTrans, prevHalfTrans, finalW, finalH, prevHalfTrans))
                        }
                        FcpTransitionType.WHIP_SPIN -> {
                            vFilter.append(String.format(Locale.US, ",rotate='-2*PI*(1-t/%.3f)':ow=iw:oh=ih:c=black:enable='lte(t,%.3f)',avgblur=sizeX=10:enable='lte(t,%.3f)',scale=w='iw*(1.45-0.45*t/%.3f)':h='ih*(1.45-0.45*t/%.3f)':eval=frame,crop=w=%d:h=%d,fade=t=in:st=0:d=%.3f:c=black", prevHalfTrans, prevHalfTrans, prevHalfTrans, prevHalfTrans, prevHalfTrans, finalW, finalH, prevHalfTrans))
                        }
                        FcpTransitionType.WIPE_HORIZONTAL -> {
                            vFilter.append(String.format(Locale.US, ",crop=w='max(2,%d*t/%.3f)':h=%d:x=0:y=0,pad=%d:%d:0:0:black", finalW, prevHalfTrans, finalH, finalW, finalH))
                        }
                        FcpTransitionType.WIPE_DIAGONAL -> {
                            vFilter.append(String.format(Locale.US, ",pad=2*%d:2*%d:%d:%d:black,crop=w=%d:h=%d:x='max(0,%d*(1-t/%.3f))':y='max(0,%d*(1-t/%.3f))'", finalW, finalH, finalW, finalH, finalW, finalH, finalW, prevHalfTrans, finalH, prevHalfTrans))
                        }
                        FcpTransitionType.CLOCK_SWEEP -> {
                            vFilter.append(String.format(Locale.US, ",rotate='-2*PI*(1-t/%.3f)':ow=iw:oh=ih:c=black:enable='lte(t,%.3f)',vignette=angle='max(0.01,(PI/2.0)*(t/%.3f))':enable='lte(t,%.3f)'", prevHalfTrans, prevHalfTrans, prevHalfTrans, prevHalfTrans))
                        }
                        FcpTransitionType.BARN_DOORS -> {
                            vFilter.append(String.format(Locale.US, ",crop=w='max(2,%d*t/%.3f)':h=%d:x='%d*0.5*(1-t/%.3f)':y=0,pad=%d:%d:'(%d-iw)/2':0:black", finalW, prevHalfTrans, finalH, finalW, prevHalfTrans, finalW, finalH, finalW))
                        }
                        FcpTransitionType.VENETIAN_BLINDS -> {
                            vFilter.append(String.format(Locale.US, ",crop=w=%d:h='max(2,%d*t/%.3f)':x=0:y=0,pad=%d:%d:0:0:black", finalW, finalH, prevHalfTrans, finalW, finalH))
                        }
                        FcpTransitionType.CIRCLE_IRIS -> {
                            vFilter.append(String.format(Locale.US, ",vignette=angle='max(0.01,(PI/2.0)*(t/%.3f))':x0='w/2':y0='h/2':enable='lte(t,%.3f)'", prevHalfTrans, prevHalfTrans))
                        }
                        FcpTransitionType.PINHOLE_SNAP -> {
                            vFilter.append(String.format(Locale.US, ",vignette=angle='max(0.005,(PI/2.0)*(t/%.3f)*(t/%.3f))':x0='w/2':y0='h/2':enable='lte(t,%.3f)'", prevHalfTrans, prevHalfTrans, prevHalfTrans))
                        }
                        FcpTransitionType.DIAMOND_IRIS -> {
                            vFilter.append(String.format(Locale.US, ",vignette=angle='max(0.01,(PI/2.2)*(t/%.3f))':aspect=1.0:enable='lte(t,%.3f)',crop=w='max(2,%d*t/%.3f)':h='max(2,%d*t/%.3f)':x='%d*0.5*(1-t/%.3f)':y='%d*0.5*(1-t/%.3f)',pad=%d:%d:'(%d-iw)/2':'(%d-ih)/2':black", prevHalfTrans, prevHalfTrans, finalW, prevHalfTrans, finalH, prevHalfTrans, finalW, prevHalfTrans, finalH, prevHalfTrans, finalW, finalH, finalW, finalH))
                        }
                        FcpTransitionType.QUAD_SHUTTER -> {
                            vFilter.append(String.format(Locale.US, ",crop=w='max(2,%d*t/%.3f)':h='max(2,%d*t/%.3f)':x='%d*0.5*(1-t/%.3f)':y='%d*0.5*(1-t/%.3f)',pad=%d:%d:'(%d-iw)/2':'(%d-ih)/2':black", finalW, prevHalfTrans, finalH, prevHalfTrans, finalW, prevHalfTrans, finalH, prevHalfTrans, finalW, finalH, finalW, finalH))
                        }
                        FcpTransitionType.CUBE_3D -> {
                            vFilter.append(String.format(Locale.US, ",scale=w='max(2,%d*sin((PI/2)*t/%.3f))':h=%d:eval=frame,pad=%d:%d:'(%d-iw)/2':0:black,eq=brightness='-0.12*(1-t/%.3f)':enable='lte(t,%.3f)'", finalW, prevHalfTrans, finalH, finalW, finalH, finalW, prevHalfTrans, prevHalfTrans))
                        }
                        FcpTransitionType.PAGE_FLIP_3D -> {
                            vFilter.append(String.format(Locale.US, ",scale=w='max(2,%d*sin((PI/2)*t/%.3f)*(0.88+0.12*t/%.3f))':h='%d*(0.88+0.12*t/%.3f)':eval=frame,pad=%d:%d:'(%d-iw)/2':'(%d-ih)/2':black,avgblur=sizeX=4:enable='lte(t,%.3f)'", finalW, prevHalfTrans, prevHalfTrans, finalH, prevHalfTrans, finalW, finalH, finalW, finalH, prevHalfTrans))
                        }
                        FcpTransitionType.PARALLAX -> {
                            vFilter.append(String.format(Locale.US, ",scale=w='iw*(0.90+0.10*t/%.3f)':h='ih*(0.90+0.10*t/%.3f)':eval=frame,crop=w=%d:h=%d,fade=t=in:st=0:d=%.3f:c=black", prevHalfTrans, prevHalfTrans, finalW, finalH, prevHalfTrans))
                        }
                        FcpTransitionType.WIPE_LEFT -> {
                            vFilter.append(String.format(Locale.US, ",fade=t=in:st=0:d=%.3f:c=black", prevHalfTrans))
                        }
                        FcpTransitionType.PUSH_LEFT, FcpTransitionType.SLIDE_LEFT, FcpTransitionType.PUSH_RIGHT, FcpTransitionType.WIPE_RIGHT, FcpTransitionType.COVER_SLIDE, FcpTransitionType.PUSH_UP, FcpTransitionType.PUSH_DOWN -> {
                            // Handled above via horizontal/vertical slide crop
                        }
                        FcpTransitionType.NONE -> {}
                    }
                }

                filterGraph.append("[$i:v]$vFilter[v_clip_$i];")
                concatVideoLabels.add("[v_clip_$i]")

                // Audio filter: trim, speed, smooth fades, standardized to 44.1kHz stereo
                if (hasAudio) {
                    val aFilter = StringBuilder()
                    aFilter.append(String.format(Locale.US, "atrim=start=%.3f:end=%.3f,asetpts=PTS-STARTPTS", sTime, eTime))
                    if (speed != 1.0f) {
                        var cur = speed.coerceIn(0.25f, 4.0f)
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

                    // Smooth boundary fades
                    val audioFadeOut = if (i < clips.size - 1 && transToNext != FcpTransitionType.NONE) halfTrans.coerceAtMost(0.25f) else 0.05f
                    val audioFadeIn = if (i > 0 && clips[i - 1].transitionToNext != FcpTransitionType.NONE) {
                        val prevTransDur = clips[i - 1].transitionDurationSec.coerceIn(0.1f, 1.5f)
                        (prevTransDur / 2f).coerceAtMost(effectiveDur / 2f).coerceAtMost(0.25f)
                    } else 0.05f

                    if (effectiveDur > 0.2f) {
                        if (audioFadeIn > 0.01f) {
                            aFilter.append(String.format(Locale.US, ",afade=t=in:st=0:d=%.3f", audioFadeIn))
                        }
                        if (audioFadeOut > 0.01f) {
                            val aFadeStart = (effectiveDur - audioFadeOut).coerceAtLeast(0f)
                            aFilter.append(String.format(Locale.US, ",afade=t=out:st=%.3f:d=%.3f", aFadeStart, audioFadeOut))
                        }
                    }

                    // Strict uniform format and duration match guarantee for concat filter
                    aFilter.append(String.format(Locale.US, ",aformat=sample_rates=44100:channel_layouts=stereo,aresample=async=1,apad,atrim=start=0:end=%.3f,asetpts=PTS-STARTPTS", effectiveDur))
                    filterGraph.append("[$i:a]$aFilter[a_clip_$i];")
                    concatAudioLabels.add("[a_clip_$i]")
                } else {
                    // Generate silent audio of exact matching duration & matching 44.1kHz stereo layout
                    filterGraph.append(String.format(Locale.US, "anullsrc=channel_layout=stereo:sample_rate=44100,atrim=start=0:end=%.3f,asetpts=PTS-STARTPTS[a_clip_$i];", effectiveDur))
                    concatAudioLabels.add("[a_clip_$i]")
                }
            }

            var finalVideoStream: String
            var finalAudioStream: String

            if (clips.size == 1) {
                // Single clip timeline
                finalVideoStream = "[v_clip_0]"
                finalAudioStream = "[a_clip_0]"
            } else {
                // Multi-clip timeline: concatenate all video and audio tracks
                val nClips = clips.size
                val concatBuilder = StringBuilder()
                for (i in 0 until nClips) {
                    concatBuilder.append(concatVideoLabels[i])
                    concatBuilder.append(concatAudioLabels[i])
                }
                concatBuilder.append("concat=n=$nClips:v=1:a=1[v_concat][a_concat];")
                filterGraph.append(concatBuilder.toString())
                finalVideoStream = "[v_concat]"
                finalAudioStream = "[a_concat]"
            }

            // Duration limit to protect timeline exact stop
            val totalTimelineDurationSec = clips.sumOf { it.trimmedDurationSec.toDouble() }.toFloat().coerceAtLeast(0.5f)

            // Handle custom soundtrack if present with looped soundtrack support (aloop)
            if (customAudioInputIndex != null && customAudio != null) {
                val vol = customAudio.volume.coerceIn(0f, 1f)
                val muteOrig = customAudio.muteOriginalVideo
                // Safe integer aloop sample allocation: size=2147483647 reference benchmark; allocate proportional to timeline to prevent 8.5GB OOM Linux process termination
                val safeLoopSamples = ((totalTimelineDurationSec + 5f) * 44100).toInt().coerceIn(44100, 26_460_000)
                val loopFilter = if (customAudio.loopAudio) "aloop=loop=-1:size=$safeLoopSamples," else ""

                val trimStart = customAudio.trimStartSec.coerceAtLeast(0f)
                val trimEnd = if (customAudio.trimEndSec > trimStart) customAudio.trimEndSec else (trimStart + totalTimelineDurationSec)
                val trimFilter = if (trimStart > 0f || customAudio.trimEndSec > 0f) String.format(Locale.US, "atrim=start=%.3f:end=%.3f,asetpts=PTS-STARTPTS,", trimStart, trimEnd) else ""

                if (muteOrig) {
                    filterGraph.append(String.format(Locale.US, "[$customAudioInputIndex:a]aformat=sample_rates=44100:channel_layouts=stereo,%s${loopFilter}atrim=start=0:end=%.3f,asetpts=PTS-STARTPTS,volume=%.3f,apad[a_custom];", trimFilter, totalTimelineDurationSec, vol))
                    finalAudioStream = "[a_custom]"
                } else {
                    filterGraph.append(String.format(Locale.US, "[$customAudioInputIndex:a]aformat=sample_rates=44100:channel_layouts=stereo,%s${loopFilter}atrim=start=0:end=%.3f,asetpts=PTS-STARTPTS,volume=%.3f[a_custom];", trimFilter, totalTimelineDurationSec, vol))
                    filterGraph.append("$finalAudioStream[a_custom]amix=inputs=2:duration=first:dropout_transition=2[a_mixed];")
                    finalAudioStream = "[a_mixed]"
                }
            }

            // Overlay titles and text layers onto final video stream
            if (textOverlayInputIndex != null) {
                filterGraph.append("$finalVideoStream[$textOverlayInputIndex:v]overlay=0:0:format=yuv420:repeatlast=1[v_titled];")
                finalVideoStream = "[v_titled]"
            }

            val finalGraph = filterGraph.toString().trimEnd(';')
            cmdArgs.add("-filter_complex")
            cmdArgs.add(finalGraph)
            cmdArgs.add("-map")
            cmdArgs.add(finalVideoStream)
            cmdArgs.add("-map")
            cmdArgs.add(finalAudioStream)

            cmdArgs.add("-t")
            cmdArgs.add(String.format(Locale.US, "%.3f", totalTimelineDurationSec))

            val (targetBitrate, maxBitrate, bufSize) = when (res.lowercase()) {
                "720p" -> Triple("4M", "6M", "8M")
                "4k" -> Triple("18M", "24M", "32M")
                else -> Triple("8M", "12M", "16M")
            }

            try {
                RxFFmpegInvoke.getInstance().setDebug(true)
            } catch (_: Throwable) {}
            var lastLoggedProgress = -1
            var ffmpegErrorMsg: String? = null

            val listener = object : RxFFmpegInvoke.IFFmpegListener {
                override fun onFinish() {
                    Log.d(TAG, "FFmpeg onFinish callback received")
                }

                override fun onProgress(progress: Int, progressTime: Long) {
                    if (progress != lastLoggedProgress && progress >= 0) {
                        lastLoggedProgress = progress
                        val normalized = (0.40f + (progress.coerceIn(0, 100) / 100f) * 0.50f).coerceIn(0.40f, 0.90f)
                        val sec = progressTime / 1000
                        onProgress(normalized, "Rendering video (${progress.coerceIn(0, 100)}% - ${sec}s)...")
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

            fun buildEncodingArgs(preset: String = "ultrafast", crf: String = "23"): Array<String> {
                val args = mutableListOf<String>()
                args.addAll(cmdArgs)
                // Multi-threaded CPU libx264 optimized for mobile ARM (zero latency, high throughput)
                args.add("-c:v")
                args.add("libx264")
                args.add("-preset")
                args.add(preset)
                args.add("-tune")
                args.add("zerolatency")
                args.add("-crf")
                args.add(crf)
                args.add("-maxrate")
                args.add(maxBitrate)
                args.add("-bufsize")
                args.add(bufSize)
                args.add("-threads")
                args.add(numCores.toString())
                args.add("-pix_fmt")
                args.add("yuv420p")
                args.add("-c:a")
                args.add("aac")
                args.add("-b:a")
                args.add("192k")
                args.add("-movflags")
                args.add("+faststart")
                args.add(outputFile.absolutePath)
                return args.toTypedArray()
            }

            if (isCancelled || !isActive) {
                try { outputFile.delete() } catch (_: Throwable) {}
                return@withContext null
            }

            try { outputFile.delete() } catch (_: Throwable) {}
            onProgress(0.40f, "⚡ Rendering video timeline...")
            val primaryCmd = buildEncodingArgs(preset = "ultrafast", crf = "23")
            Log.d(TAG, "Executing FFmpeg (${primaryCmd.size} args): ${primaryCmd.joinToString(" ")}")
            var returnCode = try {
                RxFFmpegInvoke.getInstance().runCommand(primaryCmd, listener)
            } catch (t: Throwable) {
                Log.e(TAG, "RxFFmpeg primary invocation crashed", t)
                -1
            }
            Log.d(TAG, "Primary FFmpeg returned code: $returnCode (ErrorMsg: $ffmpegErrorMsg)")

            // If primary preset fails or format unsupported, retry with safe compatibility preset
            if ((returnCode != 0 || !outputFile.exists() || outputFile.length() == 0L) && !isCancelled && isActive) {
                Log.w(TAG, "Primary FFmpeg returned $returnCode ($ffmpegErrorMsg). Retrying with compatibility encoding...")
                onProgress(0.42f, "Rendering video (Compatibility mode)...")
                try { outputFile.delete() } catch (_: Throwable) {}
                ffmpegErrorMsg = null
                val fallbackCmd = buildEncodingArgs(preset = "veryfast", crf = "26")
                Log.d(TAG, "Executing Fallback FFmpeg (${fallbackCmd.size} args): ${fallbackCmd.joinToString(" ")}")
                returnCode = try {
                    RxFFmpegInvoke.getInstance().runCommand(fallbackCmd, listener)
                } catch (t: Throwable) {
                    Log.e(TAG, "RxFFmpeg fallback invocation crashed", t)
                    -1
                }
                Log.d(TAG, "Fallback FFmpeg returned code: $returnCode (ErrorMsg: $ffmpegErrorMsg)")
            }

            if (isCancelled || !isActive) {
                try { outputFile.delete() } catch (_: Throwable) {}
                return@withContext null
            }

            if (returnCode == 0 && outputFile.exists() && outputFile.length() > 0L) {
                onProgress(0.95f, "Finishing up export...")
                Log.i(TAG, "Export succeeded! Size: ${outputFile.length()} bytes, path: ${outputFile.absolutePath}")
                return@withContext outputFile
            }

            // Native fallback: Try direct hardware muxing if FFmpeg failed
            if (!isCancelled && isActive) {
                Log.w(TAG, "FFmpeg render returned $returnCode or output file empty (error: $ffmpegErrorMsg); attempting native hardware muxer fallback")
                onProgress(0.60f, "Using hardware direct muxer...")
                try {
                    outputFile.delete()
                    val success = VideoProcessor.concatVideoClips(context, clips, outputFile)
                    if (success && outputFile.exists() && outputFile.length() > 0L) {
                        onProgress(0.90f, "Render complete!")
                        return@withContext outputFile
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Native fallback failed", e)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "exportTimeline encountered exception", e)
        } finally {
            try { overlayPngFile?.delete() } catch (_: Throwable) {}
            if (isCancelled) {
                try { outputFile.delete() } catch (_: Throwable) {}
            }
            // Clean up all copied temporary input cache files preventing cache directory leaks
            for (f in localFileCache.values) {
                if (f.exists() && f.parentFile == context.cacheDir && f.name.startsWith("fcp_input_")) {
                    try { f.delete() } catch (_: Throwable) {}
                }
            }
        }

        if (isCancelled || !isActive) {
            try { outputFile.delete() } catch (_: Throwable) {}
            return@withContext null
        }

        outputFile.delete()
        null
    }
}
