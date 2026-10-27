package com.chroma.studio.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
// Social Media Photo Studio Editor
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.chroma.studio.ai.AIGenerator
import com.chroma.studio.ui.components.AppleSlider
import com.chroma.studio.ui.components.AppleSwitch
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.GlassButton
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.GlassPanel
import com.chroma.studio.ui.components.PrimaryGradientButton
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// ─────────────────────────────────────────────────────────────────────────────
// STUDIO DATA MODELS & ENUMS
// ─────────────────────────────────────────────────────────────────────────────

enum class PhotoAspectRatio(val label: String, val ratio: Float?, val subtitle: String) {
    FREE("Original", null, "Uncropped Native"),
    SQUARE("1:1", 1f, "Square Feed Post"),
    PORTRAIT_4_5("4:5", 4f / 5f, "Instagram Portrait"),
    STORY_9_16("9:16", 9f / 16f, "Story / Reel Fullscreen"),
    LANDSCAPE_16_9("16:9", 16f / 9f, "Widescreen / Banner"),
    PORTRAIT_3_4("3:4", 3f / 4f, "Classic Social Post")
}

enum class PhotoDrawerLevel {
    COLLAPSED,
    MID,
    FULL
}

enum class StudioEditorCategory(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    LOOKS("Filters", Lucide.Sparkles),
    ADJUST("Adjust", Lucide.SlidersHorizontal),
    CROP("Crop", Lucide.Crop),
    TYPE("Text", Lucide.Type)
}

enum class StudioBackdropType {
    TRANSPARENT,
    SOLID_WHITE,
    SOLID_GRAY,
    SOLID_BLACK,
    STUDIO_GRADIENT_BLUE,
    STUDIO_GRADIENT_DARK,
    STUDIO_WARM,
    BLUR_ORIGINAL
}

enum class StudioFilterPreset(
    val displayName: String,
    val subtitle: String,
    val matrix: ColorMatrix
) {
    ORIGINAL("None", "Natural Look", ColorMatrix()),
    CINEMATIC("Cinematic", "Teal & Amber Film Grade", ColorMatrix(floatArrayOf(
        1.12f, 0f, 0f, 0f, 10f,
        0f, 1.05f, 0f, 0f, 0f,
        0f, 0f, 0.88f, 0f, -12f,
        0f, 0f, 0f, 1f, 0f
    ))),
    NORDIC_GLOW("Nordic", "Clean Dynamic Clarity", ColorMatrix(floatArrayOf(
        1.05f, 0f, 0f, 0f, 12f,
        0f, 1.08f, 0f, 0f, 12f,
        0f, 0f, 1.15f, 0f, 18f,
        0f, 0f, 0f, 1f, 0f
    ))),
    VIVID("Vivid Pop", "Punchy Saturated Hues", ColorMatrix().apply { setToSaturation(1.85f) }),
    NOIR("Mono Noir", "Deep Contrast Black & White", ColorMatrix(floatArrayOf(
        0.35f, 0.35f, 0.35f, 0f, -15f,
        0.35f, 0.35f, 0.35f, 0f, -15f,
        0.35f, 0.35f, 0.35f, 0f, -15f,
        0f, 0f, 0f, 1f, 0f
    ))),
    BW_CLASSIC("B&W Classic", "Neutral Grayscale", ColorMatrix().apply { setToSaturation(0f) }),
    WARM_SUNSET("Warm Sunset", "Golden Hour Radiance", ColorMatrix(floatArrayOf(
        1.22f, 0f, 0f, 0f, 25f,
        0f, 1.02f, 0f, 0f, 8f,
        0f, 0f, 0.82f, 0f, -22f,
        0f, 0f, 0f, 1f, 0f
    ))),
    COOL_SLATE("Cool Slate", "Clean Nordic Cool Tone", ColorMatrix(floatArrayOf(
        0.92f, 0f, 0f, 0f, -6f,
        0f, 1.01f, 0f, 0f, 2f,
        0f, 0f, 1.14f, 0f, 14f,
        0f, 0f, 0f, 1f, 0f
    ))),
    SOFT_FADE("Soft Fade", "Matte Lifted Film", ColorMatrix(floatArrayOf(
        0.88f, 0f, 0f, 0f, 28f,
        0f, 0.88f, 0f, 0f, 24f,
        0f, 0f, 0.88f, 0f, 24f,
        0f, 0f, 0f, 1f, 0f
    ))),
    SEPIA_VINTAGE("Sepia", "Antique Heritage Tone", ColorMatrix(floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))),
    MOODY_EMERALD("Emerald", "Deep Forest Green Grading", ColorMatrix(floatArrayOf(
        0.92f, 0f, 0f, 0f, -8f,
        0f, 1.15f, 0f, 0f, 16f,
        0f, 0f, 0.95f, 0f, 5f,
        0f, 0f, 0f, 1f, 0f
    ))),
    PASTEL_MIST("Pastel", "Dreamy Soft Tone", ColorMatrix(floatArrayOf(
        0.95f, 0.05f, 0f, 0f, 20f,
        0f, 0.95f, 0.05f, 0f, 18f,
        0.05f, 0f, 0.95f, 0f, 22f,
        0f, 0f, 0f, 1f, 0f
    ))),
    CHROME_MONO("Chrome", "Silver High-Key Monochrome", ColorMatrix(floatArrayOf(
        0.45f, 0.45f, 0.45f, 0f, 22f,
        0.45f, 0.45f, 0.45f, 0f, 22f,
        0.45f, 0.45f, 0.45f, 0f, 22f,
        0f, 0f, 0f, 1f, 0f
    )))
}

data class StudioPhotoSnapshot(
    val filter: StudioFilterPreset,
    val filterIntensity: Float,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val warmth: Float,
    val sepia: Float,
    val blur: Float,
    val isBw: Boolean,
    val hue: Float,
    val inkClean: Boolean,
    val inkThreshold: Float,
    val aspectRatio: PhotoAspectRatio,
    val fineRotation: Float,
    val rotation90: Float,
    val flipH: Boolean,
    val flipV: Boolean,
    val frameWidthPx: Float,
    val frameColor: Color,
    val backdropType: StudioBackdropType,
    val overlayText: String,
    val textColor: Color,
    val textBgStyle: String,
    val quality: Int
)

// ─────────────────────────────────────────────────────────────────────────────
// COLOR MATRIX PIPELINE COMPOSER
// ─────────────────────────────────────────────────────────────────────────────

