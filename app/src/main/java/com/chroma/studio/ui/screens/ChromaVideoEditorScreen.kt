package com.chroma.studio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import com.chroma.studio.ui.components.DynamicIslandManager
import com.chroma.studio.ui.components.ToastManager
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.chroma.studio.ui.components.AppleSlider
import com.chroma.studio.ui.components.AppleSwitch
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.GlassPanel
import com.chroma.studio.ui.components.LocalHazeState
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════════════════
// DATA MODELS  (in com.chroma.studio.ui.screens for FFmpegExporter compatibility)
// ═══════════════════════════════════════════════════════════════════════════════



enum class TransitionType(val label: String) {
    NONE("None"),
    CROSSFADE("Crossfade"),
    DISSOLVE("Dissolve"),
    SLIDE_LEFT("Slide L"),
    SLIDE_RIGHT("Slide R"),
    ZOOM_IN("Zoom In"),
    WIPE("Wipe")
}

enum class SpeedCurve(val label: String) {
    NORMAL("Normal (1x)"),
    HERO("Hero (Ramp down)"),
    BULLET_TIME("Bullet Time"),
    FLASH("Flash (Ramp up)"),
    SLOW_MO("Slow Mo (0.5x)"),
    FAST("Fast (2x)")
}

enum class MediaType { VIDEO, IMAGE, TEXT, AUDIO, STICKER }

/** Custom editor blend mode — intentionally distinct from androidx.compose.ui.graphics.BlendMode */
enum class BlendMode { NORMAL, MULTIPLY, SCREEN, OVERLAY }

enum class TextAnimation { NONE, TYPEWRITER, BOUNCE, FADE_UP }

enum class FilterType(val label: String) {
    NONE("None"),
    GRAYSCALE("Mono"),
    SEPIA("Sepia"),
    INVERT("Invert"),
    WARM("Warm"),
    COOL("Cool"),
    CINEMATIC("Cinema"),
    FADE("Fade"),
    VIVID("Vivid"),
    MATTE("Matte"),
    NEON("Neon"),
    SUNSET("Sunset"),
    NOIR("Noir");

    /**
     * Raw 4x5 color-matrix values for this preset, or null for NONE.
     * Exposed separately from [toColorFilter] so the live preview pipeline can combine
     * a preset's matrix with the clip's [Adjustments] matrix before rendering.
     */
    fun matrixValues(): FloatArray? = when (this) {
        NONE      -> null
        GRAYSCALE -> ColorMatrix().apply { setToSaturation(0f) }.values
        SEPIA     -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f,     0f,     0f,     1f, 0f
        )
        INVERT    -> floatArrayOf(
            -1f, 0f,  0f,  0f, 255f,
            0f, -1f,  0f,  0f, 255f,
            0f,  0f, -1f,  0f, 255f,
            0f,  0f,  0f,  1f, 0f
        )
        WARM      -> floatArrayOf(
            1.2f, 0f,   0f,   0f,  20f,
            0f,   1.0f, 0f,   0f,   5f,
            0f,   0f,   0.8f, 0f, -20f,
            0f,   0f,   0f,   1f,   0f
        )
        COOL      -> floatArrayOf(
            0.9f, 0f,   0f,   0f, -10f,
            0f,   1.0f, 0f,   0f,   5f,
            0f,   0f,   1.2f, 0f,  20f,
            0f,   0f,   0f,   1f,   0f
        )
        CINEMATIC -> floatArrayOf(
            1.1f, 0f,   0f,   0f, -10f,
            0f,   1.0f, 0f,   0f,  -5f,
            0f,   0f,   0.9f, 0f,  10f,
            0f,   0f,   0f,   1f,   0f
        )
        FADE      -> floatArrayOf(
            0.9f, 0f,   0f,   0f, 30f,
            0f,   0.9f, 0f,   0f, 25f,
            0f,   0f,   0.9f, 0f, 25f,
            0f,   0f,   0f,   1f,  0f
        )
        VIVID     -> ColorMatrix().apply { setToSaturation(1.8f) }.values
        MATTE     -> floatArrayOf(
            0.85f, 0f,    0f,    0f, 20f,
            0f,    0.85f, 0f,    0f, 15f,
            0f,    0f,    0.85f, 0f, 20f,
            0f,    0f,    0f,    1f,  0f
        )
        NEON      -> floatArrayOf(
            0.8f,  0.2f,  0f,   0f,  0f,
            0f,    0.9f,  0.1f, 0f,  0f,
            0.2f,  0f,    1.0f, 0f, 50f,
            0f,    0f,    0f,   1f,  0f
        )
        SUNSET    -> floatArrayOf(
            1.3f, 0f,   0f,   0f,  30f,
            0f,   0.9f, 0f,   0f, -10f,
            0f,   0f,   0.7f, 0f, -30f,
            0f,   0f,   0f,   1f,   0f
        )
        NOIR      -> floatArrayOf(
            0.33f, 0.33f, 0.33f, 0f, -10f,
            0.33f, 0.33f, 0.33f, 0f, -10f,
            0.33f, 0.33f, 0.33f, 0f, -10f,
            0f,    0f,    0f,    1f,   0f
        )
    }

    fun toColorFilter(): ColorFilter? = matrixValues()?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LIVE PREVIEW COLOR PIPELINE
// Combines a clip's preset FilterType with its manual Adjustments into a single
// android.graphics.ColorMatrix so both can be applied to the video surface (via
// RenderEffect) and to Compose-drawn overlays (via ColorFilter) at the same time.
// ═══════════════════════════════════════════════════════════════════════════════

