package com.chroma.studio.ui.screens

import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.chroma.studio.ui.components.AppleSlider
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.utils.ExportHelper
import com.chroma.studio.utils.VideoProcessor
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToLong

// Flash video filters
data class FlashFilter(
    val name: String,
    val colorMatrix: ColorMatrix
)

val FlashFilters = listOf(
    FlashFilter("None", ColorMatrix()),
    FlashFilter("B&W", ColorMatrix().apply { setToSaturation(0f) }),
    FlashFilter("Cinematic", ColorMatrix(floatArrayOf(
        1.1f, 0f,   0f,   0f, -10f,
        0f,   1.0f, 0f,   0f, -5f,
        0f,   0f,   0.9f, 0f,  10f,
        0f,   0f,   0f,   1f,   0f
    ))),
    FlashFilter("Warm", ColorMatrix(floatArrayOf(
        1.2f, 0f,   0f,   0f, 20f,
        0f,   1.0f, 0f,   0f, 5f,
        0f,   0f,   0.8f, 0f, -20f,
        0f,   0f,   0f,   1f, 0f
    ))),
    FlashFilter("Cool", ColorMatrix(floatArrayOf(
        0.9f, 0f,   0f,   0f, -10f,
        0f,   1.0f, 0f,   0f, 5f,
        0f,   0f,   1.2f, 0f, 20f,
        0f,   0f,   0f,   1f, 0f
    ))),
    FlashFilter("Fade", ColorMatrix(floatArrayOf(
        0.9f, 0f,   0f,   0f, 30f,
        0f,   0.9f, 0f,   0f, 25f,
        0f,   0f,   0.9f, 0f, 25f,
        0f,   0f,   0f,   1f, 0f
    ))),
    FlashFilter("Vivid", ColorMatrix().apply { setToSaturation(1.8f) }),
    FlashFilter("Matte", ColorMatrix(floatArrayOf(
        0.85f, 0f,    0f,    0f, 20f,
        0f,    0.85f, 0f,    0f, 15f,
        0f,    0f,    0.85f, 0f, 20f,
        0f,    0f,    0f,    1f, 0f
    ))),
    FlashFilter("Sepia", ColorMatrix(floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f,     0f,     0f,     1f, 0f
    ))),
    FlashFilter("Neon", ColorMatrix(floatArrayOf(
        0.8f,  0.2f,  0f,   0f, 0f,
        0f,    0.9f,  0.1f, 0f, 0f,
        0.2f,  0f,    1.0f, 0f, 50f,
        0f,    0f,    0f,   1f, 0f
    ))),
    FlashFilter("Sunset", ColorMatrix(floatArrayOf(
        1.3f, 0f,   0f,   0f, 30f,
        0f,   0.9f, 0f,   0f, -10f,
        0f,   0f,   0.7f, 0f, -30f,
        0f,   0f,   0f,   1f, 0f
    ))),
    FlashFilter("Noir", ColorMatrix(floatArrayOf(
        0.33f, 0.33f, 0.33f, 0f, -10f,
        0.33f, 0.33f, 0.33f, 0f, -10f,
        0.33f, 0.33f, 0.33f, 0f, -10f,
        0f,    0f,    0f,    1f, 0f
    )))
)

/**
 * Combines the preset [FlashFilter] color matrix with user fine-tuning adjustments:
 * brightness (-100..100), contrast (0.5..2.0), saturation (0..3.0), and hue rotation (-180..180).
 */
