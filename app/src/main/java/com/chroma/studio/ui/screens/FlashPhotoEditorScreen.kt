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
import com.chroma.studio.sticker.StickerItem
import com.chroma.studio.sticker.StickerOverlayItem
import com.chroma.studio.sticker.StickerPickerSheet
import com.chroma.studio.sticker.StickerStudioModal
import com.chroma.studio.sticker.StickerRepository
import android.graphics.BitmapFactory
import kotlin.math.roundToInt
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
    TYPE("Text", Lucide.Type),
    STICKERS("Stickers", Lucide.Sticker)
}

enum class StudioAdjustmentNode(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val range: ClosedFloatingPointRange<Float>,
    val defaultVal: Float
) {
    EXPOSURE("Exposure", Lucide.Sun, -100f..100f, 0f),
    CONTRAST("Contrast", Lucide.Contrast, 0.5f..2.0f, 1.0f),
    SATURATION("Saturation", Lucide.Palette, 0f..3.0f, 1.0f),
    WARMTH("Warmth", Lucide.Flame, -100f..100f, 0f),
    SEPIA("Sepia", Lucide.Clock, 0f..100f, 0f),
    HUE("Hue", Lucide.RefreshCw, -180f..180f, 0f),
    INK("Ink", Lucide.PenTool, 0.1f..0.9f, 0.5f)
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
    title: String = "Edit Photo",
    initialAspectRatio: PhotoAspectRatio = PhotoAspectRatio.FREE,
    nextButtonLabel: String = "Next",
    onNext: (filterName: String, thumbnailBitmap: Bitmap?, durationPerSlideMs: Long) -> Unit = { _, _, _ -> },
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
    var selectedAspectRatio by remember(photoUris) { mutableStateOf(initialAspectRatio) }
    var showRuleOfThirds by remember { mutableStateOf(false) }
    var showGoldenRatio by remember { mutableStateOf(false) }

    // Bottom Sheet Drawer State
    var drawerLevel by remember { mutableStateOf(PhotoDrawerLevel.MID) }
    var dragAccum by remember { mutableFloatStateOf(0f) }

    // Filter & Grading State
    var selectedFilter by remember { mutableStateOf(StudioFilterPreset.ORIGINAL) }
    var filterIntensity by remember { mutableFloatStateOf(1.0f) }
    var activeAdjustmentNode by remember { mutableStateOf(StudioAdjustmentNode.EXPOSURE) }
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

    // Interactive Stickers Layer State
    val activeStickers = remember { mutableStateListOf<StickerOverlayItem>() }
    var isStickerPickerOpen by remember { mutableStateOf(false) }
    var isStickerStudioOpen by remember { mutableStateOf(false) }
    var selectedStickerId by remember { mutableStateOf<String?>(null) }

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
        activeStickers.clear()
        selectedStickerId = null
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
                panOffsetY = panOffsetY,
                stickers = activeStickers.toList()
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
                            textBgStyle = textBgStyle,
                            stickers = activeStickers.toList()
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
                title = title,
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

                    // Interactive Custom Stickers Layer (with 5px white die-cut border)
                    for (sticker in activeStickers) {
                        val isSelected = selectedStickerId == sticker.id
                        var sOffset by remember(sticker.id) { mutableStateOf(sticker.offset) }
                        var sScale by remember(sticker.id) { mutableFloatStateOf(sticker.scale) }
                        var sRot by remember(sticker.id) { mutableFloatStateOf(sticker.rotation) }

                        Box(
                            modifier = Modifier
                                .offset { androidx.compose.ui.unit.IntOffset(sOffset.x.roundToInt(), sOffset.y.roundToInt()) }
                                .graphicsLayer {
                                    scaleX = sScale
                                    scaleY = sScale
                                    rotationZ = sRot
                                }
                                .pointerInput(sticker.id) {
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        sOffset += pan
                                        sticker.offset = sOffset
                                        sScale = (sScale * zoom).coerceIn(0.25f, 4f)
                                        sticker.scale = sScale
                                        sRot += rotation
                                        sticker.rotation = sRot
                                        selectedStickerId = sticker.id
                                    }
                                }
                                .clickable {
                                    selectedStickerId = if (isSelected) null else sticker.id
                                }
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .border(1.5.dp, colors.primary, RoundedCornerShape(10.dp))
                                            .padding(4.dp)
                                    } else Modifier.padding(4.dp)
                                )
                        ) {
                            Image(
                                bitmap = sticker.bitmap.asImageBitmap(),
                                contentDescription = "Sticker Overlay",
                                modifier = Modifier.size(110.dp)
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                        .clickable {
                                            activeStickers.remove(sticker)
                                            selectedStickerId = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Lucide.X,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
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

            // ── 4. CHROMA BOTTOM DOCK (Unified Tool Console & Category Bar) ─────────
            val dockShadowColor = if (colors.isDark) Color.Black.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.05f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        ambientColor = dockShadowColor,
                        spotColor = dockShadowColor
                    )
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(colors.glassBg)
                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Upper Shelf: Active Tool Panel (~130dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 10.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
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
                                StudioAdjustToolPanel(
                                    activeNode = activeAdjustmentNode,
                                    onSelectNode = { activeAdjustmentNode = it },
                                    brightness = brightness,
                                    onBrightnessChange = { pushSnapshot(); brightness = it },
                                    contrast = contrast,
                                    onContrastChange = { pushSnapshot(); contrast = it },
                                    saturation = saturation,
                                    onSaturationChange = { pushSnapshot(); saturation = it },
                                    warmth = warmth,
                                    onWarmthChange = { pushSnapshot(); warmth = it },
                                    sepia = sepia,
                                    onSepiaChange = { pushSnapshot(); sepia = it },
                                    hue = hue,
                                    onHueChange = { pushSnapshot(); hue = it },
                                    inkClean = inkClean,
                                    inkThreshold = inkThreshold,
                                    onInkChange = { clean, thresh ->
                                        pushSnapshot()
                                        inkClean = clean
                                        inkThreshold = thresh
                                    },
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
                            StudioEditorCategory.STICKERS -> {
                                StudioStickerToolPanel(
                                    activeStickersCount = activeStickers.size,
                                    onAddStickerClick = { isStickerPickerOpen = true },
                                    onCreateCustomClick = { isStickerStudioOpen = true },
                                    onClearAll = {
                                        activeStickers.clear()
                                        selectedStickerId = null
                                    }
                                )
                            }
                        }
                    }

                    // Hairline Divider
                    HorizontalDivider(
                        color = colors.glassBorder.copy(alpha = 0.5f),
                        thickness = 0.5.dp
                    )

                    // Lower Shelf: Category Navigation Dock (~54dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudioEditorCategory.values().forEach { category ->
                            val isActive = activeCategory == category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isActive) colors.primary.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        activeCategory = category
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
                                        tint = if (isActive) colors.primary else colors.textMuted,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = category.title,
                                        color = if (isActive) colors.primary else colors.textMain,
                                        fontSize = 12.sp,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
            val addStickerFromItem: (StickerItem) -> Unit = { item ->
                scope.launch {
                    val u = Uri.parse(item.uri)
                    val bmp = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(u)?.use {
                                BitmapFactory.decodeStream(it)
                            }
                        } catch (_: Exception) { null }
                    }
                    if (bmp != null) {
                        activeStickers.add(StickerOverlayItem(bitmap = bmp))
                    }
                }
            }

            if (isStickerPickerOpen) {
                StickerPickerSheet(
                    isOpen = isStickerPickerOpen,
                    onDismiss = { isStickerPickerOpen = false },
                    onStickerSelected = { sticker ->
                        addStickerFromItem(sticker)
                    },
                    onCreateCustomSticker = {
                        isStickerStudioOpen = true
                    }
                )
            }

            if (isStickerStudioOpen) {
                StickerStudioModal(
                    isOpen = isStickerStudioOpen,
                    onDismiss = { isStickerStudioOpen = false },
                    onStickerCreated = { sticker ->
                        addStickerFromItem(sticker)
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOOL PANEL IMPLEMENTATIONS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StudioStickerToolPanel(
    activeStickersCount: Int,
    onAddStickerClick: () -> Unit,
    onCreateCustomClick: () -> Unit,
    onClearAll: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onAddStickerClick,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).height(42.dp)
        ) {
            Icon(Lucide.Sticker, contentDescription = "Add Sticker", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Stickers", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = onCreateCustomClick,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, colors.primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
            modifier = Modifier.weight(1f).height(42.dp)
        ) {
            Icon(Lucide.Sparkles, contentDescription = "Cutout Sticker", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cutout", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }

        if (activeStickersCount > 0) {
            IconButton(
                onClick = onClearAll,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Lucide.Trash2,
                    contentDescription = "Clear Stickers",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

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

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Action Tools: Rotate, Flip H, Flip V, Grid
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
                modifier = Modifier.size(44.dp).minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Lucide.RotateCw,
                    contentDescription = "Rotate 90 Degrees",
                    tint = colors.textMain,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = {
                    onFlipH()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier.size(44.dp).minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Lucide.FlipHorizontal,
                    contentDescription = "Flip Horizontal",
                    tint = colors.textMain,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = {
                    onFlipV()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier.size(44.dp).minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Lucide.FlipVertical,
                    contentDescription = "Flip Vertical",
                    tint = colors.textMain,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = {
                    onToggleRuleOfThirds(!showRuleOfThirds)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (showRuleOfThirds) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Lucide.Grid3x3,
                    contentDescription = "Grid Guide",
                    tint = if (showRuleOfThirds) colors.primary else colors.textMain,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Aspect Ratios
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
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
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
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

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Slider or Filter Name row
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedFilter.displayName,
                color = colors.textMain,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (selectedFilter != StudioFilterPreset.ORIGINAL) {
                Row(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppleSlider(
                        value = intensity,
                        onValueChange = onIntensityChange,
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "${(intensity * 100).toInt()}%",
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }
        }

        // Filter cards carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(StudioFilterPreset.values()) { preset ->
                val isSelected = selectedFilter == preset
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.glassBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) colors.primary else colors.glassBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            onSelectFilter(preset)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .widthIn(min = 60.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
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
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = preset.displayName,
                        color = if (isSelected) colors.primary else colors.textMain,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioAdjustToolPanel(
    activeNode: StudioAdjustmentNode,
    onSelectNode: (StudioAdjustmentNode) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    warmth: Float,
    onWarmthChange: (Float) -> Unit,
    sepia: Float,
    onSepiaChange: (Float) -> Unit,
    hue: Float,
    onHueChange: (Float) -> Unit,
    inkClean: Boolean,
    inkThreshold: Float,
    onInkChange: (Boolean, Float) -> Unit,
    onAutoEnhance: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    val currentVal = when (activeNode) {
        StudioAdjustmentNode.EXPOSURE -> brightness
        StudioAdjustmentNode.CONTRAST -> contrast
        StudioAdjustmentNode.SATURATION -> saturation
        StudioAdjustmentNode.WARMTH -> warmth
        StudioAdjustmentNode.SEPIA -> sepia
        StudioAdjustmentNode.HUE -> hue
        StudioAdjustmentNode.INK -> inkThreshold
    }

    val defaultVal = activeNode.defaultVal
    val isNodeModified = kotlin.math.abs(currentVal - defaultVal) > 0.01f || (activeNode == StudioAdjustmentNode.INK && inkClean)

    val formatVal: (Float) -> String = when (activeNode) {
        StudioAdjustmentNode.EXPOSURE -> { v -> if (v > 0) "+${v.toInt()}" else "${v.toInt()}" }
        StudioAdjustmentNode.CONTRAST, StudioAdjustmentNode.SATURATION -> { v -> String.format(java.util.Locale.US, "%.2f", v) }
        StudioAdjustmentNode.WARMTH -> { v -> if (v > 0) "+${v.toInt()}" else "${v.toInt()}" }
        StudioAdjustmentNode.SEPIA -> { v -> "${v.toInt()}%" }
        StudioAdjustmentNode.HUE -> { v -> if (v > 0) "+${v.toInt()}°" else "${v.toInt()}°" }
        StudioAdjustmentNode.INK -> { v -> "${(v * 100).toInt()}%" }
    }

    val onValueChange: (Float) -> Unit = { newVal ->
        when (activeNode) {
            StudioAdjustmentNode.EXPOSURE -> onBrightnessChange(newVal)
            StudioAdjustmentNode.CONTRAST -> onContrastChange(newVal)
            StudioAdjustmentNode.SATURATION -> onSaturationChange(newVal)
            StudioAdjustmentNode.WARMTH -> onWarmthChange(newVal)
            StudioAdjustmentNode.SEPIA -> onSepiaChange(newVal)
            StudioAdjustmentNode.HUE -> onHueChange(newVal)
            StudioAdjustmentNode.INK -> onInkChange(true, newVal)
        }
    }

    val onResetNode: () -> Unit = {
        when (activeNode) {
            StudioAdjustmentNode.EXPOSURE -> onBrightnessChange(0f)
            StudioAdjustmentNode.CONTRAST -> onContrastChange(1.0f)
            StudioAdjustmentNode.SATURATION -> onSaturationChange(1.0f)
            StudioAdjustmentNode.WARMTH -> onWarmthChange(0f)
            StudioAdjustmentNode.SEPIA -> onSepiaChange(0f)
            StudioAdjustmentNode.HUE -> onHueChange(0f)
            StudioAdjustmentNode.INK -> onInkChange(false, 0.5f)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 1. Header Row: Title + Readout + Reset Button
        Row(
            modifier = Modifier.fillMaxWidth().height(26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = activeNode.title,
                    color = colors.textMain,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isNodeModified) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatVal(currentVal),
                    color = if (isNodeModified) colors.primary else colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isNodeModified) {
                    IconButton(
                        onClick = {
                            onResetNode()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier.size(24.dp).minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Lucide.RotateCcw,
                            contentDescription = "Reset ${activeNode.title}",
                            tint = colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // 2. Active Precision Slider
        AppleSlider(
            value = currentVal,
            onValueChange = onValueChange,
            valueRange = activeNode.range,
            modifier = Modifier.fillMaxWidth().height(28.dp)
        )

        // 3. Node Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Auto Chip
            item {
                val isAutoModified = brightness != 0f || contrast != 1f || saturation != 1f || warmth != 0f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.glassBg)
                        .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            onAutoEnhance()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Sparkles,
                        contentDescription = "Auto",
                        tint = if (isAutoModified) colors.primary else colors.textMain,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Auto",
                        color = if (isAutoModified) colors.primary else colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Adjustment nodes
            items(StudioAdjustmentNode.values()) { node ->
                val isSelected = activeNode == node
                val isModified = when (node) {
                    StudioAdjustmentNode.EXPOSURE -> kotlin.math.abs(brightness) > 0.01f
                    StudioAdjustmentNode.CONTRAST -> kotlin.math.abs(contrast - 1f) > 0.01f
                    StudioAdjustmentNode.SATURATION -> kotlin.math.abs(saturation - 1f) > 0.01f
                    StudioAdjustmentNode.WARMTH -> kotlin.math.abs(warmth) > 0.01f
                    StudioAdjustmentNode.SEPIA -> sepia > 0.01f
                    StudioAdjustmentNode.HUE -> kotlin.math.abs(hue) > 0.01f
                    StudioAdjustmentNode.INK -> inkClean
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) colors.primary.copy(alpha = 0.12f)
                            else colors.glassBg
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) colors.primary else colors.glassBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onSelectNode(node)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = node.icon,
                            contentDescription = node.title,
                            tint = if (isSelected) colors.primary else colors.textMain,
                            modifier = Modifier.size(18.dp)
                        )
                        if (isModified) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 3.dp, y = (-2).dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = node.title,
                        color = if (isSelected) colors.primary else colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
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

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact Text Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.glassBg)
                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Lucide.Type,
                    contentDescription = "Text Tool",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = overlayText,
                    onValueChange = onTextChange,
                    textStyle = TextStyle(
                        color = colors.textMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    decorationBox = { innerTextField ->
                        if (overlayText.isEmpty()) {
                            Text("Add text...", color = colors.textMuted, fontSize = 13.sp)
                        }
                        innerTextField()
                    },
                    modifier = Modifier.weight(1f)
                )
                if (overlayText.isNotEmpty()) {
                    IconButton(
                        onClick = { onTextChange("") },
                        modifier = Modifier.size(20.dp).minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Clear Text",
                            tint = colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Styles and Color Palette Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Style Pills: Plain, Solid, Glass, Outline
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("none" to "Plain", "solid" to "Solid", "glass" to "Glass", "outline" to "Outline").forEach { (styleKey, label) ->
                    val isSelected = bgStyle == styleKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.glassBg)
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) colors.primary else colors.glassBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                onBgStyleChange(styleKey)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) colors.primary else colors.textMain,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // Quick Color Dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Color.White,
                    Color.Black,
                    colors.primary,
                    Color(0xFFEF4444),
                    Color(0xFFF59E0B),
                    Color(0xFF06B6D4)
                ).forEach { colorOption ->
                    val isSelected = textColor == colorOption
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colorOption)
                            .border(
                                width = if (isSelected) 2.dp else 0.5.dp,
                                color = if (isSelected) colors.primary else Color.White.copy(alpha = 0.5f),
                                shape = CircleShape
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
}


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
    panOffsetY: Float = 0f,
    stickers: List<StickerOverlayItem> = emptyList()
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

        // Draw Placed Stickers (preserving 5px white die-cut border, scale, and rotation)
        for (stk in stickers) {
            val stkMatrix = android.graphics.Matrix()
            val stkOrigW = stk.bitmap.width.toFloat()
            val stkOrigH = stk.bitmap.height.toFloat()
            stkMatrix.postTranslate(-stkOrigW / 2f, -stkOrigH / 2f)
            stkMatrix.postScale(stk.scale, stk.scale)
            stkMatrix.postRotate(stk.rotation)
            val pScale = (finalW.toFloat() / 360f).coerceIn(0.5f, 4f)
            val relX = finalW / 2f + stk.offset.x * pScale
            val relY = finalH / 2f + stk.offset.y * pScale
            stkMatrix.postTranslate(relX, relY)
            val stkPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            outCanvas.drawBitmap(stk.bitmap, stkMatrix, stkPaint)
        }

        finalOutput
    } catch (e: Exception) {
        null
    } finally {
        if (original != null && original != finalOutput) original.recycle()
    }
}