private fun buildPreviewColorMatrix(filter: FilterType, adj: Adjustments): android.graphics.ColorMatrix {
    val result = android.graphics.ColorMatrix()

    // Saturation
    result.postConcat(android.graphics.ColorMatrix().apply { setSaturation(adj.saturation) })

    // Hue rotation (standard luminance-preserving hue matrix)
    if (adj.hue != 0f) {
        val radians = Math.toRadians(adj.hue.toDouble())
        val cos = kotlin.math.cos(radians).toFloat()
        val sin = kotlin.math.sin(radians).toFloat()
        val lumR = 0.213f; val lumG = 0.715f; val lumB = 0.072f
        val hueValues = floatArrayOf(
            lumR + cos * (1 - lumR) + sin * (-lumR), lumG + cos * (-lumG) + sin * (-lumG), lumB + cos * (-lumB) + sin * (1 - lumB), 0f, 0f,
            lumR + cos * (-lumR) + sin * (0.143f),    lumG + cos * (1 - lumG) + sin * (0.140f), lumB + cos * (-lumB) + sin * (-0.283f), 0f, 0f,
            lumR + cos * (-lumR) + sin * (-(1 - lumR)), lumG + cos * (-lumG) + sin * (lumG), lumB + cos * (1 - lumB) + sin * (lumB), 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        result.postConcat(android.graphics.ColorMatrix(hueValues))
    }

    // Brightness / Contrast / Exposure / Highlights / Shadows — combined scale + offset
    val exposureScale = Math.pow(2.0, adj.exposure.toDouble()).toFloat()
    val contrastScale = adj.contrast
    val totalScale = exposureScale * contrastScale
    val offset = adj.brightness * 255f +
            (1f - contrastScale) * 128f +
            adj.highlights * 40f -
            adj.shadows * 40f
    result.postConcat(
        android.graphics.ColorMatrix(
            floatArrayOf(
                totalScale, 0f, 0f, 0f, offset,
                0f, totalScale, 0f, 0f, offset,
                0f, 0f, totalScale, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )

    // Preset filter look on top, if any
    filter.matrixValues()?.let { result.postConcat(android.graphics.ColorMatrix(it)) }

    return result
}

/** Both android.graphics.ColorMatrix and Compose's ColorMatrix use the same 20-value row-major layout. */
private fun android.graphics.ColorMatrix.toComposeColorMatrix(): ColorMatrix = ColorMatrix(this.array)

data class Adjustments(
    val brightness: Float = 0f,     // −1 to 1
    val contrast:   Float = 1f,     //  0 to 2
    val saturation: Float = 1f,     //  0 to 3
    val hue:        Float = 0f,     //  0 to 360
    val exposure:   Float = 0f,     // −2 to 2
    val highlights: Float = 0f,     // −1 to 1
    val shadows:    Float = 0f      // −1 to 1
)

data class TransformState(
    val scale:    Float = 1.0f,
    val x:        Float = 0f,
    val y:        Float = 0f,
    val rotation: Float = 0f,
    val opacity:  Float = 1.0f
)

data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val timeOffset: Float,
    val transform: TransformState
)

data class Clip(
    val id:              String       = UUID.randomUUID().toString(),
    val type:            MediaType    = MediaType.VIDEO,
    val name:            String       = "",
    val uri:             String?      = null,
    var text:            String       = "Add text here",
    val trackId:         Int          = 1,
    var startTime:       Float        = 0f,
    var duration:        Float        = 5f,
    val color:           Color        = Color(0xFF1E3A8A),
    var transform:       TransformState = TransformState(),
    val playbackRate:    Float        = 1.0f,
    var filter:          FilterType   = FilterType.NONE,
    var adjustments:     Adjustments  = Adjustments(),
    var blendMode:       BlendMode    = BlendMode.NORMAL,
    var volume:          Float        = 1f,
    var sourceStartTime: Float        = 0f,
    val fadeIn:          Float        = 0f,
    val fadeOut:         Float        = 0f,
    val textColor:       Color        = Color.White,
    val textSize:        Float        = 48f,
    val textBold:        Boolean      = false,
    val textItalic:      Boolean      = false,
    var transitionIn:    TransitionType = TransitionType.NONE,
    var transitionOut:   TransitionType = TransitionType.NONE,
    val keyframes:       List<Keyframe> = emptyList(),
    var speedCurve:      SpeedCurve   = SpeedCurve.NORMAL,
    var preservePitch:   Boolean      = true,
    var chromaKeyEnabled: Boolean     = false,
    var chromaKeyColor:  Color        = Color.Green,
    var chromaKeyIntensity: Float     = 0.1f,
    var chromaKeySpill:  Float        = 0.1f,
    var audioFadeIn:     Float        = 0f,
    var audioFadeOut:    Float        = 0f,
    var textAnimation:   TextAnimation = TextAnimation.NONE
) {
    val endTime: Float get() = startTime + duration

    fun getInterpolatedTransform(currentTime: Float): TransformState {
        if (keyframes.isEmpty()) return transform
        val relativeTime = currentTime - startTime
        
        val before = keyframes.filter { it.timeOffset <= relativeTime }.maxByOrNull { it.timeOffset }
        val after = keyframes.filter { it.timeOffset > relativeTime }.minByOrNull { it.timeOffset }
        
        if (before == null && after == null) return transform
        if (before != null && after == null) return before.transform
        if (before == null && after != null) return after.transform
        
        val progress = (relativeTime - before!!.timeOffset) / (after!!.timeOffset - before.timeOffset)
        return TransformState(
            scale = before.transform.scale + (after.transform.scale - before.transform.scale) * progress,
            x = before.transform.x + (after.transform.x - before.transform.x) * progress,
            y = before.transform.y + (after.transform.y - before.transform.y) * progress,
            rotation = before.transform.rotation + (after.transform.rotation - before.transform.rotation) * progress,
            opacity = before.transform.opacity + (after.transform.opacity - before.transform.opacity) * progress
        )
    }
}

data class ExportSettings(
    val resolution: Int = 1080, // 720, 1080, 2160 (4K)
    val fps: Int = 30, // 24, 30, 60
    val codec: String = "libx264" // "libx264" or "libx265"
)

enum class VideoFitMode { FIT, FILL, CROP }
enum class ProjectRatio(val label: String, val w: Int, val h: Int) {
    RATIO_9_16("9:16", 9, 16), RATIO_16_9("16:9", 16, 9),
    RATIO_1_1("1:1", 1, 1), RATIO_4_3("4:3", 4, 3), RATIO_4_5("4:5", 4, 5)
}

data class ProjectState(
    val duration:        Float      = 60f,
    val isPlaying:       Boolean    = false,
    val pixelsPerSecond: Float      = 60f,
    val clips:           List<Clip> = emptyList(),
    val selectedClipId:  String?    = null,
    val exportSettings:  ExportSettings = ExportSettings(),
    val trackVisibility: Map<Int, Boolean> = mapOf(1 to true, 2 to true, 3 to true, 4 to true, 5 to true),
    val trackLock:       Map<Int, Boolean> = mapOf(1 to false, 2 to false, 3 to false, 4 to false, 5 to false),
    val videoFitMode:    VideoFitMode = VideoFitMode.FIT,
    val projectRatio:    ProjectRatio = ProjectRatio.RATIO_9_16
)

// ═══════════════════════════════════════════════════════════════════════════════
// EDITOR TOOL
// ═══════════════════════════════════════════════════════════════════════════════

enum class EditorTool(val label: String) {
    TRIM("Trim"), ADJUST("Adjust"), FILTER("Filter"), 
    TRANSFORM("Transform"), AUDIO("Audio"), TEXT("Text"), 
    SPEED("Speed"), BLEND("Blend"), CHROMA_KEY("Chroma Key")
}

// ═══════════════════════════════════════════════════════════════════════════════
// VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

class ChromaEditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProjectState())
    val state: StateFlow<ProjectState> = _state.asStateFlow()

    private val _currentTime = MutableStateFlow(0f)
    val currentTime: StateFlow<Float> = _currentTime.asStateFlow()

    private val undoStack    = mutableListOf<List<Clip>>()
    private val redoStack    = mutableListOf<List<Clip>>()
    private var dragSnapshot: List<Clip>? = null

    fun initialize(uri: String) {
        if (_state.value.clips.isNotEmpty()) return
        val clip = Clip(
            type     = MediaType.VIDEO,
            name     = "Main Video",
            trackId  = 1,
            uri      = uri,
            duration = 30f,
            color    = Color(0xFF1E3A8A)
        )
        _state.update { it.copy(clips = listOf(clip), duration = 60f, selectedClipId = clip.id) }
    }

    fun setVideoFitMode(mode: VideoFitMode) { _state.update { it.copy(videoFitMode = mode) } }
    fun setProjectRatio(ratio: ProjectRatio) { _state.update { it.copy(projectRatio = ratio) } }
    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()

    private fun pushUndo() {
        undoStack.add(_state.value.clips)
        if (undoStack.size > 20) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(_state.value.clips)
        _state.update { it.copy(clips = undoStack.removeLast(), selectedClipId = null) }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(_state.value.clips)
        _state.update { it.copy(clips = redoStack.removeLast()) }
    }

    fun startDrag() { dragSnapshot = _state.value.clips }
    fun endDrag() {
        dragSnapshot?.let {
            undoStack.add(it)
            if (undoStack.size > 20) undoStack.removeAt(0)
            redoStack.clear()
        }
        dragSnapshot = null
    }

    fun selectClip(id: String?) = _state.update { it.copy(selectedClipId = id) }

    fun seekTo(time: Float) {
        _currentTime.value = time.coerceIn(0f, _state.value.duration)
    }

    fun setZoom(pps: Float) = _state.update { it.copy(pixelsPerSecond = pps.coerceIn(10f, 300f)) }

    fun addClip(type: MediaType, name: String, trackId: Int, color: Color, uri: String? = null) {
        pushUndo()
        val clips  = _state.value.clips
        val start  = clips.filter { it.trackId == trackId }.maxOfOrNull { it.endTime } ?: 0f
        val clip   = Clip(type = type, name = name, trackId = trackId, color = color, uri = uri, startTime = start)
        val newDur = max(_state.value.duration, clip.endTime + 5f)
        _state.update { it.copy(clips = clips + clip, duration = newDur, selectedClipId = clip.id) }
    }

    fun deleteSelected() {
        pushUndo()
        val id = _state.value.selectedClipId ?: return
        val clipToDelete = _state.value.clips.find { it.id == id } ?: return
        var newClips = _state.value.clips.filter { c -> c.id != id }
        
        // Ripple delete for track 1
        if (clipToDelete.trackId == 1) {
            newClips = newClips.map { c ->
                if (c.trackId == 1 && c.startTime > clipToDelete.startTime) {
                    c.copy(startTime = (c.startTime - clipToDelete.duration).coerceAtLeast(0f))
                } else c
            }
        }
        _state.update { it.copy(clips = newClips, selectedClipId = null) }
    }

    fun trimStart(playheadTime: Float) {
        val clip = _state.value.clips.find { it.id == _state.value.selectedClipId } ?: return
        val t = playheadTime
        if (t <= clip.startTime || t >= clip.endTime) return
        pushUndo()
        val trimAmount = t - clip.startTime
        val newClip = clip.copy(startTime = t, duration = clip.duration - trimAmount, sourceStartTime = clip.sourceStartTime + trimAmount)
        _state.update { s -> s.copy(clips = s.clips.map { if (it.id == clip.id) newClip else it }) }
    }

    fun trimEnd(playheadTime: Float) {
        val clip = _state.value.clips.find { it.id == _state.value.selectedClipId } ?: return
        val t = playheadTime
        if (t <= clip.startTime || t >= clip.endTime) return
        pushUndo()
        val newClip = clip.copy(duration = t - clip.startTime)
        _state.update { s -> s.copy(clips = s.clips.map { if (it.id == clip.id) newClip else it }) }
    }

    fun splitAtPlayhead() {
        val st   = _state.value
        val clip = st.clips.find { it.id == st.selectedClipId } ?: return
        val t    = _currentTime.value
        if (t <= clip.startTime || t >= clip.endTime) return
        pushUndo()
        val left  = clip.copy(duration = t - clip.startTime)
        val right = clip.copy(
            id = UUID.randomUUID().toString(), 
            startTime = t, 
            duration = clip.endTime - t,
            sourceStartTime = clip.sourceStartTime + (t - clip.startTime)
        )
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clip.id) left else it } + right, selectedClipId = right.id)
        }
    }

    fun duplicateSelected() {
        val clip = _state.value.clips.find { it.id == _state.value.selectedClipId } ?: return
        pushUndo()
        val dup  = clip.copy(id = UUID.randomUUID().toString(), startTime = clip.endTime)
        _state.update { it.copy(clips = it.clips + dup, selectedClipId = dup.id) }
    }

    fun moveSelectedClip(deltaSeconds: Float, snapPoints: List<Float> = emptyList()) {
        val id = _state.value.selectedClipId ?: return
        _state.update { s ->
            s.copy(clips = s.clips.map { c ->
                if (c.id == id) {
                    var newTime = (c.startTime + deltaSeconds).coerceAtLeast(0f)
                    
                    // Magnetic snapping engine (threshold = 0.15s)
                    var closestSnap = newTime
                    var minDiff = 0.15f
                    for (snap in snapPoints) {
                        val diff = kotlin.math.abs(newTime - snap)
                        if (diff < minDiff) {
                            minDiff = diff
                            closestSnap = snap
                        }
                        
                        val diffEnd = kotlin.math.abs((newTime + c.duration) - snap)
                        if (diffEnd < minDiff) {
                            minDiff = diffEnd
                            closestSnap = snap - c.duration
                        }
                    }
                    c.copy(startTime = closestSnap.coerceAtLeast(0f))
                } else c
            })
        }
    }

    fun updateClipTransform(clipId: String, transform: TransformState) {
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clipId) it.copy(transform = transform) else it })
        }
    }

    fun updateClipTransition(clipId: String, transitionOut: TransitionType) {
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clipId) it.copy(transitionOut = transitionOut) else it })
        }
    }

    fun updateClipSpeedCurve(clipId: String, curve: SpeedCurve) {
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clipId) it.copy(speedCurve = curve) else it })
        }
    }

    fun updateClipPreservePitch(clipId: String, preservePitch: Boolean) {
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clipId) it.copy(preservePitch = preservePitch) else it })
        }
    }

    fun updateClipTextAnimation(clipId: String, animation: TextAnimation) {
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clipId) it.copy(textAnimation = animation) else it })
        }
    }

    fun updateClipChromaKeyEnabled(clipId: String, enabled: Boolean) {
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
    
    fun updateClipVolume(clipId: String, volume: Float) {
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clipId) it.copy(volume = volume) else it })
        }
    }

    fun updateClipAudioFadeIn(clipId: String, fadeIn: Float) {
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clipId) it.copy(audioFadeIn = fadeIn) else it })
        }
    }

    fun updateClipAudioFadeOut(clipId: String, fadeOut: Float) {
        _state.update { s ->
            s.copy(clips = s.clips.map { if (it.id == clipId) it.copy(audioFadeOut = fadeOut) else it })
        }
    }

    fun updateExportSettings(resolution: Int, fps: Int, codec: String) {
        _state.update { s ->
            s.copy(exportSettings = s.exportSettings.copy(resolution = resolution, fps = fps, codec = codec))
        }
    }

    fun trimSelectedClipDuration(duration: Float) {
        val id = _state.value.selectedClipId ?: return
        val originalClip = _state.value.clips.find { it.id == id } ?: return
        val diff = duration.coerceAtLeast(0.5f) - originalClip.duration
        
        updateSelected { it.copy(duration = duration.coerceAtLeast(0.5f)) }
        
        // Ripple trim for track 1
        if (originalClip.trackId == 1) {
            _state.update { s ->
                s.copy(clips = s.clips.map { c ->
                    if (c.trackId == 1 && c.startTime > originalClip.startTime && c.id != id) {
                        c.copy(startTime = (c.startTime + diff).coerceAtLeast(0f))
                    } else c
                })
            }
        }
    }

    private fun updateSelected(block: (Clip) -> Clip) {
        val id = _state.value.selectedClipId ?: return
        _state.update { s -> s.copy(clips = s.clips.map { if (it.id == id) block(it) else it }) }
    }

    fun setFilter(f: FilterType)        { pushUndo(); updateSelected { it.copy(filter = f) } }
    fun setAdjustments(a: Adjustments)  = updateSelected { it.copy(adjustments = a) }
    fun setTransform(t: TransformState) = updateSelected { it.copy(transform = t) }
    fun setVolume(v: Float)             = updateSelected { it.copy(volume = v.coerceIn(0f, 2f)) }
    fun setFadeIn(f: Float)             = updateSelected { it.copy(fadeIn = f.coerceIn(0f, 5f)) }
    fun setFadeOut(f: Float)            = updateSelected { it.copy(fadeOut = f.coerceIn(0f, 5f)) }
    fun setText(t: String)              = updateSelected { it.copy(text = t) }
    fun setTextColor(c: Color)          { pushUndo(); updateSelected { it.copy(textColor = c) } }
    fun setTextSize(s: Float)           = updateSelected { it.copy(textSize = s.coerceIn(12f, 120f)) }
    fun setTextBold(b: Boolean)         { pushUndo(); updateSelected { it.copy(textBold = b) } }
    fun setTextItalic(i: Boolean)       { pushUndo(); updateSelected { it.copy(textItalic = i) } }
    fun setSpeed(s: Float)              { pushUndo(); updateSelected { it.copy(playbackRate = s.coerceIn(0.25f, 4f)) } }
    fun setBlendMode(m: BlendMode)      { pushUndo(); updateSelected { it.copy(blendMode = m) } }

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
}

