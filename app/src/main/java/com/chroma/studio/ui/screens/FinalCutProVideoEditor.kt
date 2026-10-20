@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.chroma.studio.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.util.LruCache
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import android.view.Gravity
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.graphics.PathParser
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.chroma.studio.ui.components.AppleSlider
import com.chroma.studio.ui.components.AppleVideosIcon
import com.chroma.studio.ui.components.InlineColorPicker
import com.chroma.studio.ui.components.LiquidGlassKnob
import com.chroma.studio.ui.components.PrimaryGradientButton
import com.chroma.studio.ui.components.SmallSwitch
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.components.glossyBorder
import com.chroma.studio.ui.theme.LocalOscellionColors
import androidx.compose.foundation.BorderStroke
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.data.DraftPostRepository
import com.chroma.studio.model.CreatorPostDraft
import androidx.compose.ui.graphics.toArgb
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.*

// ============================================================================
// DATA MODELS
// ============================================================================

enum class FcpWorkspaceTab(val label: String, val icon: ImageVector) {
    TIMELINE("Timeline", Lucide.Film),
    CANVAS("Canvas", Lucide.LayoutGrid),
    COLOR("Color", Lucide.Palette),
    AUDIO("Audio", Lucide.Volume2),
    EFFECTS("Effects", Lucide.Zap),
    TITLES("Titles", Lucide.Type)
}

enum class FcpLutPreset(val label: String, val colors: List<Color>) {
    NONE("Rec.709", listOf(Color(0xFF64748B), Color(0xFF94A3B8))),
    TEAL_ORANGE("Teal/Orange", listOf(Color(0xFF0D9488), Color(0xFFF97316))),
    CYBER("Vivid Cinema", listOf(Color(0xFF3B82F6), Color(0xFFEC4899))),
    NOIR("Film Noir", listOf(Color(0xFF1E293B), Color(0xFFF8FAFC))),
    WARM_GOLD("Golden Hour", listOf(Color(0xFFD97706), Color(0xFFFBBF24))),
    COOL_NORDIC("Nordic Cool", listOf(Color(0xFF0284C7), Color(0xFF38BDF8))),
    VINTAGE_FADE("Vintage Film", listOf(Color(0xFF78350F), Color(0xFFFDE68A))),
    EMERALD("Emerald Mood", listOf(Color(0xFF065F46), Color(0xFF10B981)))
}

enum class FcpAspectRatio(val ratio: Float, val label: String) {
    VERTICAL_9_16(9f / 16f, "9:16 Vertical"),
    SQUARE_1_1(1f, "1:1 Square"),
    WIDESCREEN_16_9(16f / 9f, "16:9 Widescreen"),
    CINEMATIC_21_9(21f / 9f, "21:9 UltraWide")
}

// ── Canvas Background & Fill Models (Blur / Solid Color / Custom Image) ──

enum class FcpCanvasBgType(val label: String) {
    BLUR("Blur"),
    COLOR("Color"),
    IMAGE("Image")
}

data class FcpStudioBackdrop(
    val id: String,
    val name: String,
    val brush: Brush,
    val previewColor: Color
)

val AllFcpStudioBackdrops = listOf(
    FcpStudioBackdrop(
        id = "obsidian",
        name = "Obsidian",
        brush = Brush.radialGradient(listOf(Color(0xFF2E2E38), Color(0xFF14141A), Color(0xFF000000))),
        previewColor = Color(0xFF1A1A22)
    ),
    FcpStudioBackdrop(
        id = "cyber",
        name = "Cyber",
        brush = Brush.verticalGradient(listOf(Color(0xFF3B0764), Color(0xFF1E1B4B), Color(0xFF09090B))),
        previewColor = Color(0xFF3B0764)
    ),
    FcpStudioBackdrop(
        id = "sunset",
        name = "Sunset",
        brush = Brush.verticalGradient(listOf(Color(0xFF7C2D12), Color(0xFF4C0519), Color(0xFF0A0A0A))),
        previewColor = Color(0xFF831843)
    ),
    FcpStudioBackdrop(
        id = "ocean",
        name = "Abyss",
        brush = Brush.verticalGradient(listOf(Color(0xFF083344), Color(0xFF022C22), Color(0xFF020617))),
        previewColor = Color(0xFF083344)
    ),
    FcpStudioBackdrop(
        id = "velvet",
        name = "Velvet",
        brush = Brush.verticalGradient(listOf(Color(0xFF4A044E), Color(0xFF18181B), Color(0xFF000000))),
        previewColor = Color(0xFF4A044E)
    ),
    FcpStudioBackdrop(
        id = "slate",
        name = "Studio Slate",
        brush = Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A))),
        previewColor = Color(0xFF334155)
    )
)

data class FcpCanvasBackground(
    val type: FcpCanvasBgType = FcpCanvasBgType.BLUR,
    val blurRadius: Float = 35f,
    val solidColor: Color = Color.Black,
    val customImageUri: Uri? = null,
    val studioBackdropId: String = "obsidian",
    val backdropDim: Float = 0.25f
)

// ── Typography & Ready-Made Title Models ──────────────────────────────────

data class FcpFontItem(
    val name: String,
    val fileName: String,
    val category: String,
    val sampleText: String = "ChromaStudio 2026"
)

val AllFcpFonts = listOf(
    FcpFontItem("The Signature", "Thesignature.ttf", "Luxury Cursive", "Signature Elegance"),
    FcpFontItem("Mayonice", "Mayonice.ttf", "Playful Script", "Golden Moments"),
    FcpFontItem("Rainy Days", "rainydays.ttf", "Handwritten", "Memories & Stories"),
    FcpFontItem("Outfit Bold", "Outfit-Bold.ttf", "Modern Geometric", "HEADLINE DESIGN"),
    FcpFontItem("Wotham Heavy", "Wotham.ttf", "Ultra Bold Block", "BOLD IMPACT"),
    FcpFontItem("High Summit", "High Summit.ttf", "Tall Display", "ELEVATE HIGHER"),
    FcpFontItem("South Town", "SouthTown.otf", "Retro Vintage", "RETRO VIBES"),
    FcpFontItem("Comfortaa", "Comfortaa-Bold.ttf", "Rounded Sans", "Smooth Curves"),
    FcpFontItem("Quicksand", "Quicksand-Bold.ttf", "Sleek Headline", "Minimal & Sleek"),
    FcpFontItem("Malvides", "Malvides.otf", "Fashion Serif", "High Editorial"),
    FcpFontItem("Istok Clean", "Istok-Regular.ttf", "Editorial Clean", "Clean & Legible"),
    FcpFontItem("Nunito Sans", "Nunito-Bold.ttf", "Friendly Sans", "Modern Rounded")
)

private val fcpFontFamilyCache = mutableMapOf<String, FontFamily>()

fun getFcpFontFamily(context: Context, assetFileName: String): FontFamily {
    return fcpFontFamilyCache.getOrPut(assetFileName) {
        try {
            val tf = Typeface.createFromAsset(context.assets, "fonts/$assetFileName")
            FontFamily(tf)
        } catch (e: Exception) {
            try {
                val altName = assetFileName.replace(" ", "")
                val tfAlt = Typeface.createFromAsset(context.assets, "fonts/$altName")
                FontFamily(tfAlt)
            } catch (e2: Exception) {
                FontFamily.Default
            }
        }
    }
}

enum class FcpTitlePresetType(
    val title: String,
    val subtitle: String,
    val category: String
) {
    SCRIPT_HEADLINE("Script & Headline", "Warm signature script over giant bold title", "Editorial"),
    GOLD_3D("3D Golden Bevel", "Extruded 3D metallic gold with cursive crossover", "3D Titles"),
    MODERN_FOLD("Modern Folded", "Bold geometric all-caps with folded shadow", "Cinematic"),
    DESIGN_CONTRAST("Design Contrast", "Bold headline with delicate cursive script", "Editorial"),
    BOXED_FRAME("Simplicity Frame", "Clean rectangular border enclosing block text", "Minimal"),
    ABSTRACT_BADGE("Abstract Badge", "High-contrast solid badge with black cutout", "Modern"),
    CYBER_NEON("Cyber Neon", "Vibrant glow with cyan & magenta glass frame", "VFX"),
    MINIMAL_CLEAN("Minimal Cinematic", "Understated elegant lower-third title", "Clean")
}

data class FcpTextLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Title Layer",
    val presetType: FcpTitlePresetType = FcpTitlePresetType.SCRIPT_HEADLINE,
    val primaryText: String = "SCRIPT FONTS",
    val scriptText: String = "Script &",
    val subtitleText: String = "EVERYTHING YOU NEED TO KNOW",
    val badgeText: String = "AESTHETIC",
    val primaryFontAsset: String = "Outfit-Bold.ttf",
    val scriptFontAsset: String = "Thesignature.ttf",
    val fontSizeSp: Float = 28f,
    val textColor: Color = Color.White,
    val secondaryTextColor: Color = Color(0xFF60A5FA),
    val isGradientText: Boolean = false,
    val accentColor: Color = Color(0xFFFFB800),
    val isVisible: Boolean = true,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val opacity: Float = 1.0f,
    val rotationDeg: Float = 0f,
    val letterSpacingSp: Float = 1.0f,
    val hasDropShadow: Boolean = true,
    val hasBackgroundPill: Boolean = false,
    val blendMode: String = "Normal",
    val isExpanded: Boolean = true
)

fun createFcpTextLayer(preset: FcpTitlePresetType, layerIndex: Int): FcpTextLayer {
    val id = UUID.randomUUID().toString()
    val name = "Layer ${layerIndex + 1}"
    val offsetY = (layerIndex * 25f).coerceAtMost(100f)
    return when (preset) {
        FcpTitlePresetType.SCRIPT_HEADLINE -> FcpTextLayer(
            id = id,
            name = name,
            presetType = preset,
            primaryText = "SCRIPT FONTS",
            scriptText = "Script &",
            subtitleText = "EVERYTHING YOU NEED TO KNOW",
            badgeText = "AESTHETIC",
            primaryFontAsset = "Outfit-Bold.ttf",
            scriptFontAsset = "Thesignature.ttf",
            fontSizeSp = 28f,
            accentColor = Color(0xFFFFB800),
            isVisible = true,
            offsetY = offsetY
        )
        FcpTitlePresetType.GOLD_3D -> FcpTextLayer(
            id = id,
            name = name,
            presetType = preset,
            primaryText = "3D GOLD",
            scriptText = "Signature",
            subtitleText = "PREMIUM CINEMATIC EDIT",
            badgeText = "AESTHETIC",
            primaryFontAsset = "Wotham.ttf",
            scriptFontAsset = "Thesignature.ttf",
            fontSizeSp = 26f,
            accentColor = Color(0xFFFFD700),
            isVisible = true,
            offsetY = offsetY
        )
        FcpTitlePresetType.MODERN_FOLD -> FcpTextLayer(
            id = id,
            name = name,
            presetType = preset,
            primaryText = "MODERN FOLD",
            scriptText = "",
            subtitleText = "CREATE WITH PASSION",
            badgeText = "",
            primaryFontAsset = "Wotham.ttf",
            fontSizeSp = 24f,
            accentColor = Color(0xFF00E5FF),
            isVisible = true,
            offsetY = offsetY
        )
        FcpTitlePresetType.DESIGN_CONTRAST -> FcpTextLayer(
            id = id,
            name = name,
            presetType = preset,
            primaryText = "DESIGN CONTRAST",
            scriptText = "Aesthetic",
            subtitleText = "INSPIRING CREATIVE MINDS",
            badgeText = "EDITORIAL",
            primaryFontAsset = "Comfortaa-Bold.ttf",
            scriptFontAsset = "Thesignature.ttf",
            fontSizeSp = 26f,
            accentColor = Color(0xFF10B981),
            isVisible = true,
            offsetY = offsetY
        )
        FcpTitlePresetType.BOXED_FRAME -> FcpTextLayer(
            id = id,
            name = name,
            presetType = preset,
            primaryText = "FRAMEWORK",
            scriptText = "",
            subtitleText = "STRUCTURE & CLARITY",
            badgeText = "V1.0",
            primaryFontAsset = "Outfit-Bold.ttf",
            fontSizeSp = 24f,
            accentColor = Color.White,
            isVisible = true,
            offsetY = offsetY
        )
        FcpTitlePresetType.ABSTRACT_BADGE -> FcpTextLayer(
            id = id,
            name = name,
            presetType = preset,
            primaryText = "ABSTRACT BADGE",
            scriptText = "",
            subtitleText = "MODERN GEOMETRIC LOOK",
            badgeText = "BADGE",
            primaryFontAsset = "Wotham.ttf",
            fontSizeSp = 24f,
            accentColor = Color(0xFFF97316),
            isVisible = true,
            offsetY = offsetY
        )
        FcpTitlePresetType.CYBER_NEON -> FcpTextLayer(
            id = id,
            name = name,
            presetType = preset,
            primaryText = "CYBER NEON",
            scriptText = "Futuristic",
            subtitleText = "TOKYO NIGHT DRIFT",
            badgeText = "LIVE",
            primaryFontAsset = "Outfit-Bold.ttf",
            scriptFontAsset = "Thesignature.ttf",
            fontSizeSp = 26f,
            accentColor = Color(0xFFEC4899),
            isVisible = true,
            offsetY = offsetY
        )
        FcpTitlePresetType.MINIMAL_CLEAN -> FcpTextLayer(
            id = id,
            name = name,
            presetType = preset,
            primaryText = "MINIMALIST",
            scriptText = "",
            subtitleText = "LESS IS MORE",
            badgeText = "",
            primaryFontAsset = "Comfortaa-Bold.ttf",
            fontSizeSp = 22f,
            accentColor = Color.White,
            isVisible = true,
            offsetY = offsetY
        )
    }
}

enum class FcpTransitionType(val label: String, val icon: ImageVector, val description: String) {
    NONE("Cut", Lucide.X, "Direct instant cut with no transition effect"),
    DIP_TO_BLACK("Dip Black", Lucide.Moon, "Fades down to pure black at cut, then reveals incoming video"),
    DIP_TO_WHITE("Dip White", Lucide.Sun, "Dynamic cinematic white flash transition between scenes"),
    CROSSFADE("Crossfade", Lucide.Layers, "Soft atmospheric dissolve bridging outgoing and incoming video"),
    WIPE_RIGHT("Wipe Right", Lucide.ArrowRight, "Horizontal wipe curtain revealing incoming video left-to-right"),
    WIPE_LEFT("Wipe Left", Lucide.ArrowLeft, "Horizontal wipe curtain revealing incoming video right-to-left"),
    SLIDE_LEFT("Slide", Lucide.Move, "Dynamic push slide transition sliding into next video"),
    ZOOM_IN("Zoom Punch", Lucide.Maximize2, "High-energy camera scale punch into the cut"),
    GLITCH("Glitch", Lucide.Zap, "Digital RGB chromatic aberration glitch cut")
}

data class FcpClip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val durationSec: Float = 15f,
    val trimStartSec: Float = 0f,
    val trimEndSec: Float = 15f,
    val speed: Float = 1f,
    val exposure: Float = 0f,      // -50..50 (default 0)
    val contrast: Float = 100f,    // 50..150 (default 100)
    val saturation: Float = 100f,  // 0..200 (default 100)
    val temperature: Float = 0f,   // -50..50 (default 0)
    val tint: Float = 0f,          // -50..50 (default 0)
    val lut: FcpLutPreset = FcpLutPreset.NONE,
    val roleColor: Color = Color(0xFF0A84FF),
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val isDurationLoaded: Boolean = false,
    val transitionToNext: FcpTransitionType = FcpTransitionType.NONE,
    val transitionDurationSec: Float = 0.5f
) {
    val sourceTrimmedDurationSec: Float get() = (trimEndSec - trimStartSec).coerceAtLeast(0.25f)
    val trimmedDurationSec: Float get() = (sourceTrimmedDurationSec / speed.coerceAtLeast(0.1f)).coerceAtLeast(0.2f)
}

data class FcpTimelineClipSpan(
    val clip: FcpClip,
    val timelineStartSec: Float,
    val timelineEndSec: Float
) {
    val durationSec: Float get() = (timelineEndSec - timelineStartSec).coerceAtLeast(0.1f)
}

private fun buildTimelineSpans(clips: List<FcpClip>): List<FcpTimelineClipSpan> {
    var accum = 0f
    return clips.map { clip ->
        val start = accum
        val dur = clip.trimmedDurationSec
        accum += dur
        FcpTimelineClipSpan(clip, start, accum)
    }
}

data class FcpCustomAudio(
    val uri: Uri,
    val name: String,
    val durationSec: Float = 0f,
    val volume: Float = 1.0f,
    val isExtractedFromVideo: Boolean = false,
    val muteOriginalVideo: Boolean = true,
    val loopAudio: Boolean = true
)

private fun queryMediaFileName(context: Context, uri: Uri): String {
    var name: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        name = cursor.getString(nameIdx)
                    }
                }
            }
        } catch (_: Throwable) {}
    }
    if (name.isNullOrBlank()) {
        name = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Audio Track"
    }
    return name
}

private fun extractVideoDurationSec(context: Context, uri: Uri): Float? {
    val retriever = MediaMetadataRetriever()
    return try {
        try {
            retriever.setDataSource(context, uri)
        } catch (_: Throwable) {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
            }
        }
        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durMs = durStr?.toLongOrNull()
        if (durMs != null && durMs > 0L) {
            (durMs / 1000f).coerceAtLeast(0.25f)
        } else null
    } catch (_: Throwable) {
        null
    } finally {
        try { retriever.release() } catch (_: Throwable) {}
    }
}

/**
 * Builds a dynamic hardware ColorMatrix combining exposure, contrast, saturation,
 * temperature (cool/warm), tint (green/magenta), and LUT look in real time.
 */