fun buildCombinedColorMatrix(
    filter: FlashFilter,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    hue: Float
): ColorMatrix {
    val cm = ColorMatrix(filter.colorMatrix.values.copyOf())

    // 1. Brightness & Contrast
    if (brightness != 0f || contrast != 1f) {
        val scale = contrast
        val translate = brightness + (1f - scale) * 128f
        val bcMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.timesAssign(bcMatrix)
    }

    // 2. Saturation
    if (saturation != 1f) {
        val satMatrix = ColorMatrix().apply { setToSaturation(saturation) }
        cm.timesAssign(satMatrix)
    }

    // 3. Hue rotation
    if (hue != 0f) {
        val angleRad = Math.toRadians(hue.toDouble()).toFloat()
        val cosA = kotlin.math.cos(angleRad)
        val sinA = kotlin.math.sin(angleRad)
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f
        val hueMatrix = ColorMatrix(floatArrayOf(
            lumR + cosA * (1f - lumR) + sinA * (-lumR),
            lumG + cosA * (-lumG) + sinA * (-lumG),
            lumB + cosA * (-lumB) + sinA * (1f - lumB),
            0f, 0f,
            lumR + cosA * (-lumR) + sinA * 0.143f,
            lumG + cosA * (1f - lumG) + sinA * 0.140f,
            lumB + cosA * (-lumB) + sinA * -0.283f,
            0f, 0f,
            lumR + cosA * (-lumR) + sinA * (-(1f - lumR)),
            lumG + cosA * (-lumG) + sinA * lumG,
            lumB + cosA * (1f - lumB) + sinA * lumG,
            0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.timesAssign(hueMatrix)
    }

    return cm
}

/** Applies a ColorMatrix filter onto a Bitmap and returns the graded Bitmap. */
fun applyColorFilterToBitmap(src: Bitmap, matrix: ColorMatrix): Bitmap {
    val output = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    val paint = android.graphics.Paint().apply {
        colorFilter = ColorMatrixColorFilter(matrix.values)
    }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return output
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FlashVideoEditorScreen(
    videoUri: Uri,
    initialTrimStartMs: Long = 0L,
    initialTrimEndMs: Long = 0L,
    initialFilterName: String = "None",
    initialBrightness: Float = 0f,
    initialContrast: Float = 1f,
    initialSaturation: Float = 1f,
    initialHue: Float = 0f,
    remixedFromAuthorUsername: String? = null,
    onNext: (trimStart: Long, trimEnd: Long, filterName: String, brightness: Float, contrast: Float, saturation: Float, hue: Float, thumbnail: Bitmap?) -> Unit,
    onSaveDraft: (trimStart: Long, trimEnd: Long, filterName: String, brightness: Float, contrast: Float, saturation: Float, hue: Float) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalOscellionColors.current

    // Video duration
    var durationMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(videoUri) {
        durationMs = withContext(Dispatchers.IO) { VideoProcessor.getVideoDurationMs(context, videoUri) }
    }

    // Trim state
    var trimStartMs by remember { mutableLongStateOf(initialTrimStartMs) }
    var trimEndMs by remember { mutableLongStateOf(initialTrimEndMs) }
    LaunchedEffect(durationMs) {
        if (trimEndMs <= 0L && durationMs > 0L) {
            trimEndMs = durationMs
        }
    }

    // ── 1-Minute Flash Video Limit State & Warning ───────────────────────────
    val trimDuration = (trimEndMs - trimStartMs).coerceAtLeast(0L)
    val isOverOneMinute = trimDuration > 60_000L || (durationMs > 60_000L && trimEndMs == 0L)
    var showDurationWarningPopup by remember { mutableStateOf(false) }
    var hasShownOneMinuteWarning by remember { mutableStateOf(false) }
    var isSavingToGallery by remember { mutableStateOf(false) }

    // Trigger Apple-style warning popup when video exceeds 1 minute
    LaunchedEffect(durationMs) {
        if (durationMs > 60_000L && !hasShownOneMinuteWarning) {
            showDurationWarningPopup = true
            hasShownOneMinuteWarning = true
        }
    }

    fun performSaveToGallery() {
        if (isSavingToGallery) return
        isSavingToGallery = true
        scope.launch {
            try {
                val savedUri = withContext(Dispatchers.IO) {
                    val shouldTrim = trimStartMs > 100L || (trimEndMs > 0L && durationMs > 0L && trimEndMs < durationMs - 100L)
                    val fileToSave = if (shouldTrim) {
                        VideoProcessor.trimVideo(context, videoUri, trimStartMs, trimEndMs)
                    } else null

                    val finalFile = fileToSave ?: run {
                        if (videoUri.scheme == "file") {
                            java.io.File(videoUri.path ?: "")
                        } else {
                            val tempFile = java.io.File(context.cacheDir, "gallery_export_${System.currentTimeMillis()}.mp4")
                            context.contentResolver.openInputStream(videoUri)?.use { input ->
                                java.io.FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                            }
                            tempFile
                        }
                    }

                    ExportHelper.saveVideoToGallery(
                        context = context,
                        sourceFile = finalFile,
                        title = "ChromaFlash_${System.currentTimeMillis()}"
                    )
                }
                if (savedUri != null) {
                    ToastManager.showToast("Saved to Gallery! 🎬")
                } else {
                    ToastManager.showToast("Saved to device!")
                }
            } catch (e: Throwable) {
                ToastManager.showToast("Could not save to gallery: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                isSavingToGallery = false
            }
        }
    }

    // Filter & Adjustments state
    var selectedFilter by remember {
        mutableStateOf(FlashFilters.find { it.name.equals(initialFilterName, ignoreCase = true) } ?: FlashFilters[0])
    }
    var brightness by remember { mutableFloatStateOf(initialBrightness) }   // -100..+100
    var contrast by remember { mutableFloatStateOf(initialContrast) }       // 0.5..2.0
    var saturation by remember { mutableFloatStateOf(initialSaturation) }   // 0..3
    var hue by remember { mutableFloatStateOf(initialHue) }                 // -180..+180

    // Tab: Filters or Adjust
    var activeTab by remember { mutableStateOf(0) } // 0=Filters, 1=Adjust

    // ExoPlayer setup
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPlayheadMs by remember { mutableLongStateOf(0L) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val exoDur = player.duration
                    if (exoDur > 0L && durationMs <= 0L) {
                        durationMs = exoDur
                        if (trimEndMs <= 0L) {
                            trimEndMs = exoDur.coerceAtMost(60_000L)
                        }
                    }
                }
                if (playbackState == Player.STATE_ENDED) {
                    player.seekTo(trimStartMs)
                    player.play()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(videoUri) {
        player.setMediaItem(MediaItem.fromUri(videoUri))
        player.prepare()
        if (trimStartMs > 0L) {
            player.seekTo(trimStartMs)
        }
        player.play()
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    // Sync player to trim start on trim changes
    LaunchedEffect(trimStartMs) {
        player.seekTo(trimStartMs)
        currentPlayheadMs = trimStartMs
    }

    // Playback loop controller: keeps playback strictly clamped inside [trimStartMs, trimEndMs]
    LaunchedEffect(player, trimStartMs, trimEndMs, isPlaying) {
        while (true) {
            if (isPlaying && player.isPlaying) {
                val pos = player.currentPosition
                currentPlayheadMs = pos
                if (trimEndMs > trimStartMs && pos >= trimEndMs) {
                    player.seekTo(trimStartMs)
                    currentPlayheadMs = trimStartMs
                } else if (pos < trimStartMs) {
                    player.seekTo(trimStartMs)
                    currentPlayheadMs = trimStartMs
                }
            } else {
                currentPlayheadMs = player.currentPosition
            }
            delay(33L) // ~30 fps playhead refresh
        }
    }

    // Play/Pause toggle
    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
            isPlaying = false
        } else {
            if (player.currentPosition >= trimEndMs || player.currentPosition < trimStartMs) {
                player.seekTo(trimStartMs)
            }
            player.play()
            isPlaying = true
        }
        showPlayPauseIndicator = true
    }

    LaunchedEffect(showPlayPauseIndicator) {
        if (showPlayPauseIndicator) {
            delay(600L)
            showPlayPauseIndicator = false
        }
    }

    // Thumbnail extraction
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(videoUri, trimStartMs) {
        thumbnail = withContext(Dispatchers.IO) {
            VideoProcessor.extractThumbnail(context, videoUri, trimStartMs)
        }
    }

    // Combined real-time GPU render effect for video playback
    val supportsRenderEffect = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    val activeRenderEffect = remember(selectedFilter, brightness, contrast, saturation, hue, supportsRenderEffect) {
        if (!supportsRenderEffect) null
        else {
            val isDefault = selectedFilter.name == "None" && brightness == 0f && contrast == 1f && saturation == 1f && hue == 0f
            if (isDefault) null
            else {
                val combined = buildCombinedColorMatrix(selectedFilter, brightness, contrast, saturation, hue)
                val androidMatrix = android.graphics.ColorMatrix(combined.values)
                RenderEffect.createColorFilterEffect(
                    ColorMatrixColorFilter(androidMatrix)
                ).asComposeRenderEffect()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Apple-Style 1-Minute Warning Popup Dialog ────────────────────────
        if (showDurationWarningPopup) {
            Dialog(onDismissRequest = { showDurationWarningPopup = false }) {
                Box(
                    modifier = Modifier
                        .width(290.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E1E1E).copy(alpha = 0.96f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 22.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF9500).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.TriangleAlert,
                                contentDescription = null,
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Flash Video Limit",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Flash Videos must be under 1min.\nTrim your video to under 60 seconds to share on Flash, or save the full video to your gallery.",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 0.5.dp)
                        // 1. Primary Action: Trim to 1 Min
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clickable {
                                    trimStartMs = 0L
                                    trimEndMs = (trimStartMs + 60_000L).coerceAtMost(durationMs)
                                    showDurationWarningPopup = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Trim to 1 Min",
                                color = Color(0xFF007AFF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 0.5.dp)

                        // 2. Secondary Action: Save Full Video to Gallery
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable {
                                    showDurationWarningPopup = false
                                    performSaveToGallery()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF34C759),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Save Full Video to Gallery",
                                    color = Color(0xFF34C759),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 0.5.dp)

                        // 3. Dismiss Action
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clickable {
                                    showDurationWarningPopup = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Dismiss",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // ── Top bar ──────────────────────────────────────────────────────────
        ChromaScreenHeader(
            title = "Oscellion",
            badgeText = "Flash Editor",
            badgeColor = Color(0xFF007AFF),
            showBackButton = true,
            onBack = onBack,
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isOverOneMinute) {
                        // ── Strict Constraint: > 1 min videos NEVER show Next/Share! ONLY show "Save in Gallery" ──
                        val savePillShape = RoundedCornerShape(9999.dp)
                        Box(
                            modifier = Modifier
                                .clip(savePillShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF34C759), Color(0xFF30D158))
                                    )
                                )
                                .border(1.dp, com.chroma.studio.ui.components.LiquidGlassTokens.AppleSpecularGlassBorder, savePillShape)
                                .clickable(enabled = !isSavingToGallery) {
                                    performSaveToGallery()
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSavingToGallery) {
                                OscellionLoadingSpinner(modifier = Modifier.size(14.dp), color = Color.White)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.Download,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Save in Gallery",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // ── Under 1 min: Normal Draft & Next buttons ──
                        val draftPillShape = RoundedCornerShape(9999.dp)
                        Box(
                            modifier = Modifier
                                .clip(draftPillShape)
                                .background(colors.glassBgHover)
                                .border(0.5.dp, colors.glassBorder, draftPillShape)
                                .border(0.5.dp, com.chroma.studio.ui.components.LiquidGlassTokens.SurfaceSpecularGlassBorder, draftPillShape)
                                .clickable {
                                    onSaveDraft(trimStartMs, trimEndMs, selectedFilter.name, brightness, contrast, saturation, hue)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Draft", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        val nextPillShape = RoundedCornerShape(9999.dp)
                        Box(
                            modifier = Modifier
                                .clip(nextPillShape)
                                .background(com.chroma.studio.ui.components.LiquidGlassTokens.AppleSystemBlue)
                                .border(0.5.dp, Color(0x8C0051D5), nextPillShape)
                                .border(1.dp, com.chroma.studio.ui.components.LiquidGlassTokens.AppleSpecularGlassBorder, nextPillShape)
                                .clickable {
                                    val gradedThumbnail = thumbnail?.let { raw ->
                                        val isDefault = selectedFilter.name == "None" && brightness == 0f && contrast == 1f && saturation == 1f && hue == 0f
                                        if (isDefault) raw
                                        else {
                                            val combined = buildCombinedColorMatrix(selectedFilter, brightness, contrast, saturation, hue)
                                            applyColorFilterToBitmap(raw, combined)
                                        }
                                    } ?: thumbnail
                                    onNext(trimStartMs, trimEndMs, selectedFilter.name, brightness, contrast, saturation, hue, gradedThumbnail)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Next", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        )

        // ── Remix Lineage Banner ──────────────────────────────────────────────
        if (!remixedFromAuthorUsername.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF007AFF).copy(alpha = 0.15f))
                    .border(0.5.dp, Color(0xFF007AFF).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Lucide.Sparkles,
                    contentDescription = "Remix",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Remixing Flash from @$remixedFromAuthorUsername",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── Video preview ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111111))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    togglePlayPause()
                },
            contentAlignment = Alignment.Center
        ) {
            var aspectRatioLayoutRef by remember { mutableStateOf<AspectRatioFrameLayout?>(null) }
            DisposableEffect(player) {
                val sizeListener = object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        if (videoSize.height > 0 && videoSize.width > 0) {
                            val ratio = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height
                            aspectRatioLayoutRef?.setAspectRatio(ratio)
                        }
                    }
                }
                player.addListener(sizeListener)
                onDispose {
                    player.removeListener(sizeListener)
                }
            }

            AndroidView(
                factory = { ctx ->
                    val layout = AspectRatioFrameLayout(ctx).apply {
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        isClickable = false
                        isFocusable = false
                    }
                    aspectRatioLayoutRef = layout
                    val textureView = android.view.TextureView(ctx).apply {
                        isOpaque = true
                        isClickable = false
                        isFocusable = false
                    }
                    val layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    ).apply { gravity = android.view.Gravity.CENTER }
                    layout.addView(textureView, layoutParams)
                    player.setVideoTextureView(textureView)
                    layout
                },
                onRelease = {
                    aspectRatioLayoutRef = null
                },
                update = { _ -> },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (supportsRenderEffect) {
                            renderEffect = activeRenderEffect
                        }
                    }
            )

            // Fallback overlay for older devices without RenderEffect support
            if (!supportsRenderEffect) {
                val overlayAlpha = (brightness / 200f).coerceIn(-0.5f, 0.5f)
                val baseTint = if (selectedFilter.name != "None") Color.Black.copy(alpha = 0.2f) else Color.Transparent
                val brightnessTint = if (overlayAlpha > 0) Color.White.copy(alpha = overlayAlpha)
                else if (overlayAlpha < 0) Color.Black.copy(alpha = -overlayAlpha)
                else Color.Transparent

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseTint)
                        .background(brightnessTint)
                )
            }

            // Play/Pause indicator overlay (smooth fade in/out)
            androidx.compose.animation.AnimatedVisibility(
                visible = showPlayPauseIndicator || !isPlaying,
                enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.8f),
                exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 1.1f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Lucide.Play else Lucide.Pause,
                        contentDescription = if (isPlaying) "Play" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // ── Trim timeline ─────────────────────────────────────────────────────
        if (durationMs > 0L) {
            Spacer(Modifier.height(12.dp))
            TrimTimeline(
                videoUri = videoUri,
                durationMs = durationMs,
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                currentPlayheadMs = currentPlayheadMs,
                onTrimChange = { start, end ->
                    trimStartMs = start
                    trimEndMs = end
                },
                onSeekTo = { seekPos ->
                    player.seekTo(seekPos)
                },
                onExceedOneMinute = {
                    showDurationWarningPopup = true
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Duration badge & 1-Minute Constraint Warning
        if (durationMs > 0L) {
            val maxAllowedMs = minOf(durationMs, 60_000L)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "${trimDuration / 1000}s / ${if (isOverOneMinute) "60s" else "${maxAllowedMs / 1000}s"}",
                    color = if (isOverOneMinute) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isOverOneMinute) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(Color(0xFFFF9500).copy(alpha = 0.15f))
                            .border(0.5.dp, Color(0xFFFF9500).copy(alpha = 0.35f), RoundedCornerShape(9999.dp))
                            .clickable { showDurationWarningPopup = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.TriangleAlert,
                            contentDescription = null,
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Flash Videos must be under 1min",
                            color = Color(0xFFFF9500),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
        Spacer(Modifier.height(8.dp))

        // ── Tab bar (Filters / Adjust) ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Filters", "Adjust").forEachIndexed { index, label ->
                val selected = activeTab == index
                val pillTabShape = RoundedCornerShape(9999.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(pillTabShape)
                        .background(if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                        .border(
                            width = 0.5.dp,
                            color = if (selected) Color.White.copy(alpha = 0.35f) else Color.Transparent,
                            shape = pillTabShape
                        )
                        .border(
                            width = 0.5.dp,
                            brush = if (selected) com.chroma.studio.ui.components.LiquidGlassTokens.SurfaceSpecularGlassBorder else SolidColor(Color.Transparent),
                            shape = pillTabShape
                        )
                        .clickable { activeTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Filters carousel ──────────────────────────────────────────────────
        if (activeTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlashFilters.forEach { filter ->
                    FilterChip(filter, thumbnail, selected = filter.name == selectedFilter.name) {
                        selectedFilter = filter
                    }
                }
            }
        }

        // ── Adjustment sliders ────────────────────────────────────────────────
        if (activeTab == 1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdjustSlider("Brightness", brightness, -100f, 100f, "%+.0f") { brightness = it }
                AdjustSlider("Contrast", contrast, 0.5f, 2f, "%.1fx") { contrast = it }
                AdjustSlider("Saturation", saturation, 0f, 3f, "%.1fx") { saturation = it }
                AdjustSlider("Hue", hue, -180f, 180f, "%+.0f°") { hue = it }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TrimTimeline(
    videoUri: Uri,
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    currentPlayheadMs: Long,
    onTrimChange: (Long, Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onExceedOneMinute: () -> Unit = {}
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    var barWidthPx by remember { mutableFloatStateOf(0f) }

    // Filmstrip video thumbnails along the track
    var timelineThumbnails by remember(videoUri, durationMs) { mutableStateOf<List<Bitmap>>(emptyList()) }
    LaunchedEffect(videoUri, durationMs) {
        if (durationMs > 0L) {
            val thumbs = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                val list = mutableListOf<Bitmap>()
                try {
                    retriever.setDataSource(context, videoUri)
                    val count = 8
                    val stepUs = (durationMs * 1000L) / count
                    for (i in 0 until count) {
                        val timeUs = (i * stepUs + stepUs / 2).coerceAtMost(durationMs * 1000L)
                        val bm = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                        if (bm != null) list.add(bm)
                    }
                } catch (_: Throwable) {
                } finally {
                    try { retriever.release() } catch (_: Throwable) {}
                }
                list
            }
            if (thumbs.isNotEmpty()) {
                timelineThumbnails = thumbs
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .onSizeChanged { barWidthPx = it.width.toFloat() }
    ) {
        // 1. Filmstrip thumbnails
        if (timelineThumbnails.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxSize()) {
                timelineThumbnails.forEach { bm ->
                    androidx.compose.foundation.Image(
                        bitmap = bm.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // 2. Trim Range, Overlays, Playhead & Drag Handles
        if (barWidthPx > 0f && durationMs > 0L) {
            val startFrac = (trimStartMs.toFloat() / durationMs).coerceIn(0f, 1f)
            val endFrac = (trimEndMs.toFloat() / durationMs).coerceIn(0f, 1f)
            val startPx = startFrac * barWidthPx
            val endPx = endFrac * barWidthPx

            // Dim left region outside trim
            if (startPx > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(with(density) { startPx.toDp() })
                        .background(Color.Black.copy(alpha = 0.65f))
                )
            }

            // Dim right region outside trim
            if (endPx < barWidthPx) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(with(density) { (barWidthPx - endPx).toDp() })
                        .offset(x = with(density) { endPx.toDp() })
                        .background(Color.Black.copy(alpha = 0.65f))
                )
            }

            // Yellow selected range box
            val selectionWidthPx = (endPx - startPx).coerceAtLeast(0f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { selectionWidthPx.toDp() })
                    .offset(x = with(density) { startPx.toDp() })
                    .background(Color(0xFF007AFF).copy(alpha = 0.18f))
                    .border(2.dp, Color(0xFF007AFF), RoundedCornerShape(4.dp))
            )

            // Playhead indicator hairline
            val playheadFrac = (currentPlayheadMs.toFloat() / durationMs).coerceIn(0f, 1f)
            val playheadPx = playheadFrac * barWidthPx
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .offset(x = with(density) { (playheadPx - 1.dp.toPx()).toDp() })
                    .background(Color.White)
            )

            // Start handle
            DragHandle(
                fraction = startFrac,
                barWidthPx = barWidthPx,
                isLeftHandle = true,
                onDrag = { newFrac ->
                    val newStart = (newFrac * durationMs).roundToLong()
                        .coerceIn(0L, (trimEndMs - 1000L).coerceAtLeast(0L))
                    onTrimChange(newStart, trimEndMs)
                    onSeekTo(newStart)
                }
            )

            // End handle
            DragHandle(
                fraction = endFrac,
                barWidthPx = barWidthPx,
                isLeftHandle = false,
                onDrag = { newFrac ->
                    val minEnd = (trimStartMs + 1000L).coerceAtMost(durationMs)
                    val maxEnd = durationMs
                    val newEnd = (newFrac * durationMs).roundToLong()
                        .coerceIn(minEnd, maxEnd)
                    onTrimChange(trimStartMs, newEnd)
                    onSeekTo(newEnd)
                    if (newEnd - trimStartMs > 60_000L) {
                        onExceedOneMinute()
                    }
                }
            )
        }
    }
}

@Composable
private fun DragHandle(
    fraction: Float,
    barWidthPx: Float,
    isLeftHandle: Boolean,
    onDrag: (Float) -> Unit
) {
    val density = LocalDensity.current
    val offsetXPx = with(density) {
        if (isLeftHandle) {
            fraction * barWidthPx - 4.dp.toPx()
        } else {
            fraction * barWidthPx - 12.dp.toPx()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(16.dp)
            .offset(x = with(density) { offsetXPx.toDp() })
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF007AFF))
            .pointerInput(barWidthPx) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val newFrac = ((fraction * barWidthPx + dragAmount) / barWidthPx).coerceIn(0f, 1f)
                    onDrag(newFrac)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Handle grip line
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(18.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(1.dp))
        )
    }
}

@Composable
fun FilterChip(
    filter: FlashFilter,
    thumbnail: Bitmap?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    2.dp,
                    if (selected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.2f),
                    RoundedCornerShape(12.dp)
                )
                .background(Color.DarkGray)
        ) {
            if (thumbnail != null) {
                androidx.compose.foundation.Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(filter.colorMatrix),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (filter.name == "B&W") Color.Gray else Color(0xFF2C2C2E))
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            filter.name,
            color = if (selected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    format: String,
    defaultValue: Float = if (min < 0f) 0f else 1f,
    onValueChange: (Float) -> Unit
) {
    val isModified = kotlin.math.abs(value - defaultValue) > 0.01f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1C1C1E))
            .border(1.dp, if (isModified) Color(0x33007AFF) else Color(0x18FFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isModified) {
                        Text(
                            text = "Reset",
                            color = Color(0xFF007AFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .clickable { onValueChange(defaultValue) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                val valPillShape = RoundedCornerShape(9999.dp)
                Box(
                    modifier = Modifier
                        .clip(valPillShape)
                        .background(if (isModified) Color(0x26007AFF) else Color(0x14FFFFFF))
                        .border(0.5.dp, if (isModified) Color(0x55007AFF) else Color(0x1AFFFFFF), valPillShape)
                        .padding(horizontal = 9.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, format, value),
                        color = if (isModified) Color(0xFF007AFF) else Color(0xCCFFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            AppleSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = min..max,
                trackHeight = 8.dp
            )
        }
    }
}
