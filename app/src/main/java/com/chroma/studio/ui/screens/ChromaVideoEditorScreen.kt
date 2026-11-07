package com.chroma.studio.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.AppleSlider
import com.chroma.studio.ui.theme.LocalOscellionColors
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════════════
// DATA MODELS FOR STUDIO EDITORS
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
            0f,    0.85f, 0f,    0f, 20f,
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
// ═══════════════════════════════════════════════════════════════════════════════

fun buildPreviewColorMatrix(filter: FilterType, adj: Adjustments): android.graphics.ColorMatrix {
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

    // Brightness / Contrast / Exposure / Highlights / Shadows
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
    val textFont:        String       = "",
    val textStylePreset: String       = "Normal",
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

enum class EditorTool(val label: String) {
    TRIM("Trim"), ADJUST("Adjust"), FILTER("Filter"), 
    TRANSFORM("Transform"), AUDIO("Audio"), TEXT("Text"), 
    SPEED("Speed"), BLEND("Blend"), CHROMA_KEY("Chroma Key")
}

// ═══════════════════════════════════════════════════════════════════════════════
// UI HELPER COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun AppIconBox(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val brush = remember(backgroundColor) {
        when (backgroundColor.value) {
            0xFF0381FEuL -> Brush.verticalGradient(listOf(Color(0xFF44A6FF), Color(0xFF0381FE))) // Blue
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

@Composable
fun EditorSlider(
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