fun buildStudioCombinedColorMatrix(
    preset: StudioFilterPreset,
    intensity: Float,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    sepia: Float,
    hue: Float,
    isBw: Boolean,
    inkClean: Boolean,
    inkThreshold: Float
): ColorMatrix {
    val cm = ColorMatrix()

    // 1. Preset interpolation
    if (preset != StudioFilterPreset.ORIGINAL && intensity > 0f) {
        val presetVals = preset.matrix.values
        val interpVals = FloatArray(20)
        for (i in 0 until 20) {
            val identityVal = if (i % 6 == 0 && i < 19) 1f else 0f
            interpVals[i] = identityVal + (presetVals[i] - identityVal) * intensity.coerceIn(0f, 1f)
        }
        cm.timesAssign(ColorMatrix(interpVals))
    }

    // 2. Brightness & Contrast
    if (brightness != 0f || contrast != 1f) {
        val scale = contrast.coerceIn(0.2f, 3.0f)
        val translate = brightness + (1f - scale) * 128f
        val bcMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.timesAssign(bcMatrix)
    }

    // 3. Saturation & B&W
    if (isBw) {
        cm.timesAssign(ColorMatrix().apply { setToSaturation(0f) })
    } else if (saturation != 1f) {
        cm.timesAssign(ColorMatrix().apply { setToSaturation(saturation.coerceIn(0f, 3f)) })
    }

    // 4. Warmth / Temperature
    if (warmth != 0f) {
        val wNorm = warmth.coerceIn(-100f, 100f)
        val rAdd = wNorm * 0.35f
        val bAdd = -wNorm * 0.35f
        val warmMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, rAdd,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, bAdd,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.timesAssign(warmMatrix)
    }

    // 5. Sepia Tone
    if (sepia > 0f) {
        val s = (sepia / 100f).coerceIn(0f, 1f)
        val sepiaMatrix = ColorMatrix(floatArrayOf(
            1f - 0.607f * s, 0.769f * s, 0.189f * s, 0f, 0f,
            0.349f * s, 1f - 0.314f * s, 0.168f * s, 0f, 0f,
            0.272f * s, 0.534f * s, 1f - 0.869f * s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.timesAssign(sepiaMatrix)
    }

    // 6. Hue rotation in YIQ color space
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

    // 7. Signature & Ink Clean (High-Contrast Luminance Threshold)
    if (inkClean) {
        val thresh = inkThreshold.coerceIn(0.1f, 0.9f)
        val boost = 12f
        val offset = -thresh * boost * 255f
        val inkMatrix = ColorMatrix(floatArrayOf(
            0.299f * boost, 0.587f * boost, 0.114f * boost, 0f, offset,
            0.299f * boost, 0.587f * boost, 0.114f * boost, 0f, offset,
            0.299f * boost, 0.587f * boost, 0.114f * boost, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.timesAssign(inkMatrix)
    }

    return cm
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN CHROMA SOCIAL PHOTO STUDIO WORKSPACE SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashPhotoEditorScreen(
    photoUris: List<Uri>,
    initialSlideIndex: Int = 0,
    nextButtonLabel: String = "Next",
    onNext: (filterName: String, thumbnailBitmap: Bitmap?, durationPerSlideMs: Long) -> Unit,
    onExportUris: (List<Uri>) -> Unit = {},
    onSaveDraft: (filterName: String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    // Active Slide in Multi-Photo / Slideshow
    var previewIndex by remember(photoUris, initialSlideIndex) {
        mutableIntStateOf(initialSlideIndex.coerceIn(0, (photoUris.size - 1).coerceAtLeast(0)))
    }
    val previewUri = remember(photoUris, previewIndex) { photoUris.getOrNull(previewIndex) }
    val isSlideshow = photoUris.size > 1

    // Undo / Redo Stacks
    val undoStack = remember { mutableStateListOf<StudioPhotoSnapshot>() }
    val redoStack = remember { mutableStateListOf<StudioPhotoSnapshot>() }

    // Active Category Dock
    var activeCategory by remember { mutableStateOf(StudioEditorCategory.LOOKS) }
    var isExporting by remember { mutableStateOf(false) }
    var isComparingOriginal by remember { mutableStateOf(false) }

    // Transform State
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var fineRotation by remember { mutableFloatStateOf(0f) }
    var rotation90 by remember { mutableFloatStateOf(0f) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var selectedAspectRatio by remember { mutableStateOf(PhotoAspectRatio.FREE) }
    var showRuleOfThirds by remember { mutableStateOf(false) }
    var showGoldenRatio by remember { mutableStateOf(false) }

    // Bottom Sheet Drawer State
    var drawerLevel by remember { mutableStateOf(PhotoDrawerLevel.MID) }
    var dragAccum by remember { mutableFloatStateOf(0f) }

    // Filter & Grading State
    var selectedFilter by remember { mutableStateOf(StudioFilterPreset.ORIGINAL) }
    var filterIntensity by remember { mutableFloatStateOf(1.0f) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var warmth by remember { mutableFloatStateOf(0f) }
    var sepia by remember { mutableFloatStateOf(0f) }
    var blur by remember { mutableFloatStateOf(0f) }
    var isBw by remember { mutableStateOf(false) }
    var hue by remember { mutableFloatStateOf(0f) }
    var inkClean by remember { mutableStateOf(false) }
    var inkThreshold by remember { mutableFloatStateOf(0.5f) }

    // Text Overlay State
    var overlayText by remember { mutableStateOf("") }
    var textColor by remember { mutableStateOf(Color.White) }
    var textBgStyle by remember { mutableStateOf("solid") } // "none", "pill", "glass", "outline"

    // Frames & Studio Backdrop State
    var frameWidthPx by remember { mutableFloatStateOf(0f) }
    var frameColor by remember { mutableStateOf(Color.White) }
    var backdropType by remember { mutableStateOf(StudioBackdropType.TRANSPARENT) }

    // Slideshow Duration State
    var durationPerSlideMs by remember { mutableLongStateOf(3000L) }
    val maxDurationPerSlide = if (isSlideshow) (30_000L / photoUris.size.coerceAtLeast(1)).coerceIn(1000L, 10_000L) else 3000L

    // Quality & Export State
    var exportQuality by remember { mutableIntStateOf(90) }
    var exportFormat by remember { mutableStateOf("image/jpeg") }

    fun captureCurrentState() = StudioPhotoSnapshot(
        filter = selectedFilter,
        filterIntensity = filterIntensity,
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        warmth = warmth,
        sepia = sepia,
        blur = blur,
        isBw = isBw,
        hue = hue,
        inkClean = inkClean,
        inkThreshold = inkThreshold,
        aspectRatio = selectedAspectRatio,
        fineRotation = fineRotation,
        rotation90 = rotation90,
        flipH = flipH,
        flipV = flipV,
        frameWidthPx = frameWidthPx,
        frameColor = frameColor,
        backdropType = backdropType,
        overlayText = overlayText,
        textColor = textColor,
        textBgStyle = textBgStyle,
        quality = exportQuality
    )

    fun pushSnapshot() {
        undoStack.add(captureCurrentState())
        redoStack.clear()
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            redoStack.add(captureCurrentState())
            val prev = undoStack.removeAt(undoStack.lastIndex)
            selectedFilter = prev.filter
            filterIntensity = prev.filterIntensity
            brightness = prev.brightness
            contrast = prev.contrast
            saturation = prev.saturation
            warmth = prev.warmth
            sepia = prev.sepia
            blur = prev.blur
            isBw = prev.isBw
            hue = prev.hue
            inkClean = prev.inkClean
            inkThreshold = prev.inkThreshold
            selectedAspectRatio = prev.aspectRatio
            fineRotation = prev.fineRotation
            rotation90 = prev.rotation90
            flipH = prev.flipH
            flipV = prev.flipV
            frameWidthPx = prev.frameWidthPx
            frameColor = prev.frameColor
            backdropType = prev.backdropType
            overlayText = prev.overlayText
            textColor = prev.textColor
            textBgStyle = prev.textBgStyle
            exportQuality = prev.quality
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            undoStack.add(captureCurrentState())
            val next = redoStack.removeAt(redoStack.lastIndex)
            selectedFilter = next.filter
            filterIntensity = next.filterIntensity
            brightness = next.brightness
            contrast = next.contrast
            saturation = next.saturation
            warmth = next.warmth
            sepia = next.sepia
            blur = next.blur
            isBw = next.isBw
            hue = next.hue
            inkClean = next.inkClean
            inkThreshold = next.inkThreshold
            selectedAspectRatio = next.aspectRatio
            fineRotation = next.fineRotation
            rotation90 = next.rotation90
            flipH = next.flipH
            flipV = next.flipV
            frameWidthPx = next.frameWidthPx
            frameColor = next.frameColor
            backdropType = next.backdropType
            overlayText = next.overlayText
            textColor = next.textColor
            textBgStyle = next.textBgStyle
            exportQuality = next.quality
        }
    }

    fun resetAll() {
        pushSnapshot()
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        selectedFilter = StudioFilterPreset.ORIGINAL
        filterIntensity = 1.0f
        brightness = 0f
        contrast = 1f
        saturation = 1f
        warmth = 0f
        sepia = 0f
        blur = 0f
        isBw = false
        hue = 0f
        inkClean = false
        inkThreshold = 0.5f
        selectedAspectRatio = PhotoAspectRatio.FREE
        zoomScale = 1f
        panOffsetX = 0f
        panOffsetY = 0f
        fineRotation = 0f
        rotation90 = 0f
        flipH = false
        flipV = false
        frameWidthPx = 0f
        backdropType = StudioBackdropType.TRANSPARENT
        overlayText = ""
        Toast.makeText(context, "Workspace reset to original", Toast.LENGTH_SHORT).show()
    }

    // Active Live Combined Matrix
    val combinedMatrix = remember(
        selectedFilter, filterIntensity, brightness, contrast,
        saturation, warmth, sepia, hue, isBw, inkClean, inkThreshold, isComparingOriginal
    ) {
        if (isComparingOriginal) {
            ColorMatrix() // Raw Identity Matrix
        } else {
            buildStudioCombinedColorMatrix(
                preset = selectedFilter,
                intensity = filterIntensity,
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                warmth = warmth,
                sepia = sepia,
                hue = hue,
                isBw = isBw,
                inkClean = inkClean,
                inkThreshold = inkThreshold
            )
        }
    }

    // Coil Image Request
    val imageRequest = remember(previewUri) {
        previewUri?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    }

    // Export Logic
    fun handleExportAndNext() {
        val uri = previewUri ?: return
        isExporting = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            val renderedBitmap = renderStudioPhotoBitmap(
                context = context,
                uri = uri,
                colorMatrix = combinedMatrix,
                totalRotation = rotation90 + fineRotation,
                flipX = flipH,
                flipY = flipV,
                aspectRatio = selectedAspectRatio,
                borderWidthPx = frameWidthPx,
                borderColor = frameColor,
                backdropType = backdropType,
                blur = blur,
                hasStamp = overlayText.isNotBlank(),
                stampName = overlayText,
                stampDate = "",
                stampPosition = "bottom",
                quality = exportQuality,
                textColor = textColor,
                textBgStyle = textBgStyle,
                zoomScale = zoomScale,
                panOffsetX = panOffsetX,
                panOffsetY = panOffsetY
            )

            // Render all photos for multi-photo / slideshow export so all settings apply before cloud upload
            val renderedUris = mutableListOf<Uri>()
            if (photoUris.size > 1) {
                for ((idx, pUri) in photoUris.withIndex()) {
                    if (idx == previewIndex && renderedBitmap != null) {
                        try {
                            val file = File(context.cacheDir, "flash_slide_${System.currentTimeMillis()}_$idx.jpg")
                            withContext(Dispatchers.IO) {
                                FileOutputStream(file).use { out ->
                                    renderedBitmap.compress(Bitmap.CompressFormat.JPEG, exportQuality.coerceIn(80, 100), out)
                                }
                            }
                            renderedUris.add(Uri.fromFile(file))
                        } catch (_: Throwable) {
                            renderedUris.add(pUri)
                        }
                    } else {
                        val slideBmp = renderStudioPhotoBitmap(
                            context = context,
                            uri = pUri,
                            colorMatrix = combinedMatrix,
                            totalRotation = rotation90 + fineRotation,
                            flipX = flipH,
                            flipY = flipV,
                            aspectRatio = selectedAspectRatio,
                            borderWidthPx = frameWidthPx,
                            borderColor = frameColor,
                            backdropType = backdropType,
                            blur = blur,
                            hasStamp = overlayText.isNotBlank(),
                            stampName = overlayText,
                            stampDate = "",
                            stampPosition = "bottom",
                            quality = exportQuality,
                            textColor = textColor,
                            textBgStyle = textBgStyle
                        )
                        if (slideBmp != null) {
                            try {
                                val file = File(context.cacheDir, "flash_slide_${System.currentTimeMillis()}_$idx.jpg")
                                withContext(Dispatchers.IO) {
                                    FileOutputStream(file).use { out ->
                                        slideBmp.compress(Bitmap.CompressFormat.JPEG, exportQuality.coerceIn(80, 100), out)
                                    }
                                }
                                renderedUris.add(Uri.fromFile(file))
                            } catch (_: Throwable) {
                                renderedUris.add(pUri)
                            } finally {
                                if (!slideBmp.isRecycled) slideBmp.recycle()
                            }
                        } else {
                            renderedUris.add(pUri)
                        }
                    }
                }
            } else if (renderedBitmap != null) {
                try {
                    val file = File(context.cacheDir, "flash_single_${System.currentTimeMillis()}.jpg")
                    withContext(Dispatchers.IO) {
                        FileOutputStream(file).use { out ->
                            renderedBitmap.compress(Bitmap.CompressFormat.JPEG, exportQuality.coerceIn(80, 100), out)
                        }
                    }
                    renderedUris.add(Uri.fromFile(file))
                } catch (_: Throwable) {
                    renderedUris.add(uri)
                }
            }

            isExporting = false
            onExportUris(renderedUris)
            onNext(selectedFilter.displayName, renderedBitmap, durationPerSlideMs)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WORKSPACE ROOT CONTAINER
    // ─────────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // ── 1. STUDIO HEADER (Simple & Clean) ───────────────────────────
            ChromaScreenHeader(
                title = "Edit Photo",
                badgeText = if (isSlideshow) "${previewIndex + 1}/${photoUris.size}" else null,
                badgeColor = colors.primary,
                showBackButton = true,
                circularBackButton = false,
                onBack = onBack,
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Undo Action
                        if (undoStack.isNotEmpty()) {
                            IconButton(
                                onClick = { handleUndo() },
                                modifier = Modifier.size(36.dp).minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    Lucide.Undo2,
                                    contentDescription = "Undo Action",
                                    tint = colors.textMain,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Compare with Original Toggle
                        IconButton(
                            onClick = {
                                isComparingOriginal = !isComparingOriginal
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.size(36.dp).minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Lucide.Eye,
                                contentDescription = "Compare with Original",
                                tint = if (isComparingOriginal) colors.primary else colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Reset Edits
                        IconButton(
                            onClick = { resetAll() },
                            modifier = Modifier.size(36.dp).minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                Lucide.RotateCcw,
                                contentDescription = "Reset Workspace",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Next / Done Button
                        Button(
                            onClick = { handleExportAndNext() },
                            enabled = !isExporting,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = Color.White,
                                disabledContainerColor = colors.primary.copy(alpha = 0.5f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    nextButtonLabel,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            )

            // ── 2. MULTI-PHOTO FILMSTRIP (If Multiple Images) ────────────────
            if (isSlideshow) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(photoUris.indices.toList()) { index ->
                        val isSelected = index == previewIndex
                        val itemUri = photoUris[index]
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) colors.primary else colors.glassBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    previewIndex = index
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(itemUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Photo Slide ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. WORKSPACE CANVAS (Center Viewport) ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (colors.isDark) Color(0xFF0C0E12) else Color(0xFFF1F3F5))
                    .border(0.5.dp, colors.glassBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val ratio = selectedAspectRatio.ratio
                val gestureModifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                scope.launch {
                                    val startZoom = zoomScale
                                    val startPanX = panOffsetX
                                    val startPanY = panOffsetY
                                    val targetZoom = if (startZoom > 1.05f) 1f else 2.0f
                                    val anim = androidx.compose.animation.core.Animatable(0f)
                                    anim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
                                    ) {
                                        zoomScale = startZoom + (targetZoom - startZoom) * value
                                        if (targetZoom == 1f) {
                                            panOffsetX = startPanX * (1f - value)
                                            panOffsetY = startPanY * (1f - value)
                                        }
                                    }
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onLongPress = {
                                isComparingOriginal = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onPress = {
                                tryAwaitRelease()
                                if (isComparingOriginal) {
                                    isComparingOriginal = false
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newZoom = (zoomScale * zoom).coerceIn(0.85f, 4.0f)
                            zoomScale = newZoom
                            if (newZoom > 1f) {
                                val maxPan = 400f * (newZoom - 1f)
                                panOffsetX = (panOffsetX + pan.x).coerceIn(-maxPan, maxPan)
                                panOffsetY = (panOffsetY + pan.y).coerceIn(-maxPan, maxPan)
                            } else {
                                panOffsetX = (panOffsetX + pan.x * 0.15f).coerceIn(-25f, 25f)
                                panOffsetY = (panOffsetY + pan.y * 0.15f).coerceIn(-25f, 25f)
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = (if (flipH) -1f else 1f) * zoomScale
                        scaleY = (if (flipV) -1f else 1f) * zoomScale
                        translationX = panOffsetX
                        translationY = panOffsetY
                        rotationZ = rotation90 + fineRotation
                        clip = true
                        shape = RoundedCornerShape(12.dp)
                    }

                val canvasModifier = (if (ratio != null) {
                    Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .aspectRatio(ratio, matchHeightConstraintsFirst = false)
                } else {
                    Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                }).then(gestureModifier)

                Box(
                    modifier = canvasModifier,
                    contentAlignment = Alignment.Center
                ) {
                    if (imageRequest != null) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = "Studio Workspace Photo Preview",
                            contentScale = if (ratio != null) ContentScale.Crop else ContentScale.Fit,
                            colorFilter = ColorFilter.colorMatrix(combinedMatrix),
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (blur > 0.05f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Modifier.graphicsLayer {
                                            renderEffect = android.graphics.RenderEffect
                                                .createBlurEffect(blur * 2f, blur * 2f, android.graphics.Shader.TileMode.CLAMP)
                                                .asComposeRenderEffect()
                                        }
                                    } else Modifier
                                )
                        )
                    }

                    // Rule of Thirds Overlay (Inside Photo Canvas Context)
                    if (showRuleOfThirds) {
                        StudioRuleOfThirdsOverlay(modifier = Modifier.fillMaxSize())
                    }

                    // StudioFaceGuideOverlay (Passport face guide removed for clean social post editor)



                    // Golden Ratio Overlay (Inside Photo Canvas Context)
                    if (showGoldenRatio) {
                        StudioGoldenRatioOverlay(modifier = Modifier.fillMaxSize())
                    }

                    // Frame / Border Overlay
                    if (frameWidthPx > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = (frameWidthPx * 0.75f).dp,
                                    color = frameColor,
                                    shape = RoundedCornerShape(0.dp)
                                )
                        )
                    }

                    // Dynamic Typography Overlay
                    if (overlayText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .then(
                                    when (textBgStyle) {
                                        "solid" -> Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.75f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                        "glass" -> Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.glassBg.copy(alpha = 0.88f))
                                            .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                        "outline" -> Modifier
                                            .border(1.5.dp, textColor, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                        else -> Modifier.padding(8.dp)
                                    }
                                )
                        ) {
                            Text(
                                text = overlayText,
                                color = textColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Top-Left Comparison Indicator
                if (isComparingOriginal) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(colors.primary.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ORIGINAL",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── 4. CHROMA BOTTOM SHEET DRAWER (Tools & Controls) ─────────────
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenHeightDp = configuration.screenHeightDp.dp
            val targetDrawerHeight = when (drawerLevel) {
                PhotoDrawerLevel.COLLAPSED -> 68.dp
                PhotoDrawerLevel.MID -> minOf(330.dp, screenHeightDp * 0.44f)
                PhotoDrawerLevel.FULL -> minOf(520.dp, screenHeightDp * 0.70f)
            }

            val animatedDrawerHeight by animateDpAsState(
                targetValue = targetDrawerHeight,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "photoDrawerHeight"
            )

            val drawerShadowColor = if (colors.isDark) Color.Black.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.04f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedDrawerHeight)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        ambientColor = drawerShadowColor,
                        spotColor = drawerShadowColor
                    )
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(colors.glassBg)
                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Draggable Grab Bar Handle (Expand / Collapse Bottom Drawer)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (dragAccum < -35f) {
                                            drawerLevel = when (drawerLevel) {
                                                PhotoDrawerLevel.COLLAPSED -> PhotoDrawerLevel.MID
                                                PhotoDrawerLevel.MID -> PhotoDrawerLevel.FULL
                                                PhotoDrawerLevel.FULL -> PhotoDrawerLevel.FULL
                                            }
                                        } else if (dragAccum > 35f) {
                                            drawerLevel = when (drawerLevel) {
                                                PhotoDrawerLevel.FULL -> PhotoDrawerLevel.MID
                                                PhotoDrawerLevel.MID -> PhotoDrawerLevel.COLLAPSED
                                                PhotoDrawerLevel.COLLAPSED -> PhotoDrawerLevel.COLLAPSED
                                            }
                                        }
                                        dragAccum = 0f
                                    },
                                    onVerticalDrag = { _, dragAmount ->
                                        dragAccum += dragAmount
                                    }
                                )
                            }
                            .clickable {
                                drawerLevel = if (drawerLevel == PhotoDrawerLevel.COLLAPSED) PhotoDrawerLevel.MID else PhotoDrawerLevel.COLLAPSED
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(colors.textMuted.copy(alpha = 0.35f))
                        )
                    }

                    // Tool Categories Row (4 Essential Tools, perfectly balanced, zero scrolling)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudioEditorCategory.values().forEach { category ->
                            val isActive = activeCategory == category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isActive && drawerLevel != PhotoDrawerLevel.COLLAPSED)
                                            colors.primary.copy(alpha = 0.08f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        activeCategory = category
                                        if (drawerLevel == PhotoDrawerLevel.COLLAPSED) {
                                            drawerLevel = PhotoDrawerLevel.MID
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = category.title,
                                        tint = if (isActive && drawerLevel != PhotoDrawerLevel.COLLAPSED) colors.primary else colors.textMuted,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = category.title,
                                        color = if (isActive && drawerLevel != PhotoDrawerLevel.COLLAPSED) colors.primary else colors.textMain,
                                        fontSize = 12.sp,
                                        fontWeight = if (isActive && drawerLevel != PhotoDrawerLevel.COLLAPSED) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Expanded Drawer Content Deck
                    if (drawerLevel != PhotoDrawerLevel.COLLAPSED) {
                        HorizontalDivider(
                            color = colors.glassBorder.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        // Sub-header with Tool Title and Action Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = activeCategory.icon,
                                    contentDescription = activeCategory.title,
                                    tint = colors.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = activeCategory.title,
                                    color = colors.textMain,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        drawerLevel = PhotoDrawerLevel.COLLAPSED
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    modifier = Modifier.size(32.dp).minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Lucide.ChevronDown,
                                        contentDescription = "Collapse Drawer to Full View",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Active Tool Panel Content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                        ) {
                            when (activeCategory) {
                                StudioEditorCategory.LOOKS -> {
                                    StudioLooksToolPanel(
                                        selectedFilter = selectedFilter,
                                        onSelectFilter = { pushSnapshot(); selectedFilter = it },
                                        intensity = filterIntensity,
                                        onIntensityChange = { filterIntensity = it }
                                    )
                                }
                                StudioEditorCategory.ADJUST -> {
                                    StudioGradingToolPanel(
                                        brightness = brightness,
                                        onBrightnessChange = { brightness = it },
                                        contrast = contrast,
                                        onContrastChange = { contrast = it },
                                        saturation = saturation,
                                        onSaturationChange = { saturation = it },
                                        warmth = warmth,
                                        onWarmthChange = { warmth = it },
                                        onAutoEnhance = {
                                            pushSnapshot()
                                            brightness = 6f
                                            contrast = 1.15f
                                            saturation = 1.18f
                                            warmth = 8f
                                            Toast.makeText(context, "Auto enhanced", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                                StudioEditorCategory.CROP -> {
                                    StudioCropToolPanel(
                                        selectedAspectRatio = selectedAspectRatio,
                                        onSelectAspectRatio = { pushSnapshot(); selectedAspectRatio = it },
                                        showRuleOfThirds = showRuleOfThirds,
                                        onToggleRuleOfThirds = { showRuleOfThirds = it },
                                        onRotate90Cw = { pushSnapshot(); rotation90 = (rotation90 + 90f) % 360f },
                                        onFlipH = { pushSnapshot(); flipH = !flipH },
                                        onFlipV = { pushSnapshot(); flipV = !flipV }
                                    )
                                }
                                StudioEditorCategory.TYPE -> {
                                    StudioTypographyToolPanel(
                                        overlayText = overlayText,
                                        onTextChange = { overlayText = it },
                                        textColor = textColor,
                                        onColorChange = { pushSnapshot(); textColor = it },
                                        bgStyle = textBgStyle,
                                        onBgStyleChange = { pushSnapshot(); textBgStyle = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOOL PANEL IMPLEMENTATIONS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StudioCropToolPanel(
    selectedAspectRatio: PhotoAspectRatio,
    onSelectAspectRatio: (PhotoAspectRatio) -> Unit,
    showRuleOfThirds: Boolean,
    onToggleRuleOfThirds: (Boolean) -> Unit,
    onRotate90Cw: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Aspect Ratios Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(PhotoAspectRatio.values()) { ratioItem ->
                val isSelected = selectedAspectRatio == ratioItem
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.primary else colors.glassBg)
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) colors.primary else colors.glassBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onSelectAspectRatio(ratioItem)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = ratioItem.label,
                        color = if (isSelected) Color.White else colors.textMain,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.4f), thickness = 0.5.dp)

        // Rotate & Flip Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    onRotate90Cw()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier.size(48.dp).minimumInteractiveComponentSize()
            ) {
                Icon(
                    Lucide.RotateCw,
                    contentDescription = "Rotate 90 Degrees Clockwise",
                    tint = colors.textMain,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = {
                    onFlipH()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier.size(48.dp).minimumInteractiveComponentSize()
            ) {
                Icon(
                    Lucide.FlipHorizontal,
                    contentDescription = "Flip Photo Horizontally",
                    tint = colors.textMain,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = {
                    onFlipV()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier.size(48.dp).minimumInteractiveComponentSize()
            ) {
                Icon(
                    Lucide.FlipVertical,
                    contentDescription = "Flip Photo Vertically",
                    tint = colors.textMain,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.4f), thickness = 0.5.dp)

        // Composition Grid Toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Lucide.Grid3x3, contentDescription = "Rule of Thirds Grid Guide", tint = colors.primary, modifier = Modifier.size(18.dp))
                Text("Grid Overlay", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            AppleSwitch(
                checked = showRuleOfThirds,
                onCheckedChange = {
                    onToggleRuleOfThirds(it)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )
        }
    }
}

@Composable
private fun StudioLooksToolPanel(
    selectedFilter: StudioFilterPreset,
    onSelectFilter: (StudioFilterPreset) -> Unit,
    intensity: Float,
    onIntensityChange: (Float) -> Unit
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Intensity Slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Intensity",
                color = colors.textMain,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(68.dp)
            )

            AppleSlider(
                value = intensity,
                onValueChange = onIntensityChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${(intensity * 100).toInt()}%",
                color = colors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }

        HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

        // Filter Cards
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(StudioFilterPreset.values()) { preset ->
                val isSelected = selectedFilter == preset
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.glassBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) colors.primary else colors.glassBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            onSelectFilter(preset)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .widthIn(min = 90.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (preset) {
                                    StudioFilterPreset.ORIGINAL -> Brush.linearGradient(listOf(Color(0xFFBDBDBD), Color(0xFF757575)))
                                    StudioFilterPreset.CINEMATIC -> Brush.linearGradient(listOf(Color(0xFF455A64), Color(0xFF8D6E63)))
                                    StudioFilterPreset.NORDIC_GLOW -> Brush.linearGradient(listOf(Color(0xFF90A4AE), Color(0xFFCFD8DC)))
                                    StudioFilterPreset.VIVID -> Brush.linearGradient(listOf(Color(0xFFBF360C), Color(0xFFD84315)))
                                    StudioFilterPreset.NOIR -> Brush.linearGradient(listOf(Color(0xFF212121), Color(0xFF616161)))
                                    StudioFilterPreset.BW_CLASSIC -> Brush.linearGradient(listOf(Color(0xFF424242), Color(0xFF9E9E9E)))
                                    StudioFilterPreset.WARM_SUNSET -> Brush.linearGradient(listOf(Color(0xFFBF360C), Color(0xFFFFA000)))
                                    StudioFilterPreset.COOL_SLATE -> Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF607D8B)))
                                    StudioFilterPreset.SOFT_FADE -> Brush.linearGradient(listOf(Color(0xFF9E9E9E), Color(0xFFE0E0E0)))
                                    StudioFilterPreset.SEPIA_VINTAGE -> Brush.linearGradient(listOf(Color(0xFF6D4C41), Color(0xFFA1887F)))
                                    StudioFilterPreset.MOODY_EMERALD -> Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF388E3C)))
                                    StudioFilterPreset.PASTEL_MIST -> Brush.linearGradient(listOf(Color(0xFFB0BEC5), Color(0xFFCFD8DC)))
                                    StudioFilterPreset.CHROME_MONO -> Brush.linearGradient(listOf(Color(0xFF78909C), Color(0xFFB0BEC5)))
                                }
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = preset.displayName,
                        color = if (isSelected) colors.primary else colors.textMain,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioGradingToolPanel(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    warmth: Float,
    onWarmthChange: (Float) -> Unit,
    onAutoEnhance: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // One-Tap Auto Enhance
        GlassButton(
            onClick = {
                onAutoEnhance()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(36.dp)
        ) {
            Icon(Lucide.Sparkles, contentDescription = "Auto Enhance Image", tint = colors.primary, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Auto Enhance", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.4f), thickness = 0.5.dp)

        // 4 Core Photo Tuning Sliders
        GradingSliderRow(
            label = "Brightness",
            value = brightness,
            onValueChange = onBrightnessChange,
            range = -100f..100f,
            formatValue = { "${it.toInt()}" },
            onReset = { onBrightnessChange(0f) }
        )

        GradingSliderRow(
            label = "Contrast",
            value = contrast,
            onValueChange = onContrastChange,
            range = 0.5f..2.0f,
            formatValue = { String.format(java.util.Locale.US, "%.2f", it) },
            onReset = { onContrastChange(1.0f) }
        )

        GradingSliderRow(
            label = "Saturation",
            value = saturation,
            onValueChange = onSaturationChange,
            range = 0f..3.0f,
            formatValue = { String.format(java.util.Locale.US, "%.2f", it) },
            onReset = { onSaturationChange(1.0f) }
        )

        GradingSliderRow(
            label = "Warmth",
            value = warmth,
            onValueChange = onWarmthChange,
            range = -100f..100f,
            formatValue = { "${it.toInt()}" },
            onReset = { onWarmthChange(0f) }
        )
    }
}

@Composable
private fun GradingSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    formatValue: (Float) -> String,
    onReset: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = colors.textMain,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(68.dp)
        )

        AppleSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = formatValue(value),
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )

        IconButton(
            onClick = {
                onReset()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            modifier = Modifier.size(48.dp).minimumInteractiveComponentSize()
        ) {
            Icon(Lucide.RefreshCcw, contentDescription = "Reset $label Adjustment", tint = colors.textMuted, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun StudioToneAndInkPanel(
    hue: Float,
    onHueChange: (Float) -> Unit,
    inkClean: Boolean,
    onToggleInkClean: (Boolean) -> Unit,
    inkThreshold: Float,
    onInkThresholdChange: (Float) -> Unit
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Hue Shift Slider
        GradingSliderRow(
            label = "Hue Shift",
            value = hue,
            onValueChange = onHueChange,
            range = -180f..180f,
            formatValue = { "${it.toInt()}°" },
            onReset = { onHueChange(0f) }
        )

        HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

        // Signature & Ink Clean Section
        Text(
            text = "SIGNATURE & INK CLEAN",
            color = colors.textMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Clean Ink & Signatures", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("High-contrast binarization threshold", color = colors.textMuted, fontSize = 10.sp)
            }
            AppleSwitch(
                checked = inkClean,
                onCheckedChange = {
                    onToggleInkClean(it)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )
        }

        if (inkClean) {
            GradingSliderRow(
                label = "Threshold",
                value = inkThreshold,
                onValueChange = onInkThresholdChange,
                range = 0.1f..0.9f,
                formatValue = { "${(it * 100).toInt()}%" },
                onReset = { onInkThresholdChange(0.5f) }
            )
        }
    }
}

@Composable
private fun StudioTypographyToolPanel(
    overlayText: String,
    onTextChange: (String) -> Unit,
    textColor: Color,
    onColorChange: (Color) -> Unit,
    bgStyle: String,
    onBgStyleChange: (String) -> Unit
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Text Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.glassBg)
                .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = overlayText,
                onValueChange = onTextChange,
                textStyle = TextStyle(
                    color = colors.textMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(colors.primary),
                decorationBox = { innerTextField ->
                    if (overlayText.isEmpty()) {
                        Text("Add headline, caption, or studio watermark...", color = colors.textMuted, fontSize = 13.sp)
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Style Options (Clean, professional tags)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("none" to "Plain", "solid" to "Solid", "glass" to "Glass", "outline" to "Outline").forEach { (styleKey, label) ->
                val isSelected = bgStyle == styleKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.glassBg)
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) colors.primary else colors.glassBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onBgStyleChange(styleKey)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.primary else colors.textMain,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // Color Palette
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val palette = listOf(
                Color(0xFFFFFFFF), // Pure White
                Color(0xFFF5F5F0), // Soft Ivory
                Color(0xFFD8D8D8), // Light Gray
                Color(0xFF757575), // Charcoal Slate
                Color(0xFF1E2124), // Deep Black
                Color(0xFFC48B71), // Warm Terracotta
                Color(0xFF8F9E8B), // Muted Sage
                Color(0xFF6B8299), // Slate Denim
                Color(0xFFB59D77)  // Warm Sand
            )
            items(palette) { colorOption ->
                val isSelected = textColor == colorOption
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorOption)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) colors.primary else Color.White.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            onColorChange(colorOption)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                )
            }
        }
    }
}

@Composable
private fun StudioBorderToolPanel(
    frameWidthPx: Float,
    onFrameWidthChange: (Float) -> Unit,
    frameColor: Color,
    onFrameColorChange: (Color) -> Unit,
    backdropType: StudioBackdropType,
    onSelectBackdrop: (StudioBackdropType) -> Unit,
    isSlideshow: Boolean,
    durationPerSlideMs: Long,
    onDurationChange: (Long) -> Unit,
    maxDuration: Long
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Border Width Slider
        GradingSliderRow(
            label = "Thickness",
            value = frameWidthPx,
            onValueChange = onFrameWidthChange,
            range = 0f..40f,
            formatValue = { "${it.toInt()}px" },
            onReset = { onFrameWidthChange(0f) }
        )

        // Border Color Palette
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val palette = listOf(
                Color(0xFFFFFFFF), // White
                Color(0xFFF5F5F0), // Off-White
                Color(0xFF9E9E9E), // Neutral Gray
                Color(0xFF455A64), // Slate
                Color(0xFF1E2124), // Dark
                Color(0xFFC48B71), // Terracotta
                Color(0xFFB59D77)  // Sand
            )
            items(palette) { colorOption ->
                val isSelected = frameColor == colorOption
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorOption)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) colors.primary else Color.White.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            onFrameColorChange(colorOption)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                )
            }
        }

        HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

        // Backdrop Types
        Text(
            text = "STUDIO BACKDROP",
            color = colors.textMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(StudioBackdropType.values()) { backdrop ->
                val isSelected = backdropType == backdrop
                val label = when (backdrop) {
                    StudioBackdropType.TRANSPARENT -> "None"
                    StudioBackdropType.SOLID_WHITE -> "White"
                    StudioBackdropType.SOLID_GRAY -> "Gray"
                    StudioBackdropType.SOLID_BLACK -> "Black"
                    StudioBackdropType.STUDIO_GRADIENT_BLUE -> "Studio Blue"
                    StudioBackdropType.STUDIO_GRADIENT_DARK -> "Studio Dark"
                    StudioBackdropType.STUDIO_WARM -> "Warm Sand"
                    StudioBackdropType.BLUR_ORIGINAL -> "Blur Original"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.primary else colors.glassBg)
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) colors.primary else colors.glassBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            onSelectBackdrop(backdrop)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else colors.textMain,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Slideshow Duration
        if (isSlideshow) {
            HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
            GradingSliderRow(
                label = "Duration",
                value = (durationPerSlideMs / 1000f),
                onValueChange = { onDurationChange((it * 1000).toLong()) },
                range = 1f..(maxDuration / 1000f),
                formatValue = { String.format(java.util.Locale.US, "%.1fs", it) },
                onReset = { onDurationChange(3000L) }
            )
        }
    }
}

// Note: Social Media Post Editor — Print Layout Engine with cut marks and PdfDocument export
// (including SubjectSegmentation and AIGenerator compliance checks) removed per user requirements.

// ─────────────────────────────────────────────────────────────────────────────
// COMPOSITION GUIDES OVERLAYS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StudioRuleOfThirdsOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gridColor = Color.White.copy(alpha = 0.28f)
        val strokeWidth = 0.75.dp.toPx()

        drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth)
        drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth)
        drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth)
        drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth)
    }
}

@Composable
private fun StudioFaceGuideOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val guideColor = Color(0xFF00E5FF).copy(alpha = 0.65f)
        val strokeStyle = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f))

        // Head Oval
        val headWidth = w * 0.44f
        val headHeight = h * 0.44f
        val headLeft = (w - headWidth) / 2f
        val headTop = h * 0.16f
        drawOval(
            color = guideColor,
            topLeft = Offset(headLeft, headTop),
            size = Size(headWidth, headHeight),
            style = strokeStyle
        )

        // Eye Level Line
        drawLine(guideColor.copy(alpha = 0.4f), Offset(headLeft, headTop + headHeight * 0.45f), Offset(headLeft + headWidth, headTop + headHeight * 0.45f), 1.dp.toPx())

        // Shoulder guidelines
        val shoulderPath = Path()
        shoulderPath.moveTo(w * 0.20f, h)
        shoulderPath.quadraticBezierTo(w * 0.20f, h * 0.72f, w * 0.50f, h * 0.72f)
        shoulderPath.quadraticBezierTo(w * 0.80f, h * 0.72f, w * 0.80f, h)
        drawPath(shoulderPath, guideColor, style = strokeStyle)
    }
}

@Composable
private fun StudioGoldenRatioOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val phi = 0.618f
        val guideColor = Color.White.copy(alpha = 0.28f)
        val strokeWidth = 0.75.dp.toPx()

        drawLine(guideColor, Offset(w * (1f - phi), 0f), Offset(w * (1f - phi), h), strokeWidth)
        drawLine(guideColor, Offset(w * phi, 0f), Offset(w * phi, h), strokeWidth)
        drawLine(guideColor, Offset(0f, h * (1f - phi)), Offset(w, h * (1f - phi)), strokeWidth)
        drawLine(guideColor, Offset(0f, h * phi), Offset(w, h * phi), strokeWidth)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BITMAP RENDERING PIPELINE (Two-Pass, Single Canvas Pass, Memory-Safe)
// ─────────────────────────────────────────────────────────────────────────────

private suspend fun renderStudioPhotoBitmap(
    context: Context,
    uri: Uri,
    colorMatrix: ColorMatrix,
    totalRotation: Float,
    flipX: Boolean,
    flipY: Boolean,
    aspectRatio: PhotoAspectRatio,
    borderWidthPx: Float,
    borderColor: Color,
    backdropType: StudioBackdropType,
    blur: Float,
    hasStamp: Boolean,
    stampName: String,
    stampDate: String,
    stampPosition: String,
    quality: Int,
    textColor: Color = Color.White,
    textBgStyle: String = "solid",
    zoomScale: Float = 1f,
    panOffsetX: Float = 0f,
    panOffsetY: Float = 0f
): Bitmap? = withContext(Dispatchers.IO) {
    var original: Bitmap? = null
    var finalOutput: Bitmap? = null
    try {
        // Pass 1: Measure bounds only
        val boundsOptions = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val rawW = boundsOptions.outWidth
        val rawH = boundsOptions.outHeight
        if (rawW <= 0 || rawH <= 0) return@withContext null

        val maxDimension = 1920f
        var inSampleSize = 1
        val longestEdge = maxOf(rawW, rawH)
        while ((longestEdge / (inSampleSize * 2)) >= maxDimension) {
            inSampleSize *= 2
        }

        // Pass 2: Decode scaled
        val decodeOptions = android.graphics.BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        original = context.contentResolver.openInputStream(uri)?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return@withContext null

        val origW = original.width
        val origH = original.height

        // Trigonometric rotation bounding box
        val radians = Math.toRadians(totalRotation.toDouble())
        val abscos = Math.abs(Math.cos(radians))
        val abssin = Math.abs(Math.sin(radians))
        val intermediateWidth = (origW * abscos + origH * abssin).toInt().coerceAtLeast(1)
        val intermediateHeight = (origW * abssin + origH * abscos).toInt().coerceAtLeast(1)

        val ratio = aspectRatio.ratio
        val finalW: Int
        val finalH: Int
        if (ratio != null) {
            val currentRatio = intermediateWidth.toFloat() / intermediateHeight.toFloat()
            if (currentRatio > ratio) {
                finalW = (intermediateHeight * ratio).toInt().coerceAtLeast(1)
                finalH = intermediateHeight
            } else {
                finalW = intermediateWidth
                finalH = (intermediateWidth / ratio).toInt().coerceAtLeast(1)
            }
        } else {
            finalW = intermediateWidth
            finalH = intermediateHeight
        }

        // Single Canvas draw pass directly into finalOutput (0 intermediate allocations)
        finalOutput = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
        val outCanvas = Canvas(finalOutput)

        // Draw Backdrop
        when (backdropType) {
            StudioBackdropType.SOLID_WHITE -> outCanvas.drawColor(android.graphics.Color.WHITE)
            StudioBackdropType.SOLID_BLACK -> outCanvas.drawColor(android.graphics.Color.BLACK)
            StudioBackdropType.SOLID_GRAY -> outCanvas.drawColor(android.graphics.Color.DKGRAY)
            StudioBackdropType.BLUR_ORIGINAL -> {
                val blurPaint = Paint().apply {
                    maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
                }
                outCanvas.drawBitmap(original, 0f, 0f, blurPaint)
            }
            StudioBackdropType.STUDIO_GRADIENT_BLUE -> {
                val gradPaint = Paint().apply {
                    shader = android.graphics.LinearGradient(
                        0f, 0f, finalW.toFloat(), finalH.toFloat(),
                        android.graphics.Color.parseColor("#0B132B"),
                        android.graphics.Color.parseColor("#1C2541"),
                        android.graphics.Shader.TileMode.CLAMP
                    )
                }
                outCanvas.drawRect(0f, 0f, finalW.toFloat(), finalH.toFloat(), gradPaint)
            }
            StudioBackdropType.STUDIO_GRADIENT_DARK -> {
                val gradPaint = Paint().apply {
                    shader = android.graphics.LinearGradient(
                        0f, 0f, 0f, finalH.toFloat(),
                        android.graphics.Color.parseColor("#161616"),
                        android.graphics.Color.parseColor("#080808"),
                        android.graphics.Shader.TileMode.CLAMP
                    )
                }
                outCanvas.drawRect(0f, 0f, finalW.toFloat(), finalH.toFloat(), gradPaint)
            }
            StudioBackdropType.STUDIO_WARM -> {
                val gradPaint = Paint().apply {
                    shader = android.graphics.LinearGradient(
                        0f, 0f, finalW.toFloat(), finalH.toFloat(),
                        android.graphics.Color.parseColor("#1C1917"),
                        android.graphics.Color.parseColor("#0C0A09"),
                        android.graphics.Shader.TileMode.CLAMP
                    )
                }
                outCanvas.drawRect(0f, 0f, finalW.toFloat(), finalH.toFloat(), gradPaint)
            }
            else -> {}
        }

        // Draw Transformed Image with Matrix
        val matrix = android.graphics.Matrix()
        matrix.postTranslate(-origW / 2f, -origH / 2f)
        if (flipX) matrix.postScale(-1f, 1f)
        if (flipY) matrix.postScale(1f, -1f)
        if (totalRotation != 0f) matrix.postRotate(totalRotation)
        if (zoomScale != 1f) {
            matrix.postScale(zoomScale, zoomScale)
        }
        val panScale = (finalW.toFloat() / 360f).coerceIn(0.5f, 4f)
        matrix.postTranslate(finalW / 2f + panOffsetX * panScale, finalH / 2f + panOffsetY * panScale)

        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix(colorMatrix.values))
            if (blur > 0.05f) {
                maskFilter = BlurMaskFilter(blur * 2f, BlurMaskFilter.Blur.NORMAL)
            }
        }
        outCanvas.drawBitmap(original, matrix, imagePaint)

        // Draw Frame / Border
        if (borderWidthPx > 0f) {
            val strokePaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = borderWidthPx * (finalW / 300f).coerceAtLeast(1f)
                color = android.graphics.Color.argb(
                    (borderColor.alpha * 255).toInt(),
                    (borderColor.red * 255).toInt(),
                    (borderColor.green * 255).toInt(),
                    (borderColor.blue * 255).toInt()
                )
            }
            outCanvas.drawRect(0f, 0f, finalW.toFloat(), finalH.toFloat(), strokePaint)
        }

        // Draw Text Overlay (Styled with exact textColor and container style)
        if (hasStamp && stampName.isNotBlank()) {
            val textSizePx = (finalH * 0.045f).coerceIn(28f, 84f)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(
                    (textColor.alpha * 255).toInt(),
                    (textColor.red * 255).toInt(),
                    (textColor.green * 255).toInt(),
                    (textColor.blue * 255).toInt()
                )
                textSize = textSizePx
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(8f, 0f, 2f, android.graphics.Color.BLACK)
            }

            val textWidth = textPaint.measureText(stampName)
            val fontMetrics = textPaint.fontMetrics
            val centerX = finalW / 2f
            val centerY = finalH * 0.5f
            val padH = textSizePx * 0.65f
            val padV = textSizePx * 0.35f
            val bgRect = RectF(
                centerX - textWidth / 2f - padH,
                centerY + fontMetrics.top - padV,
                centerX + textWidth / 2f + padH,
                centerY + fontMetrics.bottom + padV
            )
            val cornerRadius = textSizePx * 0.3f

            when (textBgStyle) {
                "solid" -> {
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(192, 0, 0, 0)
                        style = Paint.Style.FILL
                    }
                    outCanvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)
                }
                "glass" -> {
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(220, 30, 35, 45)
                        style = Paint.Style.FILL
                    }
                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(120, 255, 255, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = (textSizePx * 0.06f).coerceAtLeast(1.5f)
                    }
                    outCanvas.drawRoundRect(bgRect, cornerRadius * 1.5f, cornerRadius * 1.5f, bgPaint)
                    outCanvas.drawRoundRect(bgRect, cornerRadius * 1.5f, cornerRadius * 1.5f, borderPaint)
                }
                "outline" -> {
                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = textPaint.color
                        style = Paint.Style.STROKE
                        strokeWidth = (textSizePx * 0.08f).coerceAtLeast(2f)
                    }
                    outCanvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, borderPaint)
                }
                else -> {}
            }

            val baseline = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
            outCanvas.drawText(stampName, centerX, baseline, textPaint)
        }

        finalOutput
    } catch (e: Exception) {
        null
    } finally {
        if (original != null && original != finalOutput) original.recycle()
    }
}