// ═══════════════════════════════════════════════════════════════════════════════
// SCREEN ROOT
// ═══════════════════════════════════════════════════════════════════════════════

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromaVideoEditorScreen(
    videoUri:  Uri,
    viewModel: ChromaEditorViewModel = viewModel(),
    onExport:  (ProjectState) -> Unit,
    onBack:    () -> Unit
) {
    val colors     = LocalOscellionColors.current
    val state      by viewModel.state.collectAsState()
    val currentTimeState = viewModel.currentTime.collectAsState()
    val context    = LocalContext.current
    val hazeState  = remember { HazeState() }

    var showAddSheet     by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showLayersSheet by remember { mutableStateOf(false) }
    var transitionTargetClip by remember { mutableStateOf<Clip?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var activeToolSheet by remember { mutableStateOf<EditorTool?>(null) }

    val selectedClip = state.clips.find { it.id == state.selectedClipId }
    val showPanel    = selectedClip != null

    // Default TEXT clips to the text tool
    LaunchedEffect(state.selectedClipId) {
        val clip = state.clips.find { it.id == state.selectedClipId }
        if (clip?.type == MediaType.TEXT) activeToolSheet = EditorTool.TEXT
    }

    LaunchedEffect(videoUri) { viewModel.initialize(videoUri.toString()) }

    // Low-latency ExoPlayer load control
    val loadControl = remember {
        androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50,   // minBufferMs
                500,  // maxBufferMs
                50,   // bufferForPlaybackMs
                100   // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    val player = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                repeatMode = ExoPlayer.REPEAT_MODE_OFF
                setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
            }
    }

    // Suppress Dynamic Island while in editor
    DisposableEffect(player) {
        DynamicIslandManager.isSuppressed = true
        onDispose {
            DynamicIslandManager.isSuppressed = false
            player.release()
        }
    }

    val mainClips = state.clips.filter { it.type == MediaType.VIDEO && it.trackId == 1 }.sortedBy { it.startTime }
    
    // Only rebuild playlist if the structural timing of main clips changes
    val mainClipsTiming = mainClips.map { "${it.id}:${it.sourceStartTime}:${it.duration}:${it.uri}" }
    
    LaunchedEffect(mainClipsTiming) {
        val wasPlaying = player.isPlaying
        val currentPos = player.currentPosition
        val currentWindow = player.currentMediaItemIndex
        
        player.clearMediaItems()
        mainClips.forEach { clip ->
            val u = clip.uri ?: return@forEach
            val startMs = (clip.sourceStartTime * 1000L).toLong()
            val endMs = ((clip.sourceStartTime + clip.duration) * 1000L).toLong()
            val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build()
            val item = MediaItem.Builder()
                .setUri(Uri.parse(u))
                .setClippingConfiguration(clippingConfig)
                .build()
            player.addMediaItem(item)
        }
        player.prepare()
        if (currentWindow < player.mediaItemCount) {
            player.seekTo(currentWindow, currentPos)
        }
        if (wasPlaying) player.play()
    }

    fun seekPlayerToTime(time: Float) {
        if (mainClips.isEmpty() || player.mediaItemCount == 0) return
        var targetIndex = -1
        var offsetInClipSec = 0f
        
        for (i in mainClips.indices) {
            val c = mainClips[i]
            if (time >= c.startTime && time <= c.endTime) {
                targetIndex = i
                offsetInClipSec = (time - c.startTime).coerceIn(0f, c.duration)
                break
            }
        }
        
        if (targetIndex == -1) {
            if (time < (mainClips.firstOrNull()?.startTime ?: 0f)) {
                targetIndex = 0
                offsetInClipSec = 0f
            } else {
                for (i in mainClips.indices) {
                    if (time < mainClips[i].startTime) {
                        targetIndex = (i - 1).coerceAtLeast(0)
                        offsetInClipSec = mainClips[targetIndex].duration
                        break
                    }
                }
                if (targetIndex == -1) {
                    targetIndex = mainClips.lastIndex
                    offsetInClipSec = mainClips.last().duration
                }
            }
        }
        
        if (targetIndex in 0 until player.mediaItemCount) {
            val targetMs = (offsetInClipSec * 1000L).toLong()
            player.seekTo(targetIndex, targetMs)
        }
    }

    // Seek player in real-time when timeline is scrubbed or clips are moved
    LaunchedEffect(currentTimeState.value, state.clips) {
        if (!player.isPlaying) {
            seekPlayerToTime(currentTimeState.value)
        }
    }

    var isPlaying by remember { mutableStateOf(false) }

    if (showExportDialog) {
        ExportSettingsDialog(
            initialSettings = state.exportSettings,
            onDismiss = { showExportDialog = false },
            onExport = { settings ->
                viewModel.updateExportSettings(settings.resolution, settings.fps, settings.codec)
                showExportDialog = false
                onExport(state.copy(exportSettings = settings))
            }
        )
    }

    var activeItemIndex by remember { mutableStateOf(player.currentMediaItemIndex) }
    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                activeItemIndex = player.currentMediaItemIndex
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Continuously sync the timeline/playhead with buttery-smooth monotonic VSYNC pacing and soft convergence.
    LaunchedEffect(isPlaying, mainClips) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrameNanos = 0L

        while (isActive && isPlaying) {
            androidx.compose.runtime.withFrameNanos { frameTimeNanos ->
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameTimeNanos
                    return@withFrameNanos
                }
                val dtNanos = frameTimeNanos - lastFrameNanos
                lastFrameNanos = frameTimeNanos
                val dtSec = (dtNanos / 1_000_000_000f).coerceIn(0f, 0.05f)

                val curIdx = player.currentMediaItemIndex
                val curClip = mainClips.getOrNull(curIdx)
                if (curClip != null) {
                    val playerPosSec = player.currentPosition / 1000f
                    val currentGlobalSec = curClip.startTime + playerPosSec
                    val speed = curClip.playbackRate.coerceAtLeast(0.1f)
                    val steppedTime = currentTimeState.value + (dtSec * speed)
                    val drift = currentGlobalSec - steppedTime
                    val convergedTime = if (kotlin.math.abs(drift) > 0.10f) {
                        currentGlobalSec
                    } else {
                        steppedTime + (drift * 0.22f)
                    }
                    viewModel.seekTo(convergedTime.coerceIn(0f, state.duration))
                }
            }
        }
    }

    // We update playback speed and volume whenever they change on the selected clip or active clip changes
    LaunchedEffect(state.clips, activeItemIndex) {
        val currentClip = mainClips.getOrNull(activeItemIndex)
        currentClip?.let {
            val pitch = if (it.preservePitch) 1.0f else it.playbackRate
            player.playbackParameters = androidx.media3.common.PlaybackParameters(it.playbackRate, pitch)
            player.volume = it.volume.coerceIn(0f, 1f)
        }
    }

    // ── Animate panel height (avoids weight-inside-AnimatedVisibility bug) ──

    // Remember the last clip so the panel can animate out gracefully
    var rememberedClip by remember { mutableStateOf<Clip?>(null) }
    SideEffect { if (selectedClip != null) rememberedClip = selectedClip }

    // Light theme overrides mapping to HTML prototype colors
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
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            ChromaScreenHeader(
                title = "Oscellion",
                badgeText = "Video Studio",
                badgeColor = Color(0xFF007AFF),
                showBackButton = true,
                onBack = onBack,
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Undo/Redo Pill
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
                                    contentDescription = "Export",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Text(
                                    text = "Export",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            )

            // ── Video Preview — 16:9 Aspect Ratio with Overlays ────────
            VideoPreviewArea(
                currentTimeState = currentTimeState,
                modifier     = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                player       = player,
                state        = state,
                selectedClip = selectedClip,
                isPlaying    = isPlaying,
                onTogglePlay = { if (player.isPlaying) player.pause() else player.play() },
                onSeek       = { viewModel.seekTo(it) },
                onTransformChange = { clipId, transform ->
                    viewModel.updateClipTransform(clipId, transform)
                }
            )

            // Quick Actions Panel
            QuickActionsPanel(
                onAddMedia = { showAddSheet = true },
                onLayers = { showLayersSheet = true },
                onSettings = { showSettingsSheet = true }
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Timeline fills remaining space + horizontal toolbar at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ChromaTimeline(
                    currentTimeState = currentTimeState,
                    modifier       = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(androidx.compose.ui.graphics.RectangleShape),
                    state          = state,
                    onSeek         = { viewModel.seekTo(it) },
                    onSelectClip   = { clipId ->
                        viewModel.selectClip(clipId)
                        val clip = state.clips.find { it.id == clipId }
                        if (clip != null && (currentTimeState.value < clip.startTime || currentTimeState.value > clip.endTime)) {
                            viewModel.seekTo(clip.startTime)
                        }
                    },
                    onDeselectClip = { viewModel.selectClip(null) },
                    onMoveClip     = { deltaX ->
                        val allSnapPoints = state.clips.filter { it.id != state.selectedClipId }.flatMap { listOf(it.startTime, it.endTime) } + currentTimeState.value
                        val deltaSeconds = deltaX / state.pixelsPerSecond
                        viewModel.moveSelectedClip(deltaSeconds, allSnapPoints)
                        val activeClip = state.clips.find { it.id == state.selectedClipId }
                        if (activeClip != null) {
                            val targetTime = if (currentTimeState.value >= activeClip.startTime && currentTimeState.value <= activeClip.endTime) {
                                currentTimeState.value
                            } else {
                                (activeClip.startTime).coerceAtLeast(0f)
                            }
                            viewModel.seekTo(targetTime)
                        }
                    },
                    onDragStart    = { viewModel.startDrag() },
                    onDragEnd      = { viewModel.endDrag() },
                    onZoom         = { viewModel.setZoom(it) },
                    onTransitionClick = { clip -> transitionTargetClip = clip }
                )

                HorizontalToolbar(
                    clip        = rememberedClip,
                    onOpenTool  = { activeToolSheet = it },
                    onSplit     = { viewModel.splitAtPlayhead() },
                    onDelete    = { viewModel.deleteSelected() },
                    onDuplicate = { viewModel.duplicateSelected() }
                )
            }
        }
    }

    if (showAddSheet) {
        AddMediaSheet(
            onDismiss  = { showAddSheet = false },
            onAddMedia = { type, name, track, color, uri ->
                viewModel.addClip(type, name, track, color, uri)
                showAddSheet = false
            }
        )
    }

    if (showSettingsSheet) {
        com.chroma.studio.ui.components.GlassBottomSheet(
            onDismissRequest = { showSettingsSheet = false }
        ) {
            ProjectSettingsPanel(
                state = state,
                onFitModeChange = { viewModel.setVideoFitMode(it) },
                onRatioChange   = { viewModel.setProjectRatio(it) }
            )
        }
    }

    if (showLayersSheet) {
        com.chroma.studio.ui.components.GlassBottomSheet(
            onDismissRequest = { showLayersSheet = false }
        ) {
            LayerManagerPanel(
                clips = state.clips,
                trackVisibility = state.trackVisibility,
                trackLock = state.trackLock,
                onMoveTrack = { from, to -> /* Handle Track Move */ },
                onToggleVisibility = { trackId -> viewModel.toggleTrackVisibility(trackId) },
                onToggleLock = { trackId -> viewModel.toggleTrackLock(trackId) }
            )
        }
    }

    if (activeToolSheet != null && selectedClip != null) {
        com.chroma.studio.ui.components.GlassBottomSheet(
            onDismissRequest = { activeToolSheet = null }
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                when (activeToolSheet) {
                    EditorTool.TRIM      -> TrimPanel(selectedClip, currentTimeState.value, viewModel)
                    EditorTool.FILTER    -> FilterPanel(selectedClip, viewModel)
                    EditorTool.ADJUST    -> AdjustmentsPanel(selectedClip, viewModel)
                    EditorTool.TRANSFORM -> TransformPanel(selectedClip, viewModel)
                    EditorTool.AUDIO     -> AudioPanel(selectedClip, viewModel)
                    EditorTool.TEXT      -> TextEditorPanel(selectedClip, viewModel)
                    EditorTool.SPEED     -> SpeedPanel(selectedClip, viewModel)
                    EditorTool.BLEND     -> BlendPanel(selectedClip, viewModel)
                    EditorTool.CHROMA_KEY-> ChromaKeyPanel(selectedClip, viewModel)
                    else -> {}
                }
            }
        }
    }

    if (transitionTargetClip != null) {
        com.chroma.studio.ui.components.GlassBottomSheet(
            onDismissRequest = { transitionTargetClip = null }
        ) {
            TransitionPickerSheet(
                currentTransition = transitionTargetClip?.transitionOut ?: TransitionType.NONE,
                onTransitionSelected = { transition ->
                    viewModel.updateClipTransition(transitionTargetClip!!.id, transition)
                    transitionTargetClip = null
                },
                onDismiss = { transitionTargetClip = null }
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// VIDEO PREVIEW AREA
// ═══════════════════════════════════════════════════════════════════════════════

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPreviewArea(
    modifier:     Modifier,
    currentTimeState: State<Float>,
    player:       ExoPlayer,
    state:        ProjectState,
    selectedClip: Clip?,
    isPlaying:    Boolean,
    onTogglePlay: () -> Unit,
    onSeek:       (Float) -> Unit,
    onTransformChange: (String, TransformState) -> Unit
) {
    val colors = LocalOscellionColors.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // The clip that actually feeds the visible video surface (track 1). Its filter and
    // adjustments are what the preview must reflect in real time.
    val mainClip = state.clips.find { it.type == MediaType.VIDEO && it.trackId == 1 }
    val activeTargetClip = selectedClip ?: mainClip ?: state.clips.firstOrNull()
    val activeTransform = activeTargetClip?.getInterpolatedTransform(currentTimeState.value) ?: TransformState()
    val activeScale = activeTransform.scale
    val activePanX = activeTransform.x
    val activePanY = activeTransform.y

    var showZoomCropSlider by remember { mutableStateOf(false) }
    var zoomSliderHideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val keepZoomSliderAlive: () -> Unit = {
        showZoomCropSlider = true
        zoomSliderHideJob?.cancel()
        zoomSliderHideJob = scope.launch {
            delay(2000L)
            showZoomCropSlider = false
        }
    }

    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var pendingTapJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val videoColorMatrixState = remember(mainClip?.filter, mainClip?.adjustments) {
        mutableStateOf(buildPreviewColorMatrix(mainClip?.filter ?: FilterType.NONE, mainClip?.adjustments ?: Adjustments()))
    }
    val supportsRenderEffect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = modifier
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // ExoPlayer — filter/adjustments are applied here via RenderEffect (Android 12+).
        // On older OS versions the color matrix can't be applied to a native video
        // surface, so the badge + timeline still reflect the selection but the live
        // pixels won't be recolored on those devices.
        AndroidView(
            factory = { ctx ->
                val aspectRatioLayout = AspectRatioFrameLayout(ctx).apply {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                val textureView = android.view.TextureView(ctx)
                val layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                ).apply { gravity = android.view.Gravity.CENTER }
                
                aspectRatioLayout.addView(textureView, layoutParams)
                player.setVideoTextureView(textureView)
                
                player.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                        if (videoSize.height > 0 && videoSize.width > 0) {
                            val ratio = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height
                            aspectRatioLayout.setAspectRatio(ratio)
                        }
                    }
                })
                aspectRatioLayout
            },
            update  = { view -> 
                // The factory already bound the player to the texture view.
            },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (supportsRenderEffect) {
                        Modifier.graphicsLayer {
                            val ct = mainClip?.getInterpolatedTransform(currentTimeState.value) ?: TransformState()
                            scaleX = ct.scale
                            scaleY = ct.scale
                            rotationZ = ct.rotation
                            translationX = ct.x.dp.toPx()
                            translationY = ct.y.dp.toPx()
                            alpha = ct.opacity
                            renderEffect = android.graphics.RenderEffect
                                .createColorFilterEffect(android.graphics.ColorMatrixColorFilter(videoColorMatrixState.value))
                                .asComposeRenderEffect()
                        }
                    } else Modifier.graphicsLayer {
                        val ct = mainClip?.getInterpolatedTransform(currentTimeState.value) ?: TransformState()
                        scaleX = ct.scale
                        scaleY = ct.scale
                        rotationZ = ct.rotation
                        translationX = ct.x.dp.toPx()
                        translationY = ct.y.dp.toPx()
                        alpha = ct.opacity
                    }
                )
        )

        // Active overlays sorted by Z-index (trackId)
        val activeOverlays by remember(state.clips, state.trackVisibility) {
            androidx.compose.runtime.derivedStateOf {
                state.clips.filter {
                    it.type != MediaType.VIDEO &&
                            it.uri != null &&
                            (state.trackVisibility[it.trackId] != false) &&
                            currentTimeState.value >= it.startTime &&
                            currentTimeState.value <= it.endTime
                }.sortedBy { it.trackId }
            }
        }
        
        activeOverlays.forEach { clip ->
            val isMedia = clip.type == MediaType.IMAGE || clip.type == MediaType.STICKER
            val currentTransform = clip.getInterpolatedTransform(currentTimeState.value)
            
            if (isMedia) {
                val clipMatrix = remember(clip.filter, clip.adjustments) {
                    buildPreviewColorMatrix(clip.filter, clip.adjustments).toComposeColorMatrix()
                }
                AsyncImage(
                    model              = clip.uri,
                    contentDescription = clip.name,
                    colorFilter        = ColorFilter.colorMatrix(clipMatrix),
                    modifier = Modifier
                        .offset(x = currentTransform.x.dp, y = currentTransform.y.dp)
                        .graphicsLayer(
                            scaleX    = currentTransform.scale,
                            scaleY    = currentTransform.scale,
                            rotationZ = currentTransform.rotation,
                            alpha     = currentTransform.opacity
                        )
                        .widthIn(max = 240.dp)
                )
            } else if (clip.type == MediaType.TEXT) {
                Box(
                    modifier = Modifier
                        .offset(x = currentTransform.x.dp, y = currentTransform.y.dp)
                        .graphicsLayer(
                            scaleX    = currentTransform.scale,
                            scaleY    = currentTransform.scale,
                            rotationZ = currentTransform.rotation,
                            alpha     = currentTransform.opacity
                        )
                ) {
                    Text(
                        text       = clip.text,
                        color      = clip.textColor,
                        fontSize   = clip.textSize.sp,
                        fontWeight = if (clip.textBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle  = if (clip.textItalic) FontStyle.Italic else FontStyle.Normal
                    )
                }
            }
        }

        // Top HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("1080p • 30fps", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .clickable { /* Toggle Fullscreen */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(Lucide.Maximize, contentDescription = "Fullscreen", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        // ══════════════════════════════════════════════════════════
        // UNIFIED TOUCH GESTURE OVERLAY (Zoom, Pan/Crop & Playback Tap)
        // ══════════════════════════════════════════════════════════
        if (state.clips.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var pan = Offset.Zero
                            var pastTouchSlop = false
                            val touchSlop = viewConfiguration.touchSlop
                            var isMultiTouch = false
                            var gestureConsumed = false
                            var event: PointerEvent

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

                                if (pointerCount >= 2) {
                                    keepZoomSliderAlive()
                                    val target = activeTargetClip
                                    if (target != null) {
                                        val curT = target.transform
                                        val newScale = (curT.scale * zoomChange).coerceIn(0.5f, 5.0f)
                                        val dxDp = panChange.x / density.density
                                        val dyDp = panChange.y / density.density
                                        val maxPanX = kotlin.math.max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                        val maxPanY = kotlin.math.max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                        val newPanX = if (newScale <= 1.02f) 0f else (curT.x + dxDp).coerceIn(-maxPanX, maxPanX)
                                        val newPanY = if (newScale <= 1.02f) 0f else (curT.y + dyDp).coerceIn(-maxPanY, maxPanY)

                                        onTransformChange(
                                            target.id,
                                            curT.copy(scale = newScale, x = newPanX, y = newPanY)
                                        )
                                        gestureConsumed = true
                                    }
                                    event.changes.forEach {
                                        if (it.positionChange() != Offset.Zero) {
                                            it.consume()
                                        }
                                    }
                                } else if (pointerCount == 1) {
                                    val target = activeTargetClip
                                    val isZoomed = (target?.transform?.scale ?: 1f) > 1.05f

                                    pan += panChange
                                    if (!pastTouchSlop) {
                                        if (pan.getDistance() > touchSlop) {
                                            pastTouchSlop = true
                                        }
                                    }

                                    if (pastTouchSlop && isZoomed && target != null) {
                                        keepZoomSliderAlive()
                                        val curT = target.transform
                                        val dxDp = panChange.x / density.density
                                        val dyDp = panChange.y / density.density
                                        val maxPanX = kotlin.math.max(100f, (curT.scale - 0.7f).coerceAtLeast(0f) * 200f)
                                        val maxPanY = kotlin.math.max(100f, (curT.scale - 0.7f).coerceAtLeast(0f) * 200f)
                                        val newPanX = (curT.x + dxDp).coerceIn(-maxPanX, maxPanX)
                                        val newPanY = (curT.y + dyDp).coerceIn(-maxPanY, maxPanY)

                                        onTransformChange(
                                            target.id,
                                            curT.copy(x = newPanX, y = newPanY)
                                        )
                                        gestureConsumed = true
                                        event.changes.forEach {
                                            if (it.positionChange() != Offset.Zero) {
                                                it.consume()
                                            }
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            if (isMultiTouch) {
                                keepZoomSliderAlive()
                            }

                            // Tap handling when not dragging or pinching
                            if (!pastTouchSlop && !isMultiTouch && !gestureConsumed) {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTimestamp < 300L) {
                                    // Double tap: toggle 1.0x Fit vs 1.35x Fill/Crop
                                    pendingTapJob?.cancel()
                                    pendingTapJob = null
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
                                    // Single tap: toggle Play / Pause after short debounce
                                    lastTapTimestamp = now
                                    pendingTapJob?.cancel()
                                    pendingTapJob = scope.launch {
                                        delay(260L)
                                        onTogglePlay()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            }
                        }
                    }
            )
        }

        // Center Big Play Button (Visible when paused)
        if (!isPlaying) {
            Icon(
                Lucide.Play,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onTogglePlay)
                    .zIndex(25f)
            )
        }

        // Floating Zoom & Crop Smooth Slider Bar (Revealed by 2-finger zoom, auto-fades after 2s of inactivity)
        androidx.compose.animation.AnimatedVisibility(
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
                    // Header Row: Icon, Title, Multiplier badge, and Reset button
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

                    // Slider Row: ZoomOut Icon, Smooth Slider, ZoomIn Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Zoom Out Step Button
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
                                    val maxPanX = kotlin.math.max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                    val maxPanY = kotlin.math.max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
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
                                val maxPanX = kotlin.math.max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                val maxPanY = kotlin.math.max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                val newPanX = if (newScale <= 1.02f) 0f else curT.x.coerceIn(-maxPanX, maxPanX)
                                val newPanY = if (newScale <= 1.02f) 0f else curT.y.coerceIn(-maxPanY, maxPanY)
                                onTransformChange(
                                    target.id,
                                    curT.copy(scale = newScale, x = newPanX, y = newPanY)
                                )
                            },
                            onValueChangeFinished = {
                                keepZoomSliderAlive()
                            },
                            valueRange = 0.5f..4.0f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF0A84FF),
                                activeTrackColor = Color(0xFF0A84FF),
                                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                            )
                        )

                        // Zoom In Step Button
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
                                    val maxPanX = kotlin.math.max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
                                    val maxPanY = kotlin.math.max(100f, (newScale - 0.7f).coerceAtLeast(0f) * 200f)
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