private fun buildFcpColorMatrix(
    exposure: Float,
    contrast: Float,
    saturation: Float,
    temperature: Float,
    tint: Float,
    lut: FcpLutPreset = FcpLutPreset.NONE
): android.graphics.ColorMatrix {
    val result = android.graphics.ColorMatrix()

    // 1. Saturation (0..200%, 100% = 1.0)
    val satFactor = (saturation / 100f).coerceIn(0f, 3f)
    if (abs(satFactor - 1f) > 0.01f) {
        result.postConcat(android.graphics.ColorMatrix().apply { setSaturation(satFactor) })
    }

    // 2. Exposure & Contrast
    val expFactor = Math.pow(2.0, (exposure / 60.0)).toFloat()
    val contFactor = (contrast / 100f).coerceIn(0.3f, 2.2f)
    val totalScale = expFactor * contFactor
    val offset = (1f - contFactor) * 128f + (exposure * 1.2f)

    if (abs(totalScale - 1f) > 0.01f || abs(offset) > 0.5f) {
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
    }

    // 3. Temperature & Tint (White Balance)
    val tempNorm = (temperature / 50f).coerceIn(-1f, 1f)
    val tintNorm = (tint / 50f).coerceIn(-1f, 1f)

    if (abs(tempNorm) > 0.01f || abs(tintNorm) > 0.01f) {
        val rOffset = tempNorm * 28f + tintNorm * 18f
        val gOffset = -tintNorm * 28f
        val bOffset = -tempNorm * 28f + tintNorm * 18f

        result.postConcat(
            android.graphics.ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, rOffset,
                    0f, 1f, 0f, 0f, gOffset,
                    0f, 0f, 1f, 0f, bOffset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }

    // 4. LUT Matrix Transformations
    when (lut) {
        FcpLutPreset.TEAL_ORANGE -> {
            result.postConcat(
                android.graphics.ColorMatrix(
                    floatArrayOf(
                        1.18f, 0f, 0f, 0f, 16f,
                        0f, 1.02f, 0f, 0f, -2f,
                        0f, 0f, 0.88f, 0f, -12f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        FcpLutPreset.CYBER -> {
            result.postConcat(
                android.graphics.ColorMatrix(
                    floatArrayOf(
                        1.22f, 0f, 0f, 0f, 12f,
                        0f, 0.92f, 0f, 0f, -8f,
                        0f, 0f, 1.28f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        FcpLutPreset.NOIR -> {
            val bw = android.graphics.ColorMatrix().apply { setSaturation(0f) }
            val contrastBw = android.graphics.ColorMatrix(
                floatArrayOf(
                    1.35f, 0f, 0f, 0f, -30f,
                    0f, 1.35f, 0f, 0f, -30f,
                    0f, 0f, 1.35f, 0f, -30f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            bw.postConcat(contrastBw)
            result.postConcat(bw)
        }
        FcpLutPreset.WARM_GOLD -> {
            result.postConcat(
                android.graphics.ColorMatrix(
                    floatArrayOf(
                        1.15f, 0f, 0f, 0f, 22f,
                        0f, 1.05f, 0f, 0f, 10f,
                        0f, 0f, 0.82f, 0f, -16f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        FcpLutPreset.COOL_NORDIC -> {
            result.postConcat(
                android.graphics.ColorMatrix(
                    floatArrayOf(
                        0.90f, 0f, 0f, 0f, -8f,
                        0f, 1.02f, 0f, 0f, 4f,
                        0f, 0f, 1.22f, 0f, 24f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        FcpLutPreset.VINTAGE_FADE -> {
            val sat = android.graphics.ColorMatrix().apply { setSaturation(0.78f) }
            val vintage = android.graphics.ColorMatrix(
                floatArrayOf(
                    1.08f, 0f, 0f, 0f, 18f,
                    0f, 0.98f, 0f, 0f, 8f,
                    0f, 0f, 0.85f, 0f, 2f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            sat.postConcat(vintage)
            result.postConcat(sat)
        }
        FcpLutPreset.EMERALD -> {
            result.postConcat(
                android.graphics.ColorMatrix(
                    floatArrayOf(
                        0.88f, 0f, 0f, 0f, -10f,
                        0f, 1.18f, 0f, 0f, 14f,
                        0f, 0f, 0.95f, 0f, -4f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        FcpLutPreset.NONE -> {}
    }

    return result
}

private enum class VideoDrawerLevel(val heightDp: Dp) {
    COLLAPSED(104.dp),
    MID(350.dp),
    FULL(580.dp)
}

// ============================================================================
// MAIN SCREEN — CHROMA STUDIO WORKSPACE FOR VIDEO
// ============================================================================

@Composable
fun FinalCutProVideoEditorScreen(
    initialUri: Uri? = null,
    initialUris: List<Uri> = emptyList(),
    onDone: (startMs: Long, endMs: Long, clips: List<Uri>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val colors = LocalOscellionColors.current

    var pixelsPerSec by remember { mutableFloatStateOf(45f) } // dp per second in storyline (zoomable 15f..120f)

    // ── Clip & History State ──────────────────────────────────────────────
    var clips by remember { mutableStateOf<List<FcpClip>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var undoStack by remember { mutableStateOf<List<List<FcpClip>>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<List<FcpClip>>>(emptyList()) }

    // ── Playback State ────────────────────────────────────────────────────
    var isPlaying by remember { mutableStateOf(false) }
    var currentTimeSec by remember { mutableFloatStateOf(0f) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var isAutoReplay by remember { mutableStateOf(true) }

    // ── UI / Workspace State ──────────────────────────────────────────────
    var activeTab by remember { mutableStateOf(FcpWorkspaceTab.TIMELINE) }
    var aspectRatio by remember { mutableStateOf(FcpAspectRatio.VERTICAL_9_16) }
    var drawerLevel by remember { mutableStateOf(VideoDrawerLevel.MID) }

    // ── Canvas Background & Blank Gap Fill State ──────────────────────────
    var canvasBackground by remember { mutableStateOf(FcpCanvasBackground()) }
    var blurBackgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // ── Export & Transitions State ────────────────────────────────────────
    var editingTransitionClipId by remember { mutableStateOf<String?>(null) }
    var showExportSheet by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }

    // ── Dialog & Quick Overlay States ─────────────────────────────────────
    var showExitConfirmationDialog by remember { mutableStateOf(false) }
    var flashOverlayIcon by remember { mutableStateOf<FlatPlaybackIconType?>(null) }
    var flashOverlayVisible by remember { mutableStateOf(false) }

    BackHandler(enabled = clips.isNotEmpty()) {
        showExitConfirmationDialog = true
    }

    fun triggerOverlayFlash(playing: Boolean) {
        flashOverlayIcon = if (playing) FlatPlaybackIconType.PLAY else FlatPlaybackIconType.PAUSE
        flashOverlayVisible = true
    }

    LaunchedEffect(flashOverlayVisible) {
        if (flashOverlayVisible) {
            delay(320)
            flashOverlayVisible = false
        }
    }

    val bgImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            canvasBackground = canvasBackground.copy(
                type = FcpCanvasBgType.IMAGE,
                customImageUri = uri
            )
            ToastManager.showToast("Background image applied")
        }
    }

    // ── Custom Soundtrack & Video Audio Extraction State ──────────────────
    var customAudio by remember { mutableStateOf<FcpCustomAudio?>(null) }

    // ── Custom Soundtrack & Video Audio Extraction Pickers ───────────────
    val audioFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = queryMediaFileName(context, uri)
            val dur = extractVideoDurationSec(context, uri) ?: 0f
            customAudio = FcpCustomAudio(
                uri = uri,
                name = fileName,
                durationSec = dur,
                volume = 1f,
                isExtractedFromVideo = false,
                muteOriginalVideo = true
            )
            ToastManager.showToast("Audio added: $fileName")
        }
    }

    val videoExtractPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = queryMediaFileName(context, uri)
            val dur = extractVideoDurationSec(context, uri) ?: 0f
            customAudio = FcpCustomAudio(
                uri = uri,
                name = "Extracted: $fileName",
                durationSec = dur,
                volume = 1f,
                isExtractedFromVideo = true,
                muteOriginalVideo = true
            )
            ToastManager.showToast("Audio extracted from video: $fileName")
        }
    }

    // ── Overlay & Filter State ────────────────────────────────────────────
    var textLayers by remember {
        mutableStateOf(emptyList<FcpTextLayer>())
    }
    var selectedTextLayerId by remember { mutableStateOf("") }
    var aiMaskEnabled by remember { mutableStateOf(false) }
    var bokehAperture by remember { mutableFloatStateOf(2.8f) }
    var dialogueGainDb by remember { mutableFloatStateOf(0f) }
    var voiceIsolation by remember { mutableStateOf(false) }

    val timelineSpans = remember(clips) {
        buildTimelineSpans(clips)
    }
    val totalDurationSec = remember(timelineSpans) {
        timelineSpans.lastOrNull()?.timelineEndSec?.coerceAtLeast(1f) ?: 1f
    }
    val selectedClip = remember(clips, selectedId) {
        clips.find { it.id == selectedId } ?: clips.firstOrNull()
    }

    fun saveProjectDraft(): Boolean {
        return try {
            val active = selectedClip ?: clips.firstOrNull()
            val startMs = ((active?.trimStartSec ?: 0f) * 1000f).toLong()
            val endMs = ((active?.trimEndSec ?: (active?.durationSec ?: 15f)) * 1000f).toLong()
            val draftRepo = DraftPostRepository(context)
            val draft = CreatorPostDraft(
                mode = "flash",
                title = textLayers.firstOrNull()?.primaryText ?: "Draft Video Project",
                description = "Saved ${clips.size} clips with ${textLayers.size} text layers" + (if (customAudio != null) " and soundtrack '${customAudio?.name}'" else ""),
                aspectRatio = aspectRatio.label,
                videoUri = clips.firstOrNull()?.uri?.toString(),
                flashTrimStartMs = startMs,
                flashTrimEndMs = endMs
            )
            draftRepo.saveDraft(draft)
            SoundManager.playTap(context)
            ToastManager.showToast("Project saved to Drafts")
            true
        } catch (_: Throwable) {
            ToastManager.showToast("Failed to save draft")
            false
        }
    }

    // ── Background Video Duration Extraction (Asynchronous) ──────────────
    var probedClipIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(clips) {
        val unprobed = clips.filter { !it.isDurationLoaded && it.id !in probedClipIds }
        if (unprobed.isNotEmpty()) {
            val probedMap = withContext(Dispatchers.IO) {
                unprobed.associate { clip ->
                    clip.id to extractVideoDurationSec(context, clip.uri)
                }
            }
            probedClipIds = probedClipIds + unprobed.map { it.id }
            clips = clips.map { clip ->
                if (!clip.isDurationLoaded && probedMap.containsKey(clip.id)) {
                    val realDur = probedMap[clip.id]
                    if (realDur != null && realDur > 0f) {
                        clip.copy(
                            durationSec = realDur,
                            trimStartSec = clip.trimStartSec.coerceIn(0f, (realDur - 0.25f).coerceAtLeast(0f)),
                            trimEndSec = if (clip.trimEndSec >= clip.durationSec - 0.1f) realDur else clip.trimEndSec.coerceAtMost(realDur),
                            isDurationLoaded = true
                        )
                    } else clip
                } else clip
            }
        }
    }

    // ── ExoPlayer Engine (Low Latency Realtime LoadControl) ───────────────
    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 1000,
                /* maxBufferMs = */ 5000,
                /* bufferForPlaybackMs = */ 60,
                /* bufferForPlaybackAfterRebufferMs = */ 150
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setSeekParameters(SeekParameters.EXACT)
            .build().apply {
                playWhenReady = false
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // ── Secondary Low-Latency ExoPlayer Engine (Dedicated to Custom Soundtrack) ──
    val customAudioPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(1500, 5000, 250, 500)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setSeekParameters(SeekParameters.EXACT)
            .build().apply {
                playWhenReady = false
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }
    DisposableEffect(Unit) {
        onDispose { customAudioPlayer.release() }
    }

    LaunchedEffect(customAudio?.loopAudio) {
        customAudioPlayer.repeatMode = if (customAudio?.loopAudio != false) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // Synchronize customAudioPlayer media source and volume with customAudio state
    LaunchedEffect(customAudio?.uri) {
        val audio = customAudio
        if (audio != null) {
            customAudioPlayer.setMediaItem(MediaItem.fromUri(audio.uri))
            customAudioPlayer.repeatMode = if (audio.loopAudio) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            customAudioPlayer.prepare()
            val posMs = (currentTimeSec * 1000f).toLong().coerceAtLeast(0L)
            customAudioPlayer.seekTo(posMs)
            customAudioPlayer.volume = audio.volume.coerceIn(0f, 1f)
            customAudioPlayer.playWhenReady = isPlaying
            if (isPlaying && !customAudioPlayer.isPlaying) {
                customAudioPlayer.play()
            }
        } else {
            customAudioPlayer.stop()
            customAudioPlayer.clearMediaItems()
        }
    }

    // Hardware LoudnessEnhancer for positive dialogue gain and voice isolation
    var loudnessEnhancer by remember { mutableStateOf<LoudnessEnhancer?>(null) }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0) {
                    try {
                        loudnessEnhancer?.release()
                        loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                            enabled = true
                        }
                    } catch (_: Throwable) {}
                }
            }
        }
        exoPlayer.addListener(listener)
        val initialSession = exoPlayer.audioSessionId
        if (initialSession != 0) {
            try {
                loudnessEnhancer = LoudnessEnhancer(initialSession).apply {
                    enabled = true
                }
            } catch (_: Throwable) {}
        }
        onDispose {
            exoPlayer.removeListener(listener)
            try { loudnessEnhancer?.release() } catch (_: Throwable) {}
        }
    }

    fun getActiveSpanAt(timeSec: Float): FcpTimelineClipSpan? {
        if (timelineSpans.isEmpty()) return null
        return timelineSpans.find { timeSec >= it.timelineStartSec && timeSec < it.timelineEndSec }
            ?: timelineSpans.lastOrNull()
    }

    fun getMediaTimeForGlobal(span: FcpTimelineClipSpan, globalSec: Float): Float {
        val timelineOffset = (globalSec - span.timelineStartSec).coerceIn(0f, span.durationSec)
        val mediaOffset = timelineOffset * span.clip.speed.coerceAtLeast(0.1f)
        return (span.clip.trimStartSec + mediaOffset).coerceIn(span.clip.trimStartSec, span.clip.trimEndSec)
    }

    fun getGlobalTimeForMedia(span: FcpTimelineClipSpan, mediaSec: Float): Float {
        val mediaOffset = (mediaSec - span.clip.trimStartSec).coerceAtLeast(0f)
        val timelineOffset = mediaOffset / span.clip.speed.coerceAtLeast(0.1f)
        return (span.timelineStartSec + timelineOffset).coerceIn(0f, totalDurationSec)
    }

    var loadedClipId by remember { mutableStateOf<String?>(null) }
    var loadedClipUri by remember { mutableStateOf<Uri?>(null) }
    var lastPlayerSeekMs by remember { mutableLongStateOf(0L) }

    fun ensureClipLoadedInPlayer(
        clip: FcpClip,
        targetMediaSec: Float,
        shouldPlay: Boolean,
        seamlessTransition: Boolean = false,
        forceExactSeek: Boolean = true
    ) {
        val mediaMs = (targetMediaSec * 1000f).toLong().coerceAtLeast(0L)
        val isDifferentClip = loadedClipId != clip.id
        val isDifferentUri = loadedClipUri != clip.uri

        loadedClipId = clip.id
        loadedClipUri = clip.uri

        if (isDifferentUri) {
            lastPlayerSeekMs = mediaMs
            exoPlayer.setSeekParameters(if (seamlessTransition) SeekParameters.CLOSEST_SYNC else SeekParameters.EXACT)
            exoPlayer.setMediaItem(MediaItem.fromUri(clip.uri), mediaMs)
            exoPlayer.prepare()
            exoPlayer.setPlaybackSpeed(clip.speed)
        } else {
            // Same media URI (e.g. split clip or reused media)
            if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.prepare()
            }
            if (abs(exoPlayer.playbackParameters.speed - clip.speed) > 0.01f) {
                exoPlayer.setPlaybackSpeed(clip.speed)
            }
            val currentPosMs = exoPlayer.currentPosition
            val shouldSeek = isDifferentClip || if (seamlessTransition) {
                abs(currentPosMs - mediaMs) > 60L
            } else if (!forceExactSeek) {
                abs(lastPlayerSeekMs - mediaMs) >= 24L
            } else {
                true
            }
            if (shouldSeek) {
                lastPlayerSeekMs = mediaMs
                exoPlayer.setSeekParameters(if (seamlessTransition) SeekParameters.CLOSEST_SYNC else SeekParameters.EXACT)
                exoPlayer.seekTo(mediaMs)
            }
        }
        exoPlayer.playWhenReady = shouldPlay
        if (shouldPlay) {
            if (!exoPlayer.isPlaying) exoPlayer.play()
        } else {
            if (exoPlayer.isPlaying) exoPlayer.pause()
        }
    }

    val scrubChannel = remember { Channel<Float>(Channel.CONFLATED) }

    fun onScrubStart() {
        isUserScrubbing = true
        if (isPlaying) {
            isPlaying = false
            if (exoPlayer.isPlaying) exoPlayer.pause()
            if (customAudioPlayer.isPlaying) customAudioPlayer.pause()
        }
        exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
    }

    fun onScrubSec(newSec: Float) {
        val clamped = newSec.coerceIn(0f, totalDurationSec)
        currentTimeSec = clamped
        scrubChannel.trySend(clamped)
        if (customAudio != null) {
            customAudioPlayer.seekTo((clamped * 1000f).toLong().coerceAtLeast(0L))
        }
    }

    fun onScrubEnd(finalSec: Float) {
        val clamped = finalSec.coerceIn(0f, totalDurationSec)
        currentTimeSec = clamped
        if (customAudio != null) {
            customAudioPlayer.seekTo((clamped * 1000f).toLong().coerceAtLeast(0L))
        }
        exoPlayer.setSeekParameters(SeekParameters.EXACT)
        val span = getActiveSpanAt(clamped)
        if (span != null) {
            val targetMediaSec = getMediaTimeForGlobal(span, clamped)
            if (selectedId != span.clip.id) {
                selectedId = span.clip.id
            }
            ensureClipLoadedInPlayer(
                clip = span.clip,
                targetMediaSec = targetMediaSec,
                shouldPlay = false,
                forceExactSeek = true
            )
        }
        isUserScrubbing = false
    }

    // High performance throttled frame seek loop (smooth 60/120 FPS display pacing without frame drops)
    LaunchedEffect(exoPlayer, totalDurationSec, clips) {
        for (targetGlobalSec in scrubChannel) {
            val clamped = targetGlobalSec.coerceIn(0f, totalDurationSec)
            val span = getActiveSpanAt(clamped) ?: continue
            val targetMediaSec = getMediaTimeForGlobal(span, clamped)

            if (selectedId != span.clip.id) {
                selectedId = span.clip.id
            }
            ensureClipLoadedInPlayer(
                clip = span.clip,
                targetMediaSec = targetMediaSec,
                shouldPlay = false,
                forceExactSeek = false
            )
            withFrameNanos { /* VSYNC display tick pacing */ }
        }
    }

    fun seekToGlobalTime(targetGlobalSec: Float, autoPlay: Boolean? = null) {
        val clamped = targetGlobalSec.coerceIn(0f, totalDurationSec)
        currentTimeSec = clamped
        if (customAudio != null) {
            if (customAudioPlayer.playbackState == Player.STATE_ENDED || customAudioPlayer.playbackState == Player.STATE_IDLE) {
                customAudioPlayer.prepare()
            }
            customAudioPlayer.seekTo((clamped * 1000f).toLong().coerceAtLeast(0L))
        }
        val span = getActiveSpanAt(clamped) ?: return
        val targetMediaSec = getMediaTimeForGlobal(span, clamped)

        if (selectedId != span.clip.id) {
            selectedId = span.clip.id
        }
        val shouldPlay = autoPlay ?: isPlaying
        if (autoPlay != null) {
            isPlaying = autoPlay
        }
        if (customAudio != null) {
            customAudioPlayer.playWhenReady = shouldPlay
            if (shouldPlay) {
                if (!customAudioPlayer.isPlaying) customAudioPlayer.play()
            } else {
                if (customAudioPlayer.isPlaying) customAudioPlayer.pause()
            }
        }
        exoPlayer.setSeekParameters(SeekParameters.EXACT)
        ensureClipLoadedInPlayer(
            clip = span.clip,
            targetMediaSec = targetMediaSec,
            shouldPlay = shouldPlay,
            forceExactSeek = true
        )
    }

    // Load initial or selected clip into ExoPlayer
    LaunchedEffect(selectedId) {
        val clip = clips.find { it.id == selectedId } ?: clips.firstOrNull()
        if (clip != null) {
            val span = timelineSpans.find { it.clip.id == clip.id }
            val mediaSec = if (span != null) getMediaTimeForGlobal(span, currentTimeSec) else clip.trimStartSec
            ensureClipLoadedInPlayer(
                clip = clip,
                targetMediaSec = mediaSec,
                shouldPlay = isPlaying,
                forceExactSeek = true
            )
        }
    }

    // Track playback duration once media is ready
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val durMs = exoPlayer.duration
                    if (durMs > 0 && selectedClip != null) {
                        val durSec = durMs / 1000f
                        if (abs(selectedClip.durationSec - durSec) > 0.5f && selectedClip.durationSec == 15f) {
                            clips = clips.map {
                                if (it.id == selectedClip.id) it.copy(durationSec = durSec, trimEndSec = durSec, isDurationLoaded = true) else it
                            }
                        }
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(isPlaying) {
        if (customAudio != null) {
            if (isPlaying && (customAudioPlayer.playbackState == Player.STATE_ENDED || customAudioPlayer.playbackState == Player.STATE_IDLE)) {
                customAudioPlayer.prepare()
            }
            customAudioPlayer.playWhenReady = isPlaying
            if (isPlaying) {
                if (!customAudioPlayer.isPlaying) customAudioPlayer.play()
            } else {
                if (customAudioPlayer.isPlaying) customAudioPlayer.pause()
            }
        }
    }

    // High precision master playback clock & timeline sync (VSYNC synchronized, buttery smooth transitions)
    LaunchedEffect(isPlaying, isUserScrubbing, clips, isAutoReplay, customAudio) {
        if (!isPlaying || isUserScrubbing) return@LaunchedEffect

        var lastFrameNanos = 0L

        while (isActive && isPlaying && !isUserScrubbing) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameTimeNanos
                    return@withFrameNanos
                }
                val dtNanos = frameTimeNanos - lastFrameNanos
                lastFrameNanos = frameTimeNanos
                val dtSec = (dtNanos / 1_000_000_000f).coerceIn(0f, 0.1f)

                val curSpan = getActiveSpanAt(currentTimeSec)
                val speed = curSpan?.clip?.speed?.coerceAtLeast(0.1f) ?: 1f
                val proposedTimeSec = currentTimeSec + (dtSec * speed)

                if (proposedTimeSec >= totalDurationSec) {
                    if (isAutoReplay) {
                        seekToGlobalTime(0f, autoPlay = true)
                    } else {
                        currentTimeSec = totalDurationSec
                        isPlaying = false
                        exoPlayer.pause()
                        if (customAudioPlayer.isPlaying) customAudioPlayer.pause()
                    }
                } else {
                    currentTimeSec = proposedTimeSec

                    val newSpan = getActiveSpanAt(proposedTimeSec)
                    if (newSpan != null) {
                        val clip = newSpan.clip
                        val targetMediaSec = getMediaTimeForGlobal(newSpan, proposedTimeSec)

                        if (loadedClipId != clip.id) {
                            // Seamless cut transition across clips!
                            selectedId = clip.id
                            ensureClipLoadedInPlayer(
                                clip = clip,
                                targetMediaSec = targetMediaSec,
                                shouldPlay = true,
                                seamlessTransition = true
                            )
                        } else {
                            if (!exoPlayer.isPlaying && exoPlayer.playbackState == Player.STATE_READY && exoPlayer.playWhenReady) {
                                exoPlayer.play()
                            }
                            // Keep player in tight sync with master timeline clock
                            val curPosSec = exoPlayer.currentPosition / 1000f
                            if (abs(curPosSec - targetMediaSec) > 0.22f) {
                                exoPlayer.seekTo((targetMediaSec * 1000f).toLong().coerceAtLeast(0L))
                            }
                        }
                    }
                }

                // Drift compensation for custom soundtrack
                if (customAudio != null && isPlaying) {
                    val audioPosMs = customAudioPlayer.currentPosition
                    val targetMs = (currentTimeSec * 1000f).toLong()
                    if (abs(audioPosMs - targetMs) > 180L) {
                        if (customAudioPlayer.playbackState == Player.STATE_ENDED || customAudioPlayer.playbackState == Player.STATE_IDLE) {
                            customAudioPlayer.prepare()
                        }
                        customAudioPlayer.seekTo(targetMs.coerceAtLeast(0L))
                    }
                    if (!customAudioPlayer.isPlaying && customAudioPlayer.playWhenReady) {
                        customAudioPlayer.play()
                    }
                }
            }
        }
    }

    // Set playback speed when clip speed changes
    LaunchedEffect(selectedClip?.speed) {
        selectedClip?.let {
            exoPlayer.setPlaybackSpeed(it.speed)
        }
    }

    // Audio Gain, Voice Isolation & Custom Audio Volume/Muting mapping
    LaunchedEffect(dialogueGainDb, voiceIsolation, customAudio?.muteOriginalVideo, customAudio?.volume) {
        val audio = customAudio
        val isMuted = audio?.muteOriginalVideo == true
        if (isMuted) {
            exoPlayer.volume = 0f
            try { loudnessEnhancer?.setTargetGain(0) } catch (_: Throwable) {}
        } else if (dialogueGainDb < 0f) {
            val volume = (10f.pow(dialogueGainDb / 20f)).coerceIn(0f, 1f)
            exoPlayer.volume = volume
            try { loudnessEnhancer?.setTargetGain(0) } catch (_: Throwable) {}
        } else {
            exoPlayer.volume = 1f
            val boostMb = (dialogueGainDb * 100f).toInt() + (if (voiceIsolation) 300 else 0)
            try {
                loudnessEnhancer?.apply {
                    setTargetGain(boostMb.coerceIn(0, 2000))
                    enabled = true
                }
            } catch (_: Throwable) {}
        }
        if (audio != null) {
            customAudioPlayer.volume = audio.volume.coerceIn(0f, 1f)
        }
    }

    // Dynamic background blur extraction from current video frame (quantized to 1.5s intervals)
    val quantizedTimeSec = remember(currentTimeSec) {
        (currentTimeSec / 1.5f).toInt() * 1.5f
    }
    LaunchedEffect(selectedClip?.uri, quantizedTimeSec, canvasBackground.type, isPlaying) {
        if (canvasBackground.type != FcpCanvasBgType.BLUR) return@LaunchedEffect
        // While actively playing, do not churn MediaMetadataRetriever to avoid disk I/O contention
        if (isPlaying && blurBackgroundBitmap != null) return@LaunchedEffect
        val curClip = selectedClip ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val timeUs = (quantizedTimeSec * 1_000_000L).toLong()
            val key = "blur_bg_${curClip.uri}_${timeUs / 1_500_000L}"
            val cached = FcpThumbnailMemoryCache.get(key)
            if (cached != null) {
                blurBackgroundBitmap = cached
            } else {
                var retriever: MediaMetadataRetriever? = null
                try {
                    retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, curClip.uri)
                    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            160,
                            200
                        )
                    } else {
                        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                    if (bmp != null) {
                        FcpThumbnailMemoryCache.put(key, bmp)
                        blurBackgroundBitmap = bmp
                    }
                } catch (_: Throwable) {
                } finally {
                    try { retriever?.release() } catch (_: Throwable) {}
                }
            }
        }
    }

    // Undo / Redo helpers
    fun saveStateForUndo() {
        undoStack = (undoStack + listOf(clips)).takeLast(25)
        redoStack = emptyList()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.last()
            undoStack = undoStack.dropLast(1)
            redoStack = (redoStack + listOf(clips)).takeLast(25)
            clips = previous
            if (clips.none { it.id == selectedId }) {
                selectedId = clips.firstOrNull()?.id
            }
            ToastManager.showToast("Action undone")
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.last()
            redoStack = redoStack.dropLast(1)
            undoStack = (undoStack + listOf(clips)).takeLast(25)
            clips = next
            ToastManager.showToast("Action redone")
        }
    }

    // Media picker for importing clips
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            saveStateForUndo()
            scope.launch {
                val newClips = withContext(Dispatchers.IO) {
                    uris.mapIndexed { idx, uri ->
                        val fileName = uri.lastPathSegment?.takeLast(14) ?: "Clip_${clips.size + idx + 1}"
                        val realDur = extractVideoDurationSec(context, uri)
                        val dur = realDur ?: 15f
                        FcpClip(
                            uri = uri,
                            name = fileName,
                            durationSec = dur,
                            trimStartSec = 0f,
                            trimEndSec = dur,
                            isDurationLoaded = realDur != null
                        )
                    }
                }
                clips = clips + newClips
                selectedId = newClips.first().id
                ToastManager.showToast("${newClips.size} clip(s) imported")
            }
        }
    }

    // Initialize with initialUri or initialUris if provided
    LaunchedEffect(initialUri, initialUris) {
        if (clips.isEmpty()) {
            val toLoad = when {
                initialUris.isNotEmpty() -> initialUris
                initialUri != null -> listOf(initialUri)
                else -> emptyList()
            }
            if (toLoad.isNotEmpty()) {
                val loadedClips = withContext(Dispatchers.IO) {
                    toLoad.mapIndexed { idx, uri ->
                        val fileName = uri.lastPathSegment?.takeLast(14) ?: "Clip_${idx + 1}"
                        val realDur = extractVideoDurationSec(context, uri)
                        val isPhoto = try {
                            val type = context.contentResolver.getType(uri)
                            type?.startsWith("image/") == true
                        } catch (_: Exception) { false }
                        val dur = realDur ?: if (isPhoto) 4f else 15f
                        FcpClip(
                            uri = uri,
                            name = fileName,
                            durationSec = dur,
                            trimStartSec = 0f,
                            trimEndSec = dur,
                            isDurationLoaded = realDur != null || isPhoto
                        )
                    }
                }
                clips = loadedClips
                selectedId = loadedClips.firstOrNull()?.id
            }
        }
    }

    // ── Root ChromaStudio Layout ──────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (colors.isDark) Color(0xFF1A1D24) else colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {

            // ══════════════════════════════════════════════════════════════════
            // 1. CHROMA APP BAR (Streamlined Header - Clean, No Collisions)
            // ══════════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(if (colors.isDark) Color(0xFF1E212B) else colors.glassBg)
                    .border(width = 0.5.dp, color = if (colors.isDark) Color(0xFF2A2F3A) else colors.glassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Back button + Branding & Clip count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconGhostButton(
                            icon = Lucide.ArrowLeft,
                            contentDesc = "Back",
                            onClick = {
                                SoundManager.playTap(context)
                                if (clips.isNotEmpty()) {
                                    showExitConfirmationDialog = true
                                } else {
                                    onBack()
                                }
                            }
                        )

                        Column(verticalArrangement = Arrangement.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Flash",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = (-0.3).sp
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Studio",
                                    color = Color(0xFF007AFF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = (-0.3).sp
                                )
                            }
                            if (clips.isNotEmpty()) {
                                Text(
                                    text = "${clips.size} ${if (clips.size == 1) "clip" else "clips"}",
                                    color = Color(0xFF8E98A8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Right: Undo, Redo (No circles), Divider, Export Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconGhostButton(
                            icon = Lucide.Undo,
                            contentDesc = "Undo",
                            disabled = undoStack.isEmpty(),
                            onClick = {
                                SoundManager.playTap(context)
                                undo()
                            }
                        )
                        IconGhostButton(
                            icon = Lucide.Redo,
                            contentDesc = "Redo",
                            disabled = redoStack.isEmpty(),
                            onClick = {
                                SoundManager.playTap(context)
                                redo()
                            }
                        )

                        // Divider
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(1.dp)
                                .height(18.dp)
                                .background(colors.glassBorder)
                        )

                        // Export Primary Button
                        PrimaryGradientButton(
                            onClick = {
                                SoundManager.playTap(context)
                                showExportSheet = true
                            },
                            height = 34.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Export",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Lucide.ChevronRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // 2. ASPECT RATIO SHAPE PILLS & CANVAS FILL PILL
            // ══════════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allAspects = listOf(
                        FcpAspectRatio.VERTICAL_9_16,
                        FcpAspectRatio.SQUARE_1_1,
                        FcpAspectRatio.WIDESCREEN_16_9,
                        FcpAspectRatio.CINEMATIC_21_9
                    )
                    allAspects.forEach { mode ->
                        AspectRatioPill(
                            aspect = mode,
                            active = aspectRatio == mode,
                            onClick = {
                                aspectRatio = mode
                                ToastManager.showToast(mode.label)
                            }
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .height(18.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.18f))
                    )

                    // Quick Canvas Background Pill
                    CanvasBgQuickPill(
                        background = canvasBackground,
                        isActive = activeTab == FcpWorkspaceTab.CANVAS,
                        onClick = {
                            activeTab = FcpWorkspaceTab.CANVAS
                            if (drawerLevel == VideoDrawerLevel.COLLAPSED) {
                                drawerLevel = VideoDrawerLevel.MID
                            }
                        }
                    )
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // 3. CANVAS / VIDEO PREVIEW CARD (Matching CanvasPreview)
            // ══════════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val previewShape = RoundedCornerShape(16.dp)
                val activeScale = selectedClip?.scale ?: 1f
                val activePanX = selectedClip?.panX ?: 0f
                val activePanY = selectedClip?.panY ?: 0f

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(aspectRatio.ratio)
                        .shadow(elevation = 8.dp, shape = previewShape, ambientColor = Color(0x0D1F2687), spotColor = Color(0x0D1F2687))
                        .clip(previewShape)
                        .background(Color(0xFF111317))
                        .border(1.dp, Color(0xFF2A2F3A), previewShape)
                        .pointerInput(selectedId) {
                            detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                                val active = selectedClip ?: return@detectTransformGestures
                                if (zoom != 1f || (active.scale > 1.02f && pan != Offset.Zero)) {
                                    val newScale = (active.scale * zoom).coerceIn(0.5f, 5.0f)
                                    val maxPanX = (newScale - 0.7f).coerceAtLeast(0f) * 600f
                                    val maxPanY = (newScale - 0.7f).coerceAtLeast(0f) * 800f
                                    val newPanX = (active.panX + pan.x).coerceIn(-maxPanX, maxPanX)
                                    val newPanY = (active.panY + pan.y).coerceIn(-maxPanY, maxPanY)
                                    clips = clips.map { c ->
                                        if (c.id == active.id) {
                                            c.copy(scale = newScale, panX = newPanX, panY = newPanY)
                                        } else c
                                    }
                                }
                            }
                        }
                        .pointerInput(totalDurationSec, currentTimeSec, isPlaying, selectedId) {
                            detectTapGestures(
                                onDoubleTap = {
                                    val active = selectedClip ?: return@detectTapGestures
                                    saveStateForUndo()
                                    val nextScale = if (active.scale > 1.05f) 1.0f else 1.35f
                                    clips = clips.map { c ->
                                        if (c.id == active.id) {
                                            c.copy(scale = nextScale, panX = 0f, panY = 0f)
                                        } else c
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    ToastManager.showToast(if (nextScale == 1.0f) "Reset to Fit (1.0x)" else "Fill & Crop (1.35x)")
                                },
                                onTap = {
                                    SoundManager.playTap(context)
                                    val isAtEnd = totalDurationSec > 0f && currentTimeSec >= totalDurationSec - 0.08f
                                    if (isAtEnd) {
                                        seekToGlobalTime(0f, autoPlay = true)
                                        triggerOverlayFlash(true)
                                    } else {
                                        val nextPlay = !isPlaying
                                        isPlaying = nextPlay
                                        if (nextPlay) {
                                            exoPlayer.play()
                                        } else {
                                            exoPlayer.pause()
                                        }
                                        triggerOverlayFlash(nextPlay)
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val supportsRenderEffect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    val activeColorMatrix = remember(
                        selectedClip?.exposure,
                        selectedClip?.contrast,
                        selectedClip?.saturation,
                        selectedClip?.temperature,
                        selectedClip?.tint,
                        selectedClip?.lut
                    ) {
                        buildFcpColorMatrix(
                            exposure = selectedClip?.exposure ?: 0f,
                            contrast = selectedClip?.contrast ?: 100f,
                            saturation = selectedClip?.saturation ?: 100f,
                            temperature = selectedClip?.temperature ?: 0f,
                            tint = selectedClip?.tint ?: 0f,
                            lut = selectedClip?.lut ?: FcpLutPreset.NONE
                        )
                    }

                    // Canvas Background Fill Layer (Blur, Solid Color, or Custom Image/Backdrop)
                    CanvasBackgroundLayer(
                        background = canvasBackground,
                        blurBitmap = blurBackgroundBitmap,
                        supportsRenderEffect = supportsRenderEffect
                    )

                    // Live Clip Transition State across cut boundaries (bridges outgoing & incoming clip)
                    val activeTransitionState = remember(currentTimeSec, timelineSpans) {
                        var result: Pair<FcpTransitionType, Float>? = null
                        for (i in 0 until timelineSpans.size - 1) {
                            val spanA = timelineSpans[i]
                            val spanB = timelineSpans[i + 1]
                            val trans = spanA.clip.transitionToNext
                            if (trans != FcpTransitionType.NONE) {
                                val cutSec = spanA.timelineEndSec
                                val dur = spanA.clip.transitionDurationSec.coerceIn(0.2f, 2f)
                                val halfDur = dur * 0.5f
                                val startSec = (cutSec - halfDur).coerceAtLeast(spanA.timelineStartSec)
                                val endSec = (cutSec + halfDur).coerceAtMost(spanB.timelineEndSec)
                                if (currentTimeSec in startSec..endSec) {
                                    val totalWindow = (endSec - startSec).coerceAtLeast(0.05f)
                                    val progress = ((currentTimeSec - startSec) / totalWindow).coerceIn(0f, 1f)
                                    result = Pair(trans, progress)
                                    break
                                }
                            }
                        }
                        result
                    }

                    // ExoPlayer Video Surface with dynamic Color Matrix filter
                    AndroidView(
                        factory = { ctx ->
                            val aspectRatioLayout = AspectRatioFrameLayout(ctx).apply {
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            }
                            val textureView = TextureView(ctx).apply {
                                isOpaque = false
                            }
                            val layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            ).apply { gravity = Gravity.CENTER }

                            aspectRatioLayout.addView(textureView, layoutParams)
                            exoPlayer.setVideoTextureView(textureView)

                            exoPlayer.addListener(object : Player.Listener {
                                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                                    if (videoSize.height > 0 && videoSize.width > 0) {
                                        val ratio = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height
                                        aspectRatioLayout.setAspectRatio(ratio)
                                    }
                                }
                            })
                            aspectRatioLayout
                        },
                        update = { _ -> },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = activeScale
                                scaleY = activeScale
                                translationX = activePanX
                                translationY = activePanY
                                if (supportsRenderEffect) {
                                    val hasCustomColor = (selectedClip?.exposure ?: 0f) != 0f ||
                                        (selectedClip?.contrast ?: 100f) != 100f ||
                                        (selectedClip?.saturation ?: 100f) != 100f ||
                                        (selectedClip?.temperature ?: 0f) != 0f ||
                                        (selectedClip?.tint ?: 0f) != 0f ||
                                        (selectedClip?.lut ?: FcpLutPreset.NONE) != FcpLutPreset.NONE

                                    renderEffect = if (hasCustomColor) {
                                        RenderEffect
                                            .createColorFilterEffect(ColorMatrixColorFilter(activeColorMatrix))
                                            .asComposeRenderEffect()
                                    } else null
                                }
                            }
                    )

                    // Photo Overlay for Image Clips
                    val isCurrentClipPhoto = remember(selectedClip?.uri) {
                        selectedClip?.let { clip ->
                            try {
                                val type = context.contentResolver.getType(clip.uri)
                                type?.startsWith("image/") == true
                            } catch (_: Exception) { false }
                        } ?: false
                    }

                    if (isCurrentClipPhoto && selectedClip != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(selectedClip.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Photo Clip",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = activeScale
                                    scaleY = activeScale
                                    translationX = activePanX
                                    translationY = activePanY
                                    if (supportsRenderEffect) {
                                        val hasCustomColor = selectedClip.exposure != 0f ||
                                            selectedClip.contrast != 100f ||
                                            selectedClip.saturation != 100f ||
                                            selectedClip.temperature != 0f ||
                                            selectedClip.tint != 0f ||
                                            selectedClip.lut != FcpLutPreset.NONE

                                        renderEffect = if (hasCustomColor) {
                                            RenderEffect
                                                .createColorFilterEffect(ColorMatrixColorFilter(activeColorMatrix))
                                                .asComposeRenderEffect()
                                        } else null
                                    }
                                },
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Live Transition Visual Overlay Layer (Renders seamlessly across video boundary cut)
                    if (activeTransitionState != null) {
                        val (transType, progress) = activeTransitionState
                        when (transType) {
                            FcpTransitionType.DIP_TO_BLACK -> {
                                val blackAlpha = if (progress <= 0.5f) (progress * 2f) else ((1f - progress) * 2f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = blackAlpha.coerceIn(0f, 1f)))
                                        .zIndex(20f)
                                )
                            }
                            FcpTransitionType.DIP_TO_WHITE -> {
                                val whiteAlpha = if (progress <= 0.5f) (progress * 2f) else ((1f - progress) * 2f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White.copy(alpha = (whiteAlpha * 0.98f).coerceIn(0f, 1f)))
                                        .zIndex(20f)
                                )
                            }
                            FcpTransitionType.CROSSFADE -> {
                                val crossAlpha = (sin(progress * Math.PI.toFloat()) * 0.75f).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = crossAlpha * 0.45f),
                                                    Color.Black.copy(alpha = crossAlpha * 0.7f)
                                                )
                                            )
                                        )
                                        .zIndex(20f)
                                )
                            }
                            FcpTransitionType.WIPE_RIGHT -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(20f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = progress)
                                            .background(Color.Black.copy(alpha = 0.92f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(3.dp)
                                            .fillMaxWidth(fraction = progress)
                                            .wrapContentWidth(Alignment.End)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF00F5D4), Color(0xFF7B2CBF))
                                                )
                                            )
                                    )
                                }
                            }
                            FcpTransitionType.WIPE_LEFT -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(20f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = 1f - progress)
                                            .align(Alignment.CenterEnd)
                                            .background(Color.Black.copy(alpha = 0.92f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(3.dp)
                                            .fillMaxWidth(fraction = 1f - progress)
                                            .align(Alignment.CenterEnd)
                                            .wrapContentWidth(Alignment.Start)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFFFF007F), Color(0xFF7B2CBF))
                                                )
                                            )
                                    )
                                }
                            }
                            FcpTransitionType.SLIDE_LEFT -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(20f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = progress)
                                            .background(Color(0xFF0A0E1A).copy(alpha = 0.88f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(2.dp)
                                            .fillMaxWidth(fraction = progress)
                                            .wrapContentWidth(Alignment.End)
                                            .background(Color.White.copy(alpha = 0.6f))
                                    )
                                }
                            }
                            FcpTransitionType.ZOOM_IN -> {
                                val zoomAlpha = sin(progress * Math.PI.toFloat()).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = zoomAlpha * 0.85f)
                                                )
                                            )
                                        )
                                        .zIndex(20f)
                                )
                            }
                            FcpTransitionType.GLITCH -> {
                                val glitchAlpha = if (progress in 0.25f..0.75f) {
                                    val subProg = (progress - 0.25f) / 0.5f
                                    sin(subProg * Math.PI.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                if (glitchAlpha > 0.05f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Cyan.copy(alpha = glitchAlpha * 0.45f),
                                                        Color.Transparent,
                                                        Color.Magenta.copy(alpha = glitchAlpha * 0.5f),
                                                        Color.Transparent,
                                                        Color.Cyan.copy(alpha = glitchAlpha * 0.4f)
                                                    )
                                                )
                                            )
                                            .zIndex(20f)
                                    )
                                }
                            }
                            FcpTransitionType.NONE -> {}
                        }
                    }

                    // Floating Zoom / Crop Indicator Badge with Reset Button
                    if (activeScale != 1f || activePanX != 0f || activePanY != 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .clickable {
                                    val active = selectedClip ?: return@clickable
                                    saveStateForUndo()
                                    clips = clips.map {
                                        if (it.id == active.id) it.copy(scale = 1f, panX = 0f, panY = 0f) else it
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    ToastManager.showToast("Zoom reset to 1.0x")
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.RotateCcw,
                                    contentDescription = "Reset Zoom",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${String.format("%.1f", activeScale)}x",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Reset",
                                    color = colors.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Cinematic LUT Atmospheric Overlay
                    when (selectedClip?.lut) {
                        FcpLutPreset.TEAL_ORANGE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF0D9488).copy(alpha = 0.12f),
                                                Color(0xFFF97316).copy(alpha = 0.12f)
                                            )
                                        )
                                    )
                            )
                        }
                        FcpLutPreset.CYBER -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF3B82F6).copy(alpha = 0.10f),
                                                Color(0xFFEC4899).copy(alpha = 0.10f)
                                            )
                                        )
                                    )
                            )
                        }
                        FcpLutPreset.WARM_GOLD -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFFD97706).copy(alpha = 0.10f),
                                                Color(0xFFFBBF24).copy(alpha = 0.08f)
                                            )
                                        )
                                    )
                            )
                        }
                        FcpLutPreset.COOL_NORDIC -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF0284C7).copy(alpha = 0.10f),
                                                Color(0xFF38BDF8).copy(alpha = 0.08f)
                                            )
                                        )
                                    )
                            )
                        }
                        FcpLutPreset.VINTAGE_FADE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF78350F).copy(alpha = 0.08f),
                                                Color(0xFFFDE68A).copy(alpha = 0.06f)
                                            )
                                        )
                                    )
                            )
                        }
                        FcpLutPreset.EMERALD -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF065F46).copy(alpha = 0.10f),
                                                Color(0xFF10B981).copy(alpha = 0.08f)
                                            )
                                        )
                                    )
                            )
                        }
                        FcpLutPreset.NOIR, FcpLutPreset.NONE, null -> {}
                    }

                    // AI Subject Mask Depth Vignette Simulation
                    if (aiMaskEnabled) {
                        val blurRadius = (bokehAperture * 100f).coerceIn(150f, 600f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                                        radius = blurRadius
                                    )
                                )
                        )
                    }

                    // Live Multi-Layer Text / Title Overlays
                    textLayers.forEach { layer ->
                        if (layer.isVisible && (layer.primaryText.isNotBlank() || layer.scriptText.isNotBlank())) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(x = layer.offsetX.dp, y = layer.offsetY.dp)
                            ) {
                                FcpTextLayerOverlay(
                                    layer = layer,
                                    isSelected = layer.id == selectedTextLayerId && activeTab == FcpWorkspaceTab.TITLES,
                                    onSelect = {
                                        selectedTextLayerId = layer.id
                                        activeTab = FcpWorkspaceTab.TITLES
                                        textLayers = textLayers.map {
                                            if (it.id == layer.id) it.copy(isExpanded = true) else it
                                        }
                                    },
                                    onTransform = { dxDp, dyDp, zoomChange, rotationDelta ->
                                        textLayers = textLayers.map {
                                            if (it.id == layer.id) {
                                                val newSize = (it.fontSizeSp * zoomChange).coerceIn(12f, 80f)
                                                val newRot = it.rotationDeg + rotationDelta
                                                val snappedRot = if (abs(newRot % 90f) < 4f) {
                                                    round(newRot / 90f) * 90f
                                                } else if (abs(newRot) < 4f) 0f else newRot
                                                it.copy(
                                                    offsetX = it.offsetX + dxDp,
                                                    offsetY = it.offsetY + dyDp,
                                                    fontSizeSp = newSize,
                                                    rotationDeg = snappedRot
                                                )
                                            } else it
                                        }
                                    },
                                    onDuplicate = {
                                        saveStateForUndo()
                                        val dup = layer.copy(
                                            id = UUID.randomUUID().toString(),
                                            name = "${layer.name} Copy",
                                            offsetY = layer.offsetY + 20f
                                        )
                                        val idx = textLayers.indexOfFirst { it.id == layer.id }
                                        val mutable = textLayers.toMutableList()
                                        mutable.add(idx + 1, dup)
                                        textLayers = mutable
                                        selectedTextLayerId = dup.id
                                        ToastManager.showToast("Title duplicated")
                                    },
                                    onDelete = {
                                        saveStateForUndo()
                                        val remaining = textLayers.filter { it.id != layer.id }
                                        textLayers = remaining
                                        selectedTextLayerId = remaining.firstOrNull()?.id ?: ""
                                        ToastManager.showToast("Title removed")
                                    }
                                )
                            }
                        }
                    }

                    // Quick center flash overlay indicator (Matches HTML overlay-icon)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = flashOverlayVisible,
                        enter = fadeIn(tween(100)) + scaleIn(tween(140), initialScale = 0.72f),
                        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 1.12f),
                        modifier = Modifier.align(Alignment.Center).zIndex(50f)
                    ) {
                        flashOverlayIcon?.let { iconType ->
                            FlatPlaybackIcon(
                                type = iconType,
                                modifier = Modifier.size(72.dp),
                                tint = Color.White.copy(alpha = 0.90f)
                            )
                        }
                    }

                    // Floating Pro Scrub Frame HUD (Appears when dragging playhead, ruler, jog wheel or trims)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isUserScrubbing,
                        enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.9f),
                        exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .zIndex(45f)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.82f))
                                .border(1.dp, Color(0xFF0A84FF).copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                                .shadow(12.dp, spotColor = Color(0xFF0A84FF), shape = RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                                Text(
                                    text = formatTimecode(currentTimeSec),
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(10.dp)
                                        .background(Color.White.copy(alpha = 0.3f))
                                )
                                val curFrame = (currentTimeSec * 30f).roundToInt()
                                Text(
                                    text = "FRAME $curFrame",
                                    color = Color(0xFF60A5FA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // Empty state indicator if no clips loaded (Apple Video Graphics)
                    if (clips.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppleVideosIcon(modifier = Modifier.size(72.dp))
                            Text(
                                text = "Select or import a video",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            PrimaryGradientButton(
                                onClick = { videoPicker.launch("video/*") },
                                height = 34.dp
                            ) {
                                Text("Import Video", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // 3.5. DEDICATED FLAT PLAYBACK TRANSPORT CONTROLS
            // (Replay, Play/Pause Toggle, Auto-Playback Toggle)
            // ══════════════════════════════════════════════════════════════════
            if (clips.isNotEmpty()) {
                PlaybackTransportControlBar(
                    isPlaying = isPlaying,
                    isAutoReplay = isAutoReplay,
                    onPlayPauseToggle = {
                        val isAtEnd = totalDurationSec > 0f && currentTimeSec >= totalDurationSec - 0.08f
                        if (isAtEnd) {
                            seekToGlobalTime(0f, autoPlay = true)
                            triggerOverlayFlash(true)
                        } else {
                            val nextPlay = !isPlaying
                            isPlaying = nextPlay
                            if (nextPlay) {
                                exoPlayer.play()
                            } else {
                                exoPlayer.pause()
                            }
                            triggerOverlayFlash(nextPlay)
                        }
                    },
                    onReplay = {
                        seekToGlobalTime(0f, autoPlay = true)
                        triggerOverlayFlash(true)
                    },
                    onAutoReplayToggle = {
                        isAutoReplay = !isAutoReplay
                        ToastManager.showToast(if (isAutoReplay) "Auto Replay: ON" else "Auto Replay: OFF")
                    }
                )
            }

            // ══════════════════════════════════════════════════════════════════
            // 4. CHROMA BOTTOM DRAWER (Inspector, Tools & Timeline)
            // ══════════════════════════════════════════════════════════════════
            val animatedDrawerHeight by animateDpAsState(
                targetValue = drawerLevel.heightDp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "videoDrawerHeight"
            )
            var dragAccum by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedDrawerHeight)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        ambientColor = Color(0x14000000),
                        spotColor = Color(0x14000000)
                    )
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(colors.glassBg)
                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Column(Modifier.fillMaxSize()) {

                    // Draggable Grab Bar Handle (Identical to ChromaStudio BottomDrawer)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (dragAccum < -35f) {
                                            drawerLevel = when (drawerLevel) {
                                                VideoDrawerLevel.COLLAPSED -> VideoDrawerLevel.MID
                                                VideoDrawerLevel.MID -> VideoDrawerLevel.FULL
                                                VideoDrawerLevel.FULL -> VideoDrawerLevel.FULL
                                            }
                                        } else if (dragAccum > 35f) {
                                            drawerLevel = when (drawerLevel) {
                                                VideoDrawerLevel.FULL -> VideoDrawerLevel.MID
                                                VideoDrawerLevel.MID -> VideoDrawerLevel.COLLAPSED
                                                VideoDrawerLevel.COLLAPSED -> VideoDrawerLevel.COLLAPSED
                                            }
                                        }
                                        dragAccum = 0f
                                    }
                                ) { change, dragY ->
                                    change.consume()
                                    dragAccum += dragY
                                }
                            }
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                drawerLevel = when (drawerLevel) {
                                    VideoDrawerLevel.COLLAPSED -> VideoDrawerLevel.MID
                                    VideoDrawerLevel.MID -> VideoDrawerLevel.FULL
                                    VideoDrawerLevel.FULL -> VideoDrawerLevel.COLLAPSED
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFD1D5DB))
                        )
                    }

                    // Interactive Tab Switcher (Matching UiverseTabs from ChromaStudio)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VideoWorkspaceTabs(
                            activeTab = activeTab,
                            onTabSelected = {
                                activeTab = it
                                if (drawerLevel == VideoDrawerLevel.COLLAPSED) {
                                    drawerLevel = VideoDrawerLevel.MID
                                }
                            },
                            isDarkMode = colors.isDark
                        )
                    }

                    // Drawer Tab Content
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        when (activeTab) {
                            FcpWorkspaceTab.TIMELINE -> {
                                TimelineTabContent(
                                    clips = clips,
                                    timelineSpans = timelineSpans,
                                    selectedId = selectedId,
                                    currentTimeSec = currentTimeSec,
                                    totalDurationSec = totalDurationSec,
                                    pixelsPerSec = pixelsPerSec,
                                    onZoomChange = { pixelsPerSec = it.coerceIn(15f, 120f) },
                                    isPlaying = isPlaying,
                                    isUserScrubbing = isUserScrubbing,
                                    isAutoReplay = isAutoReplay,
                                    onAutoReplayToggle = {
                                        isAutoReplay = !isAutoReplay
                                        SoundManager.playTap(context)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        ToastManager.showToast(if (isAutoReplay) "Auto Replay: ON" else "Auto Replay: OFF")
                                    },
                                    onPlayToggle = {
                                        SoundManager.playTap(context)
                                        val isAtEnd = totalDurationSec > 0f && currentTimeSec >= totalDurationSec - 0.08f
                                        if (isAtEnd) {
                                            seekToGlobalTime(0f, autoPlay = true)
                                        } else {
                                            val nextPlaying = !isPlaying
                                            if (nextPlaying) {
                                                isPlaying = true
                                                exoPlayer.play()
                                            } else {
                                                isPlaying = false
                                                exoPlayer.pause()
                                            }
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    onSelectClip = { clipId ->
                                        selectedId = clipId
                                        val span = timelineSpans.find { it.clip.id == clipId }
                                        if (span != null) {
                                            seekToGlobalTime(span.timelineStartSec)
                                        }
                                    },
                                    onTrimStart = {
                                        onScrubStart()
                                        saveStateForUndo()
                                    },
                                    onTrimLeft = { clipId, deltaSec ->
                                        clips = clips.map { c ->
                                            if (c.id == clipId) {
                                                val newStart = (c.trimStartSec + deltaSec).coerceIn(0f, c.trimEndSec - 0.5f)
                                                c.copy(trimStartSec = newStart)
                                            } else c
                                        }
                                        val clipIdx = clips.indexOfFirst { it.id == clipId }
                                        val prevSec = if (clipIdx > 0) {
                                            clips.take(clipIdx).sumOf { it.trimmedDurationSec.toDouble() }.toFloat()
                                        } else 0f
                                        onScrubSec(prevSec)
                                    },
                                    onTrimRight = { clipId, deltaSec ->
                                        clips = clips.map { c ->
                                            if (c.id == clipId) {
                                                val newEnd = (c.trimEndSec + deltaSec).coerceIn(c.trimStartSec + 0.5f, c.durationSec)
                                                c.copy(trimEndSec = newEnd)
                                            } else c
                                        }
                                        val clipIdx = clips.indexOfFirst { it.id == clipId }
                                        val prevSec = if (clipIdx > 0) {
                                            clips.take(clipIdx).sumOf { it.trimmedDurationSec.toDouble() }.toFloat()
                                        } else 0f
                                        val curDur = clips.find { it.id == clipId }?.trimmedDurationSec ?: 0f
                                        onScrubSec((prevSec + curDur - 0.05f).coerceAtLeast(0f))
                                    },
                                    onReorderClips = { fromIdx, toIdx ->
                                        if (fromIdx in clips.indices && toIdx in clips.indices && fromIdx != toIdx) {
                                            saveStateForUndo()
                                            val mutable = clips.toMutableList()
                                            val item = mutable.removeAt(fromIdx)
                                            mutable.add(toIdx, item)
                                            clips = mutable
                                            selectedId = item.id
                                            ToastManager.showToast("Clip reordered")
                                        }
                                    },
                                    onScrubStart = { onScrubStart() },
                                    onScrubSec = { sec -> onScrubSec(sec) },
                                    onScrubEnd = { sec -> onScrubEnd(sec) },
                                    onSplitClip = {
                                        val span = timelineSpans.find { currentTimeSec > it.timelineStartSec + 0.2f && currentTimeSec < it.timelineEndSec - 0.2f }
                                        if (span != null) {
                                            saveStateForUndo()
                                            val active = span.clip
                                            val splitOffset = currentTimeSec - span.timelineStartSec
                                            val splitMediaSec = active.trimStartSec + splitOffset
                                            val firstHalf = active.copy(trimEndSec = splitMediaSec)
                                            val secondHalf = active.copy(
                                                id = UUID.randomUUID().toString(),
                                                name = "${active.name}_b",
                                                trimStartSec = splitMediaSec
                                            )
                                            val idx = clips.indexOfFirst { it.id == active.id }
                                            if (idx >= 0) {
                                                clips = clips.toMutableList().apply {
                                                    set(idx, firstHalf)
                                                    add(idx + 1, secondHalf)
                                                }
                                                selectedId = secondHalf.id
                                                ToastManager.showToast("Clip split at ${formatTimecode(currentTimeSec)}")
                                            }
                                        } else {
                                            ToastManager.showToast("Position playhead inside a clip to split")
                                        }
                                    },
                                    onDuplicateClip = {
                                        val active = selectedClip
                                        if (active != null) {
                                            saveStateForUndo()
                                            val duplicated = active.copy(id = UUID.randomUUID().toString(), name = "${active.name}_copy")
                                            val idx = clips.indexOfFirst { it.id == active.id }
                                            clips = clips.toMutableList().apply { add(idx + 1, duplicated) }
                                            selectedId = duplicated.id
                                            ToastManager.showToast("Clip duplicated")
                                        }
                                    },
                                    onDeleteClip = {
                                        if (clips.size > 1) {
                                            saveStateForUndo()
                                            clips = clips.filter { it.id != selectedId }
                                            selectedId = clips.first().id
                                            val span = timelineSpans.find { it.clip.id == selectedId }
                                            if (span != null) {
                                                seekToGlobalTime(span.timelineStartSec)
                                            }
                                            ToastManager.showToast("Clip removed")
                                        } else {
                                            ToastManager.showToast("Cannot delete the only clip")
                                        }
                                    },
                                    onAddClip = {
                                        videoPicker.launch("video/*")
                                    },
                                    onStepFrame = { forward ->
                                        val stepSec = 1f / 30f
                                        val nextSec = (currentTimeSec + (if (forward) stepSec else -stepSec)).coerceIn(0f, totalDurationSec)
                                        seekToGlobalTime(nextSec)
                                        SoundManager.playScrub(context, force = true)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    onEditTransition = { clipId ->
                                        SoundManager.playTap(context)
                                        editingTransitionClipId = clipId
                                    },
                                    customAudio = customAudio,
                                    onOpenAudioTab = {
                                        activeTab = FcpWorkspaceTab.AUDIO
                                        if (drawerLevel == VideoDrawerLevel.COLLAPSED) {
                                            drawerLevel = VideoDrawerLevel.MID
                                        }
                                    }
                                )
                            }
                            FcpWorkspaceTab.CANVAS -> {
                                CanvasTabContent(
                                    background = canvasBackground,
                                    onBackgroundChange = { canvasBackground = it },
                                    onPickImage = { bgImagePicker.launch("image/*") },
                                    blurBitmap = blurBackgroundBitmap,
                                    isDarkMode = colors.isDark
                                )
                            }
                            FcpWorkspaceTab.COLOR -> {
                                ColorTabContent(
                                    selectedClip = selectedClip,
                                    previewBitmap = blurBackgroundBitmap ?: selectedClip?.let { FcpThumbnailMemoryCache.get("${it.uri}_0") },
                                    onUpdateClip = { updated ->
                                        clips = clips.map { if (it.id == updated.id) updated else it }
                                    },
                                    onCommitClipChange = {
                                        saveStateForUndo()
                                    }
                                )
                            }
                            FcpWorkspaceTab.AUDIO -> {
                                AudioTabContent(
                                    gainDb = dialogueGainDb,
                                    onGainChange = { dialogueGainDb = it },
                                    voiceIsolation = voiceIsolation,
                                    onVoiceIsolationChange = { voiceIsolation = it },
                                    customAudio = customAudio,
                                    onAddAudioFile = {
                                        audioFilePicker.launch("audio/*")
                                    },
                                    onExtractAudioFromVideo = {
                                        videoExtractPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                    },
                                    onAudioVolumeChange = { newVol ->
                                        customAudio = customAudio?.copy(volume = newVol)
                                    },
                                    onMuteOriginalVideoChange = { mute ->
                                        customAudio = customAudio?.copy(muteOriginalVideo = mute)
                                        ToastManager.showToast(if (mute) "Original audio muted (replaced)" else "Original audio unmuted (mixed)")
                                    },
                                    onLoopAudioChange = { loop ->
                                        customAudio = customAudio?.copy(loopAudio = loop)
                                    },
                                    onRemoveCustomAudio = {
                                        customAudio = null
                                        ToastManager.showToast("Custom audio removed")
                                    },
                                    isPlaying = isPlaying,
                                    currentTimeSec = currentTimeSec
                                )
                            }
                            FcpWorkspaceTab.EFFECTS -> {
                                EffectsTabContent(
                                    aiMaskEnabled = aiMaskEnabled,
                                    onAiMaskToggle = { aiMaskEnabled = it },
                                    bokehAperture = bokehAperture,
                                    onBokehChange = { bokehAperture = it },
                                    speed = selectedClip?.speed ?: 1f,
                                    onSpeedChange = { newSpeed ->
                                        selectedClip?.let { cur ->
                                            saveStateForUndo()
                                            clips = clips.map { if (it.id == cur.id) it.copy(speed = newSpeed) else it }
                                        }
                                    }
                                )
                            }
                            FcpWorkspaceTab.TITLES -> {
                                TitlesTabContent(
                                    textLayers = textLayers,
                                    selectedLayerId = selectedTextLayerId,
                                    onSelectLayer = { selectedTextLayerId = it },
                                    onUpdateLayer = { updated ->
                                        textLayers = textLayers.map { if (it.id == updated.id) updated else it }
                                    },
                                    onAddLayer = { presetChoice ->
                                        saveStateForUndo()
                                        val chosenPreset = presetChoice ?: FcpTitlePresetType.SCRIPT_HEADLINE
                                        val newLayer = createFcpTextLayer(chosenPreset, textLayers.size)
                                        textLayers = textLayers + newLayer
                                        selectedTextLayerId = newLayer.id
                                        ToastManager.showToast("Title layer added")
                                    },
                                    onDeleteLayer = { idToDelete ->
                                        saveStateForUndo()
                                        val remaining = textLayers.filter { it.id != idToDelete }
                                        textLayers = remaining
                                        selectedTextLayerId = remaining.firstOrNull()?.id ?: ""
                                        ToastManager.showToast("Title layer deleted")
                                    },
                                    onDuplicateLayer = { idToDuplicate ->
                                        val src = textLayers.find { it.id == idToDuplicate }
                                        if (src != null) {
                                            saveStateForUndo()
                                            val dup = src.copy(
                                                id = UUID.randomUUID().toString(),
                                                name = "${src.name} Copy",
                                                offsetY = src.offsetY + 20f,
                                                isExpanded = true
                                            )
                                            val idx = textLayers.indexOfFirst { it.id == src.id }
                                            val mutable = textLayers.toMutableList()
                                            mutable.add(idx + 1, dup)
                                            textLayers = mutable
                                            selectedTextLayerId = dup.id
                                            ToastManager.showToast("Title layer duplicated")
                                        }
                                    },
                                    onMoveLayer = { fromIdx, toIdx ->
                                        if (fromIdx in textLayers.indices && toIdx in textLayers.indices && fromIdx != toIdx) {
                                            saveStateForUndo()
                                            val mutable = textLayers.toMutableList()
                                            val item = mutable.removeAt(fromIdx)
                                            mutable.add(toIdx, item)
                                            textLayers = mutable
                                            selectedTextLayerId = item.id
                                            ToastManager.showToast("Layer reordered")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Clip Transition Selection Dialog
        if (editingTransitionClipId != null) {
            val clip = clips.find { it.id == editingTransitionClipId }
            if (clip != null) {
                FcpTransitionDialog(
                    clip = clip,
                    onDismiss = { editingTransitionClipId = null },
                    onApplyTransition = { newType, newDur ->
                        saveStateForUndo()
                        clips = clips.map {
                            if (it.id == clip.id) it.copy(transitionToNext = newType, transitionDurationSec = newDur) else it
                        }
                        ToastManager.showToast("Transition set: ${newType.label}")
                    }
                )
            }
        }

        // Export & Render Sheet
        if (showExportSheet) {
            FcpExportSheet(
                totalDurationSec = totalDurationSec,
                clipCount = clips.size,
                textLayerCount = textLayers.size,
                aspectRatio = aspectRatio,
                customAudio = customAudio,
                isExporting = isExporting,
                exportProgress = exportProgress,
                onStartExport = { res: String, fps: String ->
                    scope.launch {
                        isExporting = true
                        exportProgress = 0f
                        for (p in 1..20) {
                            kotlinx.coroutines.delay(45L)
                            exportProgress = p / 20f
                        }
                        SoundManager.playDynamicIsland(context)
                        val active = selectedClip ?: clips.firstOrNull()
                        val startMs = ((active?.trimStartSec ?: 0f) * 1000f).toLong()
                        val endMs = ((active?.trimEndSec ?: (active?.durationSec ?: 15f)) * 1000f).toLong()
                        val uris = clips.map { it.uri }
                        try {
                            val draftRepo = DraftPostRepository(context)
                            val draft = CreatorPostDraft(
                                mode = "flash",
                                title = textLayers.firstOrNull()?.primaryText ?: "Chroma Studio Video",
                                description = "Rendered at $res $fps" + (if (customAudio != null) " with soundtrack '${customAudio?.name}'" else "") + " in ChromaStudio Final Cut Pro Editor",
                                aspectRatio = aspectRatio.label,
                                videoUri = clips.firstOrNull()?.uri?.toString(),
                                flashTrimStartMs = startMs,
                                flashTrimEndMs = endMs
                            )
                            draftRepo.saveDraft(draft)
                        } catch (_: Throwable) {}
                        isExporting = false
                        showExportSheet = false
                        ToastManager.showToast("Export completed! Sending to Studio...")
                        onDone(startMs, endMs, uris)
                    }
                },
                onSaveDraftOnly = {
                    try {
                        val active = selectedClip ?: clips.firstOrNull()
                        val startMs = ((active?.trimStartSec ?: 0f) * 1000f).toLong()
                        val endMs = ((active?.trimEndSec ?: (active?.durationSec ?: 15f)) * 1000f).toLong()
                        val draftRepo = DraftPostRepository(context)
                        val draft = CreatorPostDraft(
                            mode = "flash",
                            title = textLayers.firstOrNull()?.primaryText ?: "Draft Video Project",
                            description = "Saved ${clips.size} clips with ${textLayers.size} text layers" + (if (customAudio != null) " and soundtrack '${customAudio?.name}'" else ""),
                            aspectRatio = aspectRatio.label,
                            videoUri = clips.firstOrNull()?.uri?.toString(),
                            flashTrimStartMs = startMs,
                            flashTrimEndMs = endMs
                        )
                        draftRepo.saveDraft(draft)
                        SoundManager.playTap(context)
                        ToastManager.showToast("Project saved to Drafts")
                        showExportSheet = false
                    } catch (_: Throwable) {
                        ToastManager.showToast("Failed to save draft")
                    }
                },
                onDismiss = {
                    showExportSheet = false
                }
            )
        }

        // Exit Confirmation Dialog (Matching Trash Screen Dialog Style)
        if (showExitConfirmationDialog) {
            FcpExitConfirmationDialog(
                onSaveDraftAndExit = {
                    saveProjectDraft()
                    showExitConfirmationDialog = false
                    onBack()
                },
                onDiscardAndExit = {
                    showExitConfirmationDialog = false
                    onBack()
                },
                onDismiss = {
                    showExitConfirmationDialog = false
                }
            )
        }
    }
}

// ============================================================================
// CHROMA STUDIO WORKSPACE TAB SWITCHER (UiverseTabs Style)
// ============================================================================

@Composable
private fun VideoWorkspaceTabs(
    activeTab: FcpWorkspaceTab,
    onTabSelected: (FcpWorkspaceTab) -> Unit,
    isDarkMode: Boolean
) {
    val colors = LocalOscellionColors.current
    val allTabs = FcpWorkspaceTab.entries
    val activeIndex = allTabs.indexOf(activeTab)

    val containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF3F4F6)
    val indicatorColor = if (isDarkMode) Color(0xFF333333) else Color.White
    val activeIconTint = colors.primary
    val inactiveIconTint = if (isDarkMode) Color(0xFFA0A0A0) else Color(0xFF6B7280)

    val tabWidthDp = 52.dp
    val totalWidthDp = tabWidthDp * allTabs.size + 8.dp

    val indicatorOffsetAnim = remember { Animatable(activeIndex * 52f) }

    LaunchedEffect(activeIndex) {
        indicatorOffsetAnim.animateTo(
            targetValue = activeIndex * 52f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }

    Box(
        modifier = Modifier
            .size(width = totalWidthDp, height = 38.dp)
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(4.dp)
    ) {
        // Sliding indicator pill
        Box(
            modifier = Modifier
                .offset(x = indicatorOffsetAnim.value.dp)
                .width(tabWidthDp)
                .fillMaxHeight()
                .shadow(2.dp, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .background(indicatorColor)
        )

        // Tab items — ICONS ONLY
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            allTabs.forEach { tab ->
                val selected = tab == activeTab
                Box(
                    modifier = Modifier
                        .width(tabWidthDp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onTabSelected(tab)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) activeIconTint else inactiveIconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// TIMELINE THUMBNAIL ENGINE & FILMSTRIP PREVIEWS
// ============================================================================

object FcpThumbnailMemoryCache {
    private val cache = LruCache<String, Bitmap>(200)
    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bitmap: Bitmap) { cache.put(key, bitmap) }
}

@Composable
private fun FcpClipFilmstrip(
    clip: FcpClip,
    clipWidthDp: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val tileWidthDp = 48.dp
    val tileCount = (clipWidthDp / tileWidthDp).toInt().coerceIn(1, 40)
    val actualTileWidthDp = clipWidthDp / tileCount

    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(clip.uri, clip.trimStartSec, clip.trimEndSec, tileCount) {
        withContext(Dispatchers.IO) {
            var retriever: MediaMetadataRetriever? = null
            try {
                var opened = false
                for (i in 0 until tileCount) {
                    val fraction = (i + 0.5f) / tileCount
                    val sec = clip.trimStartSec + fraction * (clip.trimEndSec - clip.trimStartSec)
                    val timeUs = (sec * 1_000_000L).toLong()
                    val key = "${clip.uri}_${timeUs / 500_000L}"
                    if (FcpThumbnailMemoryCache.get(key) == null) {
                        if (!opened) {
                            retriever = MediaMetadataRetriever()
                            retriever.setDataSource(context, clip.uri)
                            opened = true
                        }
                        val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            retriever?.getScaledFrameAtTime(
                                timeUs,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                110,
                                130
                            )
                        } else {
                            retriever?.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        }
                        if (bmp != null) {
                            FcpThumbnailMemoryCache.put(key, bmp)
                            refreshTrigger++
                        }
                    }
                }
            } catch (_: Throwable) {
                // Ignore missing file or extraction failure
            } finally {
                try { retriever?.release() } catch (_: Throwable) {}
            }
        }
    }

    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until tileCount) {
            val fraction = (i + 0.5f) / tileCount
            val sec = clip.trimStartSec + fraction * (clip.trimEndSec - clip.trimStartSec)
            val timeUs = (sec * 1_000_000L).toLong()
            val key = "${clip.uri}_${timeUs / 500_000L}"
            val bmp = remember(key, refreshTrigger) { FcpThumbnailMemoryCache.get(key) }

            Box(
                modifier = Modifier
                    .width(actualTileWidthDp)
                    .fillMaxHeight()
                    .background(if (colors.isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)),
                contentAlignment = Alignment.Center
            ) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Lucide.Film,
                        contentDescription = null,
                        tint = if (colors.isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.12f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Frame divider line between filmstrip frames
                if (i < tileCount - 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}

// ============================================================================
// TIMELINE TAB CONTENT (Storyline, Handles, Transport, Jog Wheel)
// ============================================================================

@Composable
private fun TimelineTabContent(
    clips: List<FcpClip>,
    timelineSpans: List<FcpTimelineClipSpan>,
    selectedId: String?,
    currentTimeSec: Float,
    totalDurationSec: Float,
    pixelsPerSec: Float,
    onZoomChange: (Float) -> Unit = {},
    isPlaying: Boolean,
    isUserScrubbing: Boolean = false,
    isAutoReplay: Boolean = true,
    onAutoReplayToggle: () -> Unit = {},
    onPlayToggle: () -> Unit,
    onSelectClip: (String) -> Unit,
    onTrimLeft: (String, Float) -> Unit,
    onTrimRight: (String, Float) -> Unit,
    onTrimStart: () -> Unit = {},
    onReorderClips: (Int, Int) -> Unit = { _, _ -> },
    onScrubStart: () -> Unit = {},
    onScrubSec: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit = {},
    onSplitClip: () -> Unit,
    onDuplicateClip: () -> Unit,
    onDeleteClip: () -> Unit,
    onAddClip: () -> Unit,
    onStepFrame: (Boolean) -> Unit,
    onEditTransition: (String) -> Unit = {},
    customAudio: FcpCustomAudio? = null,
    onOpenAudioTab: () -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    val context = LocalContext.current
    val currentPlayheadTime by rememberUpdatedState(currentTimeSec)
    val currentClips by rememberUpdatedState(clips)
    val currentSpans by rememberUpdatedState(timelineSpans)
    var draggingClipId by remember { mutableStateOf<String?>(null) }
    var dragCumulativeOffsetPx by remember { mutableFloatStateOf(0f) }

    val timelineScrollState = rememberScrollState()
    val timelineStartPaddingDp = 16.dp
    val timelineEndPaddingDp = 16.dp
    val totalContentWidthDp = (totalDurationSec * pixelsPerSec).dp + timelineStartPaddingDp + timelineEndPaddingDp

    var viewportWidthPx by remember { mutableIntStateOf(0) }

    // Auto-scroll to follow playhead smoothly during playback and guarantee it never gets lost
    LaunchedEffect(currentTimeSec, isPlaying) {
        val playheadPx = (timelineStartPaddingDp.value * density) + (currentTimeSec * pixelsPerSec * density)
        val scrollOffset = timelineScrollState.value
        val rightThreshold = scrollOffset + viewportWidthPx - (70 * density)
        val leftThreshold = scrollOffset + (30 * density)

        if (currentTimeSec <= 0.05f && isPlaying) {
            // When at or reset to start, smoothly scroll timeline back to start
            timelineScrollState.animateScrollTo(0, animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing))
        } else if (isPlaying && viewportWidthPx > 0) {
            if (playheadPx > rightThreshold) {
                val targetScroll = (playheadPx - viewportWidthPx * 0.38f).coerceAtLeast(0f).toInt()
                timelineScrollState.animateScrollTo(targetScroll, animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing))
            } else if (playheadPx < leftThreshold) {
                val targetScroll = (playheadPx - 30 * density).coerceAtLeast(0f).toInt()
                timelineScrollState.animateScrollTo(targetScroll, animationSpec = tween(durationMillis = 250, easing = LinearOutSlowInEasing))
            }
        } else if (!isPlaying && viewportWidthPx > 0) {
            val isAtEnd = totalDurationSec > 0f && currentTimeSec >= totalDurationSec - 0.08f
            if (isAtEnd) {
                // When stopped at the end of the video, ensure the playhead at the end line is visible!
                val targetScroll = (playheadPx - viewportWidthPx + timelineEndPaddingDp.value * density + 16 * density).coerceAtLeast(0f).toInt()
                timelineScrollState.animateScrollTo(targetScroll)
            } else if (currentTimeSec <= 0.05f) {
                timelineScrollState.animateScrollTo(0)
            } else if (!isUserScrubbing && (playheadPx < scrollOffset || playheadPx > scrollOffset + viewportWidthPx)) {
                val targetScroll = (playheadPx - viewportWidthPx * 0.35f).coerceAtLeast(0f).toInt()
                timelineScrollState.scrollTo(targetScroll)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // ── Top Row: Timecode + Zoom + Transport Actions ─────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timecode display & Zoom Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.primary.copy(alpha = if (isUserScrubbing) 0.22f else 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = formatTimecode(currentTimeSec),
                        color = colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "/ ${formatTimecode(totalDurationSec)}",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Transport Action Buttons & Zoom Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimelineMiniButton(Lucide.ZoomOut, "Zoom Out", onClick = { onZoomChange((pixelsPerSec * 0.8f).coerceIn(15f, 120f)) })
                TimelineMiniButton(Lucide.ZoomIn, "Zoom In", onClick = { onZoomChange((pixelsPerSec * 1.25f).coerceIn(15f, 120f)) })
                TimelineMiniButton(Lucide.SkipBack, "Prev Frame", onClick = { onStepFrame(false) })
                TimelineMiniButton(Lucide.Scissors, "Split", onClick = onSplitClip)
                TimelineMiniButton(Lucide.Sparkles, "Transition", onClick = {
                    if (timelineSpans.size < 2) {
                        ToastManager.showToast("Add 2 or more videos to add transitions")
                    } else {
                        val targetClipId = if (selectedId != null && selectedId != timelineSpans.last().clip.id) {
                            selectedId
                        } else {
                            timelineSpans[timelineSpans.size - 2].clip.id
                        }
                        onEditTransition(targetClipId)
                    }
                })
                TimelineMiniButton(Lucide.Copy, "Duplicate", onClick = onDuplicateClip)
                TimelineMiniButton(Lucide.Trash2, "Delete", isDanger = true, onClick = onDeleteClip)
                TimelineMiniButton(Lucide.Plus, "Add Video", onClick = onAddClip)
                TimelineMiniButton(Lucide.Music, "Audio", onClick = onOpenAudioTab)
                TimelineMiniButton(Lucide.SkipForward, "Next Frame", onClick = { onStepFrame(true) })
            }
        }

        // ── Middle: Flat Style Unified Timeline (Synchronized ruler & clips) ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (customAudio != null) 112.dp else 86.dp)
                .onSizeChanged { viewportWidthPx = it.width }
                .horizontalScroll(timelineScrollState)
        ) {
            Box(
                modifier = Modifier
                    .width(totalContentWidthDp)
                    .fillMaxHeight()
            ) {
                // 1. Time Ruler (Draggable & Tappable to scrub playhead anywhere)
                val majorIntervalSec = when {
                    pixelsPerSec >= 75f -> 2
                    pixelsPerSec >= 35f -> 5
                    else -> 10
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .pointerInput(totalDurationSec, pixelsPerSec, density) {
                            var dragSec = 0f
                            detectDragGestures(
                                onDragStart = { offset ->
                                    onScrubStart()
                                    val startPx = timelineStartPaddingDp.toPx()
                                    dragSec = ((offset.x - startPx) / (pixelsPerSec * density)).coerceIn(0f, totalDurationSec)
                                    onScrubSec(dragSec)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val deltaSec = dragAmount.x / (pixelsPerSec * density)
                                    dragSec = (dragSec + deltaSec).coerceIn(0f, totalDurationSec)
                                    onScrubSec(dragSec)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onDragEnd = {
                                    onScrubEnd(dragSec)
                                },
                                onDragCancel = {
                                    onScrubEnd(dragSec)
                                }
                            )
                        }
                        .pointerInput(totalDurationSec, pixelsPerSec, density) {
                            detectTapGestures { offset ->
                                val startPx = timelineStartPaddingDp.toPx()
                                val tappedSec = ((offset.x - startPx) / (pixelsPerSec * density)).coerceIn(0f, totalDurationSec)
                                onScrubStart()
                                onScrubSec(tappedSec)
                                onScrubEnd(tappedSec)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val startPx = timelineStartPaddingDp.toPx()
                        val pxPerSec = pixelsPerSec * density
                        val totalSecs = totalDurationSec.toInt()

                        for (sec in 0..totalSecs) {
                            val x = startPx + (sec * pxPerSec)
                            val isMajor = sec % majorIntervalSec == 0
                            val isMedium = !isMajor && (sec % (majorIntervalSec / 2).coerceAtLeast(1) == 0)

                            drawLine(
                                color = when {
                                    isMajor -> colors.textMuted.copy(alpha = 0.75f)
                                    isMedium -> colors.textMuted.copy(alpha = 0.45f)
                                    else -> colors.glassBorder.copy(alpha = 0.35f)
                                },
                                start = Offset(x, 0f),
                                end = Offset(x, if (isMajor) 11.dp.toPx() else if (isMedium) 7.dp.toPx() else 4.dp.toPx()),
                                strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                            )
                        }

                        // Video End Boundary Line on ruler
                        val endX = startPx + (totalDurationSec * pxPerSec)
                        drawLine(
                            color = Color(0xFFEF4444).copy(alpha = 0.85f),
                            start = Offset(endX, 0f),
                            end = Offset(endX, 18.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Native timecode numbers on major intervals
                    val totalSecs = totalDurationSec.toInt()
                    for (sec in majorIntervalSec..totalSecs step majorIntervalSec) {
                        val secOffsetDp = timelineStartPaddingDp + (sec * pixelsPerSec).dp
                        Text(
                            text = formatTimecode(sec.toFloat()),
                            color = colors.textMuted.copy(alpha = 0.65f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.offset(x = secOffsetDp + 2.dp, y = 2.dp)
                        )
                    }
                }

                // Video End Boundary Vertical Guide extending through clips
                Box(
                    modifier = Modifier
                        .offset(x = timelineStartPaddingDp + (totalDurationSec * pixelsPerSec).dp, y = 18.dp)
                        .width(1.5.dp)
                        .height(if (customAudio != null) 84.dp else 56.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.4f))
                )

                // 2. Video Clips Track Lane
                Row(
                    modifier = Modifier
                        .offset(x = timelineStartPaddingDp, y = 18.dp)
                        .height(56.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    timelineSpans.forEachIndexed { i, span ->
                        val clip = span.clip
                        key(clip.id) {
                            val isSelected = clip.id == selectedId
                            val isClipDragging = draggingClipId == clip.id
                            val clipWidthDp = (span.durationSec * pixelsPerSec).dp

                            val clipBg = if (isSelected) {
                                if (colors.isDark) Color(0xFF1D4ED8) else Color(0xFF2563EB)
                            } else {
                                if (colors.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                            }
                            val clipBorderColor = if (isClipDragging) Color(0xFF38BDF8) else if (isSelected) Color(0xFF60A5FA) else colors.glassBorder
                            val textColor = if (isSelected) Color.White else colors.textMain
                            val subTextColor = if (isSelected) Color.White.copy(alpha = 0.8f) else colors.textMuted

                            val dragOffsetX = if (isClipDragging) dragCumulativeOffsetPx else 0f

                            Box(
                                modifier = Modifier
                                    .width(clipWidthDp)
                                    .fillMaxHeight()
                                    .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                                    .scale(if (isClipDragging) 1.05f else 1f)
                                    .shadow(
                                        elevation = if (isClipDragging) 16.dp else if (isSelected) 2.dp else 0.dp,
                                        shape = RoundedCornerShape(6.dp),
                                        spotColor = Color(0xFF60A5FA)
                                    )
                                    .zIndex(if (isClipDragging) 100f else 1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(clipBg)
                                    .border(
                                        width = if (isClipDragging) 2.dp else if (isSelected) 1.5.dp else 0.5.dp,
                                        color = clipBorderColor,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .pointerInput(clip.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingClipId = clip.id
                                                dragCumulativeOffsetPx = 0f
                                                onSelectClip(clip.id)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDrag = { change, dragAmount ->
                                                val activeId = draggingClipId ?: clip.id
                                                val currentList = currentClips
                                                val currentIdx = currentList.indexOfFirst { it.id == activeId }
                                                if (currentIdx != -1 && currentList.size > 1) {
                                                    change.consume()
                                                    dragCumulativeOffsetPx += dragAmount.x

                                                    // Swap Right
                                                    if (currentIdx < currentList.size - 1) {
                                                        val nextSpan = currentSpans.getOrNull(currentIdx + 1)
                                                        val nextWidthPx = ((nextSpan?.durationSec ?: 1f) * pixelsPerSec * density).coerceAtLeast(20f * density)
                                                        val swapThreshold = (nextWidthPx * 0.45f).coerceIn(24f * density, 140f * density)
                                                        if (dragCumulativeOffsetPx > swapThreshold) {
                                                            onReorderClips(currentIdx, currentIdx + 1)
                                                            dragCumulativeOffsetPx -= nextWidthPx
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    }

                                                    // Swap Left
                                                    if (currentIdx > 0) {
                                                        val prevSpan = currentSpans.getOrNull(currentIdx - 1)
                                                        val prevWidthPx = ((prevSpan?.durationSec ?: 1f) * pixelsPerSec * density).coerceAtLeast(20f * density)
                                                        val swapThreshold = (prevWidthPx * 0.45f).coerceIn(24f * density, 140f * density)
                                                        if (dragCumulativeOffsetPx < -swapThreshold) {
                                                            onReorderClips(currentIdx, currentIdx - 1)
                                                            dragCumulativeOffsetPx += prevWidthPx
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                draggingClipId = null
                                                dragCumulativeOffsetPx = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            },
                                            onDragCancel = {
                                                draggingClipId = null
                                                dragCumulativeOffsetPx = 0f
                                            }
                                        )
                                    }
                                    .pointerInput(clip.id, span.timelineStartSec, span.durationSec, pixelsPerSec, density) {
                                        detectTapGestures(
                                            onTap = { offset ->
                                                onSelectClip(clip.id)
                                                val tappedSec = (span.timelineStartSec + (offset.x / (pixelsPerSec * density))).coerceIn(0f, totalDurationSec)
                                                onScrubStart()
                                                onScrubSec(tappedSec)
                                                onScrubEnd(tappedSec)
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        )
                                    }
                            ) {
                                // 1. Live Video Preview Thumbnails Filmstrip
                                FcpClipFilmstrip(
                                    clip = clip,
                                    clipWidthDp = clipWidthDp,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // 2. Cinematic Gradient Scrim (Keeps labels, badges, and trim handles high-contrast & sharp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = if (isSelected) {
                                                    listOf(
                                                        Color.Black.copy(alpha = 0.52f),
                                                        Color.Black.copy(alpha = 0.08f),
                                                        Color.Black.copy(alpha = 0.62f)
                                                    )
                                                } else {
                                                    listOf(
                                                        Color.Black.copy(alpha = 0.65f),
                                                        Color.Black.copy(alpha = 0.35f),
                                                        Color.Black.copy(alpha = 0.75f)
                                                    )
                                                }
                                            )
                                        )
                                )

                                // Floating DRAGGING badge when actively dragged
                                if (isClipDragging) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .offset(y = (-14).dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF007AFF))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .zIndex(150f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Lucide.Move,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(9.dp)
                                            )
                                            Text(
                                                text = "DRAGGING",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                val handleWidth = if (clipWidthDp < 36.dp) 10.dp else 14.dp
                                val touchHandleWidth = 36.dp

                                // Left Trim Handle (Amber bracket on active clip)
                                if (isSelected && !isClipDragging) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .width(touchHandleWidth)
                                            .fillMaxHeight()
                                            .zIndex(25f)
                                            .pointerInput(clip.id, density, pixelsPerSec) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        onScrubStart()
                                                        onTrimStart()
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val deltaSec = dragAmount.x / (pixelsPerSec * density)
                                                        onTrimLeft(clip.id, deltaSec)
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    },
                                                    onDragEnd = {
                                                        onScrubEnd(currentPlayheadTime)
                                                    },
                                                    onDragCancel = {
                                                        onScrubEnd(currentPlayheadTime)
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(handleWidth)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                                                .background(Color(0xFFF59E0B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                                Box(Modifier.width(1.dp).height(14.dp).background(Color.Black.copy(alpha = 0.6f)))
                                                Box(Modifier.width(1.dp).height(14.dp).background(Color.Black.copy(alpha = 0.6f)))
                                            }
                                        }
                                    }
                                }

                                // Clip Information
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            start = if (isSelected) (handleWidth + 6.dp) else 8.dp,
                                            end = if (isSelected) (handleWidth + 6.dp) else 8.dp,
                                            top = 4.dp,
                                            bottom = 4.dp
                                        ),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(if (isSelected) Color.White.copy(alpha = 0.22f) else colors.primary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "V1",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else colors.primary
                                                )
                                            }
                                            Text(
                                                text = clip.name,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Tactile grip affordance (indicates hold & drag)
                                        Icon(
                                            imageVector = Lucide.GripVertical,
                                            contentDescription = "Hold to drag",
                                            tint = if (isSelected) Color.White.copy(alpha = 0.8f) else colors.textMuted.copy(alpha = 0.5f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${String.format("%.1f", clip.trimmedDurationSec)}s",
                                            fontSize = 9.sp,
                                            color = subTextColor,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (clip.speed != 1f) {
                                            Text(
                                                text = "${clip.speed}x",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFFFDE047) else colors.primary
                                            )
                                        }
                                    }
                                }

                                // Right Trim Handle (Amber bracket on active clip)
                                if (isSelected && !isClipDragging) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(touchHandleWidth)
                                            .fillMaxHeight()
                                            .zIndex(25f)
                                            .pointerInput(clip.id, density, pixelsPerSec) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        onScrubStart()
                                                        onTrimStart()
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val deltaSec = dragAmount.x / (pixelsPerSec * density)
                                                        onTrimRight(clip.id, deltaSec)
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    },
                                                    onDragEnd = {
                                                        onScrubEnd(currentPlayheadTime)
                                                    },
                                                    onDragCancel = {
                                                        onScrubEnd(currentPlayheadTime)
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(handleWidth)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                                .background(Color(0xFFF59E0B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                                Box(Modifier.width(1.dp).height(14.dp).background(Color.Black.copy(alpha = 0.6f)))
                                                Box(Modifier.width(1.dp).height(14.dp).background(Color.Black.copy(alpha = 0.6f)))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2a. Interactive Transition Badges Centered on Cut Boundaries
                if (timelineSpans.size > 1) {
                    timelineSpans.take(timelineSpans.size - 1).forEach { span ->
                        val clip = span.clip
                        val trans = clip.transitionToNext
                        val hasTrans = trans != FcpTransitionType.NONE
                        val cutX = timelineStartPaddingDp + (span.timelineEndSec * pixelsPerSec).dp

                        Box(
                            modifier = Modifier
                                .offset(x = cutX - 13.dp, y = 18.dp + 15.dp)
                                .size(26.dp)
                                .zIndex(35f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (hasTrans) colors.primary
                                    else if (colors.isDark) Color(0xFF222734) else Color(0xFFE2E8F0)
                                )
                                .border(
                                    1.dp,
                                    if (hasTrans) Color.White.copy(alpha = 0.55f) else colors.glassBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onEditTransition(clip.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = trans.icon,
                                contentDescription = trans.label,
                                tint = if (hasTrans) Color.White else colors.textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (hasTrans) {
                            Box(
                                modifier = Modifier
                                    .offset(x = cutX - 20.dp, y = 18.dp + 43.dp)
                                    .width(40.dp)
                                    .zIndex(36f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${"%.1f".format(clip.transitionDurationSec)}s",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // 2b. Custom Soundtrack Lane (Positioned directly under Video Clips Track)
                if (customAudio != null) {
                    val audioDuration = if (customAudio.durationSec > 0f) customAudio.durationSec else totalDurationSec
                    val audioTrackWidthDp = (audioDuration * pixelsPerSec).dp
                    Box(
                        modifier = Modifier
                            .offset(x = timelineStartPaddingDp, y = 78.dp)
                            .width(audioTrackWidthDp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF064E3B).copy(alpha = 0.85f),
                                        Color(0xFF047857).copy(alpha = 0.65f)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .clickable { onOpenAudioTab() }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Stylized Waveform Bars background
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = 2.dp.toPx()
                            val barGap = 2.dp.toPx()
                            val totalBarStep = barWidth + barGap
                            val count = (size.width / totalBarStep).toInt()
                            val centerY = size.height / 2f
                            val maxBarHeight = size.height * 0.7f

                            for (i in 0 until count) {
                                val factor = ((Math.sin(i * 0.45) * 0.45 + Math.cos(i * 0.22) * 0.35 + 0.8).toFloat()).coerceIn(0.2f, 1.0f)
                                val barH = maxBarHeight * factor
                                val x = i * totalBarStep + barGap
                                drawRoundRect(
                                    color = Color(0xFF34D399).copy(alpha = 0.25f),
                                    topLeft = Offset(x, centerY - barH / 2f),
                                    size = Size(barWidth, barH),
                                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = if (customAudio.isExtractedFromVideo) Lucide.Film else Lucide.Music,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = customAudio.name,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (customAudio.muteOriginalVideo) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "Muted Video",
                                            fontSize = 7.5.sp,
                                            color = Color(0xFF34D399),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "${(customAudio.volume * 100f).toInt()}%",
                                    fontSize = 8.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // 3. Apple-Style Draggable Playhead Marker & Needle
                val playheadCenterX = timelineStartPaddingDp + (currentTimeSec * pixelsPerSec).dp
                val touchWidth = 48.dp

                Box(
                    modifier = Modifier
                        .offset(x = playheadCenterX - (touchWidth / 2))
                        .width(touchWidth)
                        .fillMaxHeight()
                        .zIndex(50f)
                        .pointerInput(totalDurationSec, pixelsPerSec, density) {
                            var dragSec = 0f
                            detectDragGestures(
                                onDragStart = {
                                    onScrubStart()
                                    dragSec = currentPlayheadTime
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val deltaSec = dragAmount.x / (pixelsPerSec * density)
                                    dragSec = (dragSec + deltaSec).coerceIn(0f, totalDurationSec)
                                    onScrubSec(dragSec)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onDragEnd = {
                                    onScrubEnd(dragSec)
                                },
                                onDragCancel = {
                                    onScrubEnd(dragSec)
                                }
                            )
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    val pinScale by animateFloatAsState(
                        targetValue = if (isUserScrubbing) 1.25f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "playheadPinScale"
                    )
                    // Top Marker Pin Handle
                    Box(
                        modifier = Modifier
                            .scale(pinScale)
                            .width(16.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(Color(0xFFEF4444))
                            .shadow(
                                elevation = if (isUserScrubbing) 8.dp else 4.dp,
                                shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp),
                                spotColor = Color(0xFFEF4444)
                            )
                    )
                    // Razor-sharp 2.5dp playhead line extending all the way down
                    Box(
                        modifier = Modifier
                            .width(if (isUserScrubbing) 3.dp else 2.5.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFEF4444))
                    )
                }
            }
        }

        // ── Bottom: Tactile Rotary Jog Wheel for buttery smooth frame scrubbing ─────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tactile Rotary Jog Wheel for buttery smooth frame scrubbing
            var isJogActive by remember { mutableStateOf(false) }
            var jogScrollOffsetPx by remember { mutableFloatStateOf(0f) }
            val jogDragTime by rememberUpdatedState(currentTimeSec)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (isJogActive) colors.primary.copy(alpha = 0.08f) else colors.bg
                    )
                    .border(
                        width = if (isJogActive) 1.5.dp else 1.dp,
                        color = if (isJogActive) colors.primary.copy(alpha = 0.7f) else colors.glassBorder,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .pointerInput(totalDurationSec) {
                        var jogTime = 0f
                        var lastHapticFrame = 0
                        val tickSpacingPx = 8.dp.toPx()
                        detectDragGestures(
                            onDragStart = {
                                isJogActive = true
                                onScrubStart()
                                jogTime = jogDragTime
                                lastHapticFrame = (jogTime * 30f).roundToInt()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                jogScrollOffsetPx += dragAmount.x
                                // 1 frame per tickSpacingPx (8dp)
                                val frameStep = (dragAmount.x / tickSpacingPx) * (1f / 30f)
                                jogTime = (jogTime + frameStep).coerceIn(0f, totalDurationSec)
                                onScrubSec(jogTime)

                                val curFrame = (jogTime * 30f).roundToInt()
                                if (curFrame != lastHapticFrame) {
                                    lastHapticFrame = curFrame
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    SoundManager.playScrub(context, force = true)
                                }
                            },
                            onDragEnd = {
                                isJogActive = false
                                onScrubEnd(jogTime)
                            },
                            onDragCancel = {
                                isJogActive = false
                                onScrubEnd(jogTime)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                ) {
                    val cx = size.width / 2f
                    val tickSpacing = 8.dp.toPx()
                    val offset = jogScrollOffsetPx % tickSpacing
                    val totalTicks = (size.width / tickSpacing).toInt() + 4
                    val halfTicks = totalTicks / 2
                    val currentBaseFrame = (jogScrollOffsetPx / tickSpacing).roundToInt()

                    for (i in -halfTicks..halfTicks) {
                        val x = cx + (i * tickSpacing) + offset
                        if (x < 0f || x > size.width) continue

                        val dist = abs(x - cx)
                        val maxDist = size.width / 2f
                        val normDist = (dist / maxDist).coerceIn(0f, 1f)
                        val alpha = cos(normDist * (Math.PI.toFloat() / 2f)).coerceIn(0f, 1f)

                        val frameIndex = currentBaseFrame - i
                        val isMajor = frameIndex % 10 == 0
                        val isMedium = frameIndex % 5 == 0

                        val tickHeight = when {
                            isMajor -> size.height * 0.62f
                            isMedium -> size.height * 0.42f
                            else -> size.height * 0.25f
                        }
                        val yTop = (size.height - tickHeight) / 2f

                        val strokeW = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                        val color = when {
                            isMajor -> colors.primary.copy(alpha = alpha * 0.95f)
                            isMedium -> colors.textMuted.copy(alpha = alpha * 0.75f)
                            else -> colors.textMuted.copy(alpha = alpha * 0.35f)
                        }

                        drawLine(
                            color = color,
                            start = Offset(x, yTop),
                            end = Offset(x, yTop + tickHeight),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }

                    // Center Optical Needle Indicator
                    drawLine(
                        color = Color(0xFFEF4444),
                        start = Offset(cx, 5.dp.toPx()),
                        end = Offset(cx, size.height - 5.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Interactive Center HUD label
                val currentFrame = (currentTimeSec * 30f).roundToInt()
                if (isJogActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.78f))
                            .border(0.5.dp, colors.primary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "◀  FRAME $currentFrame  ▶",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            letterSpacing = 0.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Text(
                        text = "◀  SCRUB FRAME  ▶",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted.copy(alpha = 0.65f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// COLOR TAB CONTENT (Exposure, Contrast, Saturation, Temperature, Tint, LUTs)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FcpColorSliderRow(
    title: String,
    icon: ImageVector,
    value: Float,
    defaultValue: Float = 0f,
    valueRange: ClosedFloatingPointRange<Float>,
    isBipolar: Boolean = false,
    formatValue: (Float) -> String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    gradientColors: List<Color>? = null
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val isModified = abs(value - defaultValue) > 0.05f

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Label + Value + Reset
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isModified) colors.primary else colors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = colors.textMain,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isModified) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onValueChange(defaultValue)
                                onValueChangeFinished?.invoke()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.RotateCcw,
                            contentDescription = "Reset $title",
                            tint = colors.textMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isModified) colors.primary.copy(alpha = 0.12f)
                            else if (colors.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (isModified) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onValueChange(defaultValue)
                                onValueChangeFinished?.invoke()
                            }
                        }
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatValue(value),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isModified) colors.primary else colors.textMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Tactile Precision Slider
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val isDragged by interactionSource.collectIsDraggedAsState()
        val isInteracting = isPressed || isDragged

        val thumbWidth by animateDpAsState(
            targetValue = if (isInteracting) 28.dp else 24.dp,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
            label = "tw"
        )
        val thumbScale by animateFloatAsState(
            targetValue = if (isInteracting) 1.15f else 1.0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
            label = "ts"
        )

        val trackBg = if (colors.isDark) Color(0xFF272A34) else Color(0xFFE5E5EA)
        val strokeColor = colors.glassBorder
        val gradColors = gradientColors ?: listOf(colors.primary, Color(0xFF38BDF8))

        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = { newVal ->
                val span = valueRange.endInclusive - valueRange.start
                val snapZone = span * 0.035f
                val finalVal = if (abs(newVal - defaultValue) < snapZone) {
                    if (value != defaultValue) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    defaultValue
                } else newVal
                onValueChange(finalVal)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            colors = SliderDefaults.colors(
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                thumbColor = Color.Transparent
            ),
            track = { sliderState ->
                val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
                val fraction = if (range > 0f) {
                    ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
                } else 0f
                val defaultFraction = if (range > 0f) {
                    ((defaultValue - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
                } else 0.5f

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.5.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val cornerRadius = CornerRadius(h / 2f, h / 2f)

                    // Track base
                    drawRoundRect(
                        color = trackBg,
                        size = size,
                        cornerRadius = cornerRadius
                    )

                    // Active fill
                    if (isBipolar) {
                        val centerPx = defaultFraction * w
                        val currentPx = fraction * w
                        val left = minOf(centerPx, currentPx)
                        val right = maxOf(centerPx, currentPx)
                        val fillWidth = right - left
                        if (fillWidth > 1f) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(gradColors, startX = left, endX = right),
                                topLeft = Offset(left, 0f),
                                size = Size(fillWidth, h),
                                cornerRadius = cornerRadius
                            )
                        }
                        // Center tick
                        drawLine(
                            color = colors.textMuted.copy(alpha = 0.75f),
                            start = Offset(centerPx, -2f),
                            end = Offset(centerPx, h + 2f),
                            strokeWidth = 2f
                        )
                    } else {
                        val fillWidth = fraction * w
                        if (fillWidth > 1f) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(gradColors, startX = 0f, endX = fillWidth),
                                size = Size(fillWidth, h),
                                cornerRadius = cornerRadius
                            )
                        }
                        if (defaultValue > sliderState.valueRange.start) {
                            val centerPx = defaultFraction * w
                            drawLine(
                                color = colors.textMuted.copy(alpha = 0.75f),
                                start = Offset(centerPx, -2f),
                                end = Offset(centerPx, h + 2f),
                                strokeWidth = 2f
                            )
                        }
                    }

                    // Border outline
                    drawRoundRect(
                        color = strokeColor,
                        size = size,
                        cornerRadius = cornerRadius,
                        style = Stroke(width = 0.5.dp.toPx())
                    )
                }
            },
            thumb = {
                LiquidGlassKnob(
                    width = thumbWidth,
                    height = 18.dp,
                    scale = thumbScale,
                    baseAlpha = 1.0f
                )
            }
        )
    }
}

@Composable
private fun FcpChromaSliderRow(
    title: String,
    icon: ImageVector,
    value: Float,
    defaultValue: Float = 0f,
    valueRange: ClosedFloatingPointRange<Float>,
    isBipolar: Boolean = false,
    formatValue: (Float) -> String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    gradientColors: List<Color>? = null
) = FcpColorSliderRow(
    title = title,
    icon = icon,
    value = value,
    defaultValue = defaultValue,
    valueRange = valueRange,
    isBipolar = isBipolar,
    formatValue = formatValue,
    onValueChange = onValueChange,
    onValueChangeFinished = onValueChangeFinished,
    gradientColors = gradientColors
)

@Composable
private fun ColorTabContent(
    selectedClip: FcpClip?,
    previewBitmap: Bitmap? = null,
    onUpdateClip: (FcpClip) -> Unit,
    onCommitClipChange: () -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    if (selectedClip == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a clip to grade color", color = colors.textMuted, fontSize = 12.sp)
        }
        return
    }

    val isAnyModified = abs(selectedClip.exposure) > 0.05f ||
            abs(selectedClip.contrast - 100f) > 0.05f ||
            abs(selectedClip.saturation - 100f) > 0.05f ||
            abs(selectedClip.temperature) > 0.05f ||
            abs(selectedClip.tint) > 0.05f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sliders Section
        item {
            WorkspaceSectionCard(
                title = "COLOR CORRECTION",
                icon = Lucide.Palette,
                trailingContent = {
                    if (isAnyModified) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.primary.copy(alpha = 0.12f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateClip(
                                        selectedClip.copy(
                                            exposure = 0f,
                                            contrast = 100f,
                                            saturation = 100f,
                                            temperature = 0f,
                                            tint = 0f
                                        )
                                    )
                                    onCommitClipChange()
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Reset All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }
                    }
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Exposure (Sunlight Lux Multi-stop Gradient)
                    FcpColorSliderRow(
                        title = "Exposure",
                        icon = Lucide.Sun,
                        value = selectedClip.exposure,
                        defaultValue = 0f,
                        valueRange = -50f..50f,
                        isBipolar = true,
                        formatValue = { v ->
                            val iv = v.toInt()
                            if (iv > 0) "+$iv%" else if (iv < 0) "$iv%" else "0%"
                        },
                        onValueChange = { onUpdateClip(selectedClip.copy(exposure = it)) },
                        onValueChangeFinished = onCommitClipChange,
                        gradientColors = listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFFEF08A))
                    )

                    // Contrast (Deep Indigo / Violet Multi-stop Gradient)
                    FcpColorSliderRow(
                        title = "Contrast",
                        icon = Lucide.Contrast,
                        value = selectedClip.contrast,
                        defaultValue = 100f,
                        valueRange = 50f..150f,
                        isBipolar = false,
                        formatValue = { "${it.toInt()}%" },
                        onValueChange = { onUpdateClip(selectedClip.copy(contrast = it)) },
                        onValueChangeFinished = onCommitClipChange,
                        gradientColors = listOf(Color(0xFF4338CA), Color(0xFF6366F1), Color(0xFFA5B4FC))
                    )

                    // Saturation (Chromatic Spectrum Multi-stop Gradient)
                    FcpColorSliderRow(
                        title = "Saturation",
                        icon = Lucide.Droplet,
                        value = selectedClip.saturation,
                        defaultValue = 100f,
                        valueRange = 0f..200f,
                        isBipolar = false,
                        formatValue = { "${it.toInt()}%" },
                        onValueChange = { onUpdateClip(selectedClip.copy(saturation = it)) },
                        onValueChangeFinished = onCommitClipChange,
                        gradientColors = listOf(Color(0xFF64748B), Color(0xFF06B6D4), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFF43F5E))
                    )

                    // Temperature (Bipolar Arctic Blue to Sunset Amber)
                    FcpColorSliderRow(
                        title = "Temperature",
                        icon = Lucide.SlidersHorizontal,
                        value = selectedClip.temperature,
                        defaultValue = 0f,
                        valueRange = -50f..50f,
                        isBipolar = true,
                        formatValue = { v ->
                            val iv = v.toInt()
                            if (iv > 0) "+$iv Warm" else if (iv < 0) "$iv Cool" else "0 (Neutral)"
                        },
                        onValueChange = { onUpdateClip(selectedClip.copy(temperature = it)) },
                        onValueChangeFinished = onCommitClipChange,
                        gradientColors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFF8FAFC), Color(0xFFF59E0B), Color(0xFFEA580C))
                    )

                    // Tint (Bipolar Mint Emerald to Vivid Magenta)
                    FcpColorSliderRow(
                        title = "Tint",
                        icon = Lucide.Sparkles,
                        value = selectedClip.tint,
                        defaultValue = 0f,
                        valueRange = -50f..50f,
                        isBipolar = true,
                        formatValue = { v ->
                            val iv = v.toInt()
                            if (iv > 0) "+$iv Magenta" else if (iv < 0) "$iv Green" else "0 (Neutral)"
                        },
                        onValueChange = { onUpdateClip(selectedClip.copy(tint = it)) },
                        onValueChangeFinished = onCommitClipChange,
                        gradientColors = listOf(Color(0xFF059669), Color(0xFF34D399), Color(0xFFF8FAFC), Color(0xFFE879F9), Color(0xFFC026D3))
                    )
                }
            }
        }

        // Apple LUT Presets Grid with Real Video Frame Previews
        item {
            WorkspaceSectionCard(title = "CINEMATIC LUT PRESETS", icon = Lucide.Sparkles) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(176.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FcpLutPreset.entries) { preset ->
                        val isSelected = selectedClip.lut == preset
                        val presetMatrix = remember(preset) {
                            val gm = buildFcpColorMatrix(0f, 100f, 100f, 0f, 0f, preset)
                            androidx.compose.ui.graphics.ColorMatrix(gm.array)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .shadow(if (isSelected) 4.dp else 1.dp, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) colors.primary else colors.glassBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onUpdateClip(selectedClip.copy(lut = preset))
                                    onCommitClipChange()
                                    ToastManager.showToast("LUT: ${preset.label}")
                                }
                        ) {
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = preset.label,
                                    contentScale = ContentScale.Crop,
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(presetMatrix),
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Gradient scrim for high legibility
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color(0x44000000),
                                                    Color(0xCC000000)
                                                )
                                            )
                                        )
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(preset.colors))
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(horizontal = 9.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color.Black.copy(alpha = 0.85f),
                                            blurRadius = 4f
                                        )
                                    )
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
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
}

// ============================================================================
// AUDIO TAB CONTENT (Dialogue Gain, Voice Isolation, Waveform)
// ============================================================================

@Composable
private fun AudioTabContent(
    gainDb: Float,
    onGainChange: (Float) -> Unit,
    voiceIsolation: Boolean,
    onVoiceIsolationChange: (Boolean) -> Unit,
    customAudio: FcpCustomAudio? = null,
    onAddAudioFile: () -> Unit = {},
    onExtractAudioFromVideo: () -> Unit = {},
    onAudioVolumeChange: (Float) -> Unit = {},
    onMuteOriginalVideoChange: (Boolean) -> Unit = {},
    onLoopAudioChange: (Boolean) -> Unit = {},
    onRemoveCustomAudio: () -> Unit = {},
    isPlaying: Boolean = false,
    currentTimeSec: Float = 0f
) {
    val colors = LocalOscellionColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── 1. CUSTOM SOUNDTRACK & VIDEO AUDIO EXTRACTION CARD ──
        item {
            WorkspaceSectionCard(title = "SOUNDTRACK & AUDIO REPLACEMENT", icon = Lucide.Music) {
                if (customAudio == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Replace the video's original audio track with custom music or extract audio from another video.",
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            lineHeight = 15.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "Add Audio" Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                colors.primary.copy(alpha = 0.22f),
                                                colors.primary.copy(alpha = 0.08f)
                                            )
                                        )
                                    )
                                    .border(1.dp, colors.primary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .clickable { onAddAudioFile() }
                                    .padding(vertical = 12.dp, horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Music, contentDescription = "Add Audio", tint = colors.primary, modifier = Modifier.size(18.dp))
                                    }
                                    Text("Add Audio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                    Text("MP3, M4A, WAV", fontSize = 9.5.sp, color = colors.textMuted)
                                }
                            }

                            // "Extract from a video" Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF8B5CF6).copy(alpha = 0.22f),
                                                Color(0xFF8B5CF6).copy(alpha = 0.08f)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .clickable { onExtractAudioFromVideo() }
                                    .padding(vertical = 12.dp, horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF8B5CF6).copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Film, contentDescription = "Extract Audio", tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
                                    }
                                    Text("Extract From Video", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                    Text("Rip audio from video", fontSize = 9.5.sp, color = colors.textMuted)
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Header banner with track info and delete button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF064E3B).copy(alpha = 0.6f),
                                            colors.glassBg
                                        )
                                    )
                                )
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (customAudio.isExtractedFromVideo) Lucide.Film else Lucide.Music,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customAudio.name,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = if (customAudio.isExtractedFromVideo) "Extracted Audio" else "Custom Audio",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF34D399)
                                                )
                                            }
                                            if (customAudio.durationSec > 0f) {
                                                Text(
                                                    text = formatTimecode(customAudio.durationSec),
                                                    fontSize = 9.5.sp,
                                                    color = colors.textMuted,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = onRemoveCustomAudio,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.Trash2,
                                        contentDescription = "Remove soundtrack",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Toggle: Replace Original Video Audio (Mute Original)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.glassBg)
                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Replace Video Audio (Mute Original)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = if (customAudio.muteOriginalVideo)
                                        "Original video sound is 100% muted & replaced"
                                    else
                                        "Mix custom audio with original video sound",
                                    fontSize = 10.sp,
                                    color = if (customAudio.muteOriginalVideo) Color(0xFF34D399) else colors.textMuted
                                )
                            }
                            SmallSwitch(
                                checked = customAudio.muteOriginalVideo,
                                onCheckedChange = onMuteOriginalVideoChange
                            )
                        }

                        // Toggle: Loop Soundtrack
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.glassBg)
                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Loop Soundtrack",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = if (customAudio.loopAudio)
                                        "Soundtrack repeats continuously across entire project"
                                    else
                                        "Plays once and stops when track ends",
                                    fontSize = 10.sp,
                                    color = if (customAudio.loopAudio) Color(0xFF34D399) else colors.textMuted
                                )
                            }
                            SmallSwitch(
                                checked = customAudio.loopAudio,
                                onCheckedChange = onLoopAudioChange
                            )
                        }

                        // Soundtrack Volume Slider
                        FcpColorSliderRow(
                            title = "Soundtrack Volume",
                            icon = Lucide.Volume2,
                            value = customAudio.volume * 100f,
                            defaultValue = 100f,
                            valueRange = 0f..150f,
                            isBipolar = false,
                            formatValue = { v -> "${v.toInt()}%" },
                            onValueChange = { onAudioVolumeChange(it / 100f) },
                            gradientColors = listOf(Color(0xFF10B981), Color(0xFF84CC16), Color(0xFFFACC15), Color(0xFF38BDF8))
                        )

                        // Quick Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.glassBg)
                                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                    .clickable { onAddAudioFile() }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Lucide.Music, contentDescription = null, tint = colors.primary, modifier = Modifier.size(13.dp))
                                    Text("Change Audio", fontSize = 11.sp, color = colors.textMain, fontWeight = FontWeight.Medium)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.glassBg)
                                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                    .clickable { onExtractAudioFromVideo() }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Lucide.Film, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(13.dp))
                                    Text("Extract From Video", fontSize = 11.sp, color = colors.textMain, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 2. ORIGINAL VIDEO AUDIO MIXER CARD ──
        item {
            WorkspaceSectionCard(title = "ORIGINAL VIDEO AUDIO", icon = Lucide.Volume2) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (customAudio?.muteOriginalVideo == true) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                                .border(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Lucide.VolumeX, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Original audio is muted. Toggle 'Replace Video Audio' above to unmute and mix.",
                                    fontSize = 10.sp,
                                    color = Color(0xFFF59E0B),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // Gain Slider
                    FcpColorSliderRow(
                        title = "Dialogue Gain",
                        icon = Lucide.Volume2,
                        value = gainDb,
                        defaultValue = 0f,
                        valueRange = -20f..20f,
                        isBipolar = true,
                        formatValue = { v ->
                            val iv = v.toInt()
                            if (iv > 0) "+$iv dB" else if (iv < 0) "$iv dB" else "0 dB"
                        },
                        onValueChange = onGainChange,
                        gradientColors = listOf(Color(0xFF10B981), Color(0xFF84CC16), Color(0xFFFACC15), Color(0xFF38BDF8))
                    )

                    // Voice Isolation Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Voice Isolation", fontSize = 12.sp, color = colors.textMain, fontWeight = FontWeight.SemiBold)
                            Text("AI background noise removal", fontSize = 10.sp, color = colors.textMuted)
                        }
                        SmallSwitch(
                            checked = voiceIsolation,
                            onCheckedChange = {
                                onVoiceIsolationChange(it)
                                ToastManager.showToast(if (it) "Voice Isolation enabled" else "Voice Isolation disabled")
                            }
                        )
                    }
                }
            }
        }

        // ── 3. LIVE MASTER AUDIO WAVEFORM CANVAS ──
        item {
            WorkspaceSectionCard(title = "MASTER AUDIO WAVEFORM", icon = Lucide.Activity) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.bg)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val gainScale = (1f + (gainDb / 28f)).coerceIn(0.2f, 1.75f)
                    val effectiveScale = if (customAudio != null) {
                        if (customAudio.muteOriginalVideo) customAudio.volume.coerceIn(0.2f, 1.75f)
                        else (gainScale * 0.5f + customAudio.volume * 0.5f).coerceIn(0.2f, 1.75f)
                    } else gainScale
                    val animPhase = if (isPlaying) (currentTimeSec * 12f) else 0f
                    Canvas(Modifier.fillMaxSize()) {
                        val barCount = 42
                        val barSpacing = size.width / barCount
                        val midY = size.height / 2f
                        for (i in 0 until barCount) {
                            val baseSeed = ((i * 17 + 11) % 100) / 100f
                            val liveWave = if (isPlaying) (sin(animPhase + i * 0.45f) * 0.18f + 0.82f) else 1f
                            val barHeight = (baseSeed * liveWave * effectiveScale * size.height * 0.78f)
                                .coerceIn(4f, size.height * 0.92f)
                            val barColor = if (barHeight > size.height * 0.75f) {
                                Color(0xFFF59E0B)
                            } else if (customAudio != null) {
                                Color(0xFF10B981)
                            } else if (voiceIsolation) {
                                Color(0xFF10B981)
                            } else {
                                colors.primary.copy(alpha = 0.78f)
                            }
                            drawLine(
                                color = barColor,
                                start = Offset(i * barSpacing + 2f, midY - barHeight / 2f),
                                end = Offset(i * barSpacing + 2f, midY + barHeight / 2f),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// EFFECTS TAB CONTENT (AI Subject Mask, Cinematic Depth, Speed)
// ============================================================================

@Composable
private fun EffectsTabContent(
    aiMaskEnabled: Boolean,
    onAiMaskToggle: (Boolean) -> Unit,
    bokehAperture: Float,
    onBokehChange: (Float) -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val colors = LocalOscellionColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // AI Subject Mask & Bokeh Depth
        item {
            WorkspaceSectionCard(title = "AI SUBJECT DEPTH & BOKEH", icon = Lucide.Zap) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("AI Subject Isolation", fontSize = 12.sp, color = colors.textMain, fontWeight = FontWeight.SemiBold)
                            Text("Segment foreground subject", fontSize = 10.sp, color = colors.textMuted)
                        }
                        SmallSwitch(
                            checked = aiMaskEnabled,
                            onCheckedChange = {
                                onAiMaskToggle(it)
                                ToastManager.showToast(if (it) "Subject Mask Active" else "Subject Mask Disabled")
                            }
                        )
                    }

                    if (aiMaskEnabled) {
                        FcpColorSliderRow(
                            title = "Aperture Depth Blur",
                            icon = Lucide.Camera,
                            value = bokehAperture,
                            defaultValue = 2.8f,
                            valueRange = 1.4f..16f,
                            isBipolar = false,
                            formatValue = { "f/${String.format("%.1f", it)}" },
                            onValueChange = onBokehChange,
                            gradientColors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFF06B6D4))
                        )
                    }
                }
            }
        }

        // Playback Speed Selector
        item {
            WorkspaceSectionCard(title = "PLAYBACK SPEED", icon = Lucide.Activity) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
                    speeds.forEach { s ->
                        val active = speed == s
                        ShapePill(
                            label = "${s}x",
                            active = active,
                            onClick = {
                                onSpeedChange(s)
                                ToastManager.showToast("Speed: ${s}x")
                            }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// TITLES TAB CONTENT & TYPOGRAPHY ENGINE (All 12 Fonts & Pro Presets)
// ============================================================================

@Composable
private fun FcpFontDropdownSelector(
    label: String,
    selectedFontAsset: String,
    onSelectFont: (String) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    var showDialog by remember { mutableStateOf(false) }

    val currentFontItem = remember(selectedFontAsset) {
        AllFcpFonts.find { it.fileName.equals(selectedFontAsset, ignoreCase = true) }
            ?: AllFcpFonts.first()
    }
    val currentFontFamily = remember(currentFontItem.fileName) {
        getFcpFontFamily(context, currentFontItem.fileName)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.sp, color = colors.textMuted, fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.bg)
                .border(1.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showDialog = true
                }
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aa",
                            fontFamily = currentFontFamily,
                            color = colors.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = currentFontItem.name,
                            fontFamily = currentFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = currentFontItem.category,
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Change", fontSize = 11.sp, color = colors.primary, fontWeight = FontWeight.SemiBold)
                    Icon(
                        imageVector = Lucide.ChevronDown,
                        contentDescription = "Select Font",
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Modal Dialog showing all 12 fonts rendered in their actual typefaces
    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .heightIn(max = 520.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFFFFFFF))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Dialog Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Select Typography",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "All 12 Fonts inside fonts/ folder",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(colors.glassBg)
                                    .clickable { showDialog = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = "Close",
                                    tint = colors.textMain,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Scrollable List of all 12 fonts
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            items(AllFcpFonts) { fontItem ->
                                val isSelected = fontItem.fileName.equals(selectedFontAsset, ignoreCase = true)
                                val fontFamily = remember(fontItem.fileName) {
                                    getFcpFontFamily(context, fontItem.fileName)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) colors.primary.copy(alpha = 0.15f)
                                            else colors.bg
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) colors.primary else colors.glassBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectFont(fontItem.fileName)
                                            showDialog = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = fontItem.name,
                                                fontFamily = fontFamily,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) colors.primary else colors.textMain
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colors.primary.copy(alpha = 0.10f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = fontItem.category,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.primary
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = fontItem.sampleText,
                                            fontFamily = fontFamily,
                                            fontSize = 13.sp,
                                            color = colors.textMuted
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(colors.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Lucide.Check,
                                                contentDescription = "Selected",
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
            }
        }
    }
}

@Composable
private fun FcpTextLayerOverlay(
    layer: FcpTextLayer,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onTransform: (dxDp: Float, dyDp: Float, zoomChange: Float, rotationDelta: Float) -> Unit,
    onDuplicate: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val primaryFont = remember(layer.primaryFontAsset) {
        getFcpFontFamily(context, layer.primaryFontAsset)
    }
    val scriptFont = remember(layer.scriptFontAsset) {
        getFcpFontFamily(context, layer.scriptFontAsset)
    }
    val colors = LocalOscellionColors.current

    val headlineBrush = remember(layer.isGradientText, layer.textColor, layer.secondaryTextColor) {
        if (layer.isGradientText) {
            Brush.linearGradient(listOf(layer.textColor, layer.secondaryTextColor))
        } else {
            SolidColor(layer.textColor)
        }
    }

    val shadowStyle = remember(layer.hasDropShadow) {
        if (layer.hasDropShadow) {
            androidx.compose.ui.graphics.Shadow(
                color = Color.Black.copy(alpha = 0.85f),
                offset = Offset(2f, 3f),
                blurRadius = 6f
            )
        } else null
    }

    val effectiveOpacity = remember(layer.opacity, layer.blendMode) {
        when (layer.blendMode) {
            "Screen" -> (layer.opacity * 0.9f).coerceIn(0.1f, 1f)
            "Overlay" -> (layer.opacity * 0.95f).coerceIn(0.1f, 1f)
            "Multiply" -> (layer.opacity * 0.85f).coerceIn(0.1f, 1f)
            "Add" -> layer.opacity
            else -> layer.opacity
        }
    }

    Box(
        modifier = Modifier
            .pointerInput(layer.id) {
                detectTapGestures(
                    onTap = { onSelect() }
                )
            }
            .pointerInput(layer.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    onSelect()
                    val dxDp = with(density) { pan.x.toDp().value }
                    val dyDp = with(density) { pan.y.toDp().value }
                    onTransform(dxDp, dyDp, zoom, rotation)
                }
            }
            .graphicsLayer {
                alpha = effectiveOpacity
                rotationZ = layer.rotationDeg
            }
            .then(
                if (layer.hasBackgroundPill) {
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                } else Modifier
            )
            .then(
                if (isSelected) {
                    Modifier
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF3B82F6))),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(6.dp)
                } else Modifier.padding(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when (layer.presetType) {
            FcpTitlePresetType.SCRIPT_HEADLINE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = layer.primaryText,
                            fontFamily = primaryFont,
                            fontWeight = FontWeight.Black,
                            fontSize = layer.fontSizeSp.sp,
                            style = TextStyle(
                                brush = headlineBrush,
                                shadow = shadowStyle
                            ),
                            letterSpacing = layer.letterSpacingSp.sp,
                            textAlign = TextAlign.Center
                        )
                        if (layer.scriptText.isNotBlank()) {
                            Text(
                                text = layer.scriptText,
                                fontFamily = scriptFont,
                                fontSize = (layer.fontSizeSp * 1.05f).sp,
                                color = layer.accentColor,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (-8).dp, y = (-12).dp)
                                    .rotate(-7f),
                                style = TextStyle(
                                    shadow = shadowStyle ?: androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        offset = Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                    }
                    if (layer.subtitleText.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = layer.subtitleText,
                            fontFamily = primaryFont,
                            fontSize = (layer.fontSizeSp * 0.32f).coerceAtLeast(9f).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = (layer.letterSpacingSp * 2.5f + 1f).sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            FcpTitlePresetType.GOLD_3D -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)
                ) {
                    if (layer.badgeText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, layer.accentColor.copy(alpha = 0.7f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = layer.badgeText.uppercase(),
                                fontFamily = primaryFont,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = layer.accentColor,
                                letterSpacing = (layer.letterSpacingSp * 2f).sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    Box(contentAlignment = Alignment.Center) {
                        val goldGrad = Brush.verticalGradient(
                            listOf(
                                Color(0xFFFFF9C4),
                                layer.accentColor,
                                Color(0xFFF59E0B),
                                Color(0xFFB45309)
                            )
                        )
                        // Under-shadow 1
                        Text(
                            text = layer.primaryText,
                            fontFamily = primaryFont,
                            fontWeight = FontWeight.Black,
                            fontSize = layer.fontSizeSp.sp,
                            color = Color(0xFF261204),
                            letterSpacing = layer.letterSpacingSp.sp,
                            modifier = Modifier.offset(x = 3.dp, y = 3.dp)
                        )
                        // Under-shadow 2
                        Text(
                            text = layer.primaryText,
                            fontFamily = primaryFont,
                            fontWeight = FontWeight.Black,
                            fontSize = layer.fontSizeSp.sp,
                            color = Color(0xFF78350F),
                            letterSpacing = layer.letterSpacingSp.sp,
                            modifier = Modifier.offset(x = 1.5.dp, y = 1.5.dp)
                        )
                        // Front Metallic Gold / Gradient text
                        Text(
                            text = layer.primaryText,
                            fontFamily = primaryFont,
                            fontWeight = FontWeight.Black,
                            fontSize = layer.fontSizeSp.sp,
                            letterSpacing = layer.letterSpacingSp.sp,
                            style = TextStyle(brush = if (layer.isGradientText) headlineBrush else goldGrad, shadow = shadowStyle)
                        )

                        // Signature crossover script
                        if (layer.scriptText.isNotBlank()) {
                            Text(
                                text = layer.scriptText,
                                fontFamily = scriptFont,
                                fontSize = (layer.fontSizeSp * 0.95f).sp,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .rotate(-9f)
                                    .offset(y = 2.dp),
                                style = TextStyle(
                                    shadow = shadowStyle ?: androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        offset = Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                    }

                    if (layer.subtitleText.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = layer.subtitleText,
                            fontFamily = primaryFont,
                            fontSize = (layer.fontSizeSp * 0.3f).coerceAtLeast(9f).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = (layer.letterSpacingSp * 2.5f + 1f).sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            FcpTitlePresetType.MODERN_FOLD -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = layer.primaryText,
                            fontFamily = primaryFont,
                            fontWeight = FontWeight.Black,
                            fontSize = layer.fontSizeSp.sp,
                            color = Color.Black.copy(alpha = 0.6f),
                            letterSpacing = layer.letterSpacingSp.sp,
                            modifier = Modifier.offset(x = 2.dp, y = 3.dp)
                        )
                        Text(
                            text = layer.primaryText,
                            fontFamily = primaryFont,
                            fontWeight = FontWeight.Black,
                            fontSize = layer.fontSizeSp.sp,
                            letterSpacing = layer.letterSpacingSp.sp,
                            style = TextStyle(brush = headlineBrush, shadow = shadowStyle)
                        )
                    }
                    if (layer.subtitleText.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = layer.subtitleText,
                            fontFamily = primaryFont,
                            fontSize = (layer.fontSizeSp * 0.32f).coerceAtLeast(9f).sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = (layer.letterSpacingSp * 2f + 1f).sp
                        )
                    }
                }
            }

            FcpTitlePresetType.DESIGN_CONTRAST -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)
                ) {
                    Text(
                        text = layer.primaryText.uppercase(),
                        fontFamily = primaryFont,
                        fontWeight = FontWeight.Black,
                        fontSize = layer.fontSizeSp.sp,
                        letterSpacing = (layer.letterSpacingSp + 1f).sp,
                        style = TextStyle(brush = headlineBrush, shadow = shadowStyle)
                    )
                    if (layer.scriptText.isNotBlank()) {
                        Text(
                            text = layer.scriptText,
                            fontFamily = scriptFont,
                            fontSize = (layer.fontSizeSp * 0.75f).sp,
                            color = layer.accentColor,
                            modifier = Modifier.offset(y = (-4).dp),
                            style = TextStyle(shadow = shadowStyle)
                        )
                    }
                }
            }

            FcpTitlePresetType.BOXED_FRAME -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .border(2.dp, layer.accentColor, RoundedCornerShape(2.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = layer.primaryText.uppercase(),
                            fontFamily = primaryFont,
                            fontWeight = FontWeight.Black,
                            fontSize = layer.fontSizeSp.sp,
                            letterSpacing = (layer.letterSpacingSp * 2f).sp,
                            style = TextStyle(brush = headlineBrush, shadow = shadowStyle)
                        )
                    }
                    if (layer.subtitleText.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(Modifier.width(20.dp).height(1.dp).background(layer.accentColor.copy(alpha = 0.7f)))
                            Text(
                                text = layer.subtitleText.uppercase(),
                                fontFamily = primaryFont,
                                fontSize = (layer.fontSizeSp * 0.32f).coerceAtLeast(8.5f).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f),
                                letterSpacing = (layer.letterSpacingSp * 2f + 1f).sp
                            )
                            Box(Modifier.width(20.dp).height(1.dp).background(layer.accentColor.copy(alpha = 0.7f)))
                        }
                    }
                }
            }

            FcpTitlePresetType.ABSTRACT_BADGE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(layer.accentColor)
                            .shadow(6.dp, RoundedCornerShape(8.dp))
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = layer.primaryText.uppercase(),
                            fontFamily = primaryFont,
                            fontWeight = FontWeight.Black,
                            fontSize = layer.fontSizeSp.sp,
                            letterSpacing = layer.letterSpacingSp.sp,
                            style = TextStyle(brush = headlineBrush)
                        )
                    }
                    if (layer.subtitleText.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = layer.subtitleText.uppercase(),
                            fontFamily = primaryFont,
                            fontSize = (layer.fontSizeSp * 0.32f).coerceAtLeast(8.5f).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (layer.letterSpacingSp * 2f + 1f).sp,
                            style = TextStyle(shadow = shadowStyle)
                        )
                    }
                }
            }

            FcpTitlePresetType.CYBER_NEON -> {
                val neonBrush = Brush.horizontalGradient(listOf(layer.textColor, layer.accentColor))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                        .border(1.5.dp, neonBrush, RoundedCornerShape(12.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = layer.primaryText.uppercase(),
                            fontFamily = primaryFont,
                            fontWeight = FontWeight.Black,
                            fontSize = layer.fontSizeSp.sp,
                            letterSpacing = layer.letterSpacingSp.sp,
                            style = TextStyle(
                                brush = neonBrush,
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = layer.textColor.copy(alpha = 0.8f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 12f
                                )
                            )
                        )
                        if (layer.subtitleText.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = layer.subtitleText.uppercase(),
                                fontFamily = primaryFont,
                                fontSize = (layer.fontSizeSp * 0.32f).coerceAtLeast(8.5f).sp,
                                color = Color.White.copy(alpha = 0.85f),
                                letterSpacing = (layer.letterSpacingSp * 2f).sp
                            )
                        }
                    }
                }
            }

            FcpTitlePresetType.MINIMAL_CLEAN -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(layer.accentColor)
                    )
                    Column {
                        Text(
                            text = layer.primaryText,
                            fontFamily = primaryFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = (layer.fontSizeSp * 0.75f).sp,
                            letterSpacing = layer.letterSpacingSp.sp,
                            style = TextStyle(brush = headlineBrush, shadow = shadowStyle)
                        )
                        if (layer.subtitleText.isNotBlank()) {
                            Text(
                                text = layer.subtitleText,
                                fontFamily = primaryFont,
                                fontSize = (layer.fontSizeSp * 0.35f).coerceAtLeast(9f).sp,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = (layer.letterSpacingSp * 1.5f).sp
                            )
                        }
                    }
                }
            }
        }

        // ── Floating Corner Interactive Badges When Selected ──────────────────
        if (isSelected) {
            // Top-Right: Delete [X]
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Lucide.X, contentDescription = "Delete Title", tint = Color.White, modifier = Modifier.size(12.dp))
            }

            // Top-Left: Duplicate [+]
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-8).dp, y = (-8).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDuplicate()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Lucide.Copy, contentDescription = "Duplicate Title", tint = Color.White, modifier = Modifier.size(11.dp))
            }

            // Bottom-Right: Corner Node
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color(0xFF3B82F6), CircleShape)
            )

            // Bottom-Left: Corner Node
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-6).dp, y = 6.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color(0xFF3B82F6), CircleShape)
            )
        }
    }
}

