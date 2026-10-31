package com.chroma.studio.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient as AndroidRadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.chroma.studio.ui.components.AppleDialog
import com.chroma.studio.ui.components.AppleSlider
import com.chroma.studio.ui.components.AppleSwitch
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.DynamicIslandManager
import com.chroma.studio.ui.components.GalleryLayoutMode
import com.chroma.studio.ui.components.GalleryMediaTypeFilter
import com.chroma.studio.ui.components.GlassBottomSheet
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.IosAlertDialog
import com.chroma.studio.ui.components.LocalHazeState
import com.chroma.studio.ui.components.OscellionGalleryLayout
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.graphics3d.AppleSettingsSquircleVectorIcon
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════════════
// PHOTO EDITOR MODELS (Shares FilterType, Adjustments, TransformState, BlendMode,
// VideoFitMode, ProjectRatio, Clip, MediaType, AppIconBox & PropertySlider with
// ChromaVideoEditorScreen in package com.chroma.studio.ui.screens)
// ═══════════════════════════════════════════════════════════════════════════════

enum class PhotoEditorTool(val label: String) {
    CROP("Crop"),
    FILTER("Filter"),
    ADJUST("Adjust"),
    CURVES("Curves"),
    COLOR_MIX("Color Mix"),
    EFFECTS("Effects"),
    TRANSFORM("Transform"),
    TEXT("Text"),
    CHROMA_KEY("Chroma Key"),
    BLEND("Blend"),
    MARKUP("Markup"),
    FRAME("Frame")
}

enum class LightLeakType(val label: String, val colors: List<Color>) {
    NONE("None", listOf(Color.Transparent, Color.Transparent)),
    SOLAR_FLARE("Solar Flare", listOf(Color(0x99FF5E00), Color(0x55FFB800), Color.Transparent)),
    PRISM_BURN("Prism Burn", listOf(Color(0x88FF007A), Color(0x667928CA), Color.Transparent)),
    CYBER_NEON("Cyber Neon", listOf(Color(0x8800F0FF), Color(0x667000FF), Color.Transparent)),
    GOLDEN_35MM("35mm Gold", listOf(Color(0x99FF9500), Color(0x44FF2D55), Color.Transparent)),
    AURORA_MIST("Aurora", listOf(Color(0x7700FF94), Color(0x55007AFF), Color.Transparent))
}

enum class ToneCurveChannel(val label: String, val color: Color) {
    MASTER("RGB", Color(0xFF1C1C1E)),
    RED("Red", Color(0xFFFF3B30)),
    GREEN("Green", Color(0xFF34C759)),
    BLUE("Blue", Color(0xFF007AFF))
}

data class ToneCurvePoints(
    val blacks: Float = 0f,     // -1f to 1f
    val shadows: Float = 0f,    // -1f to 1f
    val midtones: Float = 0f,   // -1f to 1f
    val highlights: Float = 0f, // -1f to 1f
    val whites: Float = 0f      // -1f to 1f
) {
    val isIdentity: Boolean
        get() = abs(blacks) < 0.01f && abs(shadows) < 0.01f && abs(midtones) < 0.01f &&
                abs(highlights) < 0.01f && abs(whites) < 0.01f
}

data class SplitToningState(
    val shadowHue: Float = 220f,       // 0f to 360f
    val shadowStrength: Float = 0f,    // 0f to 1f
    val highlightHue: Float = 40f,     // 0f to 360f
    val highlightStrength: Float = 0f, // 0f to 1f
    val balance: Float = 0f            // -1f to 1f
)

enum class PhotoFrameStyle(val label: String) {
    NONE("None"),
    GALLERY_WHITE("Gallery"),
    POLAROID("Polaroid"),
    FILM_BORDER("35mm Film"),
    CINEMA_LETTERBOX("Cinema 2.35:1"),
    NEON_GLOW("Neon Frame")
}

data class BrushStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Offset> = emptyList(),
    val color: Color = Color(0xFF007AFF),
    val strokeWidth: Float = 12f,
    val isNeon: Boolean = false
)

data class PhotoExportSettings(
    val resolution: Int = 2160, // 1080 (FHD), 2160 (4K), 4320 (8K)
    val format: String = "PNG", // "PNG", "JPEG", "WEBP"
    val quality: Int = 95       // 80, 95, 100
)

data class PhotoProjectState(
    val clips: List<Clip> = emptyList(),
    val selectedClipId: String? = null,
    val pixelsPerSecond: Float = 60f,
    val trackVisibility: Map<Int, Boolean> = mapOf(1 to true, 2 to true, 3 to true, 4 to true, 5 to true),
    val trackLock: Map<Int, Boolean> = mapOf(1 to false, 2 to false, 3 to false, 4 to false, 5 to false),
    val videoFitMode: VideoFitMode = VideoFitMode.FIT,
    val projectRatio: ProjectRatio = ProjectRatio.RATIO_9_16,
    val frameStyle: PhotoFrameStyle = PhotoFrameStyle.NONE,
    val framePadding: Float = 0f,
    val frameCornerRadius: Float = 0f,
    val frameColor: Color = Color.White,
    val filterIntensity: Float = 1.0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val sharpness: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val dehaze: Float = 0f,
    val bloom: Float = 0f,
    val chromaticAberration: Float = 0f,
    val lightLeak: LightLeakType = LightLeakType.NONE,
    val lightLeakIntensity: Float = 0.65f,
    val activeCurveChannel: ToneCurveChannel = ToneCurveChannel.MASTER,
    val toneCurves: ToneCurvePoints = ToneCurvePoints(),
    val redCurve: ToneCurvePoints = ToneCurvePoints(),
    val greenCurve: ToneCurvePoints = ToneCurvePoints(),
    val blueCurve: ToneCurvePoints = ToneCurvePoints(),
    val splitToning: SplitToningState = SplitToningState(),
    val flipHorizontalMap: Map<String, Boolean> = emptyMap(),
    val flipVerticalMap: Map<String, Boolean> = emptyMap(),
    val brushStrokes: List<BrushStroke> = emptyList(),
    val activeBrushColor: Color = Color(0xFF007AFF),
    val activeBrushSize: Float = 12f,
    val activeBrushNeon: Boolean = false,
    val isDrawingMode: Boolean = false,
    val showGridOverlay: Boolean = false,
    val showHistogramHud: Boolean = false,
    val isComparingOriginal: Boolean = false,
    val isSplitCompareMode: Boolean = false,
    val splitCompareFraction: Float = 0.5f,
    val exportSettings: PhotoExportSettings = PhotoExportSettings()
)

// ═══════════════════════════════════════════════════════════════════════════════
// LIVE PREVIEW COLOR PIPELINE (Matches ChromaVideoEditorScreen + Photo Optics)
// ═══════════════════════════════════════════════════════════════════════════════

