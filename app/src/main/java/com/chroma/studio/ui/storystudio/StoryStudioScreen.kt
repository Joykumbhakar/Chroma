/**
 * ==============================================================================
 * STORY STUDIO — PRO MOBILE APPLE HIG EDITION (KOTLIN + JETPACK COMPOSE)
 * ==============================================================================
 * Single-file, zero-external-image-library implementation with 1:1 design,
 * logic, 3D borderless iconography, multi-touch ergonomics, and 1080x1920 PNG
 * export parity with the Apple HIG Story Studio web specification.
 *
 * REQUIRED GRADLE DEPENDENCIES (app/build.gradle.kts):
 *   implementation(platform("androidx.compose:compose-bom:2025.02.00"))
 *   implementation("androidx.compose.ui:ui")
 *   implementation("androidx.compose.foundation:foundation")
 *   implementation("androidx.compose.material3:material3")
 *   implementation("androidx.activity:activity-compose:1.10.0")
 *   implementation("androidx.core:core-ktx:1.15.0")
 *
 * OPTIONAL FOR SYSTEM SHARE SHEET (AndroidManifest.xml):
 *   Register a FileProvider with authority "${applicationId}.fileprovider"
 *   pointing to cache-path "." for sharing exported PNGs via Intent.ACTION_SEND.
 *
 * USAGE IN MainActivity.kt:
 *   enableEdgeToEdge()
 *   setContent { StoryStudioScreen() }
 * ==============================================================================
 */
package com.chroma.studio.ui.storystudio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter as AndroidColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.LinearGradient as AndroidLinearGradient
import android.graphics.Matrix
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient as AndroidRadialGradient
import android.graphics.RectF
import android.graphics.Shader as AndroidShader
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/* ==============================================================================
   1. APPLE HIG DESIGN SYSTEM TOKENS (COMPLETE LIGHT & DARK MODE PALETTES)
   ============================================================================== */

@Immutable
data class HigColors(
    val isDark: Boolean,
    val bgApp: Color,
    val bgSurface: Color,
    val bgSurfaceSecondary: Color,
    val bgSurfaceTertiary: Color,
    val bgMaterial: Color,
    val bgMaterialHeavy: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val separator: Color,
    val accent: Color,
    val accentText: Color,
    val semanticGreen: Color,
    val semanticRed: Color,
    val semanticOrange: Color,
    val semanticPurple: Color
)

private val LightHigColors = HigColors(
    isDark = false,
    bgApp = Color(0xFFF2F2F7),
    bgSurface = Color(0xFFFFFFFF),
    bgSurfaceSecondary = Color(0xFFE5E5EA),
    bgSurfaceTertiary = Color(0xFFD1D1D6),
    bgMaterial = Color(0xDEFFFFFF),
    bgMaterialHeavy = Color(0xF5F8F8FA),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xAD3C3C43),
    textTertiary = Color(0x573C3C43),
    separator = Color(0x293C3C43),
    accent = Color(0xFF007AFF),
    accentText = Color(0xFFFFFFFF),
    semanticGreen = Color(0xFF34C759),
    semanticRed = Color(0xFFFF3B30),
    semanticOrange = Color(0xFFFF9500),
    semanticPurple = Color(0xFFAF52DE)
)

private val DarkHigColors = HigColors(
    isDark = true,
    bgApp = Color(0xFF000000),
    bgSurface = Color(0xFF1C1C1E),
    bgSurfaceSecondary = Color(0xFF2C2C2E),
    bgSurfaceTertiary = Color(0xFF3A3A3C),
    bgMaterial = Color(0xD91C1C1E),
    bgMaterialHeavy = Color(0xF51C1C1E),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xA3EBEBF5),
    textTertiary = Color(0x52EBEBF5),
    separator = Color(0x7A545458),
    accent = Color(0xFF0A84FF),
    accentText = Color(0xFFFFFFFF),
    semanticGreen = Color(0xFF30D158),
    semanticRed = Color(0xFFFF453A),
    semanticOrange = Color(0xFFFF9F0A),
    semanticPurple = Color(0xFFBF5AF2)
)

private val LocalHigColors = compositionLocalOf { DarkHigColors }

/* ==============================================================================
   2. DOMAIN MODELS & CURATED PRESETS (IMMUTABLE STATE FOR O(1) UNDO/REDO)
   ============================================================================== */

@Immutable
data class StoryBackdrop(
    val name: String,
    val startColor: Color,
    val endColor: Color,
    val dotColor: Color,
    val isLight: Boolean
)

@Immutable
data class FilterPreset(
    val name: String,
    val brightness: Float,
    val contrast: Float,
    val saturate: Float,
    val sepia: Float,
    val grayscale: Float,
    val vignette: Float
)

@Immutable
data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val freq: Double
)

enum class LayoutType(val label: String) {
    NONE("Single"),
    SPLIT_H("2 Rows"),
    SPLIT_V("2 Cols"),
    GRID_3("3 Grid"),
    GRID_4("4 Grid")
}

enum class PhotoFitMode { CONTAIN, COVER }

enum class TextFontStyle(val label: String) {
    SYSTEM("SF Modern"),
    SERIF("Editorial"),
    ITALIC("Classic"),
    MONO("Typewriter"),
    BOLD("Poster"),
    NEON("Glow")
}

enum class TextBgMode(val label: String) {
    CLEAN("Clean"),
    PILL("Solid"),
    TRANSLUCENT("Frosted"),
    OUTLINE("Outline")
}

enum class TextAnimStyle(val label: String) {
    NONE("Static"),
    FADE("Fade"),
    SLIDE("Slide Up"),
    POP("Pop")
}

enum class TextAlignmentMode { CENTER, LEFT, RIGHT }

enum class PhotoStickerShape(val label: String) {
    ROUNDED("rounded"),
    CIRCLE("circle"),
    POLAROID("polaroid"),
    HEART("heart"),
    STAR("star")
}

enum class BrushType(val label: String) {
    PEN("Pen"),
    MARKER("Marker"),
    NEON("Glow"),
    AURORA("Aurora"),
    EMOJI("Emoji"),
    ERASER("Eraser")
}

enum class StrokeLineStyle(val label: String) {
    SOLID("Solid"),
    DASHED("Dashed"),
    DOTTED("Dotted"),
    DOUBLE("Double"),
    LASER("Laser")
}

enum class WidgetType {
    POLL, SLIDER, QUIZ, COUNTDOWN, QUESTION, MENTION, LINK, LOCATION, TIME, MUSIC
}

enum class ActiveSheet {
    NONE, STICKERS, DRAW, LAYOUT, FILTERS, MUSIC, SHARE
}

enum class EyedropperTarget {
    NONE, TOAST, TEXT, DRAW
}

enum class StoryAudience { PUBLIC, CLOSE_FRIENDS }

@Immutable
data class PhotoState(
    val uriKey: String? = null,
    val xDp: Float = 0f,
    val yDp: Float = 0f,
    val scale: Float = 1f,
    val fitMode: PhotoFitMode = PhotoFitMode.CONTAIN,
    val flipX: Boolean = false
)

@Immutable
data class FilterState(
    val preset: String = "Original",
    val brightness: Float = 100f,
    val contrast: Float = 100f,
    val saturate: Float = 100f,
    val sepia: Float = 0f,
    val grayscale: Float = 0f,
    val vignette: Float = 0f
)

@Immutable
data class NormalizedPoint(val x: Float, val y: Float)

@Immutable
data class DrawStroke(
    val brush: BrushType,
    val colorArgb: Int,
    val emoji: String = "✨",
    val sizeDp: Float = 6f,
    val lineStyle: StrokeLineStyle = StrokeLineStyle.SOLID,
    val glowEnabled: Boolean = false,
    val recognizedShape: RecognizedShapeType? = null,
    val points: List<NormalizedPoint> = emptyList()
)

@Immutable
sealed interface StoryItem {
    val id: String
    val xDp: Float
    val yDp: Float
    val scale: Float
    val rotation: Float
    val locked: Boolean

    fun copyTransform(
        xDp: Float = this.xDp,
        yDp: Float = this.yDp,
        scale: Float = this.scale,
        rotation: Float = this.rotation,
        locked: Boolean = this.locked,
        id: String = this.id
    ): StoryItem
}

@Immutable
data class TextStoryItem(
    override val id: String = newId("txt"),
    override val xDp: Float = 0f,
    override val yDp: Float = 0f,
    override val scale: Float = 1f,
    override val rotation: Float = 0f,
    override val locked: Boolean = false,
    val text: String = "",
    val font: TextFontStyle = TextFontStyle.SYSTEM,
    val colorArgb: Int = AndroidColor.WHITE,
    val bgMode: TextBgMode = TextBgMode.CLEAN,
    val align: TextAlignmentMode = TextAlignmentMode.CENTER,
    val sizeSp: Float = 32f,
    val anim: TextAnimStyle = TextAnimStyle.NONE
) : StoryItem {
    override fun copyTransform(
        xDp: Float, yDp: Float, scale: Float, rotation: Float, locked: Boolean, id: String
    ) = copy(id = id, xDp = xDp, yDp = yDp, scale = scale, rotation = rotation, locked = locked)
}

@Immutable
data class EmojiStoryItem(
    override val id: String = newId("emo"),
    override val xDp: Float = 0f,
    override val yDp: Float = 0f,
    override val scale: Float = 1f,
    override val rotation: Float = 0f,
    override val locked: Boolean = false,
    val emoji: String
) : StoryItem {
    override fun copyTransform(
        xDp: Float, yDp: Float, scale: Float, rotation: Float, locked: Boolean, id: String
    ) = copy(id = id, xDp = xDp, yDp = yDp, scale = scale, rotation = rotation, locked = locked)
}

@Immutable
data class PhotoStickerItem(
    override val id: String = newId("stk"),
    override val xDp: Float = 0f,
    override val yDp: Float = 0f,
    override val scale: Float = 1f,
    override val rotation: Float = 0f,
    override val locked: Boolean = false,
    val uriKey: String,
    val shape: PhotoStickerShape = PhotoStickerShape.ROUNDED
) : StoryItem {
    override fun copyTransform(
        xDp: Float, yDp: Float, scale: Float, rotation: Float, locked: Boolean, id: String
    ) = copy(id = id, xDp = xDp, yDp = yDp, scale = scale, rotation = rotation, locked = locked)
}

@Immutable
data class WidgetStoryItem(
    override val id: String = newId("wdg"),
    override val xDp: Float = 0f,
    override val yDp: Float = 0f,
    override val scale: Float = 1f,
    override val rotation: Float = 0f,
    override val locked: Boolean = false,
    val widgetType: WidgetType,
    // Poll & Quiz
    val question: String = "",
    val optA: String = "Light HIG",
    val optB: String = "Dark Mode",
    val quizOpts: List<String> = listOf("Cupertino", "Tokyo", "Bengaluru"),
    val correctIndex: Int = 0,
    // Slider & Question
    val prompt: String = "",
    val emoji: String = "🔥",
    val sliderValue: Int = 72,
    // Countdown
    val title: String = "LAUNCH DAY",
    val days: String = "03",
    val hours: String = "14",
    val mins: String = "28",
    // Mention / Link / Location / Time / Music
    val handle: String = "@DESIGNER",
    val linkLabel: String = "apple.com/design",
    val place: String = "BENGALURU, IN",
    val timeText: String = "12:00 PM",
    val musicTitle: String = "",
    val musicArtist: String = "",
    val musicFreq: Double = 261.63,
    val musicTrackId: String = "",
    val musicAlbum: String = "",
    val musicCoverUrl: String = "",
    val musicAudioUrl: String = "",
    val musicStartSec: Int = 0,
    val musicDurationSec: Int = 15
) : StoryItem {
    override fun copyTransform(
        xDp: Float, yDp: Float, scale: Float, rotation: Float, locked: Boolean, id: String
    ) = copy(id = id, xDp = xDp, yDp = yDp, scale = scale, rotation = rotation, locked = locked)
}

@Immutable
data class StorySlide(
    val id: String = newId("slide"),
    val backdropIndex: Int = 0,
    val layout: LayoutType = LayoutType.NONE,
    val layoutImages: Map<Int, String> = emptyMap(),
    val photo: PhotoState = PhotoState(),
    val filters: FilterState = FilterState(),
    val items: List<StoryItem> = emptyList(),
    val strokes: List<DrawStroke> = emptyList()
)

@Immutable
private data class SlideHistory(
    val stack: List<StorySlide>,
    val index: Int
)

private fun newId(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().take(8)}"

private val STORY_BACKDROPS = listOf(
    StoryBackdrop("Obsidian", Color(0xFF1E1E24), Color(0xFF121215), Color(0xFF2C2C2E), false),
    StoryBackdrop("Pure Light", Color(0xFFF9F9FB), Color(0xFFE5E5EA), Color(0xFFF2F2F7), true),
    StoryBackdrop("Cupertino Sky", Color(0xFF007AFF), Color(0xFF5856D6), Color(0xFF007AFF), false),
    StoryBackdrop("Sunset", Color(0xFFFF9500), Color(0xFFFF2D55), Color(0xFFFF5E3A), false),
    StoryBackdrop("Emerald", Color(0xFF34C759), Color(0xFF008060), Color(0xFF34C759), false),
    StoryBackdrop("Midnight Indigo", Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF1E1B4B), false)
)

private val APPLE_SWATCH_COLORS = listOf(
    Color(0xFFFFFFFF), Color(0xFF000000), Color(0xFF007AFF), Color(0xFF34C759), Color(0xFFFF9500),
    Color(0xFFFF3B30), Color(0xFFAF52DE), Color(0xFFFF2D55), Color(0xFF5AC8FA), Color(0xFFFFCC00)
)

private val STICKER_EMOJIS = listOf(
    "✨", "🔥", "❤️", "😂", "🙌", "🥳", "😍", "🤩",
    "💯", "📍", "🎶", "☕️", "🌿", "☀️", "🌙", "⚡️",
    "🥂", "📸", "🫶", "🚀", "🎨", "🌊", "🦋", ""
)

private val BRUSH_EMOJIS = listOf("✨", "❤️", "🔥", "⚡️", "🌸", "⭐️", "🦋", "💎")

private val FILTER_PRESETS = listOf(
    FilterPreset("Original", 100f, 100f, 100f, 0f, 0f, 0f),
    FilterPreset("Vivid", 104f, 114f, 132f, 0f, 0f, 10f),
    FilterPreset("Warm", 103f, 104f, 112f, 24f, 0f, 12f),
    FilterPreset("Cool", 102f, 108f, 90f, 0f, 0f, 8f),
    FilterPreset("Chrome", 106f, 122f, 118f, 4f, 0f, 18f),
    FilterPreset("Noir", 105f, 130f, 0f, 0f, 100f, 28f),
    FilterPreset("Fade", 109f, 84f, 82f, 12f, 0f, 0f),
    FilterPreset("Dramatic", 94f, 134f, 92f, 6f, 0f, 42f)
)

private val MUSIC_TRACKS = listOf(
    MusicTrack("m1", "Midnight Reflections", "Ambient Studio", "0:15", 261.63),
    MusicTrack("m2", "Golden Hour Drive", "West Coast Keys", "0:15", 329.63),
    MusicTrack("m3", "Rain on Glass", "Lo-Fi Collective", "0:15", 293.66),
    MusicTrack("m4", "Solaris Horizon", "Synthwave Lab", "0:15", 392.00)
)

/* ==============================================================================
   3. CLEAN 3D APPLE-INSPIRED VECTOR ICON SYSTEM (ZERO ICON BACKGROUNDS)
   ============================================================================== */

enum class Icon3DName {
    IMAGE_PLUS, UNDO, REDO, PIPETTE, SUN, MOON, PLAY, ARROW_UP_RIGHT,
    PLUS, MINUS, ROTATE_CCW, CLOSE, CHEVRON_DOWN,
    TYPE, STICKERS, PEN_TOOL, LAYOUT_GRID, SLIDERS_HORIZONTAL, MUSIC,
    IMAGES, POLL, SLIDER_WIDGET, QUIZ, TIMER, QUESTION, MENTION, LINK, MAP_PIN, CLOCK,
    LAYOUT_SINGLE, LAYOUT_ROWS2, LAYOUT_COLS2, LAYOUT_GRID3, LAYOUT_GRID4,
    CONTRAST, DROPLET, THERMOMETER, APERTURE,
    EDIT, LAYERS, LOCK, UNLOCK, COPY, TRASH, FLIP_HORIZONTAL,
    ALIGN_CENTER, ALIGN_LEFT, ALIGN_RIGHT, HIGHLIGHTER, SPARKLES,
    GLOBE, STAR, DOWNLOAD, SHARE, HEART, SEND, CHECK_CIRCLE, PALETTE, SHAPES, INFO, ALERT
}