// ============================================================================
// CHROMA STUDIO WORKSPACE-PARITY TEXT LAYER MANAGEMENT
// ============================================================================

fun applyFcpTitlePreset(cur: FcpTextLayer, preset: FcpTitlePresetType): FcpTextLayer {
    return when (preset) {
        FcpTitlePresetType.SCRIPT_HEADLINE -> cur.copy(
            presetType = preset,
            primaryText = if (cur.primaryText.isBlank() || cur.primaryText.startsWith("Layer")) "SCRIPT FONTS" else cur.primaryText,
            scriptText = "Script &",
            subtitleText = "EVERYTHING YOU NEED TO KNOW",
            primaryFontAsset = "Outfit-Bold.ttf",
            scriptFontAsset = "Thesignature.ttf",
            textColor = Color.White,
            accentColor = Color(0xFFFFB800)
        )
        FcpTitlePresetType.GOLD_3D -> cur.copy(
            presetType = preset,
            primaryText = if (cur.primaryText.isBlank() || cur.primaryText.startsWith("Layer")) "3D GOLD" else cur.primaryText,
            scriptText = "Signature",
            subtitleText = "PREMIUM CINEMATIC EDIT",
            badgeText = "AESTHETIC",
            primaryFontAsset = "Wotham.ttf",
            scriptFontAsset = "Thesignature.ttf",
            textColor = Color(0xFFFFF9C4),
            accentColor = Color(0xFFFFD700)
        )
        FcpTitlePresetType.MODERN_FOLD -> cur.copy(
            presetType = preset,
            primaryText = if (cur.primaryText.isBlank() || cur.primaryText.startsWith("Layer")) "MODERN ANIMATED" else cur.primaryText,
            scriptText = "",
            subtitleText = "CREATE WITH PASSION",
            primaryFontAsset = "Wotham.ttf",
            textColor = Color.White,
            accentColor = Color(0xFF3B82F6)
        )
        FcpTitlePresetType.DESIGN_CONTRAST -> cur.copy(
            presetType = preset,
            primaryText = if (cur.primaryText.isBlank() || cur.primaryText.startsWith("Layer")) "DESIGN IS" else cur.primaryText,
            scriptText = "thinking made visual",
            subtitleText = "",
            primaryFontAsset = "Outfit-Bold.ttf",
            scriptFontAsset = "Thesignature.ttf",
            textColor = Color.White,
            accentColor = Color(0xFFFFD166)
        )
        FcpTitlePresetType.BOXED_FRAME -> cur.copy(
            presetType = preset,
            primaryText = if (cur.primaryText.isBlank() || cur.primaryText.startsWith("Layer")) "SIMPLICITY" else cur.primaryText,
            scriptText = "",
            subtitleText = "LESS IS MORE",
            primaryFontAsset = "Wotham.ttf",
            textColor = Color.White,
            accentColor = Color.White
        )
        FcpTitlePresetType.ABSTRACT_BADGE -> cur.copy(
            presetType = preset,
            primaryText = if (cur.primaryText.isBlank() || cur.primaryText.startsWith("Layer")) "ABSTRACT" else cur.primaryText,
            scriptText = "",
            subtitleText = "INNOVATION & ART",
            primaryFontAsset = "Wotham.ttf",
            textColor = Color.Black,
            accentColor = Color.White
        )
        FcpTitlePresetType.CYBER_NEON -> cur.copy(
            presetType = preset,
            primaryText = if (cur.primaryText.isBlank() || cur.primaryText.startsWith("Layer")) "CYBER CITY" else cur.primaryText,
            scriptText = "",
            subtitleText = "FUTURE VISION 2026",
            primaryFontAsset = "Outfit-Bold.ttf",
            textColor = Color(0xFF06B6D4),
            accentColor = Color(0xFFF43F5E)
        )
        FcpTitlePresetType.MINIMAL_CLEAN -> cur.copy(
            presetType = preset,
            primaryText = if (cur.primaryText.isBlank() || cur.primaryText.startsWith("Layer")) "CINEMATIC STORY" else cur.primaryText,
            scriptText = "",
            subtitleText = "DIRECTOR'S CUT",
            primaryFontAsset = "Comfortaa-Bold.ttf",
            textColor = Color.White,
            accentColor = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun FieldLabel(text: String, value: String? = null) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            color = colors.textMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        if (value != null) {
            Text(
                text = value,
                color = colors.textMain,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FcpTypeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val shape = RoundedCornerShape(12.dp)
    val bgColor = colors.glassBg
    val contentColor = if (active) colors.primary else colors.textMain

    Column(
        modifier = modifier
            .clip(shape)
            .background(bgColor, shape)
            .border(
                width = if (active) 2.dp else 1.dp,
                brush = if (active) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF60A5FA),
                            Color(0xFF3B82F6),
                            Color(0xFF2563EB)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 3f).coerceAtMost(1f)),
                            colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 0.2f).coerceAtMost(1f))
                        )
                    )
                },
                shape = shape
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FcpColorControlRow(
    label: String,
    color: Color,
    onColorChange: (Color) -> Unit
) {
    val colors = LocalOscellionColors.current
    var showPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Main swatch + Monospace Hex row (Matching LayerCard.kt)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (showPicker) colors.primary.copy(alpha = 0.1f) else colors.glassBg)
                .border(
                    1.dp,
                    if (showPicker) colors.primary.copy(alpha = 0.5f) else colors.glassBorder,
                    RoundedCornerShape(8.dp)
                )
                .clickable { showPicker = !showPicker }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, colors.glassBorder, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = String.format("#%06X", 0xFFFFFF and color.toArgb()),
                    color = colors.textMain,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label,
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Lucide.ChevronDown,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(if (showPicker) 180f else 0f)
                )
            }
        }

        // Quick 1-Tap Color Swatches Palette
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val quickColors = listOf(
                Color(0xFFFFFFFF), // White
                Color(0xFFFFB800), // Amber Gold
                Color(0xFF06B6D4), // Cyan
                Color(0xFFF43F5E), // Coral
                Color(0xFFA855F7), // Purple
                Color(0xFF10B981), // Emerald
                Color(0xFFF59E0B), // Warm Gold
                Color(0xFF0F172A)  // Dark Slate
            )
            quickColors.forEach { c ->
                val isSelectedColor = (c.toArgb() and 0xFFFFFF) == (color.toArgb() and 0xFFFFFF)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(
                            width = if (isSelectedColor) 2.dp else 1.dp,
                            color = if (isSelectedColor) colors.primary else Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .clickable { onColorChange(c) }
                )
            }
        }

        // Inline Color Picker Dropdown (ChromaStudio Native)
        if (showPicker) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.glassBg)
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                InlineColorPicker(
                    initialColor = color,
                    onColorChange = onColorChange
                )
            }
        }
    }
}