private fun formatTime(seconds: Float): String {
    val totalSec = seconds.toInt()
    val min      = totalSec / 60
    val sec      = totalSec % 60
    val frame    = ((seconds - totalSec) * 30).toInt()
    return "%d:%02d.%02d".format(min, sec, frame)
}

// ═══════════════════════════════════════════════════════════════════════════════
// TIMELINE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChromaTimeline(
    modifier:       Modifier,
    currentTimeState: State<Float>,
    state:          ProjectState,
    onSeek:         (Float) -> Unit,
    onSelectClip:   (String) -> Unit,
    onDeselectClip: () -> Unit,
    onMoveClip:     (Float) -> Unit,
    onDragStart:    () -> Unit,
    onDragEnd:      () -> Unit,
    onZoom:         (Float) -> Unit,
    onTransitionClick: (Clip) -> Unit,
    onToggleVisibility: (Int) -> Unit = {},
    onToggleLock: (Int) -> Unit = {}
) {
    val colors         = LocalOscellionColors.current
    val pps            = state.pixelsPerSecond
    val trackGutterDp  = 110.dp
    val trackHeightDp  = 44.dp
    val scrollState    = rememberScrollState()

    val verticalScrollState = rememberScrollState()
    val totalHeightDp = (trackHeightDp + 4.dp) * 5 + 16.dp

    // Track colors matching the HTML prototype
    val trackColors = listOf(
        Color(0xFF3B82F6), // Video (Blue)
        Color(0xFFA855F7), // B-Roll (Purple)
        Color(0xFFFCD34D), // Text (Yellow)
        Color(0xFF22C55E), // Audio (Green)
        Color(0xFFEC4899)  // Sticker (Pink)
    )
    val trackLabels = listOf("Video", "B-Roll", "Text", "Audio", "Stickers")

    val hazeState = com.chroma.studio.ui.components.LocalHazeState.current
    Column(
        modifier = modifier
            .hazeChild(state = hazeState, shape = androidx.compose.ui.graphics.RectangleShape)
            .background(colors.bg.copy(alpha = 0.5f))
    ) {
        // Zoom row
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Lucide.ZoomIn, contentDescription = "Zoom", tint = colors.textMuted, modifier = Modifier.size(12.dp))
            AppleSlider(
                value         = pps,
                onValueChange = onZoom,
                valueRange    = 10f..300f,
                modifier      = Modifier.weight(1f)
            )
            Text("${String.format("%.1f", pps / 60f)}×", color = colors.textMuted, fontSize = 9.sp)
        }

        // Track area
        Row(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(verticalScrollState)) {

            // Left gutter (track labels — non-scrollable)
            Column(
                modifier = Modifier
                    .width(trackGutterDp)
                    .background(colors.bg)
                    .padding(top = 16.dp, start = 12.dp, end = 8.dp),  // ruler height offset + side padding
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val trackIcons = listOf(Lucide.Play, Lucide.Image, Lucide.Type, Lucide.Volume2, Lucide.Sticker)
                (0..4).forEach { i ->
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(trackColors[i]),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(trackIcons[i], contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
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
                            val trackId = i + 1
                            val isVisible = state.trackVisibility[trackId] ?: true
                            val isLocked = state.trackLock[trackId] ?: false
                            Icon(
                                if (isVisible) Lucide.Eye else Lucide.EyeOff,
                                contentDescription = "Toggle Visibility",
                                tint = if (isVisible) Color(0xFF9CA3AF) else Color(0xFFD1D5DB),
                                modifier = Modifier.size(12.dp).clickable { onToggleVisibility(trackId) }
                            )
                            Icon(
                                if (isLocked) Lucide.Lock else Lucide.LockOpen,
                                contentDescription = "Toggle Lock",
                                tint = if (isLocked) Color(0xFF9CA3AF) else Color(0xFFD1D5DB),
                                modifier = Modifier.size(12.dp).clickable { onToggleLock(trackId) }
                            )
                        }
                    }
                }
            }

            // Scrollable track content
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(totalHeightDp)
            ) {
                val halfWidthDp = maxWidth / 2

                val transformableState = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
                    if (zoomChange != 1f) {
                        onZoom(pps * zoomChange)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(state = transformableState)
                        .horizontalScroll(scrollState)
                        .background(Color.Transparent)
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick           = onDeselectClip
                        )
                ) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    
                    // Update current time based on scroll position IF scrolling
                    LaunchedEffect(scrollState.value) {
                        if (scrollState.isScrollInProgress) {
                            val scrollDp = with(density) { scrollState.value.toDp() }
                            onSeek((scrollDp.value / pps).coerceAtLeast(0f).coerceAtMost(state.duration))
                        }
                    }

                    // Auto-scroll when playing, seeking externally, or zooming without continuous coroutine restarts
                    LaunchedEffect(pps) {
                        snapshotFlow { currentTimeState.value }
                            .collect { time ->
                                if (!scrollState.isScrollInProgress) {
                                    val targetPx = with(density) { (time * pps).dp.toPx() }.toInt()
                                    if (kotlin.math.abs(scrollState.value - targetPx) > 1) {
                                        scrollState.scrollTo(targetPx)
                                    }
                                }
                            }
                    }

                    val totalWidthDp = (state.duration * pps).dp + halfWidthDp * 2

                    Column(
                        modifier = Modifier
                            .width(totalWidthDp)
                            .padding(horizontal = halfWidthDp)
                            .fillMaxHeight()
                    ) {
                    // Time ruler
                    TimeRuler(
                        modifier = Modifier.fillMaxWidth().height(16.dp),
                        duration = state.duration,
                        pps      = pps,
                        onSeek   = onSeek
                    )

                    // 5 track rows
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { trackId ->
                            TimelineTrackRow(
                                modifier       = Modifier.fillMaxWidth().height(trackHeightDp),
                                trackColor     = trackColors[trackId - 1],
                                clips          = state.clips.filter { it.trackId == trackId },
                                selectedClipId = state.selectedClipId,
                                pps            = pps,
                                onClipTap      = onSelectClip,
                                onClipDrag     = onMoveClip,
                                onDragStart    = onDragStart,
                                onDragEnd      = onDragEnd,
                                onTransitionClick = onTransitionClick
                            )
                        }
                    }
                }
            }

            // Fixed Red playhead line
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFFF3B30).copy(alpha = 0.8f))
                )
                // Fixed Playhead head (triangle-like box)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 12.dp)
                        .size(10.dp, 10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFFF3B30).copy(alpha = 0.8f))
                )
            }
        }
    }
}