@Composable
private fun Icon3D(
    icon: Icon3DName,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    alpha: Float = 1f
) {
    val isDark = LocalHigColors.current.isDark
    val ambientShadow = if (isDark) Color(0x88000000) else Color(0x2E0F172A)
    val softShadow = if (isDark) Color(0x45000000) else Color(0x180F172A)
    val glyphShadow = Color(0x42000000)

    Canvas(
        modifier = modifier
            .size(size)
            .alpha(alpha)
    ) {
        val s = this.size.minDimension / 24f
        fun px(v: Float) = v * s

        // 4-Stop Studio-Lit 3D Gradients (Top-Left Key Light -> Core -> Rich Shadow -> Bounced Rim)
        val gBlue = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFF93E2FE),
                0.35f to Color(0xFF3B82F6),
                0.82f to Color(0xFF1D4ED8),
                1.00f to Color(0xFF1E40AF)
            ),
            start = Offset(px(2f), px(1f)), end = Offset(px(22f), px(23f))
        )
        val gBlueDeep = Color(0xFF1E3A8A)

        val gAmber = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFFFEF08A),
                0.36f to Color(0xFFFBBF24),
                0.78f to Color(0xFFEA580C),
                1.00f to Color(0xFFC2410C)
            ),
            start = Offset(px(2f), px(1f)), end = Offset(px(22f), px(23f))
        )
        val gAmberDeep = Color(0xFF9A3412)

        val gPurple = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFFF3E8FF),
                0.32f to Color(0xFFC084FC),
                0.76f to Color(0xFF7C3AED),
                1.00f to Color(0xFF5B21B6)
            ),
            start = Offset(px(2f), px(1f)), end = Offset(px(22f), px(23f))
        )
        val gPurpleDeep = Color(0xFF4C1D95)

        val gEmerald = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFFA7F3D0),
                0.36f to Color(0xFF10B981),
                0.82f to Color(0xFF047857),
                1.00f to Color(0xFF065F46)
            ),
            start = Offset(px(2f), px(1f)), end = Offset(px(22f), px(23f))
        )
        val gEmeraldDeep = Color(0xFF064E3B)

        val gIndigo = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFFC7D2FE),
                0.36f to Color(0xFF6366F1),
                0.82f to Color(0xFF4338CA),
                1.00f to Color(0xFF312E81)
            ),
            start = Offset(px(2f), px(1f)), end = Offset(px(22f), px(23f))
        )
        val gIndigoDeep = Color(0xFF312E81)

        val gRose = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFFFECDD3),
                0.34f to Color(0xFFFB7185),
                0.76f to Color(0xFFE11D48),
                1.00f to Color(0xFF9F1239)
            ),
            start = Offset(px(2f), px(1f)), end = Offset(px(22f), px(23f))
        )
        val gRoseDeep = Color(0xFF881337)

        val gCyan = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFFCFFAFE),
                0.35f to Color(0xFF22D3EE),
                0.80f to Color(0xFF0891B2),
                1.00f to Color(0xFF0E7490)
            ),
            start = Offset(px(2f), px(1f)), end = Offset(px(22f), px(23f))
        )
        val gCyanDeep = Color(0xFF155E75)

        val gGold = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFFFEF9C3),
                0.40f to Color(0xFFFACC15),
                0.82f to Color(0xFFD97706),
                1.00f to Color(0xFF92400E)
            ),
            start = Offset(px(2f), px(1f)), end = Offset(px(22f), px(23f))
        )

        // Adaptive Anodized Metallic Silver (Crisp contrast on BOTH Light & Dark Modes)
        val gSilver = if (isDark) {
            Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFFFFFFF),
                    0.44f to Color(0xFFDCE3EC),
                    0.85f to Color(0xFF94A3B8),
                    1.00f to Color(0xFF64748B)
                ),
                start = Offset(px(2f), px(1f)), end = Offset(px(22f), px(23f))
            )
        } else {
            Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF94A3B8),
                    0.42f to Color(0xFF64748B),
                    0.85f to Color(0xFF334155),
                    1.00f to Color(0xFF1E293B)
                ),
                start = Offset(px(2f), px(1f)), end = Offset(px(22f), px(23f))
            )
        }
        val gSilverDeep = if (isDark) Color(0xFF475569) else Color(0xFF0F172A)

        // Specular Top-Dome Glass Reflection & Bevel Rim Stroke
        val gGloss = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = 0.68f),
                0.45f to Color.White.copy(alpha = 0.16f),
                1.00f to Color.Transparent
            ),
            startY = px(1.5f), endY = px(13.5f)
        )
        val gBevel = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = 0.82f),
                0.42f to Color.White.copy(alpha = 0.18f),
                1.00f to Color.Black.copy(alpha = 0.28f)
            ),
            start = Offset(px(2f), px(2f)), end = Offset(px(22f), px(22f))
        )

        // Reusable 3D Sculpted Primitives (Contact Shadow + Extrusion Thickness + Specular Dome + Bevel)
        fun DrawScope.drawSculptedRect(
            x: Float, y: Float, w: Float, h: Float, r: Float,
            brush: Brush, extrudeColor: Color? = null, gloss: Boolean = true
        ) {
            val tl = Offset(px(x), px(y))
            val sz = Size(px(w), px(h))
            val cr = CornerRadius(px(r), px(r))

            // Soft grounded shadow
            drawRoundRect(softShadow, Offset(px(x), px(y + 1.9f)), sz, cr)
            drawRoundRect(ambientShadow, Offset(px(x), px(y + 1.15f)), sz, cr)
            // Physical 3D bottom extrusion rim
            if (extrudeColor != null) {
                drawRoundRect(extrudeColor, Offset(px(x), px(y + 0.72f)), sz, cr)
            }
            // Main lit body
            drawRoundRect(brush, tl, sz, cr)
            // Curved upper glass reflection
            if (gloss) {
                val clipP = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(px(x), px(y), px(x + w), px(y + h), cr))
                }
                clipPath(clipP) {
                    drawOval(
                        brush = gGloss,
                        topLeft = Offset(px(x - w * 0.12f), px(y - h * 0.22f)),
                        size = Size(px(w * 1.24f), px(h * 0.74f))
                    )
                }
            }
            // Top-left specular bevel rim
            drawRoundRect(gBevel, tl, sz, cr, style = Stroke(width = px(0.7f)))
        }

        fun DrawScope.drawSculptedOrb(
            cx: Float, cy: Float, r: Float,
            brush: Brush, extrudeColor: Color? = null
        ) {
            val center = Offset(px(cx), px(cy))
            drawCircle(softShadow, px(r * 1.02f), Offset(px(cx), px(cy + 1.85f)))
            drawCircle(ambientShadow, px(r), Offset(px(cx), px(cy + 1.1f)))
            if (extrudeColor != null) {
                drawCircle(extrudeColor, px(r), Offset(px(cx), px(cy + 0.65f)))
            }
            drawCircle(brush, px(r), center)
            // Specular upper-left dome highlight
            rotate(-22f, Offset(px(cx - r * 0.2f), px(cy - r * 0.36f))) {
                drawOval(
                    brush = gGloss,
                    topLeft = Offset(px(cx - r * 0.68f), px(cy - r * 0.84f)),
                    size = Size(px(r * 1.22f), px(r * 0.66f))
                )
            }
            // Crisp bottom bounced rim shimmer + outer bevel
            drawCircle(gBevel, px(r), center, style = Stroke(width = px(0.7f)))
        }

        fun DrawScope.drawSculptedPath(
            path: Path, brush: Brush, extrudeColor: Color? = null, gloss: Boolean = true
        ) {
            translate(0f, px(1.7f)) { drawPath(path, color = softShadow) }
            translate(0f, px(1.05f)) { drawPath(path, color = ambientShadow) }
            if (extrudeColor != null) {
                translate(0f, px(0.65f)) { drawPath(path, color = extrudeColor) }
            }
            drawPath(path, brush = brush)
            if (gloss) {
                clipPath(path) {
                    drawOval(
                        brush = gGloss,
                        topLeft = Offset(px(1f), px(0.5f)),
                        size = Size(px(22f), px(12.5f))
                    )
                }
            }
            drawPath(path, brush = gBevel, style = Stroke(width = px(0.65f), join = StrokeJoin.Round))
        }

        fun DrawScope.drawEmbossedLine(start: Offset, end: Offset, strokeWidth: Float, color: Color = Color.White) {
            drawLine(glyphShadow, start + Offset(0f, px(0.7f)), end + Offset(0f, px(0.7f)), strokeWidth, StrokeCap.Round)
            drawLine(color, start, end, strokeWidth, StrokeCap.Round)
        }

        when (icon) {
            Icon3DName.IMAGE_PLUS, Icon3DName.IMAGES -> {
                if (icon == Icon3DName.IMAGES) {
                    rotate(7f, Offset(px(13.5f), px(9.5f))) {
                        drawSculptedRect(5.6f, 2.6f, 15.2f, 13.0f, 3.6f, gIndigo, gIndigoDeep, gloss = false)
                    }
                }
                val topY = if (icon == Icon3DName.IMAGES) 5.5f else 3.6f
                val h = if (icon == Icon3DName.IMAGES) 14.2f else 15.4f
                val w = 16.8f
                drawSculptedRect(2.2f, topY, w, h, 4.2f, gBlue, gBlueDeep)

                // Sun orb with soft glow
                drawCircle(Color(0x55FEF08A), px(2.5f), Offset(px(7.4f), px(topY + 4.2f)))
                drawCircle(gGold, px(1.65f), Offset(px(7.4f), px(topY + 4.2f)))

                // Layered Twin Mountain Peaks (Back translucent + Front crisp white)
                val backMtn = Path().apply {
                    moveTo(px(9.2f), px(topY + h - 0.9f))
                    lineTo(px(13.7f), px(topY + h - 7.3f))
                    lineTo(px(18.0f), px(topY + h - 2.3f))
                    lineTo(px(17.1f), px(topY + h - 0.9f))
                    close()
                }
                drawPath(backMtn, color = Color.White.copy(alpha = 0.58f))

                val frontMtn = Path().apply {
                    moveTo(px(3.5f), px(topY + h - 1.6f))
                    lineTo(px(8.0f), px(topY + h - 6.9f))
                    lineTo(px(11.3f), px(topY + h - 3.6f))
                    lineTo(px(13.4f), px(topY + h - 5.5f))
                    lineTo(px(16.8f), px(topY + h - 0.9f))
                    lineTo(px(4.9f), px(topY + h - 0.9f))
                    close()
                }
                drawPath(frontMtn, color = Color.White.copy(alpha = 0.95f))

                if (icon == Icon3DName.IMAGE_PLUS) {
                    drawSculptedOrb(18.0f, 17.8f, 4.4f, gEmerald, gEmeraldDeep)
                    drawEmbossedLine(Offset(px(18.0f), px(15.7f)), Offset(px(18.0f), px(19.9f)), px(1.85f))
                    drawEmbossedLine(Offset(px(15.9f), px(17.8f)), Offset(px(20.1f), px(17.8f)), px(1.85f))
                }
            }

            Icon3DName.UNDO, Icon3DName.REDO -> {
                // Sculpted 3D filled curved ribbon arrow
                val flip = icon == Icon3DName.REDO
                val mx = { x: Float -> if (flip) px(24f - x) else px(x) }
                val ribbon = Path().apply {
                    moveTo(mx(9.4f), px(5.2f))
                    cubicTo(mx(9.4f), px(4.3f), mx(8.3f), px(3.9f), mx(7.6f), px(4.5f))
                    lineTo(mx(3.2f), px(8.6f))
                    cubicTo(mx(2.7f), px(9.05f), mx(2.7f), px(9.85f), mx(3.2f), px(10.3f))
                    lineTo(mx(7.6f), px(14.4f))
                    cubicTo(mx(8.3f), px(15.0f), mx(9.4f), px(14.6f), mx(9.4f), px(13.7f))
                    lineTo(mx(9.4f), px(11.3f))
                    lineTo(mx(14.0f), px(11.3f))
                    cubicTo(mx(16.5f), px(11.3f), mx(18.2f), px(12.9f), mx(18.2f), px(15.1f))
                    cubicTo(mx(18.2f), px(16.9f), mx(17.0f), px(18.2f), mx(15.1f), px(18.7f))
                    cubicTo(mx(14.5f), px(18.9f), mx(14.7f), px(19.7f), mx(15.4f), px(19.65f))
                    cubicTo(mx(18.9f), px(19.3f), mx(21.2f), px(16.9f), mx(21.2f), px(13.7f))
                    cubicTo(mx(21.2f), px(9.9f), mx(18.1f), px(7.5f), mx(14.0f), px(7.5f))
                    lineTo(mx(9.4f), px(7.5f))
                    close()
                }
                drawSculptedPath(ribbon, gSilver, gSilverDeep)
            }

            Icon3DName.PIPETTE -> {
                rotate(45f, Offset(px(12f), px(12f))) {
                    // Bulb
                    drawSculptedRect(9.0f, 1.8f, 6.0f, 6.4f, 3.0f, gPurple, gPurpleDeep)
                    // Metallic collar
                    drawSculptedRect(7.0f, 7.3f, 10.0f, 2.5f, 1.25f, gSilver, gSilverDeep, gloss = false)
                    // Translucent glass tube
                    val tube = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                px(9.6f), px(9.6f), px(14.4f), px(20.6f),
                                CornerRadius(px(2.4f), px(2.4f))
                            )
                        )
                    }
                    drawSculptedPath(tube, gSilver, gSilverDeep, gloss = false)
                    // Glowing cyan sampled liquid inside barrel
                    drawRoundRect(
                        brush = gCyan,
                        topLeft = Offset(px(10.1f), px(13.8f)),
                        size = Size(px(3.8f), px(6.4f)),
                        cornerRadius = CornerRadius(px(1.8f))
                    )
                    // Crisp glass specular streak
                    drawLine(
                        Color.White.copy(alpha = 0.85f),
                        Offset(px(11.1f), px(10.7f)),
                        Offset(px(11.1f), px(18.4f)),
                        px(1.05f),
                        StrokeCap.Round
                    )
                }
            }

            Icon3DName.SUN -> {
                for (i in 0 until 8) {
                    rotate(i * 45f, Offset(px(12f), px(12f))) {
                        drawRoundRect(
                            color = ambientShadow,
                            topLeft = Offset(px(10.9f), px(2.1f)),
                            size = Size(px(2.2f), px(3.4f)),
                            cornerRadius = CornerRadius(px(1.1f))
                        )
                        drawRoundRect(
                            brush = gGold,
                            topLeft = Offset(px(10.9f), px(1.6f)),
                            size = Size(px(2.2f), px(3.4f)),
                            cornerRadius = CornerRadius(px(1.1f))
                        )
                    }
                }
                drawSculptedOrb(12f, 12f, 5.4f, gAmber, gAmberDeep)
            }

            Icon3DName.MOON -> {
                val base = Path().apply { addOval(Rect(px(3.2f), px(3.2f), px(20.8f), px(20.8f))) }
                val cutout = Path().apply { addOval(Rect(px(8.4f), px(1.0f), px(24.0f), px(16.6f))) }
                val crescent = Path.combine(PathOperation.Difference, base, cutout)
                drawSculptedPath(crescent, gIndigo, gIndigoDeep)

                // Gleaming 3D gold micro-star nestled inside crescent
                val miniStar = Path().apply {
                    moveTo(px(16.8f), px(4.6f))
                    lineTo(px(17.6f), px(6.8f))
                    lineTo(px(19.8f), px(7.6f))
                    lineTo(px(17.6f), px(8.4f))
                    lineTo(px(16.8f), px(10.6f))
                    lineTo(px(16.0f), px(8.4f))
                    lineTo(px(13.8f), px(7.6f))
                    lineTo(px(16.0f), px(6.8f))
                    close()
                }
                drawPath(miniStar, brush = gGold)
            }

            Icon3DName.PLAY -> {
                // Smoothly rounded 3D emerald prism triangle
                val prism = Path().apply {
                    moveTo(px(6.8f), px(4.2f))
                    cubicTo(px(5.3f), px(3.35f), px(3.6f), px(4.45f), px(3.6f), px(6.15f))
                    lineTo(px(3.6f), px(17.85f))
                    cubicTo(px(3.6f), px(19.55f), px(5.3f), px(20.65f), px(6.8f), px(19.8f))
                    lineTo(px(18.6f), px(13.9f))
                    cubicTo(px(20.1f), px(13.15f), px(20.1f), px(10.85f), px(18.6f), px(10.1f))
                    close()
                }
                drawSculptedPath(prism, gEmerald, gEmeraldDeep)
            }

            Icon3DName.ARROW_UP_RIGHT -> {
                val arrow = Path().apply {
                    moveTo(px(7.4f), px(6.2f)); lineTo(px(17.8f), px(6.2f)); lineTo(px(17.8f), px(16.6f))
                }
                translate(0f, px(1.0f)) {
                    drawPath(arrow, ambientShadow, style = Stroke(px(2.8f), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawLine(ambientShadow, Offset(px(6.4f), px(17.6f)), Offset(px(17.2f), px(6.8f)), px(2.8f), StrokeCap.Round)
                }
                drawPath(arrow, gBlue, style = Stroke(px(2.7f), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(gBlue, Offset(px(6.4f), px(17.6f)), Offset(px(17.2f), px(6.8f)), px(2.7f), StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.55f), Offset(px(8.5f), px(6.1f)), Offset(px(16.8f), px(6.1f)), px(0.8f), StrokeCap.Round)
            }

            Icon3DName.PLUS -> {
                val cross = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(px(10.3f), px(3.8f), px(13.7f), px(20.2f), CornerRadius(px(1.7f))))
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(px(3.8f), px(10.3f), px(20.2f), px(13.7f), CornerRadius(px(1.7f))))
                }
                val merged = Path.combine(
                    PathOperation.Union,
                    Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(px(10.3f), px(3.8f), px(13.7f), px(20.2f), CornerRadius(px(1.7f)))) },
                    Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(px(3.8f), px(10.3f), px(20.2f), px(13.7f), CornerRadius(px(1.7f)))) }
                )
                drawSculptedPath(merged, gBlue, gBlueDeep, gloss = true)
            }

            Icon3DName.MINUS -> {
                drawSculptedRect(4.0f, 10.2f, 16.0f, 3.6f, 1.8f, gSilver, gSilverDeep)
            }

            Icon3DName.ROTATE_CCW -> {
                translate(0f, px(0.95f)) {
                    drawArc(
                        color = ambientShadow, startAngle = -140f, sweepAngle = 290f, useCenter = false,
                        topLeft = Offset(px(4.2f), px(4.2f)), size = Size(px(15.6f), px(15.6f)),
                        style = Stroke(px(2.5f), cap = StrokeCap.Round)
                    )
                }
                drawArc(
                    brush = gSilver, startAngle = -140f, sweepAngle = 290f, useCenter = false,
                    topLeft = Offset(px(4.2f), px(4.2f)), size = Size(px(15.6f), px(15.6f)),
                    style = Stroke(px(2.4f), cap = StrokeCap.Round)
                )
                val head = Path().apply {
                    moveTo(px(4.0f), px(3.4f))
                    lineTo(px(4.0f), px(8.8f))
                    lineTo(px(9.4f), px(8.8f))
                    close()
                }
                drawSculptedPath(head, gSilver, gSilverDeep, gloss = false)
            }

            Icon3DName.CLOSE -> {
                translate(0f, px(0.9f)) {
                    drawLine(ambientShadow, Offset(px(6.0f), px(6.0f)), Offset(px(18.0f), px(18.0f)), px(2.7f), StrokeCap.Round)
                    drawLine(ambientShadow, Offset(px(18.0f), px(6.0f)), Offset(px(6.0f), px(18.0f)), px(2.7f), StrokeCap.Round)
                }
                drawLine(gSilver, Offset(px(6.0f), px(6.0f)), Offset(px(18.0f), px(18.0f)), px(2.6f), StrokeCap.Round)
                drawLine(gSilver, Offset(px(18.0f), px(6.0f)), Offset(px(6.0f), px(18.0f)), px(2.6f), StrokeCap.Round)
            }

            Icon3DName.CHEVRON_DOWN -> {
                val p = Path().apply { moveTo(px(5.8f), px(9.0f)); lineTo(px(12f), px(15.2f)); lineTo(px(18.2f), px(9.0f)) }
                translate(0f, px(0.9f)) {
                    drawPath(p, ambientShadow, style = Stroke(px(2.7f), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawPath(p, gSilver, style = Stroke(px(2.6f), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            Icon3DName.TYPE -> {
                drawSculptedRect(2.8f, 3.0f, 18.4f, 17.2f, 4.8f, gBlue, gBlueDeep)
                // Embossed crisp white typography 'T' with subtle depth shadow
                drawEmbossedLine(Offset(px(7.4f), px(8.0f)), Offset(px(16.6f), px(8.0f)), px(2.3f))
                drawEmbossedLine(Offset(px(12.0f), px(8.0f)), Offset(px(12.0f), px(16.0f)), px(2.3f))
                drawEmbossedLine(Offset(px(9.8f), px(16.0f)), Offset(px(14.2f), px(16.0f)), px(2.2f))
            }

            Icon3DName.STICKERS -> {
                // Peeled 3D Glossy Amber Sticker
                val stickerBody = Path().apply {
                    moveTo(px(12f), px(2.4f))
                    cubicTo(px(6.7f), px(2.4f), px(2.4f), px(6.7f), px(2.4f), px(12f))
                    cubicTo(px(2.4f), px(17.3f), px(6.7f), px(21.6f), px(12f), px(21.6f))
                    cubicTo(px(13.5f), px(21.6f), px(14.9f), px(21.2f), px(16.1f), px(20.5f))
                    lineTo(px(20.5f), px(16.1f))
                    cubicTo(px(21.2f), px(14.9f), px(21.6f), px(13.5f), px(21.6f), px(12f))
                    cubicTo(px(21.6f), px(6.7f), px(17.3f), px(2.4f), px(12f), px(2.4f))
                    close()
                }
                drawSculptedPath(stickerBody, gAmber, gAmberDeep)

                // Expressive eyes & warm smile
                drawCircle(Color(0xFF78240E), px(1.3f), Offset(px(8.8f), px(10.2f)))
                drawCircle(Color.White.copy(alpha = 0.6f), px(0.45f), Offset(px(8.4f), px(9.8f)))
                drawCircle(Color(0xFF78240E), px(1.3f), Offset(px(15.2f), px(10.2f)))
                drawCircle(Color.White.copy(alpha = 0.6f), px(0.45f), Offset(px(14.8f), px(9.8f)))
                drawArc(
                    color = Color(0xFF78240E), startAngle = 22f, sweepAngle = 136f, useCenter = false,
                    topLeft = Offset(px(8.3f), px(11.4f)), size = Size(px(7.4f), px(4.8f)),
                    style = Stroke(px(1.85f), cap = StrokeCap.Round)
                )

                // Soft cast shadow beneath curled flap + curled peel corner
                val peelShadow = Path().apply {
                    moveTo(px(15.2f), px(20.7f))
                    lineTo(px(15.2f), px(16.9f))
                    cubicTo(px(15.2f), px(15.9f), px(16.0f), px(15.2f), px(17.0f), px(15.2f))
                    lineTo(px(20.7f), px(15.2f))
                    close()
                }
                drawPath(peelShadow, color = Color(0x407C2D12))
                val peelFlap = Path().apply {
                    moveTo(px(15.9f), px(20.5f))
                    lineTo(px(15.9f), px(17.4f))
                    cubicTo(px(15.9f), px(16.5f), px(16.5f), px(15.9f), px(17.4f), px(15.9f))
                    lineTo(px(20.5f), px(15.9f))
                    close()
                }
                drawPath(peelFlap, color = Color(0xFFFEF08A))
                drawPath(peelFlap, brush = gBevel, style = Stroke(px(0.55f)))
            }

            Icon3DName.PEN_TOOL -> {
                // Sleek 45° Angled 3D Purple Stylus with Rose Cap & Precision Nib
                rotate(45f, Offset(px(12f), px(12f))) {
                    drawSculptedRect(9.1f, 1.8f, 5.8f, 4.0f, 2.0f, gRose, gRoseDeep, gloss = false)
                    drawSculptedRect(9.0f, 5.2f, 6.0f, 1.6f, 0.7f, gSilver, gSilverDeep, gloss = false)
                    drawSculptedRect(9.1f, 6.6f, 5.8f, 10.6f, 1.2f, gPurple, gPurpleDeep)
                    // Specular highlight along barrel edge
                    drawLine(Color.White.copy(alpha = 0.58f), Offset(px(10.6f), px(7.6f)), Offset(px(10.6f), px(16.0f)), px(1.1f), StrokeCap.Round)
                    // Tapered Metallic Cone + Precision Ink Point
                    val tip = Path().apply {
                        moveTo(px(9.1f), px(17.1f))
                        lineTo(px(14.9f), px(17.1f))
                        lineTo(px(12.0f), px(22.5f))
                        close()
                    }
                    drawSculptedPath(tip, gSilver, gSilverDeep, gloss = false)
                    val nib = Path().apply {
                        moveTo(px(11.0f), px(20.6f))
                        lineTo(px(13.0f), px(20.6f))
                        lineTo(px(12.0f), px(22.5f))
                        close()
                    }
                    drawPath(nib, color = gPurpleDeep)
                }
            }

            Icon3DName.LAYOUT_GRID -> {
                drawSculptedRect(2.6f, 2.8f, 8.4f, 10.6f, 2.8f, gEmerald, gEmeraldDeep)
                drawSculptedRect(13.0f, 2.8f, 8.4f, 6.8f, 2.8f, gCyan, gCyanDeep)
                drawSculptedRect(13.0f, 11.4f, 8.4f, 9.6f, 2.8f, gEmerald, gEmeraldDeep, gloss = false)
                drawSculptedRect(2.6f, 15.2f, 8.4f, 5.8f, 2.8f, gCyan, gCyanDeep, gloss = false)
            }

            Icon3DName.SLIDERS_HORIZONTAL -> {
                val ys = listOf(6.4f, 12.0f, 17.6f)
                val knobs = listOf(15.2f, 8.8f, 16.4f)
                val brushes = listOf(gCyan, gBlue, gIndigo)
                val deeps = listOf(gCyanDeep, gBlueDeep, gIndigoDeep)
                ys.forEachIndexed { idx, y ->
                    drawRoundRect(
                        color = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.12f),
                        topLeft = Offset(px(2.8f), px(y - 1.3f)),
                        size = Size(px(18.4f), px(2.6f)),
                        cornerRadius = CornerRadius(px(1.3f))
                    )
                    drawRoundRect(
                        brush = brushes[idx],
                        topLeft = Offset(px(2.8f), px(y - 1.3f)),
                        size = Size(px(knobs[idx] - 2.8f), px(2.6f)),
                        cornerRadius = CornerRadius(px(1.3f))
                    )
                    drawSculptedOrb(
                        knobs[idx], y, 2.75f,
                        Brush.linearGradient(listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8))),
                        deeps[idx]
                    )
                }
            }

            Icon3DName.MUSIC -> {
                // Sculpted 3D Rose-Coral Beamed Notes
                translate(0f, px(1.0f)) {
                    drawLine(ambientShadow, Offset(px(9.4f), px(17.0f)), Offset(px(9.4f), px(5.8f)), px(2.6f), StrokeCap.Round)
                    drawLine(ambientShadow, Offset(px(19.8f), px(14.8f)), Offset(px(19.8f), px(4.0f)), px(2.6f), StrokeCap.Round)
                }
                drawLine(gRose, Offset(px(9.4f), px(17.0f)), Offset(px(9.4f), px(5.8f)), px(2.5f), StrokeCap.Round)
                drawLine(gRose, Offset(px(19.8f), px(14.8f)), Offset(px(19.8f), px(4.0f)), px(2.5f), StrokeCap.Round)

                val beam = Path().apply {
                    moveTo(px(8.5f), px(5.4f))
                    cubicTo(px(8.5f), px(4.5f), px(9.2f), px(3.8f), px(10.1f), px(3.6f))
                    lineTo(px(19.3f), px(1.8f))
                    cubicTo(px(20.3f), px(1.6f), px(21.1f), px(2.3f), px(21.1f), px(3.3f))
                    lineTo(px(21.1f), px(6.1f))
                    lineTo(px(8.5f), px(8.6f))
                    close()
                }
                drawSculptedPath(beam, gRose, gRoseDeep)

                // Glossy tilted elliptical noteheads
                rotate(-16f, Offset(px(6.8f), px(17.4f))) {
                    drawOval(ambientShadow, Offset(px(3.2f), px(15.6f)), Size(px(7.2f), px(5.4f)))
                    drawOval(gRose, Offset(px(3.2f), px(14.7f)), Size(px(7.2f), px(5.4f)))
                    drawOval(gGloss, Offset(px(4.2f), px(15.3f)), Size(px(4.2f), px(2.2f)))
                    drawOval(gBevel, Offset(px(3.2f), px(14.7f)), Size(px(7.2f), px(5.4f)), style = Stroke(px(0.6f)))
                }
                rotate(-16f, Offset(px(17.2f), px(15.2f))) {
                    drawOval(ambientShadow, Offset(px(13.6f), px(13.4f)), Size(px(7.2f), px(5.4f)))
                    drawOval(gRose, Offset(px(13.6f), px(12.5f)), Size(px(7.2f), px(5.4f)))
                    drawOval(gGloss, Offset(px(14.6f), px(13.1f)), Size(px(4.2f), px(2.2f)))
                    drawOval(gBevel, Offset(px(13.6f), px(12.5f)), Size(px(7.2f), px(5.4f)), style = Stroke(px(0.6f)))
                }
            }

            Icon3DName.POLL -> {
                drawSculptedRect(2.8f, 10.6f, 4.9f, 10.4f, 2.0f, gCyan, gCyanDeep)
                drawSculptedRect(9.55f, 3.6f, 4.9f, 17.4f, 2.0f, gBlue, gBlueDeep)
                drawSculptedRect(16.3f, 7.6f, 4.9f, 13.4f, 2.0f, gIndigo, gIndigoDeep)
            }

            Icon3DName.SLIDER_WIDGET -> {
                drawRoundRect(
                    color = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f),
                    topLeft = Offset(px(2.4f), px(10.0f)),
                    size = Size(px(19.2f), px(4.0f)),
                    cornerRadius = CornerRadius(px(2.0f))
                )
                drawRoundRect(
                    brush = gAmber,
                    topLeft = Offset(px(2.4f), px(10.0f)),
                    size = Size(px(12.8f), px(4.0f)),
                    cornerRadius = CornerRadius(px(2.0f))
                )
                drawSculptedOrb(15.2f, 12.0f, 4.4f, gRose, gRoseDeep)
            }

            Icon3DName.QUIZ -> {
                drawSculptedRect(2.8f, 2.8f, 18.4f, 18.4f, 4.8f, gPurple, gPurpleDeep)
                val chk = Path().apply {
                    moveTo(px(6.6f), px(9.2f)); lineTo(px(8.2f), px(10.8f)); lineTo(px(11.2f), px(7.6f))
                }
                drawPath(chk, Color(0xFF6EE7B7), style = Stroke(px(2.1f), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawEmbossedLine(Offset(px(13.0f), px(9.2f)), Offset(px(17.4f), px(9.2f)), px(2.0f))
                drawCircle(Color.White.copy(alpha = 0.88f), px(1.45f), Offset(px(8.6f), px(15.0f)))
                drawEmbossedLine(Offset(px(13.0f), px(15.0f)), Offset(px(17.4f), px(15.0f)), px(2.0f), Color.White.copy(alpha = 0.88f))
            }

            Icon3DName.TIMER -> {
                drawSculptedRect(9.2f, 1.8f, 5.6f, 2.4f, 1.2f, gSilver, gSilverDeep, gloss = false)
                rotate(35f, Offset(px(17.8f), px(6.5f))) {
                    drawRoundRect(gSilver, Offset(px(16.6f), px(5.1f)), Size(px(2.4f), px(2.4f)), CornerRadius(px(0.8f)))
                }
                drawSculptedOrb(12.0f, 13.4f, 8.2f, gRose, gRoseDeep)
                drawCircle(Color.White.copy(alpha = 0.16f), px(6.0f), Offset(px(12.0f), px(13.4f)))
                drawEmbossedLine(Offset(px(12.0f), px(13.4f)), Offset(px(12.0f), px(9.2f)), px(2.05f))
                drawEmbossedLine(Offset(px(12.0f), px(13.4f)), Offset(px(15.2f), px(15.2f)), px(2.05f))
                drawCircle(Color.White, px(1.3f), Offset(px(12.0f), px(13.4f)))
            }

            Icon3DName.QUESTION -> {
                val bubble = Path().apply {
                    moveTo(px(12f), px(2.6f))
                    cubicTo(px(6.6f), px(2.6f), px(2.3f), px(6.5f), px(2.3f), px(11.4f))
                    cubicTo(px(2.3f), px(14.0f), px(3.5f), px(16.3f), px(5.4f), px(17.9f))
                    lineTo(px(4.3f), px(21.2f))
                    cubicTo(px(4.15f), px(21.7f), px(4.65f), px(22.1f), px(5.1f), px(21.85f))
                    lineTo(px(8.9f), px(19.95f))
                    cubicTo(px(9.9f), px(20.15f), px(10.9f), px(20.25f), px(12f), px(20.25f))
                    cubicTo(px(17.4f), px(20.25f), px(21.7f), px(16.35f), px(21.7f), px(11.4f))
                    cubicTo(px(21.7f), px(6.5f), px(17.4f), px(2.6f), px(12f), px(2.6f))
                    close()
                }
                drawSculptedPath(bubble, gAmber, gAmberDeep)
                drawArc(
                    color = glyphShadow, startAngle = 195f, sweepAngle = 235f, useCenter = false,
                    topLeft = Offset(px(9.4f), px(7.5f)), size = Size(px(5.2f), px(4.6f)),
                    style = Stroke(px(2.1f), cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color.White, startAngle = 195f, sweepAngle = 235f, useCenter = false,
                    topLeft = Offset(px(9.4f), px(6.9f)), size = Size(px(5.2f), px(4.6f)),
                    style = Stroke(px(2.1f), cap = StrokeCap.Round)
                )
                drawCircle(glyphShadow, px(1.3f), Offset(px(12f), px(16.1f)))
                drawCircle(Color.White, px(1.3f), Offset(px(12f), px(15.5f)))
            }

            Icon3DName.MENTION -> {
                drawSculptedOrb(12f, 12f, 9.4f, gBlue, gBlueDeep)
                drawCircle(glyphShadow, px(3.0f), Offset(px(12f), px(12.6f)), style = Stroke(px(2.0f)))
                drawCircle(Color.White, px(3.0f), Offset(px(12f), px(12f)), style = Stroke(px(2.0f)))
                drawArc(
                    color = Color.White, startAngle = 38f, sweepAngle = 298f, useCenter = false,
                    topLeft = Offset(px(5.8f), px(5.8f)), size = Size(px(12.4f), px(12.4f)),
                    style = Stroke(px(1.85f), cap = StrokeCap.Round)
                )
            }

            Icon3DName.LINK -> {
                translate(0f, px(0.95f)) {
                    drawRoundRect(ambientShadow, Offset(px(2.4f), px(7.4f)), Size(px(11.4f), px(9.2f)), CornerRadius(px(4.6f)), style = Stroke(px(2.8f)))
                    drawRoundRect(ambientShadow, Offset(px(10.2f), px(7.4f)), Size(px(11.4f), px(9.2f)), CornerRadius(px(4.6f)), style = Stroke(px(2.8f)))
                }
                drawRoundRect(gEmerald, Offset(px(2.4f), px(7.4f)), Size(px(11.4f), px(9.2f)), CornerRadius(px(4.6f)), style = Stroke(px(2.7f)))
                drawRoundRect(gCyan, Offset(px(10.2f), px(7.4f)), Size(px(11.4f), px(9.2f)), CornerRadius(px(4.6f)), style = Stroke(px(2.7f)))
                drawSculptedRect(7.6f, 10.6f, 8.8f, 2.8f, 1.4f, gSilver, gSilverDeep, gloss = false)
            }

            Icon3DName.MAP_PIN -> {
                drawOval(softShadow, Offset(px(8.2f), px(20.8f)), Size(px(7.6f), px(2.2f)))
                val pin = Path().apply {
                    moveTo(px(12f), px(21.4f))
                    cubicTo(px(12f), px(21.4f), px(4.0f), px(15.0f), px(4.0f), px(10.0f))
                    cubicTo(px(4.0f), px(5.6f), px(7.6f), px(2.2f), px(12f), px(2.2f))
                    cubicTo(px(16.4f), px(2.2f), px(20.0f), px(5.6f), px(20.0f), px(10.0f))
                    cubicTo(px(20.0f), px(15.0f), px(12f), px(21.4f), px(12f), px(21.4f))
                    close()
                }
                drawSculptedPath(pin, gRose, gRoseDeep)
                drawCircle(glyphShadow, px(3.15f), Offset(px(12f), px(10.6f)))
                drawCircle(Color.White, px(3.15f), Offset(px(12f), px(10.0f)))
            }

            Icon3DName.CLOCK -> {
                drawSculptedOrb(12f, 12f, 9.4f, gEmerald, gEmeraldDeep)
                drawCircle(Color.White.copy(alpha = 0.16f), px(7.1f), Offset(px(12f), px(12f)))
                drawEmbossedLine(Offset(px(12f), px(7.2f)), Offset(px(12f), px(12.2f)), px(2.15f))
                drawEmbossedLine(Offset(px(12f), px(12.2f)), Offset(px(15.6f), px(14.3f)), px(2.15f))
                drawCircle(Color.White, px(1.3f), Offset(px(12f), px(12.2f)))
            }

            Icon3DName.LAYOUT_SINGLE -> drawSculptedRect(3.0f, 3.0f, 18.0f, 18.0f, 4.4f, gBlue, gBlueDeep)
            Icon3DName.LAYOUT_ROWS2 -> {
                drawSculptedRect(3.0f, 2.8f, 18.0f, 8.1f, 2.8f, gBlue, gBlueDeep)
                drawSculptedRect(3.0f, 13.1f, 18.0f, 8.1f, 2.8f, gIndigo, gIndigoDeep, gloss = false)
            }
            Icon3DName.LAYOUT_COLS2 -> {
                drawSculptedRect(2.8f, 3.0f, 8.1f, 18.0f, 2.8f, gBlue, gBlueDeep)
                drawSculptedRect(13.1f, 3.0f, 8.1f, 18.0f, 2.8f, gIndigo, gIndigoDeep, gloss = false)
            }
            Icon3DName.LAYOUT_GRID3 -> {
                drawSculptedRect(3.0f, 2.8f, 18.0f, 8.1f, 2.8f, gBlue, gBlueDeep)
                drawSculptedRect(3.0f, 13.1f, 8.0f, 8.1f, 2.6f, gIndigo, gIndigoDeep, gloss = false)
                drawSculptedRect(13.0f, 13.1f, 8.0f, 8.1f, 2.6f, gCyan, gCyanDeep, gloss = false)
            }
            Icon3DName.LAYOUT_GRID4 -> {
                drawSculptedRect(2.8f, 2.8f, 8.1f, 8.1f, 2.6f, gBlue, gBlueDeep)
                drawSculptedRect(13.1f, 2.8f, 8.1f, 8.1f, 2.6f, gCyan, gCyanDeep)
                drawSculptedRect(2.8f, 13.1f, 8.1f, 8.1f, 2.6f, gIndigo, gIndigoDeep, gloss = false)
                drawSculptedRect(13.1f, 13.1f, 8.1f, 8.1f, 2.6f, gBlue, gBlueDeep, gloss = false)
            }

            Icon3DName.CONTRAST -> {
                drawSculptedOrb(12f, 12f, 9.2f, gSilver, gSilverDeep)
                drawArc(gIndigo, -90f, 180f, true, Offset(px(2.8f), px(2.8f)), Size(px(18.4f), px(18.4f)))
                drawCircle(gBevel, px(9.2f), Offset(px(12f), px(12f)), style = Stroke(px(0.7f)))
            }

            Icon3DName.DROPLET -> {
                val drop = Path().apply {
                    moveTo(px(12f), px(2.2f))
                    cubicTo(px(12f), px(2.2f), px(4.6f), px(10.0f), px(4.6f), px(14.8f))
                    cubicTo(px(4.6f), px(18.8f), px(7.9f), px(21.8f), px(12f), px(21.8f))
                    cubicTo(px(16.1f), px(21.8f), px(19.4f), px(18.8f), px(19.4f), px(14.8f))
                    cubicTo(px(19.4f), px(10.0f), px(12f), px(2.2f), px(12f), px(2.2f))
                    close()
                }
                drawSculptedPath(drop, gCyan, gCyanDeep)
                rotate(16f, Offset(px(9.2f), px(14.2f))) {
                    drawOval(Color.White.copy(alpha = 0.52f), Offset(px(7.8f), px(11.4f)), Size(px(2.5f), px(5.6f)))
                }
            }

            Icon3DName.THERMOMETER -> {
                drawSculptedRect(9.2f, 2.2f, 5.6f, 14.2f, 2.8f, gSilver, gSilverDeep, gloss = false)
                drawSculptedOrb(12f, 18.0f, 4.3f, gAmber, gAmberDeep)
                drawLine(gRose, Offset(px(12f), px(8.4f)), Offset(px(12f), px(18.0f)), px(2.4f), StrokeCap.Round)
                drawCircle(Color.White.copy(alpha = 0.65f), px(1.1f), Offset(px(10.9f), px(16.9f)))
            }

            Icon3DName.APERTURE -> {
                drawSculptedOrb(12f, 12f, 9.2f, gPurple, gPurpleDeep)
                drawCircle(Color(0xFF0F172A), px(4.2f), Offset(px(12f), px(12f)))
                drawCircle(Color(0xFF38BDF8).copy(alpha = 0.72f), px(1.4f), Offset(px(10.8f), px(10.8f)))
                drawCircle(Color.White.copy(alpha = 0.72f), px(4.2f), Offset(px(12f), px(12f)), style = Stroke(px(1.25f)))
            }

            Icon3DName.EDIT -> {
                rotate(45f, Offset(px(12f), px(12f))) {
                    drawSculptedRect(9.4f, 2.4f, 5.2f, 14.0f, 1.8f, gBlue, gBlueDeep)
                    drawLine(Color.White.copy(alpha = 0.6f), Offset(px(10.8f), px(3.8f)), Offset(px(10.8f), px(15.2f)), px(1.0f), StrokeCap.Round)
                    val tip = Path().apply {
                        moveTo(px(9.4f), px(16.3f)); lineTo(px(14.6f), px(16.3f)); lineTo(px(12f), px(21.6f)); close()
                    }
                    drawSculptedPath(tip, gSilver, gSilverDeep, gloss = false)
                }
            }

            Icon3DName.LAYERS -> {
                fun diamond(cy: Float) = Path().apply {
                    moveTo(px(12f), px(cy - 4.4f))
                    lineTo(px(20.6f), px(cy))
                    lineTo(px(12f), px(cy + 4.4f))
                    lineTo(px(3.4f), px(cy))
                    close()
                }
                drawSculptedPath(diamond(16.2f), gCyan, gCyanDeep, gloss = false)
                drawSculptedPath(diamond(12.0f), gBlue, gBlueDeep, gloss = false)
                drawSculptedPath(diamond(7.6f), gIndigo, gIndigoDeep, gloss = true)
            }

            Icon3DName.LOCK, Icon3DName.UNLOCK -> {
                val isLocked = icon == Icon3DName.LOCK
                drawArc(
                    brush = gSilver,
                    startAngle = 180f,
                    sweepAngle = if (isLocked) 180f else 135f,
                    useCenter = false,
                    topLeft = Offset(px(8.3f), px(3.4f)),
                    size = Size(px(7.4f), px(8.4f)),
                    style = Stroke(px(2.35f), cap = StrokeCap.Round)
                )
                drawLine(gSilver, Offset(px(8.3f), px(7.6f)), Offset(px(8.3f), px(10.5f)), px(2.35f), StrokeCap.Round)
                if (isLocked) {
                    drawLine(gSilver, Offset(px(15.7f), px(7.6f)), Offset(px(15.7f), px(10.5f)), px(2.35f), StrokeCap.Round)
                }
                drawSculptedRect(4.3f, 10.0f, 15.4f, 11.2f, 3.6f, if (isLocked) gAmber else gEmerald, if (isLocked) gAmberDeep else gEmeraldDeep)
                drawCircle(Color.White, px(1.55f), Offset(px(12f), px(14.9f)))
                drawLine(Color.White, Offset(px(12f), px(15.5f)), Offset(px(12f), px(17.6f)), px(1.55f), StrokeCap.Round)
            }

            Icon3DName.COPY -> {
                drawSculptedRect(8.4f, 2.8f, 12.2f, 13.6f, 3.2f, gIndigo, gIndigoDeep, gloss = false)
                drawSculptedRect(3.4f, 7.4f, 12.2f, 13.6f, 3.2f, gBlue, gBlueDeep)
            }

            Icon3DName.TRASH -> {
                drawSculptedRect(8.8f, 2.0f, 6.4f, 2.6f, 1.3f, gRose, gRoseDeep, gloss = false)
                drawSculptedRect(3.2f, 4.4f, 17.6f, 2.5f, 1.25f, gRose, gRoseDeep, gloss = false)
                drawSculptedRect(5.4f, 8.0f, 13.2f, 13.8f, 3.4f, gRose, gRoseDeep)
                drawEmbossedLine(Offset(px(9.6f), px(11.2f)), Offset(px(9.6f), px(18.4f)), px(1.85f), Color.White.copy(alpha = 0.88f))
                drawEmbossedLine(Offset(px(14.4f), px(11.2f)), Offset(px(14.4f), px(18.4f)), px(1.85f), Color.White.copy(alpha = 0.88f))
            }

            Icon3DName.FLIP_HORIZONTAL -> {
                val left = Path().apply { moveTo(px(9.7f), px(4.8f)); lineTo(px(3.4f), px(18.6f)); lineTo(px(9.7f), px(18.6f)); close() }
                val right = Path().apply { moveTo(px(14.3f), px(4.8f)); lineTo(px(20.6f), px(18.6f)); lineTo(px(14.3f), px(18.6f)); close() }
                drawSculptedPath(left, gBlue, gBlueDeep)
                drawSculptedPath(right, gCyan, gCyanDeep)
                drawLine(
                    brush = gSilver,
                    start = Offset(px(12f), px(3.0f)),
                    end = Offset(px(12f), px(21.0f)),
                    strokeWidth = px(1.7f),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(px(2.4f), px(2.4f)))
                )
            }

            Icon3DName.ALIGN_CENTER -> {
                drawSculptedRect(3.4f, 4.6f, 17.2f, 2.8f, 1.4f, gSilver, gSilverDeep, gloss = false)
                drawSculptedRect(6.4f, 10.6f, 11.2f, 2.8f, 1.4f, gBlue, gBlueDeep, gloss = false)
                drawSculptedRect(4.4f, 16.6f, 15.2f, 2.8f, 1.4f, gSilver, gSilverDeep, gloss = false)
            }
            Icon3DName.ALIGN_LEFT -> {
                drawSculptedRect(3.4f, 4.6f, 17.2f, 2.8f, 1.4f, gSilver, gSilverDeep, gloss = false)
                drawSculptedRect(3.4f, 10.6f, 11.6f, 2.8f, 1.4f, gBlue, gBlueDeep, gloss = false)
                drawSculptedRect(3.4f, 16.6f, 14.6f, 2.8f, 1.4f, gSilver, gSilverDeep, gloss = false)
            }
            Icon3DName.ALIGN_RIGHT -> {
                drawSculptedRect(3.4f, 4.6f, 17.2f, 2.8f, 1.4f, gSilver, gSilverDeep, gloss = false)
                drawSculptedRect(9.0f, 10.6f, 11.6f, 2.8f, 1.4f, gBlue, gBlueDeep, gloss = false)
                drawSculptedRect(6.0f, 16.6f, 14.6f, 2.8f, 1.4f, gSilver, gSilverDeep, gloss = false)
            }

            Icon3DName.HIGHLIGHTER -> {
                rotate(45f, Offset(px(12f), px(10.8f))) {
                    drawSculptedRect(9.0f, 2.0f, 6.0f, 12.8f, 2.0f, gAmber, gAmberDeep)
                    drawSculptedRect(9.8f, 14.8f, 4.4f, 3.2f, 1.0f, gSilver, gSilverDeep, gloss = false)
                }
                drawSculptedRect(3.6f, 19.3f, 16.2f, 2.5f, 1.25f, gGold, gAmberDeep, gloss = false)
            }

            Icon3DName.SPARKLES -> {
                val big = Path().apply {
                    moveTo(px(10.6f), px(2.0f)); lineTo(px(12.7f), px(7.9f)); lineTo(px(18.6f), px(10.0f))
                    lineTo(px(12.7f), px(12.1f)); lineTo(px(10.6f), px(18.0f)); lineTo(px(8.5f), px(12.1f))
                    lineTo(px(2.6f), px(10.0f)); lineTo(px(8.5f), px(7.9f)); close()
                }
                val sm = Path().apply {
                    moveTo(px(18.2f), px(13.4f)); lineTo(px(19.2f), px(16.2f)); lineTo(px(22.0f), px(17.2f))
                    lineTo(px(19.2f), px(18.2f)); lineTo(px(18.2f), px(21.0f)); lineTo(px(17.2f), px(18.2f))
                    lineTo(px(14.4f), px(17.2f)); lineTo(px(17.2f), px(16.2f)); close()
                }
                drawSculptedPath(big, gGold, gAmberDeep)
                drawSculptedPath(sm, gPurple, gPurpleDeep, gloss = false)
                drawCircle(gCyan, px(1.1f), Offset(px(18.4f), px(4.8f)))
            }

            Icon3DName.GLOBE -> {
                drawSculptedOrb(12f, 12f, 9.4f, gBlue, gBlueDeep)
                drawOval(Color.White.copy(alpha = 0.68f), Offset(px(7.5f), px(2.8f)), Size(px(9.0f), px(18.4f)), style = Stroke(px(1.3f)))
                drawLine(Color.White.copy(alpha = 0.65f), Offset(px(3.0f), px(12f)), Offset(px(21.0f), px(12f)), px(1.25f))
                drawLine(Color.White.copy(alpha = 0.45f), Offset(px(4.8f), px(7.8f)), Offset(px(19.2f), px(7.8f)), px(1.1f))
                drawLine(Color.White.copy(alpha = 0.45f), Offset(px(4.8f), px(16.2f)), Offset(px(19.2f), px(16.2f)), px(1.1f))
            }

            Icon3DName.STAR -> {
                val star = buildStarPathCompose(px(20.4f), Offset(px(12f), px(12.2f)))
                drawSculptedPath(star, gEmerald, gEmeraldDeep)
            }

            Icon3DName.DOWNLOAD -> {
                drawEmbossedLine(Offset(px(12f), px(3.4f)), Offset(px(12f), px(14.6f)), px(2.5f))
                drawEmbossedLine(Offset(px(12f), px(14.6f)), Offset(px(7.6f), px(10.2f)), px(2.5f))
                drawEmbossedLine(Offset(px(12f), px(14.6f)), Offset(px(16.4f), px(10.2f)), px(2.5f))
                val tray = Path().apply {
                    moveTo(px(4.4f), px(16.6f)); lineTo(px(4.4f), px(18.4f))
                    cubicTo(px(4.4f), px(19.4f), px(5.2f), px(20.2f), px(6.2f), px(20.2f))
                    lineTo(px(17.8f), px(20.2f))
                    cubicTo(px(18.8f), px(20.2f), px(19.6f), px(19.4f), px(19.6f), px(18.4f))
                    lineTo(px(19.6f), px(16.6f))
                }
                translate(0f, px(0.75f)) {
                    drawPath(tray, glyphShadow, style = Stroke(px(2.4f), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawPath(tray, Color.White.copy(alpha = 0.95f), style = Stroke(px(2.4f), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            Icon3DName.SHARE -> {
                translate(0f, px(0.85f)) {
                    drawLine(ambientShadow, Offset(px(12f), px(14.8f)), Offset(px(12f), px(4.0f)), px(2.5f), StrokeCap.Round)
                    drawLine(ambientShadow, Offset(px(12f), px(4.0f)), Offset(px(8.0f), px(8.0f)), px(2.5f), StrokeCap.Round)
                    drawLine(ambientShadow, Offset(px(12f), px(4.0f)), Offset(px(16.0f), px(8.0f)), px(2.5f), StrokeCap.Round)
                }
                drawLine(gBlue, Offset(px(12f), px(14.8f)), Offset(px(12f), px(4.0f)), px(2.5f), StrokeCap.Round)
                drawLine(gBlue, Offset(px(12f), px(4.0f)), Offset(px(8.0f), px(8.0f)), px(2.5f), StrokeCap.Round)
                drawLine(gBlue, Offset(px(12f), px(4.0f)), Offset(px(16.0f), px(8.0f)), px(2.5f), StrokeCap.Round)
                val tray = Path().apply {
                    moveTo(px(4.8f), px(12.6f)); lineTo(px(4.8f), px(18.2f))
                    cubicTo(px(4.8f), px(19.4f), px(5.8f), px(20.4f), px(7.0f), px(20.4f))
                    lineTo(px(17.0f), px(20.4f))
                    cubicTo(px(18.2f), px(20.4f), px(19.2f), px(19.4f), px(19.2f), px(18.2f))
                    lineTo(px(19.2f), px(12.6f))
                }
                drawPath(tray, gSilver, style = Stroke(px(2.35f), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            Icon3DName.HEART -> {
                val h = buildHeartPathCompose(px(19.6f), Offset(px(12f), px(12.2f)))
                drawSculptedPath(h, gRose, gRoseDeep)
                rotate(-24f, Offset(px(8.2f), px(7.8f))) {
                    drawOval(Color.White.copy(alpha = 0.52f), Offset(px(6.4f), px(6.5f)), Size(px(3.6f), px(2.1f)))
                }
            }

            Icon3DName.SEND -> {
                val upperWing = Path().apply {
                    moveTo(px(21.2f), px(3.2f)); lineTo(px(2.8f), px(10.6f))
                    lineTo(px(10.2f), px(13.8f)); close()
                }
                val lowerWing = Path().apply {
                    moveTo(px(21.2f), px(3.2f)); lineTo(px(10.2f), px(13.8f))
                    lineTo(px(13.0f), px(21.2f)); close()
                }
                drawSculptedPath(lowerWing, gIndigo, gIndigoDeep, gloss = false)
                drawSculptedPath(upperWing, gBlue, gBlueDeep, gloss = true)
            }

            Icon3DName.CHECK_CIRCLE -> {
                drawSculptedOrb(12f, 12f, 9.4f, gEmerald, gEmeraldDeep)
                val chk = Path().apply { moveTo(px(7.8f), px(12.3f)); lineTo(px(10.7f), px(15.2f)); lineTo(px(16.4f), px(9.1f)) }
                translate(0f, px(0.75f)) {
                    drawPath(chk, glyphShadow, style = Stroke(px(2.45f), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawPath(chk, Color.White, style = Stroke(px(2.45f), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            Icon3DName.PALETTE -> {
                drawSculptedOrb(12f, 12f, 9.2f, gPurple, gPurpleDeep)
                drawCircle(gAmber, px(1.65f), Offset(px(7.4f), px(11.4f)))
                drawCircle(gRose, px(1.65f), Offset(px(9.8f), px(7.4f)))
                drawCircle(gCyan, px(1.65f), Offset(px(14.6f), px(7.4f)))
                drawCircle(gEmerald, px(1.65f), Offset(px(17.1f), px(10.6f)))
                drawCircle(Color(0x55000000), px(2.0f), Offset(px(15.2f), px(16.2f)))
            }

            Icon3DName.SHAPES -> {
                val tri = Path().apply {
                    moveTo(px(12f), px(2.4f)); lineTo(px(17.6f), px(11.8f)); lineTo(px(6.4f), px(11.8f)); close()
                }
                drawSculptedPath(tri, gAmber, gAmberDeep)
                drawSculptedOrb(7.5f, 16.8f, 4.1f, gRose, gRoseDeep)
                drawSculptedRect(12.6f, 13.0f, 7.8f, 7.8f, 2.2f, gBlue, gBlueDeep)
            }

            Icon3DName.INFO -> {
                drawSculptedOrb(12f, 12f, 9.4f, gBlue, gBlueDeep)
                drawCircle(glyphShadow, px(1.35f), Offset(px(12f), px(8.6f)))
                drawCircle(Color.White, px(1.35f), Offset(px(12f), px(8.0f)))
                drawEmbossedLine(Offset(px(12f), px(11.2f)), Offset(px(12f), px(16.8f)), px(2.3f))
            }

            Icon3DName.ALERT -> {
                drawSculptedOrb(12f, 12f, 9.4f, gRose, gRoseDeep)
                drawEmbossedLine(Offset(px(12f), px(7.2f)), Offset(px(12f), px(13.2f)), px(2.35f))
                drawCircle(glyphShadow, px(1.35f), Offset(px(12f), px(16.8f)))
                drawCircle(Color.White, px(1.35f), Offset(px(12f), px(16.2f)))
            }
        }
    }
}

/* ==============================================================================
   4. SHARED GEOMETRY SHAPES, COLOR MATRIX & AUDIO SYNTHESIS HELPERS
   ============================================================================== */

private fun buildHeartPathCompose(sizePx: Float, center: Offset): Path {
    val mx = { nx: Float -> center.x + (nx - 0.5f) * sizePx }
    val my = { ny: Float -> center.y + (ny - 0.5f) * sizePx }
    return Path().apply {
        moveTo(mx(0.5f), my(0.92f))
        cubicTo(mx(0.5f), my(0.92f), mx(0.04f), my(0.62f), mx(0.04f), my(0.31f))
        cubicTo(mx(0.04f), my(0.14f), mx(0.17f), my(0.03f), mx(0.31f), my(0.03f))
        cubicTo(mx(0.40f), my(0.03f), mx(0.46f), my(0.08f), mx(0.50f), my(0.15f))
        cubicTo(mx(0.54f), my(0.08f), mx(0.60f), my(0.03f), mx(0.69f), my(0.03f))
        cubicTo(mx(0.83f), my(0.03f), mx(0.96f), my(0.14f), mx(0.96f), my(0.31f))
        cubicTo(mx(0.96f), my(0.62f), mx(0.50f), my(0.92f), mx(0.50f), my(0.92f))
        close()
    }
}

private fun buildStarPathCompose(sizePx: Float, center: Offset): Path {
    val pts = listOf(
        0.5f to 0.0f, 0.618f to 0.353f, 0.988f to 0.353f, 0.685f to 0.571f, 0.801f to 0.924f,
        0.5f to 0.706f, 0.199f to 0.924f, 0.315f to 0.571f, 0.012f to 0.353f, 0.382f to 0.353f
    )
    return Path().apply {
        pts.forEachIndexed { i, (nx, ny) ->
            val x = center.x + (nx - 0.5f) * sizePx
            val y = center.y + (ny - 0.5f) * sizePx
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

private val HeartStickerShape = GenericShape { size, _ ->
    addPath(buildHeartPathCompose(size.minDimension, Offset(size.width / 2f, size.height / 2f)))
}

private val StarStickerShape = GenericShape { size, _ ->
    addPath(buildStarPathCompose(size.minDimension, Offset(size.width / 2f, size.height / 2f)))
}

private fun isLightArgb(argb: Int): Boolean {
    val r = AndroidColor.red(argb)
    val g = AndroidColor.green(argb)
    val b = AndroidColor.blue(argb)
    return (r * 0.299f + g * 0.587f + b * 0.114f) > 205f
}

/**
 * Builds an Android ColorMatrix matching CSS brightness(), contrast(), saturate(),
 * sepia(), and grayscale() filters identically for both live preview and PNG export.
 */
private fun buildFilterAndroidColorMatrix(f: FilterState): AndroidColorMatrix {
    val result = AndroidColorMatrix()

    // 1. Brightness
    val b = f.brightness / 100f
    result.postConcat(
        AndroidColorMatrix(
            floatArrayOf(
                b, 0f, 0f, 0f, 0f,
                0f, b, 0f, 0f, 0f,
                0f, 0f, b, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )

    // 2. Contrast
    val c = f.contrast / 100f
    val t = 128f * (1f - c)
    result.postConcat(
        AndroidColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )

    // 3. Saturation (combined with grayscale)
    val effectiveSat = (f.saturate / 100f) * (1f - f.grayscale / 100f)
    val satMat = AndroidColorMatrix().apply { setSaturation(effectiveSat) }
    result.postConcat(satMat)

    // 4. Warmth / Sepia
    val sep = (f.sepia / 100f).coerceIn(0f, 1f)
    if (sep > 0f) {
        val inv = 1f - sep
        result.postConcat(
            AndroidColorMatrix(
                floatArrayOf(
                    inv + sep * 0.393f, sep * 0.769f, sep * 0.189f, 0f, 0f,
                    sep * 0.349f, inv + sep * 0.686f, sep * 0.168f, 0f, 0f,
                    sep * 0.272f, sep * 0.534f, inv + sep * 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }

    return result
}

private fun buildComposeColorFilter(f: FilterState): ColorFilter {
    val cm = buildFilterAndroidColorMatrix(f)
    return ColorFilter.colorMatrix(ColorMatrix(cm.array))
}

/**
 * Loads a Bitmap safely from a content Uri with downsampling and EXIF orientation
 * correction so Android camera portrait photos always appear right-side up.
 */
private suspend fun decodeBitmapSafely(context: Context, uri: Uri, maxDim: Int = 2048): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val orientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } else {
                ExifInterface.ORIENTATION_NORMAL
            }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            var sample = 1
            while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return@withContext null

            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
                    else -> return@withContext decoded
                }
            }
            val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (oriented !== decoded && !decoded.isRecycled) decoded.recycle()
            oriented
        } catch (_: Throwable) {
            null
        }
    }

/**
 * Synthesizes a smooth ambient triad chord on a background coroutine via AudioTrack
 * (zero external audio files needed, identical harmonic feel to Web Audio version).
 */
private suspend fun playAmbientChordAudio(baseFreq: Double) = withContext(Dispatchers.Default) {
    try {
        val sampleRate = 22050
        val durationSec = 1.4
        val totalSamples = (sampleRate * durationSec).toInt()
        val pcm = ShortArray(totalSamples)
        val freqs = doubleArrayOf(baseFreq, baseFreq * 1.25, baseFreq * 1.5)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val env = when {
                t < 0.14 -> t / 0.14
                else -> kotlin.math.exp(-2.8 * (t - 0.14))
            }
            var sample = 0.0
            freqs.forEachIndexed { idx, f ->
                val delayT = t - idx * 0.07
                if (delayT > 0.0) {
                    sample += sin(2.0 * PI * f * delayT)
                }
            }
            val normalized = (sample / 3.0) * env * 0.35
            pcm[i] = (normalized * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.play()
        delay((durationSec * 1000).toLong() + 80L)
        track.stop()
        track.release()
    } catch (_: Throwable) {
        // Ignore safely if device audio hardware is unavailable
    }
}

/* ==============================================================================
   5. TACTILE BORDERLESS APPLE HIG BUTTON HELPER
   ============================================================================== */

@Composable
private fun HigPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "higPressScale"
    )
    val alpha = if (!enabled) 0.28f else if (pressed) 0.65f else 1f

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

/* ==============================================================================
   6. MAIN STORY STUDIO SCREEN (COMPLETE STATE, STAGE, SHEETS & PLAYER)
   ============================================================================== */

private data class StudioDeletePrompt(
    val title: String,
    val message: String,
    val confirmText: String = "Delete",
    val onConfirm: () -> Unit
)

@Composable
fun StoryStudioScreen(
    modifier: Modifier = Modifier,
    initialPhotoUri: Uri? = null,
    initialIsDark: Boolean = com.chroma.studio.ui.theme.LocalOscellionColors.current.isDark,
    onNavigateBack: (() -> Unit)? = null,
    onPublishStory: ((List<StoryPublishSlide>, StoryAudience) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Theme & Audience State (Apple HIG Light & Dark Mode)
    var isDark by remember(initialIsDark) { mutableStateOf(initialIsDark) }
    val colors = if (isDark) DarkHigColors else LightHigColors
    var audience by remember { mutableStateOf(StoryAudience.PUBLIC) }

    // Multi-slide storyboard & Per-Slide Undo/Redo history stacks
    val slides = remember { mutableStateListOf(StorySlide()) }
    var currentSlideIndex by remember { mutableIntStateOf(0) }
    val currentSlide = slides[currentSlideIndex.coerceIn(0, slides.lastIndex)]

    val slideHistories = remember {
        mutableStateMapOf(slides[0].id to SlideHistory(listOf(slides[0]), 0))
    }
    val currentHistory = slideHistories[currentSlide.id] ?: SlideHistory(listOf(currentSlide), 0)
    val canUndo = currentHistory.index > 0
    val canRedo = currentHistory.index < currentHistory.stack.lastIndex

    // Bitmap cache keyed by Uri string
    val bitmapCache = remember { mutableStateMapOf<String, Bitmap>() }
    var isExporting by remember { mutableStateOf(false) }

    // Selection & Active UI Modals/Sheets
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var activeSheet by remember { mutableStateOf(ActiveSheet.NONE) }
    var isDrawingActive by remember { mutableStateOf(false) }
    var isDrawingPanelCollapsed by remember { mutableStateOf(false) }

    // Drawing Tool State
    var brushType by remember { mutableStateOf(BrushType.PEN) }
    var drawLineStyle by remember { mutableStateOf(StrokeLineStyle.SOLID) }
    var drawGlowEnabled by remember { mutableStateOf(false) }
    var autoShapeEnabled by remember { mutableStateOf(true) }
    var drawColorArgb by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var drawEmoji by remember { mutableStateOf("✨") }
    var drawSizeDp by remember { mutableFloatStateOf(6f) }
    var activeLiveStroke by remember { mutableStateOf<DrawStroke?>(null) }

    // Text Composer State
    var showTextComposer by remember { mutableStateOf(false) }
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var draftText by remember { mutableStateOf("") }
    var draftFont by remember { mutableStateOf(TextFontStyle.SYSTEM) }
    var draftTextColorArgb by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var draftBgMode by remember { mutableStateOf(TextBgMode.CLEAN) }
    var draftAlign by remember { mutableStateOf(TextAlignmentMode.CENTER) }
    var draftSizeSp by remember { mutableFloatStateOf(32f) }
    var draftAnim by remember { mutableStateOf(TextAnimStyle.NONE) }
    var lastAnimatedItemId by remember { mutableStateOf<String?>(null) }

    // Widget Edit Dialog State
    var editingWidget by remember { mutableStateOf<WidgetStoryItem?>(null) }

    // Delete Confirmation State
    var pendingDeleteConfirmation by remember { mutableStateOf<StudioDeletePrompt?>(null) }

    // Stage metrics & Drag HUD state
    var stageSizePx by remember { mutableStateOf(IntSize(360, 640)) }
    var isDraggingItem by remember { mutableStateOf(false) }
    var showSnapV by remember { mutableStateOf(false) }
    var showSnapH by remember { mutableStateOf(false) }
    var isOverTrash by remember { mutableStateOf(false) }
    var activeTransformHint by remember { mutableStateOf<String?>(null) }

    // Live Pixel Eyedropper State
    var eyedropperTarget by remember { mutableStateOf(EyedropperTarget.NONE) }
    var eyedropperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var eyedropperPos by remember { mutableStateOf<Offset?>(null) }
    var sampledColorArgb by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var customSampledArgb by remember { mutableStateOf<Int?>(null) }

    // Fullscreen Multi-Slide Story Player State
    var showPreviewPlayer by remember { mutableStateOf(false) }

    // Toast Notification State
    var toastMsg by remember { mutableStateOf<String?>(null) }
    var toastIcon by remember { mutableStateOf(Icon3DName.CHECK_CIRCLE) }
    var toastJob by remember { mutableStateOf<Job?>(null) }

    fun showToast(message: String, icon: Icon3DName = Icon3DName.CHECK_CIRCLE) {
        toastMsg = message
        toastIcon = icon
        toastJob?.cancel()
        toastJob = scope.launch {
            delay(2100)
            toastMsg = null
        }
    }

    fun updateCurrentSlide(commitHistory: Boolean = true, transform: (StorySlide) -> StorySlide) {
        val current = slides[currentSlideIndex]
        val updated = transform(current)
        slides[currentSlideIndex] = updated
        if (commitHistory) {
            val prev = slideHistories[updated.id] ?: SlideHistory(listOf(current), 0)
            val truncated = prev.stack.take(prev.index + 1) + updated
            val capped = if (truncated.size > 30) truncated.drop(1) else truncated
            slideHistories[updated.id] = SlideHistory(capped, capped.lastIndex)
        }
    }

    fun ensureHistoryForSlide(slide: StorySlide) {
        if (!slideHistories.containsKey(slide.id)) {
            slideHistories[slide.id] = SlideHistory(listOf(slide), 0)
        }
    }

    fun canRefineLastStroke(): Boolean {
        val last = currentSlide.strokes.lastOrNull() ?: return false
        return last.brush != BrushType.EMOJI &&
            last.brush != BrushType.ERASER &&
            last.recognizedShape == null &&
            last.points.size >= 4
    }

    fun refineLastStroke() {
        val last = currentSlide.strokes.lastOrNull() ?: return
        if (last.brush == BrushType.EMOJI || last.brush == BrushType.ERASER) {
            showToast("Select Pen, Marker, Glow, or Aurora stroke to refine", Icon3DName.INFO)
            return
        }
        val w = stageSizePx.width.toFloat().coerceAtLeast(360f)
        val h = stageSizePx.height.toFloat().coerceAtLeast(640f)
        val res = StoryShapeRecognizer.recognize(last.points, w, h)
        if (res != null) {
            val refinedStroke = last.copy(points = res.refinedPoints, recognizedShape = res.type)
            updateCurrentSlide { s -> s.copy(strokes = s.strokes.dropLast(1) + refinedStroke) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showToast("Refined: ${res.type.label}", Icon3DName.SPARKLES)
        } else {
            showToast("Draw a clearer line, arc, circle, triangle, or box", Icon3DName.INFO)
        }
    }

    fun undo() {
        val hist = slideHistories[currentSlide.id] ?: return
        if (hist.index > 0) {
            val nextIdx = hist.index - 1
            slideHistories[currentSlide.id] = hist.copy(index = nextIdx)
            slides[currentSlideIndex] = hist.stack[nextIdx]
            selectedItemId = null
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun redo() {
        val hist = slideHistories[currentSlide.id] ?: return
        if (hist.index < hist.stack.lastIndex) {
            val nextIdx = hist.index + 1
            slideHistories[currentSlide.id] = hist.copy(index = nextIdx)
            slides[currentSlideIndex] = hist.stack[nextIdx]
            selectedItemId = null
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun closeAllSheets() {
        activeSheet = ActiveSheet.NONE
        isDrawingActive = false
        isDrawingPanelCollapsed = false
    }

    fun openTextComposer(existing: TextStoryItem? = null) {
        closeAllSheets()
        if (existing != null) {
            editingTextId = existing.id
            draftText = existing.text
            draftFont = existing.font
            draftTextColorArgb = existing.colorArgb
            draftBgMode = existing.bgMode
            draftAlign = existing.align
            draftSizeSp = existing.sizeSp
            draftAnim = existing.anim
        } else {
            editingTextId = null
            draftText = ""
            draftFont = TextFontStyle.SYSTEM
            draftTextColorArgb = AndroidColor.WHITE
            draftBgMode = TextBgMode.CLEAN
            draftAlign = TextAlignmentMode.CENTER
            draftSizeSp = 32f
            draftAnim = TextAnimStyle.NONE
        }
        showTextComposer = true
    }

    fun startEyedropper(target: EyedropperTarget) {
        closeAllSheets()
        showTextComposer = false
        selectedItemId = null
        val w = max(180, stageSizePx.width)
        val h = max(320, stageSizePx.height)
        val stageWdp = with(density) { stageSizePx.width.toDp().value }.coerceAtLeast(180f)
        val stageHdp = with(density) { stageSizePx.height.toDp().value }.coerceAtLeast(320f)
        scope.launch {
            val bmp = withContext(Dispatchers.Default) {
                renderCompositeBitmap(
                    slide = currentSlide,
                    bitmaps = bitmapCache,
                    outW = w,
                    outH = h,
                    stageWdp = stageWdp,
                    stageHdp = stageHdp
                )
            }
            eyedropperBitmap?.let { if (!it.isRecycled) it.recycle() }
            eyedropperBitmap = bmp
            eyedropperPos = null
            eyedropperTarget = target
        }
    }

    fun cancelEyedropper() {
        val prev = eyedropperTarget
        eyedropperTarget = EyedropperTarget.NONE
        eyedropperPos = null
        eyedropperBitmap?.let { if (!it.isRecycled) it.recycle() }
        eyedropperBitmap = null
        if (prev == EyedropperTarget.TEXT) {
            showTextComposer = true
        } else if (prev == EyedropperTarget.DRAW) {
            isDrawingActive = true
            isDrawingPanelCollapsed = false
            activeSheet = ActiveSheet.DRAW
        }
    }

    // Native Android System Back Button & Predictive Gesture Support
    val hasActiveOverlay = showPreviewPlayer ||
        editingWidget != null ||
        showTextComposer ||
        eyedropperTarget != EyedropperTarget.NONE ||
        activeSheet != ActiveSheet.NONE ||
        isDrawingActive ||
        selectedItemId != null

    BackHandler(enabled = hasActiveOverlay || onNavigateBack != null) {
        when {
            showPreviewPlayer -> showPreviewPlayer = false
            editingWidget != null -> editingWidget = null
            showTextComposer -> showTextComposer = false
            eyedropperTarget != EyedropperTarget.NONE -> cancelEyedropper()
            activeSheet != ActiveSheet.NONE || isDrawingActive -> closeAllSheets()
            selectedItemId != null -> selectedItemId = null
            onNavigateBack != null -> onNavigateBack()
        }
    }

    // Preload initial image URI if launched with a gallery/camera photo
    LaunchedEffect(initialPhotoUri) {
        val uri = initialPhotoUri ?: return@LaunchedEffect
        val bmp = decodeBitmapSafely(context, uri)
        if (bmp != null) {
            val key = uri.toString()
            bitmapCache[key] = bmp
            updateCurrentSlide(commitHistory = false) { s ->
                s.copy(
                    layout = LayoutType.NONE,
                    photo = PhotoState(uriKey = key, fitMode = PhotoFitMode.CONTAIN)
                )
            }
        }
    }

    // Unified Enhanced Oscellion Gallery Pickers (Background Photo, Photo Sticker Cutout, Layout Grid Cell)
    var pendingLayoutCell by remember { mutableStateOf<Int?>(null) }

    val bgPhotoPicker = com.chroma.studio.ui.components.rememberOscellionMediaPicker(
        title = "Choose Story Media",
        mediaFilter = com.chroma.studio.ui.components.GalleryMediaTypeFilter.PHOTOS,
        showAlbumsTab = true
    ) { uri ->
        scope.launch {
            val bmp = decodeBitmapSafely(context, uri)
            if (bmp != null) {
                val key = uri.toString()
                bitmapCache[key] = bmp
                updateCurrentSlide { s ->
                    s.copy(
                        layout = LayoutType.NONE,
                        photo = PhotoState(uriKey = key, fitMode = PhotoFitMode.CONTAIN)
                    )
                }
                showToast("Media added", Icon3DName.IMAGES)
            }
        }
    }

    val stickerPhotoPicker = com.chroma.studio.ui.components.rememberOscellionMediaPicker(
        title = "Add Photo Sticker",
        mediaFilter = com.chroma.studio.ui.components.GalleryMediaTypeFilter.PHOTOS,
        showAlbumsTab = true
    ) { uri ->
        scope.launch {
            val bmp = decodeBitmapSafely(context, uri, maxDim = 1024)
            if (bmp != null) {
                val key = uri.toString()
                bitmapCache[key] = bmp
                val item = PhotoStickerItem(uriKey = key, shape = PhotoStickerShape.ROUNDED)
                updateCurrentSlide { it.copy(items = it.items + item) }
                selectedItemId = item.id
                closeAllSheets()
                showToast("Tap Shape on toolbar to change frame", Icon3DName.IMAGES)
            }
        }
    }

    val layoutCellPicker = com.chroma.studio.ui.components.rememberOscellionMediaPicker(
        title = "Fill Layout Cell",
        mediaFilter = com.chroma.studio.ui.components.GalleryMediaTypeFilter.PHOTOS,
        showAlbumsTab = true
    ) { uri ->
        val cellIdx = pendingLayoutCell
        if (cellIdx != null) {
            scope.launch {
                val bmp = decodeBitmapSafely(context, uri, maxDim = 1440)
                if (bmp != null) {
                    val key = uri.toString()
                    bitmapCache[key] = bmp
                    updateCurrentSlide { s ->
                        s.copy(layoutImages = s.layoutImages + (cellIdx to key))
                    }
                }
            }
        }
    }

    fun launchPhotoPicker(launcher: com.chroma.studio.ui.components.OscellionGalleryLauncher) {
        launcher.launch()
    }

    CompositionLocalProvider(LocalHigColors provides colors) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.bgApp)
        ) {
            // ==================================================================
            // MAIN MOBILE EDITOR SHELL
            // ==================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // --------------------------------------------------------------
                // TOP NAVIGATION BAR (ZERO ICON BACKGROUNDS)
                // --------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Close (when hosted in nav) + Media + Undo + Redo
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onNavigateBack != null) {
                            HigPressable(
                                onClick = onNavigateBack,
                                modifier = Modifier.size(width = 36.dp, height = 40.dp)
                            ) {
                                Icon3D(Icon3DName.CLOSE, size = 19.dp)
                            }
                        }

                        HigPressable(
                            onClick = { launchPhotoPicker(bgPhotoPicker) },
                            modifier = Modifier
                                .height(40.dp)
                                .padding(horizontal = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon3D(Icon3DName.IMAGE_PLUS, size = 22.dp)
                                Text(
                                    text = "Media",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HigPressable(
                            onClick = ::undo,
                            enabled = canUndo,
                            modifier = Modifier.size(width = 36.dp, height = 40.dp)
                        ) {
                            Icon3D(Icon3DName.UNDO, size = 20.dp)
                        }

                        HigPressable(
                            onClick = ::redo,
                            enabled = canRedo,
                            modifier = Modifier.size(width = 36.dp, height = 40.dp)
                        ) {
                            Icon3D(Icon3DName.REDO, size = 20.dp)
                        }
                    }

                    // Right: Backdrop Orb + Eyedropper + Theme + Play + Share
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val backdrop = STORY_BACKDROPS[currentSlide.backdropIndex]
                        HigPressable(
                            onClick = {
                                val next = (currentSlide.backdropIndex + 1) % STORY_BACKDROPS.size
                                updateCurrentSlide { it.copy(backdropIndex = next) }
                                showToast("Backdrop: ${STORY_BACKDROPS[next].name}", Icon3DName.PALETTE)
                            },
                            modifier = Modifier.size(width = 36.dp, height = 40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(19.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(backdrop.startColor, backdrop.endColor)
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                            )
                        }

                        HigPressable(
                            onClick = { startEyedropper(EyedropperTarget.TOAST) },
                            modifier = Modifier.size(width = 36.dp, height = 40.dp)
                        ) {
                            Icon3D(Icon3DName.PIPETTE, size = 20.dp)
                        }

                        HigPressable(
                            onClick = { isDark = !isDark },
                            modifier = Modifier.size(width = 36.dp, height = 40.dp)
                        ) {
                            Icon3D(if (isDark) Icon3DName.SUN else Icon3DName.MOON, size = 20.dp)
                        }

                        HigPressable(
                            onClick = {
                                selectedItemId = null
                                closeAllSheets()
                                showPreviewPlayer = true
                            },
                            modifier = Modifier.size(width = 36.dp, height = 40.dp)
                        ) {
                            Icon3D(Icon3DName.PLAY, size = 20.dp)
                        }

                        HigPressable(
                            onClick = {
                                selectedItemId = null
                                closeAllSheets()
                                activeSheet = ActiveSheet.SHARE
                            },
                            modifier = Modifier
                                .height(40.dp)
                                .padding(start = 6.dp, end = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Share",
                                    color = colors.accent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon3D(Icon3DName.ARROW_UP_RIGHT, size = 16.dp)
                            }
                        }
                    }
                }

                // --------------------------------------------------------------
                // MULTI-SLIDE STORYBOARD STRIP
                // --------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        slides.forEachIndexed { idx, slideItem ->
                            val isCurrent = idx == currentSlideIndex
                            val slideBg = STORY_BACKDROPS[slideItem.backdropIndex]
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isCurrent) colors.bgSurface else Color.Transparent)
                                    .border(
                                        0.75.dp,
                                        if (isCurrent) colors.accent else colors.separator,
                                        CircleShape
                                    )
                                    .clickable {
                                        if (idx != currentSlideIndex) {
                                            currentSlideIndex = idx
                                            selectedItemId = null
                                            ensureHistoryForSlide(slideItem)
                                        }
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(slideBg.startColor, slideBg.endColor)))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                                )
                                Text(
                                    text = "Slide ${idx + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCurrent) colors.accent else colors.textSecondary
                                )
                                if (slides.size > 1 && isCurrent) {
                                    HigPressable(
                                        onClick = {
                                            val slideToRemove = slides.getOrNull(idx)
                                            if (slideToRemove != null && slides.size > 1) {
                                                pendingDeleteConfirmation = StudioDeletePrompt(
                                                    title = "Delete Slide ${idx + 1}?",
                                                    message = "Are you sure you want to remove this slide from your story?"
                                                ) {
                                                    val removeIdx = slides.indexOfFirst { it.id == slideToRemove.id }
                                                    if (removeIdx >= 0 && slides.size > 1) {
                                                        val removed = slides.removeAt(removeIdx)
                                                        slideHistories.remove(removed.id)
                                                        currentSlideIndex = currentSlideIndex.coerceIn(0, slides.lastIndex)
                                                        selectedItemId = null
                                                        ensureHistoryForSlide(slides[currentSlideIndex])
                                                        showToast("Slide removed", Icon3DName.TRASH)
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(14.dp)
                                    ) {
                                        Icon3D(Icon3DName.CLOSE, size = 11.dp)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HigPressable(
                            onClick = {
                                if (slides.size >= 8) {
                                    showToast("Maximum 8 slides reached", Icon3DName.INFO)
                                } else {
                                    val fresh = StorySlide()
                                    slides.add(fresh)
                                    currentSlideIndex = slides.lastIndex
                                    selectedItemId = null
                                    ensureHistoryForSlide(fresh)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showToast("Created Slide ${slides.size}", Icon3DName.LAYERS)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon3D(Icon3DName.PLUS, size = 14.dp)
                                Text("Slide", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        HigPressable(
                            onClick = {
                                updateCurrentSlide { s -> StorySlide(id = s.id) }
                                selectedItemId = null
                                showToast("Slide reset · Tap Undo to restore", Icon3DName.ROTATE_CCW)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon3D(Icon3DName.ROTATE_CCW, size = 16.dp)
                        }
                    }
                }

                // --------------------------------------------------------------
                // EXACT 9:16 STORY STAGE
                // --------------------------------------------------------------
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val targetRatio = 9f / 16f
                    val availRatio = maxWidth / maxHeight
                    val stageW = if (availRatio > targetRatio) maxHeight * targetRatio else maxWidth
                    val stageH = if (availRatio > targetRatio) maxHeight else maxWidth / targetRatio

                    val backdrop = STORY_BACKDROPS[currentSlide.backdropIndex]
                    val composeColorFilter = remember(currentSlide.filters) {
                        buildComposeColorFilter(currentSlide.filters)
                    }

                    Box(
                        modifier = Modifier
                            .size(width = stageW, height = stageH)
                            .shadow(16.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .border(0.5.dp, colors.separator, RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(backdrop.startColor, backdrop.endColor),
                                    start = Offset.Zero,
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                            .onGloballyPositioned { coords -> stageSizePx = coords.size }
                            .pointerInput(isDrawingActive) {
                                if (!isDrawingActive) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        selectedItemId = null
                                    }
                                }
                            }
                    ) {
                        // Layer 2: Single Photo OR Empty Hint OR Layout Grid
                        if (currentSlide.layout != LayoutType.NONE) {
                            StoryLayoutGridLayer(
                                layout = currentSlide.layout,
                                images = currentSlide.layoutImages,
                                bitmapCache = bitmapCache,
                                colorFilter = composeColorFilter,
                                onCellClick = { cellIdx ->
                                    pendingLayoutCell = cellIdx
                                    launchPhotoPicker(layoutCellPicker)
                                }
                            )
                        } else if (currentSlide.photo.uriKey != null && bitmapCache.containsKey(currentSlide.photo.uriKey)) {
                            val bmp = bitmapCache[currentSlide.photo.uriKey]!!
                            val imageBmp = remember(bmp) { bmp.asImageBitmap() }
                            val photo = currentSlide.photo
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(currentSlide.id, isDrawingActive) {
                                        if (isDrawingActive) return@pointerInput
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            selectedItemId = null
                                            var photoMoved = false
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (event.changes.none { it.pressed }) break
                                                val pan = event.calculatePan()
                                                val zoom = event.calculateZoom()
                                                if (pan != Offset.Zero || zoom != 1f) {
                                                    if (hypot(pan.x.toDouble(), pan.y.toDouble()) > 1.0 || abs(zoom - 1f) > 0.005f) {
                                                        photoMoved = true
                                                    }
                                                    event.changes.forEach { it.consume() }
                                                    val dxDp = with(density) { pan.x.toDp().value }
                                                    val dyDp = with(density) { pan.y.toDp().value }
                                                    updateCurrentSlide(commitHistory = false) { s ->
                                                        val p = s.photo
                                                        s.copy(
                                                            photo = p.copy(
                                                                xDp = p.xDp + dxDp,
                                                                yDp = p.yDp + dyDp,
                                                                scale = (p.scale * zoom).coerceIn(0.35f, 3.5f)
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                            if (photoMoved) {
                                                updateCurrentSlide(commitHistory = true) { it }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = imageBmp,
                                    contentDescription = "Story Background Photo",
                                    contentScale = if (photo.fitMode == PhotoFitMode.COVER) ContentScale.Crop else ContentScale.Fit,
                                    colorFilter = composeColorFilter,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            translationX = with(density) { photo.xDp.dp.toPx() }
                                            translationY = with(density) { photo.yDp.dp.toPx() }
                                            scaleX = photo.scale * (if (photo.flipX) -1f else 1f)
                                            scaleY = photo.scale
                                        }
                                )
                            }
                        } else {
                            // Clean Empty Stage Hint (Adapts to Light/Dark Backdrop)
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clickable { launchPhotoPicker(bgPhotoPicker) }
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon3D(Icon3DName.IMAGE_PLUS, size = 40.dp)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Tap to add media",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (backdrop.isLight) Color(0xE0000000) else Color(0xEBFFFFFF)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Or create with Layouts, Typography, Interactive Stickers & Markup",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = if (backdrop.isLight) Color(0x85000000) else Color(0x8CFFFFFF),
                                    modifier = Modifier.widthIn(max = 210.dp)
                                )
                            }
                        }

                        // Layer 4: Optical Vignette Overlay
                        if (currentSlide.filters.vignette > 0f) {
                            val vigAlpha = (currentSlide.filters.vignette / 100f).coerceIn(0f, 0.85f)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colorStops = arrayOf(
                                                0.42f to Color.Transparent,
                                                1.0f to Color.Black.copy(alpha = vigAlpha)
                                            )
                                        )
                                    )
                            )
                        }

                        // Layer 5: Isolated Offscreen Markup Canvas (Eraser never punches through photo!)
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .then(
                                    if (isDrawingActive) {
                                        Modifier.pointerInput(
                                            brushType,
                                            drawLineStyle,
                                            drawGlowEnabled,
                                            autoShapeEnabled,
                                            drawColorArgb,
                                            drawEmoji,
                                            drawSizeDp
                                        ) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown()
                                                down.consume()
                                                val w = size.width.toFloat().coerceAtLeast(1f)
                                                val h = size.height.toFloat().coerceAtLeast(1f)
                                                val startPt = NormalizedPoint(down.position.x / w, down.position.y / h)
                                                var stroke = DrawStroke(
                                                    brush = brushType,
                                                    colorArgb = drawColorArgb,
                                                    emoji = drawEmoji,
                                                    sizeDp = drawSizeDp,
                                                    lineStyle = drawLineStyle,
                                                    glowEnabled = drawGlowEnabled,
                                                    points = listOf(startPt)
                                                )
                                                activeLiveStroke = stroke

                                                var holdJob: Job? = null
                                                var snapped = false
                                                var anchorPos = down.position

                                                fun scheduleHoldSnap() {
                                                    holdJob?.cancel()
                                                    if (!autoShapeEnabled || snapped || brushType == BrushType.EMOJI || brushType == BrushType.ERASER) {
                                                        return
                                                    }
                                                    holdJob = scope.launch {
                                                        delay(380L)
                                                        val recognized = StoryShapeRecognizer.recognize(stroke.points, w, h)
                                                        if (recognized != null && activeLiveStroke != null) {
                                                            snapped = true
                                                            stroke = stroke.copy(
                                                                points = recognized.refinedPoints,
                                                                recognizedShape = recognized.type
                                                            )
                                                            activeLiveStroke = stroke
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            showToast("Snapped: ${recognized.type.label}", Icon3DName.SPARKLES)
                                                        }
                                                    }
                                                }

                                                try {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.firstOrNull() ?: break
                                                        if (!change.pressed) {
                                                            change.consume()
                                                            break
                                                        }
                                                        if (change.position != change.previousPosition) {
                                                            change.consume()
                                                            val deltaPx = hypot(
                                                                (change.position.x - anchorPos.x).toDouble(),
                                                                (change.position.y - anchorPos.y).toDouble()
                                                            )
                                                            if (!snapped && deltaPx >= 4.0) {
                                                                anchorPos = change.position
                                                                val pt = NormalizedPoint(change.position.x / w, change.position.y / h)
                                                                stroke = stroke.copy(points = stroke.points + pt)
                                                                activeLiveStroke = stroke
                                                                scheduleHoldSnap()
                                                            }
                                                        }
                                                    }
                                                } finally {
                                                    holdJob?.cancel()
                                                }

                                                activeLiveStroke = null
                                                updateCurrentSlide { s -> s.copy(strokes = s.strokes + stroke) }
                                            }
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            val allStrokes = if (activeLiveStroke != null) {
                                currentSlide.strokes + activeLiveStroke!!
                            } else {
                                currentSlide.strokes
                            }
                            drawStrokesInCompose(allStrokes, density.density)
                        }

                        // Layer 6: Interactive Draggable / Pinchable / Rotatable Objects
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            currentSlide.items.forEach { item ->
                                val isSelected = item.id == selectedItemId
                                DraggableStoryObject(
                                    item = item,
                                    isSelected = isSelected,
                                    isDrawingActive = isDrawingActive,
                                    animateEntrance = item.id == lastAnimatedItemId,
                                    stageHeightPx = stageSizePx.height.toFloat(),
                                    bitmapCache = bitmapCache,
                                    onSelect = { selectedItemId = item.id },
                                    onDragStart = {
                                        isDraggingItem = true
                                    },
                                    onTransformDelta = { dxDp, dyDp, zoom, rotDelta, screenYInStage ->
                                        val nowOverTrash = screenYInStage > stageSizePx.height - with(density) { 72.dp.toPx() }
                                        if (nowOverTrash && !isOverTrash) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        isOverTrash = nowOverTrash
                                        updateCurrentSlide(commitHistory = false) { s ->
                                            s.copy(
                                                items = s.items.map { live ->
                                                    if (live.id != item.id) {
                                                        live
                                                    } else {
                                                        var nextX = live.xDp + dxDp
                                                        var nextY = live.yDp + dyDp
                                                        if (abs(nextX) < 8f) {
                                                            if (!showSnapV) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            nextX = 0f
                                                            showSnapV = true
                                                        } else {
                                                            showSnapV = false
                                                        }
                                                        if (abs(nextY) < 8f) {
                                                            if (!showSnapH) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            nextY = 0f
                                                            showSnapH = true
                                                        } else {
                                                            showSnapH = false
                                                        }
                                                        val nextScale = (live.scale * zoom).coerceIn(0.35f, 3.2f)
                                                        var nextRot = live.rotation + rotDelta
                                                        if (abs(nextRot % 360f) < 4f) {
                                                            if (live.rotation != 0f) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            nextRot = 0f
                                                        }
                                                        if (abs(zoom - 1f) > 0.004f || abs(rotDelta) > 0.4f) {
                                                            activeTransformHint = "${(nextScale * 100).roundToInt()}% · ${nextRot.roundToInt()}°"
                                                        }
                                                        live.copyTransform(
                                                            xDp = nextX,
                                                            yDp = nextY,
                                                            scale = nextScale,
                                                            rotation = nextRot
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    onDragEnd = { shouldDelete ->
                                        isDraggingItem = false
                                        showSnapV = false
                                        showSnapH = false
                                        isOverTrash = false
                                        activeTransformHint = null
                                        if (shouldDelete) {
                                            updateCurrentSlide { s -> s.copy(items = s.items.filterNot { it.id == item.id }) }
                                            selectedItemId = null
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showToast("Deleted", Icon3DName.TRASH)
                                        } else {
                                            updateCurrentSlide(commitHistory = true) { it }
                                        }
                                    },
                                    onDoubleTap = {
                                        when (item) {
                                            is TextStoryItem -> openTextComposer(item)
                                            is PhotoStickerItem -> {
                                                val shapes = PhotoStickerShape.entries
                                                val nextShape = shapes[(shapes.indexOf(item.shape) + 1) % shapes.size]
                                                updateCurrentSlide { s ->
                                                    s.copy(items = s.items.map { if (it.id == item.id) item.copy(shape = nextShape) else it })
                                                }
                                                showToast("Frame: ${nextShape.label}", Icon3DName.SHAPES)
                                            }
                                            is WidgetStoryItem -> {
                                                if (item.widgetType == WidgetType.MUSIC) {
                                                    activeSheet = ActiveSheet.MUSIC
                                                } else if (item.widgetType != WidgetType.TIME) {
                                                    editingWidget = item
                                                }
                                            }
                                            else -> Unit
                                        }
                                    }
                                )
                            }
                        }

                        // Layer 7: Apple HIG Magnetic Alignment Snap Guides
                        if (showSnapV) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(colors.accent.copy(alpha = 0.92f))
                            )
                        }
                        if (showSnapH) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.accent.copy(alpha = 0.92f))
                            )
                        }

                        // Live Scale & Rotation HUD Pill while Pinching / Rotating
                        androidx.compose.animation.AnimatedVisibility(
                            visible = activeTransformHint != null,
                            enter = fadeIn(tween(120)),
                            exit = fadeOut(tween(150)),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 64.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.bgMaterial)
                                    .border(0.5.dp, colors.separator, CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = activeTransformHint.orEmpty(),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            }
                        }

                        // Layer 8: Instagram/Snapchat Safe-Area HUD Overlay while dragging
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isDraggingItem,
                            enter = fadeIn(tween(150)),
                            exit = fadeOut(tween(150)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .background(Color.Black.copy(alpha = 0.22f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.28f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("HEADER SAFE ZONE", fontSize = 10.sp, letterSpacing = 1.4.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .background(Color.Black.copy(alpha = 0.22f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.28f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("REPLY BAR SAFE ZONE", fontSize = 10.sp, letterSpacing = 1.4.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Drag-to-Trash Drop Zone Indicator
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isDraggingItem,
                            enter = fadeIn(tween(150)),
                            exit = fadeOut(tween(150)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 18.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon3D(Icon3DName.TRASH, size = if (isOverTrash) 32.dp else 26.dp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (isOverTrash) "Release to delete" else "DROP TO DELETE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOverTrash) colors.semanticRed else Color.White
                                )
                            }
                        }

                        // Floating Drawing Pill (when Markup drawer is collapsed for edge-to-edge drawing)
                        if (isDrawingActive && isDrawingPanelCollapsed) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp)
                                    .clip(CircleShape)
                                    .background(colors.bgMaterial)
                                    .border(0.5.dp, colors.separator, CircleShape)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HigPressable(onClick = {
                                    isDrawingPanelCollapsed = false
                                    activeSheet = ActiveSheet.DRAW
                                }) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Icon3D(Icon3DName.SLIDERS_HORIZONTAL, size = 16.dp)
                                        Text("Brushes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                    }
                                }
                                if (canRefineLastStroke()) {
                                    Box(Modifier.width(0.5.dp).height(14.dp).background(colors.separator))
                                    HigPressable(onClick = ::refineLastStroke) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon3D(Icon3DName.SPARKLES, size = 15.dp)
                                            Text("Refine", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                        }
                                    }
                                }
                                if (currentSlide.strokes.isNotEmpty()) {
                                    Box(Modifier.width(0.5.dp).height(14.dp).background(colors.separator))
                                    HigPressable(onClick = {
                                        updateCurrentSlide { it.copy(strokes = it.strokes.dropLast(1)) }
                                    }) {
                                        Icon3D(Icon3DName.UNDO, size = 15.dp)
                                    }
                                }
                                Box(Modifier.width(0.5.dp).height(14.dp).background(colors.separator))
                                HigPressable(onClick = ::closeAllSheets) {
                                    Text("Done", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                                }
                            }
                        }

                        // Floating Selected Item Inspector Bar (Zero Icon Backgrounds)
                        val selectedItem = currentSlide.items.find { it.id == selectedItemId }
                        if (selectedItem != null && !isDraggingItem && !isDrawingActive) {
                            val isEditable = selectedItem is TextStoryItem ||
                                selectedItem is PhotoStickerItem ||
                                (selectedItem is WidgetStoryItem && selectedItem.widgetType != WidgetType.TIME)
                            val editLabel = when {
                                selectedItem is PhotoStickerItem -> "Shape"
                                selectedItem is WidgetStoryItem && selectedItem.widgetType == WidgetType.MUSIC -> "Track"
                                else -> "Edit"
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp, start = 8.dp, end = 8.dp)
                                    .clip(CircleShape)
                                    .background(colors.bgMaterial)
                                    .border(0.5.dp, colors.separator, CircleShape)
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (isEditable) {
                                    HigPressable(
                                        onClick = {
                                            when (selectedItem) {
                                                is TextStoryItem -> openTextComposer(selectedItem)
                                                is PhotoStickerItem -> {
                                                    val shapes = PhotoStickerShape.entries
                                                    val nextShape = shapes[(shapes.indexOf(selectedItem.shape) + 1) % shapes.size]
                                                    updateCurrentSlide { s ->
                                                        s.copy(items = s.items.map { if (it.id == selectedItem.id) selectedItem.copy(shape = nextShape) else it })
                                                    }
                                                    showToast("Frame: ${nextShape.label}", Icon3DName.SHAPES)
                                                }
                                                is WidgetStoryItem -> {
                                                    if (selectedItem.widgetType == WidgetType.MUSIC) {
                                                        activeSheet = ActiveSheet.MUSIC
                                                    } else {
                                                        editingWidget = selectedItem
                                                    }
                                                }
                                                is EmojiStoryItem -> Unit
                                            }
                                        },
                                        modifier = Modifier.height(30.dp).padding(horizontal = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon3D(Icon3DName.EDIT, size = 15.dp)
                                            Text(editLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                        }
                                    }
                                    Box(Modifier.width(0.5.dp).height(16.dp).background(colors.separator))
                                }

                                HigPressable(
                                    onClick = {
                                        if (!selectedItem.locked) {
                                            updateCurrentSlide { s ->
                                                s.copy(items = s.items.map {
                                                    if (it.id == selectedItem.id) it.copyTransform(scale = max(0.35f, it.scale - 0.12f)) else it
                                                })
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) { Icon3D(Icon3DName.MINUS, size = 15.dp) }

                                HigPressable(
                                    onClick = {
                                        if (!selectedItem.locked) {
                                            updateCurrentSlide { s ->
                                                s.copy(items = s.items.map {
                                                    if (it.id == selectedItem.id) it.copyTransform(scale = min(3.2f, it.scale + 0.12f)) else it
                                                })
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) { Icon3D(Icon3DName.PLUS, size = 15.dp) }

                                HigPressable(
                                    onClick = {
                                        if (currentSlide.items.size > 1) {
                                            val idx = currentSlide.items.indexOfFirst { it.id == selectedItem.id }
                                            val mutable = currentSlide.items.toMutableList()
                                            val moved = mutable.removeAt(idx)
                                            if (idx == mutable.size) {
                                                mutable.add(0, moved)
                                                showToast("Sent to back", Icon3DName.LAYERS)
                                            } else {
                                                mutable.add(moved)
                                                showToast("Brought to front", Icon3DName.LAYERS)
                                            }
                                            updateCurrentSlide { it.copy(items = mutable) }
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) { Icon3D(Icon3DName.LAYERS, size = 16.dp) }

                                HigPressable(
                                    onClick = {
                                        val nextLock = !selectedItem.locked
                                        updateCurrentSlide { s ->
                                            s.copy(items = s.items.map {
                                                if (it.id == selectedItem.id) it.copyTransform(locked = nextLock) else it
                                            })
                                        }
                                        showToast(if (nextLock) "Layer locked" else "Layer unlocked", if (nextLock) Icon3DName.LOCK else Icon3DName.UNLOCK)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) { Icon3D(if (selectedItem.locked) Icon3DName.LOCK else Icon3DName.UNLOCK, size = 16.dp) }

                                HigPressable(
                                    onClick = {
                                        val copy = selectedItem.copyTransform(
                                            id = newId("dup"),
                                            xDp = selectedItem.xDp + 18f,
                                            yDp = selectedItem.yDp + 18f,
                                            locked = false
                                        )
                                        updateCurrentSlide { it.copy(items = it.items + copy) }
                                        selectedItemId = copy.id
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) { Icon3D(Icon3DName.COPY, size = 16.dp) }

                                HigPressable(
                                    onClick = {
                                        val targetItemId = selectedItem.id
                                        pendingDeleteConfirmation = StudioDeletePrompt(
                                            title = "Delete Element?",
                                            message = "Are you sure you want to remove this item from the slide?"
                                        ) {
                                            updateCurrentSlide { s -> s.copy(items = s.items.filterNot { it.id == targetItemId }) }
                                            if (selectedItemId == targetItemId) selectedItemId = null
                                            showToast("Deleted", Icon3DName.TRASH)
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) { Icon3D(Icon3DName.TRASH, size = 16.dp) }
                            }
                        }

                        // Photo Quick Controls Pill (Fit/Fill, Flip, Zoom %, Reset)
                        if (currentSlide.layout == LayoutType.NONE && currentSlide.photo.uriKey != null && !isDraggingItem) {
                            val p = currentSlide.photo
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .clip(CircleShape)
                                    .background(colors.bgMaterial)
                                    .border(0.5.dp, colors.separator, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HigPressable(onClick = {
                                    val nextFit = if (p.fitMode == PhotoFitMode.CONTAIN) PhotoFitMode.COVER else PhotoFitMode.CONTAIN
                                    updateCurrentSlide { it.copy(photo = p.copy(fitMode = nextFit, xDp = 0f, yDp = 0f, scale = 1f)) }
                                }) {
                                    Text(
                                        text = if (p.fitMode == PhotoFitMode.COVER) "Fill" else "Fit",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                }
                                Box(Modifier.width(0.5.dp).height(12.dp).background(colors.separator))
                                HigPressable(onClick = {
                                    updateCurrentSlide { it.copy(photo = p.copy(flipX = !p.flipX)) }
                                }) {
                                    Icon3D(Icon3DName.FLIP_HORIZONTAL, size = 16.dp)
                                }
                                Box(Modifier.width(0.5.dp).height(12.dp).background(colors.separator))
                                Text(
                                    text = "${(p.scale * 100).roundToInt()}%",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textSecondary
                                )
                                HigPressable(onClick = {
                                    updateCurrentSlide { it.copy(photo = p.copy(xDp = 0f, yDp = 0f, scale = 1f, flipX = false)) }
                                }) {
                                    Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                                }
                            }
                        }

                        // Layer 9: Interactive Live Pixel Eyedropper Overlay
                        if (eyedropperTarget != EyedropperTarget.NONE && eyedropperBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(eyedropperBitmap) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown()
                                            down.consume()
                                            fun sample(pos: Offset) {
                                                eyedropperPos = pos
                                                val bmp = eyedropperBitmap ?: return
                                                val px = ((pos.x / size.width.coerceAtLeast(1)) * bmp.width)
                                                    .toInt().coerceIn(0, bmp.width - 1)
                                                val py = ((pos.y / size.height.coerceAtLeast(1)) * bmp.height)
                                                    .toInt().coerceIn(0, bmp.height - 1)
                                                sampledColorArgb = bmp.getPixel(px, py)
                                            }
                                            sample(down.position)
                                            while (true) {
                                                val ev = awaitPointerEvent()
                                                val ch = ev.changes.firstOrNull() ?: break
                                                if (!ch.pressed) {
                                                    ch.consume()
                                                    break
                                                }
                                                ch.consume()
                                                sample(ch.position)
                                            }

                                            // Commit picked color
                                            val hex = String.format("#%06X", 0xFFFFFF and sampledColorArgb)
                                            customSampledArgb = sampledColorArgb
                                            drawColorArgb = sampledColorArgb
                                            draftTextColorArgb = sampledColorArgb
                                            val target = eyedropperTarget
                                            eyedropperTarget = EyedropperTarget.NONE
                                            eyedropperPos = null
                                            eyedropperBitmap?.let { if (!it.isRecycled) it.recycle() }
                                            eyedropperBitmap = null

                                            when (target) {
                                                EyedropperTarget.TEXT -> showTextComposer = true
                                                EyedropperTarget.DRAW -> {
                                                    if (brushType == BrushType.ERASER) brushType = BrushType.PEN
                                                    isDrawingActive = true
                                                    isDrawingPanelCollapsed = false
                                                    activeSheet = ActiveSheet.DRAW
                                                }
                                                EyedropperTarget.TOAST -> {
                                                    val sel = currentSlide.items.find { it.id == selectedItemId }
                                                    if (sel is TextStoryItem) {
                                                        updateCurrentSlide { s ->
                                                            s.copy(items = s.items.map {
                                                                if (it.id == sel.id) sel.copy(colorArgb = sampledColorArgb) else it
                                                            })
                                                        }
                                                    }
                                                }
                                                EyedropperTarget.NONE -> Unit
                                            }
                                            showToast("Picked $hex", Icon3DName.PIPETTE)
                                        }
                                    }
                            ) {
                                // Top Instruction & Cancel Pill
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 12.dp)
                                        .clip(CircleShape)
                                        .background(colors.bgMaterial)
                                        .border(0.5.dp, colors.separator, CircleShape)
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Drag to pick color", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    HigPressable(onClick = ::cancelEyedropper) {
                                        Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                                    }
                                }

                                // Floating Loupe Ring above Finger
                                eyedropperPos?.let { pos ->
                                    Box(
                                        modifier = Modifier
                                            .offset {
                                                IntOffset(
                                                    (pos.x - 29.dp.toPx()).roundToInt(),
                                                    (pos.y - 78.dp.toPx()).roundToInt()
                                                )
                                            }
                                            .size(58.dp)
                                            .shadow(10.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(Color(sampledColorArgb))
                                            .border(3.dp, Color.White, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                // --------------------------------------------------------------
                // BOTTOM MOBILE CREATIVE DOCK (ZERO ICON BACKGROUNDS)
                // --------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomDockTab("Text", Icon3DName.TYPE, false) {
                        selectedItemId = null
                        openTextComposer()
                    }
                    BottomDockTab("Stickers", Icon3DName.STICKERS, activeSheet == ActiveSheet.STICKERS) {
                        closeAllSheets()
                        selectedItemId = null
                        activeSheet = ActiveSheet.STICKERS
                    }
                    BottomDockTab("Draw", Icon3DName.PEN_TOOL, isDrawingActive) {
                        closeAllSheets()
                        selectedItemId = null
                        isDrawingActive = true
                        isDrawingPanelCollapsed = false
                        activeSheet = ActiveSheet.DRAW
                    }
                    BottomDockTab("Layout", Icon3DName.LAYOUT_GRID, activeSheet == ActiveSheet.LAYOUT) {
                        closeAllSheets()
                        selectedItemId = null
                        activeSheet = ActiveSheet.LAYOUT
                    }
                    BottomDockTab("Adjust", Icon3DName.SLIDERS_HORIZONTAL, activeSheet == ActiveSheet.FILTERS) {
                        closeAllSheets()
                        selectedItemId = null
                        activeSheet = ActiveSheet.FILTERS
                    }
                    BottomDockTab("Audio", Icon3DName.MUSIC, activeSheet == ActiveSheet.MUSIC) {
                        closeAllSheets()
                        selectedItemId = null
                        activeSheet = ActiveSheet.MUSIC
                    }
                }
            }

            // ==================================================================
            // SHEET BACKDROP & APPLE HIG BOTTOM SHEETS
            // ==================================================================
            val showSheetBackdrop = activeSheet != ActiveSheet.NONE && activeSheet != ActiveSheet.DRAW
            AnimatedVisibility(
                visible = showSheetBackdrop,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = ::closeAllSheets
                        )
                )
            }

            // Animated Bottom Sheet Container with Swipe-Down to Dismiss
            AnimatedVisibility(
                visible = activeSheet != ActiveSheet.NONE,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(220)
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .shadow(20.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                        .background(colors.bgMaterialHeavy)
                        .border(0.5.dp, colors.separator, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                        .navigationBarsPadding()
                        .pointerInput(activeSheet) {
                            var dragSum = 0f
                            detectDragGestures(
                                onDragStart = { dragSum = 0f },
                                onDragEnd = {
                                    if (dragSum > 140f) {
                                        if (activeSheet == ActiveSheet.DRAW && isDrawingActive) {
                                            activeSheet = ActiveSheet.NONE
                                            isDrawingPanelCollapsed = true
                                        } else {
                                            closeAllSheets()
                                        }
                                    }
                                }
                            ) { _, dragAmount -> dragSum += dragAmount.y }
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Apple HIG Sheet Grabber Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(colors.bgSurfaceTertiary)
                    )
                    Spacer(Modifier.height(12.dp))

                    when (activeSheet) {
                        ActiveSheet.STICKERS -> StickersAndWidgetsSheet(
                            onClose = ::closeAllSheets,
                            onAddPhotoSticker = { launchPhotoPicker(stickerPhotoPicker) },
                            onAddWidget = { type ->
                                val nowStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                val item = when (type) {
                                    WidgetType.POLL -> WidgetStoryItem(widgetType = type, question = "Which aesthetic wins?", optA = "Light HIG", optB = "Dark Mode")
                                    WidgetType.SLIDER -> WidgetStoryItem(widgetType = type, prompt = "How good is this vibe?", emoji = "🔥", sliderValue = 72)
                                    WidgetType.QUIZ -> WidgetStoryItem(widgetType = type, question = "Guess the city!", quizOpts = listOf("Cupertino", "Tokyo", "Bengaluru"), correctIndex = 0)
                                    WidgetType.COUNTDOWN -> WidgetStoryItem(widgetType = type, title = "LAUNCH DAY", days = "03", hours = "14", mins = "28")
                                    WidgetType.QUESTION -> WidgetStoryItem(widgetType = type, prompt = "Ask me anything...")
                                    WidgetType.MENTION -> WidgetStoryItem(widgetType = type, handle = "@DESIGNER")
                                    WidgetType.LINK -> WidgetStoryItem(widgetType = type, linkLabel = "apple.com/design")
                                    WidgetType.LOCATION -> WidgetStoryItem(widgetType = type, place = "BENGALURU, IN")
                                    WidgetType.TIME -> WidgetStoryItem(widgetType = type, timeText = nowStr)
                                    WidgetType.MUSIC -> WidgetStoryItem(widgetType = type)
                                }
                                updateCurrentSlide { it.copy(items = it.items + item) }
                                selectedItemId = item.id
                                closeAllSheets()
                            },
                            onAddEmoji = { em ->
                                val item = EmojiStoryItem(emoji = em)
                                updateCurrentSlide { it.copy(items = it.items + item) }
                                selectedItemId = item.id
                                closeAllSheets()
                            }
                        )

                        ActiveSheet.DRAW -> MarkupDrawSheet(
                            brush = brushType,
                            lineStyle = drawLineStyle,
                            glowEnabled = drawGlowEnabled,
                            autoShapeEnabled = autoShapeEnabled,
                            colorArgb = drawColorArgb,
                            customArgb = customSampledArgb,
                            emoji = drawEmoji,
                            sizeDp = drawSizeDp,
                            canUndoStroke = currentSlide.strokes.isNotEmpty(),
                            canRefineLastStroke = canRefineLastStroke(),
                            onBrushChange = { brushType = it },
                            onLineStyleChange = { drawLineStyle = it },
                            onToggleGlow = { drawGlowEnabled = !drawGlowEnabled },
                            onToggleAutoShape = { autoShapeEnabled = !autoShapeEnabled },
                            onRefineLastStroke = ::refineLastStroke,
                            onColorChange = {
                                drawColorArgb = it
                                if (brushType == BrushType.ERASER) brushType = BrushType.PEN
                            },
                            onEmojiChange = {
                                drawEmoji = it
                                showToast("Brush: $it", Icon3DName.SPARKLES)
                            },
                            onSizeChange = { drawSizeDp = it },
                            onPickEyedropper = { startEyedropper(EyedropperTarget.DRAW) },
                            onUndoStroke = {
                                if (currentSlide.strokes.isNotEmpty()) {
                                    updateCurrentSlide { it.copy(strokes = it.strokes.dropLast(1)) }
                                }
                            },
                            onCollapse = {
                                activeSheet = ActiveSheet.NONE
                                isDrawingPanelCollapsed = true
                            },
                            onClear = { updateCurrentSlide { it.copy(strokes = emptyList()) } },
                            onDone = ::closeAllSheets
                        )

                        ActiveSheet.LAYOUT -> LayoutGridSheet(
                            activeLayout = currentSlide.layout,
                            onSelectLayout = { layout ->
                                updateCurrentSlide { it.copy(layout = layout) }
                                showToast(
                                    if (layout == LayoutType.NONE) "Single frame mode" else "Tap any cell to add photo",
                                    Icon3DName.LAYOUT_GRID
                                )
                            },
                            onClose = ::closeAllSheets
                        )

                        ActiveSheet.FILTERS -> FiltersAdjustSheet(
                            filters = currentSlide.filters,
                            previewBitmap = currentSlide.photo.uriKey?.let { bitmapCache[it] },
                            backdrop = STORY_BACKDROPS[currentSlide.backdropIndex],
                            onPresetSelected = { preset ->
                                updateCurrentSlide {
                                    it.copy(
                                        filters = FilterState(
                                            preset = preset.name,
                                            brightness = preset.brightness,
                                            contrast = preset.contrast,
                                            saturate = preset.saturate,
                                            sepia = preset.sepia,
                                            grayscale = preset.grayscale,
                                            vignette = preset.vignette
                                        )
                                    )
                                }
                            },
                            onFilterChanged = { newFilters ->
                                updateCurrentSlide(commitHistory = false) { it.copy(filters = newFilters) }
                            },
                            onCommitHistory = {
                                updateCurrentSlide(commitHistory = true) { it }
                            },
                            onReset = {
                                updateCurrentSlide { it.copy(filters = FilterState()) }
                                showToast("Adjustments reset", Icon3DName.ROTATE_CCW)
                            },
                            onClose = ::closeAllSheets
                        )

                        ActiveSheet.MUSIC -> com.chroma.studio.ui.components.StoryMusicPickerModal(
                            isOpen = true,
                            initialTrackId = currentSlide.items.filterIsInstance<WidgetStoryItem>()
                                .firstOrNull { it.widgetType == WidgetType.MUSIC }?.musicTrackId?.takeIf { it.isNotBlank() },
                            headerTitle = "Story Music",
                            onDismiss = ::closeAllSheets,
                            onClipConfirmed = { selection ->
                                val musicWidget = WidgetStoryItem(
                                    widgetType = WidgetType.MUSIC,
                                    musicTitle = selection.title,
                                    musicArtist = selection.artist,
                                    musicFreq = 261.63,
                                    musicTrackId = selection.trackId,
                                    musicAlbum = selection.album,
                                    musicCoverUrl = selection.coverUrl,
                                    musicAudioUrl = selection.audioUrl,
                                    musicStartSec = selection.startSec,
                                    musicDurationSec = selection.durationSec,
                                    yDp = -150f
                                )
                                updateCurrentSlide { s ->
                                    val filtered = s.items.filterNot { it is WidgetStoryItem && it.widgetType == WidgetType.MUSIC }
                                    s.copy(items = filtered + musicWidget)
                                }
                                closeAllSheets()
                                showToast("Added \"${selection.title}\"", Icon3DName.MUSIC)
                            }
                        )

                        ActiveSheet.SHARE -> ShareExportSheet(
                            audience = audience,
                            slideCount = slides.size,
                            isExporting = isExporting,
                            canPublishToApp = onPublishStory != null,
                            onAudienceChange = { audience = it },
                            onPublishToApp = {
                                val publishCb = onPublishStory ?: return@ShareExportSheet
                                if (isExporting) return@ShareExportSheet
                                isExporting = true
                                val stageWdp = with(density) { stageSizePx.width.toDp().value }.coerceAtLeast(180f)
                                val stageHdp = with(density) { stageSizePx.height.toDp().value }.coerceAtLeast(320f)
                                val snapshot = slides.toList()
                                scope.launch {
                                    val exportedSlides = try {
                                        withContext(Dispatchers.IO) {
                                            val outDir = File(context.cacheDir, "story_exports").apply { mkdirs() }
                                            val stamp = System.currentTimeMillis()
                                            snapshot.mapIndexedNotNull { idx, slideItem ->
                                                var bmp: Bitmap? = null
                                                try {
                                                    val widgetJson = slideItem.toPublishedWidgetsJson(stageWdp, stageHdp)
                                                    bmp = renderCompositeBitmap(
                                                        slide = slideItem,
                                                        bitmaps = bitmapCache,
                                                        outW = 1080,
                                                        outH = 1920,
                                                        stageWdp = stageWdp,
                                                        stageHdp = stageHdp,
                                                        skipWidgets = widgetJson.isNotBlank()
                                                    )
                                                    val outFile = File(outDir, "story-${stamp}-${idx + 1}.png")
                                                    FileOutputStream(outFile).use { stream ->
                                                        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                                    }
                                                    val attachedMusic = slideItem.items.filterIsInstance<WidgetStoryItem>()
                                                        .firstOrNull { it.widgetType == WidgetType.MUSIC && it.musicTrackId.isNotBlank() }
                                                        ?.let { mw ->
                                                            com.chroma.studio.data.AttachedMusicSelection(
                                                                trackId = mw.musicTrackId,
                                                                title = mw.musicTitle,
                                                                artist = mw.musicArtist,
                                                                album = mw.musicAlbum,
                                                                coverUrl = mw.musicCoverUrl,
                                                                audioUrl = mw.musicAudioUrl,
                                                                startSec = mw.musicStartSec,
                                                                durationSec = mw.musicDurationSec,
                                                                totalTrackSec = mw.musicDurationSec.coerceAtLeast(15),
                                                                showLyricsSticker = true
                                                            )
                                                        }
                                                    StoryPublishSlide(
                                                        uri = Uri.fromFile(outFile),
                                                        interactiveFormatJson = widgetJson,
                                                        musicTrack = attachedMusic
                                                    )
                                                } catch (_: Throwable) {
                                                    null
                                                } finally {
                                                    if (bmp != null && !bmp.isRecycled) bmp.recycle()
                                                }
                                            }
                                        }
                                    } finally {
                                        isExporting = false
                                    }
                                    closeAllSheets()
                                    if (exportedSlides.isNotEmpty()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        publishCb(exportedSlides, audience)
                                    } else {
                                        showToast("Could not prepare story image", Icon3DName.ALERT)
                                    }
                                }
                            },
                            onSavePng = {
                                if (isExporting) return@ShareExportSheet
                                isExporting = true
                                val stageWdp = with(density) { stageSizePx.width.toDp().value }.coerceAtLeast(180f)
                                val stageHdp = with(density) { stageSizePx.height.toDp().value }.coerceAtLeast(320f)
                                scope.launch {
                                    val saved = try {
                                        withContext(Dispatchers.IO) {
                                            val bmp = renderCompositeBitmap(currentSlide, bitmapCache, 1080, 1920, stageWdp, stageHdp)
                                            saveBitmapToGallery(context, bmp, "story-slide-${currentSlideIndex + 1}-${System.currentTimeMillis()}.png")
                                        }
                                    } catch (_: Throwable) {
                                        false
                                    } finally {
                                        isExporting = false
                                    }
                                    closeAllSheets()
                                    if (saved) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showToast("Saved 1080×1920 PNG to Gallery", Icon3DName.DOWNLOAD)
                                    } else {
                                        showToast("Could not save PNG", Icon3DName.ALERT)
                                    }
                                }
                            },
                            onSaveAllSlides = {
                                if (isExporting) return@ShareExportSheet
                                isExporting = true
                                val stageWdp = with(density) { stageSizePx.width.toDp().value }.coerceAtLeast(180f)
                                val stageHdp = with(density) { stageSizePx.height.toDp().value }.coerceAtLeast(320f)
                                val snapshot = slides.toList()
                                scope.launch {
                                    val savedCount = try {
                                        withContext(Dispatchers.IO) {
                                            var count = 0
                                            snapshot.forEachIndexed { idx, s ->
                                                val bmp = renderCompositeBitmap(s, bitmapCache, 1080, 1920, stageWdp, stageHdp)
                                                if (saveBitmapToGallery(context, bmp, "story-slide-${idx + 1}-${System.currentTimeMillis()}.png")) {
                                                    count++
                                                }
                                            }
                                            count
                                        }
                                    } catch (_: Throwable) {
                                        0
                                    } finally {
                                        isExporting = false
                                    }
                                    closeAllSheets()
                                    if (savedCount == snapshot.size) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showToast("Saved all $savedCount slides to Gallery", Icon3DName.DOWNLOAD)
                                    } else {
                                        showToast("Saved $savedCount of ${snapshot.size} slides", Icon3DName.INFO)
                                    }
                                }
                            },
                            onSystemShare = {
                                if (isExporting) return@ShareExportSheet
                                isExporting = true
                                val stageWdp = with(density) { stageSizePx.width.toDp().value }.coerceAtLeast(180f)
                                val stageHdp = with(density) { stageSizePx.height.toDp().value }.coerceAtLeast(320f)
                                scope.launch {
                                    val shared = try {
                                        withContext(Dispatchers.IO) {
                                            val bmp = renderCompositeBitmap(currentSlide, bitmapCache, 1080, 1920, stageWdp, stageHdp)
                                            shareBitmapViaIntent(context, bmp)
                                        }
                                    } catch (_: Throwable) {
                                        false
                                    } finally {
                                        isExporting = false
                                    }
                                    closeAllSheets()
                                    if (!shared) showToast("Configure FileProvider to share", Icon3DName.INFO)
                                }
                            },
                            onClose = ::closeAllSheets
                        )

                        ActiveSheet.NONE -> Unit
                    }
                }
            }

            // ==================================================================
            // FULLSCREEN TYPOGRAPHY COMPOSER OVERLAY
            // ==================================================================
            if (showTextComposer) {
                TextComposerOverlay(
                    text = draftText,
                    font = draftFont,
                    colorArgb = draftTextColorArgb,
                    customArgb = customSampledArgb,
                    bgMode = draftBgMode,
                    align = draftAlign,
                    sizeSp = draftSizeSp,
                    anim = draftAnim,
                    onTextChange = { draftText = it },
                    onFontChange = { draftFont = it },
                    onColorChange = { draftTextColorArgb = it },
                    onBgModeCycle = {
                        val all = TextBgMode.entries
                        draftBgMode = all[(all.indexOf(draftBgMode) + 1) % all.size]
                    },
                    onAlignCycle = {
                        val all = TextAlignmentMode.entries
                        draftAlign = all[(all.indexOf(draftAlign) + 1) % all.size]
                    },
                    onAnimCycle = {
                        val all = TextAnimStyle.entries
                        draftAnim = all[(all.indexOf(draftAnim) + 1) % all.size]
                    },
                    onSizeChange = { draftSizeSp = it },
                    onPickEyedropper = { startEyedropper(EyedropperTarget.TEXT) },
                    onDone = {
                        val content = draftText.trim()
                        showTextComposer = false
                        if (content.isNotEmpty()) {
                            val existingId = editingTextId
                            if (existingId != null) {
                                updateCurrentSlide { s ->
                                    s.copy(
                                        items = s.items.map {
                                            if (it.id == existingId && it is TextStoryItem) {
                                                it.copy(
                                                    text = content,
                                                    font = draftFont,
                                                    colorArgb = draftTextColorArgb,
                                                    bgMode = draftBgMode,
                                                    align = draftAlign,
                                                    sizeSp = draftSizeSp,
                                                    anim = draftAnim
                                                )
                                            } else it
                                        }
                                    )
                                }
                                lastAnimatedItemId = existingId
                            } else {
                                val created = TextStoryItem(
                                    text = content,
                                    font = draftFont,
                                    colorArgb = draftTextColorArgb,
                                    bgMode = draftBgMode,
                                    align = draftAlign,
                                    sizeSp = draftSizeSp,
                                    anim = draftAnim
                                )
                                updateCurrentSlide { it.copy(items = it.items + created) }
                                selectedItemId = created.id
                                lastAnimatedItemId = created.id
                            }
                        }
                        editingTextId = null
                    }
                )
            }

            // ==================================================================
            // WIDGET CUSTOMIZATION DIALOG
            // ==================================================================
            editingWidget?.let { widget ->
                WidgetEditDialog(
                    widget = widget,
                    onDismiss = { editingWidget = null },
                    onSave = { updated ->
                        updateCurrentSlide { s ->
                            s.copy(items = s.items.map { if (it.id == updated.id) updated else it })
                        }
                        editingWidget = null
                    }
                )
            }

            pendingDeleteConfirmation?.let { prompt ->
                com.chroma.studio.ui.components.IosAlertDialog(
                    title = prompt.title,
                    message = prompt.message,
                    confirmText = prompt.confirmText,
                    isDestructive = true,
                    onConfirm = {
                        pendingDeleteConfirmation = null
                        prompt.onConfirm()
                    },
                    dismissText = "Cancel",
                    onDismiss = { pendingDeleteConfirmation = null }
                )
            }

            // ==================================================================
            // FULLSCREEN MULTI-SLIDE STORY PLAYER (TAP NEXT/PREV + HOLD PAUSE)
            // ==================================================================
            if (showPreviewPlayer) {
                StoryPreviewPlayer(
                    slides = slides.toList(),
                    audience = audience,
                    bitmapCache = bitmapCache,
                    stageWdp = with(density) { stageSizePx.width.toDp().value }.coerceAtLeast(180f),
                    stageHdp = with(density) { stageSizePx.height.toDp().value }.coerceAtLeast(320f),
                    onPlayChord = { freq -> scope.launch { playAmbientChordAudio(freq) } },
                    onClose = { showPreviewPlayer = false }
                )
            }

            // ==================================================================
            // APPLE HIG FLOATING TOAST PILL
            // ==================================================================
            AnimatedVisibility(
                visible = toastMsg != null,
                enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(colors.bgMaterial)
                        .border(0.5.dp, colors.separator, CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon3D(toastIcon, size = 18.dp)
                    Text(
                        text = toastMsg.orEmpty(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}

/* ==============================================================================
   7. SUB-COMPONENTS: DOCK TAB, LAYOUT GRID, DRAGGABLE OBJECTS & WIDGETS
   ============================================================================== */

@Composable
private fun RowScope.BottomDockTab(
    label: String,
    icon: Icon3DName,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalHigColors.current
    val liftScale by animateFloatAsState(if (active) 1.12f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "dockScale")
    HigPressable(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon3D(
                icon = icon,
                size = 23.dp,
                alpha = if (active) 1f else 0.9f,
                modifier = Modifier.scale(liftScale)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                color = if (active) colors.accent else colors.textSecondary
            )
        }
    }
}

@Composable
private fun StoryLayoutGridLayer(
    layout: LayoutType,
    images: Map<Int, String>,
    bitmapCache: Map<String, Bitmap>,
    colorFilter: ColorFilter,
    onCellClick: (Int) -> Unit
) {
    @Composable
    fun Cell(index: Int, modifier: Modifier) {
        val uriKey = images[index]
        val bmp = uriKey?.let { bitmapCache[it] }
        val imgBmp = remember(bmp) { bmp?.asImageBitmap() }
        Box(
            modifier = modifier
                .background(Color.White.copy(alpha = 0.06f))
                .clickable { onCellClick(index) },
            contentAlignment = Alignment.Center
        ) {
            if (imgBmp != null) {
                Image(
                    bitmap = imgBmp,
                    contentDescription = "Collage Photo ${index + 1}",
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon3D(Icon3DName.PLUS, size = 26.dp)
                    Spacer(Modifier.height(4.dp))
                    Text("Photo ${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.65f))
                }
            }
        }
    }

    val gap = 3.dp
    when (layout) {
        LayoutType.SPLIT_H -> Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), verticalArrangement = Arrangement.spacedBy(gap)) {
            Cell(0, Modifier.fillMaxWidth().weight(1f))
            Cell(1, Modifier.fillMaxWidth().weight(1f))
        }
        LayoutType.SPLIT_V -> Row(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), horizontalArrangement = Arrangement.spacedBy(gap)) {
            Cell(0, Modifier.fillMaxHeight().weight(1f))
            Cell(1, Modifier.fillMaxHeight().weight(1f))
        }
        LayoutType.GRID_3 -> Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), verticalArrangement = Arrangement.spacedBy(gap)) {
            Cell(0, Modifier.fillMaxWidth().weight(1f))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                Cell(1, Modifier.fillMaxHeight().weight(1f))
                Cell(2, Modifier.fillMaxHeight().weight(1f))
            }
        }
        LayoutType.GRID_4 -> Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                Cell(0, Modifier.fillMaxHeight().weight(1f))
                Cell(1, Modifier.fillMaxHeight().weight(1f))
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                Cell(2, Modifier.fillMaxHeight().weight(1f))
                Cell(3, Modifier.fillMaxHeight().weight(1f))
            }
        }
        LayoutType.NONE -> Unit
    }
}

@Composable
private fun DraggableStoryObject(
    item: StoryItem,
    isSelected: Boolean,
    isDrawingActive: Boolean,
    animateEntrance: Boolean,
    stageHeightPx: Float,
    bitmapCache: Map<String, Bitmap>,
    onSelect: () -> Unit,
    onDragStart: () -> Unit,
    onTransformDelta: (dxDp: Float, dyDp: Float, zoom: Float, rotDelta: Float, screenYInStage: Float) -> Unit,
    onDragEnd: (shouldDelete: Boolean) -> Unit,
    onDoubleTap: () -> Unit
) {
    val colors = LocalHigColors.current
    val density = LocalDensity.current
    var lastTapMs by remember { mutableStateOf(0L) }

    // Keep fresh references inside long-lived pointerInput gesture scopes
    val latestItem by rememberUpdatedState(item)
    val latestStageHeightPx by rememberUpdatedState(stageHeightPx)
    val latestOnSelect by rememberUpdatedState(onSelect)
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnTransformDelta by rememberUpdatedState(onTransformDelta)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    val latestOnDoubleTap by rememberUpdatedState(onDoubleTap)

    // Optional text entrance animation trigger
    val animProgress = remember(item.id, animateEntrance) { Animatable(if (animateEntrance) 0f else 1f) }
    LaunchedEffect(item.id, animateEntrance) {
        if (animateEntrance) {
            animProgress.snapTo(0f)
            animProgress.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    val animMod = if (item is TextStoryItem && item.anim != TextAnimStyle.NONE) {
        val p = animProgress.value
        Modifier.graphicsLayer {
            when (item.anim) {
                TextAnimStyle.FADE -> alpha = p
                TextAnimStyle.SLIDE -> {
                    alpha = p
                    translationY = (1f - p) * 22.dp.toPx()
                }
                TextAnimStyle.POP -> {
                    alpha = p
                    val sc = 0.72f + p * 0.28f
                    scaleX = sc; scaleY = sc
                }
                TextAnimStyle.NONE -> Unit
            }
        }
    } else Modifier

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationX = with(density) { item.xDp.dp.toPx() }
                translationY = with(density) { item.yDp.dp.toPx() }
                scaleX = item.scale
                scaleY = item.scale
                rotationZ = item.rotation
            }
            .then(animMod)
            .then(
                if (isSelected) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = if (item.locked) colors.semanticOrange else Color.White.copy(alpha = 0.92f),
                            topLeft = Offset(-6.dp.toPx(), -6.dp.toPx()),
                            size = Size(size.width + 12.dp.toPx(), size.height + 12.dp.toPx()),
                            cornerRadius = CornerRadius(10.dp.toPx()),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                            )
                        )
                    }
                } else Modifier
            )
            .pointerInput(item.id, item.locked, isDrawingActive) {
                if (isDrawingActive) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    latestOnSelect()
                    if (latestItem.locked) {
                        return@awaitEachGesture
                    }

                    var moved = false
                    var lastScreenY = latestStageHeightPx / 2f + with(density) { latestItem.yDp.dp.toPx() }

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }
                        if (pressedChanges.isEmpty()) break

                        val pan = event.calculatePan()
                        val zoom = event.calculateZoom()
                        val rot = event.calculateRotation()

                        if (pan != Offset.Zero || zoom != 1f || rot != 0f) {
                            if (!moved && (hypot(pan.x.toDouble(), pan.y.toDouble()) > 3.0 || abs(zoom - 1f) > 0.01f || abs(rot) > 0.5f)) {
                                moved = true
                                latestOnDragStart()
                            }
                            if (moved) {
                                event.changes.forEach { it.consume() }
                                lastScreenY += pan.y
                                val dxDp = with(density) { pan.x.toDp().value }
                                val dyDp = with(density) { pan.y.toDp().value }
                                latestOnTransformDelta(dxDp, dyDp, zoom, rot, lastScreenY)
                            }
                        }
                    }

                    if (!moved) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapMs < 300L) latestOnDoubleTap()
                        lastTapMs = now
                    } else {
                        val overTrash = lastScreenY > latestStageHeightPx - with(density) { 72.dp.toPx() }
                        latestOnDragEnd(overTrash)
                    }
                }
            }
    ) {
        when (item) {
            is TextStoryItem -> StoryTextNode(item)
            is EmojiStoryItem -> Text(text = item.emoji, fontSize = 54.sp, lineHeight = 56.sp)
            is PhotoStickerItem -> StoryPhotoStickerNode(item, bitmapCache[item.uriKey])
            is WidgetStoryItem -> StoryWidgetNode(item)
        }
    }
}

@Composable
private fun StoryTextNode(item: TextStoryItem, scaleFactor: Float = 1f) {
    val fontFamily = when (item.font) {
        TextFontStyle.SERIF, TextFontStyle.ITALIC -> FontFamily.Serif
        TextFontStyle.MONO -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    val textColor = Color(item.colorArgb)
    val contentColor = if (item.bgMode == TextBgMode.PILL && isLightArgb(item.colorArgb)) Color.Black else textColor
    val displayText = if (item.font == TextFontStyle.BOLD) item.text.uppercase() else item.text
    val align = when (item.align) {
        TextAlignmentMode.LEFT -> TextAlign.Left
        TextAlignmentMode.RIGHT -> TextAlign.Right
        TextAlignmentMode.CENTER -> TextAlign.Center
    }

    val boxMod = when (item.bgMode) {
        TextBgMode.CLEAN -> Modifier
        TextBgMode.PILL -> Modifier
            .shadow(8.dp, RoundedCornerShape((14f * scaleFactor).dp))
            .clip(RoundedCornerShape((14f * scaleFactor).dp))
            .background(textColor)
            .padding(horizontal = (16f * scaleFactor).dp, vertical = (8f * scaleFactor).dp)
        TextBgMode.TRANSLUCENT -> Modifier
            .clip(RoundedCornerShape((14f * scaleFactor).dp))
            .background(Color(0xC71C1C1E))
            .padding(horizontal = (16f * scaleFactor).dp, vertical = (8f * scaleFactor).dp)
        TextBgMode.OUTLINE -> Modifier
            .clip(RoundedCornerShape((12f * scaleFactor).dp))
            .border((2f * scaleFactor).dp, textColor, RoundedCornerShape((12f * scaleFactor).dp))
            .padding(horizontal = (14f * scaleFactor).dp, vertical = (6f * scaleFactor).dp)
    }

    val shadow = when {
        item.font == TextFontStyle.NEON -> Shadow(color = textColor, offset = Offset.Zero, blurRadius = 24f * scaleFactor)
        item.bgMode == TextBgMode.CLEAN -> Shadow(color = Color.Black.copy(alpha = 0.4f), offset = Offset(0f, 3f), blurRadius = 10f)
        else -> null
    }

    Box(modifier = boxMod) {
        Text(
            text = displayText,
            color = contentColor,
            fontSize = (item.sizeSp * scaleFactor).sp,
            lineHeight = (item.sizeSp * 1.2f * scaleFactor).sp,
            fontFamily = fontFamily,
            fontWeight = if (item.font == TextFontStyle.BOLD) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontStyle = if (item.font == TextFontStyle.ITALIC) FontStyle.Italic else FontStyle.Normal,
            textAlign = align,
            style = TextStyle(shadow = shadow),
            modifier = Modifier.widthIn(max = (275f * scaleFactor).dp)
        )
    }
}

@Composable
private fun StoryPhotoStickerNode(item: PhotoStickerItem, bitmap: Bitmap?) {
    if (bitmap == null) return
    val imageBmp = remember(bitmap) { bitmap.asImageBitmap() }
    when (item.shape) {
        PhotoStickerShape.POLAROID -> {
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 180.dp)
                    .shadow(12.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 28.dp)
            ) {
                Image(
                    bitmap = imageBmp,
                    contentDescription = "Polaroid Sticker",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        else -> {
            val shape = when (item.shape) {
                PhotoStickerShape.CIRCLE -> CircleShape
                PhotoStickerShape.HEART -> HeartStickerShape
                PhotoStickerShape.STAR -> StarStickerShape
                else -> RoundedCornerShape(18.dp)
            }
            Image(
                bitmap = imageBmp,
                contentDescription = "Photo Sticker",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(144.dp)
                    .shadow(12.dp, shape)
                    .clip(shape)
            )
        }
    }
}

@Composable
private fun StoryWidgetNode(item: WidgetStoryItem) {
    when (item.widgetType) {
        WidgetType.POLL -> {
            Column(
                modifier = Modifier
                    .width(236.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF5FFFFFF))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.question, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111), textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(Color(0xFFF2F2F7)).padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(item.optA, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF007AFF), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(Color(0xFFF2F2F7)).padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(item.optB, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1C1E), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }

        WidgetType.SLIDER -> {
            Column(
                modifier = Modifier
                    .width(236.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF5FFFFFF))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.prompt, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111), textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val frac = (item.sliderValue / 100f).coerceIn(0.05f, 0.95f)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0xFFE5E5EA)))
                    Box(
                        Modifier
                            .fillMaxWidth(frac)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF9500), Color(0xFFFF2D55))))
                    )
                    Text(
                        text = item.emoji,
                        fontSize = 20.sp,
                        modifier = Modifier.offset(x = maxWidth * frac - 12.dp)
                    )
                }
            }
        }

        WidgetType.QUIZ -> {
            val letters = listOf("A", "B", "C")
            Column(
                modifier = Modifier
                    .width(236.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF5FFFFFF))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.question, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    item.quizOpts.forEachIndexed { idx, opt ->
                        val isCorrect = idx == item.correctIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (isCorrect) Color(0xFFECFDF3) else Color(0xFFF9FAFB))
                                .border(1.dp, if (isCorrect) Color(0xFF34C759) else Color(0xFFE5E5EA), RoundedCornerShape(11.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isCorrect) Color(0xFF34C759) else Color(0xFFE5E5EA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(letters.getOrElse(idx) { "?" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isCorrect) Color.White else Color(0xFF3A3A3C))
                            }
                            Text(opt, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF111111), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        WidgetType.COUNTDOWN -> {
            Column(
                modifier = Modifier
                    .width(230.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xEB1C1C1E))
                    .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.title, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Color.White.copy(alpha = 0.68f))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(item.days to "DAYS", item.hours to "HRS", item.mins to "MIN").forEach { (v, l) ->
                        Column(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(alpha = 0.11f)).padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(v, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(l, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        WidgetType.QUESTION -> {
            Column(
                modifier = Modifier
                    .width(232.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF5FFFFFF))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.prompt, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF2F2F7)).padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Type something...", fontSize = 12.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.Medium) }
            }
        }

        WidgetType.MENTION -> {
            Box(
                modifier = Modifier.shadow(10.dp, CircleShape).clip(CircleShape).background(Color(0xF5FFFFFF)).padding(horizontal = 14.dp, vertical = 6.dp)
            ) { Text(item.handle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF)) }
        }

        WidgetType.LINK -> {
            Row(
                modifier = Modifier.shadow(10.dp, CircleShape).clip(CircleShape).background(Color(0xF5FFFFFF)).padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("🔗", fontSize = 12.sp)
                Text(item.linkLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
            }
        }

        WidgetType.LOCATION -> {
            Row(
                modifier = Modifier.shadow(10.dp, CircleShape).clip(CircleShape).background(Color(0xF5FFFFFF)).padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("📍", fontSize = 12.sp)
                Text(item.place, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
            }
        }

        WidgetType.TIME -> {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .border(0.75.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(item.timeText, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        WidgetType.MUSIC -> {
            Row(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE817171A))
                    .border(0.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF5856D6)))),
                    contentAlignment = Alignment.Center
                ) { Text("♫", color = Color.White, fontSize = 14.sp) }
                Column {
                    Text(item.musicTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(item.musicArtist, fontSize = 10.sp, color = Color.White.copy(alpha = 0.62f))
                }
            }
        }
    }
}

/* ==============================================================================
   8. MARKUP STROKES RENDERER (UNIFIED LIVE & 1080x1920 EXPORT INK ENGINE)
   ============================================================================== */

private fun buildSmoothAndroidPath(
    points: List<NormalizedPoint>,
    w: Float,
    h: Float,
    recognizedShape: RecognizedShapeType? = null
): AndroidPath = AndroidPath().apply {
    val x0 = points[0].x * w
    val y0 = points[0].y * h
    moveTo(x0, y0)

    when {
        points.size == 1 -> lineTo(x0 + 0.1f, y0 + 0.1f)
        recognizedShape == RecognizedShapeType.ARROW && points.size >= 5 -> {
            val tipX = points[1].x * w
            val tipY = points[1].y * h
            lineTo(tipX, tipY)
            moveTo(points[2].x * w, points[2].y * h)
            lineTo(tipX, tipY)
            lineTo(points[4].x * w, points[4].y * h)
        }
        recognizedShape == RecognizedShapeType.LINE -> {
            for (i in 1 until points.size) {
                lineTo(points[i].x * w, points[i].y * h)
            }
        }
        recognizedShape == RecognizedShapeType.SQUARE ||
            recognizedShape == RecognizedShapeType.RECTANGLE ||
            recognizedShape == RecognizedShapeType.TRIANGLE -> {
            val lastIdx = if (
                points.size > 2 &&
                hypot((points.first().x - points.last().x).toDouble(), (points.first().y - points.last().y).toDouble()) < 0.002
            ) points.lastIndex else points.size
            for (i in 1 until lastIdx) {
                lineTo(points[i].x * w, points[i].y * h)
            }
            close()
        }
        recognizedShape == RecognizedShapeType.CIRCLE || recognizedShape == RecognizedShapeType.ELLIPSE -> {
            val closedLoop = points.size > 2 &&
                hypot((points.first().x - points.last().x).toDouble(), (points.first().y - points.last().y).toDouble()) < 0.004
            val endLimit = if (closedLoop) points.lastIndex else points.size
            for (i in 1 until endLimit - 1) {
                val cx = points[i].x * w
                val cy = points[i].y * h
                val nx = points[i + 1].x * w
                val ny = points[i + 1].y * h
                quadTo(cx, cy, (cx + nx) / 2f, (cy + ny) / 2f)
            }
            if (closedLoop) close() else lineTo(points.last().x * w, points.last().y * h)
        }
        points.size == 2 -> lineTo(points[1].x * w, points[1].y * h)
        else -> {
            for (i in 1 until points.size - 1) {
                val cx = points[i].x * w
                val cy = points[i].y * h
                val nx = points[i + 1].x * w
                val ny = points[i + 1].y * h
                quadTo(cx, cy, (cx + nx) / 2f, (cy + ny) / 2f)
            }
            lineTo(points.last().x * w, points.last().y * h)
        }
    }
}

private val AURORA_SHADER_COLORS = intArrayOf(
    0xFFFF2D55.toInt(),
    0xFFAF52DE.toInt(),
    0xFF0A84FF.toInt(),
    0xFF5AC8FA.toInt(),
    0xFF30D158.toInt()
)

private fun drawStrokeToAndroidCanvas(
    canvas: AndroidCanvas,
    stroke: DrawStroke,
    w: Float,
    h: Float,
    scaleRatio: Float
) {
    if (stroke.points.isEmpty()) return

    if (stroke.brush == BrushType.EMOJI) {
        val ePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textSize = max(18f, stroke.sizeDp * 2.4f) * scaleRatio
            textAlign = AndroidPaint.Align.CENTER
        }
        var lastX = -999f
        var lastY = -999f
        val minGap = max(22f, stroke.sizeDp * 2.5f) * scaleRatio
        stroke.points.forEach { pt ->
            val px = pt.x * w
            val py = pt.y * h
            if (hypot((px - lastX).toDouble(), (py - lastY).toDouble()) >= minGap) {
                canvas.drawText(stroke.emoji, px, py + ePaint.textSize / 3f, ePaint)
                lastX = px
                lastY = py
            }
        }
        return
    }

    val path = buildSmoothAndroidPath(stroke.points, w, h, stroke.recognizedShape)
    val strokePx = stroke.sizeDp * scaleRatio
    val sharpCorners = stroke.recognizedShape == RecognizedShapeType.SQUARE ||
        stroke.recognizedShape == RecognizedShapeType.RECTANGLE ||
        stroke.recognizedShape == RecognizedShapeType.TRIANGLE

    val dashEffect: DashPathEffect? = when (stroke.lineStyle) {
        StrokeLineStyle.DASHED -> DashPathEffect(
            floatArrayOf(max(8f * scaleRatio, strokePx * 2.25f), max(6f * scaleRatio, strokePx * 1.45f)),
            0f
        )
        StrokeLineStyle.DOTTED -> DashPathEffect(
            floatArrayOf(0.5f, max(6f * scaleRatio, strokePx * 1.85f)),
            0f
        )
        else -> null
    }

    fun makePaint(
        widthPx: Float,
        colorArgb: Int,
        alphaVal: Int = 255,
        blurPx: Float = 0f,
        shader: AndroidShader? = null,
        clearMode: Boolean = false
    ): AndroidPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        style = AndroidPaint.Style.STROKE
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = if (sharpCorners) AndroidPaint.Join.MITER else AndroidPaint.Join.ROUND
        strokeMiter = 4f
        strokeWidth = widthPx
        color = colorArgb
        alpha = alphaVal
        pathEffect = dashEffect
        if (shader != null) this.shader = shader
        if (blurPx > 0.5f) maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        if (clearMode) xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    if (stroke.brush == BrushType.ERASER) {
        canvas.drawPath(path, makePaint(widthPx = strokePx, colorArgb = AndroidColor.TRANSPARENT, clearMode = true))
        return
    }

    val auroraShader: AndroidShader? = if (stroke.brush == BrushType.AURORA) {
        val bounds = RectF()
        path.computeBounds(bounds, true)
        val x0 = bounds.left
        val y0 = bounds.top
        val x1 = if (bounds.width() < 14f * scaleRatio) x0 + 64f * scaleRatio else bounds.right
        val y1 = if (bounds.height() < 14f * scaleRatio) y0 + 64f * scaleRatio else bounds.bottom
        AndroidLinearGradient(x0, y0, x1, y1, AURORA_SHADER_COLORS, null, AndroidShader.TileMode.CLAMP)
    } else {
        null
    }

    val isGlowing = stroke.glowEnabled || stroke.brush == BrushType.NEON || stroke.lineStyle == StrokeLineStyle.LASER
    val baseAlpha = if (stroke.brush == BrushType.MARKER) 107 else 255

    // 1. Multi-pass Luminous Bloom Halo
    if (isGlowing || stroke.brush == BrushType.AURORA) {
        val outerBlur = (strokePx * 1.35f).coerceAtLeast(if (isGlowing) 13f * scaleRatio else 8f * scaleRatio)
        val outerWidth = strokePx * if (stroke.lineStyle == StrokeLineStyle.LASER) 2.25f else 1.72f
        val outerAlpha = when {
            stroke.brush == BrushType.MARKER -> 108
            isGlowing -> 195
            else -> 130
        }
        canvas.drawPath(
            path,
            makePaint(
                widthPx = outerWidth,
                colorArgb = stroke.colorArgb,
                alphaVal = outerAlpha,
                blurPx = outerBlur,
                shader = auroraShader
            )
        )
        if (isGlowing) {
            canvas.drawPath(
                path,
                makePaint(
                    widthPx = strokePx * 1.24f,
                    colorArgb = stroke.colorArgb,
                    alphaVal = if (stroke.brush == BrushType.MARKER) 128 else 235,
                    blurPx = (strokePx * 0.55f).coerceAtLeast(5f * scaleRatio),
                    shader = auroraShader
                )
            )
        }
    }

    // 2. Primary Ink Body (Double cuts a crisp transparent center channel on offscreen layer)
    if (stroke.lineStyle == StrokeLineStyle.DOUBLE) {
        canvas.drawPath(
            path,
            makePaint(
                widthPx = strokePx * 1.52f,
                colorArgb = stroke.colorArgb,
                alphaVal = if (stroke.brush == BrushType.MARKER) 135 else 255,
                shader = auroraShader
            )
        )
        canvas.drawPath(
            path,
            makePaint(
                widthPx = strokePx * 0.52f,
                colorArgb = AndroidColor.TRANSPARENT,
                clearMode = true
            )
        )
    } else {
        canvas.drawPath(
            path,
            makePaint(
                widthPx = strokePx,
                colorArgb = stroke.colorArgb,
                alphaVal = baseAlpha,
                shader = auroraShader
            )
        )
    }

    // 3. Incandescent Core Highlight for Glow / Laser strokes
    if ((stroke.brush == BrushType.NEON || stroke.lineStyle == StrokeLineStyle.LASER || (stroke.glowEnabled && stroke.brush == BrushType.AURORA)) &&
        stroke.lineStyle != StrokeLineStyle.DOUBLE
    ) {
        val coreRatio = if (stroke.lineStyle == StrokeLineStyle.LASER) 0.44f else 0.58f
        canvas.drawPath(
            path,
            makePaint(
                widthPx = strokePx * coreRatio,
                colorArgb = AndroidColor.WHITE,
                alphaVal = if (stroke.brush == BrushType.MARKER) 170 else 255
            )
        )
    }
}

private fun DrawScope.drawStrokesInCompose(strokes: List<DrawStroke>, densityScale: Float) {
    drawIntoCanvas { canvas ->
        strokes.forEach { stroke ->
            drawStrokeToAndroidCanvas(
                canvas = canvas.nativeCanvas,
                stroke = stroke,
                w = size.width,
                h = size.height,
                scaleRatio = densityScale
            )
        }
    }
}

/* ==============================================================================
   9. APPLE HIG BOTTOM SHEETS (STICKERS, MARKUP, LAYOUT, FILTERS, AUDIO, SHARE)
   ============================================================================== */

@Composable
private fun StickersAndWidgetsSheet(
    onClose: () -> Unit,
    onAddPhotoSticker: () -> Unit,
    onAddWidget: (WidgetType) -> Unit,
    onAddEmoji: (String) -> Unit
) {
    val colors = LocalHigColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Stickers & Widgets", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        HigPressable(onClick = onClose, modifier = Modifier.size(32.dp)) { Icon3D(Icon3DName.CLOSE, size = 18.dp) }
    }
    Spacer(Modifier.height(10.dp))
    Text("INTERACTIVE & CUTOUTS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, letterSpacing = 0.8.sp)
    Spacer(Modifier.height(8.dp))

    val widgetButtons = listOf(
        Triple("Photo", Icon3DName.IMAGES, null),
        Triple("Poll", Icon3DName.POLL, WidgetType.POLL),
        Triple("Slider", Icon3DName.SLIDER_WIDGET, WidgetType.SLIDER),
        Triple("Quiz", Icon3DName.QUIZ, WidgetType.QUIZ),
        Triple("Timer", Icon3DName.TIMER, WidgetType.COUNTDOWN),
        Triple("Ask Me", Icon3DName.QUESTION, WidgetType.QUESTION),
        Triple("Mention", Icon3DName.MENTION, WidgetType.MENTION),
        Triple("Link", Icon3DName.LINK, WidgetType.LINK),
        Triple("Place", Icon3DName.MAP_PIN, WidgetType.LOCATION),
        Triple("Clock", Icon3DName.CLOCK, WidgetType.TIME)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        widgetButtons.chunked(5).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                rowItems.forEach { (label, icon, type) ->
                    HigPressable(
                        onClick = { if (type == null) onAddPhotoSticker() else onAddWidget(type) },
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon3D(icon, size = 23.dp)
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    Text("EMOJIS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, letterSpacing = 0.8.sp)
    Spacer(Modifier.height(6.dp))
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = Modifier.height(132.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(STICKER_EMOJIS) { em ->
            HigPressable(onClick = { onAddEmoji(em) }, modifier = Modifier.height(38.dp)) {
                Text(em, fontSize = 23.sp)
            }
        }
    }
}

@Composable
private fun MarkupDrawSheet(
    brush: BrushType,
    lineStyle: StrokeLineStyle,
    glowEnabled: Boolean,
    autoShapeEnabled: Boolean,
    colorArgb: Int,
    customArgb: Int?,
    emoji: String,
    sizeDp: Float,
    canUndoStroke: Boolean,
    canRefineLastStroke: Boolean,
    onBrushChange: (BrushType) -> Unit,
    onLineStyleChange: (StrokeLineStyle) -> Unit,
    onToggleGlow: () -> Unit,
    onToggleAutoShape: () -> Unit,
    onRefineLastStroke: () -> Unit,
    onColorChange: (Int) -> Unit,
    onEmojiChange: (String) -> Unit,
    onSizeChange: (Float) -> Unit,
    onPickEyedropper: () -> Unit,
    onUndoStroke: () -> Unit,
    onCollapse: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit
) {
    val colors = LocalHigColors.current
    val density = LocalDensity.current

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Markup", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (brush != BrushType.EMOJI && brush != BrushType.ERASER) {
                HigPressable(
                    onClick = onRefineLastStroke,
                    enabled = canRefineLastStroke,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (canRefineLastStroke) colors.accent.copy(alpha = 0.14f) else colors.bgSurfaceSecondary)
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon3D(Icon3DName.SPARKLES, size = 14.dp, alpha = if (canRefineLastStroke) 1f else 0.45f)
                        Text(
                            "Refine",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (canRefineLastStroke) colors.accent else colors.textTertiary
                        )
                    }
                }
            }
            HigPressable(onClick = onUndoStroke, enabled = canUndoStroke, modifier = Modifier.size(28.dp)) {
                Icon3D(Icon3DName.UNDO, size = 18.dp, alpha = if (canUndoStroke) 1f else 0.4f)
            }
            HigPressable(onClick = onPickEyedropper, modifier = Modifier.size(28.dp)) { Icon3D(Icon3DName.PIPETTE, size = 18.dp) }
            HigPressable(onClick = onCollapse, modifier = Modifier.size(28.dp)) { Icon3D(Icon3DName.CHEVRON_DOWN, size = 19.dp) }
            HigPressable(onClick = onClear, enabled = canUndoStroke) {
                Text("Clear", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (canUndoStroke) colors.semanticRed else colors.textTertiary)
            }
            HigPressable(onClick = onDone) { Text("Done", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.accent) }
        }
    }
    Spacer(Modifier.height(10.dp))

    // 6-Brush Segmented Control
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgSurfaceSecondary)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        BrushType.entries.forEach { b ->
            val selected = b == brush
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) colors.bgSurface else Color.Transparent)
                    .clickable { onBrushChange(b) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = b.label,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }

    Spacer(Modifier.height(10.dp))

    if (brush == BrushType.EMOJI) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BRUSH_EMOJIS.forEach { em ->
                HigPressable(onClick = { onEmojiChange(em) }) {
                    Text(em, fontSize = if (em == emoji) 26.sp else 21.sp)
                }
            }
        }
    } else {
        // Line Styles (Solid, Dashed, Dotted, Double, Laser) + Glow + Auto-Shape
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (brush != BrushType.ERASER) {
                val glowOn = glowEnabled || brush == BrushType.NEON
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (glowOn) colors.accent else colors.bgSurfaceSecondary)
                        .clickable { if (brush != BrushType.NEON) onToggleGlow() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (glowOn) "✦ Glow On" else "✦ Glow",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (glowOn) Color.White else colors.textPrimary
                    )
                }
                Box(Modifier.width(0.5.dp).height(16.dp).background(colors.separator))
            }

            StrokeLineStyle.entries.forEach { style ->
                val selected = style == lineStyle
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) colors.textPrimary else colors.bgSurfaceSecondary)
                        .clickable { onLineStyleChange(style) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = style.label,
                        fontSize = 11.5.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) colors.bgSurface else colors.textSecondary
                    )
                }
            }

            if (brush != BrushType.ERASER) {
                Box(Modifier.width(0.5.dp).height(16.dp).background(colors.separator))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (autoShapeEnabled) colors.accent.copy(alpha = 0.16f) else colors.bgSurfaceSecondary)
                        .border(
                            0.75.dp,
                            if (autoShapeEnabled) colors.accent else Color.Transparent,
                            CircleShape
                        )
                        .clickable(onClick = onToggleAutoShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (autoShapeEnabled) "◯ Auto-Shape" else "◯ Freehand",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (autoShapeEnabled) colors.accent else colors.textSecondary
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Live Stroke Sample Curve + Brush Indicator
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (brush == BrushType.ERASER) colors.bgSurfaceSecondary else Color(0xFF14161B)),
            contentAlignment = Alignment.Center
        ) {
            if (brush == BrushType.EMOJI) {
                Text(emoji, fontSize = (sizeDp * 0.65f).coerceIn(13f, 22f).sp)
            } else {
                val sampleStroke = remember(brush, lineStyle, glowEnabled, colorArgb, sizeDp) {
                    DrawStroke(
                        brush = brush,
                        colorArgb = colorArgb,
                        sizeDp = (sizeDp * 0.42f).coerceIn(2.5f, 10f),
                        lineStyle = lineStyle,
                        glowEnabled = glowEnabled,
                        points = listOf(
                            NormalizedPoint(0.14f, 0.72f),
                            NormalizedPoint(0.38f, 0.25f),
                            NormalizedPoint(0.64f, 0.75f),
                            NormalizedPoint(0.88f, 0.28f)
                        )
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                ) {
                    if (brush == BrushType.ERASER) {
                        drawIntoCanvas { canvas ->
                            drawStrokeToAndroidCanvas(
                                canvas = canvas.nativeCanvas,
                                stroke = sampleStroke.copy(brush = BrushType.PEN, colorArgb = colors.textTertiary.toArgb()),
                                w = size.width,
                                h = size.height,
                                scaleRatio = density.density
                            )
                        }
                    } else {
                        drawStrokesInCompose(listOf(sampleStroke), density.density)
                    }
                }
            }
        }
        Slider(
            value = sizeDp,
            onValueChange = onSizeChange,
            valueRange = 2f..34f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = colors.accent, inactiveTrackColor = colors.bgSurfaceTertiary)
        )
        Text("${sizeDp.roundToInt()}px", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = colors.textPrimary, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
    }

    Spacer(Modifier.height(6.dp))
    ColorSwatchesRow(selectedArgb = colorArgb, customArgb = customArgb, onSelect = onColorChange)
}

@Composable
private fun LayoutGridSheet(
    activeLayout: LayoutType,
    onSelectLayout: (LayoutType) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalHigColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Layout Grid", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text("Tap any grid frame on canvas to insert a photo", fontSize = 12.sp, color = colors.textSecondary)
        }
        HigPressable(onClick = onClose, modifier = Modifier.size(32.dp)) { Icon3D(Icon3DName.CLOSE, size = 18.dp) }
    }
    Spacer(Modifier.height(14.dp))

    val options = listOf(
        LayoutType.NONE to Icon3DName.LAYOUT_SINGLE,
        LayoutType.SPLIT_H to Icon3DName.LAYOUT_ROWS2,
        LayoutType.SPLIT_V to Icon3DName.LAYOUT_COLS2,
        LayoutType.GRID_3 to Icon3DName.LAYOUT_GRID3,
        LayoutType.GRID_4 to Icon3DName.LAYOUT_GRID4
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        options.forEach { (layout, icon) ->
            val selected = layout == activeLayout
            HigPressable(onClick = { onSelectLayout(layout) }, modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon3D(icon, size = 26.dp, alpha = if (selected) 1f else 0.56f, modifier = Modifier.scale(if (selected) 1.08f else 1f))
                    Text(
                        text = layout.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) colors.accent else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun FiltersAdjustSheet(
    filters: FilterState,
    previewBitmap: Bitmap?,
    backdrop: StoryBackdrop,
    onPresetSelected: (FilterPreset) -> Unit,
    onFilterChanged: (FilterState) -> Unit,
    onCommitHistory: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalHigColors.current
    val thumbImageBmp = remember(previewBitmap) { previewBitmap?.asImageBitmap() }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Filters & Light", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HigPressable(onClick = onReset) { Text("Reset", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.accent) }
            HigPressable(onClick = onClose, modifier = Modifier.size(30.dp)) { Icon3D(Icon3DName.CLOSE, size = 18.dp) }
        }
    }
    Spacer(Modifier.height(10.dp))

    // Visual Filter Preset Thumbnails (Live ColorMatrix Preview)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(FILTER_PRESETS) { preset ->
            val selected = preset.name == filters.preset
            val presetFilter = remember(preset) {
                buildComposeColorFilter(
                    FilterState(
                        preset = preset.name,
                        brightness = preset.brightness,
                        contrast = preset.contrast,
                        saturate = preset.saturate,
                        sepia = preset.sepia,
                        grayscale = preset.grayscale,
                        vignette = preset.vignette
                    )
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onPresetSelected(preset) }
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 66.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (selected) 2.dp else 0.75.dp,
                            color = if (selected) colors.accent else colors.separator,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(Brush.linearGradient(listOf(backdrop.startColor, backdrop.endColor)))
                ) {
                    if (thumbImageBmp != null) {
                        Image(
                            bitmap = thumbImageBmp,
                            contentDescription = preset.name,
                            contentScale = ContentScale.Crop,
                            colorFilter = presetFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        when (preset.name) {
                                            "Vivid" -> listOf(Color(0xFF0A84FF), Color(0xFFFF375F))
                                            "Warm" -> listOf(Color(0xFFFF9F0A), Color(0xFFD97706))
                                            "Cool" -> listOf(Color(0xFF38BDF8), Color(0xFF3730A3))
                                            "Noir" -> listOf(Color(0xFF52525B), Color(0xFF09090B))
                                            "Fade" -> listOf(Color(0xFF94A3B8), Color(0xFF475569))
                                            "Dramatic" -> listOf(Color(0xFF312E81), Color(0xFF09090B))
                                            else -> listOf(backdrop.startColor, backdrop.endColor)
                                        }
                                    )
                                )
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = preset.name,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) colors.accent else colors.textSecondary
                )
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.separator))
    Spacer(Modifier.height(8.dp))

    @Composable
    fun AdjustRow(icon: Icon3DName, label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, onFinished: () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon3D(icon, size = 18.dp)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary, modifier = Modifier.width(62.dp))
            Slider(
                value = value,
                onValueChange = onChange,
                onValueChangeFinished = onFinished,
                valueRange = range,
                modifier = Modifier.weight(1f).height(32.dp),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = colors.accent, inactiveTrackColor = colors.bgSurfaceTertiary)
            )
        }
    }

    AdjustRow(Icon3DName.SUN, "Exposure", filters.brightness, 60f..140f,
        { onFilterChanged(filters.copy(brightness = it, preset = "Custom")) }, onCommitHistory)
    AdjustRow(Icon3DName.CONTRAST, "Contrast", filters.contrast, 60f..150f,
        { onFilterChanged(filters.copy(contrast = it, preset = "Custom")) }, onCommitHistory)
    AdjustRow(Icon3DName.DROPLET, "Vibrance", filters.saturate, 0f..180f,
        { onFilterChanged(filters.copy(saturate = it, preset = "Custom")) }, onCommitHistory)
    AdjustRow(Icon3DName.THERMOMETER, "Warmth", filters.sepia, 0f..60f,
        { onFilterChanged(filters.copy(sepia = it, preset = "Custom")) }, onCommitHistory)
    AdjustRow(Icon3DName.APERTURE, "Vignette", filters.vignette, 0f..85f,
        { onFilterChanged(filters.copy(vignette = it, preset = "Custom")) }, onCommitHistory)
}