@Composable
private fun FcpTitleLayerCard(
    layer: FcpTextLayer,
    index: Int,
    totalLayers: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleExpand: () -> Unit,
    onToggleVisibility: () -> Unit,
    onUpdate: (FcpTextLayer) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(16.dp)

    val borderBrush = remember(isSelected, layer.isExpanded, colors.primary, colors.glassBorder) {
        if (isSelected || layer.isExpanded) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF60A5FA),
                    Color(0xFF3B82F6),
                    Color(0xFF2563EB)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 3f).coerceAtMost(1f)),
                    colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 0.2f).coerceAtMost(1f))
                )
            )
        }
    }

    var isRenaming by remember { mutableStateOf(false) }
    var renameText by remember(layer.name) { mutableStateOf(layer.name) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
            .clip(shape)
            .background(colors.glassBg, shape)
            .border(
                width = if (isSelected || layer.isExpanded) 2.dp else 1.dp,
                brush = borderBrush,
                shape = shape
            )
            .padding(12.dp)
    ) {
        // ── 1. Header Row (Reorder, Visibility, Swatch, Title, Expand, Duplicate, Delete) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reorder handles (ChevronUp & ChevronDown)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Icon(
                    imageVector = Lucide.ChevronUp,
                    contentDescription = "Move Layer Up",
                    tint = if (index > 0) colors.textMain else colors.textMuted.copy(alpha = 0.25f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(enabled = index > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMoveUp()
                        }
                )
                Icon(
                    imageVector = Lucide.ChevronDown,
                    contentDescription = "Move Layer Down",
                    tint = if (index < totalLayers - 1) colors.textMain else colors.textMuted.copy(alpha = 0.25f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(enabled = index < totalLayers - 1) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMoveDown()
                        }
                )
            }

            Spacer(Modifier.width(6.dp))

            // Visibility toggle (Eye / EyeOff)
            Icon(
                imageVector = if (layer.isVisible) Lucide.Eye else Lucide.EyeOff,
                contentDescription = "Toggle Visibility",
                tint = if (layer.isVisible) colors.textMain else colors.textMuted.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleVisibility()
                    }
            )

            Spacer(Modifier.width(8.dp))

            // Circular Preview Swatch (24.dp) with color gradient and letter badge
            val isLightText = (0.299f * layer.textColor.red + 0.587f * layer.textColor.green + 0.114f * layer.textColor.blue) > 0.55f
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (layer.isGradientText) {
                            Brush.linearGradient(listOf(layer.textColor, layer.secondaryTextColor))
                        } else {
                            Brush.linearGradient(listOf(layer.textColor, layer.accentColor))
                        }
                    )
                    .border(1.dp, colors.glassBorder, CircleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onSelect()
                        onToggleExpand()
                    },
                contentAlignment = Alignment.Center
            ) {
                val letter = layer.primaryText.take(1).uppercase().ifBlank { "T" }
                Text(
                    text = letter,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isLightText) Color.Black else Color.White
                )
            }

            Spacer(Modifier.width(8.dp))

            // Layer Title & Preset Subtitle OR Inline Renaming Field
            if (isRenaming) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = colors.textMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.bg)
                            .border(1.dp, colors.primary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = "Save Layer Name",
                        tint = colors.primary,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdate(layer.copy(name = renameText.ifBlank { "Title Layer ${index + 1}" }))
                                isRenaming = false
                            }
                            .padding(2.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onSelect()
                            onToggleExpand()
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = layer.name.ifBlank { layer.primaryText.ifBlank { "Title Layer ${index + 1}" } },
                            color = colors.textMain,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Lucide.Pencil,
                            contentDescription = "Rename Layer",
                            tint = colors.textMuted.copy(alpha = 0.55f),
                            modifier = Modifier
                                .size(12.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    renameText = layer.name.ifBlank { "Title Layer ${index + 1}" }
                                    isRenaming = true
                                }
                        )
                    }
                    Text(
                        text = layer.presetType.title,
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Expand Chevron (rotates 180°)
            Icon(
                imageVector = Lucide.ChevronDown,
                contentDescription = "Expand",
                tint = colors.textMuted,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(if (layer.isExpanded) 180f else 0f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onSelect()
                        onToggleExpand()
                    }
                    .padding(2.dp)
            )

            Spacer(Modifier.width(8.dp))

            // Duplicate button
            Icon(
                imageVector = Lucide.Copy,
                contentDescription = "Duplicate Layer",
                tint = colors.textMuted,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDuplicate()
                    }
            )

            Spacer(Modifier.width(8.dp))

            // Delete button
            Icon(
                imageVector = Lucide.Trash2,
                contentDescription = "Delete Layer",
                tint = colors.error,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDelete()
                    }
            )
        }

        // ── 2. Expandable Body (ChromaStudio Microscopic Parity) ───────────────
        AnimatedVisibility(
            visible = layer.isExpanded,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(spring(stiffness = Spring.StiffnessHigh))
        ) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Live Mini Preview Box (Real-Time Canvas Lookahead) ─────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0B0F19))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LIVE CANVAS PREVIEW",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.18f),
                        letterSpacing = 2.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    )

                    val miniBrush = if (layer.isGradientText) {
                        Brush.linearGradient(listOf(layer.textColor, layer.secondaryTextColor))
                    } else {
                        SolidColor(layer.textColor)
                    }
                    val miniShadow = if (layer.hasDropShadow) {
                        androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.85f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    } else null

                    val context = LocalContext.current
                    val primaryFont = remember(layer.primaryFontAsset) { getFcpFontFamily(context, layer.primaryFontAsset) }
                    val scriptFont = remember(layer.scriptFontAsset) { getFcpFontFamily(context, layer.scriptFontAsset) }

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = 0.72f
                                scaleY = 0.72f
                                alpha = layer.opacity
                                rotationZ = layer.rotationDeg
                            }
                            .then(
                                if (layer.hasBackgroundPill) {
                                    Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = layer.primaryText.ifBlank { "TITLE PREVIEW" },
                                    fontFamily = primaryFont,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    style = TextStyle(brush = miniBrush, shadow = miniShadow),
                                    letterSpacing = layer.letterSpacingSp.sp,
                                    textAlign = TextAlign.Center
                                )
                                if (layer.scriptText.isNotBlank()) {
                                    Text(
                                        text = layer.scriptText,
                                        fontFamily = scriptFont,
                                        fontSize = 24.sp,
                                        color = layer.accentColor,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .offset(x = (-8).dp, y = (-10).dp)
                                            .rotate(-7f),
                                        style = TextStyle(shadow = miniShadow)
                                    )
                                }
                            }
                            if (layer.subtitleText.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = layer.subtitleText,
                                    fontFamily = primaryFont,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    letterSpacing = (layer.letterSpacingSp * 2f).sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Section A: Text Content Inputs
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Text Content")

                    // Headline Input
                    FcpTextInput(
                        value = layer.primaryText,
                        placeholder = "Enter main title...",
                        onValueChange = { onUpdate(layer.copy(primaryText = it)) }
                    )

                    // Script Accent Input (for presets with script)
                    val usesScript = layer.presetType in listOf(
                        FcpTitlePresetType.SCRIPT_HEADLINE,
                        FcpTitlePresetType.GOLD_3D,
                        FcpTitlePresetType.DESIGN_CONTRAST
                    )
                    if (usesScript) {
                        FcpTextInput(
                            value = layer.scriptText,
                            placeholder = "Script accent (e.g. Script &)...",
                            onValueChange = { onUpdate(layer.copy(scriptText = it)) }
                        )
                    }

                    // Subtitle Input
                    if (layer.presetType != FcpTitlePresetType.DESIGN_CONTRAST) {
                        FcpTextInput(
                            value = layer.subtitleText,
                            placeholder = "Subtitle / caption...",
                            onValueChange = { onUpdate(layer.copy(subtitleText = it)) }
                        )
                    }

                    // Badge Input (for 3D gold)
                    if (layer.presetType == FcpTitlePresetType.GOLD_3D) {
                        FcpTextInput(
                            value = layer.badgeText,
                            placeholder = "Badge (e.g. AESTHETIC)...",
                            onValueChange = { onUpdate(layer.copy(badgeText = it)) }
                        )
                    }
                }

                // Section B: Preset Style Engine Grid (Matching TypeButton 3-Col Layout)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Preset Engine", value = layer.presetType.category)

                    val allPresets = FcpTitlePresetType.values().toList()
                    val presetChunks = allPresets.chunked(3)
                    presetChunks.forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowPresets.forEach { preset ->
                                val isPresetActive = layer.presetType == preset
                                val icon = when (preset) {
                                    FcpTitlePresetType.SCRIPT_HEADLINE -> Lucide.PenTool
                                    FcpTitlePresetType.GOLD_3D -> Lucide.Sparkles
                                    FcpTitlePresetType.MODERN_FOLD -> Lucide.Film
                                    FcpTitlePresetType.DESIGN_CONTRAST -> Lucide.Type
                                    FcpTitlePresetType.BOXED_FRAME -> Lucide.Square
                                    FcpTitlePresetType.ABSTRACT_BADGE -> Lucide.Layers
                                    FcpTitlePresetType.CYBER_NEON -> Lucide.Zap
                                    FcpTitlePresetType.MINIMAL_CLEAN -> Lucide.SlidersHorizontal
                                }
                                FcpTypeButton(
                                    label = preset.title,
                                    icon = icon,
                                    active = isPresetActive,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val updated = applyFcpTitlePreset(layer, preset)
                                    onUpdate(updated)
                                }
                            }
                            // Fill remaining slots in last row if not multiple of 3
                            if (rowPresets.size < 3) {
                                repeat(3 - rowPresets.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Section C: Typography & Font Dropdowns
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Typography")

                    FcpFontDropdownSelector(
                        label = "Headline Font",
                        selectedFontAsset = layer.primaryFontAsset,
                        onSelectFont = { chosen ->
                            onUpdate(layer.copy(primaryFontAsset = chosen))
                        }
                    )

                    val usesScript = layer.presetType in listOf(
                        FcpTitlePresetType.SCRIPT_HEADLINE,
                        FcpTitlePresetType.GOLD_3D,
                        FcpTitlePresetType.DESIGN_CONTRAST
                    )
                    if (usesScript) {
                        FcpFontDropdownSelector(
                            label = "Cursive Script Font",
                            selectedFontAsset = layer.scriptFontAsset,
                            onSelectFont = { chosen ->
                                onUpdate(layer.copy(scriptFontAsset = chosen))
                            }
                        )
                    }
                }

                // Section D: Text Fill Mode & Color Management (Solid vs Linear Gradient)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldLabel("Headline Fill Mode", value = if (layer.isGradientText) "Linear Gradient" else "Solid Color")

                    // Fill Mode Toggle Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.glassBg)
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!layer.isGradientText) colors.primary else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdate(layer.copy(isGradientText = false))
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Solid Color",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!layer.isGradientText) colors.onPrimary else colors.textMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (layer.isGradientText) colors.primary else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdate(layer.copy(isGradientText = true))
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Linear Gradient",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (layer.isGradientText) colors.onPrimary else colors.textMuted
                            )
                        }
                    }

                    if (!layer.isGradientText) {
                        // Primary Text Color
                        FcpColorControlRow(
                            label = "Headline Color",
                            color = layer.textColor,
                            onColorChange = { onUpdate(layer.copy(textColor = it)) }
                        )
                    } else {
                        // Gradient Start Color
                        FcpColorControlRow(
                            label = "Gradient Start Color",
                            color = layer.textColor,
                            onColorChange = { onUpdate(layer.copy(textColor = it)) }
                        )

                        // Gradient End Color
                        FcpColorControlRow(
                            label = "Gradient End Color",
                            color = layer.secondaryTextColor,
                            onColorChange = { onUpdate(layer.copy(secondaryTextColor = it)) }
                        )

                        // 6 Instant Pro Gradient Presets
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "PRO GRADIENT PRESETS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = 1.sp
                            )

                            val gradientPresets = listOf(
                                "Sunset" to Pair(Color(0xFFFF5E3A), Color(0xFFFF2A68)),
                                "Cyber" to Pair(Color(0xFF00F2FE), Color(0xFF4FACFE)),
                                "Aurora" to Pair(Color(0xFF00FF87), Color(0xFF60EFFF)),
                                "Gold" to Pair(Color(0xFFFFDF00), Color(0xFFD4AF37)),
                                "Royal" to Pair(Color(0xFF8A2387), Color(0xFFE94057)),
                                "Ocean" to Pair(Color(0xFF2E3192), Color(0xFF1BFFFF))
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                gradientPresets.forEach { (name, pair) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Brush.linearGradient(listOf(pair.first, pair.second)))
                                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onUpdate(
                                                    layer.copy(
                                                        isGradientText = true,
                                                        textColor = pair.first,
                                                        secondaryTextColor = pair.second
                                                    )
                                                )
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Accent / Script Color
                    val usesScript = layer.presetType in listOf(
                        FcpTitlePresetType.SCRIPT_HEADLINE,
                        FcpTitlePresetType.GOLD_3D,
                        FcpTitlePresetType.DESIGN_CONTRAST
                    )
                    if (usesScript) {
                        FcpColorControlRow(
                            label = "Accent / Script Color",
                            color = layer.accentColor,
                            onColorChange = { onUpdate(layer.copy(accentColor = it)) }
                        )
                    }
                }

                // Section E: Title Size & Tracking (Letter Spacing)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Title Size", value = "${layer.fontSizeSp.toInt()} pt")
                        AppleSlider(
                            value = layer.fontSizeSp,
                            onValueChange = { onUpdate(layer.copy(fontSizeSp = it)) },
                            valueRange = 14f..64f
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Tracking", value = "${String.format("%.1f", layer.letterSpacingSp)} sp")
                        AppleSlider(
                            value = layer.letterSpacingSp,
                            onValueChange = { onUpdate(layer.copy(letterSpacingSp = it)) },
                            valueRange = 0f..8f
                        )
                    }
                }

                // Section F: Opacity & Rotation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Opacity", value = "${(layer.opacity * 100).toInt()}%")
                        AppleSlider(
                            value = layer.opacity,
                            onValueChange = { onUpdate(layer.copy(opacity = it)) },
                            valueRange = 0.1f..1f
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Rotation", value = "${layer.rotationDeg.toInt()}°")
                        AppleSlider(
                            value = layer.rotationDeg,
                            onValueChange = { onUpdate(layer.copy(rotationDeg = it)) },
                            valueRange = -180f..180f
                        )
                    }
                }

                // Section G: Blend Mode Selection (matching LayerCard.kt)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Blend Mode", value = layer.blendMode)
                    val blendModes = listOf("Normal", "Screen", "Overlay", "Multiply", "Add")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        blendModes.forEach { mode ->
                            ShapePill(
                                label = mode,
                                active = layer.blendMode == mode,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdate(layer.copy(blendMode = mode))
                                }
                            )
                        }
                    }
                }

                // Section H: Vertical Offset (Position Y)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FieldLabel("Position Y", value = "${layer.offsetY.toInt()} dp")
                    AppleSlider(
                        value = layer.offsetY,
                        onValueChange = { onUpdate(layer.copy(offsetY = it)) },
                        valueRange = -150f..150f
                    )
                }

                // Section I: Effects Toggles (Drop Shadow & Frosted Pill)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Effects & Backdrop")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.glassBg)
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Drop Shadow", color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("Cinematic shadow depth", color = colors.textMuted, fontSize = 9.sp)
                        }
                        SmallSwitch(
                            checked = layer.hasDropShadow,
                            onCheckedChange = { onUpdate(layer.copy(hasDropShadow = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.glassBg)
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Frosted Glass Pill Backdrop", color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("Translucent glass background pill", color = colors.textMuted, fontSize = 9.sp)
                        }
                        SmallSwitch(
                            checked = layer.hasBackgroundPill,
                            onCheckedChange = { onUpdate(layer.copy(hasBackgroundPill = it)) }
                        )
                    }
                }

                // Section J: Center Alignment & Drag Hint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.glassBg)
                        .glossyBorder(RoundedCornerShape(8.dp), colors)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onUpdate(layer.copy(offsetX = 0f, offsetY = 0f))
                            ToastManager.showToast("Title centered on video")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Lucide.Crosshair, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                        Text("Center on Video Canvas", color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text(
                    text = "Tip: Pinch to resize, twist to rotate, or drag anywhere directly on the video preview above.",
                    fontSize = 10.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TitlesTabContent(
    textLayers: List<FcpTextLayer>,
    selectedLayerId: String,
    onSelectLayer: (String) -> Unit,
    onUpdateLayer: (FcpTextLayer) -> Unit,
    onAddLayer: (FcpTitlePresetType?) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onMoveLayer: (Int, Int) -> Unit
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    val activeLayer = textLayers.find { it.id == selectedLayerId } ?: textLayers.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── 1. Top Panel Header (Parity with LayersPanel.kt) ──────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ACTIVE TEXT LAYERS",
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${textLayers.size}",
                            color = colors.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Add Title Glass Button (glossyBorder)
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.glassBg)
                        .glossyBorder(RoundedCornerShape(8.dp), colors)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAddLayer(null)
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Plus,
                            contentDescription = "Add Title",
                            tint = colors.textMain,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Add Title",
                            color = colors.textMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ── 2. Empty State (When textLayers is Empty) ──────────────────────────
        if (textLayers.isEmpty()) {
            item {
                WorkspaceSectionCard(
                    title = "TEXT & TITLE LAYERS",
                    icon = Lucide.Type
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Type,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "No Text on Video",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "Choose a ready-made preset below or tap '+ Add Title' to put text on your video canvas.",
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        PrimaryGradientButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAddLayer(null)
                            },
                            height = 38.dp,
                            contentPadding = PaddingValues(horizontal = 18.dp)
                        ) {
                            Icon(Lucide.Plus, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("+ Add Title Layer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Presets Row in Empty State
            item {
                WorkspaceSectionCard(title = "READY-MADE PROFESSIONAL PRESETS", icon = Lucide.Sparkles) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Tap any preset to instantly add styled text on video:",
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FcpTitlePresetType.values().forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.bg)
                                        .border(0.5.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onAddLayer(preset)
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = preset.category,
                                            fontSize = 9.sp,
                                            color = colors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = preset.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = preset.subtitle,
                                            fontSize = 9.sp,
                                            color = colors.textMuted,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "+ Tap to Add",
                                            fontSize = 9.sp,
                                            color = colors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ── 3. Populated State: List of FcpTitleLayerCard ──────────────────
            itemsIndexed(textLayers, key = { _, l -> l.id }) { index, layer ->
                val isSelected = layer.id == (activeLayer?.id ?: "")
                FcpTitleLayerCard(
                    layer = layer,
                    index = index,
                    totalLayers = textLayers.size,
                    isSelected = isSelected,
                    onSelect = { onSelectLayer(layer.id) },
                    onToggleExpand = {
                        onUpdateLayer(layer.copy(isExpanded = !layer.isExpanded))
                    },
                    onToggleVisibility = {
                        onUpdateLayer(layer.copy(isVisible = !layer.isVisible))
                    },
                    onUpdate = { updated ->
                        onUpdateLayer(updated)
                    },
                    onDuplicate = {
                        onDuplicateLayer(layer.id)
                    },
                    onDelete = {
                        onDeleteLayer(layer.id)
                    },
                    onMoveUp = {
                        if (index > 0) onMoveLayer(index, index - 1)
                    },
                    onMoveDown = {
                        if (index < textLayers.size - 1) onMoveLayer(index, index + 1)
                    }
                )
            }

            // Quick Presets Row at bottom to add more styled titles easily
            item {
                WorkspaceSectionCard(title = "ADD FROM PRESET", icon = Lucide.Sparkles) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Tap any preset below to add another styled title layer:",
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FcpTitlePresetType.values().forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.bg)
                                        .border(0.5.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onAddLayer(preset)
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = preset.category,
                                            fontSize = 8.5.sp,
                                            color = colors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = preset.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "+ Tap to Add",
                                            fontSize = 9.sp,
                                            color = colors.primary,
                                            fontWeight = FontWeight.Bold
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
}

@Composable
private fun FcpTextInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    val colors = LocalOscellionColors.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bg)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        textStyle = TextStyle(
            fontSize = 13.sp,
            color = colors.textMain,
            fontWeight = FontWeight.Medium
        ),
        singleLine = true,
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(placeholder, color = colors.textMuted.copy(alpha = 0.5f), fontSize = 13.sp)
            }
            innerTextField()
        }
    )
}

// ============================================================================
// REUSABLE WORKSPACE COMPONENTS (Matching ChromaStudio Workspace)
// ============================================================================

@Composable
private fun ShapePill(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val shape = remember { RoundedCornerShape(50) }
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (active) colors.onPrimary else colors.textMuted,
        modifier = Modifier
            .clip(shape)
            .background(if (active) colors.primary else colors.glassBg, shape)
            .border(0.5.dp, if (active) colors.primary else colors.glassBorder, shape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

@Composable
private fun AspectRatioBoxIcon(
    aspect: FcpAspectRatio,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val strokeColor = if (active) colors.onPrimary else colors.textMuted
    val fillColor = if (active) colors.onPrimary.copy(alpha = 0.28f) else Color.Transparent

    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val (w, h) = when (aspect) {
            FcpAspectRatio.VERTICAL_9_16 -> 10.dp to 17.dp
            FcpAspectRatio.SQUARE_1_1 -> 14.dp to 14.dp
            FcpAspectRatio.WIDESCREEN_16_9 -> 17.dp to 10.dp
            FcpAspectRatio.CINEMATIC_21_9 -> 19.dp to 8.dp
        }
        Box(
            modifier = Modifier
                .size(w, h)
                .clip(RoundedCornerShape(2.5.dp))
                .background(fillColor)
                .border(1.5.dp, strokeColor, RoundedCornerShape(2.5.dp))
        )
    }
}

@Composable
private fun AspectRatioPill(
    aspect: FcpAspectRatio,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val shape = remember { RoundedCornerShape(50) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (active) colors.primary else colors.glassBg, shape)
            .border(0.5.dp, if (active) colors.primary else colors.glassBorder, shape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AspectRatioBoxIcon(
            aspect = aspect,
            active = active
        )
    }
}

// ============================================================================
// CANVAS BACKGROUND & LETTERBOX FILL ENGINE
// ============================================================================

@Composable
private fun CanvasBgQuickPill(
    background: FcpCanvasBackground,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val shape = remember { RoundedCornerShape(50) }

    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (isActive) colors.primary.copy(alpha = 0.22f) else colors.glassBg, shape)
            .border(
                width = if (isActive) 1.dp else 0.5.dp,
                color = if (isActive) colors.primary else colors.glassBorder,
                shape = shape
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            when (background.type) {
                FcpCanvasBgType.BLUR -> {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color.White, Color(0xFF6366F1))))
                    )
                    Text(
                        text = "Blur",
                        color = if (isActive) colors.primary else colors.textMain,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                FcpCanvasBgType.COLOR -> {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(background.solidColor)
                            .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                    Text(
                        text = "Color",
                        color = if (isActive) colors.primary else colors.textMain,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                FcpCanvasBgType.IMAGE -> {
                    Icon(
                        imageVector = Lucide.Image,
                        contentDescription = null,
                        tint = if (isActive) colors.primary else colors.textMain,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "Image",
                        color = if (isActive) colors.primary else colors.textMain,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasBackgroundLayer(
    background: FcpCanvasBackground,
    blurBitmap: Bitmap?,
    supportsRenderEffect: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (background.type) {
            FcpCanvasBgType.BLUR -> {
                if (blurBitmap != null) {
                    Image(
                        bitmap = blurBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (supportsRenderEffect) {
                                    Modifier.graphicsLayer {
                                        val safeRadius = background.blurRadius.coerceAtLeast(1f)
                                        renderEffect = RenderEffect.createBlurEffect(
                                            safeRadius,
                                            safeRadius,
                                            Shader.TileMode.CLAMP
                                        ).asComposeRenderEffect()
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            )
                    )
                }
                // Ambient contrast scrim so foreground video always pops out
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = background.backdropDim.coerceAtLeast(0.35f)))
                )
            }
            FcpCanvasBgType.COLOR -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background.solidColor)
                )
            }
            FcpCanvasBgType.IMAGE -> {
                if (background.customImageUri != null) {
                    val painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(background.customImageUri)
                            .crossfade(true)
                            .build()
                    )
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val backdrop = AllFcpStudioBackdrops.find { it.id == background.studioBackdropId }
                        ?: AllFcpStudioBackdrops.first()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backdrop.brush)
                    )
                }
                // Optional user-controlled dimming overlay
                if (background.backdropDim > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = background.backdropDim))
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasTabContent(
    background: FcpCanvasBackground,
    onBackgroundChange: (FcpCanvasBackground) -> Unit,
    onPickImage: () -> Unit,
    blurBitmap: Bitmap?,
    isDarkMode: Boolean
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CANVAS FILL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Fill blank gaps, letterboxes & borders",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }
        }

        // 3-Segment Mode Selector (Blur / Solid Color / Custom Image)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDarkMode) Color(0xFF1E1E22) else Color(0xFFE5E7EB))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FcpCanvasBgType.entries.forEach { type ->
                val isSelected = background.type == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (isSelected) {
                                if (isDarkMode) Color(0xFF2C2C34) else Color.White
                            } else Color.Transparent
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBackgroundChange(background.copy(type = type))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) colors.primary else colors.textMuted
                    )
                }
            }
        }

        // Mode Content
        when (background.type) {
            FcpCanvasBgType.BLUR -> {
                // Blur Radius Presets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "BLUR INTENSITY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp
                    )

                    val blurPresets = listOf(
                        15f to "Subtle",
                        35f to "Frosted",
                        65f to "Deep",
                        100f to "Ultra"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        blurPresets.forEach { (radius, label) ->
                            val isSelected = abs(background.blurRadius - radius) < 1f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) colors.primary.copy(alpha = 0.15f)
                                        else (if (isDarkMode) Color(0xFF1A1A1E) else Color(0xFFF3F4F6))
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) colors.primary else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onBackgroundChange(background.copy(blurRadius = radius))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.primary else colors.textMain
                                    )
                                    Text(
                                        text = "${radius.toInt()}px",
                                        fontSize = 9.sp,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Ambient Dark Contrast Scrim
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONTRAST SCRIM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${(background.backdropDim * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                    AppleSlider(
                        value = background.backdropDim,
                        onValueChange = { onBackgroundChange(background.copy(backdropDim = it)) },
                        valueRange = 0.15f..0.75f
                    )
                }
            }

            FcpCanvasBgType.COLOR -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "STUDIO COLOR PALETTE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp
                    )

                    val colorPalette = listOf(
                        Color(0xFF000000) to "Obsidian Black",
                        Color(0xFFFFFFFF) to "Studio White",
                        Color(0xFF0F172A) to "Apple Midnight",
                        Color(0xFF1E293B) to "Deep Slate",
                        Color(0xFF334155) to "Cool Charcoal",
                        Color(0xFF475569) to "Titanium Gray",
                        Color(0xFF581C87) to "Cyber Violet",
                        Color(0xFF1E1B4B) to "Royal Indigo",
                        Color(0xFF0C4A6E) to "Ocean Deep",
                        Color(0xFF064E3B) to "Emerald Forest",
                        Color(0xFF881337) to "Crimson Noir",
                        Color(0xFF9A3412) to "Sunset Amber",
                        Color(0xFF713F12) to "Warm Gold",
                        Color(0xFF292524) to "Warm Stone"
                    )

                    for (rowChunk in colorPalette.chunked(7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowChunk.forEach { (c, name) ->
                                val isSelected = background.solidColor == c
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) colors.primary else Color.White.copy(alpha = 0.25f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onBackgroundChange(background.copy(solidColor = c))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        val iconTint = if (c == Color.White) Color.Black else Color.White
                                        Icon(
                                            imageVector = Lucide.Check,
                                            contentDescription = "Selected",
                                            tint = iconTint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FcpCanvasBgType.IMAGE -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Upload button from device photo gallery
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkMode) Color(0xFF1E1E24) else Color(0xFFF3F4F6))
                            .border(1.dp, colors.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPickImage()
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Upload,
                                    contentDescription = "Upload",
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (background.customImageUri != null) "Change Photo from Gallery" else "Upload from Gallery",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "PNG, JPG, or custom photo",
                                    fontSize = 10.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        if (background.customImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Red.copy(alpha = 0.15f))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onBackgroundChange(background.copy(customImageUri = null))
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Clear",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF453A)
                                )
                            }
                        }
                    }

                    // Studio Backdrop Presets
                    Text(
                        text = "STUDIO BACKDROP PRESETS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AllFcpStudioBackdrops.forEach { preset ->
                            val isSelected = background.customImageUri == null && background.studioBackdropId == preset.id
                            Box(
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(preset.brush)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.5.dp,
                                        color = if (isSelected) colors.primary else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onBackgroundChange(
                                            background.copy(
                                                customImageUri = null,
                                                studioBackdropId = preset.id
                                            )
                                        )
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Text(
                                    text = preset.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 1f), 3f))
                                )
                            }
                        }
                    }

                    // Backdrop Dimmer Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BACKDROP DIMMER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${(background.backdropDim * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }
                        AppleSlider(
                            value = background.backdropDim,
                            onValueChange = { onBackgroundChange(background.copy(backdropDim = it)) },
                            valueRange = 0.0f..0.85f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconGhostButton(
    icon: ImageVector,
    contentDesc: String,
    disabled: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                enabled = !disabled
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val tint = if (disabled) colors.textMuted.copy(alpha = 0.28f) else colors.textMain
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            modifier = Modifier.size(19.dp),
            tint = tint
        )
    }
}