private fun buildPhotoPreviewColorMatrix(
    filter: FilterType,
    filterIntensity: Float,
    adj: Adjustments,
    warmth: Float = 0f,
    tint: Float = 0f,
    sharpness: Float = 0f,
    dehaze: Float = 0f,
    toneCurves: ToneCurvePoints = ToneCurvePoints(),
    redCurve: ToneCurvePoints = ToneCurvePoints(),
    greenCurve: ToneCurvePoints = ToneCurvePoints(),
    blueCurve: ToneCurvePoints = ToneCurvePoints(),
    splitToning: SplitToningState = SplitToningState()
): android.graphics.ColorMatrix {
    val result = android.graphics.ColorMatrix()

    // Saturation + Clarity + Dehaze vibrance boost
    val effectiveSat = (adj.saturation * (1f + sharpness * 0.12f + dehaze * 0.15f)).coerceIn(0f, 3f)
    result.postConcat(android.graphics.ColorMatrix().apply { setSaturation(effectiveSat) })

    // Hue rotation
    if (adj.hue != 0f) {
        val radians = Math.toRadians(adj.hue.toDouble())
        val cosVal = cos(radians).toFloat()
        val sinVal = sin(radians).toFloat()
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f
        val hueValues = floatArrayOf(
            lumR + cosVal * (1 - lumR) + sinVal * (-lumR), lumG + cosVal * (-lumG) + sinVal * (-lumG), lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0f, 0f,
            lumR + cosVal * (-lumR) + sinVal * (0.143f), lumG + cosVal * (1 - lumG) + sinVal * (0.140f), lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0f, 0f,
            lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)), lumG + cosVal * (-lumG) + sinVal * (lumG), lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        result.postConcat(android.graphics.ColorMatrix(hueValues))
    }

    // Brightness / Contrast / Exposure / Highlights / Shadows / Dehaze / Parametric Curves
    val exposureScale = 2.0.pow(adj.exposure.toDouble()).toFloat()
    val clarityScale = 1f + (sharpness * 0.2f) + (dehaze * 0.22f)
    val curveContrast = 1f + (toneCurves.highlights - toneCurves.shadows) * 0.35f + (toneCurves.whites - toneCurves.blacks) * 0.2f
    val contrastScale = adj.contrast * clarityScale * curveContrast.coerceIn(0.4f, 2.2f)
    val totalScale = exposureScale * contrastScale

    val curveOffset = (toneCurves.midtones * 45f) + (toneCurves.blacks * 38f) + (toneCurves.shadows * 24f) +
            (toneCurves.highlights * 20f) + (toneCurves.whites * 30f)

    val baseOffset = adj.brightness * 255f +
            (1f - contrastScale) * 128f +
            adj.highlights * 40f -
            adj.shadows * 40f -
            dehaze * 18f +
            curveOffset

    // Per-channel RGB curves & Split-Toning offsets
    val rScale = totalScale * (1f + (redCurve.highlights - redCurve.shadows) * 0.25f)
    val gScale = totalScale * (1f + (greenCurve.highlights - greenCurve.shadows) * 0.25f)
    val bScale = totalScale * (1f + (blueCurve.highlights - blueCurve.shadows) * 0.25f)

    val shRad = Math.toRadians(splitToning.shadowHue.toDouble())
    val hiRad = Math.toRadians(splitToning.highlightHue.toDouble())
    val splitR = (cos(shRad).toFloat() * splitToning.shadowStrength * 28f) + (cos(hiRad).toFloat() * splitToning.highlightStrength * 28f)
    val splitG = (sin(shRad).toFloat() * splitToning.shadowStrength * 22f) + (sin(hiRad).toFloat() * splitToning.highlightStrength * 22f)
    val splitB = (-cos(shRad).toFloat() * splitToning.shadowStrength * 28f) + (-cos(hiRad).toFloat() * splitToning.highlightStrength * 28f)

    val rOffset = baseOffset + warmth * 35f + tint * 12f + (redCurve.midtones * 40f) + splitR
    val gOffset = baseOffset - tint * 28f + (greenCurve.midtones * 40f) + splitG
    val bOffset = baseOffset - warmth * 35f + tint * 12f + (blueCurve.midtones * 40f) + splitB

    result.postConcat(
        android.graphics.ColorMatrix(
            floatArrayOf(
                rScale, 0f, 0f, 0f, rOffset,
                0f, gScale, 0f, 0f, gOffset,
                0f, 0f, bScale, 0f, bOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )

    // Preset filter look blended by filterIntensity
    val presetValues = filter.matrixValues()
    if (presetValues != null && filterIntensity > 0.01f) {
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val blended = FloatArray(20) { i ->
            identity[i] + (presetValues[i] - identity[i]) * filterIntensity.coerceIn(0f, 1f)
        }
        result.postConcat(android.graphics.ColorMatrix(blended))
    }

    return result
}

private fun android.graphics.ColorMatrix.toComposeColorMatrix(): ColorMatrix = ColorMatrix(this.array)

// ═══════════════════════════════════════════════════════════════════════════════
// VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

class ChromaPhotoEditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(PhotoProjectState())
    val state: StateFlow<PhotoProjectState> = _state.asStateFlow()

    private val undoStack = mutableListOf<PhotoProjectState>()
    private val redoStack = mutableListOf<PhotoProjectState>()
    private var dragSnapshot: PhotoProjectState? = null

    fun initialize(uri: String) {
        if (_state.value.clips.isNotEmpty()) return
        val baseClip = Clip(
            type = MediaType.IMAGE,
            name = "Main Photo",
            trackId = 1,
            uri = uri,
            duration = 10f,
            color = Color(0xFF3B82F6)
        )
        _state.update {
            it.copy(
                clips = listOf(baseClip),
                selectedClipId = baseClip.id
            )
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    private fun pushUndo() {
        undoStack.add(_state.value)
        if (undoStack.size > 25) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(_state.value)
        _state.value = undoStack.removeAt(undoStack.lastIndex)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(_state.value)
        _state.value = redoStack.removeAt(redoStack.lastIndex)
    }

    fun startDrag() {
        dragSnapshot = _state.value
    }

    fun endDrag() {
        dragSnapshot?.let {
            undoStack.add(it)
            if (undoStack.size > 25) undoStack.removeAt(0)
            redoStack.clear()
        }
        dragSnapshot = null
    }

    fun selectClip(id: String?) = _state.update { it.copy(selectedClipId = id) }

    fun setZoom(pps: Float) = _state.update { it.copy(pixelsPerSecond = pps.coerceIn(20f, 200f)) }

    fun setVideoFitMode(mode: VideoFitMode) {
        pushUndo()
        _state.update { it.copy(videoFitMode = mode) }
    }

    fun setProjectRatio(ratio: ProjectRatio) {
        pushUndo()
        _state.update { it.copy(projectRatio = ratio) }
    }

    fun addClip(type: MediaType, name: String, trackId: Int, color: Color, uri: String? = null) {
        pushUndo()
        val clip = Clip(
            type = type,
            name = name,
            trackId = trackId,
            color = color,
            uri = uri,
            startTime = 0f,
            duration = 10f
        )
        _state.update { it.copy(clips = it.clips + clip, selectedClipId = clip.id) }
    }

    fun duplicateSelected() {
        val clip = _state.value.clips.find { it.id == _state.value.selectedClipId } ?: return
        pushUndo()
        val dup = clip.copy(
            id = UUID.randomUUID().toString(),
            name = "${clip.name} Copy",
            transform = clip.transform.copy(x = clip.transform.x + 16f, y = clip.transform.y + 16f)
        )
        _state.update { it.copy(clips = it.clips + dup, selectedClipId = dup.id) }
        ToastManager.showToast("Layer duplicated")
    }

    fun deleteSelected() {
        val id = _state.value.selectedClipId ?: return
        val target = _state.value.clips.find { it.id == id } ?: return
        if (target.trackId == 1 && _state.value.clips.count { it.trackId == 1 } <= 1) {
            ToastManager.showToast("Main photo layer cannot be deleted")
            return
        }
        pushUndo()
        val remaining = _state.value.clips.filter { it.id != id }
        _state.update {
            it.copy(
                clips = remaining,
                selectedClipId = remaining.lastOrNull()?.id
            )
        }
    }

    fun updateClipTransform(clipId: String, transform: TransformState) {
        val clip = _state.value.clips.find { it.id == clipId } ?: return
        if (_state.value.trackLock[clip.trackId] == true) return
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clipId) it.copy(transform = transform) else it })
        }
    }

    private fun updateSelected(block: (Clip) -> Clip) {
        val id = _state.value.selectedClipId ?: _state.value.clips.firstOrNull()?.id ?: return
        _state.update { s -> s.copy(clips = s.clips.map { if (it.id == id) block(it) else it }) }
    }

    fun setFilter(f: FilterType) {
        pushUndo()
        updateSelected { it.copy(filter = f) }
    }

    fun setFilterIntensity(intensity: Float) {
        _state.update { it.copy(filterIntensity = intensity.coerceIn(0f, 1f)) }
    }

    fun setAdjustments(a: Adjustments) = updateSelected { it.copy(adjustments = a) }
    fun setTransform(t: TransformState) = updateSelected { it.copy(transform = t) }
    fun setText(t: String) = updateSelected { it.copy(text = t) }
    fun setTextColor(c: Color) {
        pushUndo()
        updateSelected { it.copy(textColor = c) }
    }
    fun setTextSize(s: Float) = updateSelected { it.copy(textSize = s.coerceIn(12f, 120f)) }
    fun setTextBold(b: Boolean) {
        pushUndo()
        updateSelected { it.copy(textBold = b) }
    }
    fun setTextItalic(i: Boolean) {
        pushUndo()
        updateSelected { it.copy(textItalic = i) }
    }
    fun setBlendMode(m: BlendMode) {
        pushUndo()
        updateSelected { it.copy(blendMode = m) }
    }

    fun updateClipChromaKeyEnabled(clipId: String, enabled: Boolean) {
        pushUndo()
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clipId) it.copy(chromaKeyEnabled = enabled) else it })
        }
    }

    fun updateClipChromaKeyIntensity(clipId: String, intensity: Float) {
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clipId) it.copy(chromaKeyIntensity = intensity) else it })
        }
    }

    fun updateClipChromaKeySpill(clipId: String, spill: Float) {
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clipId) it.copy(chromaKeySpill = spill) else it })
        }
    }

    fun rotate90Degrees() {
        pushUndo()
        updateSelected {
            it.copy(transform = it.transform.copy(rotation = (it.transform.rotation + 90f) % 360f))
        }
    }

    fun autoEnhance() {
        pushUndo()
        val main = _state.value.clips.find { it.trackId == 1 } ?: return
        val current = main.adjustments
        val isAlreadyEnhanced = abs(current.contrast - 1.16f) < 0.02f && abs(current.saturation - 1.2f) < 0.02f
        val nextAdj = if (isAlreadyEnhanced) {
            Adjustments()
        } else {
            Adjustments(
                brightness = 0.03f,
                contrast = 1.16f,
                saturation = 1.2f,
                exposure = 0.12f,
                highlights = -0.1f,
                shadows = 0.16f
            )
        }
        _state.update { s ->
            s.copy(
                clips = s.clips.map { if (it.id == main.id) it.copy(adjustments = nextAdj) else it },
                sharpness = if (isAlreadyEnhanced) 0f else 0.25f
            )
        }
        ToastManager.showToast(if (isAlreadyEnhanced) "Auto-Enhance reset" else "Magic Auto-Enhance applied")
    }

    fun setOptics(
        vignette: Float = _state.value.vignette,
        grain: Float = _state.value.grain,
        sharpness: Float = _state.value.sharpness,
        warmth: Float = _state.value.warmth,
        tint: Float = _state.value.tint,
        dehaze: Float = _state.value.dehaze,
        bloom: Float = _state.value.bloom,
        chromaticAberration: Float = _state.value.chromaticAberration
    ) {
        _state.update {
            it.copy(
                vignette = vignette,
                grain = grain,
                sharpness = sharpness,
                warmth = warmth,
                tint = tint,
                dehaze = dehaze,
                bloom = bloom,
                chromaticAberration = chromaticAberration
            )
        }
    }

    fun setLightLeak(type: LightLeakType, intensity: Float = _state.value.lightLeakIntensity) {
        _state.update { it.copy(lightLeak = type, lightLeakIntensity = intensity.coerceIn(0f, 1f)) }
    }

    fun setActiveCurveChannel(channel: ToneCurveChannel) {
        _state.update { it.copy(activeCurveChannel = channel) }
    }

    fun updateActiveCurvePoints(points: ToneCurvePoints) {
        _state.update { s ->
            when (s.activeCurveChannel) {
                ToneCurveChannel.MASTER -> s.copy(toneCurves = points)
                ToneCurveChannel.RED -> s.copy(redCurve = points)
                ToneCurveChannel.GREEN -> s.copy(greenCurve = points)
                ToneCurveChannel.BLUE -> s.copy(blueCurve = points)
            }
        }
    }

    fun resetCurves() {
        _state.update {
            it.copy(
                toneCurves = ToneCurvePoints(),
                redCurve = ToneCurvePoints(),
                greenCurve = ToneCurvePoints(),
                blueCurve = ToneCurvePoints()
            )
        }
    }

    fun updateSplitToning(splitToning: SplitToningState) {
        _state.update { it.copy(splitToning = splitToning) }
    }

    fun toggleFlipHorizontal(clipId: String) {
        pushUndo()
        _state.update { s ->
            val next = s.flipHorizontalMap.toMutableMap()
            next[clipId] = !(next[clipId] ?: false)
            s.copy(flipHorizontalMap = next)
        }
    }

    fun toggleFlipVertical(clipId: String) {
        pushUndo()
        _state.update { s ->
            val next = s.flipVerticalMap.toMutableMap()
            next[clipId] = !(next[clipId] ?: false)
            s.copy(flipVerticalMap = next)
        }
    }

    fun toggleHistogramHud() {
        _state.update { it.copy(showHistogramHud = !it.showHistogramHud) }
    }

    fun resetAllAdjustments() {
        pushUndo()
        updateSelected { it.copy(filter = FilterType.NONE, adjustments = Adjustments()) }
        _state.update {
            it.copy(
                filterIntensity = 1.0f,
                vignette = 0f,
                grain = 0f,
                sharpness = 0f,
                warmth = 0f,
                tint = 0f,
                dehaze = 0f,
                bloom = 0f,
                chromaticAberration = 0f,
                lightLeak = LightLeakType.NONE,
                toneCurves = ToneCurvePoints(),
                redCurve = ToneCurvePoints(),
                greenCurve = ToneCurvePoints(),
                blueCurve = ToneCurvePoints(),
                splitToning = SplitToningState()
            )
        }
        ToastManager.showToast("Reset all color adjustments")
    }

    fun setFrame(
        style: PhotoFrameStyle = _state.value.frameStyle,
        padding: Float = _state.value.framePadding,
        cornerRadius: Float = _state.value.frameCornerRadius,
        color: Color = _state.value.frameColor
    ) {
        _state.update {
            it.copy(
                frameStyle = style,
                framePadding = padding,
                frameCornerRadius = cornerRadius,
                frameColor = color
            )
        }
    }

    fun setDrawingMode(enabled: Boolean) {
        _state.update { it.copy(isDrawingMode = enabled) }
    }

    fun setBrushStyle(color: Color, size: Float, neon: Boolean) {
        _state.update {
            it.copy(
                activeBrushColor = color,
                activeBrushSize = size.coerceIn(3f, 42f),
                activeBrushNeon = neon
            )
        }
    }

    fun addBrushStroke(points: List<Offset>) {
        if (points.size < 2) return
        if (_state.value.trackLock[4] == true) return
        pushUndo()
        val stroke = BrushStroke(
            points = points,
            color = _state.value.activeBrushColor,
            strokeWidth = _state.value.activeBrushSize,
            isNeon = _state.value.activeBrushNeon
        )
        _state.update { it.copy(brushStrokes = it.brushStrokes + stroke) }
    }

    fun clearBrushStrokes() {
        if (_state.value.brushStrokes.isEmpty()) return
        pushUndo()
        _state.update { it.copy(brushStrokes = emptyList()) }
    }

    fun toggleTrackVisibility(trackId: Int) {
        _state.update { s ->
            val vis = s.trackVisibility.toMutableMap()
            vis[trackId] = !(vis[trackId] ?: true)
            s.copy(trackVisibility = vis)
        }
    }

    fun toggleTrackLock(trackId: Int) {
        _state.update { s ->
            val locks = s.trackLock.toMutableMap()
            locks[trackId] = !(locks[trackId] ?: false)
            s.copy(trackLock = locks)
        }
    }

    fun toggleGridOverlay() = _state.update { it.copy(showGridOverlay = !it.showGridOverlay) }

    fun setComparingOriginal(comparing: Boolean) = _state.update { it.copy(isComparingOriginal = comparing) }

    fun toggleSplitCompareMode() = _state.update { it.copy(isSplitCompareMode = !it.isSplitCompareMode) }

    fun setSplitCompareFraction(fraction: Float) = _state.update {
        it.copy(splitCompareFraction = fraction.coerceIn(0.05f, 0.95f))
    }

    fun updateExportSettings(resolution: Int, format: String, quality: Int) {
        _state.update {
            it.copy(exportSettings = PhotoExportSettings(resolution = resolution, format = format, quality = quality))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SCREEN ROOT
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromaPhotoEditorScreen(
    photoUri: Uri,
    viewModel: ChromaPhotoEditorViewModel = viewModel(),
    exportButtonLabel: String = "Export",
    onExportSuccess: ((Uri) -> Unit)? = null,
    onExport: (PhotoProjectState) -> Unit = {},
    onExportUri: ((Uri) -> Unit)? = null,
    onBack: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }

    var showAddSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showLayersSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var activeToolSheet by remember { mutableStateOf<PhotoEditorTool?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    val selectedClip = state.clips.find { it.id == state.selectedClipId } ?: state.clips.firstOrNull()

    // Open text tool automatically when a newly created text clip is selected
    LaunchedEffect(state.selectedClipId) {
        val clip = state.clips.find { it.id == state.selectedClipId }
        if (clip?.type == MediaType.TEXT) activeToolSheet = PhotoEditorTool.TEXT
    }

    LaunchedEffect(photoUri) {
        viewModel.initialize(photoUri.toString())
    }

    // Suppress Dynamic Island while inside editor (same as ChromaVideoEditorScreen)
    DisposableEffect(Unit) {
        DynamicIslandManager.isSuppressed = true
        onDispose {
            DynamicIslandManager.isSuppressed = false
        }
    }

    // Sync drawing mode with Markup sheet state
    LaunchedEffect(activeToolSheet) {
        viewModel.setDrawingMode(activeToolSheet == PhotoEditorTool.MARKUP)
    }

    var rememberedClip by remember { mutableStateOf<Clip?>(null) }
    SideEffect {
        if (selectedClip != null) rememberedClip = selectedClip
    }

    if (showExportDialog) {
        PhotoExportSettingsDialog(
            initialSettings = state.exportSettings,
            isExporting = isExporting,
            onDismiss = { if (!isExporting) showExportDialog = false },
            onExport = { settings ->
                viewModel.updateExportSettings(settings.resolution, settings.format, settings.quality)
                isExporting = true
                scope.launch {
                    val savedUri = exportEditedPhotoToGallery(
                        context = context,
                        state = state.copy(exportSettings = settings)
                    )
                    isExporting = false
                    showExportDialog = false
                    if (savedUri != null) {
                        ToastManager.showToast("Saved ${settings.resolution}p ${settings.format} to Gallery")
                        onExport(state.copy(exportSettings = settings))
                        onExportUri?.invoke(savedUri)
                        onExportSuccess?.invoke(savedUri)
                    } else {
                        ToastManager.showToast("Export failed")
                    }
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        IosAlertDialog(
            title = "Reset Adjustments?",
            message = "Restore the selected photo layer's filters, tone curves, and optical adjustments to default?",
            confirmText = "Reset",
            isDestructive = true,
            onConfirm = {
                showResetConfirmDialog = false
                viewModel.resetAllAdjustments()
            },
            dismissText = "Cancel",
            onDismiss = { showResetConfirmDialog = false }
        )
    }

    // Exact same light-theme overrides as ChromaVideoEditorScreen
    val editorColors = colors.copy(
        bg = Color(0xFFF4F5F8),
        glassBg = Color(0xFFFFFFFF),
        textMain = Color(0xFF1C1C1E),
        textMuted = Color(0xFF8E8E93),
        primary = Color(0xFF3B82F6),
        glassBorder = Color(0xFFE5E7EB)
    )

    CompositionLocalProvider(
        LocalHazeState provides hazeState,
        LocalOscellionColors provides editorColors
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(editorColors.bg)
        ) {
            // ── Top Bar (Exact ChromaScreenHeader as ChromaVideoEditorScreen) ──
            ChromaScreenHeader(
                title = "Oscellion",
                badgeText = "Photo Studio",
                badgeColor = Color(0xFF007AFF),
                showBackButton = true,
                onBack = onBack,
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Undo / Redo Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(colors.glassBgHover)
                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(50))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp, 28.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(enabled = viewModel.canUndo(), onClick = { viewModel.undo() }),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Undo2,
                                    contentDescription = "Undo",
                                    tint = if (viewModel.canUndo()) colors.textMain else colors.textMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(14.dp)
                                    .background(colors.glassBorder)
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp, 28.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(enabled = viewModel.canRedo(), onClick = { viewModel.redo() }),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Redo2,
                                    contentDescription = "Redo",
                                    tint = if (viewModel.canRedo()) colors.textMain else colors.textMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Layer Button
                        Icon(
                            imageVector = Lucide.Layers,
                            contentDescription = "Layers",
                            tint = colors.textMain,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showLayersSheet = true }
                                .padding(2.dp)
                        )

                        // Export Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF007AFF), Color(0xFF5856D6))
                                    )
                                )
                                .clickable { showExportDialog = true }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Share,
                                    contentDescription = exportButtonLabel,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Text(
                                    text = exportButtonLabel,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            )

            // ── Photo Preview Area — 16:9 Aspect Ratio Box with Overlays (Same as VideoPreviewArea) ──
            PhotoPreviewArea(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                state = state,
                selectedClip = selectedClip,
                onTransformChange = { clipId, transform ->
                    viewModel.updateClipTransform(clipId, transform)
                },
                onDragStart = { viewModel.startDrag() },
                onDragEnd = { viewModel.endDrag() },
                onAddBrushStroke = { viewModel.addBrushStroke(it) },
                onAutoEnhance = { viewModel.autoEnhance() },
                onToggleSplitCompare = { viewModel.toggleSplitCompareMode() },
                onSplitFractionChange = { viewModel.setSplitCompareFraction(it) },
                onToggleGrid = { viewModel.toggleGridOverlay() },
                onToggleHistogram = { viewModel.toggleHistogramHud() },
                onToggleOriginal = { viewModel.setComparingOriginal(!state.isComparingOriginal) }
            )

            // ── Quick Actions Panel (Exact same 3-button glass panel as ChromaVideoEditorScreen) ──
            PhotoQuickActionsPanel(
                onAddMedia = { showAddSheet = true },
                onLayers = { showLayersSheet = true },
                onSettings = { showSettingsSheet = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Layer Track Stack + Bottom Horizontal Toolbar (Same layout as ChromaVideoEditorScreen) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ChromaPhotoTrackWorkspace(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RectangleShape),
                    state = state,
                    onSelectClip = { viewModel.selectClip(it) },
                    onDeselectClip = { viewModel.selectClip(null) },
                    onZoom = { viewModel.setZoom(it) },
                    onToggleVisibility = { viewModel.toggleTrackVisibility(it) },
                    onToggleLock = { viewModel.toggleTrackLock(it) },
                    onOpenTool = { activeToolSheet = it }
                )

                PhotoHorizontalToolbar(
                    clip = rememberedClip,
                    onOpenTool = { activeToolSheet = it },
                    onRotate = { viewModel.rotate90Degrees() },
                    onDelete = { viewModel.deleteSelected() },
                    onDuplicate = { viewModel.duplicateSelected() }
                )
            }
        }
    }

    // ── Bottom Sheets (All using com.chroma.studio.ui.components.GlassBottomSheet) ──
    if (showAddSheet) {
        AddPhotoMediaSheet(
            onDismiss = { showAddSheet = false },
            onAddMedia = { type, name, track, color, uri ->
                viewModel.addClip(type, name, track, color, uri)
                showAddSheet = false
            },
            onOpenMarkup = {
                showAddSheet = false
                activeToolSheet = PhotoEditorTool.MARKUP
            }
        )
    }

    if (showSettingsSheet) {
        GlassBottomSheet(
            onDismissRequest = { showSettingsSheet = false }
        ) {
            PhotoProjectSettingsPanel(
                state = state,
                onFitModeChange = { viewModel.setVideoFitMode(it) },
                onRatioChange = { viewModel.setProjectRatio(it) },
                onResetAll = {
                    showSettingsSheet = false
                    showResetConfirmDialog = true
                }
            )
        }
    }

    if (showLayersSheet) {
        GlassBottomSheet(
            onDismissRequest = { showLayersSheet = false }
        ) {
            PhotoLayerManagerPanel(
                clips = state.clips,
                trackVisibility = state.trackVisibility,
                trackLock = state.trackLock,
                onToggleVisibility = { viewModel.toggleTrackVisibility(it) },
                onToggleLock = { viewModel.toggleTrackLock(it) }
            )
        }
    }

    if (activeToolSheet != null && selectedClip != null) {
        GlassBottomSheet(
            onDismissRequest = { activeToolSheet = null }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                when (activeToolSheet) {
                    PhotoEditorTool.CROP -> PhotoCropPanel(
                        state = state,
                        clip = selectedClip,
                        viewModel = viewModel
                    )
                    PhotoEditorTool.FILTER -> PhotoFilterPanel(selectedClip, state, viewModel)
                    PhotoEditorTool.ADJUST -> PhotoAdjustmentsPanel(selectedClip, state, viewModel)
                    PhotoEditorTool.CURVES -> PhotoCurvesPanel(state, viewModel)
                    PhotoEditorTool.COLOR_MIX -> PhotoColorMixPanel(selectedClip, state, viewModel)
                    PhotoEditorTool.EFFECTS -> PhotoEffectsPanel(state, viewModel)
                    PhotoEditorTool.TRANSFORM -> PhotoTransformPanel(selectedClip, state, viewModel)
                    PhotoEditorTool.TEXT -> PhotoTextEditorPanel(selectedClip, viewModel)
                    PhotoEditorTool.BLEND -> PhotoBlendPanel(selectedClip, viewModel)
                    PhotoEditorTool.CHROMA_KEY -> PhotoChromaKeyPanel(selectedClip, viewModel)
                    PhotoEditorTool.MARKUP -> PhotoMarkupPanel(state, viewModel)
                    PhotoEditorTool.FRAME -> PhotoFramePanel(state, viewModel)
                    null -> {}
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PHOTO PREVIEW AREA (Matches VideoPreviewArea with Unified Gestures & Zoom Slider)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PhotoPreviewArea(
    modifier: Modifier,
    state: PhotoProjectState,
    selectedClip: Clip?,
    onTransformChange: (String, TransformState) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onAddBrushStroke: (List<Offset>) -> Unit,
    onAutoEnhance: () -> Unit,
    onToggleSplitCompare: () -> Unit,
    onSplitFractionChange: (Float) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleHistogram: () -> Unit,
    onToggleOriginal: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val mainClip = state.clips.find { it.trackId == 1 }
    val activeTargetClip = selectedClip ?: mainClip ?: state.clips.firstOrNull()
    val activeTransform = activeTargetClip?.transform ?: TransformState()
    val activeScale = activeTransform.scale
    val activePanX = activeTransform.x
    val activePanY = activeTransform.y

    var showSnapGuideX by remember { mutableStateOf(false) }
    var showSnapGuideY by remember { mutableStateOf(false) }

    var showZoomCropSlider by remember { mutableStateOf(false) }
    var zoomSliderHideJob by remember { mutableStateOf<Job?>(null) }
    val keepZoomSliderAlive: () -> Unit = {
        showZoomCropSlider = true
        zoomSliderHideJob?.cancel()
        zoomSliderHideJob = scope.launch {
            delay(2000L)
            showZoomCropSlider = false
        }
    }

    var currentStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }

    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val stageWidthPx = with(density) { maxWidth.toPx() }

        // Frame Matte Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(state.frameCornerRadius.dp))
                .background(if (state.frameStyle == PhotoFrameStyle.NONE) Color.Black else state.frameColor)
                .padding(
                    top = state.framePadding.dp,
                    start = state.framePadding.dp,
                    end = state.framePadding.dp,
                    bottom = if (state.frameStyle == PhotoFrameStyle.POLAROID) (state.framePadding * 2.2f).dp else state.framePadding.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            // Render visible image/sticker/text layers sorted by trackId
            val visibleClips = remember(state.clips, state.trackVisibility) {
                state.clips.filter { state.trackVisibility[it.trackId] != false }.sortedBy { it.trackId }
            }

            visibleClips.forEach { clip ->
                val ct = clip.transform
                val flipH = if (state.flipHorizontalMap[clip.id] == true) -1f else 1f
                val flipV = if (state.flipVerticalMap[clip.id] == true) -1f else 1f
                val isComparing = state.isComparingOriginal
                val clipMatrix = remember(
                    clip.filter,
                    state.filterIntensity,
                    clip.adjustments,
                    state.warmth,
                    state.tint,
                    state.sharpness,
                    state.dehaze,
                    state.toneCurves,
                    state.redCurve,
                    state.greenCurve,
                    state.blueCurve,
                    state.splitToning,
                    isComparing
                ) {
                    if (isComparing) {
                        ColorMatrix()
                    } else {
                        buildPhotoPreviewColorMatrix(
                            filter = clip.filter,
                            filterIntensity = state.filterIntensity,
                            adj = clip.adjustments,
                            warmth = if (clip.trackId == 1) state.warmth else 0f,
                            tint = if (clip.trackId == 1) state.tint else 0f,
                            sharpness = if (clip.trackId == 1) state.sharpness else 0f,
                            dehaze = if (clip.trackId == 1) state.dehaze else 0f,
                            toneCurves = if (clip.trackId == 1) state.toneCurves else ToneCurvePoints(),
                            redCurve = if (clip.trackId == 1) state.redCurve else ToneCurvePoints(),
                            greenCurve = if (clip.trackId == 1) state.greenCurve else ToneCurvePoints(),
                            blueCurve = if (clip.trackId == 1) state.blueCurve else ToneCurvePoints(),
                            splitToning = if (clip.trackId == 1) state.splitToning else SplitToningState()
                        ).toComposeColorMatrix()
                    }
                }

                when (clip.type) {
                    MediaType.IMAGE, MediaType.VIDEO, MediaType.STICKER -> {
                        val contentScale = when {
                            clip.trackId != 1 -> ContentScale.Fit
                            state.videoFitMode == VideoFitMode.FILL || state.videoFitMode == VideoFitMode.CROP -> ContentScale.Crop
                            else -> ContentScale.Fit
                        }

                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            // Chromatic Aberration (RGB Split) ghost channels for main photo
                            if (!isComparing && clip.trackId == 1 && state.chromaticAberration > 0.02f) {
                                val shiftDp = (state.chromaticAberration * 7f).dp
                                val redMatrix = remember {
                                    ColorMatrix(
                                        floatArrayOf(
                                            1f, 0f, 0f, 0f, 0f,
                                            0f, 0f, 0f, 0f, 0f,
                                            0f, 0f, 0f, 0f, 0f,
                                            0f, 0f, 0f, 0.42f, 0f
                                        )
                                    )
                                }
                                val cyanMatrix = remember {
                                    ColorMatrix(
                                        floatArrayOf(
                                            0f, 0f, 0f, 0f, 0f,
                                            0f, 1f, 0f, 0f, 0f,
                                            0f, 0f, 1f, 0f, 0f,
                                            0f, 0f, 0f, 0.42f, 0f
                                        )
                                    )
                                }
                                AsyncImage(
                                    model = clip.uri,
                                    contentDescription = null,
                                    contentScale = contentScale,
                                    colorFilter = ColorFilter.colorMatrix(redMatrix),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .offset(x = ct.x.dp - shiftDp, y = ct.y.dp)
                                        .graphicsLayer(
                                            scaleX = ct.scale * flipH,
                                            scaleY = ct.scale * flipV,
                                            rotationZ = ct.rotation
                                        )
                                )
                                AsyncImage(
                                    model = clip.uri,
                                    contentDescription = null,
                                    contentScale = contentScale,
                                    colorFilter = ColorFilter.colorMatrix(cyanMatrix),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .offset(x = ct.x.dp + shiftDp, y = ct.y.dp)
                                        .graphicsLayer(
                                            scaleX = ct.scale * flipH,
                                            scaleY = ct.scale * flipV,
                                            rotationZ = ct.rotation
                                        )
                                )
                            }

                            AsyncImage(
                                model = clip.uri,
                                contentDescription = clip.name,
                                contentScale = contentScale,
                                colorFilter = ColorFilter.colorMatrix(clipMatrix),
                                modifier = Modifier
                                    .then(
                                        if (clip.trackId == 1) Modifier.fillMaxSize()
                                        else Modifier.widthIn(max = 220.dp)
                                    )
                                    .offset(x = ct.x.dp, y = ct.y.dp)
                                    .graphicsLayer(
                                        scaleX = ct.scale * flipH,
                                        scaleY = ct.scale * flipV,
                                        rotationZ = ct.rotation,
                                        alpha = ct.opacity * (if (!isComparing && clip.trackId == 1 && state.chromaticAberration > 0.02f) 0.86f else 1f)
                                    )
                            )

                            // Split-screen interactive wipe for main photo (left side = Original)
                            if (state.isSplitCompareMode && clip.trackId == 1) {
                                AsyncImage(
                                    model = clip.uri,
                                    contentDescription = "Original Split",
                                    contentScale = contentScale,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .offset(x = ct.x.dp, y = ct.y.dp)
                                        .graphicsLayer(
                                            scaleX = ct.scale * flipH,
                                            scaleY = ct.scale * flipV,
                                            rotationZ = ct.rotation
                                        )
                                        .drawWithContent {
                                            clipRect(
                                                left = 0f,
                                                top = 0f,
                                                right = size.width * state.splitCompareFraction,
                                                bottom = size.height
                                            ) {
                                                this@drawWithContent.drawContent()
                                            }
                                        }
                                )
                            }
                        }
                    }

                    MediaType.TEXT -> {
                        Box(
                            modifier = Modifier
                                .offset(x = ct.x.dp, y = ct.y.dp)
                                .graphicsLayer(
                                    scaleX = ct.scale * flipH,
                                    scaleY = ct.scale * flipV,
                                    rotationZ = ct.rotation,
                                    alpha = ct.opacity
                                )
                        ) {
                            Text(
                                text = clip.text,
                                color = clip.textColor,
                                fontSize = clip.textSize.sp,
                                fontWeight = if (clip.textBold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (clip.textItalic) FontStyle.Italic else FontStyle.Normal
                            )
                        }
                    }

                    MediaType.AUDIO -> {}
                }
            }

            // Optical Vignette, Film Grain, Bloom, Light Leaks & Frame Overlays
            if (!state.isComparingOriginal && (
                        state.vignette > 0.01f ||
                        state.grain > 0.01f ||
                        state.bloom > 0.01f ||
                        state.lightLeak != LightLeakType.NONE ||
                        state.frameStyle == PhotoFrameStyle.CINEMA_LETTERBOX ||
                        state.frameStyle == PhotoFrameStyle.NEON_GLOW ||
                        state.frameStyle == PhotoFrameStyle.FILM_BORDER
                        )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Halation / Bloom Soft Glow
                    if (state.bloom > 0.01f) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = (state.bloom * 0.25f).coerceIn(0f, 0.35f)),
                                    Color(0xFFFFE5B4).copy(alpha = (state.bloom * 0.12f).coerceIn(0f, 0.2f)),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = max(size.width, size.height) * 0.65f
                            ),
                            size = size
                        )
                    }
                    // Studio Light Leak Overlay
                    if (state.lightLeak != LightLeakType.NONE && state.lightLeakIntensity > 0.01f) {
                        val leakAlpha = state.lightLeakIntensity.coerceIn(0f, 1f)
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = state.lightLeak.colors.map { it.copy(alpha = it.alpha * leakAlpha) },
                                start = Offset.Zero,
                                end = Offset(size.width * 0.85f, size.height * 0.9f)
                            ),
                            size = size
                        )
                    }
                    if (state.vignette > 0.01f) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = (state.vignette * 0.8f).coerceIn(0f, 0.9f))
                                ),
                                center = center,
                                radius = max(size.width, size.height) * 0.72f
                            ),
                            size = size
                        )
                    }
                    if (state.grain > 0.01f) {
                        val step = 6.dp.toPx().toInt().coerceAtLeast(4)
                        val alpha = (state.grain * 0.16f).coerceIn(0f, 0.24f)
                        var seed = 42
                        var x = 0
                        while (x < size.width.toInt()) {
                            var y = 0
                            while (y < size.height.toInt()) {
                                seed = seed * 1103515245 + 12345
                                val bright = (seed ushr 16) and 0xFF
                                drawRect(
                                    color = if (bright > 128) Color.White.copy(alpha = alpha) else Color.Black.copy(alpha = alpha),
                                    topLeft = Offset(x.toFloat(), y.toFloat()),
                                    size = Size(2f, 2f)
                                )
                                y += step
                            }
                            x += step
                        }
                    }
                    if (state.frameStyle == PhotoFrameStyle.CINEMA_LETTERBOX) {
                        val barH = size.height * 0.12f
                        drawRect(Color.Black, topLeft = Offset.Zero, size = Size(size.width, barH))
                        drawRect(Color.Black, topLeft = Offset(0f, size.height - barH), size = Size(size.width, barH))
                    }
                    if (state.frameStyle == PhotoFrameStyle.NEON_GLOW) {
                        drawRect(
                            brush = Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFFAF52DE), Color(0xFFFF2D55))),
                            style = Stroke(width = 5.dp.toPx())
                        )
                    }
                    if (state.frameStyle == PhotoFrameStyle.FILM_BORDER) {
                        val stripH = 14.dp.toPx()
                        drawRect(Color.Black, topLeft = Offset.Zero, size = Size(size.width, stripH))
                        drawRect(Color.Black, topLeft = Offset(0f, size.height - stripH), size = Size(size.width, stripH))
                        val holeW = 8.dp.toPx()
                        val holeH = 6.dp.toPx()
                        val holeStep = 18.dp.toPx()
                        var hx = 10.dp.toPx()
                        while (hx + holeW < size.width) {
                            drawRect(Color.White.copy(alpha = 0.8f), topLeft = Offset(hx, (stripH - holeH) / 2f), size = Size(holeW, holeH))
                            drawRect(Color.White.copy(alpha = 0.8f), topLeft = Offset(hx, size.height - stripH + (stripH - holeH) / 2f), size = Size(holeW, holeH))
                            hx += holeStep
                        }
                    }
                }
            }

            // Freehand Markup Layer (Track 4)
            if (state.trackVisibility[4] != false && (state.brushStrokes.isNotEmpty() || currentStrokePoints.isNotEmpty())) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val allStrokes = if (currentStrokePoints.size >= 2) {
                        state.brushStrokes + BrushStroke(
                            points = currentStrokePoints,
                            color = state.activeBrushColor,
                            strokeWidth = state.activeBrushSize,
                            isNeon = state.activeBrushNeon
                        )
                    } else {
                        state.brushStrokes
                    }

                    allStrokes.forEach { stroke ->
                        val path = Path().apply {
                            moveTo(stroke.points.first().x, stroke.points.first().y)
                            for (i in 1 until stroke.points.size) {
                                lineTo(stroke.points[i].x, stroke.points[i].y)
                            }
                        }
                        if (stroke.isNeon) {
                            drawPath(
                                path = path,
                                color = stroke.color.copy(alpha = 0.35f),
                                style = Stroke(width = stroke.strokeWidth * 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                        drawPath(
                            path = path,
                            color = stroke.color,
                            style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }

            // Rule of Thirds Grid, Magnetic Center Snap Guides & Split-Screen Wipe Line
            if (state.showGridOverlay || state.isSplitCompareMode || showSnapGuideX || showSnapGuideY) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (state.showGridOverlay) {
                        val thirdW = size.width / 3f
                        val thirdH = size.height / 3f
                        val gridColor = Color.White.copy(alpha = 0.32f)
                        drawLine(gridColor, Offset(thirdW, 0f), Offset(thirdW, size.height), strokeWidth = 1f)
                        drawLine(gridColor, Offset(thirdW * 2, 0f), Offset(thirdW * 2, size.height), strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, thirdH), Offset(size.width, thirdH), strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, thirdH * 2), Offset(size.width, thirdH * 2), strokeWidth = 1f)
                    }
                    if (showSnapGuideX) {
                        drawLine(
                            color = Color(0xFF0A84FF),
                            start = Offset(size.width / 2f, 0f),
                            end = Offset(size.width / 2f, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                    if (showSnapGuideY) {
                        drawLine(
                            color = Color(0xFF0A84FF),
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                    if (state.isSplitCompareMode) {
                        val splitX = size.width * state.splitCompareFraction
                        drawLine(Color.White, Offset(splitX, 0f), Offset(splitX, size.height), strokeWidth = 2.dp.toPx())
                        drawCircle(Color(0xFF007AFF), radius = 12.dp.toPx(), center = Offset(splitX, size.height / 2f))
                        drawCircle(Color.White, radius = 12.dp.toPx(), center = Offset(splitX, size.height / 2f), style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }
        }

        // Top HUD (Matches VideoPreviewArea Top HUD + Studio Toggles)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp)
                .zIndex(30f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.showHistogramHud) Color(0xFF007AFF).copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.4f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .clickable { onToggleHistogram() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                val filterLabel = mainClip?.filter?.takeIf { it != FilterType.NONE }?.label
                Text(
                    text = buildString {
                        append("${state.exportSettings.resolution}p • ${state.projectRatio.label}")
                        if (filterLabel != null) append(" • $filterLabel")
                        if (state.isComparingOriginal) append(" • ORIGINAL")
                    },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Magic Auto-Enhance
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .clickable { onAutoEnhance() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Sparkles, contentDescription = "Auto Enhance", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                // Split Compare Wipe
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.isSplitCompareMode) Color(0xFF007AFF) else Color.Black.copy(alpha = 0.4f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .clickable { onToggleSplitCompare() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Columns2, contentDescription = "Split Compare", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                // Rule of Thirds Grid
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.showGridOverlay) Color(0xFF007AFF) else Color.Black.copy(alpha = 0.4f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .clickable { onToggleGrid() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Grid3x3, contentDescription = "Grid", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                // Compare Original
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.isComparingOriginal) Color(0xFF007AFF) else Color.Black.copy(alpha = 0.4f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .clickable { onToggleOriginal() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Eye, contentDescription = "Original", tint = Color.White, modifier = Modifier.size(14.dp))
                }

                // Zoom Slider Toggle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .clickable {
                            if (showZoomCropSlider) showZoomCropSlider = false
                            else keepZoomSliderAlive()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Maximize, contentDescription = "Zoom & Crop", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Live Glassmorphic RGB Parade / Luminance Histogram HUD (Toggled by tapping top-left badge)
        AnimatedVisibility(
            visible = state.showHistogramHud,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.9f),
            exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .zIndex(32f)
        ) {
            LiveRgbHistogramCard(
                clip = mainClip,
                state = state
            )
        }

        // ══════════════════════════════════════════════════════════
        // UNIFIED TOUCH GESTURE OVERLAY (Identical to ChromaVideoEditorScreen)
        // ══════════════════════════════════════════════════════════
        if (state.clips.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .pointerInput(state.isDrawingMode, state.isSplitCompareMode, activeTargetClip?.id) {
                        if (state.isDrawingMode) {
                            detectDragGestures(
                                onDragStart = { start -> currentStrokePoints = listOf(start) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentStrokePoints = currentStrokePoints + change.position
                                },
                                onDragEnd = {
                                    onAddBrushStroke(currentStrokePoints)
                                    currentStrokePoints = emptyList()
                                },
                                onDragCancel = { currentStrokePoints = emptyList() }
                            )
                        } else {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var pan = Offset.Zero
                                var pastTouchSlop = false
                                val touchSlop = viewConfiguration.touchSlop
                                var isMultiTouch = false
                                var gestureConsumed = false
                                var event: PointerEvent

                                val isDraggingSplitHandle = state.isSplitCompareMode &&
                                        abs(down.position.x - stageWidthPx * state.splitCompareFraction) < 36.dp.toPx()

                                onDragStart()

                                do {
                                    event = awaitPointerEvent()
                                    val canceled = event.changes.any { it.isConsumed }
                                    if (canceled) break

                                    val pointerCount = event.changes.count { it.pressed }
                                    if (pointerCount >= 2) {
                                        isMultiTouch = true
                                        pastTouchSlop = true
                                        keepZoomSliderAlive()
                                    }

                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()

                                    if (isDraggingSplitHandle && pointerCount == 1) {
                                        val posX = event.changes.firstOrNull()?.position?.x ?: (stageWidthPx * state.splitCompareFraction)
                                        onSplitFractionChange(posX / stageWidthPx.coerceAtLeast(1f))
                                        gestureConsumed = true
                                        event.changes.forEach { it.consume() }
                                    } else if (pointerCount >= 2) {
                                        keepZoomSliderAlive()
                                        val target = activeTargetClip
                                        if (target != null) {
                                            val curT = target.transform
                                            val newScale = (curT.scale * zoomChange).coerceIn(0.5f, 5.0f)
                                            val dxDp = panChange.x / density.density
                                            val dyDp = panChange.y / density.density
                                            val maxPanX = max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                            val maxPanY = max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                            val newPanX = if (newScale <= 1.02f && target.trackId == 1) 0f else (curT.x + dxDp).coerceIn(-maxPanX, maxPanX)
                                            val newPanY = if (newScale <= 1.02f && target.trackId == 1) 0f else (curT.y + dyDp).coerceIn(-maxPanY, maxPanY)

                                            onTransformChange(
                                                target.id,
                                                curT.copy(scale = newScale, x = newPanX, y = newPanY)
                                            )
                                            gestureConsumed = true
                                        }
                                        event.changes.forEach {
                                            if (it.positionChange() != Offset.Zero) it.consume()
                                        }
                                    } else if (pointerCount == 1) {
                                        val target = activeTargetClip
                                        val canDrag = (target?.transform?.scale ?: 1f) > 1.02f || (target?.trackId ?: 1) != 1

                                        pan += panChange
                                        if (!pastTouchSlop && pan.getDistance() > touchSlop) {
                                            pastTouchSlop = true
                                        }

                                        if (pastTouchSlop && canDrag && target != null) {
                                            if (target.trackId == 1) keepZoomSliderAlive()
                                            val curT = target.transform
                                            val dxDp = panChange.x / density.density
                                            val dyDp = panChange.y / density.density
                                            val maxPanX = max(100f, (curT.scale - 0.7f).coerceAtLeast(0f) * 200f)
                                            val maxPanY = max(100f, (curT.scale - 0.7f).coerceAtLeast(0f) * 200f)
                                            var newPanX = (curT.x + dxDp).coerceIn(-maxPanX, maxPanX)
                                            var newPanY = (curT.y + dyDp).coerceIn(-maxPanY, maxPanY)

                                            // Magnetic center snap with haptic feedback
                                            val snapNearX = abs(newPanX) < 4.5f
                                            val snapNearY = abs(newPanY) < 4.5f
                                            if (snapNearX && !showSnapGuideX) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            if (snapNearY && !showSnapGuideY) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            showSnapGuideX = snapNearX
                                            showSnapGuideY = snapNearY
                                            if (snapNearX) newPanX = 0f
                                            if (snapNearY) newPanY = 0f

                                            onTransformChange(
                                                target.id,
                                                curT.copy(x = newPanX, y = newPanY)
                                            )
                                            gestureConsumed = true
                                            event.changes.forEach {
                                                if (it.positionChange() != Offset.Zero) it.consume()
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                showSnapGuideX = false
                                showSnapGuideY = false

                                if (isMultiTouch) {
                                    keepZoomSliderAlive()
                                }

                                // Double-tap: toggle 1.0x Fit vs 1.35x Fill/Crop (Same as ChromaVideoEditorScreen)
                                if (!pastTouchSlop && !isMultiTouch && !gestureConsumed) {
                                    val now = System.currentTimeMillis()
                                    if (now - lastTapTimestamp < 300L) {
                                        lastTapTimestamp = 0L
                                        val target = activeTargetClip
                                        if (target != null) {
                                            val curT = target.transform
                                            val nextScale = if (curT.scale > 1.15f) 1.0f else 1.35f
                                            val nextPanX = if (nextScale == 1.0f) 0f else curT.x
                                            val nextPanY = if (nextScale == 1.0f) 0f else curT.y
                                            onTransformChange(
                                                target.id,
                                                curT.copy(scale = nextScale, x = nextPanX, y = nextPanY)
                                            )
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            ToastManager.showToast(if (nextScale == 1.0f) "Zoom: 1.0x (Fit)" else "Zoom: 1.35x (Fill)")
                                        }
                                    } else {
                                        lastTapTimestamp = now
                                    }
                                }

                                onDragEnd()
                            }
                        }
                    }
            )
        }

        // Floating Zoom & Crop Smooth Slider Bar (Exact same UI as ChromaVideoEditorScreen)
        AnimatedVisibility(
            visible = showZoomCropSlider && state.clips.isNotEmpty(),
            enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.92f),
            exit = fadeOut(tween(450)) + scaleOut(tween(450), targetScale = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .zIndex(45f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 380.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xE614161E))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                    .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { keepZoomSliderAlive() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Maximize2,
                                contentDescription = null,
                                tint = Color(0xFF0A84FF),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "ZOOM & CROP",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0A84FF).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${String.format("%.2f", activeScale)}x",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (activeScale != 1f || activePanX != 0f || activePanY != 0f) {
                                Text(
                                    text = "Reset",
                                    color = Color(0xFFFF453A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            keepZoomSliderAlive()
                                            val target = activeTargetClip ?: return@clickable
                                            onTransformChange(
                                                target.id,
                                                target.transform.copy(scale = 1f, x = 0f, y = 0f)
                                            )
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            ToastManager.showToast("Zoom reset to 1.0x")
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    keepZoomSliderAlive()
                                    val target = activeTargetClip ?: return@clickable
                                    val curT = target.transform
                                    val newScale = (curT.scale - 0.2f).coerceAtLeast(0.5f)
                                    val maxPanX = max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                    val maxPanY = max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                    val newPanX = if (newScale <= 1.02f) 0f else curT.x.coerceIn(-maxPanX, maxPanX)
                                    val newPanY = if (newScale <= 1.02f) 0f else curT.y.coerceIn(-maxPanY, maxPanY)
                                    onTransformChange(
                                        target.id,
                                        curT.copy(scale = newScale, x = newPanX, y = newPanY)
                                    )
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.ZoomOut,
                                contentDescription = "Zoom Out",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Slider(
                            value = activeScale.coerceIn(0.5f, 4.0f),
                            onValueChange = { newScale ->
                                keepZoomSliderAlive()
                                val target = activeTargetClip ?: return@Slider
                                val curT = target.transform
                                val maxPanX = max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                val maxPanY = max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                val newPanX = if (newScale <= 1.02f) 0f else curT.x.coerceIn(-maxPanX, maxPanX)
                                val newPanY = if (newScale <= 1.02f) 0f else curT.y.coerceIn(-maxPanY, maxPanY)
                                onTransformChange(
                                    target.id,
                                    curT.copy(scale = newScale, x = newPanX, y = newPanY)
                                )
                            },
                            onValueChangeFinished = { keepZoomSliderAlive() },
                            valueRange = 0.5f..4.0f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF0A84FF),
                                activeTrackColor = Color(0xFF0A84FF),
                                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                            )
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    keepZoomSliderAlive()
                                    val target = activeTargetClip ?: return@clickable
                                    val curT = target.transform
                                    val newScale = (curT.scale + 0.2f).coerceAtMost(4.0f)
                                    val maxPanX = max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                    val maxPanY = max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                    val newPanX = if (newScale <= 1.02f) 0f else curT.x.coerceIn(-maxPanX, maxPanX)
                                    val newPanY = if (newScale <= 1.02f) 0f else curT.y.coerceIn(-maxPanY, maxPanY)
                                    onTransformChange(
                                        target.id,
                                        curT.copy(scale = newScale, x = newPanX, y = newPanY)
                                    )
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.ZoomIn,
                                contentDescription = "Zoom In",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveRgbHistogramCard(
    clip: Clip?,
    state: PhotoProjectState
) {
    val adj = clip?.adjustments ?: Adjustments()
    Box(
        modifier = Modifier
            .size(width = 138.dp, height = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xD914161E))
            .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RGB HISTOGRAM", color = Color.White.copy(alpha = 0.85f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "EV ${String.format("%+.1f", adj.exposure)}",
                    color = Color(0xFF60A5FA),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(4.dp))
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val samples = 24
                val shift = ((adj.brightness + adj.exposure * 0.35f + state.toneCurves.midtones * 0.4f) * 6f).coerceIn(-6f, 6f)

                fun buildChannelPath(offsetPhase: Float, warmBias: Float): Path {
                    return Path().apply {
                        moveTo(0f, h)
                        for (i in 0..samples) {
                            val t = i.toFloat() / samples
                            val x = t * w
                            val bell = kotlin.math.exp(-((t - 0.5f - (shift + warmBias) * 0.04f).pow(2)) / (0.08f * adj.contrast.coerceAtLeast(0.4f)))
                            val ripple = 0.18f * sin(t * 9f + offsetPhase)
                            val y = (h - (bell + ripple).coerceIn(0.08f, 0.96f) * h).coerceIn(0f, h)
                            lineTo(x, y)
                        }
                        lineTo(w, h)
                        close()
                    }
                }

                drawPath(buildChannelPath(0.4f, state.warmth * 1.5f), Color(0x66FF3B30))
                drawPath(buildChannelPath(1.8f, -state.tint), Color(0x6634C759))
                drawPath(buildChannelPath(3.1f, -state.warmth * 1.5f), Color(0x66007AFF))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// QUICK ACTIONS PANEL (Exact 3-item glassmorphic panel as ChromaVideoEditorScreen)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PhotoQuickActionsPanel(
    onAddMedia: () -> Unit,
    onLayers: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(72.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x1A000000), ambientColor = Color(0x1A000000))
            .hazeChild(state = LocalHazeState.current, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.6f))
            .border(0.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PhotoQuickActionButton(icon = Lucide.Plus, label = "Add Media", onClick = onAddMedia)
        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color(0xFFF3F4F6)))
        PhotoQuickActionButton(icon = Lucide.Layers, label = "Layers", onClick = onLayers)
        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color(0xFFF3F4F6)))
        PhotoQuickActionButton(icon = Lucide.Settings, label = "Settings", onClick = onSettings)
    }
}

@Composable
private fun RowScope.PhotoQuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = Color(0xFF374151), modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4B5563))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PHOTO LAYER WORKSPACE (Same 5-Track Left Gutter + Track Bars as ChromaTimeline)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChromaPhotoTrackWorkspace(
    modifier: Modifier,
    state: PhotoProjectState,
    onSelectClip: (String) -> Unit,
    onDeselectClip: () -> Unit,
    onZoom: (Float) -> Unit,
    onToggleVisibility: (Int) -> Unit,
    onToggleLock: (Int) -> Unit,
    onOpenTool: (PhotoEditorTool) -> Unit
) {
    val colors = LocalOscellionColors.current
    val pps = state.pixelsPerSecond
    val trackGutterDp = 110.dp
    val trackHeightDp = 44.dp
    val verticalScrollState = rememberScrollState()

    val trackColors = listOf(
        Color(0xFF3B82F6), // 1: Photo (Blue)
        Color(0xFFA855F7), // 2: Overlay (Purple)
        Color(0xFFFCD34D), // 3: Text (Yellow)
        Color(0xFF22C55E), // 4: Markup (Green)
        Color(0xFFEC4899)  // 5: Stickers (Pink)
    )
    val trackLabels = listOf("Photo", "Overlay", "Text", "Markup", "Stickers")
    val trackIcons = listOf(Lucide.Image, Lucide.Layers, Lucide.Type, Lucide.Brush, Lucide.Sticker)

    val hazeState = LocalHazeState.current

    Column(
        modifier = modifier
            .hazeChild(state = hazeState, shape = RectangleShape)
            .background(colors.bg.copy(alpha = 0.5f))
    ) {
        // Zoom / Scale row using AppleSlider (Matches ChromaTimeline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Lucide.ZoomIn, contentDescription = "Zoom", tint = colors.textMuted, modifier = Modifier.size(12.dp))
            AppleSlider(
                value = pps,
                onValueChange = onZoom,
                valueRange = 20f..200f,
                modifier = Modifier.weight(1f)
            )
            Text("${String.format("%.1f", pps / 60f)}×", color = colors.textMuted, fontSize = 9.sp)
        }

        // 5-Track Row Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(verticalScrollState)
        ) {
            // Left Gutter (Exact 110.dp track headers with Eye & Lock icons)
            Column(
                modifier = Modifier
                    .width(trackGutterDp)
                    .background(colors.bg)
                    .padding(top = 16.dp, start = 12.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (0..4).forEach { i ->
                    val trackId = i + 1
                    val isVisible = state.trackVisibility[trackId] ?: true
                    val isLocked = state.trackLock[trackId] ?: false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(trackHeightDp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(0.5.dp, Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(trackColors[i]),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = trackIcons[i],
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = trackLabels[i],
                                color = Color(0xFF1F2937),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isVisible) Lucide.Eye else Lucide.EyeOff,
                                contentDescription = "Toggle Visibility",
                                tint = if (isVisible) Color(0xFF9CA3AF) else Color(0xFFD1D5DB),
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onToggleVisibility(trackId) }
                            )
                            Icon(
                                imageVector = if (isLocked) Lucide.Lock else Lucide.LockOpen,
                                contentDescription = "Toggle Lock",
                                tint = if (isLocked) Color(0xFF9CA3AF) else Color(0xFFD1D5DB),
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onToggleLock(trackId) }
                            )
                        }
                    }
                }
            }

            // Right Layer Bars Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDeselectClip
                    )
            ) {
                // Luminance / Tone Ruler Header (16.dp height matching TimeRuler)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(colors.glassBg.copy(alpha = 0.3f))
                ) {
                    val steps = 20
                    val stepW = size.width / steps
                    for (i in 0..steps) {
                        val x = i * stepW
                        drawLine(
                            color = colors.textMuted,
                            start = Offset(x, if (i % 5 == 0) 2f else 8f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                    }
                }

                // 5 Track Rows
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { trackId ->
                        PhotoTrackRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(trackHeightDp),
                            trackId = trackId,
                            trackColor = trackColors[trackId - 1],
                            clips = state.clips.filter { it.trackId == trackId },
                            selectedClipId = state.selectedClipId,
                            brushStrokeCount = state.brushStrokes.size,
                            onClipTap = onSelectClip,
                            onOpenTool = onOpenTool
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoTrackRow(
    modifier: Modifier,
    trackId: Int,
    trackColor: Color,
    clips: List<Clip>,
    selectedClipId: String?,
    brushStrokeCount: Int,
    onClipTap: (String) -> Unit,
    onOpenTool: (PhotoEditorTool) -> Unit
) {
    val colors = LocalOscellionColors.current

    Box(
        modifier = modifier
            .background(colors.bg.copy(alpha = 0.2f))
            .border(BorderStroke(0.5.dp, colors.glassBorder.copy(alpha = 0.2f)))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (trackId == 4) {
            // Track 4: Markup Brush Bar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (brushStrokeCount > 0) trackColor else trackColor.copy(alpha = 0.25f))
                    .border(0.5.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
                    .clickable { onOpenTool(PhotoEditorTool.MARKUP) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (brushStrokeCount > 0) "Markup ($brushStrokeCount strokes)" else "Tap to draw markup",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.35f)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Empty track",
                    color = colors.textMuted.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                clips.forEach { clip ->
                    val selected = clip.id == selectedClipId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(trackColor)
                            .then(
                                if (selected) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(5.dp))
                                else Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
                            )
                            .clickable { onClipTap(clip.id) },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Subtle histogram waveform overlay inside the bar (Matches WaveformOverlay)
                        PhotoHistogramBarOverlay(
                            clip = clip,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (clip.type) {
                                    MediaType.TEXT -> clip.text.take(16)
                                    else -> clip.name
                                },
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val badgeText = buildString {
                                if (clip.filter != FilterType.NONE) append(clip.filter.label)
                                if (clip.blendMode != BlendMode.NORMAL) {
                                    if (isNotEmpty()) append(" • ")
                                    append(clip.blendMode.name.lowercase())
                                }
                            }
                            if (badgeText.isNotEmpty()) {
                                Text(
                                    text = badgeText,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoHistogramBarOverlay(clip: Clip, modifier: Modifier) {
    val colors = LocalOscellionColors.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barWidth = 3.dp.toPx()
        val spacing = 1.dp.toPx()
        val totalBars = (w / (barWidth + spacing)).toInt().coerceAtLeast(1)

        var seed = clip.id.hashCode()
        for (i in 0 until totalBars) {
            seed = seed * 31 + 17
            val n = abs(seed % 100) / 100f
            val contrastBoost = clip.adjustments.contrast.coerceIn(0.4f, 1.8f)
            val barHeight = (h * 0.15f + (h * 0.55f * n * contrastBoost)).coerceAtMost(h * 0.9f)
            val x = i * (barWidth + spacing)
            val y = (h - barHeight) / 2f
            drawRect(
                color = colors.onPrimary.copy(alpha = 0.25f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HORIZONTAL TOOLBAR (Exact same bottom bar style as ChromaVideoEditorScreen)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PhotoHorizontalToolbar(
    clip: Clip?,
    onOpenTool: (PhotoEditorTool) -> Unit,
    onRotate: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    data class ToolItem(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val label: String,
        val color: Color,
        val action: () -> Unit,
        val requiresClip: Boolean = true
    )

    val tools = listOf(
        ToolItem(Lucide.Crop, "Crop", Color(0xFF007AFF), { onOpenTool(PhotoEditorTool.CROP) }),
        ToolItem(Lucide.SlidersHorizontal, "Filter", Color(0xFFFF9500), { onOpenTool(PhotoEditorTool.FILTER) }),
        ToolItem(Lucide.Settings2, "Adjust", Color(0xFF34C759), { onOpenTool(PhotoEditorTool.ADJUST) }),
        ToolItem(Lucide.Activity, "Curves", Color(0xFF5856D6), { onOpenTool(PhotoEditorTool.CURVES) }),
        ToolItem(Lucide.Palette, "Color Mix", Color(0xFFAF52DE), { onOpenTool(PhotoEditorTool.COLOR_MIX) }),
        ToolItem(Lucide.Sparkles, "Effects", Color(0xFFFF2D55), { onOpenTool(PhotoEditorTool.EFFECTS) }, requiresClip = false),
        ToolItem(Lucide.Move, "Transform", Color(0xFF8E8E93), { onOpenTool(PhotoEditorTool.TRANSFORM) }),
        ToolItem(Lucide.Type, "Text", Color(0xFFFF3B30), { onOpenTool(PhotoEditorTool.TEXT) }),
        ToolItem(Lucide.Pipette, "Chroma", Color(0xFF34C759), { onOpenTool(PhotoEditorTool.CHROMA_KEY) }),
        ToolItem(Lucide.Blend, "Blend", Color(0xFFAF52DE), { onOpenTool(PhotoEditorTool.BLEND) }),
        ToolItem(Lucide.Brush, "Markup", Color(0xFF30B0C7), { onOpenTool(PhotoEditorTool.MARKUP) }, requiresClip = false),
        ToolItem(Lucide.Frame, "Frame", Color(0xFF5856D6), { onOpenTool(PhotoEditorTool.FRAME) }, requiresClip = false),
        ToolItem(Lucide.RotateCw, "Rotate", Color(0xFF007AFF), { onRotate() }),
        ToolItem(Lucide.Copy, "Duplicate", Color(0xFF5856D6), { onDuplicate() }),
        ToolItem(Lucide.Trash2, "Delete", Color(0xFFFF3B30), { onDelete() })
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tools.forEach { tool ->
            val enabled = if (tool.requiresClip) clip != null else true
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .alpha(if (enabled) 1f else 0.35f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled, onClick = tool.action)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.label,
                    tint = if (enabled) tool.color else Color(0xFF9CA3AF),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = tool.label,
                    color = if (enabled) Color(0xFF374151) else Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TOOL PANELS (Uses PropertySlider, AppleSlider, AppleSwitch, GlassIconButton)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PhotoCropPanel(
    state: PhotoProjectState,
    clip: Clip,
    viewModel: ChromaPhotoEditorViewModel
) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Crop & Ratio", color = colors.textMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassIconButton(
                    onClick = { viewModel.toggleFlipHorizontal(clip.id) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Lucide.FlipHorizontal,
                        contentDescription = "Flip H",
                        tint = colors.textMain,
                        modifier = Modifier.size(18.dp)
                    )
                }
                GlassIconButton(
                    onClick = { viewModel.toggleFlipVertical(clip.id) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Lucide.FlipVertical,
                        contentDescription = "Flip V",
                        tint = colors.textMain,
                        modifier = Modifier.size(18.dp)
                    )
                }
                GlassIconButton(
                    onClick = { viewModel.rotate90Degrees() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Lucide.RotateCw,
                        contentDescription = "Rotate",
                        tint = colors.textMain,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Aspect Ratio Chips (Same ProjectRatio enum as ChromaVideoEditorScreen)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProjectRatio.values().forEach { ratio ->
                val selected = state.projectRatio == ratio
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color(0xFF3B82F6) else Color(0xFFF3F4F6))
                        .border(1.dp, if (selected) Color(0xFF3B82F6) else Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                        .clickable { viewModel.setProjectRatio(ratio) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ratio.label,
                        color = if (selected) Color.White else Color(0xFF374151),
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        val t = clip.transform
        PropertySlider("Straighten", t.rotation, -45f, 45f) {
            viewModel.setTransform(t.copy(rotation = it))
        }
        PropertySlider("Zoom Scale", t.scale, 0.5f, 4f) {
            viewModel.setTransform(t.copy(scale = it))
        }
    }
}

@Composable
private fun PhotoFilterPanel(
    clip: Clip,
    state: PhotoProjectState,
    viewModel: ChromaPhotoEditorViewModel
) {
    val colors = LocalOscellionColors.current
    val filters = FilterType.values().toList()

    val filterGradients: Map<FilterType, List<Color>> = mapOf(
        FilterType.NONE to listOf(Color(0xFF252525), Color(0xFF3D3D3D)),
        FilterType.GRAYSCALE to listOf(Color(0xFF606060), Color(0xFF909090)),
        FilterType.SEPIA to listOf(Color(0xFF704214), Color(0xFFB8884A)),
        FilterType.INVERT to listOf(Color(0xFF001AFF), Color(0xFFFF1493)),
        FilterType.WARM to listOf(Color(0xFFFF6B35), Color(0xFFFFD700)),
        FilterType.COOL to listOf(Color(0xFF0066CC), Color(0xFF00CCFF)),
        FilterType.CINEMATIC to listOf(Color(0xFF0D1B2A), Color(0xFF1B4332)),
        FilterType.FADE to listOf(Color(0xFFB0998A), Color(0xFFD4BFB0)),
        FilterType.VIVID to listOf(Color(0xFFFF0080), Color(0xFF00FFCC)),
        FilterType.MATTE to listOf(Color(0xFF7A6855), Color(0xFF9B8B72)),
        FilterType.NEON to listOf(Color(0xFF00FF41), Color(0xFF0066FF)),
        FilterType.SUNSET to listOf(Color(0xFFFF4E00), Color(0xFF990000)),
        FilterType.NOIR to listOf(colors.glassBg, colors.glassBorder)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            "  Filter",
            color = colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filters.forEach { filter ->
                val selected = clip.filter == filter
                val gradColors = filterGradients[filter] ?: listOf(Color.Gray, Color.DarkGray)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { viewModel.setFilter(filter) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 58.dp, height = 76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(gradColors))
                            .then(
                                if (selected) Modifier.border(2.dp, colors.primary, RoundedCornerShape(10.dp))
                                else Modifier.border(0.5.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                            )
                    ) {
                        if (selected) {
                            Icon(
                                Lucide.Check,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = filter.label,
                        color = if (selected) colors.primary else colors.textMuted,
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        if (clip.filter != FilterType.NONE) {
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                PropertySlider(
                    label = "Intensity",
                    value = state.filterIntensity,
                    min = 0f,
                    max = 1f,
                    onValueChange = { viewModel.setFilterIntensity(it) }
                )
            }
        }
    }
}

@Composable
private fun PhotoAdjustmentsPanel(
    clip: Clip,
    state: PhotoProjectState,
    viewModel: ChromaPhotoEditorViewModel
) {
    val adj = clip.adjustments

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        PropertySlider("Brightness", adj.brightness, -1f, 1f) { viewModel.setAdjustments(adj.copy(brightness = it)) }
        PropertySlider("Contrast", adj.contrast, 0f, 2f) { viewModel.setAdjustments(adj.copy(contrast = it)) }
        PropertySlider("Saturation", adj.saturation, 0f, 3f) { viewModel.setAdjustments(adj.copy(saturation = it)) }
        PropertySlider("Hue", adj.hue, 0f, 360f) { viewModel.setAdjustments(adj.copy(hue = it)) }
        PropertySlider("Exposure", adj.exposure, -2f, 2f) { viewModel.setAdjustments(adj.copy(exposure = it)) }
        PropertySlider("Highlights", adj.highlights, -1f, 1f) { viewModel.setAdjustments(adj.copy(highlights = it)) }
        PropertySlider("Shadows", adj.shadows, -1f, 1f) { viewModel.setAdjustments(adj.copy(shadows = it)) }
        PropertySlider("Warmth", state.warmth, -1f, 1f) { viewModel.setOptics(warmth = it) }
        PropertySlider("Tint", state.tint, -1f, 1f) { viewModel.setOptics(tint = it) }
        PropertySlider("Clarity", state.sharpness, 0f, 1f) { viewModel.setOptics(sharpness = it) }
        PropertySlider("Vignette", state.vignette, 0f, 1f) { viewModel.setOptics(vignette = it) }
        PropertySlider("Film Grain", state.grain, 0f, 1f) { viewModel.setOptics(grain = it) }
    }
}

@Composable
private fun PhotoCurvesPanel(
    state: PhotoProjectState,
    viewModel: ChromaPhotoEditorViewModel
) {
    val colors = LocalOscellionColors.current
    val activePoints = when (state.activeCurveChannel) {
        ToneCurveChannel.MASTER -> state.toneCurves
        ToneCurveChannel.RED -> state.redCurve
        ToneCurveChannel.GREEN -> state.greenCurve
        ToneCurveChannel.BLUE -> state.blueCurve
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Parametric Tone Curves", color = colors.textMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Reset",
                color = Color(0xFFFF3B30),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { viewModel.resetCurves() }
            )
        }

        // RGB Channel Selector Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToneCurveChannel.values().forEach { ch ->
                val sel = state.activeCurveChannel == ch
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) ch.color else Color(0xFFF3F4F6))
                        .clickable { viewModel.setActiveCurveChannel(ch) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ch.label,
                        color = if (sel) Color.White else Color(0xFF374151),
                        fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Interactive 5-Point Spline Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF14161E))
                .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Grid lines
                for (i in 1..3) {
                    val gx = w * (i / 4f)
                    val gy = h * (i / 4f)
                    drawLine(Color.White.copy(alpha = 0.1f), Offset(gx, 0f), Offset(gx, h), 1f)
                    drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, gy), Offset(w, gy), 1f)
                }
                // Baseline diagonal
                drawLine(Color.White.copy(alpha = 0.22f), Offset(0f, h), Offset(w, 0f), 1.5f)

                val pts = listOf(
                    Offset(0f, (h * 1f - activePoints.blacks * h * 0.35f).coerceIn(0f, h)),
                    Offset(w * 0.25f, (h * 0.75f - activePoints.shadows * h * 0.35f).coerceIn(0f, h)),
                    Offset(w * 0.50f, (h * 0.50f - activePoints.midtones * h * 0.35f).coerceIn(0f, h)),
                    Offset(w * 0.75f, (h * 0.25f - activePoints.highlights * h * 0.35f).coerceIn(0f, h)),
                    Offset(w, (0f - activePoints.whites * h * 0.35f).coerceIn(0f, h))
                )

                val curvePath = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (i in 0 until pts.lastIndex) {
                        val p0 = pts[i]
                        val p1 = pts[i + 1]
                        val midX = (p0.x + p1.x) / 2f
                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }
                }

                val strokeColor = if (state.activeCurveChannel == ToneCurveChannel.MASTER) Color.White else state.activeCurveChannel.color
                drawPath(curvePath, color = strokeColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                pts.forEach { pt ->
                    drawCircle(strokeColor, radius = 4.5.dp.toPx(), center = pt)
                    drawCircle(Color(0xFF14161E), radius = 2.dp.toPx(), center = pt)
                }
            }
        }

        // Studio Curve Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                "Linear" to ToneCurvePoints(),
                "S-Contrast" to ToneCurvePoints(blacks = -0.1f, shadows = -0.25f, midtones = 0f, highlights = 0.25f, whites = 0.1f),
                "Matte Film" to ToneCurvePoints(blacks = 0.32f, shadows = 0.12f, midtones = 0f, highlights = -0.1f, whites = -0.18f),
                "Lifted Fade" to ToneCurvePoints(blacks = 0.24f, shadows = 0.18f, midtones = 0.08f, highlights = 0f, whites = -0.08f),
                "Crushed Noir" to ToneCurvePoints(blacks = -0.35f, shadows = -0.28f, midtones = -0.05f, highlights = 0.22f, whites = 0.15f)
            )
            presets.forEach { (label, preset) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3F4F6))
                        .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                        .clickable { viewModel.updateActiveCurvePoints(preset) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(label, color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        PropertySlider("Blacks", activePoints.blacks, -1f, 1f) {
            viewModel.updateActiveCurvePoints(activePoints.copy(blacks = it))
        }
        PropertySlider("Shadows", activePoints.shadows, -1f, 1f) {
            viewModel.updateActiveCurvePoints(activePoints.copy(shadows = it))
        }
        PropertySlider("Midtones", activePoints.midtones, -1f, 1f) {
            viewModel.updateActiveCurvePoints(activePoints.copy(midtones = it))
        }
        PropertySlider("Highlights", activePoints.highlights, -1f, 1f) {
            viewModel.updateActiveCurvePoints(activePoints.copy(highlights = it))
        }
        PropertySlider("Whites", activePoints.whites, -1f, 1f) {
            viewModel.updateActiveCurvePoints(activePoints.copy(whites = it))
        }
    }
}

@Composable
private fun PhotoColorMixPanel(
    clip: Clip,
    state: PhotoProjectState,
    viewModel: ChromaPhotoEditorViewModel
) {
    val colors = LocalOscellionColors.current
    val st = state.splitToning
    val adj = clip.adjustments

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Split Toning & Color Mix", color = colors.textMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        Text("Highlight Grade", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        PropertySlider("Hi Hue", st.highlightHue, 0f, 360f) {
            viewModel.updateSplitToning(st.copy(highlightHue = it))
        }
        PropertySlider("Hi Amount", st.highlightStrength, 0f, 1f) {
            viewModel.updateSplitToning(st.copy(highlightStrength = it))
        }

        Text("Shadow Grade", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        PropertySlider("Sh Hue", st.shadowHue, 0f, 360f) {
            viewModel.updateSplitToning(st.copy(shadowHue = it))
        }
        PropertySlider("Sh Amount", st.shadowStrength, 0f, 1f) {
            viewModel.updateSplitToning(st.copy(shadowStrength = it))
        }

        Text("White Balance & Global Hue", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        PropertySlider("Warmth", state.warmth, -1f, 1f) { viewModel.setOptics(warmth = it) }
        PropertySlider("Tint", state.tint, -1f, 1f) { viewModel.setOptics(tint = it) }
        PropertySlider("Hue Shift", adj.hue, 0f, 360f) { viewModel.setAdjustments(adj.copy(hue = it)) }
        PropertySlider("Vibrance", adj.saturation, 0f, 3f) { viewModel.setAdjustments(adj.copy(saturation = it)) }
    }
}

@Composable
private fun PhotoEffectsPanel(
    state: PhotoProjectState,
    viewModel: ChromaPhotoEditorViewModel
) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Optical FX & Light Leaks", color = colors.textMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        // Studio Light Leak Presets
        Text("Light Leak Overlay", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LightLeakType.values().forEach { leak ->
                val selected = state.lightLeak == leak
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color(0xFF007AFF) else Color(0xFFF3F4F6))
                        .border(1.dp, if (selected) Color(0xFF007AFF) else Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                        .clickable { viewModel.setLightLeak(leak) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = leak.label,
                        color = if (selected) Color.White else colors.textMain,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        if (state.lightLeak != LightLeakType.NONE) {
            PropertySlider("Leak Mix", state.lightLeakIntensity, 0f, 1f) {
                viewModel.setLightLeak(state.lightLeak, it)
            }
        }

        PropertySlider("RGB Split", state.chromaticAberration, 0f, 1f) {
            viewModel.setOptics(chromaticAberration = it)
        }
        PropertySlider("Halation", state.bloom, 0f, 1f) {
            viewModel.setOptics(bloom = it)
        }
        PropertySlider("Dehaze", state.dehaze, -1f, 1f) {
            viewModel.setOptics(dehaze = it)
        }
        PropertySlider("Clarity", state.sharpness, 0f, 1f) {
            viewModel.setOptics(sharpness = it)
        }
        PropertySlider("Vignette", state.vignette, 0f, 1f) {
            viewModel.setOptics(vignette = it)
        }
        PropertySlider("Film Grain", state.grain, 0f, 1f) {
            viewModel.setOptics(grain = it)
        }
    }
}

@Composable
private fun PhotoTransformPanel(
    clip: Clip,
    state: PhotoProjectState,
    viewModel: ChromaPhotoEditorViewModel
) {
    val t = clip.transform
    val isFlippedH = state.flipHorizontalMap[clip.id] == true
    val isFlippedV = state.flipVerticalMap[clip.id] == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhotoToggleChip(
                label = "Flip Horizontal",
                selected = isFlippedH,
                onClick = { viewModel.toggleFlipHorizontal(clip.id) },
                modifier = Modifier.weight(1f)
            )
            PhotoToggleChip(
                label = "Flip Vertical",
                selected = isFlippedV,
                onClick = { viewModel.toggleFlipVertical(clip.id) },
                modifier = Modifier.weight(1f)
            )
        }
        PropertySlider("Scale", t.scale, 0.1f, 3f) { viewModel.setTransform(t.copy(scale = it)) }
        PropertySlider("X Pos", t.x, -500f, 500f) { viewModel.setTransform(t.copy(x = it)) }
        PropertySlider("Y Pos", t.y, -500f, 500f) { viewModel.setTransform(t.copy(y = it)) }
        PropertySlider("Rotate", t.rotation, -180f, 180f) { viewModel.setTransform(t.copy(rotation = it)) }
        PropertySlider("Opacity", t.opacity, 0f, 1f) { viewModel.setTransform(t.copy(opacity = it)) }
    }
}

@Composable
private fun PhotoTextEditorPanel(clip: Clip, viewModel: ChromaPhotoEditorViewModel) {
    val colors = LocalOscellionColors.current

    val textSwatches = listOf(
        Color.White, Color.Black,
        Color(0xFFFF3B30), Color(0xFFFF9500),
        Color(0xFFFFCC00), Color(0xFF34C759),
        Color(0xFF007AFF), Color(0xFFAF52DE),
        Color(0xFFFF2D55), Color(0xFF5AC8FA),
        Color(0xFFFF6B35), Color(0xFF10B981)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = clip.text,
            onValueChange = { viewModel.setText(it) },
            label = { Text("Text Content", color = colors.textMuted, fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.glassBorder,
                focusedTextColor = colors.textMain,
                unfocusedTextColor = colors.textMain,
                cursorColor = colors.primary
            ),
            maxLines = 3
        )

        Spacer(Modifier.height(10.dp))
        PropertySlider("Size", clip.textSize, 12f, 120f) { viewModel.setTextSize(it) }
        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhotoToggleChip("Bold", clip.textBold, { viewModel.setTextBold(!clip.textBold) }, Modifier.weight(1f))
            PhotoToggleChip("Italic", clip.textItalic, { viewModel.setTextItalic(!clip.textItalic) }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))
        Text("Color", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            textSwatches.take(6).forEach { swatchColor ->
                val sel = clip.textColor == swatchColor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .then(
                            if (sel) Modifier.border(2.dp, colors.primary, CircleShape)
                            else Modifier.border(0.5.dp, Color(0xFF333333), CircleShape)
                        )
                        .clickable { viewModel.setTextColor(swatchColor) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            textSwatches.drop(6).take(6).forEach { swatchColor ->
                val sel = clip.textColor == swatchColor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .then(
                            if (sel) Modifier.border(2.dp, colors.primary, CircleShape)
                            else Modifier.border(0.5.dp, Color(0xFF333333), CircleShape)
                        )
                        .clickable { viewModel.setTextColor(swatchColor) }
                )
            }
        }
    }
}

@Composable
private fun PhotoToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.8f) else colors.glassBg.copy(alpha = 0.3f))
            .border(
                0.5.dp,
                if (selected) colors.primary else colors.glassBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) colors.onPrimary else colors.textMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PhotoBlendPanel(clip: Clip, viewModel: ChromaPhotoEditorViewModel) {
    val colors = LocalOscellionColors.current
    val modes = BlendMode.values().toList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text("Blend Mode", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        modes.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { mode ->
                    val selected = clip.blendMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) Brush.linearGradient(listOf(colors.primary, colors.primaryHover))
                                else Brush.linearGradient(listOf(colors.glassBg, colors.glassBg))
                            )
                            .border(
                                0.5.dp,
                                if (selected) Color.Transparent else colors.glassBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setBlendMode(mode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (selected) colors.onPrimary else colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PhotoChromaKeyPanel(clip: Clip, viewModel: ChromaPhotoEditorViewModel) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Chroma Key", color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            AppleSwitch(
                checked = clip.chromaKeyEnabled,
                onCheckedChange = { viewModel.updateClipChromaKeyEnabled(clip.id, it) }
            )
        }

        if (clip.chromaKeyEnabled) {
            PropertySlider("Intensity", clip.chromaKeyIntensity, 0f, 1f) {
                viewModel.updateClipChromaKeyIntensity(clip.id, it)
            }
            PropertySlider("Spill Suppression", clip.chromaKeySpill, 0f, 1f) {
                viewModel.updateClipChromaKeySpill(clip.id, it)
            }
        }
    }
}

@Composable
private fun PhotoMarkupPanel(
    state: PhotoProjectState,
    viewModel: ChromaPhotoEditorViewModel
) {
    val colors = LocalOscellionColors.current
    val swatches = listOf(
        Color(0xFF007AFF), Color(0xFFFF2D55), Color(0xFF34C759),
        Color(0xFFFFCC00), Color(0xFFAF52DE), Color.White
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Markup Brush (Draw on Preview)", color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (state.brushStrokes.isNotEmpty()) {
                Text(
                    text = "Clear (${state.brushStrokes.size})",
                    color = Color(0xFFFF3B30),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { viewModel.clearBrushStrokes() }
                )
            }
        }

        PropertySlider("Brush Size", state.activeBrushSize, 3f, 42f) {
            viewModel.setBrushStyle(state.activeBrushColor, it, state.activeBrushNeon)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Neon Glow Ink", color = colors.textMain, fontSize = 12.sp)
            AppleSwitch(
                checked = state.activeBrushNeon,
                onCheckedChange = { viewModel.setBrushStyle(state.activeBrushColor, state.activeBrushSize, it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            swatches.forEach { c ->
                val sel = state.activeBrushColor == c
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (sel) 2.5.dp else 1.dp, if (sel) colors.primary else colors.glassBorder, CircleShape)
                        .clickable { viewModel.setBrushStyle(c, state.activeBrushSize, state.activeBrushNeon) }
                )
            }
        }
    }
}

@Composable
private fun PhotoFramePanel(
    state: PhotoProjectState,
    viewModel: ChromaPhotoEditorViewModel
) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Studio Frame & Matte", color = colors.textMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PhotoFrameStyle.values().forEach { style ->
                val selected = state.frameStyle == style
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color(0xFF3B82F6) else Color(0xFFF3F4F6))
                        .border(1.dp, if (selected) Color(0xFF3B82F6) else Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                        .clickable {
                            val defaultPad = when (style) {
                                PhotoFrameStyle.NONE -> 0f
                                PhotoFrameStyle.GALLERY_WHITE -> 14f
                                PhotoFrameStyle.POLAROID -> 14f
                                else -> 8f
                            }
                            viewModel.setFrame(style = style, padding = defaultPad)
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = style.label,
                        color = if (selected) Color.White else Color(0xFF374151),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        PropertySlider("Padding", state.framePadding, 0f, 36f) {
            viewModel.setFrame(padding = it)
        }
        PropertySlider("Rounding", state.frameCornerRadius, 0f, 32f) {
            viewModel.setFrame(cornerRadius = it)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ADD MEDIA SHEET (Uses AppIconBox + OscellionGalleryLayout + GlassBottomSheet)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPhotoMediaSheet(
    onDismiss: () -> Unit,
    onAddMedia: (MediaType, String, Int, Color, String?) -> Unit,
    onOpenMarkup: () -> Unit
) {
    val colors = LocalOscellionColors.current
    var activeGalleryPickerType by remember { mutableStateOf<MediaType?>(null) }

    GlassBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Add to Photo Canvas",
                color = Color(0xFF1C1C1E),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            data class MediaOption(
                val icon: androidx.compose.ui.graphics.vector.ImageVector,
                val label: String,
                val subtext: String,
                val color: Color
            )

            val options = listOf(
                MediaOption(Lucide.Image, "Image Overlay", "Add a PIP photo or PNG texture", Color(0xFF34C759)),
                MediaOption(Lucide.AlignLeft, "Text Layer", "Add typography overlay", Color(0xFFAF52DE)),
                MediaOption(Lucide.Layers, "Sticker", "Add a PNG sticker overlay", Color(0xFFFF9500)),
                MediaOption(Lucide.Brush, "Markup Brush", "Draw freehand or neon strokes", Color(0xFF30B0C7))
            )

            options.forEachIndexed { i, opt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            when (i) {
                                0 -> activeGalleryPickerType = MediaType.IMAGE
                                1 -> {
                                    onAddMedia(MediaType.TEXT, "Text", 3, Color(0xFF701A75), null)
                                    onDismiss()
                                }
                                2 -> activeGalleryPickerType = MediaType.STICKER
                                3 -> onOpenMarkup()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AppIconBox(backgroundColor = opt.color) {
                        Icon(opt.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(opt.label, color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(opt.subtext, color = Color(0xFF6B7280), fontSize = 11.sp)
                    }
                    Icon(Lucide.ChevronRight, contentDescription = null, tint = Color(0xFF333333), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (activeGalleryPickerType != null) {
        OscellionGalleryLayout(
            layoutMode = GalleryLayoutMode.BOTTOM_SHEET,
            title = if (activeGalleryPickerType == MediaType.STICKER) "Sticker Overlay" else "Image Overlay",
            badgeText = "Editor",
            allowedMediaFilter = GalleryMediaTypeFilter.PHOTOS,
            showCameraTile = true,
            showCaptionInput = false,
            onVideoSelected = {},
            onPhotosSelected = { uris ->
                val currentType = activeGalleryPickerType
                activeGalleryPickerType = null
                uris.firstOrNull()?.let { uri ->
                    when (currentType) {
                        MediaType.STICKER -> onAddMedia(MediaType.STICKER, "Sticker", 5, Color(0xFFB45309), uri.toString())
                        else -> onAddMedia(MediaType.IMAGE, "Image", 2, Color(0xFF064E3B), uri.toString())
                    }
                }
                onDismiss()
            },
            onClose = { activeGalleryPickerType = null }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PROJECT SETTINGS & LAYER MANAGER PANELS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PhotoProjectSettingsPanel(
    state: PhotoProjectState,
    onFitModeChange: (VideoFitMode) -> Unit,
    onRatioChange: (ProjectRatio) -> Unit,
    onResetAll: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Project Settings", color = colors.textMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Reset Edits",
                color = Color(0xFFFF3B30),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onResetAll() }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Aspect Ratio", color = colors.textMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProjectRatio.values().forEach { ratio ->
                    val selected = state.projectRatio == ratio
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Color(0xFF3B82F6) else Color(0xFFF3F4F6))
                            .border(1.dp, if (selected) Color(0xFF3B82F6) else Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                            .clickable { onRatioChange(ratio) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ratio.label,
                            color = if (selected) Color.White else Color(0xFF374151),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Photo Scale Mode", color = colors.textMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            data class FitOption(
                val mode: VideoFitMode,
                val label: String,
                val desc: String,
                val icon: androidx.compose.ui.graphics.vector.ImageVector
            )
            val options = listOf(
                FitOption(VideoFitMode.FIT, "Fit", "Show full photo", Lucide.Expand),
                FitOption(VideoFitMode.FILL, "Fill", "Fill the frame", Lucide.Maximize2),
                FitOption(VideoFitMode.CROP, "Crop to Fit", "Crop to fill", Lucide.Crop)
            )
            options.forEach { option ->
                val selected = state.videoFitMode == option.mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Color(0xFFEFF6FF) else Color(0xFFF9FAFB))
                        .border(1.5.dp, if (selected) Color(0xFF3B82F6) else Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                        .clickable { onFitModeChange(option.mode) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFF3B82F6) else Color(0xFFE5E7EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(option.icon, contentDescription = null, tint = if (selected) Color.White else Color(0xFF6B7280), modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(option.label, color = if (selected) Color(0xFF3B82F6) else Color(0xFF1C1C1E), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(option.desc, color = Color(0xFF8E8E93), fontSize = 11.sp)
                    }
                    if (selected) {
                        Icon(Lucide.Check, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PhotoLayerManagerPanel(
    clips: List<Clip>,
    trackVisibility: Map<Int, Boolean>,
    trackLock: Map<Int, Boolean>,
    onToggleVisibility: (Int) -> Unit,
    onToggleLock: (Int) -> Unit
) {
    val colors = LocalOscellionColors.current
    val trackNames = mapOf(
        1 to "Track 1 • Main Photo",
        2 to "Track 2 • Image Overlay",
        3 to "Track 3 • Text Layer",
        4 to "Track 4 • Markup Brush",
        5 to "Track 5 • Stickers"
    )
    val trackIcons = mapOf(
        1 to Lucide.Image,
        2 to Lucide.Layers,
        3 to Lucide.Type,
        4 to Lucide.Brush,
        5 to Lucide.Sticker
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Layer Manager", color = colors.textMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))

        (1..5).forEach { trackId ->
            val count = clips.count { it.trackId == trackId }
            val visible = trackVisibility[trackId] ?: true
            val locked = trackLock[trackId] ?: false
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF9FAFB))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppleSettingsSquircleVectorIcon(
                        icon = trackIcons[trackId] ?: Lucide.Layers,
                        size = 28.dp,
                        iconTint = Color(0xFF0A84FF)
                    )
                    Column {
                        Text(
                            text = trackNames[trackId] ?: "Track $trackId",
                            color = colors.textMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            text = "$count ${if (count == 1) "item" else "items"}",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassIconButton(
                        onClick = { onToggleVisibility(trackId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (visible) Lucide.Eye else Lucide.EyeOff,
                            contentDescription = "Toggle Visibility",
                            tint = colors.textMain,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    GlassIconButton(
                        onClick = { onToggleLock(trackId) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (locked) Lucide.Lock else Lucide.LockOpen,
                            contentDescription = "Toggle Lock",
                            tint = colors.textMain,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXPORT DIALOG (Uses AppleDialog from HomeScreen.kt) & MEDIASTORE EXPORTER
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PhotoExportSettingsDialog(
    initialSettings: PhotoExportSettings,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onExport: (PhotoExportSettings) -> Unit
) {
    val colors = LocalOscellionColors.current
    var resolution by remember { mutableIntStateOf(initialSettings.resolution) }
    var format by remember { mutableStateOf(initialSettings.format) }
    var quality by remember { mutableIntStateOf(initialSettings.quality) }

    AppleDialog(
        title = "Export Studio Photo",
        message = "Choose output resolution and image format to save to your device gallery.",
        primaryButtonText = if (isExporting) "Rendering..." else "Export Photo",
        onPrimaryClick = {
            if (!isExporting) {
                onExport(PhotoExportSettings(resolution = resolution, format = format, quality = quality))
            }
        },
        secondaryButtonText = "Cancel",
        onSecondaryClick = onDismiss,
        onDismiss = onDismiss,
        customContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Resolution", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1080 to "1080p", 2160 to "4K UHD", 4320 to "8K Max").forEach { (res, label) ->
                        val sel = resolution == res
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) Color(0xFF007AFF) else Color(0xFFF2F2F7))
                                .clickable { resolution = res },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (sel) Color.White else Color(0xFF1C1C1E),
                                fontSize = 12.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Text("Format", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("PNG", "JPEG", "WEBP").forEach { fmt ->
                        val sel = format == fmt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) Color(0xFF007AFF) else Color(0xFFF2F2F7))
                                .clickable { format = fmt },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fmt,
                                color = if (sel) Color.White else Color(0xFF1C1C1E),
                                fontSize = 12.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    )
}

private suspend fun exportEditedPhotoToGallery(
    context: Context,
    state: PhotoProjectState
): Uri? = withContext(Dispatchers.IO) {
    try {
        val loader = ImageLoader(context)
        val outWidth = state.exportSettings.resolution.coerceIn(720, 4096)
        val outHeight = ((outWidth.toFloat() / state.projectRatio.w) * state.projectRatio.h).toInt().coerceIn(720, 4096)

        val outputBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(outputBitmap)

        // Frame background
        canvas.drawColor(
            if (state.frameStyle == PhotoFrameStyle.NONE) android.graphics.Color.BLACK
            else state.frameColor.toArgb()
        )

        val visibleClips = state.clips
            .filter { state.trackVisibility[it.trackId] != false }
            .sortedBy { it.trackId }

        for (clip in visibleClips) {
            val t = clip.transform
            val flipH = if (state.flipHorizontalMap[clip.id] == true) -1f else 1f
            val flipV = if (state.flipVerticalMap[clip.id] == true) -1f else 1f
            when (clip.type) {
                MediaType.IMAGE, MediaType.VIDEO, MediaType.STICKER -> {
                    val uriStr = clip.uri ?: continue
                    val req = ImageRequest.Builder(context)
                        .data(Uri.parse(uriStr))
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(req)
                    val bmp = (result as? SuccessResult)?.drawable?.let { (it as? BitmapDrawable)?.bitmap } ?: continue

                    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG).apply {
                        alpha = (t.opacity * 255).toInt().coerceIn(0, 255)
                        colorFilter = ColorMatrixColorFilter(
                            buildPhotoPreviewColorMatrix(
                                filter = clip.filter,
                                filterIntensity = state.filterIntensity,
                                adj = clip.adjustments,
                                warmth = if (clip.trackId == 1) state.warmth else 0f,
                                tint = if (clip.trackId == 1) state.tint else 0f,
                                sharpness = if (clip.trackId == 1) state.sharpness else 0f,
                                dehaze = if (clip.trackId == 1) state.dehaze else 0f,
                                toneCurves = if (clip.trackId == 1) state.toneCurves else ToneCurvePoints(),
                                redCurve = if (clip.trackId == 1) state.redCurve else ToneCurvePoints(),
                                greenCurve = if (clip.trackId == 1) state.greenCurve else ToneCurvePoints(),
                                blueCurve = if (clip.trackId == 1) state.blueCurve else ToneCurvePoints(),
                                splitToning = if (clip.trackId == 1) state.splitToning else SplitToningState()
                            )
                        )
                        if (clip.blendMode != BlendMode.NORMAL) {
                            xfermode = PorterDuffXfermode(
                                when (clip.blendMode) {
                                    BlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
                                    BlendMode.SCREEN -> PorterDuff.Mode.SCREEN
                                    BlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
                                    BlendMode.NORMAL -> PorterDuff.Mode.SRC_OVER
                                }
                            )
                        }
                    }

                    canvas.save()
                    val cx = outWidth / 2f + (t.x / 300f) * outWidth
                    val cy = outHeight / 2f + (t.y / 300f) * outHeight
                    canvas.translate(cx, cy)
                    canvas.rotate(t.rotation)
                    canvas.scale(t.scale * flipH, t.scale * flipV)

                    val fitScale = if (state.videoFitMode == VideoFitMode.FILL || state.videoFitMode == VideoFitMode.CROP) {
                        max(outWidth.toFloat() / bmp.width, outHeight.toFloat() / bmp.height)
                    } else {
                        min(outWidth.toFloat() / bmp.width, outHeight.toFloat() / bmp.height)
                    }
                    val scaleMult = if (clip.trackId == 1) 1f else 0.55f
                    val drawW = bmp.width * fitScale * scaleMult
                    val drawH = bmp.height * fitScale * scaleMult
                    val dstRect = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
                    canvas.drawBitmap(bmp, null, dstRect, paint)
                    canvas.restore()
                }

                MediaType.TEXT -> {
                    val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                        color = clip.textColor.toArgb()
                        alpha = (t.opacity * 255).toInt().coerceIn(0, 255)
                        textSize = clip.textSize * (outWidth / 380f)
                        textAlign = AndroidPaint.Align.CENTER
                        typeface = Typeface.create(
                            Typeface.DEFAULT,
                            when {
                                clip.textBold && clip.textItalic -> Typeface.BOLD_ITALIC
                                clip.textBold -> Typeface.BOLD
                                clip.textItalic -> Typeface.ITALIC
                                else -> Typeface.NORMAL
                            }
                        )
                    }
                    canvas.save()
                    val cx = outWidth / 2f + (t.x / 300f) * outWidth
                    val cy = outHeight / 2f + (t.y / 300f) * outHeight
                    canvas.translate(cx, cy)
                    canvas.rotate(t.rotation)
                    canvas.scale(t.scale * flipH, t.scale * flipV)
                    canvas.drawText(clip.text, 0f, 0f, textPaint)
                    canvas.restore()
                }

                MediaType.AUDIO -> {}
            }
        }

        // Optical Vignette
        if (state.vignette > 0.01f) {
            val vPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                shader = AndroidRadialGradient(
                    outWidth / 2f,
                    outHeight / 2f,
                    max(outWidth, outHeight) * 0.72f,
                    intArrayOf(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.argb((state.vignette * 210).toInt().coerceIn(0, 235), 0, 0, 0)
                    ),
                    floatArrayOf(0.35f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, outWidth.toFloat(), outHeight.toFloat(), vPaint)
        }

        // Markup Brush Strokes (Track 4)
        if (state.trackVisibility[4] != false && state.brushStrokes.isNotEmpty()) {
            val scaleFactor = outWidth / 380f
            for (stroke in state.brushStrokes) {
                if (stroke.points.size < 2) continue
                val path = AndroidPath().apply {
                    moveTo(stroke.points.first().x * scaleFactor, stroke.points.first().y * scaleFactor)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x * scaleFactor, stroke.points[i].y * scaleFactor)
                    }
                }
                val sPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    color = stroke.color.toArgb()
                    style = AndroidPaint.Style.STROKE
                    strokeWidth = stroke.strokeWidth * scaleFactor
                    strokeCap = AndroidPaint.Cap.ROUND
                    strokeJoin = AndroidPaint.Join.ROUND
                }
                canvas.drawPath(path, sPaint)
            }
        }

        val ext = when (state.exportSettings.format) {
            "JPEG" -> "jpg"
            "WEBP" -> "webp"
            else -> "png"
        }
        val mime = when (state.exportSettings.format) {
            "JPEG" -> "image/jpeg"
            "WEBP" -> "image/webp"
            else -> "image/png"
        }
        val filename = "Oscellion_Photo_${System.currentTimeMillis()}.$ext"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Oscellion")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val outUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        resolver.openOutputStream(outUri)?.use { stream ->
            val compressFormat = when (state.exportSettings.format) {
                "JPEG" -> Bitmap.CompressFormat.JPEG
                "WEBP" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                else -> Bitmap.CompressFormat.PNG
            }
            outputBitmap.compress(compressFormat, state.exportSettings.quality, stream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(outUri, values, null, null)
        }
        outUri
    } catch (e: Exception) {
        null
    }
}