@Composable
private fun TimeRuler(
    modifier: Modifier,
    duration: Float,
    pps:      Float,
    onSeek:   (Float) -> Unit
) {
    val colors = LocalOscellionColors.current
    Canvas(
        modifier = modifier
            .background(colors.glassBg.copy(alpha = 0.3f))
            .pointerInput(pps, "tap") {
                detectTapGestures { offset ->
                    val pxPerSec = pps.dp.toPx()
                    onSeek((offset.x / pxPerSec).coerceAtLeast(0f))
                }
            }
    ) {
        val stepSec: Float = when {
            pps < 15f  -> 10f
            pps < 40f  -> 5f
            pps < 80f  -> 2f
            pps < 160f -> 1f
            else       -> 0.5f
        }
        val pxPerSec = pps.dp.toPx()
        var t = 0f
        while (t <= duration + 1f) {
            val x = t * pxPerSec
            if (x > size.width) break
            drawLine(
                color       = colors.textMuted,
                start       = Offset(x, 0f),
                end         = Offset(x, size.height),
                strokeWidth = 1f
            )
            t += stepSec
        }
    }
}

@Composable
private fun TimelineTrackRow(
    modifier:       Modifier,
    trackColor:     Color,
    clips:          List<Clip>,
    selectedClipId: String?,
    pps:            Float,
    onClipTap:      (String) -> Unit,
    onClipDrag:     (Float) -> Unit,
    onDragStart:    () -> Unit,
    onDragEnd:      () -> Unit,
    onTransitionClick: (Clip) -> Unit
) {
    val colors = LocalOscellionColors.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val currentViewConfig = LocalViewConfiguration.current
    val customViewConfig = remember(currentViewConfig) {
        object : ViewConfiguration by currentViewConfig {
            override val longPressTimeoutMillis: Long
                get() = 1000L // 1s minimum
        }
    }
    
    androidx.compose.runtime.CompositionLocalProvider(LocalViewConfiguration provides customViewConfig) {
        Box(
        modifier = modifier
            .background(colors.bg.copy(alpha = 0.2f))
            .border(BorderStroke(0.5.dp, colors.glassBorder.copy(alpha = 0.2f)))
    ) {
        clips.forEach { clip ->
            val leftDp   = (clip.startTime * pps).dp
            val widthDp  = (clip.duration * pps).coerceAtLeast(4f).dp
            val selected = clip.id == selectedClipId

            Box(
                modifier = Modifier
                    .offset(x = leftDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp, horizontal = 1.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(trackColor)
                    .then(
                        if (selected)
                            Modifier.border(1.5.dp, Color.White, RoundedCornerShape(5.dp))
                        else
                            Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
                    )
                    .pointerInput(clip.id) {
                        detectTapGestures(onTap = { onClipTap(clip.id) })
                    }
                    .pointerInput(clip.id + "_d") {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                onClipTap(clip.id)
                                // Trigger vibration
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onDragStart() 
                            },
                            onDragEnd   = { onDragEnd() },
                            onDrag      = { change, drag ->
                                change.consume()
                                if (clip.id == selectedClipId) onClipDrag(drag.x / density)
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                if (clip.type == MediaType.VIDEO || clip.type == MediaType.AUDIO) {
                    WaveformOverlay(clip, Modifier.fillMaxSize().padding(horizontal = 4.dp))
                }
                
                Text(
                    text       = when (clip.type) {
                        MediaType.TEXT -> clip.text.take(12)
                        else           -> clip.name
                    },
                    color      = Color.White.copy(alpha = 0.92f),
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.padding(horizontal = 5.dp)
                )
            }

            // Transition Badge (+ icon in the middle of two clips)
            val nextClip = clips.find { kotlin.math.abs(it.startTime - clip.endTime) < 0.1f }
            if (nextClip != null) {
                val hasTransition = clip.transitionOut != TransitionType.NONE
                Icon(
                    imageVector = if (hasTransition) Lucide.Sparkles else Lucide.Plus,
                    contentDescription = "Transition",
                    tint = if (hasTransition) Color(0xFF2563EB) else Color.White,
                    modifier = Modifier
                        .offset(x = (clip.endTime * pps).dp - 9.dp)
                        .size(18.dp)
                        .clickable { onTransitionClick(clip) }
                )
            }
        }
    }
    }
}

@Composable
private fun WaveformOverlay(clip: Clip, modifier: Modifier) {
    val colors = LocalOscellionColors.current
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barWidth = 3.dp.toPx()
        val spacing = 1.dp.toPx()
        val totalBars = (w / (barWidth + spacing)).toInt().coerceAtLeast(1)
        
        val seed = clip.id.hashCode()
        var randomValue = seed
        
        for (i in 0 until totalBars) {
            randomValue = (randomValue * 31 + 17)
            val n = Math.abs(randomValue % 100) / 100f
            
            val timeRatio = i.toFloat() / totalBars
            val clipTime = timeRatio * clip.duration
            var fadeMultiplier = 1f
            if (clip.audioFadeIn > 0f && clipTime < clip.audioFadeIn) {
                fadeMultiplier = clipTime / clip.audioFadeIn
            } else if (clip.audioFadeOut > 0f && clipTime > clip.duration - clip.audioFadeOut) {
                fadeMultiplier = (clip.duration - clipTime) / clip.audioFadeOut
            }
            
            val barHeight = h * 0.15f + (h * 0.6f * n * clip.volume * fadeMultiplier)
            val x = i * (barWidth + spacing)
            val y = (h - barHeight) / 2
            
            drawRect(
                color = colors.onPrimary.copy(alpha = 0.3f),
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BOTTOM TOOL PANEL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EditorBottomPanel(
    modifier:     Modifier,
    clip:         Clip,
    selectedTool: EditorTool,
    onToolSelect: (EditorTool) -> Unit,
    onSplit:      () -> Unit,
    onDelete:     () -> Unit,
    onDuplicate:  () -> Unit,
    viewModel:    ChromaEditorViewModel,
    currentTime:  Float
) {
    val colors = LocalOscellionColors.current

    val hazeState = com.chroma.studio.ui.components.LocalHazeState.current
    Column(
        modifier = modifier
            .hazeChild(state = hazeState, shape = androidx.compose.ui.graphics.RectangleShape)
            .background(colors.glassBgHover.copy(alpha = 0.5f))
            .navigationBarsPadding()
    ) {
        // Thin accent line at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, colors.primary.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        // Clip info + action chips
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Clip type dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(clip.color)
            )
            Text(
                text     = clip.name.ifBlank { clip.type.name.lowercase().replaceFirstChar { it.uppercase() } },
                color    = colors.textMuted,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            EditorActionChip(Lucide.Scissors, "Split",     onSplit,     tint = colors.textMuted)
            EditorActionChip(Lucide.Copy,     "Dup",       onDuplicate, tint = colors.textMuted)
            EditorActionChip(Lucide.Trash2,   "Delete",    onDelete,    tint = colors.error)
        }

        // Tool tab pills
        val availableTools = buildList {
            if (clip.type != MediaType.AUDIO) add(EditorTool.TRIM)
            add(EditorTool.FILTER)
            if (clip.type != MediaType.AUDIO && clip.type != MediaType.TEXT) add(EditorTool.ADJUST)
            if (clip.type != MediaType.TEXT) add(EditorTool.TRANSFORM)
            if (clip.type == MediaType.TEXT) add(EditorTool.TEXT)
            if (clip.type == MediaType.VIDEO || clip.type == MediaType.AUDIO) add(EditorTool.AUDIO)
            if (clip.type == MediaType.VIDEO) add(EditorTool.SPEED)
            if (clip.type == MediaType.VIDEO || clip.type == MediaType.IMAGE) add(EditorTool.BLEND)
            if (clip.type == MediaType.VIDEO || clip.type == MediaType.IMAGE) add(EditorTool.CHROMA_KEY)
        }

        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            availableTools.forEach { tool ->
                val selected = tool == selectedTool
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) Brush.linearGradient(listOf(colors.primary.copy(alpha=0.8f), colors.primary.copy(alpha = 0.6f)))
                            else Brush.linearGradient(listOf(colors.glassBg.copy(alpha=0.3f), colors.glassBg.copy(alpha=0.3f)))
                        )
                        .border(
                            width  = if (selected) 0.dp else 0.5.dp,
                            color  = if (selected) Color.Transparent else colors.glassBorder,
                            shape  = RoundedCornerShape(14.dp)
                        )
                        .clickable { onToolSelect(tool) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = tool.label,
                        color      = if (selected) colors.onPrimary else colors.textMuted,
                        fontSize   = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tool content (animated crossfade)
        AnimatedContent(
            targetState   = selectedTool,
            modifier      = Modifier.fillMaxWidth().weight(1f),
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
            label         = "toolContent"
        ) { tool ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (tool) {
                    EditorTool.TRIM      -> TrimPanel(clip, currentTime, viewModel)
                    EditorTool.FILTER    -> FilterPanel(clip, viewModel)
                    EditorTool.ADJUST    -> AdjustmentsPanel(clip, viewModel)
                    EditorTool.TRANSFORM -> TransformPanel(clip, viewModel)
                    EditorTool.AUDIO     -> AudioPanel(clip, viewModel)
                    EditorTool.TEXT      -> TextEditorPanel(clip, viewModel)
                    EditorTool.SPEED     -> SpeedPanel(clip, viewModel)
                    EditorTool.BLEND     -> BlendPanel(clip, viewModel)
                    EditorTool.CHROMA_KEY-> ChromaKeyPanel(clip, viewModel)
                }
            }
        }
    }
}

@Composable
private fun EditorActionChip(
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    label:   String,
    onClick: () -> Unit,
    tint:    Color? = null
) {
    val colors = LocalOscellionColors.current
    val actualTint = tint ?: colors.textMuted
    Row(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(colors.glassBg.copy(alpha = 0.3f))
            .border(0.5.dp, colors.glassBorder.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = actualTint, modifier = Modifier.size(11.dp))
        Text(label, color = actualTint, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TRIM PANEL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TrimPanel(clip: Clip, currentTime: Float, viewModel: ChromaEditorViewModel) {
    val colors = LocalOscellionColors.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val clipStart = clip.startTime
    val clipEnd   = clip.endTime
    val clipDur   = clip.duration
    val playhead  = currentTime

    val playheadInClip = playhead > clipStart && playhead < clipEnd
    val leftRatio  = if (clipDur > 0f) ((playhead - clipStart) / clipDur).coerceIn(0f, 1f) else 0f

    fun fmt(s: Float): String {
        val t = s.toInt(); return "%d:%02d".format(t / 60, t % 60)
    }
    fun fmtS(s: Float) = String.format("%.2f", s) + "s"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Trim", color = colors.textMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Playhead: ${fmt(playhead)}",
                color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Clip Preview", color = colors.textMuted, fontSize = 11.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3F4F6))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(2.dp)
                        .clip(RoundedCornerShape(8.dp)).background(Color(0xFF3B82F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(clip.name, color = Color.White, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp))
                }
                if (playheadInClip) {
                    Box(
                        modifier = Modifier.fillMaxWidth(leftRatio).fillMaxHeight().padding(2.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                            .background(Color.Black.copy(alpha = 0.38f))
                    )
                    Box(modifier = Modifier.fillMaxWidth(leftRatio).fillMaxHeight().wrapContentWidth(Alignment.End)) {
                        Box(modifier = Modifier.width(2.5.dp).fillMaxHeight().background(Color(0xFFFF3B30)))
                    }
                }
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(fmt(clipStart), color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp)
                    Text(fmt(clipEnd), color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Duration: ${fmtS(clipDur)}", color = colors.textMuted, fontSize = 10.sp)
                if (playheadInClip)
                    Text("Left: ${fmtS(playhead - clipStart)}  |  Right: ${fmtS(clipEnd - playhead)}", color = colors.textMuted, fontSize = 10.sp)
                else
                    Text("Move playhead onto the clip", color = Color(0xFFFF3B30), fontSize = 10.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Trim Operations", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

            TrimActionButton(
                icon = Lucide.PanelLeftClose, title = "Left Cut",
                description = if (playheadInClip) "Remove ${fmtS(playhead - clipStart)} from start" else "Playhead not on clip",
                color = Color(0xFF3B82F6), enabled = playheadInClip
            ) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); viewModel.trimStart(playhead) }

            TrimActionButton(
                icon = Lucide.SquareSplitHorizontal, title = "Split Here",
                description = "Split clip into two parts at playhead",
                color = Color(0xFFFF9500), enabled = playheadInClip
            ) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); viewModel.splitAtPlayhead() }

            TrimActionButton(
                icon = Lucide.PanelRightClose, title = "Right Cut",
                description = if (playheadInClip) "Remove ${fmtS(clipEnd - playhead)} from end" else "Playhead not on clip",
                color = Color(0xFFFF3B30), enabled = playheadInClip
            ) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); viewModel.trimEnd(playhead) }
        }
    }
}

@Composable
private fun TrimActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.5.dp, if (enabled) color.copy(alpha = 0.3f) else Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                .background(if (enabled) color else Color(0xFFE5E7EB)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (enabled) Color(0xFF1C1C1E) else Color(0xFF9CA3AF), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Color(0xFF8E8E93), fontSize = 11.sp)
        }
        if (enabled) {
            Icon(Lucide.ChevronRight, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FilterPanel(clip: Clip, viewModel: ChromaEditorViewModel) {
    val colors  = LocalOscellionColors.current
    val filters = FilterType.values().toList()

    // Each filter has a representative gradient that visually simulates the effect
    val filterGradients: Map<FilterType, List<Color>> = mapOf(
        FilterType.NONE      to listOf(Color(0xFF252525), Color(0xFF3D3D3D)),
        FilterType.GRAYSCALE to listOf(Color(0xFF606060), Color(0xFF909090)),
        FilterType.SEPIA     to listOf(Color(0xFF704214), Color(0xFFB8884A)),
        FilterType.INVERT    to listOf(Color(0xFF001AFF), Color(0xFFFF1493)),
        FilterType.WARM      to listOf(Color(0xFFFF6B35), Color(0xFFFFD700)),
        FilterType.COOL      to listOf(Color(0xFF0066CC), Color(0xFF00CCFF)),
        FilterType.CINEMATIC to listOf(Color(0xFF0D1B2A), Color(0xFF1B4332)),
        FilterType.FADE      to listOf(Color(0xFFB0998A), Color(0xFFD4BFB0)),
        FilterType.VIVID     to listOf(Color(0xFFFF0080), Color(0xFF00FFCC)),
        FilterType.MATTE     to listOf(Color(0xFF7A6855), Color(0xFF9B8B72)),
        FilterType.NEON      to listOf(Color(0xFF00FF41), Color(0xFF0066FF)),
        FilterType.SUNSET    to listOf(Color(0xFFFF4E00), Color(0xFF990000)),
        FilterType.NOIR      to listOf(colors.glassBg, colors.glassBorder)
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            "  Filter",
            color      = colors.textMuted,
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filters.forEach { filter ->
                val selected   = clip.filter == filter
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
                                if (selected)
                                    Modifier.border(2.dp, colors.primary, RoundedCornerShape(10.dp))
                                else
                                    Modifier.border(0.5.dp, colors.glassBorder, RoundedCornerShape(10.dp))
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
                        text       = filter.label,
                        color      = if (selected) colors.primary else colors.textMuted,
                        fontSize   = 9.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ADJUSTMENTS PANEL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AdjustmentsPanel(clip: Clip, viewModel: ChromaEditorViewModel) {
    val adj = clip.adjustments

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        EditorSlider("Brightness", adj.brightness, -1f,   1f)   { viewModel.setAdjustments(adj.copy(brightness = it)) }
        EditorSlider("Contrast",   adj.contrast,    0f,   2f)   { viewModel.setAdjustments(adj.copy(contrast   = it)) }
        EditorSlider("Saturation", adj.saturation,  0f,   3f)   { viewModel.setAdjustments(adj.copy(saturation = it)) }
        EditorSlider("Hue",        adj.hue,          0f, 360f)  { viewModel.setAdjustments(adj.copy(hue        = it)) }
        EditorSlider("Exposure",   adj.exposure,    -2f,   2f)  { viewModel.setAdjustments(adj.copy(exposure   = it)) }
        EditorSlider("Highlights", adj.highlights,  -1f,   1f)  { viewModel.setAdjustments(adj.copy(highlights = it)) }
        EditorSlider("Shadows",    adj.shadows,     -1f,   1f)  { viewModel.setAdjustments(adj.copy(shadows    = it)) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TRANSFORM PANEL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TransformPanel(clip: Clip, viewModel: ChromaEditorViewModel) {
    val t = clip.transform

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        EditorSlider("Scale",   t.scale,    0.1f,   3f)   { viewModel.setTransform(t.copy(scale    = it)) }
        EditorSlider("X Pos",   t.x,       -500f, 500f)   { viewModel.setTransform(t.copy(x        = it)) }
        EditorSlider("Y Pos",   t.y,       -500f, 500f)   { viewModel.setTransform(t.copy(y        = it)) }
        EditorSlider("Rotate",  t.rotation,-180f, 180f)   { viewModel.setTransform(t.copy(rotation = it)) }
        EditorSlider("Opacity", t.opacity,   0f,   1f)    { viewModel.setTransform(t.copy(opacity  = it)) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// AUDIO PANEL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AudioPanel(clip: Clip, viewModel: ChromaEditorViewModel) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text("Volume", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Lucide.Volume2, contentDescription = "Volume", tint = colors.textMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            AppleSlider(
                value = clip.volume,
                onValueChange = { viewModel.updateClipVolume(clip.id, it) },
                valueRange = 0f..2f,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text("${(clip.volume * 100).toInt()}%", color = colors.primary, fontSize = 12.sp, modifier = Modifier.width(40.dp))
        }

        Spacer(Modifier.height(16.dp))
        
        Text("Fade In (s)", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        AppleSlider(
            value = clip.audioFadeIn,
            onValueChange = { viewModel.updateClipAudioFadeIn(clip.id, it) },
            valueRange = 0f..5f,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(8.dp))
        Text("Fade Out (s)", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        AppleSlider(
            value = clip.audioFadeOut,
            onValueChange = { viewModel.updateClipAudioFadeOut(clip.id, it) },
            valueRange = 0f..5f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TEXT EDITOR PANEL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TextEditorPanel(clip: Clip, viewModel: ChromaEditorViewModel) {
    val colors = LocalOscellionColors.current

    val textSwatches = listOf(
        Color.White,          Color.Black,
        Color(0xFFFF3B30),    Color(0xFFFF9500),
        Color(0xFFFFCC00),    Color(0xFF34C759),
        Color(0xFF007AFF),    Color(0xFFAF52DE),
        Color(0xFFFF2D55),    Color(0xFF5AC8FA),
        Color(0xFFFF6B35),    Color(0xFF10B981)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value         = clip.text,
            onValueChange = { viewModel.setText(it) },
            label         = { Text("Text Content", color = colors.textMuted, fontSize = 11.sp) },
            modifier      = Modifier.fillMaxWidth(),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = colors.primary,
                unfocusedBorderColor = colors.glassBorder,
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                cursorColor          = colors.primary
            ),
            maxLines = 3
        )

        Spacer(Modifier.height(10.dp))
        EditorSlider("Size", clip.textSize, 12f, 120f) { viewModel.setTextSize(it) }
        Spacer(Modifier.height(6.dp))

        // Bold / Italic
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleChip("Bold",   clip.textBold,   { viewModel.setTextBold(!clip.textBold) },   Modifier.weight(1f))
            ToggleChip("Italic", clip.textItalic, { viewModel.setTextItalic(!clip.textItalic) }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))
        Text("Color", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
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
                            else     Modifier.border(0.5.dp, Color(0xFF333333), CircleShape)
                        )
                        .clickable { viewModel.setTextColor(swatchColor) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
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
                            else     Modifier.border(0.5.dp, Color(0xFF333333), CircleShape)
                        )
                        .clickable { viewModel.setTextColor(swatchColor) }
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text("Animation", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TextAnimation.values().forEach { anim ->
                val selected = clip.textAnimation == anim
                val label = anim.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) colors.primary.copy(alpha=0.8f) else colors.glassBg.copy(alpha = 0.3f))
                        .clickable { viewModel.updateClipTextAnimation(clip.id, anim) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (selected) colors.onPrimary else colors.textMain, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalOscellionColors.current
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.primary.copy(alpha=0.8f) else colors.glassBg.copy(alpha = 0.3f))
            .border(
                0.5.dp,
                if (selected) colors.primary else colors.glassBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            color      = if (selected) colors.onPrimary else colors.textMuted,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SPEED PANEL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SpeedPanel(clip: Clip, viewModel: ChromaEditorViewModel) {
    val colors      = LocalOscellionColors.current
    val speedValues = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f, 4.0f)
    val speedLabels = listOf("0.25×", "0.5×", "1×", "1.5×", "2×", "3×", "4×")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Speed", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${String.format("%.2f", clip.playbackRate)}×",
                color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            speedValues.forEachIndexed { i, speed ->
                val selected = abs(clip.playbackRate - speed) < 0.01f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
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
                        .clickable { viewModel.setSpeed(speed) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        speedLabels[i],
                        color      = if (selected) colors.onPrimary else colors.textMuted,
                        fontSize   = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))

        Text("Speed Curve", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpeedCurve.values().forEach { curve ->
                val selected = clip.speedCurve == curve
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) colors.primary.copy(alpha=0.8f) else colors.glassBg.copy(alpha = 0.3f))
                        .clickable { viewModel.updateClipSpeedCurve(clip.id, curve) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(curve.label, color = if (selected) colors.onPrimary else colors.textMain, fontSize = 10.sp)
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Preserve Pitch", color = colors.textMain, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            AppleSwitch(
                checked = clip.preservePitch,
                onCheckedChange = { viewModel.updateClipPreservePitch(clip.id, it) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BLEND MODE PANEL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun BlendPanel(clip: Clip, viewModel: ChromaEditorViewModel) {
    val colors = LocalOscellionColors.current
    val modes  = BlendMode.values().toList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text("Blend Mode", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        modes.chunked(2).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                            text       = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                            color      = if (selected) colors.onPrimary else colors.textMuted,
                            fontSize   = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// -------------------------------------------------------------------------------
// ADD MEDIA SHEET
// -------------------------------------------------------------------------------

@Composable
fun AppIconBox(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val brush = remember(backgroundColor) {
        when (backgroundColor.value) {
            0xFF007AFFuL -> Brush.verticalGradient(listOf(Color(0xFF44A6FF), Color(0xFF007AFF))) // Blue
            0xFF5856D6uL -> Brush.verticalGradient(listOf(Color(0xFF7868E6), Color(0xFF5856D6))) // Indigo
            0xFF34C759uL -> Brush.verticalGradient(listOf(Color(0xFF4CD964), Color(0xFF34C759))) // Green
            0xFFFF9500uL -> Brush.verticalGradient(listOf(Color(0xFFFFB340), Color(0xFFFF9500))) // Orange
            0xFFFF3B30uL -> Brush.verticalGradient(listOf(Color(0xFFFF665A), Color(0xFFFF3B30))) // Red
            0xFF8E8E93uL -> Brush.verticalGradient(listOf(Color(0xFFA3AAB2), Color(0xFF8E8E93))) // Silver
            0xFFAF52DEuL -> Brush.verticalGradient(listOf(Color(0xFFC973F0), Color(0xFFAF52DE))) // Purple
            0xFF30B0C7uL -> Brush.verticalGradient(listOf(Color(0xFF5AC8FA), Color(0xFF30B0C7))) // Teal
            else -> Brush.verticalGradient(listOf(backgroundColor, backgroundColor))
        }
    }

    Box(
        modifier = modifier
            .size(30.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .background(brush)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                shadowElevation = 2f
                shape = RoundedCornerShape(1.dp)
                ambientShadowColor = Color.Black.copy(alpha = 0.12f)
                spotShadowColor = Color.Black.copy(alpha = 0.12f)
            }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMediaSheet(
    onDismiss:  () -> Unit,
    onAddMedia: (MediaType, String, Int, Color, String?) -> Unit
) {
    val colors = LocalOscellionColors.current
    var activeGalleryPickerType by remember { mutableStateOf<MediaType?>(null) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAddMedia(MediaType.VIDEO, "Video", 1, Color(0xFF1E3A8A), uri.toString())
        onDismiss()
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAddMedia(MediaType.IMAGE, "Image", 2, Color(0xFF064E3B), uri.toString())
        onDismiss()
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onAddMedia(MediaType.AUDIO, "Audio", 4, Color(0xFF047857), uri.toString())
        onDismiss()
    }
    val stickerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onAddMedia(MediaType.STICKER, "Sticker", 5, Color(0xFFB45309), uri.toString())
        onDismiss()
    }

    com.chroma.studio.ui.components.GlassBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Add to Timeline",
                color      = Color(0xFF1C1C1E),
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(bottom = 16.dp)
            )

            data class MediaOption(
                val icon:    androidx.compose.ui.graphics.vector.ImageVector,
                val label:   String,
                val subtext: String,
                val color:   Color
            )

            val options = listOf(
                MediaOption(Lucide.Film,      "Video Clip",   "Add a video to the main track",   Color(0xFF007AFF)),
                MediaOption(Lucide.Image,     "Image Overlay","Add a photo or PNG overlay",      Color(0xFF34C759)),
                MediaOption(Lucide.AlignLeft, "Text Layer",   "Add animated text overlay",       Color(0xFFAF52DE)),
                MediaOption(Lucide.Music,     "Audio Track",  "Add background music or audio",   Color(0xFF30B0C7)),
                MediaOption(Lucide.Layers,    "Sticker",      "Add a PNG sticker overlay",       Color(0xFFFF9500))
            )

            options.forEachIndexed { i, opt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            when (i) {
                                0 -> activeGalleryPickerType = MediaType.VIDEO
                                1 -> activeGalleryPickerType = MediaType.IMAGE
                                2 -> { onAddMedia(MediaType.TEXT, "Text", 3, Color(0xFF701A75), null); onDismiss() }
                                3 -> audioPicker.launch("audio/*")
                                4 -> activeGalleryPickerType = MediaType.STICKER
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AppIconBox(backgroundColor = opt.color) {
                        Icon(opt.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(opt.label,   color = colors.textMain,          fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(opt.subtext, color = Color(0xFF6B7280),    fontSize = 11.sp)
                    }
                    Icon(Lucide.ChevronRight, contentDescription = null, tint = Color(0xFF333333), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (activeGalleryPickerType != null) {
        val pickerType = activeGalleryPickerType!!
        val isVideo = pickerType == MediaType.VIDEO
        com.chroma.studio.ui.components.OscellionGalleryLayout(
            layoutMode = com.chroma.studio.ui.components.GalleryLayoutMode.BOTTOM_SHEET,
            title = if (isVideo) "Video Clip" else "Image Overlay",
            badgeText = "Editor",
            allowedMediaFilter = if (isVideo) com.chroma.studio.ui.components.GalleryMediaTypeFilter.VIDEOS else com.chroma.studio.ui.components.GalleryMediaTypeFilter.PHOTOS,
            showCameraTile = true,
            showCaptionInput = false,
            onVideoSelected = { uri ->
                activeGalleryPickerType = null
                onAddMedia(MediaType.VIDEO, "Video", 1, Color(0xFF1E3A8A), uri.toString())
                onDismiss()
            },
            onPhotosSelected = { uris ->
                val currentType = activeGalleryPickerType
                activeGalleryPickerType = null
                uris.firstOrNull()?.let { uri ->
                    when (currentType) {
                        MediaType.VIDEO -> onAddMedia(MediaType.VIDEO, "Video", 1, Color(0xFF1E3A8A), uri.toString())
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

// -------------------------------------------------------------------------------
// HELPER: EDITOR SLIDER
// -------------------------------------------------------------------------------

@Composable
private fun EditorSlider(
    label:         String,
    value:         Float,
    min:           Float,
    max:           Float,
    onValueChange: (Float) -> Unit
) {
    val colors = LocalOscellionColors.current
    var pos by remember(value) { mutableStateOf(value) }

    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text     = label,
            color    = colors.textMuted,
            fontSize = 10.sp,
            modifier = Modifier.width(68.dp),
            maxLines = 1
        )
        AppleSlider(
            value          = pos,
            onValueChange  = { pos = it; onValueChange(it) },
            valueRange     = min..max,
            modifier       = Modifier.weight(1f)
        )
        Text(
            text      = String.format("%.2f", pos),
            color     = colors.primary,
            fontSize  = 9.sp,
            modifier  = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}

/** Backward-compatible alias used by any external code that referenced PropertySlider */
@Composable
fun PropertySlider(
    label:         String,
    value:         Float,
    min:           Float,
    max:           Float,
    onChangeStart: () -> Unit = {},
    onChangeEnd:   () -> Unit = {},
    onValueChange: (Float) -> Unit
) = EditorSlider(label, value, min, max, onValueChange)
@Composable
private fun QuickActionsPanel(
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
            .hazeChild(state = com.chroma.studio.ui.components.LocalHazeState.current, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.6f))
            .border(0.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickActionButton(icon = Lucide.Plus, label = "Add Media", onClick = onAddMedia)
        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color(0xFFF3F4F6)))
        QuickActionButton(icon = Lucide.Layers, label = "Layers", onClick = onLayers)
        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color(0xFFF3F4F6)))
        QuickActionButton(icon = Lucide.Settings, label = "Settings", onClick = onSettings)
    }
}

@Composable
private fun RowScope.QuickActionButton(
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


@Composable
private fun HorizontalToolbar(
    clip: Clip?,
    onOpenTool: (EditorTool) -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val hazeState = com.chroma.studio.ui.components.LocalHazeState.current

    // All tools with their icons and Apple gradient colors
    data class ToolItem(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val label: String,
        val color: Color,
        val action: () -> Unit,
        val requiresClip: Boolean = true
    )

    val tools = listOf(
        ToolItem(Lucide.Scissors,  "Trim",      Color(0xFF007AFF), { onOpenTool(EditorTool.TRIM) }),
        ToolItem(Lucide.Clock,     "Speed",     Color(0xFF5856D6), { onOpenTool(EditorTool.SPEED) }),
        ToolItem(Lucide.SlidersHorizontal, "Filter", Color(0xFFFF9500), { onOpenTool(EditorTool.FILTER) }),
        ToolItem(Lucide.Settings2, "Adjust",    Color(0xFF34C759), { onOpenTool(EditorTool.ADJUST) }),
        ToolItem(Lucide.Move,      "Transform", Color(0xFF8E8E93), { onOpenTool(EditorTool.TRANSFORM) }),
        ToolItem(Lucide.Volume2,   "Audio",     Color(0xFF007AFF), { onOpenTool(EditorTool.AUDIO) }),
        ToolItem(Lucide.Type,      "Text",      Color(0xFFFF3B30), { onOpenTool(EditorTool.TEXT) }),
        ToolItem(Lucide.Pipette,   "Chroma",    Color(0xFF34C759), { onOpenTool(EditorTool.CHROMA_KEY) }),
        ToolItem(Lucide.Blend,     "Blend",     Color(0xFFAF52DE), { onOpenTool(EditorTool.BLEND) }),
        ToolItem(Lucide.Split,     "Split",     Color(0xFFFF3B30), { onSplit() }),
        ToolItem(Lucide.Copy,      "Duplicate", Color(0xFF5856D6), { onDuplicate() }),
        ToolItem(Lucide.Trash2,    "Delete",    Color(0xFFFF3B30), { onDelete() })
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

@Composable
private fun ProjectSettingsPanel(
    state: ProjectState,
    onFitModeChange: (VideoFitMode) -> Unit,
    onRatioChange: (ProjectRatio) -> Unit
) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Project Settings", color = colors.textMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        // Aspect Ratio
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Aspect Ratio", color = colors.textMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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

        // Video Fit Mode
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Video Scale Mode", color = colors.textMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            data class FitOption(val mode: VideoFitMode, val label: String, val desc: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
            val options = listOf(
                FitOption(VideoFitMode.FIT,  "Fit",          "Show full video", Lucide.Expand),
                FitOption(VideoFitMode.FILL, "Fill",         "Fill the frame",  Lucide.Maximize2),
                FitOption(VideoFitMode.CROP, "Crop to Fit",  "Crop to fill",    Lucide.Crop)
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
private fun EditorBottomPanelGridUI(
    modifier: Modifier,
    clip: Clip,
    onOpenTool: (EditorTool) -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Edit") }
    val tabs = listOf("Edit", "Visuals", "Audio", "Text")
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(top = 20.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
    ) {
        // Tabs
        val tabIcons = mapOf(
            "Edit" to Lucide.Scissors,
            "Visuals" to Lucide.Palette,
            "Audio" to Lucide.Volume2,
            "Text" to Lucide.Type
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                val isActive = tab == selectedTab
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isActive) Color(0xFF3B82F6) else Color.Transparent)
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = tabIcons[tab] ?: Lucide.Circle,
                        contentDescription = tab,
                        tint = if (isActive) Color.White else Color(0xFF6B7280),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = tab,
                        color = if (isActive) Color.White else Color(0xFF6B7280),
                        fontSize = 14.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Grids
        when (selectedTab) {
            "Edit" -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolGridItem(Modifier.weight(1f), Lucide.Scissors, "Trim", "Set start & end", { onOpenTool(EditorTool.TRIM) })
                    ToolGridItem(Modifier.weight(1f), Lucide.Clock, "Speed", "Fast or slow", { onOpenTool(EditorTool.SPEED) })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolGridItem(Modifier.weight(1f), Lucide.Move, "Transform", "Scale & position", { onOpenTool(EditorTool.TRANSFORM) })
                    ToolGridItem(Modifier.weight(1f), Lucide.Copy, "Duplicate", "Clone this clip", onDuplicate)
                }
            }
            "Visuals" -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolGridItem(Modifier.weight(1f), Lucide.SlidersHorizontal, "Filters", "Color presets", { onOpenTool(EditorTool.FILTER) })
                    ToolGridItem(Modifier.weight(1f), Lucide.Settings2, "Adjust", "Brightness & contrast", { onOpenTool(EditorTool.ADJUST) })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolGridItem(Modifier.weight(1f), Lucide.Pipette, "Chroma Key", "Remove green screen", { onOpenTool(EditorTool.CHROMA_KEY) })
                    ToolGridItem(Modifier.weight(1f), Lucide.Blend, "Blend", "Opacity & modes", { onOpenTool(EditorTool.BLEND) })
                }
            }
            "Audio" -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolGridItem(Modifier.weight(1f), Lucide.Volume2, "Volume", "Adjust levels", { onOpenTool(EditorTool.AUDIO) })
                }
            }
            "Text" -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolGridItem(Modifier.weight(1f), Lucide.Type, "Edit Text", "Content & style", { onOpenTool(EditorTool.TEXT) })
                }
            }
        }
    }
}

@Composable
private fun ToolGridItem(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x0F000000), ambientColor = Color(0x0F000000))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = title, tint = Color(0xFF3B82F6), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
        Spacer(modifier = Modifier.height(2.dp))
        Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF9CA3AF), lineHeight = 14.sp)
    }
}