@Composable
private fun TimelineMiniButton(
    icon: ImageVector,
    contentDesc: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val iconTint = when {
        isDanger -> if (isPressed) colors.error else colors.error.copy(alpha = 0.85f)
        isPressed -> colors.primary
        else -> colors.textMain.copy(alpha = 0.85f)
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                if (isPressed) {
                    if (isDanger) colors.error.copy(alpha = 0.15f) else colors.primary.copy(alpha = 0.12f)
                } else Color.Transparent
            )
            .clickable(
                indication = null,
                interactionSource = interactionSource
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = iconTint,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun WorkspaceSectionCard(
    title: String,
    icon: ImageVector,
    collapsible: Boolean = false,
    defaultExpanded: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = LocalOscellionColors.current
    var expanded by remember { mutableStateOf(defaultExpanded) }
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.glassBg, shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 2.5f).coerceAtMost(1f)),
                        colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 0.3f).coerceAtMost(1f))
                    )
                ),
                shape = shape
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (collapsible) Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { expanded = !expanded } else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
            Text(
                text = title,
                color = colors.textMain,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            trailingContent?.invoke()
            if (collapsible) {
                Icon(
                    imageVector = Lucide.ChevronDown,
                    contentDescription = "Expand",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp).rotate(if (expanded) 180f else 0f)
                )
            }
        }

        if (collapsible) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column(Modifier.padding(top = 12.dp)) {
                    content()
                }
            }
        } else {
            Column(Modifier.padding(top = 12.dp)) {
                content()
            }
        }
    }
}