@Composable
private fun SoundtrackSheet(
    activeMusicTitle: String?,
    onSelectTrack: (MusicTrack) -> Unit,
    onRemoveMusic: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalHigColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Soundtrack", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (activeMusicTitle != null) {
                HigPressable(onClick = onRemoveMusic) {
                    Text("Remove", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.semanticRed)
                }
            }
            HigPressable(onClick = onClose, modifier = Modifier.size(30.dp)) { Icon3D(Icon3DName.CLOSE, size = 18.dp) }
        }
    }
    Spacer(Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MUSIC_TRACKS.forEach { track ->
            val isActive = track.title == activeMusicTitle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.bgSurface)
                    .border(0.75.dp, if (isActive) colors.accent else colors.separator, RoundedCornerShape(16.dp))
                    .clickable { onSelectTrack(track) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon3D(Icon3DName.MUSIC, size = 22.dp)
                    Column {
                        Text(track.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("${track.artist} · ${track.duration}", fontSize = 12.sp, color = colors.textSecondary)
                    }
                }
                Text(if (isActive) "Active" else "Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
            }
        }
    }
}

@Composable
private fun ShareExportSheet(
    audience: StoryAudience,
    slideCount: Int,
    isExporting: Boolean,
    canPublishToApp: Boolean = false,
    onAudienceChange: (StoryAudience) -> Unit,
    onPublishToApp: () -> Unit = {},
    onSavePng: () -> Unit,
    onSaveAllSlides: () -> Unit,
    onSystemShare: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalHigColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Share Story", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        HigPressable(onClick = onClose, enabled = !isExporting, modifier = Modifier.size(30.dp)) { Icon3D(Icon3DName.CLOSE, size = 18.dp) }
    }
    Spacer(Modifier.height(14.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgSurface)
                .border(1.dp, if (audience == StoryAudience.PUBLIC) colors.accent else colors.separator, RoundedCornerShape(16.dp))
                .clickable(enabled = !isExporting) { onAudienceChange(StoryAudience.PUBLIC) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon3D(Icon3DName.GLOBE, size = 24.dp)
            Column {
                Text("Your Story", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text("All followers", fontSize = 11.sp, color = colors.textSecondary)
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgSurface)
                .border(1.dp, if (audience == StoryAudience.CLOSE_FRIENDS) colors.semanticGreen else colors.separator, RoundedCornerShape(16.dp))
                .clickable(enabled = !isExporting) { onAudienceChange(StoryAudience.CLOSE_FRIENDS) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon3D(Icon3DName.STAR, size = 24.dp)
            Column {
                Text("Close Friends", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text("Private ring", fontSize = 11.sp, color = colors.textSecondary)
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    if (canPublishToApp) {
        HigPressable(
            onClick = onPublishToApp,
            enabled = !isExporting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (audience == StoryAudience.CLOSE_FRIENDS) colors.semanticGreen else colors.accent)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Text("Preparing Story...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                } else {
                    Icon3D(Icon3DName.SEND, size = 18.dp)
                    Text(
                        text = if (audience == StoryAudience.CLOSE_FRIENDS) "Share to Close Friends" else "Share to Your Story",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    HigPressable(
        onClick = onSavePng,
        enabled = !isExporting,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (canPublishToApp) 44.dp else 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (canPublishToApp) colors.bgSurface else colors.accent)
            .then(if (canPublishToApp) Modifier.border(0.75.dp, colors.separator, RoundedCornerShape(16.dp)) else Modifier)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isExporting && !canPublishToApp) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Text("Rendering 1080×1920 PNG...", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            } else {
                Icon3D(Icon3DName.DOWNLOAD, size = 18.dp)
                Text(
                    "Save Current Slide (1080×1920 PNG)",
                    fontSize = if (canPublishToApp) 14.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (canPublishToApp) colors.textPrimary else Color.White
                )
            }
        }
    }

    if (slideCount > 1) {
        Spacer(Modifier.height(8.dp))
        HigPressable(
            onClick = onSaveAllSlides,
            enabled = !isExporting,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgSurface)
                .border(0.75.dp, colors.accent.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon3D(Icon3DName.LAYERS, size = 18.dp)
                Text("Save All $slideCount Slides to Gallery", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    HigPressable(
        onClick = onSystemShare,
        enabled = !isExporting,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgSurface)
            .border(0.75.dp, colors.separator, RoundedCornerShape(16.dp))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon3D(Icon3DName.SHARE, size = 18.dp)
            Text("System Share Sheet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        }
    }
}

@Composable
private fun ColorSwatchesRow(
    selectedArgb: Int,
    customArgb: Int?,
    onSelect: (Int) -> Unit
) {
    val colors = LocalHigColors.current
    val allSwatches = buildList {
        if (customArgb != null && APPLE_SWATCH_COLORS.none { it.toArgb() == customArgb }) {
            add(Color(customArgb))
        }
        addAll(APPLE_SWATCH_COLORS)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        allSwatches.forEach { swatch ->
            val argb = swatch.toArgb()
            val isSelected = argb == selectedArgb
            Box(
                modifier = Modifier
                    .size(if (isSelected) 32.dp else 28.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) Modifier.border(2.dp, colors.accent, CircleShape).padding(3.dp)
                        else Modifier
                    )
                    .clip(CircleShape)
                    .background(swatch)
                    .border(1.5.dp, Color.White.copy(alpha = 0.72f), CircleShape)
                    .clickable { onSelect(argb) }
            )
        }
    }
}

/* ==============================================================================
   10. FULLSCREEN ADVANCED TYPOGRAPHY COMPOSER OVERLAY
   ============================================================================== */

@Composable
private fun TextComposerOverlay(
    text: String,
    font: TextFontStyle,
    colorArgb: Int,
    customArgb: Int?,
    bgMode: TextBgMode,
    align: TextAlignmentMode,
    sizeSp: Float,
    anim: TextAnimStyle,
    onTextChange: (String) -> Unit,
    onFontChange: (TextFontStyle) -> Unit,
    onColorChange: (Int) -> Unit,
    onBgModeCycle: () -> Unit,
    onAlignCycle: () -> Unit,
    onAnimCycle: () -> Unit,
    onSizeChange: (Float) -> Unit,
    onPickEyedropper: () -> Unit,
    onDone: () -> Unit
) {
    val colors = LocalHigColors.current
    val textColor = Color(colorArgb)
    val contentColor = if (bgMode == TextBgMode.PILL && isLightArgb(colorArgb)) Color.Black else textColor
    val fontFamily = when (font) {
        TextFontStyle.SERIF, TextFontStyle.ITALIC -> FontFamily.Serif
        TextFontStyle.MONO -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    val textAlign = when (align) {
        TextAlignmentMode.LEFT -> TextAlign.Left
        TextAlignmentMode.RIGHT -> TextAlign.Right
        TextAlignmentMode.CENTER -> TextAlign.Center
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(50)
        try { focusRequester.requestFocus() } catch (_: Throwable) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.84f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Controls
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HigPressable(onClick = onAlignCycle, modifier = Modifier.size(36.dp)) {
                    Icon3D(
                        when (align) {
                            TextAlignmentMode.LEFT -> Icon3DName.ALIGN_LEFT
                            TextAlignmentMode.RIGHT -> Icon3DName.ALIGN_RIGHT
                            TextAlignmentMode.CENTER -> Icon3DName.ALIGN_CENTER
                        },
                        size = 20.dp
                    )
                }
                HigPressable(onClick = onBgModeCycle) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon3D(Icon3DName.HIGHLIGHTER, size = 18.dp)
                        Text(bgMode.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                HigPressable(onClick = onAnimCycle) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon3D(Icon3DName.SPARKLES, size = 18.dp)
                        Text(anim.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                HigPressable(onClick = onPickEyedropper, modifier = Modifier.size(36.dp)) {
                    Icon3D(Icon3DName.PIPETTE, size = 19.dp)
                }
            }
            HigPressable(onClick = onDone) {
                Text("Done", color = colors.accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Center Input + Left-Edge Mobile Vertical Size Slider
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Left-Edge Vertical Size Slider (Just like Instagram Stories)
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("A", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.height(140.dp).width(32.dp), contentAlignment = Alignment.Center) {
                    Slider(
                        value = sizeSp,
                        onValueChange = onSizeChange,
                        valueRange = 16f..64f,
                        modifier = Modifier
                            .width(136.dp)
                            .graphicsLayer { rotationZ = -90f },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
                Text("A", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            }

            // Styled Live Input Box
            val inputContainerMod = when (bgMode) {
                TextBgMode.CLEAN -> Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                TextBgMode.PILL -> Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(textColor)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                TextBgMode.TRANSLUCENT -> Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xC71C1C1E))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                TextBgMode.OUTLINE -> Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, textColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            }

            Box(
                modifier = Modifier
                    .padding(start = 40.dp, end = 16.dp)
                    .then(inputContainerMod),
                contentAlignment = Alignment.Center
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Type something...",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = sizeSp.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = textAlign
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    cursorBrush = SolidColor(colors.accent),
                    textStyle = TextStyle(
                        color = contentColor,
                        fontSize = sizeSp.sp,
                        lineHeight = (sizeSp * 1.2f).sp,
                        fontFamily = fontFamily,
                        fontWeight = if (font == TextFontStyle.BOLD) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontStyle = if (font == TextFontStyle.ITALIC) FontStyle.Italic else FontStyle.Normal,
                        textAlign = textAlign,
                        shadow = if (font == TextFontStyle.NEON) Shadow(textColor, Offset.Zero, 26f) else null
                    ),
                    modifier = Modifier
                        .widthIn(min = 160.dp, max = 300.dp)
                        .focusRequester(focusRequester)
                )
            }
        }

        // Bottom Font Style Chips & Apple Swatches
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextFontStyle.entries.forEach { f ->
                    val selected = f == font
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) Color.White else Color.Transparent)
                            .border(if (selected) 0.dp else 1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                            .clickable { onFontChange(f) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (f == TextFontStyle.BOLD) f.label.uppercase() else f.label,
                            fontSize = 13.sp,
                            fontWeight = if (f == TextFontStyle.BOLD) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontStyle = if (f == TextFontStyle.ITALIC) FontStyle.Italic else FontStyle.Normal,
                            fontFamily = when (f) {
                                TextFontStyle.SERIF, TextFontStyle.ITALIC -> FontFamily.Serif
                                TextFontStyle.MONO -> FontFamily.Monospace
                                else -> FontFamily.SansSerif
                            },
                            color = if (selected) Color.Black else Color.White
                        )
                    }
                }
            }

            ColorSwatchesRow(selectedArgb = colorArgb, customArgb = customArgb, onSelect = onColorChange)
        }
    }
}

/* ==============================================================================
   11. WIDGET CONFIGURATION DIALOG (POLL, SLIDER, QUIZ A/B/C, TIMER, ETC.)
   ============================================================================== */

@Composable
private fun WidgetEditDialog(
    widget: WidgetStoryItem,
    onDismiss: () -> Unit,
    onSave: (WidgetStoryItem) -> Unit
) {
    val colors = LocalHigColors.current
    var q by remember(widget) { mutableStateOf(widget.question) }
    var optA by remember(widget) { mutableStateOf(widget.optA) }
    var optB by remember(widget) { mutableStateOf(widget.optB) }
    var prompt by remember(widget) { mutableStateOf(widget.prompt) }
    var emoji by remember(widget) { mutableStateOf(widget.emoji) }
    var sliderVal by remember(widget) { mutableFloatStateOf(widget.sliderValue.toFloat()) }
    var q0 by remember(widget) { mutableStateOf(widget.quizOpts.getOrElse(0) { "A" }) }
    var q1 by remember(widget) { mutableStateOf(widget.quizOpts.getOrElse(1) { "B" }) }
    var q2 by remember(widget) { mutableStateOf(widget.quizOpts.getOrElse(2) { "C" }) }
    var correctIdx by remember(widget) { mutableIntStateOf(widget.correctIndex) }
    var title by remember(widget) { mutableStateOf(widget.title) }
    var days by remember(widget) { mutableStateOf(widget.days) }
    var hours by remember(widget) { mutableStateOf(widget.hours) }
    var mins by remember(widget) { mutableStateOf(widget.mins) }
    var handle by remember(widget) { mutableStateOf(widget.handle) }
    var link by remember(widget) { mutableStateOf(widget.linkLabel) }
    var place by remember(widget) { mutableStateOf(widget.place) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.separator
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .imePadding()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.bgSurface)
                .border(0.5.dp, colors.separator, RoundedCornerShape(24.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Customize Widget", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)

            when (widget.widgetType) {
                WidgetType.POLL -> {
                    OutlinedTextField(q, { q = it }, label = { Text("Question") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(optA, { optA = it }, label = { Text("Option 1") }, singleLine = true, colors = fieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(optB, { optB = it }, label = { Text("Option 2") }, singleLine = true, colors = fieldColors, modifier = Modifier.weight(1f))
                    }
                }
                WidgetType.SLIDER -> {
                    OutlinedTextField(prompt, { prompt = it }, label = { Text("Prompt") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(emoji, { if (it.length <= 4) emoji = it }, singleLine = true, colors = fieldColors, modifier = Modifier.width(68.dp))
                        Slider(value = sliderVal, onValueChange = { sliderVal = it }, valueRange = 10f..95f, modifier = Modifier.weight(1f))
                    }
                }
                WidgetType.QUIZ -> {
                    OutlinedTextField(q, { q = it }, label = { Text("Quiz Question") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                    Text("Tap A, B, or C to mark the correct answer:", fontSize = 11.sp, color = colors.textSecondary)
                    listOf(Triple(0, "A", q0 to { v: String -> q0 = v }), Triple(1, "B", q1 to { v: String -> q1 = v }), Triple(2, "C", q2 to { v: String -> q2 = v }))
                        .forEach { (idx, lbl, pair) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.5.dp, if (idx == correctIdx) colors.semanticGreen else colors.separator, RoundedCornerShape(12.dp))
                                        .clickable { correctIdx = idx },
                                    contentAlignment = Alignment.Center
                                ) { Text(lbl, fontWeight = FontWeight.Bold, color = if (idx == correctIdx) colors.semanticGreen else colors.textSecondary) }
                                OutlinedTextField(pair.first, pair.second, singleLine = true, colors = fieldColors, modifier = Modifier.weight(1f))
                            }
                        }
                }
                WidgetType.COUNTDOWN -> {
                    OutlinedTextField(title, { title = it }, label = { Text("Event Name") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(days, { if (it.length <= 2) days = it }, label = { Text("Days") }, singleLine = true, colors = fieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(hours, { if (it.length <= 2) hours = it }, label = { Text("Hrs") }, singleLine = true, colors = fieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(mins, { if (it.length <= 2) mins = it }, label = { Text("Min") }, singleLine = true, colors = fieldColors, modifier = Modifier.weight(1f))
                    }
                }
                WidgetType.QUESTION -> OutlinedTextField(prompt, { prompt = it }, label = { Text("Ask prompt") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                WidgetType.MENTION -> OutlinedTextField(handle, { handle = it }, label = { Text("@username") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                WidgetType.LINK -> OutlinedTextField(link, { link = it }, label = { Text("URL / Label") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                WidgetType.LOCATION -> OutlinedTextField(place, { place = it }, label = { Text("City or Venue") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                else -> Unit
            }

            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                HigPressable(onClick = onDismiss, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                }
                HigPressable(
                    onClick = {
                        val updated = when (widget.widgetType) {
                            WidgetType.POLL -> widget.copy(question = q.trim().ifEmpty { "Poll" }, optA = optA.trim().ifEmpty { "Yes" }, optB = optB.trim().ifEmpty { "No" })
                            WidgetType.SLIDER -> widget.copy(prompt = prompt.trim().ifEmpty { "Rate this" }, emoji = emoji.trim().ifEmpty { "🔥" }, sliderValue = sliderVal.roundToInt())
                            WidgetType.QUIZ -> widget.copy(question = q.trim().ifEmpty { "Quiz" }, quizOpts = listOf(q0.trim().ifEmpty { "A" }, q1.trim().ifEmpty { "B" }, q2.trim().ifEmpty { "C" }), correctIndex = correctIdx)
                            WidgetType.COUNTDOWN -> widget.copy(
                                title = title.trim().ifEmpty { "EVENT" }.uppercase(),
                                days = days.trim().ifEmpty { "00" }.padStart(2, '0'),
                                hours = hours.trim().ifEmpty { "00" }.padStart(2, '0'),
                                mins = mins.trim().ifEmpty { "00" }.padStart(2, '0')
                            )
                            WidgetType.QUESTION -> widget.copy(prompt = prompt.trim().ifEmpty { "Ask me anything" })
                            WidgetType.MENTION -> {
                                val raw = handle.trim().ifEmpty { "USER" }
                                widget.copy(handle = (if (raw.startsWith("@")) raw else "@$raw").uppercase())
                            }
                            WidgetType.LINK -> widget.copy(linkLabel = link.trim().ifEmpty { "apple.com" })
                            WidgetType.LOCATION -> widget.copy(place = place.trim().ifEmpty { "LOCATION" }.uppercase())
                            else -> widget
                        }
                        onSave(updated)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.accent)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Update", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

/* ==============================================================================
   12. FULLSCREEN MULTI-SLIDE STORY PLAYER (HOLD-TO-PAUSE + LIVE TEXT ANIMATION)
   ============================================================================== */

@Composable
private fun StoryPreviewPlayer(
    slides: List<StorySlide>,
    audience: StoryAudience,
    bitmapCache: Map<String, Bitmap>,
    stageWdp: Float,
    stageHdp: Float,
    onPlayChord: (Double) -> Unit,
    onClose: () -> Unit
) {
    val playerScope = rememberCoroutineScope()
    var slideIdx by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var renderedBmp by remember { mutableStateOf<Bitmap?>(null) }

    val slide = slides[slideIdx.coerceIn(0, slides.lastIndex)]
    val durationMs = 4500L

    // Render base image (without animated text, which animates live above!)
    LaunchedEffect(slideIdx) {
        progress = 0f
        val nextBmp = withContext(Dispatchers.Default) {
            renderCompositeBitmap(slide, bitmapCache, 1080, 1920, stageWdp, stageHdp, skipAnimatedText = true)
        }
        val old = renderedBmp
        renderedBmp = nextBmp
        if (old != null && !old.isRecycled) old.recycle()
        val music = slide.items.filterIsInstance<WidgetStoryItem>().firstOrNull { it.widgetType == WidgetType.MUSIC }
        if (music != null) onPlayChord(music.musicFreq)
    }

    DisposableEffect(Unit) {
        onDispose {
            renderedBmp?.let { if (!it.isRecycled) it.recycle() }
            renderedBmp = null
        }
    }

    // 60fps progress loop honoring hold-to-pause
    LaunchedEffect(slideIdx, isPaused) {
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dtMs = (now - lastTime) / 1_000_000f
                lastTime = now
                if (!isPaused) {
                    progress += dtMs / durationMs
                }
            }
            if (progress >= 1f) {
                if (slideIdx < slides.lastIndex) {
                    slideIdx++
                } else {
                    onClose()
                }
                break
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val targetRatio = 9f / 16f
        val availRatio = maxWidth / maxHeight
        val previewW = if (availRatio > targetRatio) maxHeight * targetRatio else maxWidth
        val previewH = if (availRatio > targetRatio) maxHeight else maxWidth / targetRatio
        val scaleFactor = previewW.value / stageWdp.coerceAtLeast(180f)
        val previewImageBmp = remember(renderedBmp) { renderedBmp?.asImageBitmap() }

        Box(
            modifier = Modifier
                .size(width = previewW, height = previewH)
                .background(Color(0xFF141416))
        ) {
            // Base rendered slide
            if (previewImageBmp != null) {
                Image(
                    bitmap = previewImageBmp,
                    contentDescription = "Story Slide Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Live animated text overlays
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                slide.items.filterIsInstance<TextStoryItem>().filter { it.anim != TextAnimStyle.NONE }.forEach { item ->
                    val anim = remember(slideIdx, item.id) { Animatable(0f) }
                    LaunchedEffect(slideIdx, item.id) {
                        anim.snapTo(0f)
                        anim.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
                    }
                    val p = anim.value
                    Box(
                        modifier = Modifier.graphicsLayer {
                            translationX = (item.xDp * scaleFactor).dp.toPx()
                            translationY = (item.yDp * scaleFactor).dp.toPx() +
                                if (item.anim == TextAnimStyle.SLIDE) (1f - p) * 22.dp.toPx() else 0f
                            val popMult = if (item.anim == TextAnimStyle.POP) 0.72f + p * 0.28f else 1f
                            scaleX = item.scale * popMult
                            scaleY = item.scale * popMult
                            rotationZ = item.rotation
                            alpha = p
                        }
                    ) {
                        StoryTextNode(item, scaleFactor = scaleFactor)
                    }
                }
            }

            // Unified Tap-Left / Tap-Right + Hold-to-Pause Touch Surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, bottom = 84.dp)
                    .pointerInput(slideIdx, slides.size) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val downMs = System.currentTimeMillis()
                            val pauseJob = playerScope.launch {
                                delay(170L)
                                isPaused = true
                            }
                            while (true) {
                                val ev = awaitPointerEvent()
                                if (ev.changes.none { it.pressed }) break
                            }
                            pauseJob.cancel()
                            val held = System.currentTimeMillis() - downMs >= 170L
                            if (held) {
                                isPaused = false
                            } else {
                                val relX = down.position.x / size.width.coerceAtLeast(1)
                                if (relX < 0.33f) {
                                    if (slideIdx > 0) slideIdx-- else progress = 0f
                                } else {
                                    if (slideIdx < slides.lastIndex) slideIdx++ else onClose()
                                }
                            }
                        }
                    }
            )

            // Segmented Progress Bars + Header
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                    slides.forEachIndexed { idx, _ ->
                        val barFill = when {
                            idx < slideIdx -> 1f
                            idx == slideIdx -> progress.coerceIn(0f, 1f)
                            else -> 0f
                        }
                        Box(Modifier.weight(1f).height(2.5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(barFill).background(Color.White))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (audience == StoryAudience.CLOSE_FRIENDS) Brush.linearGradient(listOf(Color(0xFF34C759), Color(0xFF30D158)))
                                    else Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF5856D6)))
                                )
                                .padding(2.dp)
                        ) {
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF17171A)), contentAlignment = Alignment.Center) {
                                Text("You", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column {
                            Text(
                                text = if (audience == StoryAudience.CLOSE_FRIENDS) "Close Friends" else "Your Story",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isPaused) "Paused" else "Slide ${slideIdx + 1}/${slides.size} · Hold to pause",
                                color = Color.White.copy(alpha = 0.68f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    HigPressable(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon3D(Icon3DName.CLOSE, size = 20.dp)
                    }
                }
            }

            // Simulated Bottom Reply Bar (Zero Icon Backgrounds)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("Send message...", color = Color.White.copy(alpha = 0.62f), fontSize = 13.sp)
                }
                Icon3D(Icon3DName.HEART, size = 22.dp)
                Icon3D(Icon3DName.SEND, size = 21.dp)
            }
        }
    }
}

/* ==============================================================================
   13. HIGH-RESOLUTION 1080x1920 ANDROID CANVAS COMPOSITOR & GALLERY EXPORT
   ============================================================================== */

private fun renderCompositeBitmap(
    slide: StorySlide,
    bitmaps: Map<String, Bitmap>,
    outW: Int,
    outH: Int,
    stageWdp: Float,
    stageHdp: Float,
    skipAnimatedText: Boolean = false,
    skipWidgets: Boolean = false
): Bitmap {
    val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val ratioX = outW / stageWdp.coerceAtLeast(1f)
    val ratioY = outH / stageHdp.coerceAtLeast(1f)
    val ratio = min(ratioX, ratioY)

    // 1. Backdrop Gradient
    val bg = STORY_BACKDROPS[slide.backdropIndex]
    val bgPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        shader = AndroidLinearGradient(
            0f, 0f, outW * 0.4f, outH.toFloat(),
            bg.startColor.toArgb(), bg.endColor.toArgb(), AndroidShader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), bgPaint)

    val filterPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG).apply {
        colorFilter = AndroidColorMatrixColorFilter(buildFilterAndroidColorMatrix(slide.filters))
    }

    fun drawCoverBitmap(bmp: Bitmap, dest: RectF) {
        val scale = max(dest.width() / bmp.width, dest.height() / bmp.height)
        val sw = dest.width() / scale
        val sh = dest.height() / scale
        val sx = (bmp.width - sw) / 2f
        val sy = (bmp.height - sh) / 2f
        val src = android.graphics.Rect(sx.roundToInt(), sy.roundToInt(), (sx + sw).roundToInt(), (sy + sh).roundToInt())
        canvas.drawBitmap(bmp, src, dest, filterPaint)
    }

    // 2. Layout Grid OR Single Photo
    if (slide.layout != LayoutType.NONE) {
        val gap = 8f * (outW / 1080f)
        val w = outW.toFloat()
        val h = outH.toFloat()
        val rects = when (slide.layout) {
            LayoutType.SPLIT_H -> listOf(RectF(0f, 0f, w, h / 2f - gap / 2f), RectF(0f, h / 2f + gap / 2f, w, h))
            LayoutType.SPLIT_V -> listOf(RectF(0f, 0f, w / 2f - gap / 2f, h), RectF(w / 2f + gap / 2f, 0f, w, h))
            LayoutType.GRID_3 -> listOf(
                RectF(0f, 0f, w, h / 2f - gap / 2f),
                RectF(0f, h / 2f + gap / 2f, w / 2f - gap / 2f, h),
                RectF(w / 2f + gap / 2f, h / 2f + gap / 2f, w, h)
            )
            LayoutType.GRID_4 -> listOf(
                RectF(0f, 0f, w / 2f - gap / 2f, h / 2f - gap / 2f),
                RectF(w / 2f + gap / 2f, 0f, w, h / 2f - gap / 2f),
                RectF(0f, h / 2f + gap / 2f, w / 2f - gap / 2f, h),
                RectF(w / 2f + gap / 2f, h / 2f + gap / 2f, w, h)
            )
            LayoutType.NONE -> emptyList()
        }
        rects.forEachIndexed { idx, r ->
            val key = slide.layoutImages[idx]
            val bmp = key?.let { bitmaps[it] }
            if (bmp != null) drawCoverBitmap(bmp, r)
        }
    } else if (slide.photo.uriKey != null) {
        val bmp = bitmaps[slide.photo.uriKey]
        if (bmp != null) {
            val p = slide.photo
            canvas.save()
            canvas.translate(outW / 2f + p.xDp * ratioX, outH / 2f + p.yDp * ratioY)
            canvas.scale(p.scale * (if (p.flipX) -1f else 1f), p.scale)
            val baseScale = if (p.fitMode == PhotoFitMode.COVER) {
                max(outW.toFloat() / bmp.width, outH.toFloat() / bmp.height)
            } else {
                min(outW.toFloat() / bmp.width, outH.toFloat() / bmp.height)
            }
            val dw = bmp.width * baseScale
            val dh = bmp.height * baseScale
            canvas.drawBitmap(bmp, null, RectF(-dw / 2f, -dh / 2f, dw / 2f, dh / 2f), filterPaint)
            canvas.restore()
        }
    }

    // 3. Optical Vignette
    if (slide.filters.vignette > 0f) {
        val alphaInt = ((slide.filters.vignette / 100f) * 255).roundToInt().coerceIn(0, 220)
        val vigPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            shader = AndroidRadialGradient(
                outW / 2f, outH / 2f, outH * 0.65f,
                intArrayOf(AndroidColor.TRANSPARENT, AndroidColor.argb(alphaInt, 0, 0, 0)),
                floatArrayOf(0.42f, 1.0f),
                AndroidShader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), vigPaint)
    }

    // 4. Isolated Offscreen Markup Layer (Eraser never punches through photo!)
    if (slide.strokes.isNotEmpty()) {
        val strokeBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val sCanvas = AndroidCanvas(strokeBmp)
        slide.strokes.forEach { stroke ->
            drawStrokeToAndroidCanvas(
                canvas = sCanvas,
                stroke = stroke,
                w = outW.toFloat(),
                h = outH.toFloat(),
                scaleRatio = ratio
            )
        }
        canvas.drawBitmap(strokeBmp, 0f, 0f, null)
        strokeBmp.recycle()
    }

    // 5. Stage Objects (Text, Emojis, Photo Stickers, All 10 Widgets)
    slide.items.forEach { item ->
        if (skipAnimatedText && item is TextStoryItem && item.anim != TextAnimStyle.NONE) return@forEach
        if (skipWidgets && item is WidgetStoryItem) return@forEach

        canvas.save()
        canvas.translate(outW / 2f + item.xDp * ratioX, outH / 2f + item.yDp * ratioY)
        canvas.scale(item.scale, item.scale)
        canvas.rotate(item.rotation)

        when (item) {
            is EmojiStoryItem -> {
                val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    textSize = 54f * ratio
                    textAlign = AndroidPaint.Align.CENTER
                }
                canvas.drawText(item.emoji, 0f, p.textSize / 3f, p)
            }

            is PhotoStickerItem -> {
                val pBmp = bitmaps[item.uriKey]
                if (pBmp != null) {
                    val plainPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
                    fun drawUnfilteredCover(bmp: Bitmap, dest: RectF) {
                        val scale = max(dest.width() / bmp.width, dest.height() / bmp.height)
                        val sw = dest.width() / scale
                        val sh = dest.height() / scale
                        val sx = (bmp.width - sw) / 2f
                        val sy = (bmp.height - sh) / 2f
                        val src = android.graphics.Rect(sx.roundToInt(), sy.roundToInt(), (sx + sw).roundToInt(), (sy + sh).roundToInt())
                        canvas.drawBitmap(bmp, src, dest, plainPaint)
                    }

                    val s = 144f * ratio
                    val half = s / 2f
                    canvas.save()
                    if (item.shape == PhotoStickerShape.POLAROID) {
                        val bgP = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.WHITE
                            setShadowLayer(16f * ratio, 0f, 6f * ratio, AndroidColor.argb(72, 0, 0, 0))
                        }
                        canvas.drawRoundRect(
                            RectF(-half - 8f * ratio, -half - 8f * ratio, half + 8f * ratio, half + 28f * ratio),
                            6f * ratio, 6f * ratio, bgP
                        )
                        drawUnfilteredCover(pBmp, RectF(-half, -half, half, half))
                    } else {
                        val clipPath = when (item.shape) {
                            PhotoStickerShape.CIRCLE -> AndroidPath().apply { addCircle(0f, 0f, half, AndroidPath.Direction.CW) }
                            PhotoStickerShape.HEART -> buildHeartAndroidPath(s)
                            PhotoStickerShape.STAR -> buildStarAndroidPath(s)
                            else -> AndroidPath().apply {
                                addRoundRect(RectF(-half, -half, half, half), 18f * ratio, 18f * ratio, AndroidPath.Direction.CW)
                            }
                        }
                        val shadowP = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.argb(65, 0, 0, 0)
                            maskFilter = BlurMaskFilter(12f * ratio, BlurMaskFilter.Blur.NORMAL)
                        }
                        canvas.save()
                        canvas.translate(0f, 4f * ratio)
                        canvas.drawPath(clipPath, shadowP)
                        canvas.restore()

                        canvas.clipPath(clipPath)
                        drawUnfilteredCover(pBmp, RectF(-half, -half, half, half))
                    }
                    canvas.restore()
                }
            }

            is TextStoryItem -> {
                val fontSize = item.sizeSp * ratio
                val typeface = when (item.font) {
                    TextFontStyle.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    TextFontStyle.ITALIC -> Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
                    TextFontStyle.MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    TextFontStyle.BOLD -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    else -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }

                val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    textSize = fontSize
                    this.typeface = typeface
                    textAlign = when (item.align) {
                        TextAlignmentMode.LEFT -> AndroidPaint.Align.LEFT
                        TextAlignmentMode.RIGHT -> AndroidPaint.Align.RIGHT
                        TextAlignmentMode.CENTER -> AndroidPaint.Align.CENTER
                    }
                }

                val displayText = if (item.font == TextFontStyle.BOLD) item.text.uppercase() else item.text
                val maxTextW = 275f * ratio
                val lines = wrapAndroidText(textPaint, displayText, maxTextW)
                val lh = fontSize * 1.2f
                val totalH = lines.size * lh
                val maxW = min(maxTextW, lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f)

                if (item.bgMode == TextBgMode.PILL || item.bgMode == TextBgMode.TRANSLUCENT || item.bgMode == TextBgMode.OUTLINE) {
                    val padX = 16f * ratio
                    val padY = 8f * ratio
                    val rect = RectF(-maxW / 2f - padX, -totalH / 2f - padY, maxW / 2f + padX, totalH / 2f + padY)
                    val bgPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
                    if (item.bgMode == TextBgMode.OUTLINE) {
                        bgPaint.style = AndroidPaint.Style.STROKE
                        bgPaint.strokeWidth = 2f * ratio
                        bgPaint.color = item.colorArgb
                    } else {
                        bgPaint.style = AndroidPaint.Style.FILL
                        bgPaint.color = if (item.bgMode == TextBgMode.PILL) item.colorArgb else AndroidColor.argb(199, 28, 28, 30)
                    }
                    canvas.drawRoundRect(rect, 14f * ratio, 14f * ratio, bgPaint)
                }

                if (item.font == TextFontStyle.NEON) {
                    textPaint.setShadowLayer(18f * ratio, 0f, 0f, item.colorArgb)
                } else if (item.bgMode == TextBgMode.CLEAN) {
                    textPaint.setShadowLayer(10f * ratio, 0f, 2f * ratio, AndroidColor.argb(102, 0, 0, 0))
                }

                textPaint.color = if (item.bgMode == TextBgMode.PILL && isLightArgb(item.colorArgb)) {
                    AndroidColor.BLACK
                } else {
                    item.colorArgb
                }

                val drawX = when (item.align) {
                    TextAlignmentMode.LEFT -> -maxW / 2f
                    TextAlignmentMode.RIGHT -> maxW / 2f
                    TextAlignmentMode.CENTER -> 0f
                }

                lines.forEachIndexed { idx, line ->
                    val cy = (idx - (lines.size - 1) / 2f) * lh
                    drawCenteredAndroidText(canvas, line, drawX, cy, textPaint)
                }
            }

            is WidgetStoryItem -> {
                drawWidgetOnAndroidCanvas(canvas, item, ratio)
            }
        }
        canvas.restore()
    }

    return bitmap
}

private fun buildHeartAndroidPath(s: Float): AndroidPath {
    val mx = { nx: Float -> (nx - 0.5f) * s }
    val my = { ny: Float -> (ny - 0.5f) * s }
    return AndroidPath().apply {
        moveTo(mx(0.5f), my(0.92f))
        cubicTo(mx(0.5f), my(0.92f), mx(0.04f), my(0.62f), mx(0.04f), my(0.31f))
        cubicTo(mx(0.04f), my(0.14f), mx(0.17f), my(0.03f), mx(0.31f), my(0.03f))
        cubicTo(mx(0.40f), my(0.03f), mx(0.46f), my(0.08f), mx(0.50f), my(0.15f))
        cubicTo(mx(0.54f), my(0.08f), mx(0.60f), my(0.03f), mx(0.69f), my(0.03f))
        cubicTo(mx(0.83f), my(0.03f), mx(0.96f), my(0.14f), mx(0.96f), my(0.31f))
        cubicTo(mx(0.96f), my(0.62f), mx(0.50f), my(0.92f), mx(0.50f), my(0.92f))
        close()
    }
}

private fun buildStarAndroidPath(s: Float): AndroidPath {
    val pts = listOf(
        0.5f to 0.0f, 0.618f to 0.353f, 0.988f to 0.353f, 0.685f to 0.571f, 0.801f to 0.924f,
        0.5f to 0.706f, 0.199f to 0.924f, 0.315f to 0.571f, 0.012f to 0.353f, 0.382f to 0.353f
    )
    return AndroidPath().apply {
        pts.forEachIndexed { idx, (nx, ny) ->
            val x = (nx - 0.5f) * s
            val y = (ny - 0.5f) * s
            if (idx == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

private fun wrapAndroidText(paint: AndroidPaint, text: String, maxWidth: Float): List<String> {
    val out = mutableListOf<String>()
    text.split('\n').forEach { para ->
        val words = para.split(' ')
        var line = ""
        words.forEachIndexed { idx, word ->
            val test = if (idx == 0 || line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > maxWidth && line.isNotEmpty()) {
                out.add(line)
                line = word
            } else {
                line = test
            }
        }
        out.add(line)
    }
    return out
}

private fun drawCenteredAndroidText(canvas: AndroidCanvas, text: String, x: Float, centerY: Float, paint: AndroidPaint) {
    val fm = paint.fontMetrics
    val baseline = centerY - (fm.ascent + fm.descent) / 2f
    canvas.drawText(text, x, baseline, paint)
}

private fun drawWidgetOnAndroidCanvas(canvas: AndroidCanvas, item: WidgetStoryItem, r: Float) {
    val fillPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
    val cardShadowPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(54, 0, 0, 0)
        maskFilter = BlurMaskFilter(14f * r, BlurMaskFilter.Blur.NORMAL)
    }
    val strokePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { style = AndroidPaint.Style.STROKE }
    val textPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { textAlign = AndroidPaint.Align.CENTER }

    fun drawCardShadow(w: Float, h: Float, radius: Float) {
        canvas.drawRoundRect(
            RectF(-w / 2f, -h / 2f + 4f * r, w / 2f, h / 2f + 4f * r),
            radius, radius, cardShadowPaint
        )
    }

    when (item.widgetType) {
        WidgetType.POLL -> {
            val w = 236f * r; val h = 92f * r
            drawCardShadow(w, h, 16f * r)
            fillPaint.color = AndroidColor.argb(245, 255, 255, 255)
            canvas.drawRoundRect(RectF(-w / 2f, -h / 2f, w / 2f, h / 2f), 16f * r, 16f * r, fillPaint)

            textPaint.color = AndroidColor.parseColor("#111111")
            textPaint.textSize = 13f * r
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            drawCenteredAndroidText(canvas, item.question, 0f, -22f * r, textPaint)

            val bw = 100f * r; val bh = 34f * r; val by = 2f * r
            fillPaint.color = AndroidColor.parseColor("#F2F2F7")
            canvas.drawRoundRect(RectF(-w / 2f + 14f * r, by, -w / 2f + 14f * r + bw, by + bh), 11f * r, 11f * r, fillPaint)
            canvas.drawRoundRect(RectF(4f * r, by, 4f * r + bw, by + bh), 11f * r, 11f * r, fillPaint)

            textPaint.textSize = 12f * r
            textPaint.color = AndroidColor.parseColor("#007AFF")
            drawCenteredAndroidText(canvas, item.optA, -w / 2f + 14f * r + bw / 2f, by + bh / 2f, textPaint)
            textPaint.color = AndroidColor.parseColor("#1C1C1E")
            drawCenteredAndroidText(canvas, item.optB, 4f * r + bw / 2f, by + bh / 2f, textPaint)
        }

        WidgetType.SLIDER -> {
            val w = 236f * r; val h = 84f * r
            drawCardShadow(w, h, 16f * r)
            fillPaint.color = AndroidColor.argb(245, 255, 255, 255)
            canvas.drawRoundRect(RectF(-w / 2f, -h / 2f, w / 2f, h / 2f), 16f * r, 16f * r, fillPaint)

            textPaint.color = AndroidColor.parseColor("#111111")
            textPaint.textSize = 13f * r
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            drawCenteredAndroidText(canvas, item.prompt, 0f, -17f * r, textPaint)

            val trackW = 196f * r; val trackH = 8f * r; val ty = 14f * r
            fillPaint.color = AndroidColor.parseColor("#E5E5EA")
            canvas.drawRoundRect(RectF(-trackW / 2f, ty - trackH / 2f, trackW / 2f, ty + trackH / 2f), 4f * r, 4f * r, fillPaint)

            val fillW = trackW * (item.sliderValue / 100f).coerceIn(0.05f, 0.95f)
            val gradPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                shader = AndroidLinearGradient(-trackW / 2f, 0f, trackW / 2f, 0f, AndroidColor.parseColor("#FF9500"), AndroidColor.parseColor("#FF2D55"), AndroidShader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(RectF(-trackW / 2f, ty - trackH / 2f, -trackW / 2f + fillW, ty + trackH / 2f), 4f * r, 4f * r, gradPaint)

            textPaint.textSize = 22f * r
            drawCenteredAndroidText(canvas, item.emoji, -trackW / 2f + fillW, ty, textPaint)
        }

        WidgetType.QUIZ -> {
            val w = 236f * r; val h = 152f * r
            drawCardShadow(w, h, 16f * r)
            fillPaint.color = AndroidColor.argb(245, 255, 255, 255)
            canvas.drawRoundRect(RectF(-w / 2f, -h / 2f, w / 2f, h / 2f), 16f * r, 16f * r, fillPaint)

            textPaint.color = AndroidColor.parseColor("#111111")
            textPaint.textSize = 13f * r
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            drawCenteredAndroidText(canvas, item.question, 0f, -h / 2f + 22f * r, textPaint)

            val labels = listOf("A", "B", "C")
            item.quizOpts.forEachIndexed { idx, opt ->
                val ry = -h / 2f + 40f * r + idx * 35f * r
                val rw = w - 28f * r; val rh = 29f * r
                val isCorr = idx == item.correctIndex

                fillPaint.color = AndroidColor.parseColor(if (isCorr) "#ECFDF3" else "#F9FAFB")
                canvas.drawRoundRect(RectF(-rw / 2f, ry, rw / 2f, ry + rh), 10f * r, 10f * r, fillPaint)
                strokePaint.color = AndroidColor.parseColor(if (isCorr) "#34C759" else "#E5E5EA")
                strokePaint.strokeWidth = 1.2f * r
                canvas.drawRoundRect(RectF(-rw / 2f, ry, rw / 2f, ry + rh), 10f * r, 10f * r, strokePaint)

                fillPaint.color = AndroidColor.parseColor(if (isCorr) "#34C759" else "#E5E5EA")
                canvas.drawCircle(-rw / 2f + 16f * r, ry + rh / 2f, 9f * r, fillPaint)

                textPaint.textAlign = AndroidPaint.Align.CENTER
                textPaint.textSize = 10f * r
                textPaint.color = if (isCorr) AndroidColor.WHITE else AndroidColor.parseColor("#3A3A3C")
                drawCenteredAndroidText(canvas, labels.getOrElse(idx) { "?" }, -rw / 2f + 16f * r, ry + rh / 2f, textPaint)

                textPaint.textAlign = AndroidPaint.Align.LEFT
                textPaint.textSize = 12f * r
                textPaint.color = AndroidColor.parseColor("#111111")
                drawCenteredAndroidText(canvas, opt, -rw / 2f + 32f * r, ry + rh / 2f, textPaint)
            }
        }

        WidgetType.COUNTDOWN -> {
            val w = 230f * r; val h = 98f * r
            drawCardShadow(w, h, 16f * r)
            fillPaint.color = AndroidColor.argb(235, 28, 28, 30)
            canvas.drawRoundRect(RectF(-w / 2f, -h / 2f, w / 2f, h / 2f), 16f * r, 16f * r, fillPaint)

            textPaint.color = AndroidColor.argb(173, 255, 255, 255)
            textPaint.textSize = 10f * r
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            drawCenteredAndroidText(canvas, item.title, 0f, -28f * r, textPaint)

            val units = listOf(item.days to "DAYS", item.hours to "HRS", item.mins to "MIN")
            val bw = 62f * r; val bh = 46f * r; val gap = 8f * r
            units.forEachIndexed { i, (value, lbl) ->
                val bx = -w / 2f + 14f * r + i * (bw + gap)
                val by = -10f * r
                fillPaint.color = AndroidColor.argb(31, 255, 255, 255)
                canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), 11f * r, 11f * r, fillPaint)

                textPaint.color = AndroidColor.WHITE
                textPaint.textSize = 18f * r
                textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                drawCenteredAndroidText(canvas, value, bx + bw / 2f, by + 18f * r, textPaint)

                textPaint.color = AndroidColor.argb(128, 255, 255, 255)
                textPaint.textSize = 9f * r
                textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                drawCenteredAndroidText(canvas, lbl, bx + bw / 2f, by + 35f * r, textPaint)
            }
        }

        WidgetType.QUESTION -> {
            val w = 232f * r; val h = 88f * r
            drawCardShadow(w, h, 16f * r)
            fillPaint.color = AndroidColor.argb(245, 255, 255, 255)
            canvas.drawRoundRect(RectF(-w / 2f, -h / 2f, w / 2f, h / 2f), 16f * r, 16f * r, fillPaint)

            textPaint.color = AndroidColor.parseColor("#111111")
            textPaint.textSize = 13f * r
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            drawCenteredAndroidText(canvas, item.prompt, 0f, -18f * r, textPaint)

            fillPaint.color = AndroidColor.parseColor("#F2F2F7")
            canvas.drawRoundRect(RectF(-w / 2f + 14f * r, 2f * r, w / 2f - 14f * r, 34f * r), 10f * r, 10f * r, fillPaint)
            textPaint.color = AndroidColor.parseColor("#9CA3AF")
            textPaint.textSize = 12f * r
            textPaint.typeface = Typeface.SANS_SERIF
            drawCenteredAndroidText(canvas, "Type something...", 0f, 18f * r, textPaint)
        }

        WidgetType.MENTION, WidgetType.LINK, WidgetType.LOCATION -> {
            val label = when (item.widgetType) {
                WidgetType.MENTION -> item.handle
                WidgetType.LINK -> "🔗 ${item.linkLabel}"
                else -> "📍 ${item.place}"
            }
            textPaint.textSize = 13f * r
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            val pw = textPaint.measureText(label) + 30f * r
            val ph = 34f * r
            drawCardShadow(pw, ph, ph / 2f)
            fillPaint.color = AndroidColor.argb(245, 255, 255, 255)
            canvas.drawRoundRect(RectF(-pw / 2f, -ph / 2f, pw / 2f, ph / 2f), ph / 2f, ph / 2f, fillPaint)
            textPaint.color = if (item.widgetType == WidgetType.MENTION) AndroidColor.parseColor("#007AFF") else AndroidColor.parseColor("#111111")
            drawCenteredAndroidText(canvas, label, 0f, 0f, textPaint)
        }

        WidgetType.TIME -> {
            textPaint.textSize = 18f * r
            textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            val pw = textPaint.measureText(item.timeText) + 30f * r
            val ph = 40f * r
            fillPaint.color = AndroidColor.argb(148, 0, 0, 0)
            canvas.drawRoundRect(RectF(-pw / 2f, -ph / 2f, pw / 2f, ph / 2f), 14f * r, 14f * r, fillPaint)
            strokePaint.color = AndroidColor.argb(56, 255, 255, 255)
            strokePaint.strokeWidth = 1f * r
            canvas.drawRoundRect(RectF(-pw / 2f, -ph / 2f, pw / 2f, ph / 2f), 14f * r, 14f * r, strokePaint)
            textPaint.color = AndroidColor.WHITE
            drawCenteredAndroidText(canvas, item.timeText, 0f, 0f, textPaint)
        }

        WidgetType.MUSIC -> {
            textPaint.textSize = 12f * r
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            val tw = max(textPaint.measureText(item.musicTitle), textPaint.measureText(item.musicArtist))
            val pw = tw + 68f * r
            val ph = 48f * r
            drawCardShadow(pw, ph, 16f * r)
            fillPaint.color = AndroidColor.argb(235, 23, 23, 26)
            canvas.drawRoundRect(RectF(-pw / 2f, -ph / 2f, pw / 2f, ph / 2f), 16f * r, 16f * r, fillPaint)

            val artS = 32f * r
            val artLeft = -pw / 2f + 10f * r
            val artPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                shader = AndroidLinearGradient(artLeft, -artS / 2f, artLeft + artS, artS / 2f, AndroidColor.parseColor("#007AFF"), AndroidColor.parseColor("#5856D6"), AndroidShader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(RectF(artLeft, -artS / 2f, artLeft + artS, artS / 2f), 8f * r, 8f * r, artPaint)

            textPaint.color = AndroidColor.WHITE
            textPaint.textSize = 14f * r
            drawCenteredAndroidText(canvas, "♫", artLeft + artS / 2f, 0f, textPaint)

            textPaint.textAlign = AndroidPaint.Align.LEFT
            textPaint.textSize = 12f * r
            textPaint.color = AndroidColor.WHITE
            drawCenteredAndroidText(canvas, item.musicTitle, -pw / 2f + 52f * r, -7f * r, textPaint)

            textPaint.textSize = 10f * r
            textPaint.typeface = Typeface.SANS_SERIF
            textPaint.color = AndroidColor.argb(158, 255, 255, 255)
            drawCenteredAndroidText(canvas, item.musicArtist, -pw / 2f + 52f * r, 8f * r, textPaint)
        }
    }
}

/* ==============================================================================
   14. ANDROID GALLERY EXPORT (MEDIASTORE) & NATIVE SYSTEM SHARE SHEET
   ============================================================================== */

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filename: String): Boolean {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/StoryStudio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        } ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    } catch (_: Throwable) {
        false
    } finally {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}

private fun shareBitmapViaIntent(context: Context, bitmap: Bitmap): Boolean {
    return try {
        val dir = File(context.cacheDir, "shared_stories").apply { mkdirs() }
        val file = File(dir, "story-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Story").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    } catch (_: Throwable) {
        false
    } finally {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}