private fun formatTimecode(sec: Float): String {
    val totalSeconds = sec.coerceAtLeast(0f)
    val minutes = (totalSeconds / 60).toInt()
    val remainingSec = (totalSeconds % 60).toInt()
    val hundredths = ((totalSeconds - totalSeconds.toInt()) * 100).toInt()
    return String.format("%02d:%02d.%02d", minutes, remainingSec, hundredths)
}


// ============================================================================
// CLIP TRANSITION DIALOG & EXPORT BOTTOM SHEET
// ============================================================================

@Composable
fun FcpTransitionDialog(
    clip: FcpClip,
    onDismiss: () -> Unit,
    onApplyTransition: (FcpTransitionType, Float) -> Unit
) {
    val colors = LocalOscellionColors.current
    var selectedType by remember { mutableStateOf(clip.transitionToNext) }
    var durationSec by remember { mutableFloatStateOf(clip.transitionDurationSec) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFFFFFFF))
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(24.dp))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Clip Transition",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Text(
                                text = "Select transition effect to the next clip",
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Close",
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Transition options grid (3 columns x 3 rows)
                    val chunkedTransitions = remember { FcpTransitionType.entries.chunked(3) }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunkedTransitions.forEach { rowTypes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTypes.forEach { type ->
                                    val isSelected = type == selectedType
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) colors.primary.copy(alpha = 0.18f)
                                                else if (colors.isDark) Color(0xFF272B38) else Color(0xFFF1F3F7)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) colors.primary else Color.Transparent,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedType = type }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = type.icon,
                                                contentDescription = type.label,
                                                tint = if (isSelected) colors.primary else colors.textMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = type.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) colors.primary else colors.textMain,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                if (rowTypes.size < 3) {
                                    repeat(3 - rowTypes.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    if (selectedType != FcpTransitionType.NONE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = selectedType.description,
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transition Duration",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textMain
                            )
                            Text(
                                text = "${"%.1f".format(durationSec)}s",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }
                        Slider(
                            value = durationSec,
                            onValueChange = { durationSec = it },
                            valueRange = 0.2f..2.0f,
                            steps = 17,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = if (colors.isDark) Color(0xFF333846) else Color(0xFFE2E8F0)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.glassBorder)
                        ) {
                            Text("Cancel", color = colors.textMuted)
                        }
                        Button(
                            onClick = {
                                onApplyTransition(selectedType, durationSec)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FcpExportSheet(
    totalDurationSec: Float,
    clipCount: Int,
    textLayerCount: Int,
    aspectRatio: FcpAspectRatio,
    customAudio: FcpCustomAudio? = null,
    isExporting: Boolean,
    exportProgress: Float,
    onStartExport: (String, String) -> Unit,
    onSaveDraftOnly: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    var selectedRes by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableStateOf("60 FPS") }
    val resolutions = listOf("720p", "1080p", "4K")
    val frameRates = listOf("24 FPS", "30 FPS", "60 FPS")

    Dialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { if (!isExporting) onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(if (colors.isDark) Color(0xFF181A22) else Color(0xFFFFFFFF))
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.textMuted.copy(alpha = 0.3f))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Export & Render",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Text(
                                text = "Chroma Studio Final Cut Pro Master Engine",
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                        if (!isExporting) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = "Close",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Project Summary Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (colors.isDark) Color(0xFF222633) else Color(0xFFF3F5F9))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val durMin = (totalDurationSec / 60).toInt()
                            val durSec = (totalDurationSec % 60).toInt()
                            val timeStr = "%02d:%02d".format(durMin, durSec)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Duration", fontSize = 11.sp, color = colors.textMuted)
                                Text(timeStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Clips", fontSize = 11.sp, color = colors.textMuted)
                                Text("$clipCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Titles", fontSize = 11.sp, color = colors.textMuted)
                                Text("$textLayerCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Format", fontSize = 11.sp, color = colors.textMuted)
                                Text(aspectRatio.label.split(" ").firstOrNull() ?: "9:16", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                            }
                        }
                    }

                    // Active Custom Soundtrack Badge (if attached)
                    if (customAudio != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF064E3B).copy(alpha = 0.45f))
                                .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (customAudio.isExtractedFromVideo) Lucide.Film else Lucide.Music,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customAudio.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMain,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (customAudio.muteOriginalVideo) "Replaces video audio (original muted)" else "Mixed with original video audio",
                                    fontSize = 10.sp,
                                    color = Color(0xFF34D399)
                                )
                            }
                            if (customAudio.loopAudio) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.25f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Looping", fontSize = 8.5.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (isExporting) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Rendering frames...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "${(exportProgress * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { exportProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = colors.primary,
                                trackColor = if (colors.isDark) Color(0xFF2E3444) else Color(0xFFE2E8F0)
                            )
                            Text(
                                text = "Encoding video stream with color matrix and audio mix",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    } else {
                        // Resolution selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Resolution", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                resolutions.forEach { res ->
                                    val isSel = res == selectedRes
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSel) colors.primary.copy(alpha = 0.18f)
                                                else if (colors.isDark) Color(0xFF222633) else Color(0xFFF3F5F9)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) colors.primary else Color.Transparent,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedRes = res }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = res,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) colors.primary else colors.textMain
                                        )
                                    }
                                }
                            }
                        }

                        // Frame rate selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Frame Rate", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                frameRates.forEach { fps ->
                                    val isSel = fps == selectedFps
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSel) colors.primary.copy(alpha = 0.18f)
                                                else if (colors.isDark) Color(0xFF222633) else Color(0xFFF3F5F9)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) colors.primary else Color.Transparent,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedFps = fps }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = fps,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) colors.primary else colors.textMain
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onSaveDraftOnly,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, colors.glassBorder)
                            ) {
                                Icon(
                                    imageVector = Lucide.FileText,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Draft", color = colors.textMain, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { onStartExport(selectedRes, selectedFps) },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                            ) {
                                Icon(
                                    imageVector = Lucide.Share2,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// FLAT WHITE PLAYBACK ICONS & TRANSPORT CONTROLS
// (100% Vector Geometry matching User HTML/CSS Mockup)
// ============================================================================

enum class FlatPlaybackIconType {
    PLAY,
    PAUSE,
    REPLAY,
    RETRY
}

@Composable
fun FlatPlaybackIcon(
    type: FlatPlaybackIconType,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val playPathData = "M148.5,0C66.486,0,0,66.486,0,148.5S66.486,297,148.5,297S297,230.514,297,148.5S230.514,0,148.5,0z M202.79,161.734l-78.501,45.322c-2.421,1.398-5.326,2.138-8.083,2.138c-8.752,0-16.039-7.12-16.039-15.872v-90.645c0-8.752,7.287-15.872,16.039-15.872c2.758,0,5.579,0.739,8.001,2.138l78.542,45.322c4.966,2.867,7.951,8.001,7.951,13.734S207.756,158.867,202.79,161.734z"
    val pausePathData = "M148.5,0C66.486,0,0,66.486,0,148.5S66.486,297,148.5,297S297,230.514,297,148.5S230.514,0,148.5,0z M116,92 a14,14 0 0 1 14,14 v85 a14,14 0 0 1 -28,0 v-85 a14,14 0 0 1 14,-14 z M181,92 a14,14 0 0 1 14,14 v85 a14,14 0 0 1 -28,0 v-85 a14,14 0 0 1 14,-14 z"
    val replayMaskPathData = "M86 148.5 A 62.5 62.5 0 1 0 106 102 L 78 128 M78 86 v 42 h 42"
    val retryMaskPathData = "M228 108 A 82 82 0 0 0 86 122 M 84 80 L 84 124 L 128 124 M 69 189 A 82 82 0 0 0 211 175 M 213 217 L 213 173 L 169 173"

    val composePath = remember(type) {
        try {
            when (type) {
                FlatPlaybackIconType.PLAY -> {
                    val p = PathParser.createPathFromPathData(playPathData)
                    p.fillType = android.graphics.Path.FillType.EVEN_ODD
                    p.asComposePath()
                }
                FlatPlaybackIconType.PAUSE -> {
                    val p = PathParser.createPathFromPathData(pausePathData)
                    p.fillType = android.graphics.Path.FillType.EVEN_ODD
                    p.asComposePath()
                }
                FlatPlaybackIconType.REPLAY -> {
                    val p = PathParser.createPathFromPathData(replayMaskPathData)
                    p.asComposePath()
                }
                FlatPlaybackIconType.RETRY -> {
                    val p = PathParser.createPathFromPathData(retryMaskPathData)
                    p.asComposePath()
                }
            }
        } catch (_: Throwable) {
            androidx.compose.ui.graphics.Path()
        }
    }

    Canvas(
        modifier = modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val scale = size.minDimension / 297f
        val strokeStyle = Stroke(
            width = 26f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        drawContext.canvas.save()
        drawContext.canvas.scale(scale, scale)

        when (type) {
            FlatPlaybackIconType.PLAY, FlatPlaybackIconType.PAUSE -> {
                drawPath(
                    path = composePath,
                    color = tint
                )
            }
            FlatPlaybackIconType.REPLAY, FlatPlaybackIconType.RETRY -> {
                drawCircle(
                    color = tint,
                    radius = 148.5f,
                    center = Offset(148.5f, 148.5f)
                )
                drawPath(
                    path = composePath,
                    color = Color.Transparent,
                    style = strokeStyle,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )
            }
        }

        drawContext.canvas.restore()
    }
}

@Composable
fun PlaybackTransportControlBar(
    isPlaying: Boolean,
    isAutoReplay: Boolean,
    onPlayPauseToggle: () -> Unit,
    onReplay: () -> Unit,
    onAutoReplayToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Replay interaction & scale
    val replayInteraction = remember { MutableInteractionSource() }
    val isReplayPressed by replayInteraction.collectIsPressedAsState()
    val replayScale by animateFloatAsState(
        targetValue = if (isReplayPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "replayScale"
    )

    // Play / Pause toggle interaction & scale
    val playPauseInteraction = remember { MutableInteractionSource() }
    val isPlayPausePressed by playPauseInteraction.collectIsPressedAsState()
    val playPauseScale by animateFloatAsState(
        targetValue = if (isPlayPausePressed) 0.90f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "playPauseScale"
    )

    // Retry / Auto-replay toggle interaction, scale & spin animation
    val retryInteraction = remember { MutableInteractionSource() }
    val isRetryPressed by retryInteraction.collectIsPressedAsState()
    val retryScale by animateFloatAsState(
        targetValue = if (isRetryPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "retryScale"
    )
    var spinDegrees by remember { mutableFloatStateOf(0f) }
    val animatedSpin by animateFloatAsState(
        targetValue = spinDegrees,
        animationSpec = tween(durationMillis = 450, easing = LinearOutSlowInEasing),
        label = "retrySpin"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Replay Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(replayScale)
                .clickable(
                    interactionSource = replayInteraction,
                    indication = null
                ) {
                    SoundManager.playTap(context)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onReplay()
                },
            contentAlignment = Alignment.Center
        ) {
            FlatPlaybackIcon(
                type = FlatPlaybackIconType.REPLAY,
                modifier = Modifier.size(44.dp),
                tint = Color.White
            )
        }

        // 2. Unified Play / Pause Toggle Button
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(playPauseScale)
                .clickable(
                    interactionSource = playPauseInteraction,
                    indication = null
                ) {
                    SoundManager.playTap(context)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPlayPauseToggle()
                },
            contentAlignment = Alignment.Center
        ) {
            FlatPlaybackIcon(
                type = if (isPlaying) FlatPlaybackIconType.PAUSE else FlatPlaybackIconType.PLAY,
                modifier = Modifier.size(56.dp),
                tint = Color.White
            )
        }

        // 3. Retry / Auto-Playback Toggle Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(retryScale)
                .clickable(
                    interactionSource = retryInteraction,
                    indication = null
                ) {
                    SoundManager.playTap(context)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    spinDegrees -= 360f
                    onAutoReplayToggle()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FlatPlaybackIcon(
                    type = FlatPlaybackIconType.RETRY,
                    modifier = Modifier
                        .size(44.dp)
                        .rotate(animatedSpin),
                    tint = if (isAutoReplay) Color.White else Color.White.copy(alpha = 0.40f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isAutoReplay) Color(0xFF007AFF) else Color.Transparent)
                )
            }
        }
    }
}

// ============================================================================
// EXIT CONFIRMATION DIALOG (Trash Screen Style)
// ============================================================================

@Composable
fun FcpExitConfirmationDialog(
    onSaveDraftAndExit: () -> Unit,
    onDiscardAndExit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val scaleAnim = remember { Animatable(0.94f) }
        val alphaAnim = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            launch {
                scaleAnim.animateTo(
                    1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                alphaAnim.animateTo(1f, tween(180, easing = LinearOutSlowInEasing))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f * alphaAnim.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                    }
                    .shadow(32.dp, RoundedCornerShape(16.dp), spotColor = Color.Black)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E212B))
                    .border(1.dp, Color(0xFF2A2F3A), RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Video Graphic Header
                Column(
                    modifier = Modifier.padding(top = 22.dp, bottom = 14.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppleVideosIcon(modifier = Modifier.size(68.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Exit Video Editor?",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "You have unsaved changes in this project. Do you want to save before leaving?",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }

                HorizontalDivider(color = Color(0xFF2A2F3A), thickness = 0.5.dp)

                // Actions:
                // 1. Solid Blue Save Draft & Exit Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(Color(0xFF007AFF))
                        .clickable { onSaveDraftAndExit() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save Draft & Exit",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(color = Color(0xFF2A2F3A), thickness = 0.5.dp)

                // 2. Discard Changes Button (Destructive Red)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable { onDiscardAndExit() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Discard Changes",
                        color = Color(0xFFFF3B30),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                HorizontalDivider(color = Color(0xFF2A2F3A), thickness = 0.5.dp)

                // 3. Keep Editing Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keep Editing",
                        color = Color(0xFF8E98A8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
