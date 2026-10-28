@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.chroma.studio.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.DraftPostRepository
import com.chroma.studio.data.FlashUploadManager
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.ChromaWork
import com.chroma.studio.model.CreatorPostDraft
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.AppleTrashBinIcon
import com.chroma.studio.ui.components.rememberAppleTrashState
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.ui.components.PrimaryGradientButton
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.FlashViewModel
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ============================================================================
// DATA MODELS & ENUMS (V3 UNIFIED CREATOR STUDIO SPEC)
// ============================================================================

enum class CreatorStudioMode {
    POST,  // Photo Carousel & Collage Post (community_posts)
    FLASH, // 9:16 Vertical Short Reel (flash_videos)
    WORK   // Chroma Studio Saved Vector Work (community_posts)
}

enum class StudioStep(val stepNumber: Int, val title: String) {
    STEP_1_MEDIA(1, "Step 1 · Media & Edit"),
    STEP_2_ASPECT_COVER(2, "Step 2 · Aspect & Cover"),
    STEP_3_DETAILS(3, "Step 3 · Details & Meta"),
    STEP_4_UPLOAD(4, "Step 4 · Upload & Share")
}

/**
 * Step 2 Tabbed Editor Tabs (v3 spec: Trim/Sound/Cover for Flash, Crop/Filters/Adjust for Photo, Frame for Work)
 */
enum class StudioEditTab(val id: String, val label: String, val icon: ImageVector) {
    TRIM("trim", "Trim", Lucide.Scissors),
    SOUND("sound", "Sound", Lucide.Music),
    COVER("cover", "Cover", Lucide.Image),
    CROP("crop", "Crop", Lucide.Crop),
    FILTERS("filters", "Filters", Lucide.Sparkles),
    ADJUST("adjust", "Adjust", Lucide.SlidersHorizontal),
    FRAME("frame", "Frame", Lucide.Crop)
}

data class AudioTrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val duration: String,
    val bpm: Int
)

val FEATURED_SOUNDTRACKS = listOf(
    AudioTrackItem("1", "Alpine Breeze", "Komorebi Beats", "Lo-Fi Chill", "02:14", 128),
    AudioTrackItem("2", "Coastal Sunset", "Aura Wave", "Ambient Wave", "01:45", 96),
    AudioTrackItem("3", "Neon Horizon", "Synth Pulse", "Synthwave", "02:30", 118),
    AudioTrackItem("4", "Serene Pulse", "MonoSound", "Minimalist", "01:58", 85),
    AudioTrackItem("5", "Lagoon Shimmer", "Echo Bay", "Acoustic", "01:30", 105)
)

data class StudioSlide(
    val id: String = UUID.randomUUID().toString(),
    val imageUri: Uri? = null,
    val imageUrl: String? = null,
    val work: ChromaWork? = null,
    val videoUri: Uri? = null,
    val rotationDegrees: Float = 0f,
    val brightness: Float = 0f,    // -100 to 100
    val contrast: Float = 1f,      // 0.5 to 2.0
    val saturation: Float = 1f,    // 0 to 3.0
    val warmth: Float = 0f,        // -100 to 100
    val vignette: Float = 0f,      // 0 to 100
    val sharpness: Float = 0f,     // 0 to 100
    val filterName: String = "Original",
    val label: String = "Slide"
)

data class StudioRatioConfig(
    val id: String,
    val label: String,
    val description: String,
    val wRatio: Float,
    val hRatio: Float,
    val icon: ImageVector,
    val boxWidthDp: Dp,
    val boxHeightDp: Dp
)

val STUDIO_RATIO_CONFIGS = listOf(
    StudioRatioConfig("1:1", "1:1", "Square (1:1)", 1f, 1f, Lucide.Square, 28.dp, 28.dp),
    StudioRatioConfig("4:5", "4:5", "Portrait (4:5)", 4f, 5f, Lucide.Frame, 24.dp, 30.dp),
    StudioRatioConfig("16:9", "16:9", "Landscape (16:9)", 16f, 9f, Lucide.RectangleHorizontal, 32.dp, 18.dp),
    StudioRatioConfig("9:16", "9:16", "Story/Flash (9:16)", 9f, 16f, Lucide.Smartphone, 18.dp, 32.dp),
    StudioRatioConfig("4:3", "4:3", "Classic (4:3)", 4f, 3f, Lucide.Tv, 30.dp, 23.dp)
)

val ALGO_CATEGORIES = listOf(
    "Art",
    "Design",
    "Motion",
    "3D",
    "Photography",
    "Code",
    "Music",
    "Science",
    "Educational",
    "Inspiration",
    "Casually",
    "Others..."
)

val PHOTO_FILTER_PRESETS = listOf(
    "Original",
    "Vivid",
    "Warm",
    "Noir",
    "Cyber",
    "Pastel",
    "Sunset",
    "Emerald",
    "Chrome",
    "Vintage",
    "Mono",
    "Drama"
)

/**
 * Pre-defined Recent Media Items for Step 1 Recents Grids (v3 spec)
 */
data class RecentMediaItem(
    val id: String,
    val url: String,
    val durationText: String? = null,
    val isVideo: Boolean = false,
    val title: String = ""
)

val RECENT_FLASH_PRESETS = listOf(
    RecentMediaItem("rec_f1", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80", "15s", true, "Alpine Lake"),
    RecentMediaItem("rec_f2", "https://images.unsplash.com/photo-1519046904884-53103b34b206?auto=format&fit=crop&w=600&q=80", "08s", true, "Sunset Coast"),
    RecentMediaItem("rec_f3", "https://images.unsplash.com/photo-1499793983690-e29da59ef1c2?auto=format&fit=crop&w=600&q=80", "24s", true, "Azure Wave"),
    RecentMediaItem("rec_f4", "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?auto=format&fit=crop&w=600&q=80", "30s", true, "Highway Motion")
)

val RECENT_PHOTO_PRESETS = listOf(
    RecentMediaItem("rec_p1", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80", null, false, "Lakeview"),
    RecentMediaItem("rec_p2", "https://images.unsplash.com/photo-1519046904884-53103b34b206?auto=format&fit=crop&w=600&q=80", null, false, "Dusk"),
    RecentMediaItem("rec_p3", "https://images.unsplash.com/photo-1499793983690-e29da59ef1c2?auto=format&fit=crop&w=600&q=80", null, false, "Beach Villa"),
    RecentMediaItem("rec_p4", "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?auto=format&fit=crop&w=600&q=80", null, false, "Canyon"),
    RecentMediaItem("rec_p5", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=600&q=80", null, false, "Valley"),
    RecentMediaItem("rec_p6", "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?auto=format&fit=crop&w=600&q=80", null, false, "Urban")
)

/**
 * Share Target Item for Step 4 Share-to Sheet (v3 spec)
 */
data class ShareTargetItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val bgTint: Color,
    val iconTint: Color
)

val SHARE_TARGETS = listOf(
    ShareTargetItem("link", "Copy link", Lucide.Link, Color(0xFFEFF6FF), Color(0xFF2563EB)),
    ShareTargetItem("story", "Story", Lucide.CirclePlus, Color(0xFFFFF1F2), Color(0xFFF43F5E)),
    ShareTargetItem("message", "Message", Lucide.MessageCircle, Color(0xFFECFDF5), Color(0xFF10B981)),
    ShareTargetItem("email", "Email", Lucide.Mail, Color(0xFFF5F3FF), Color(0xFF8B5CF6)),
    ShareTargetItem("more", "More", Lucide.Share2, Color(0xFFF1F5F9), Color(0xFF64748B))
)

fun formatDurationTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format("%02d:%02d", mins, secs)
}

// ────────────────────────────────────────────────────────────────────────────
// HELPER COMPOSABLES: PREVIEWS, OVERLAYS & SETTING ROWS
// ────────────────────────────────────────────────────────────────────────────

@Composable
fun SlidePreviewContent(
    slide: StudioSlide,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            slide.work != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F4C64), Color(0xFF38B8D1), Color(0xFFF5882E))
                            )
                        )
                )
            }
            slide.imageUri != null -> {
                AsyncImage(
                    model = slide.imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = slide.rotationDegrees
                        },
                    contentScale = contentScale
                )
            }
            !slide.imageUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = slide.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = slide.rotationDegrees
                        },
                    contentScale = contentScale
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F4C64), Color(0xFF38B8D1), Color(0xFFF5882E))
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun ThirdsGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.dp.toPx()
        val gridColor = Color.White.copy(alpha = 0.5f)

        // Vertical lines
        val x1 = size.width / 3f
        val x2 = size.width * 2f / 3f
        drawLine(color = gridColor, start = Offset(x1, 0f), end = Offset(x1, size.height), strokeWidth = strokeWidth)
        drawLine(color = gridColor, start = Offset(x2, 0f), end = Offset(x2, size.height), strokeWidth = strokeWidth)

        // Horizontal lines
        val y1 = size.height / 3f
        val y2 = size.height * 2f / 3f
        drawLine(color = gridColor, start = Offset(0f, y1), end = Offset(size.width, y1), strokeWidth = strokeWidth)
        drawLine(color = gridColor, start = Offset(0f, y2), end = Offset(size.width, y2), strokeWidth = strokeWidth)
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF232838) else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) colors.primary else colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = colors.textMain,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (colors.isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}


// ============================================================================
// MAIN COMPOSABLE: CreatorPostOnboardingStudioScreen
// ============================================================================

@Composable
fun CreatorPostOnboardingStudioScreen(
    initialMode: CreatorStudioMode = CreatorStudioMode.POST,
    initialDraftId: String? = null,
    initialImportedUris: List<Uri> = emptyList(),
    workRepository: WorkRepository,
    discoverRepository: DiscoverRepository = remember { DiscoverRepository() },
    flashViewModel: FlashViewModel? = null,
    onNavigateBack: () -> Unit,
    onPostSuccess: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colors = LocalOscellionColors.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val draftRepo = remember { DraftPostRepository(context) }

    // Core Screen State
    var studioMode by remember { mutableStateOf(initialMode) }
    var currentStep by remember { mutableStateOf(StudioStep.STEP_1_MEDIA) }
    var activeEditTab by remember { mutableStateOf(StudioEditTab.CROP) }
    var isComparingOriginal by remember { mutableStateOf(false) }
    var isAdvancedSettingsOpen by remember { mutableStateOf(false) }
    var isAltTextOpen by remember { mutableStateOf(false) }
    var currentDraftId by remember { mutableStateOf(initialDraftId) }

    // Multi-slides & active slide index
    val slides = remember { mutableStateListOf<StudioSlide>() }
    var currentSlideIndex by remember { mutableIntStateOf(0) }

    // Aspect ratio & framing
    var selectedRatioId by remember {
        mutableStateOf(if (initialMode == CreatorStudioMode.FLASH) "9:16" else "4:3")
    }
    var showThirdsGrid by remember { mutableStateOf(false) }
    var showWatermark by remember { mutableStateOf(false) }
    var cropOffsetX by remember { mutableFloatStateOf(50f) } // 0..100%
    var cropOffsetY by remember { mutableFloatStateOf(50f) } // 0..100%

    // Cover picker states (Flash / Photo)
    var coverPickerMode by remember { mutableStateOf("frame") } // "frame" or "custom"
    var customThumbUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCoverFrameMs by remember { mutableLongStateOf(0L) }
    var coverThumbnailBmp by remember { mutableStateOf<Bitmap?>(null) }

    // v2 Oscellion Dynamic Island & Modals State
    var isIslandExpanded by remember { mutableStateOf(false) }
    var showAudioMixerModal by remember { mutableStateOf(false) }
    var origAudioVolume by remember { mutableFloatStateOf(100f) }
    var soundtrackVolume by remember { mutableFloatStateOf(75f) }
    var selectedSoundtrack by remember { mutableStateOf<AudioTrackItem?>(FEATURED_SOUNDTRACKS.firstOrNull()) }
    var showDiscardModal by remember { mutableStateOf(false) }
    var showFullPostModal by remember { mutableStateOf(false) }
    var scheduleReleaseEnabled by remember { mutableStateOf(false) }
    var scheduleDateTime by remember { mutableStateOf("Tomorrow, 10:00 AM") }
    var remixAttributionEnabled by remember { mutableStateOf(true) }

    // Post details & metadata
    var postTitle by remember { mutableStateOf("") }
    var postDescription by remember { mutableStateOf("") }
    var selectedCategoryTag by remember { mutableStateOf<String?>(null) }
    var customCategoryTag by remember { mutableStateOf("") }
    var locationString by remember { mutableStateOf("Zurich, Switzerland") }
    var altTextString by remember { mutableStateOf("") }
    val hashtags = remember { mutableStateListOf<String>() }
    val mentionedUsers = remember { mutableStateListOf<UserProfile>() }

    // Privacy & Audience
    var audienceVisibility by remember { mutableStateOf("public") } // "public", "followers", "friends", "private"
    var hideLikesCount by remember { mutableStateOf(false) }
    var turnOffComments by remember { mutableStateOf(false) }
    var turnOffRemix by remember { mutableStateOf(false) }
    var includeOriginalAudio by remember { mutableStateOf(true) }
    var allowDownloads by remember { mutableStateOf(true) }
    var highQualityUpload by remember { mutableStateOf(true) }

    // Flash-specific states
    var flashTrimStartMs by remember { mutableLongStateOf(0L) }
    var flashTrimEndMs by remember { mutableLongStateOf(15000L) }
    var flashVideoDurationMs by remember { mutableLongStateOf(15000L) }
    var flashPlaybackSpeed by remember { mutableFloatStateOf(1.0f) }
    var flashIsMuted by remember { mutableStateOf(false) }
    var flashIsFlipped by remember { mutableStateOf(false) }

    // Work-specific states
    var workShowGrain by remember { mutableStateOf(false) }
    var workMeshAngle by remember { mutableFloatStateOf(0f) }
    var workSearchQuery by remember { mutableStateOf("") }
    var workIsSeriesMode by remember { mutableStateOf(false) }

    // Modals & Drawers
    var showPhotoStudioModal by remember { mutableStateOf(false) }
    var showVideoStudioModal by remember { mutableStateOf(false) }
    var showInspectorModal by remember { mutableStateOf(false) }
    var showDraftsDrawer by remember { mutableStateOf(false) }
    var showMentionModal by remember { mutableStateOf(false) }
    var safetyViolationResult by remember { mutableStateOf<com.chroma.studio.ml.safety.SafetyScanResult?>(null) }
    var savedDraftsList by remember { mutableStateOf<List<CreatorPostDraft>>(emptyList()) }

    // Upload & completion states
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStage by remember { mutableIntStateOf(1) } // 1: Safety, 2: Transcode, 3: Storage, 4: Firestore
    var isUploadFinished by remember { mutableStateOf(false) }
    var generatedPostId by remember { mutableStateOf("") }
    var generatedDeepLink by remember { mutableStateOf("") }
    var uploadErrorMessage by remember { mutableStateOf<String?>(null) }

    // Refresh drafts list from repo
    fun refreshDrafts() {
        scope.launch(Dispatchers.IO) {
            val list = draftRepo.getAllDrafts()
            withContext(Dispatchers.Main) {
                savedDraftsList = list
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshDrafts()
    }

    // Auto-probe actual video duration when video slide is available
    LaunchedEffect(slides.firstOrNull()?.videoUri) {
        val vUri = slides.firstOrNull()?.videoUri ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val dur = com.chroma.studio.utils.VideoProcessor.getVideoDurationMs(context, vUri)
            if (dur > 0L) {
                withContext(Dispatchers.Main) {
                    flashVideoDurationMs = dur
                    if (flashTrimEndMs <= 0L || flashTrimEndMs == 15000L || flashTrimEndMs > dur) {
                        flashTrimEndMs = dur.coerceAtMost(30000L)
                    }
                }
            }
        }
    }

    // Extract cover thumbnail bitmap for preview
    LaunchedEffect(customThumbUri, selectedCoverFrameMs, slides.firstOrNull()?.videoUri) {
        val vUri = slides.firstOrNull()?.videoUri
        if (customThumbUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(customThumbUri!!)
                    val bmp = BitmapFactory.decodeStream(stream)
                    withContext(Dispatchers.Main) { coverThumbnailBmp = bmp }
                } catch (_: Throwable) {
                    withContext(Dispatchers.Main) { coverThumbnailBmp = null }
                }
            }
        } else if (vUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, vUri)
                    val timeUs = selectedCoverFrameMs * 1000L
                    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 360, 480)
                    } else {
                        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                    retriever.release()
                    withContext(Dispatchers.Main) { coverThumbnailBmp = bmp }
                } catch (_: Throwable) {
                    withContext(Dispatchers.Main) { coverThumbnailBmp = null }
                }
            }
        }
    }

    // Pickers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val newSlide = StudioSlide(
                imageUri = uri,
                label = "Slide ${slides.size + 1}"
            )
            slides.add(newSlide)
            currentSlideIndex = slides.lastIndex
        }
    }

    val multipleImagesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEachIndexed { idx, u ->
                slides.add(StudioSlide(imageUri = u, label = "Slide ${slides.size + 1}"))
            }
            currentSlideIndex = slides.lastIndex
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            slides.clear()
            slides.add(StudioSlide(videoUri = uri, label = "Flash Reel"))
            currentSlideIndex = 0
            selectedRatioId = "9:16"
        }
    }

    val customCoverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customThumbUri = uri
            coverPickerMode = "custom"
        }
    }

    // Load Draft if provided, or initialize from imported URIs
    LaunchedEffect(initialDraftId, initialImportedUris) {
        if (initialImportedUris.isNotEmpty()) {
            slides.clear()
            initialImportedUris.forEachIndexed { idx, uri ->
                slides.add(StudioSlide(videoUri = uri, label = "Clip ${idx + 1}"))
            }
            if (slides.isNotEmpty()) currentSlideIndex = 0
            studioMode = CreatorStudioMode.FLASH
            selectedRatioId = "9:16"
        } else if (!initialDraftId.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                val draft = draftRepo.getDraftById(initialDraftId)
                if (draft != null) {
                    withContext(Dispatchers.Main) {
                        currentDraftId = draft.id
                        studioMode = when (draft.mode.lowercase()) {
                            "flash" -> CreatorStudioMode.FLASH
                            "work" -> CreatorStudioMode.WORK
                            else -> CreatorStudioMode.POST
                        }
                        postTitle = draft.title
                        postDescription = draft.description
                        selectedRatioId = draft.aspectRatio
                        selectedCategoryTag = draft.categoryTag
                        hashtags.clear()
                        hashtags.addAll(draft.hashtags)
                        mentionedUsers.clear()
                        draft.mentions.forEach { uname ->
                            mentionedUsers.add(UserProfile(uid = "", username = uname, displayName = uname))
                        }
                        hideLikesCount = draft.hideLikeCount
                        turnOffComments = draft.turnOffComments
                        turnOffRemix = draft.turnOffRemix
                        flashTrimStartMs = draft.flashTrimStartMs
                        flashTrimEndMs = draft.flashTrimEndMs
                        draft.customCoverUri?.let { customThumbUri = Uri.parse(it) }
                        selectedCoverFrameMs = draft.selectedCoverFrameMs
                        audienceVisibility = draft.audienceVisibility
                        includeOriginalAudio = draft.includeOriginalAudio
                        allowDownloads = draft.allowDownloads
                        highQualityUpload = draft.highQualityUpload

                        slides.clear()
                        when (draft.mode.lowercase()) {
                            "flash" -> {
                                draft.videoUri?.let { uriStr ->
                                    slides.add(StudioSlide(videoUri = Uri.parse(uriStr), label = "Flash Video"))
                                }
                            }
                            "work" -> {
                                draft.workIds.forEachIndexed { idx, wid ->
                                    val work = workRepository.getWorkById(wid)
                                    if (work != null) {
                                        slides.add(StudioSlide(work = work, label = "Work ${idx + 1}"))
                                    }
                                }
                            }
                            else -> {
                                draft.imageUris.forEachIndexed { idx, uriStr ->
                                    slides.add(StudioSlide(imageUri = Uri.parse(uriStr), label = "Slide ${idx + 1}"))
                                }
                            }
                        }
                        if (slides.isNotEmpty()) currentSlideIndex = 0
                    }
                }
            }
        }
    }

    // Function to save current studio state as Draft
    fun saveCurrentDraft(showToast: Boolean = true): String {
        val draftId = currentDraftId ?: UUID.randomUUID().toString()
        currentDraftId = draftId
        val modeStr = when (studioMode) {
            CreatorStudioMode.POST -> "post"
            CreatorStudioMode.FLASH -> "flash"
            CreatorStudioMode.WORK -> "work"
        }

        val imgUris = slides.mapNotNull { it.imageUri?.toString() }
        val vUri = slides.firstOrNull { it.videoUri != null }?.videoUri?.toString()
        val wIds = slides.mapNotNull { it.work?.id }

        val draft = CreatorPostDraft(
            id = draftId,
            mode = modeStr,
            title = postTitle,
            description = postDescription,
            aspectRatio = selectedRatioId,
            categoryTag = selectedCategoryTag ?: customCategoryTag.ifBlank { null },
            hashtags = hashtags.toList(),
            mentions = mentionedUsers.map { it.username },
            hideLikeCount = hideLikesCount,
            turnOffComments = turnOffComments,
            turnOffRemix = turnOffRemix,
            imageUris = imgUris,
            videoUri = vUri,
            workIds = wIds,
            flashTrimStartMs = flashTrimStartMs,
            flashTrimEndMs = flashTrimEndMs,
            timestamp = System.currentTimeMillis(),
            customCoverUri = customThumbUri?.toString(),
            selectedCoverFrameMs = selectedCoverFrameMs,
            audienceVisibility = audienceVisibility,
            includeOriginalAudio = includeOriginalAudio,
            allowDownloads = allowDownloads,
            highQualityUpload = highQualityUpload
        )

        draftRepo.saveDraft(draft)
        refreshDrafts()
        if (showToast) {
            Toast.makeText(context, "Draft saved successfully", Toast.LENGTH_SHORT).show()
        }
        return draftId
    }

    // Function to restore a selected draft
    fun loadDraft(draft: CreatorPostDraft) {
        currentDraftId = draft.id
        studioMode = when (draft.mode.lowercase()) {
            "flash" -> CreatorStudioMode.FLASH
            "work" -> CreatorStudioMode.WORK
            else -> CreatorStudioMode.POST
        }
        postTitle = draft.title
        postDescription = draft.description
        selectedRatioId = draft.aspectRatio
        selectedCategoryTag = draft.categoryTag
        hashtags.clear()
        hashtags.addAll(draft.hashtags)
        mentionedUsers.clear()
        draft.mentions.forEach { uname ->
            mentionedUsers.add(UserProfile(uid = "", username = uname, displayName = uname))
        }
        hideLikesCount = draft.hideLikeCount
        turnOffComments = draft.turnOffComments
        turnOffRemix = draft.turnOffRemix
        flashTrimStartMs = draft.flashTrimStartMs
        flashTrimEndMs = draft.flashTrimEndMs
        draft.customCoverUri?.let { customThumbUri = Uri.parse(it) }
        selectedCoverFrameMs = draft.selectedCoverFrameMs
        audienceVisibility = draft.audienceVisibility
        includeOriginalAudio = draft.includeOriginalAudio
        allowDownloads = draft.allowDownloads
        highQualityUpload = draft.highQualityUpload

        slides.clear()
        when (draft.mode.lowercase()) {
            "flash" -> {
                draft.videoUri?.let { uriStr ->
                    slides.add(StudioSlide(videoUri = Uri.parse(uriStr), label = "Flash Video"))
                }
            }
            "work" -> {
                draft.workIds.forEachIndexed { idx, wid ->
                    val work = workRepository.getWorkById(wid)
                    if (work != null) {
                        slides.add(StudioSlide(work = work, label = "Work ${idx + 1}"))
                    }
                }
            }
            else -> {
                draft.imageUris.forEachIndexed { idx, uriStr ->
                    slides.add(StudioSlide(imageUri = Uri.parse(uriStr), label = "Slide ${idx + 1}"))
                }
            }
        }
        if (slides.isNotEmpty()) currentSlideIndex = 0
        showDraftsDrawer = false
        currentStep = StudioStep.STEP_1_MEDIA
    }

    // Step 4 Cloud Upload Pipeline
    fun startUploadPipeline() {
        val savedDraftId = saveCurrentDraft(showToast = false)
        currentStep = StudioStep.STEP_4_UPLOAD
        uploadProgress = 0.05f
        uploadStage = 1
        isUploadFinished = false
        uploadErrorMessage = null

        val newId = "post_" + UUID.randomUUID().toString().take(8)
        generatedPostId = newId
        val baseUrl = if (studioMode == CreatorStudioMode.FLASH) "https://oscellion.vercel.app/flash/" else "https://oscellion.vercel.app/post/"
        generatedDeepLink = baseUrl + newId

        scope.launch(Dispatchers.IO) {
            try {
                // STAGE 1: Safety & NSFW Verification
                withContext(Dispatchers.Main) {
                    uploadStage = 1
                    uploadProgress = 0.15f
                }
                delay(600)

                when (studioMode) {
                    CreatorStudioMode.POST -> {
                        val imageUris = slides.mapNotNull { it.imageUri }
                        if (imageUris.isNotEmpty()) {
                            val safety = com.chroma.studio.ml.safety.ContentSafetyScanner.scanMultipleImageUris(context, imageUris)
                            if (!safety.isSafe) {
                                withContext(Dispatchers.Main) {
                                    safetyViolationResult = safety
                                    uploadErrorMessage = "Community guidelines violation: prohibited image content detected"
                                }
                                return@launch
                            }
                        }
                    }
                    CreatorStudioMode.FLASH -> {
                        val videoUri = slides.firstOrNull { it.videoUri != null }?.videoUri
                        if (videoUri != null) {
                            val vSafety = com.chroma.studio.ml.safety.ContentSafetyScanner.scanVideoUri(context, videoUri, sampleCount = 6)
                            if (!vSafety.isSafe) {
                                withContext(Dispatchers.Main) {
                                    safetyViolationResult = vSafety
                                    uploadErrorMessage = "Community guidelines violation: prohibited video content detected"
                                }
                                return@launch
                            }
                        }
                    }
                    CreatorStudioMode.WORK -> {
                        // Work vectors are mathematical meshes; passed
                    }
                }

                // STAGE 2: Transcoding & 1:1 Crop
                withContext(Dispatchers.Main) {
                    uploadStage = 2
                    uploadProgress = 0.35f
                }
                delay(700)

                // STAGE 3: Cloud Storage Upload
                withContext(Dispatchers.Main) {
                    uploadStage = 3
                    uploadProgress = 0.50f
                }

                when (studioMode) {
                    CreatorStudioMode.POST -> {
                        val activeSlides = slides.toList()
                        val bytesList = mutableListOf<ByteArray>()
                        for (slide in activeSlides) {
                            slide.imageUri?.let { uri ->
                                val bitmap = try {
                                    context.contentResolver.openInputStream(uri)?.use { stream ->
                                        BitmapFactory.decodeStream(stream)
                                    }
                                } catch (_: Throwable) { null }
                                if (bitmap != null) {
                                    val stream = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                                    bytesList.add(stream.toByteArray())
                                    if (!bitmap.isRecycled) bitmap.recycle()
                                }
                            }
                        }

                        withContext(Dispatchers.Main) { uploadProgress = 0.72f }

                        // STAGE 4: Firestore Index & Deep Link
                        withContext(Dispatchers.Main) {
                            uploadStage = 4
                            uploadProgress = 0.88f
                        }

                        val success = discoverRepository.uploadMultiImagePost(
                            imageBytesList = bytesList,
                            caption = postTitle.trim(),
                            description = postDescription.trim(),
                            aspectRatio = selectedRatioId,
                            tags = hashtags.toList(),
                            mentions = mentionedUsers.map { it.username },
                            categoryTag = selectedCategoryTag ?: customCategoryTag.ifBlank { null },
                            hideLikesCount = hideLikesCount,
                            turnOffComments = turnOffComments,
                            turnOffRemix = turnOffRemix
                        )

                        if (success) {
                            draftRepo.deleteDraft(savedDraftId)
                            withContext(Dispatchers.Main) {
                                uploadProgress = 1f
                                isUploadFinished = true
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                uploadErrorMessage = "Failed to upload post. Please try again."
                            }
                        }
                    }

                    CreatorStudioMode.WORK -> {
                        val activeSlides = slides.toList()
                        val works = activeSlides.mapNotNull { it.work }

                        withContext(Dispatchers.Main) { uploadProgress = 0.70f }

                        withContext(Dispatchers.Main) {
                            uploadStage = 4
                            uploadProgress = 0.88f
                        }

                        val success = discoverRepository.publishMultiWork(
                            works = works,
                            caption = postTitle.trim(),
                            description = postDescription.trim(),
                            aspectRatio = selectedRatioId,
                            tags = hashtags.toList(),
                            mentions = mentionedUsers.map { it.username },
                            categoryTag = selectedCategoryTag ?: customCategoryTag.ifBlank { null },
                            hideLikesCount = hideLikesCount,
                            turnOffComments = turnOffComments,
                            turnOffRemix = turnOffRemix
                        )

                        if (success) {
                            draftRepo.deleteDraft(savedDraftId)
                            withContext(Dispatchers.Main) {
                                uploadProgress = 1f
                                isUploadFinished = true
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                uploadErrorMessage = "Failed to publish work to Community."
                            }
                        }
                    }

                    CreatorStudioMode.FLASH -> {
                        val videoSlide = slides.firstOrNull { it.videoUri != null }
                        val videoUri = videoSlide?.videoUri
                        if (videoUri == null) {
                            withContext(Dispatchers.Main) {
                                uploadErrorMessage = "No video available to publish."
                            }
                            return@launch
                        }

                        // Trim video if required
                        val shouldTrim = flashTrimStartMs > 100L || (flashTrimEndMs > 0L && flashTrimEndMs < flashVideoDurationMs - 100L)
                        val trimmed = if (shouldTrim) {
                            com.chroma.studio.utils.VideoProcessor.trimVideo(
                                context, videoUri, flashTrimStartMs, flashTrimEndMs
                            )
                        } else {
                            if (videoUri.scheme == "file") {
                                File(videoUri.path ?: "")
                            } else {
                                val tempFile = File(context.cacheDir, "ready_flash_${System.currentTimeMillis()}.mp4")
                                try {
                                    context.contentResolver.openInputStream(videoUri)?.use { input ->
                                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                                    }
                                    if (tempFile.exists() && tempFile.length() > 0L) tempFile else null
                                } catch (_: Throwable) { null }
                            }
                        } ?: run {
                            withContext(Dispatchers.Main) { uploadErrorMessage = "Video preparation failed." }
                            return@launch
                        }

                        withContext(Dispatchers.Main) { uploadProgress = 0.65f }

                        val thumbBitmap = coverThumbnailBmp ?: run {
                            com.chroma.studio.utils.VideoProcessor.extractThumbnail(
                                context, Uri.fromFile(trimmed), 0L
                            )
                        }

                        val thumbUrl: String = if (thumbBitmap != null) {
                            val baos = ByteArrayOutputStream()
                            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                            com.chroma.studio.data.CloudinaryClient.uploadFlashThumbnail(baos.toByteArray()) ?: ""
                        } else ""

                        val videoUrl = com.chroma.studio.data.CloudinaryClient.uploadVideoFromUri(
                            context = context,
                            uri = Uri.fromFile(trimmed),
                            folder = "community_videos",
                            onProgress = { p ->
                                val prog = 0.65f + p * 0.25f
                                scope.launch(Dispatchers.Main) { uploadProgress = prog }
                            }
                        ) ?: run {
                            withContext(Dispatchers.Main) { uploadErrorMessage = "Video upload failed. Check connection." }
                            return@launch
                        }

                        trimmed.delete()

                        // STAGE 4
                        withContext(Dispatchers.Main) {
                            uploadStage = 4
                            uploadProgress = 0.95f
                        }

                        val success = discoverRepository.uploadCommunityVideoPost(
                            videoUrl = videoUrl,
                            thumbnailUrl = thumbUrl,
                            caption = if (postTitle.isNotBlank()) postTitle else postDescription,
                            description = postDescription.trim(),
                            aspectRatio = selectedRatioId,
                            tags = hashtags.toList(),
                            mentions = mentionedUsers.map { it.username },
                            duration = (flashTrimEndMs - flashTrimStartMs).coerceAtLeast(1000L),
                            categoryTag = selectedCategoryTag ?: customCategoryTag.ifBlank { null },
                            hideLikesCount = hideLikesCount,
                            turnOffComments = turnOffComments,
                            turnOffRemix = turnOffRemix,
                            hideDownloadsCount = !allowDownloads
                        )

                        if (success) {
                            draftRepo.deleteDraft(savedDraftId)
                            withContext(Dispatchers.Main) {
                                uploadProgress = 1f
                                isUploadFinished = true
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                uploadErrorMessage = "Failed to publish video to Community."
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uploadErrorMessage = e.message ?: "Publishing encountered an unexpected error."
                }
            }
        }
    }

    // Android Hardware Back Navigation
    val handleStudioBack: () -> Unit = {
        when (currentStep) {
            StudioStep.STEP_1_MEDIA -> {
                if (slides.isNotEmpty() || postTitle.isNotBlank() || postDescription.isNotBlank()) {
                    showDiscardModal = true
                } else {
                    onNavigateBack()
                }
            }
            StudioStep.STEP_2_ASPECT_COVER -> currentStep = StudioStep.STEP_1_MEDIA
            StudioStep.STEP_3_DETAILS -> currentStep = StudioStep.STEP_2_ASPECT_COVER
            StudioStep.STEP_4_UPLOAD -> {
                if (isUploadFinished) onPostSuccess() else onNavigateBack()
            }
        }
    }

    BackHandler {
        handleStudioBack()
    }

    // Main Scaffold Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (colors.isDark) Color(0xFF0F1117) else Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Oscellion Dynamic Island (Matches HTML v2 Top Island)
            OscellionDynamicIsland(
                isExpanded = isIslandExpanded,
                onToggleExpand = { isIslandExpanded = !isIslandExpanded },
                title = "Oscellion Studio",
                subtitle = "Step ${currentStep.stepNumber}: ${currentStep.title} • Payload synced",
                badgeText = "LIVE"
            )

            // ================================================================
            // STICKY HEADER (Matches exact HTML reference layout)
            // ================================================================
            StudioStickyHeader(
                studioMode = studioMode,
                currentStep = currentStep,
                draftsCount = savedDraftsList.size,
                onBackClick = { handleStudioBack() },
                onSwitchMode = { newMode ->
                    if (studioMode != newMode) {
                        studioMode = newMode
                        selectedRatioId = if (newMode == CreatorStudioMode.FLASH) "9:16" else "4:3"
                    }
                },
                onOpenDrafts = { showDraftsDrawer = true },
                onOpenInspector = { showInspectorModal = true },
                onNextStep = {
                    when (currentStep) {
                        StudioStep.STEP_1_MEDIA -> currentStep = StudioStep.STEP_2_ASPECT_COVER
                        StudioStep.STEP_2_ASPECT_COVER -> currentStep = StudioStep.STEP_3_DETAILS
                        StudioStep.STEP_3_DETAILS -> startUploadPipeline()
                        StudioStep.STEP_4_UPLOAD -> if (isUploadFinished) onPostSuccess()
                    }
                },
                onJumpToStep = { targetStep ->
                    currentStep = targetStep
                }
            )

            // ================================================================
            // SCROLLABLE VIEWPORT FOR ACTIVE STEP
            // ================================================================
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (currentStep) {
                    StudioStep.STEP_1_MEDIA -> {
                        Step1MediaAndEditStage(
                            studioMode = studioMode,
                            slides = slides,
                            currentSlideIndex = currentSlideIndex,
                            flashTrimStartMs = flashTrimStartMs,
                            flashTrimEndMs = flashTrimEndMs,
                            flashVideoDurationMs = flashVideoDurationMs,
                            flashPlaybackSpeed = flashPlaybackSpeed,
                            flashIsMuted = flashIsMuted,
                            flashIsFlipped = flashIsFlipped,
                            workShowGrain = workShowGrain,
                            workMeshAngle = workMeshAngle,
                            workSearchQuery = workSearchQuery,
                            workIsSeriesMode = workIsSeriesMode,
                            workRepository = workRepository,
                            onSlideChange = { currentSlideIndex = it },
                            onOpenPhotoStudio = { showPhotoStudioModal = true },
                            onOpenVideoStudio = { showVideoStudioModal = true },
                            onLaunchImagePicker = {
                                multipleImagesPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onLaunchVideoPicker = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            onUpdateTrim = { start, end ->
                                flashTrimStartMs = start
                                flashTrimEndMs = end
                            },
                            onCycleSpeed = {
                                flashPlaybackSpeed = when (flashPlaybackSpeed) {
                                    0.5f -> 1.0f
                                    1.0f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 0.5f
                                }
                            },
                            onToggleMute = { flashIsMuted = !flashIsMuted },
                            onToggleFlip = { flashIsFlipped = !flashIsFlipped },
                            onSnapFifteenSec = {
                                flashTrimStartMs = 0L
                                flashTrimEndMs = 15000L.coerceAtMost(flashVideoDurationMs)
                            },
                            onUpdatePhotoAdjustments = { b, c, s, w, v, sh ->
                                val cur = slides.getOrNull(currentSlideIndex)
                                if (cur != null) {
                                    slides[currentSlideIndex] = cur.copy(
                                        brightness = b,
                                        contrast = c,
                                        saturation = s,
                                        warmth = w,
                                        vignette = v,
                                        sharpness = sh
                                    )
                                }
                            },
                            onSelectFilterPreset = { preset ->
                                val cur = slides.getOrNull(currentSlideIndex)
                                if (cur != null) {
                                    slides[currentSlideIndex] = cur.copy(filterName = preset)
                                }
                            },
                            onRotatePhoto = {
                                val cur = slides.getOrNull(currentSlideIndex)
                                if (cur != null) {
                                    slides[currentSlideIndex] = cur.copy(rotationDegrees = (cur.rotationDegrees + 90f) % 360f)
                                }
                            },
                            onFlipPhoto = {
                                val cur = slides.getOrNull(currentSlideIndex)
                                if (cur != null) {
                                    slides[currentSlideIndex] = cur.copy(rotationDegrees = (cur.rotationDegrees + 180f) % 360f)
                                }
                            },
                            onToggleWorkGrain = { workShowGrain = !workShowGrain },
                            onRotateWorkMesh = { workMeshAngle = (workMeshAngle + 45f) % 360f },
                            onWorkSearchChange = { workSearchQuery = it },
                            onToggleWorkSeries = { workIsSeriesMode = !workIsSeriesMode },
                            onSelectWork = { work ->
                                if (workIsSeriesMode) {
                                    if (slides.any { it.work?.id == work.id }) {
                                        slides.removeAll { it.work?.id == work.id }
                                    } else {
                                        slides.add(StudioSlide(work = work, label = "Work ${slides.size + 1}"))
                                    }
                                } else {
                                    slides.clear()
                                    slides.add(StudioSlide(work = work, label = work.name))
                                    currentSlideIndex = 0
                                    if (postTitle.isBlank()) postTitle = work.name
                                    if (postDescription.isBlank()) postDescription = work.description
                                }
                            },
                            onOpenAudioMixer = { showAudioMixerModal = true },
                            selectedSoundtrackTitle = selectedSoundtrack?.title,
                            onSelectRecentFlash = { preset ->
                                if (slides.isEmpty()) {
                                    slides.add(StudioSlide(imageUrl = preset.url, label = preset.title))
                                } else {
                                    slides[0] = slides[0].copy(imageUrl = preset.url, label = preset.title)
                                }
                                currentSlideIndex = 0
                            },
                            onToggleRecentPhoto = { preset ->
                                val existingIndex = slides.indexOfFirst { it.imageUrl == preset.url }
                                if (existingIndex >= 0) {
                                    if (slides.size > 1) {
                                        slides.removeAt(existingIndex)
                                        currentSlideIndex = currentSlideIndex.coerceIn(0, slides.lastIndex)
                                    }
                                } else if (slides.size < 10) {
                                    slides.add(StudioSlide(imageUrl = preset.url, label = preset.title))
                                    currentSlideIndex = slides.lastIndex
                                }
                            }
                        )
                    }

                    StudioStep.STEP_2_ASPECT_COVER -> {
                        Step2AspectAndCoverStage(
                            studioMode = studioMode,
                            slides = slides,
                            currentSlideIndex = currentSlideIndex,
                            selectedRatioId = selectedRatioId,
                            showThirdsGrid = showThirdsGrid,
                            showWatermark = showWatermark,
                            cropOffsetX = cropOffsetX,
                            cropOffsetY = cropOffsetY,
                            coverPickerMode = coverPickerMode,
                            selectedCoverFrameMs = selectedCoverFrameMs,
                            flashVideoDurationMs = flashVideoDurationMs,
                            coverThumbnailBmp = coverThumbnailBmp,
                            activeEditTab = activeEditTab,
                            isComparingOriginal = isComparingOriginal,
                            flashTrimStartMs = flashTrimStartMs,
                            flashTrimEndMs = flashTrimEndMs,
                            flashPlaybackSpeed = flashPlaybackSpeed,
                            flashIsMuted = flashIsMuted,
                            flashIsFlipped = flashIsFlipped,
                            onSelectEditTab = { activeEditTab = it },
                            onHoldOriginal = { isComparingOriginal = it },
                            onRotatePhoto = {
                                val cur = slides.getOrNull(currentSlideIndex)
                                if (cur != null) {
                                    slides[currentSlideIndex] = cur.copy(rotationDegrees = (cur.rotationDegrees + 90f) % 360f)
                                }
                            },
                            onFlipPhoto = {
                                val cur = slides.getOrNull(currentSlideIndex)
                                if (cur != null) {
                                    slides[currentSlideIndex] = cur.copy(rotationDegrees = (cur.rotationDegrees + 180f) % 360f)
                                }
                            },
                            onResetFraming = {
                                cropOffsetX = 50f
                                cropOffsetY = 50f
                                selectedRatioId = "1:1"
                                val cur = slides.getOrNull(currentSlideIndex)
                                if (cur != null) {
                                    slides[currentSlideIndex] = cur.copy(
                                        rotationDegrees = 0f,
                                        filterName = "Original",
                                        brightness = 0f,
                                        contrast = 1f,
                                        saturation = 1f,
                                        warmth = 0f,
                                        vignette = 0f,
                                        sharpness = 0f
                                    )
                                }
                            },
                            onSelectRatio = { selectedRatioId = it },
                            onToggleThirdsGrid = { showThirdsGrid = !showThirdsGrid },
                            onToggleWatermark = { showWatermark = !showWatermark },
                            onCropOffsetChange = { x, y ->
                                cropOffsetX = x
                                cropOffsetY = y
                            },
                            onCenterCrop = {
                                cropOffsetX = 50f
                                cropOffsetY = 50f
                            },
                            onCoverPickerModeChange = { coverPickerMode = it },
                            onScrubCoverFrame = { selectedCoverFrameMs = it },
                            onPickCustomCover = {
                                customCoverPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onSelectSlide = { currentSlideIndex = it },
                            onRemoveSlide = { idx ->
                                if (slides.size > 1 && idx in slides.indices) {
                                    slides.removeAt(idx)
                                    currentSlideIndex = currentSlideIndex.coerceIn(0, slides.lastIndex)
                                }
                            },
                            onReorderSlide = { from, to ->
                                if (from in slides.indices && to in slides.indices) {
                                    val item = slides.removeAt(from)
                                    slides.add(to, item)
                                    currentSlideIndex = to
                                }
                            },
                            onUpdateTrim = { s, e ->
                                flashTrimStartMs = s
                                flashTrimEndMs = e
                            },
                            onCycleSpeed = {
                                flashPlaybackSpeed = when (flashPlaybackSpeed) {
                                    0.5f -> 1.0f
                                    1.0f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 0.5f
                                }
                            },
                            onToggleMute = { flashIsMuted = !flashIsMuted },
                            onToggleFlip = { flashIsFlipped = !flashIsFlipped },
                            onSnapFifteenSec = {
                                flashTrimStartMs = 0L
                                flashTrimEndMs = 15000L.coerceAtMost(flashVideoDurationMs)
                            },
                            onOpenAudioMixer = { showAudioMixerModal = true },
                            onSelectFilterPreset = { preset ->
                                val cur = slides.getOrNull(currentSlideIndex)
                                if (cur != null) {
                                    slides[currentSlideIndex] = cur.copy(filterName = preset)
                                }
                            },
                            onUpdatePhotoAdjustments = { b, c, s, w, v, sh ->
                                val cur = slides.getOrNull(currentSlideIndex)
                                if (cur != null) {
                                    slides[currentSlideIndex] = cur.copy(
                                        brightness = b,
                                        contrast = c,
                                        saturation = s,
                                        warmth = w,
                                        vignette = v,
                                        sharpness = sh
                                    )
                                }
                            }
                        )
                    }

                    StudioStep.STEP_3_DETAILS -> {
                        Step3PostDetailsStage(
                            studioMode = studioMode,
                            slides = slides,
                            currentSlideIndex = currentSlideIndex,
                            selectedRatioId = selectedRatioId,
                            postTitle = postTitle,
                            postDescription = postDescription,
                            selectedCategoryTag = selectedCategoryTag,
                            customCategoryTag = customCategoryTag,
                            locationString = locationString,
                            altTextString = altTextString,
                            hashtags = hashtags,
                            mentionedUsers = mentionedUsers,
                            audienceVisibility = audienceVisibility,
                            hideLikesCount = hideLikesCount,
                            turnOffComments = turnOffComments,
                            turnOffRemix = turnOffRemix,
                            includeOriginalAudio = includeOriginalAudio,
                            allowDownloads = allowDownloads,
                            highQualityUpload = highQualityUpload,
                            scheduleReleaseEnabled = scheduleReleaseEnabled,
                            scheduleDateTime = scheduleDateTime,
                            remixAttributionEnabled = remixAttributionEnabled,
                            isAdvancedSettingsOpen = isAdvancedSettingsOpen,
                            isAltTextOpen = isAltTextOpen,
                            onEditCoverClick = { currentStep = StudioStep.STEP_2_ASPECT_COVER },
                            onToggleAdvancedSettings = { isAdvancedSettingsOpen = !isAdvancedSettingsOpen },
                            onToggleAltText = { isAltTextOpen = !isAltTextOpen },
                            onToggleScheduleRelease = { scheduleReleaseEnabled = it },
                            onScheduleDateTimeChange = { scheduleDateTime = it },
                            onToggleRemixAttribution = { remixAttributionEnabled = it },
                            onTitleChange = { if (it.length <= 100) postTitle = it },
                            onDescriptionChange = { if (it.length <= 1500) postDescription = it },
                            onCategorySelect = { cat ->
                                selectedCategoryTag = if (selectedCategoryTag == cat) null else cat
                            },
                            onCustomCategoryChange = { customCategoryTag = it },
                            onLocationChange = { locationString = it },
                            onAltTextChange = { altTextString = it },
                            onAddHashtag = { tag ->
                                val clean = tag.trim().removePrefix("#").lowercase()
                                if (clean.isNotBlank() && !hashtags.contains(clean)) {
                                    hashtags.add(clean)
                                }
                            },
                            onRemoveHashtag = { hashtags.remove(it) },
                            onOpenMentionModal = { showMentionModal = true },
                            onRemoveMention = { mentionedUsers.remove(it) },
                            onAudienceSelect = { audienceVisibility = it },
                            onToggleHideLikes = { hideLikesCount = it },
                            onToggleComments = { turnOffComments = it },
                            onToggleRemix = { turnOffRemix = it },
                            onToggleAudio = { includeOriginalAudio = it },
                            onToggleDownloads = { allowDownloads = it },
                            onToggleHighQuality = { highQualityUpload = it }
                        )
                    }

                    StudioStep.STEP_4_UPLOAD -> {
                        Step4UploadAndShareStage(
                            studioMode = studioMode,
                            slides = slides,
                            currentSlideIndex = currentSlideIndex,
                            selectedRatioId = selectedRatioId,
                            postTitle = postTitle,
                            locationString = locationString,
                            categoryTag = selectedCategoryTag ?: "Art",
                            audienceVisibility = audienceVisibility,
                            uploadProgress = uploadProgress,
                            uploadStage = uploadStage,
                            isUploadFinished = isUploadFinished,
                            generatedDeepLink = generatedDeepLink,
                            errorMessage = uploadErrorMessage,
                            onCopyDeepLink = {
                                clipboardManager.setText(AnnotatedString(generatedDeepLink))
                                Toast.makeText(context, "Deep link copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onShareLink = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Check out my new creation on Oscellion: $generatedDeepLink")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Creation"))
                            },
                            onDoneClick = { showFullPostModal = true },
                            onRetryUpload = { startUploadPipeline() },
                            onShareTargetClick = { target ->
                                when (target.id) {
                                    "link" -> {
                                        clipboardManager.setText(AnnotatedString(generatedDeepLink))
                                        Toast.makeText(context, "Deep link copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                    "story" -> {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Shared to Story: $generatedDeepLink")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share to Story"))
                                    }
                                    "message" -> {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Check out this creation: $generatedDeepLink")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Send Message"))
                                    }
                                    "email" -> {
                                        val emailIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "message/rfc822"
                                            putExtra(Intent.EXTRA_SUBJECT, postTitle.ifBlank { "Oscellion Creation" })
                                            putExtra(Intent.EXTRA_TEXT, "Look at my new creation on Oscellion: $generatedDeepLink")
                                        }
                                        try {
                                            context.startActivity(Intent.createChooser(emailIntent, "Send Email"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    "more" -> {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Check out my new creation on Oscellion: $generatedDeepLink")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Creation"))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // ====================================================================
        // MODAL 1: SAVED DRAFTS DRAWER
        // ====================================================================
        if (showDraftsDrawer) {
            SavedDraftsDrawerModal(
                drafts = savedDraftsList,
                onSelectDraft = { draft -> loadDraft(draft) },
                onDeleteDraft = { draftId ->
                    draftRepo.deleteDraft(draftId)
                    refreshDrafts()
                },
                onDismiss = { showDraftsDrawer = false }
            )
        }

        // ====================================================================
        // MODAL 2: MENTION CREATOR SEARCH MODAL
        // ====================================================================
        if (showMentionModal) {
            MentionUserSearchModal(
                discoverRepository = discoverRepository,
                selectedUsers = mentionedUsers.toList(),
                onToggleUser = { user ->
                    if (mentionedUsers.any { it.uid == user.uid }) {
                        mentionedUsers.removeAll { it.uid == user.uid }
                    } else if (mentionedUsers.size < 5) {
                        mentionedUsers.add(user)
                    }
                },
                onDismiss = { showMentionModal = false }
            )
        }

        // ====================================================================
        // MODAL 3: AUDIO TRACK & SOUND MIXER SHEET
        // ====================================================================
        if (showAudioMixerModal) {
            AudioTrackMixerModalSheet(
                origVolume = origAudioVolume,
                soundtrackVolume = soundtrackVolume,
                selectedTrack = selectedSoundtrack,
                onOrigVolumeChange = { origAudioVolume = it },
                onSoundtrackVolumeChange = { soundtrackVolume = it },
                onSelectTrack = { selectedSoundtrack = it },
                onDismiss = { showAudioMixerModal = false }
            )
        }

        // ====================================================================
        // MODAL 4: DISCARD DRAFT CONFIRMATION SHEET (Apple HIG 3D Trash Bin)
        // ====================================================================
        if (showDiscardModal) {
            DiscardDraftConfirmationModal(
                onSaveDraft = {
                    saveCurrentDraft()
                    showDiscardModal = false
                    Toast.makeText(context, "Draft saved to repository", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                onDiscard = {
                    showDiscardModal = false
                    onNavigateBack()
                },
                onKeepEditing = {
                    showDiscardModal = false
                }
            )
        }

        // ====================================================================
        // MODAL 5: FULL LIVE POST VIEW MODAL
        // ====================================================================
        if (showFullPostModal) {
            FullLivePostViewModal(
                postTitle = postTitle,
                postDescription = postDescription,
                locationString = locationString,
                categoryTag = selectedCategoryTag ?: "Art",
                hashtags = hashtags.toList(),
                activeSlide = slides.getOrNull(currentSlideIndex) ?: slides.firstOrNull(),
                deepLinkUrl = generatedDeepLink.ifBlank { "https://oscellion.vercel.app/post/new-creation" },
                onDismiss = { showFullPostModal = false },
                onReadyForNewUpload = {
                    showFullPostModal = false
                    onPostSuccess()
                }
            )
        }

        // ====================================================================
        // CONTENT SAFETY VIOLATION MODAL
        // ====================================================================
        safetyViolationResult?.let { result ->
            com.chroma.studio.ui.components.ContentSafetyViolationDialog(
                scanResult = result,
                onDismiss = { safetyViolationResult = null }
            )
        }
    }
}


// ============================================================================
// OSCELION DYNAMIC ISLAND (Matches HTML Reference Layout)
// ============================================================================

@Composable
fun OscellionDynamicIsland(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    title: String = "Oscellion Studio",
    subtitle: String = "Ready · UnifiedPostPayload synced",
    badgeText: String = "LIVE",
    modifier: Modifier = Modifier
) {
    val islandWidth by animateDpAsState(
        targetValue = if (isExpanded) 310.dp else 146.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "islandWidth"
    )
    val islandHeight by animateDpAsState(
        targetValue = if (isExpanded) 52.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "islandHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .width(islandWidth)
                .height(islandHeight)
                .clip(CircleShape)
                .background(Color(0xFF0F172A))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), CircleShape)
                .clickable { onToggleExpand() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Emerald pulsing dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )

                if (isExpanded) {
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                } else {
                    Text(
                        text = "OSCELLION",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Live Pill Badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    color = Color(0xFF10B981),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ============================================================================
// STICKY HEADER COMPONENT (Matches exact HTML reference layout)
// ============================================================================

@Composable
fun StudioStickyHeader(
    studioMode: CreatorStudioMode,
    currentStep: StudioStep,
    draftsCount: Int,
    onBackClick: () -> Unit,
    onSwitchMode: (CreatorStudioMode) -> Unit,
    onOpenDrafts: () -> Unit,
    onOpenInspector: () -> Unit = {},
    onNextStep: () -> Unit,
    onJumpToStep: (StudioStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (colors.isDark) Color(0xFF0F1117) else Color(0xFFF8FAFC))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Back Button, Mode Switcher, Drafts Drawer Icon & Next Step Liquid Glass Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action: Back Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (colors.isDark) Color(0xFF1E2230) else Color.White)
                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)), CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.ArrowLeft,
                    contentDescription = "Back",
                    tint = colors.textMain,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Center: Mode Switcher (Pill with sliding active state)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFEDF2F7))
                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(20.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FLASH / REEL Tab
                val isFlash = studioMode == CreatorStudioMode.FLASH
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isFlash) (if (colors.isDark) Color(0xFF282D3D) else Color.White) else Color.Transparent)
                        .clickable { onSwitchMode(CreatorStudioMode.FLASH) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "REEL",
                        color = if (isFlash) colors.primary else colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isFlash) FontWeight.Bold else FontWeight.SemiBold
                    )
                }

                // POST Tab
                val isPhoto = studioMode == CreatorStudioMode.POST
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isPhoto) (if (colors.isDark) Color(0xFF282D3D) else Color.White) else Color.Transparent)
                        .clickable { onSwitchMode(CreatorStudioMode.POST) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "POST",
                        color = if (isPhoto) colors.primary else colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isPhoto) FontWeight.Bold else FontWeight.SemiBold
                    )
                }

                // WORK Tab
                val isWork = studioMode == CreatorStudioMode.WORK
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isWork) (if (colors.isDark) Color(0xFF282D3D) else Color.White) else Color.Transparent)
                        .clickable { onSwitchMode(CreatorStudioMode.WORK) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "WORK",
                        color = if (isWork) colors.primary else colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isWork) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            }

            // Right Action Group: Live Payload Inspector, Saved Drafts + Next Step Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // macOS Live Payload & Compose Inspector Icon Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF1E2230) else Color.White)
                        .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)), CircleShape)
                        .clickable { onOpenInspector() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Code2,
                        contentDescription = "Inspect Payload",
                        tint = Color(0xFFF5882E),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Drafts Drawer Icon Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF1E2230) else Color.White)
                        .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)), CircleShape)
                        .clickable { onOpenDrafts() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Archive,
                        contentDescription = "Saved Drafts",
                        tint = colors.textMain,
                        modifier = Modifier.size(16.dp)
                    )
                    if (draftsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF5882E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$draftsCount",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Next Step Liquid Glass Button (Orange gradient)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF5882E), Color(0xFFEA580C))
                            )
                        )
                        .shadow(elevation = 4.dp, shape = CircleShape)
                        .clickable { onNextStep() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentStep == StudioStep.STEP_4_UPLOAD) Lucide.Check else Lucide.ArrowRight,
                        contentDescription = "Next Step",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // Row 2: Target Collection Pill + 4-Segment Step Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Target Collection Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (colors.isDark) Color(0xFF1696B2).copy(alpha = 0.2f) else Color(0xFFE0F7FA))
                    .border(BorderStroke(1.dp, if (colors.isDark) Color(0xFF1696B2).copy(alpha = 0.4f) else Color(0xFFB2EBF2)), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Lucide.Database,
                        contentDescription = null,
                        tint = Color(0xFF1696B2),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (studioMode == CreatorStudioMode.FLASH) "flash_videos" else "community_posts",
                        color = Color(0xFF1696B2),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 4-Segment Interactive Step Bar
            Row(
                modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioStep.values().forEach { step ->
                    val isActive = step.stepNumber <= currentStep.stepNumber
                    val isCurrent = step == currentStep
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isCurrent) 28.dp else 16.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrent -> Brush.horizontalGradient(listOf(Color(0xFF38B8D1), Color(0xFFF5882E)))
                                    isActive -> SolidColor(Color(0xFF1696B2))
                                    else -> SolidColor(if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1))
                                }
                            )
                            .clickable { onJumpToStep(step) }
                    )
                }
            }
        }
    }
}

// ============================================================================
// STEP 1: MEDIA INGESTION & PRE-TOOL EDITING (V3 ENHANCED SPEC)
// ============================================================================

@Composable
fun Step1MediaAndEditStage(
    studioMode: CreatorStudioMode,
    slides: List<StudioSlide>,
    currentSlideIndex: Int,
    flashTrimStartMs: Long,
    flashTrimEndMs: Long,
    flashVideoDurationMs: Long,
    flashPlaybackSpeed: Float,
    flashIsMuted: Boolean,
    flashIsFlipped: Boolean,
    workShowGrain: Boolean,
    workMeshAngle: Float,
    workSearchQuery: String,
    workIsSeriesMode: Boolean,
    workRepository: WorkRepository,
    onOpenAudioMixer: () -> Unit = {},
    selectedSoundtrackTitle: String? = null,
    onOpenPhotoStudio: () -> Unit = {},
    onOpenVideoStudio: () -> Unit = {},
    onSlideChange: (Int) -> Unit,
    onLaunchImagePicker: () -> Unit,
    onLaunchVideoPicker: () -> Unit,
    onUpdateTrim: (Long, Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFlip: () -> Unit,
    onSnapFifteenSec: () -> Unit,
    onUpdatePhotoAdjustments: (Float, Float, Float, Float, Float, Float) -> Unit,
    onSelectFilterPreset: (String) -> Unit,
    onRotatePhoto: () -> Unit,
    onFlipPhoto: () -> Unit,
    onToggleWorkGrain: () -> Unit,
    onRotateWorkMesh: () -> Unit,
    onWorkSearchChange: (String) -> Unit,
    onToggleWorkSeries: () -> Unit,
    onSelectWork: (ChromaWork) -> Unit,
    onSelectRecentFlash: (RecentMediaItem) -> Unit = {},
    onToggleRecentPhoto: (RecentMediaItem) -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current

    val allWorks = remember {
        try {
            workRepository.loadActiveWorks()
        } catch (e: Exception) {
            emptyList()
        }
    }
    val filteredWorks = remember(allWorks, workSearchQuery) {
        if (workSearchQuery.isBlank()) allWorks
        else allWorks.filter { it.name.contains(workSearchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (studioMode) {
            // ────────────────────────────────────────────────────────────────
            // 1A. FLASH VIDEO MODE (Reel / Flash 9:16)
            // ────────────────────────────────────────────────────────────────
            CreatorStudioMode.FLASH -> {
                // Source Selector Card & Recents Grid
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gallery Upload Button
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (colors.isDark) Color(0xFF1A2634) else Color(0xFFEBF7FA))
                                    .border(1.dp, if (colors.isDark) Color(0xFF1696B2).copy(alpha = 0.4f) else Color(0xFFBAE6FD), RoundedCornerShape(16.dp))
                                    .clickable { onLaunchVideoPicker() }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (colors.isDark) Color(0xFF141720) else Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.Video, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(16.dp))
                                }
                                Column {
                                    Text("Gallery", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("MP4 · 30s", color = colors.textMuted, fontSize = 10.sp)
                                }
                            }

                            // Record Live Camera Button
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (colors.isDark) Color(0xFF2D1E1E) else Color(0xFFFEF2E8))
                                    .border(1.dp, if (colors.isDark) Color(0xFFF5882E).copy(alpha = 0.4f) else Color(0xFFFED7AA), RoundedCornerShape(16.dp))
                                    .clickable { onLaunchVideoPicker() }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (colors.isDark) Color(0xFF141720) else Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.Video, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(16.dp))
                                }
                                Column {
                                    Text("Recorder", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("9:16 Live", color = colors.textMuted, fontSize = 10.sp)
                                }
                            }
                        }

                        // V3 Recents Grid for Flash Reels (Reels up to 30s)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Lucide.Clock3, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(13.dp))
                                    Text("Recents", color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Reels up to 30s", color = colors.textMuted, fontSize = 10.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RECENT_FLASH_PRESETS.forEach { preset ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(9f / 16f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF0F172A))
                                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                                            .clickable { onSelectRecentFlash(preset) }
                                    ) {
                                        AsyncImage(
                                            model = preset.url,
                                            contentDescription = preset.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                                        )
                                        // Duration badge
                                        preset.durationText?.let { dur ->
                                            Text(
                                                text = dur,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 9:16 Reel Preview Stage with Quick Action Rail
                item {
                    val activeSlide = slides.getOrNull(currentSlideIndex)
                    var isPlaying by remember { mutableStateOf(false) }
                    var currentPositionMs by remember { mutableLongStateOf(0L) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(28.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 9:16 Reel Frame (196dp width)
                            Box(
                                modifier = Modifier
                                    .width(196.dp)
                                    .aspectRatio(9f / 16f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(20.dp))
                            ) {
                                // Background preview
                                if (activeSlide?.imageUri != null || !activeSlide?.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = activeSlide?.imageUri ?: activeSlide?.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = if (flashIsFlipped) -1f else 1f
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF0F4C64), Color(0xFF1CA3BC), Color(0xFFF5882E))
                                                )
                                            )
                                    )
                                }

                                // Top Floating Pills
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Lucide.Smartphone, contentDescription = null, tint = Color(0xFFFB923C), modifier = Modifier.size(10.dp))
                                        Text("9:16", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text("${flashPlaybackSpeed}x", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }

                                // Center Play / Pause Icon Button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.9f))
                                        .clickable { isPlaying = !isPlaying },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Lucide.Pause else Lucide.Play,
                                        contentDescription = "Play/Pause",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Bottom Sound Pill & Duration
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .clickable { onOpenAudioMixer() }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Lucide.Disc, contentDescription = null, tint = Color(0xFFFB923C), modifier = Modifier.size(10.dp))
                                        Text(selectedSoundtrackTitle ?: "Original Reel Audio", color = Color.White, fontSize = 9.sp, maxLines = 1)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatDurationTime(currentPositionMs),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Reel 9:16",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            // Vertical Tool Rail (Quick Actions)
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC))
                                    .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Mute / Unmute
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (colors.isDark) Color(0xFF141720) else Color.White)
                                        .clickable { onToggleMute() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (flashIsMuted) Lucide.VolumeX else Lucide.Volume2,
                                        contentDescription = "Mute",
                                        tint = if (flashIsMuted) Color(0xFFEF4444) else colors.textMain,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Playback Speed Cycle
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (colors.isDark) Color(0xFF141720) else Color.White)
                                        .clickable { onCycleSpeed() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${flashPlaybackSpeed}x",
                                        color = Color(0xFF1696B2),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Mirror Flip
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (colors.isDark) Color(0xFF141720) else Color.White)
                                        .clickable { onToggleFlip() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.FlipHorizontal,
                                        contentDescription = "Mirror",
                                        tint = if (flashIsFlipped) Color(0xFFF5882E) else colors.textMain,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Snap 15s
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (colors.isDark) Color(0xFF141720) else Color.White)
                                        .clickable { onSnapFifteenSec() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.WandSparkles, contentDescription = "15s Snap", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                }

                                // Audio Mixer Modal Launcher
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (colors.isDark) Color(0xFF141720) else Color.White)
                                        .clickable { onOpenAudioMixer() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.SlidersVertical, contentDescription = "Mixer", tint = Color(0xFFF5882E), modifier = Modifier.size(16.dp))
                                }

                                // Pro Reel Editor Launcher
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF007AFF))
                                        .clickable { onOpenVideoStudio() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.Film, contentDescription = "Pro Reel Editor", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Horizontal Timeline Scrubber
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Timeline Trim Range", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("${flashTrimStartMs / 1000}s - ${flashTrimEndMs / 1000}s", color = Color(0xFF1696B2), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            RangeSlider(
                                value = (flashTrimStartMs.toFloat())..(flashTrimEndMs.toFloat()),
                                onValueChange = { range ->
                                    onUpdateTrim(range.start.toLong(), range.endInclusive.toLong())
                                },
                                valueRange = 0f..(flashVideoDurationMs.coerceAtLeast(15000L).toFloat()),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFF5882E),
                                    activeTrackColor = Color(0xFF1696B2),
                                    inactiveTrackColor = if (colors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth().height(24.dp)
                            )
                        }

                        // Wide Pro Reel Editor Launcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF1696B2), Color(0xFF38B8D1))
                                    )
                                )
                                .clickable { onOpenVideoStudio() }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Lucide.Film, contentDescription = "Pro Reel Editor", tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Pro Reel Editor (Timeline & FCP Suite)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ────────────────────────────────────────────────────────────────
            // 1B. PHOTO POST MODE (Instagram/Facebook Carousel Multi-Select)
            // ────────────────────────────────────────────────────────────────
            CreatorStudioMode.POST -> {
                // Source Selector Bar & Recents Grid with Multi-select Badges
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gallery Upload Button
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (colors.isDark) Color(0xFF1A2634) else Color(0xFFEBF7FA))
                                    .border(1.dp, if (colors.isDark) Color(0xFF1696B2).copy(alpha = 0.4f) else Color(0xFFBAE6FD), RoundedCornerShape(16.dp))
                                    .clickable { onLaunchImagePicker() }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (colors.isDark) Color(0xFF141720) else Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.Images, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(16.dp))
                                }
                                Column {
                                    Text("Gallery", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Up to 10", color = colors.textMuted, fontSize = 10.sp)
                                }
                            }

                            // Camera Capture Button
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (colors.isDark) Color(0xFF2D1E1E) else Color(0xFFFEF2E8))
                                    .border(1.dp, if (colors.isDark) Color(0xFFF5882E).copy(alpha = 0.4f) else Color(0xFFFED7AA), RoundedCornerShape(16.dp))
                                    .clickable { onLaunchImagePicker() }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (colors.isDark) Color(0xFF141720) else Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.Camera, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(16.dp))
                                }
                                Column {
                                    Text("Camera", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("High-Res", color = colors.textMuted, fontSize = 10.sp)
                                }
                            }

                            // Carousel Slide Count Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1696B2))
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(Lucide.Layers, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "${slides.size.coerceAtLeast(1)}/10",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // V3 Photo Recents Grid with Multi-select Number Badges (1, 2, 3...)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Lucide.Clock3, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(13.dp))
                                    Text("Recents", color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Tap to select up to 10", color = colors.textMuted, fontSize = 10.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RECENT_PHOTO_PRESETS.take(4).forEach { preset ->
                                    val selIndex = slides.indexOfFirst { it.imageUrl == preset.url }
                                    val isSelected = selIndex >= 0

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF0F172A))
                                            .border(
                                                BorderStroke(
                                                    if (isSelected) 2.dp else 1.dp,
                                                    if (isSelected) Color(0xFFF5882E) else (if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
                                                ),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onToggleRecentPhoto(preset) }
                                    ) {
                                        AsyncImage(
                                            model = preset.url,
                                            contentDescription = preset.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Selection Order Number Badge (1, 2, 3...)
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(Color(0xFFF5882E), Color(0xFFEA580C))
                                                        )
                                                    )
                                                    .border(BorderStroke(1.dp, Color.White), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${selIndex + 1}",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Active Photo Preview Stage (4:3 aspect)
                item {
                    val activeSlide = slides.getOrNull(currentSlideIndex)
                    var showOriginal by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Main Photo Preview Stage
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0F172A))
                        ) {
                            if (activeSlide?.imageUri != null || !activeSlide?.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = activeSlide?.imageUri ?: activeSlide?.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(rotationZ = activeSlide?.rotationDegrees ?: 0f),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF0F4C64), Color(0xFF38B8D1), Color(0xFFF5882E))
                                            )
                                        )
                                )
                            }

                            // Top Floating Badges
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Lucide.Image, contentDescription = null, tint = Color(0xFF38B8D1), modifier = Modifier.size(11.dp))
                                    Text("Photo 4:3", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Slide Number Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("${currentSlideIndex + 1}/${slides.size.coerceAtLeast(1)}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }

                                    // Hold to Compare Original
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .clickable { showOriginal = !showOriginal },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Eye,
                                            contentDescription = "Compare",
                                            tint = if (showOriginal) Color(0xFFF5882E) else Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // V3 Photo Mini Carousel Strip (Thumbnails of all selected photos)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Carousel Sequence", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text("${slides.size} slides", color = Color(0xFF1696B2), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // Studio Photo Editor Launcher (Filters, Adjustments & Stickers)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFF5882E), Color(0xFFFB923C))
                                        )
                                    )
                                    .clickable { onOpenPhotoStudio() }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Lucide.Sparkles, contentDescription = "Studio Photo Editor", tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open Studio Photo Editor (Filters, Crop & Stickers)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                itemsIndexed(slides) { idx, slide ->
                                    val isCur = idx == currentSlideIndex
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF0F172A))
                                            .border(
                                                BorderStroke(
                                                    if (isCur) 2.dp else 1.dp,
                                                    if (isCur) Color(0xFFF5882E) else (if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
                                                ),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onSlideChange(idx) }
                                    ) {
                                        SlidePreviewContent(
                                            slide = slide,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        // Index badge
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(2.dp)
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(if (isCur) Color(0xFFF5882E) else Color.Black.copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${idx + 1}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Add Slide Button Card
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF1F5F9))
                                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFCBD5E1)), RoundedCornerShape(12.dp))
                                            .clickable { onLaunchImagePicker() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Plus, contentDescription = "Add Slide", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ────────────────────────────────────────────────────────────────
            // 1C. WORK POST MODE (Room SQLite activeWorks Layer Works)
            // ────────────────────────────────────────────────────────────────
            CreatorStudioMode.WORK -> {
                // SQLite Project Chooser & Live Mesh Canvas
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Live Mesh Canvas Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0F172A))
                        ) {
                            val activeSlide = slides.getOrNull(currentSlideIndex)
                            if (activeSlide?.work != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF0F4C64), Color(0xFF38B8D1), Color(0xFFF5882E))
                                            )
                                        )
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF0F4C64), Color(0xFF38B8D1), Color(0xFFF5882E))
                                            )
                                        )
                                )
                            }

                            // Top pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Lucide.Database, contentDescription = null, tint = Color(0xFF38B8D1), modifier = Modifier.size(11.dp))
                                    Text("activeWorks", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Grain overlay toggle
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (workShowGrain) Color(0xFFF5882E) else Color.Black.copy(alpha = 0.45f))
                                            .clickable { onToggleWorkGrain() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Sparkles, contentDescription = "Grain", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }

                                    // Rotate angle
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .clickable { onRotateWorkMesh() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Compass, contentDescription = "Angle", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }

                        // Search and Series Mode Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = workSearchQuery,
                                onValueChange = onWorkSearchChange,
                                placeholder = { Text("Search local activeWorks...", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(Lucide.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f).height(44.dp),
                                textStyle = TextStyle(fontSize = 12.sp)
                            )

                            // Series Mode Toggle
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (workIsSeriesMode) Color(0xFF1696B2) else (if (colors.isDark) Color(0xFF1E212B) else Color(0xFFEDF2F7)))
                                    .clickable { onToggleWorkSeries() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Lucide.Layers, contentDescription = null, tint = if (workIsSeriesMode) Color.White else colors.textMain, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = if (workIsSeriesMode) "Series" else "Single",
                                        color = if (workIsSeriesMode) Color.White else colors.textMain,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Works Grid
                        if (filteredWorks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No local activeWorks found", color = colors.textMuted, fontSize = 11.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredWorks) { work ->
                                    val isSelected = slides.any { it.work?.id == work.id }
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC))
                                            .border(
                                                BorderStroke(
                                                    if (isSelected) 2.dp else 1.dp,
                                                    if (isSelected) Color(0xFF1696B2) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                                                ),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable { onSelectWork(work) }
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(4f / 3f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFF0F4C64), Color(0xFF38B8D1), Color(0xFFF5882E))
                                                    )
                                                )
                                        )
                                        Text(
                                            text = work.name,
                                            color = colors.textMain,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
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
// STEP 2: EDIT - TABBED EDITOR (Trim/Sound/Cover · Crop/Filters/Adjust)
// ============================================================================

@Composable
fun Step2AspectAndCoverStage(
    studioMode: CreatorStudioMode,
    slides: List<StudioSlide>,
    currentSlideIndex: Int,
    selectedRatioId: String,
    showThirdsGrid: Boolean,
    showWatermark: Boolean,
    cropOffsetX: Float,
    cropOffsetY: Float,
    coverPickerMode: String,
    selectedCoverFrameMs: Long,
    flashVideoDurationMs: Long,
    coverThumbnailBmp: Bitmap?,
    activeEditTab: StudioEditTab,
    isComparingOriginal: Boolean,
    flashTrimStartMs: Long,
    flashTrimEndMs: Long,
    flashPlaybackSpeed: Float,
    flashIsMuted: Boolean,
    flashIsFlipped: Boolean,
    onSelectEditTab: (StudioEditTab) -> Unit,
    onHoldOriginal: (Boolean) -> Unit,
    onRotatePhoto: () -> Unit,
    onFlipPhoto: () -> Unit,
    onResetFraming: () -> Unit,
    onSelectRatio: (String) -> Unit,
    onToggleThirdsGrid: () -> Unit,
    onToggleWatermark: () -> Unit,
    onCropOffsetChange: (Float, Float) -> Unit,
    onCenterCrop: () -> Unit,
    onCoverPickerModeChange: (String) -> Unit,
    onScrubCoverFrame: (Long) -> Unit,
    onPickCustomCover: () -> Unit,
    onSelectSlide: (Int) -> Unit,
    onRemoveSlide: (Int) -> Unit,
    onReorderSlide: (from: Int, to: Int) -> Unit,
    onUpdateTrim: (Long, Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFlip: () -> Unit,
    onSnapFifteenSec: () -> Unit,
    onOpenAudioMixer: () -> Unit,
    onSelectFilterPreset: (String) -> Unit,
    onUpdatePhotoAdjustments: (Float, Float, Float, Float, Float, Float) -> Unit
) {
    val colors = LocalOscellionColors.current
    val activeSlide = slides.getOrNull(currentSlideIndex) ?: slides.firstOrNull()

    // Determine available tabs for current mode
    val tabsForMode = remember(studioMode) {
        when (studioMode) {
            CreatorStudioMode.FLASH -> listOf(StudioEditTab.TRIM, StudioEditTab.SOUND, StudioEditTab.COVER)
            CreatorStudioMode.POST -> listOf(StudioEditTab.CROP, StudioEditTab.FILTERS, StudioEditTab.ADJUST)
            CreatorStudioMode.WORK -> listOf(StudioEditTab.FRAME)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 2A. Header & Canvas Stage Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with dynamic title & quick tools
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Lucide.WandSparkles, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(16.dp))
                        Text(
                            text = when (studioMode) {
                                CreatorStudioMode.FLASH -> "Edit reel"
                                CreatorStudioMode.POST -> "Edit photos"
                                CreatorStudioMode.WORK -> "Frame your work"
                            },
                            color = colors.textMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quick Tool Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (studioMode == CreatorStudioMode.POST) {
                            // Hold to Compare Original
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isComparingOriginal) Color(0xFFF5882E) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)))
                                    .clickable { onHoldOriginal(!isComparingOriginal) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Eye, contentDescription = "Compare", tint = if (isComparingOriginal) Color.White else colors.textMain, modifier = Modifier.size(14.dp))
                            }

                            // Flip Horizontal
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                                    .clickable { onFlipPhoto() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.FlipHorizontal, contentDescription = "Flip", tint = colors.textMain, modifier = Modifier.size(14.dp))
                            }

                            // Rotate 90
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                                    .clickable { onRotatePhoto() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.RotateCw, contentDescription = "Rotate", tint = colors.textMain, modifier = Modifier.size(14.dp))
                            }
                        }

                        // 3x3 Rule-of-Thirds Grid Toggle
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (showThirdsGrid) Color(0xFF1696B2) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)))
                                .clickable { onToggleThirdsGrid() }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Lucide.Grid3x3, contentDescription = null, tint = if (showThirdsGrid) Color.White else colors.textMain, modifier = Modifier.size(12.dp))
                                Text("3x3", color = if (showThirdsGrid) Color.White else colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Watermark Stamp Toggle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (showWatermark) Color(0xFFF5882E) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)))
                                .clickable { onToggleWatermark() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Award, contentDescription = "Watermark", tint = if (showWatermark) Color.White else colors.textMain, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Interactive Center Viewport Stage
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            when (selectedRatioId) {
                                "1:1" -> 1f
                                "4:5" -> 4f / 5f
                                "16:9" -> 16f / 9f
                                "9:16" -> 9f / 16f
                                "4:3" -> 4f / 3f
                                else -> 4f / 3f
                            }
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F172A))
                ) {
                    if (activeSlide != null) {
                        SlidePreviewContent(
                            slide = activeSlide,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // 3x3 Rule-of-Thirds Grid Overlay
                    if (showThirdsGrid) {
                        ThirdsGridOverlay(modifier = Modifier.fillMaxSize())
                    }

                    // Oscellion Minimalist Watermark Stamp
                    if (showWatermark) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Lucide.Sparkles, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                Text("@cameron_williamson", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Interactive 1:1 Square Crop Indicator Box
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.72f)
                            .align(Alignment.Center)
                            .border(BorderStroke(2.dp, Color(0xFFF5882E)), RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5882E).copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF5882E))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("1:1 Crop", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2B. Tab Bar (editTabBar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF1F5F9))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    tabsForMode.forEach { tab ->
                        val isSel = tab == activeEditTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (isSel) (if (colors.isDark) Color(0xFF282D3D) else Color.White) else Color.Transparent)
                                .shadow(if (isSel) 2.dp else 0.dp, RoundedCornerShape(11.dp))
                                .clickable { onSelectEditTab(tab) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSel) Color(0xFF1696B2) else colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = tab.label,
                                    color = if (isSel) colors.textMain else colors.textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // 2C. Tab Panels Based on Active Edit Tab
                when (activeEditTab) {
                    // ── CROP TAB (Photo) ──────────────────────────────────────────
                    StudioEditTab.CROP -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("ASPECT RATIO", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                STUDIO_RATIO_CONFIGS.forEach { config ->
                                    val isSel = config.id == selectedRatioId
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSel) Color(0xFF1696B2)
                                                else (if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC))
                                            )
                                            .border(
                                                BorderStroke(1.dp, if (isSel) Color(0xFF1696B2) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onSelectRatio(config.id) }
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(config.icon, contentDescription = null, tint = if (isSel) Color.White else colors.textMain, modifier = Modifier.size(16.dp))
                                        Text(config.label, color = if (isSel) Color.White else colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // 1:1 Profile Grid Crop Alignment Box
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF8FAFC))
                                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF0F172A))
                                        ) {
                                            activeSlide?.let {
                                                SlidePreviewContent(slide = it, modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                        Column {
                                            Text("1:1 Profile Grid Crop", color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text("X:${cropOffsetX.roundToInt()}% Y:${cropOffsetY.roundToInt()}%", color = Color(0xFF1696B2), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFEDF2F7))
                                            .clickable { onCenterCrop() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Focus, contentDescription = "Center Crop", tint = colors.textMain, modifier = Modifier.size(15.dp))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // X Slider
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Horizontal (X)", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                        Slider(
                                            value = cropOffsetX,
                                            onValueChange = { onCropOffsetChange(it, cropOffsetY) },
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(thumbColor = Color(0xFF1696B2), activeTrackColor = Color(0xFF1696B2)),
                                            modifier = Modifier.height(20.dp)
                                        )
                                    }
                                    // Y Slider
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Vertical (Y)", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                        Slider(
                                            value = cropOffsetY,
                                            onValueChange = { onCropOffsetChange(cropOffsetX, it) },
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(thumbColor = Color(0xFF1696B2), activeTrackColor = Color(0xFF1696B2)),
                                            modifier = Modifier.height(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── FILTERS TAB (Photo) ────────────────────────────────────────
                    StudioEditTab.FILTERS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("COLOR PRESETS", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                                        .clickable { onResetFraming() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.RotateCcw, contentDescription = "Reset", tint = colors.textMuted, modifier = Modifier.size(13.dp))
                                }
                            }

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(PHOTO_FILTER_PRESETS) { preset ->
                                    val isCur = (activeSlide?.filterName ?: "Original") == preset
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isCur) Color(0xFF1696B2)
                                                else (if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC))
                                            )
                                            .border(
                                                BorderStroke(1.dp, if (isCur) Color(0xFF1696B2) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onSelectFilterPreset(preset) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = preset,
                                            color = if (isCur) Color.White else colors.textMain,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── ADJUST TAB (Photo) ─────────────────────────────────────────
                    StudioEditTab.ADJUST -> {
                        val slide = activeSlide ?: StudioSlide()
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("FINE ADJUSTMENTS", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                                        .clickable {
                                            onUpdatePhotoAdjustments(0f, 1f, 1f, 0f, 0f, 0f)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.RotateCcw, contentDescription = "Reset All", tint = colors.textMuted, modifier = Modifier.size(13.dp))
                                }
                            }

                            // Sliders Grid
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Brightness
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Brightness", color = colors.textMuted, fontSize = 9.sp)
                                        Text("${slide.brightness.roundToInt()}", color = colors.textMain, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Slider(
                                        value = slide.brightness,
                                        onValueChange = { b ->
                                            onUpdatePhotoAdjustments(b, slide.contrast, slide.saturation, slide.warmth, slide.vignette, slide.sharpness)
                                        },
                                        valueRange = -100f..100f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFF5882E), activeTrackColor = Color(0xFFF5882E)),
                                        modifier = Modifier.height(20.dp)
                                    )
                                }
                                // Contrast
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Contrast", color = colors.textMuted, fontSize = 9.sp)
                                        Text(String.format("%.1fx", slide.contrast), color = colors.textMain, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Slider(
                                        value = slide.contrast,
                                        onValueChange = { c ->
                                            onUpdatePhotoAdjustments(slide.brightness, c, slide.saturation, slide.warmth, slide.vignette, slide.sharpness)
                                        },
                                        valueRange = 0.5f..2.0f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF1696B2), activeTrackColor = Color(0xFF1696B2)),
                                        modifier = Modifier.height(20.dp)
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Saturation
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Saturation", color = colors.textMuted, fontSize = 9.sp)
                                        Text(String.format("%.1fx", slide.saturation), color = colors.textMain, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Slider(
                                        value = slide.saturation,
                                        onValueChange = { s ->
                                            onUpdatePhotoAdjustments(slide.brightness, slide.contrast, s, slide.warmth, slide.vignette, slide.sharpness)
                                        },
                                        valueRange = 0f..3.0f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF1696B2), activeTrackColor = Color(0xFF1696B2)),
                                        modifier = Modifier.height(20.dp)
                                    )
                                }
                                // Warmth
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Warmth", color = colors.textMuted, fontSize = 9.sp)
                                        Text("${slide.warmth.roundToInt()}", color = colors.textMain, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Slider(
                                        value = slide.warmth,
                                        onValueChange = { w ->
                                            onUpdatePhotoAdjustments(slide.brightness, slide.contrast, slide.saturation, w, slide.vignette, slide.sharpness)
                                        },
                                        valueRange = -50f..50f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFF5882E), activeTrackColor = Color(0xFFF5882E)),
                                        modifier = Modifier.height(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ── TRIM TAB (Flash Video) ────────────────────────────────────
                    StudioEditTab.TRIM -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("TRIM TIMELINE", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                        .clickable { onSnapFifteenSec() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Snap to 15s", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("In: ${flashTrimStartMs / 1000}s", color = colors.textMuted, fontSize = 9.sp)
                                    Slider(
                                        value = flashTrimStartMs.toFloat(),
                                        onValueChange = { onUpdateTrim(it.toLong(), flashTrimEndMs) },
                                        valueRange = 0f..(flashTrimEndMs - 1000L).coerceAtLeast(1000L).toFloat(),
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFF5882E), activeTrackColor = Color(0xFFF5882E)),
                                        modifier = Modifier.height(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Out: ${flashTrimEndMs / 1000}s", color = colors.textMuted, fontSize = 9.sp)
                                    Slider(
                                        value = flashTrimEndMs.toFloat(),
                                        onValueChange = { onUpdateTrim(flashTrimStartMs, it.toLong()) },
                                        valueRange = (flashTrimStartMs + 1000L).toFloat()..(flashVideoDurationMs.coerceAtLeast(15000L).toFloat()),
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF1696B2), activeTrackColor = Color(0xFF1696B2)),
                                        modifier = Modifier.height(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ── SOUND TAB (Flash Video) ───────────────────────────────────
                    StudioEditTab.SOUND -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC))
                                        .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                                        .clickable { onToggleMute() }
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(if (flashIsMuted) Lucide.VolumeX else Lucide.Volume2, contentDescription = null, tint = if (flashIsMuted) Color(0xFFEF4444) else Color(0xFF1696B2), modifier = Modifier.size(18.dp))
                                    Text("Audio", color = colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            item {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC))
                                        .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                                        .clickable { onOpenAudioMixer() }
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Lucide.Music, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(18.dp))
                                    Text("Add music", color = colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            item {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC))
                                        .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                                        .clickable { onCycleSpeed() }
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Lucide.Gauge, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(18.dp))
                                    Text("${flashPlaybackSpeed}x Speed", color = colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            item {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC))
                                        .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                                        .clickable { onToggleFlip() }
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Lucide.FlipHorizontal, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(18.dp))
                                    Text("Mirror", color = colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            item {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC))
                                        .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                                        .clickable { onSnapFifteenSec() }
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Lucide.WandSparkles, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    Text("Snap 15s", color = colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            item {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC))
                                        .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                                        .clickable { onOpenAudioMixer() }
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Lucide.SlidersVertical, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(18.dp))
                                    Text("Mixer", color = colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // ── COVER TAB (Flash Video) ───────────────────────────────────
                    StudioEditTab.COVER -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("REEL COVER FRAME", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFEDF2F7))
                                        .padding(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (coverPickerMode == "frame") Color(0xFFF5882E) else Color.Transparent)
                                            .clickable { onCoverPickerModeChange("frame") }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Frame", color = if (coverPickerMode == "frame") Color.White else colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (coverPickerMode == "custom") Color(0xFFF5882E) else Color.Transparent)
                                            .clickable { onCoverPickerModeChange("custom") }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Upload", color = if (coverPickerMode == "custom") Color.White else colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (coverPickerMode == "frame") {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Scrub Video Poster Frame", color = colors.textMuted, fontSize = 10.sp)
                                        Text(String.format("%.1fs", selectedCoverFrameMs / 1000f), color = Color(0xFF1696B2), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = selectedCoverFrameMs.toFloat(),
                                        onValueChange = { onScrubCoverFrame(it.toLong()) },
                                        valueRange = 0f..(flashVideoDurationMs.coerceAtLeast(15000L).toFloat()),
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFF1696B2), activeTrackColor = Color(0xFF1696B2)),
                                        modifier = Modifier.height(20.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(BorderStroke(1.dp, Color(0xFF1696B2)), RoundedCornerShape(14.dp))
                                        .background(if (colors.isDark) Color(0xFF1696B2).copy(alpha = 0.1f) else Color(0xFFE0F7FA))
                                        .clickable { onPickCustomCover() }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Lucide.Upload, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(15.dp))
                                        Text("Upload Custom High-Res Cover", color = Color(0xFF1696B2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // ── FRAME TAB (Work) ──────────────────────────────────────────
                    StudioEditTab.FRAME -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("FRAME DIMENSIONS", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                STUDIO_RATIO_CONFIGS.take(4).forEach { config ->
                                    val isSel = config.id == selectedRatioId
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSel) Color(0xFF1696B2)
                                                else (if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC))
                                            )
                                            .border(
                                                BorderStroke(1.dp, if (isSel) Color(0xFF1696B2) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onSelectRatio(config.id) }
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(config.label, color = if (isSel) Color.White else colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2D. Multi-Slide Carousel Strip (Photo & Work Modes)
        if (studioMode != CreatorStudioMode.FLASH && slides.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                        .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Lucide.GalleryHorizontalEnd, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(16.dp))
                            Text("Carousel Slides", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1696B2).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("${currentSlideIndex + 1} / ${slides.size}", color = Color(0xFF1696B2), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(slides) { idx, slide ->
                            val isCur = idx == currentSlideIndex
                            Column(
                                modifier = Modifier
                                    .width(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC))
                                    .border(
                                        BorderStroke(
                                            if (isCur) 2.dp else 1.dp,
                                            if (isCur) Color(0xFFF5882E) else (if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
                                        ),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectSlide(idx) }
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A))
                                ) {
                                    SlidePreviewContent(slide = slide, modifier = Modifier.fillMaxSize())
                                    // Number badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(3.dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(if (isCur) Color(0xFFF5882E) else Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${idx + 1}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Reorder & Delete controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Move Left
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFEDF2F7))
                                            .clickable { if (idx > 0) onReorderSlide(idx, idx - 1) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.ChevronLeft, contentDescription = "Move Left", tint = colors.textMuted, modifier = Modifier.size(11.dp))
                                    }

                                    // Remove
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                            .clickable { onRemoveSlide(idx) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.X, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(10.dp))
                                    }

                                    // Move Right
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFEDF2F7))
                                            .clickable { if (idx < slides.lastIndex) onReorderSlide(idx, idx + 1) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.ChevronRight, contentDescription = "Move Right", tint = colors.textMuted, modifier = Modifier.size(11.dp))
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
// STEP 3: POST DETAILS, METADATA & ADVANCED PRIVACY SETTINGS (V3 SPEC)
// ============================================================================

@Composable
fun Step3PostDetailsStage(
    studioMode: CreatorStudioMode,
    slides: List<StudioSlide>,
    currentSlideIndex: Int,
    selectedRatioId: String,
    postTitle: String,
    postDescription: String,
    selectedCategoryTag: String?,
    customCategoryTag: String,
    locationString: String,
    altTextString: String,
    hashtags: List<String>,
    mentionedUsers: List<UserProfile>,
    audienceVisibility: String,
    hideLikesCount: Boolean,
    turnOffComments: Boolean,
    turnOffRemix: Boolean,
    includeOriginalAudio: Boolean,
    allowDownloads: Boolean,
    highQualityUpload: Boolean,
    scheduleReleaseEnabled: Boolean = false,
    scheduleDateTime: String = "Tomorrow, 10:00 AM",
    remixAttributionEnabled: Boolean = true,
    isAdvancedSettingsOpen: Boolean = false,
    isAltTextOpen: Boolean = false,
    onEditCoverClick: () -> Unit = {},
    onToggleAdvancedSettings: () -> Unit = {},
    onToggleAltText: () -> Unit = {},
    onToggleScheduleRelease: (Boolean) -> Unit = {},
    onScheduleDateTimeChange: (String) -> Unit = {},
    onToggleRemixAttribution: (Boolean) -> Unit = {},
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onCustomCategoryChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onAltTextChange: (String) -> Unit,
    onAddHashtag: (String) -> Unit,
    onRemoveHashtag: (String) -> Unit,
    onOpenMentionModal: () -> Unit,
    onRemoveMention: (UserProfile) -> Unit,
    onAudienceSelect: (String) -> Unit,
    onToggleHideLikes: (Boolean) -> Unit,
    onToggleComments: (Boolean) -> Unit,
    onToggleRemix: (Boolean) -> Unit,
    onToggleAudio: (Boolean) -> Unit,
    onToggleDownloads: (Boolean) -> Unit,
    onToggleHighQuality: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current
    val activeSlide = slides.getOrNull(currentSlideIndex) ?: slides.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 3-0. Post Summary Header Row (Instagram "New post" header row) ─────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 68x68 Thumbnail with bottom "Edit" pill button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                ) {
                    if (activeSlide != null) {
                        SlidePreviewContent(slide = activeSlide, modifier = Modifier.fillMaxSize())
                    }
                    // Bottom "Edit" overlay button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable { onEditCoverClick() }
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (studioMode == CreatorStudioMode.FLASH) "Edit cover" else "Edit",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Metadata Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Mode Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1696B2).copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (studioMode) {
                                    CreatorStudioMode.FLASH -> "REEL"
                                    CreatorStudioMode.POST -> "POST"
                                    CreatorStudioMode.WORK -> "WORK"
                                },
                                color = Color(0xFF1696B2),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Audience Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFEDF2F7))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Lucide.Globe, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(10.dp))
                            Text(audienceVisibility.replaceFirstChar { it.uppercaseChar() }, color = colors.textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = when (studioMode) {
                            CreatorStudioMode.FLASH -> "15.0s · 9:16 Vertical"
                            CreatorStudioMode.POST -> "${slides.size} Photos · $selectedRatioId"
                            CreatorStudioMode.WORK -> "Room SQLite · activeWorks"
                        },
                        color = colors.textMain,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add a caption, tag people and choose who sees it",
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ── 3A. Content & Copywriting (Title, Caption & Quick Formatting) ──────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title Input with Character Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Lucide.PenTool, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(15.dp))
                        Text("Headline & Story", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("${postTitle.length} / 100", color = colors.textMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }

                OutlinedTextField(
                    value = postTitle,
                    onValueChange = onTitleChange,
                    placeholder = { Text("Give your creation a title...", fontSize = 11.sp) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )

                // Caption / Story Textarea
                OutlinedTextField(
                    value = postDescription,
                    onValueChange = onDescriptionChange,
                    placeholder = { Text("Write a caption, tell the story behind this work...", fontSize = 11.sp) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 85.dp),
                    textStyle = TextStyle(fontSize = 11.sp, lineHeight = 16.sp)
                )

                // Formatting Toolbar below Caption (#, @, Bold, Alt, Counter)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Insert #
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (colors.isDark) Color(0xFF1A2634) else Color(0xFFEBF7FA))
                                .clickable { onDescriptionChange("$postDescription #") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Hash, contentDescription = "Hashtag", tint = Color(0xFF1696B2), modifier = Modifier.size(15.dp))
                        }

                        // Insert @
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (colors.isDark) Color(0xFF2D1E1E) else Color(0xFFFEF2E8))
                                .clickable { onOpenMentionModal() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.AtSign, contentDescription = "Mention", tint = Color(0xFFF5882E), modifier = Modifier.size(15.dp))
                        }

                        // Bold Format
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                                .clickable { onDescriptionChange("$postDescription **bold**") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Bold, contentDescription = "Bold", tint = colors.textMain, modifier = Modifier.size(14.dp))
                        }

                        // Alt-Text Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAltTextOpen) Color(0xFF1696B2) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)))
                                .clickable { onToggleAltText() }
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Lucide.Accessibility, contentDescription = null, tint = if (isAltTextOpen) Color.White else colors.textMain, modifier = Modifier.size(12.dp))
                                Text("Alt", color = if (isAltTextOpen) Color.White else colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("${postDescription.length} / 1,500", color = colors.textMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }

                // Expandable Accessibility Alt-Text Input
                if (isAltTextOpen) {
                    OutlinedTextField(
                        value = altTextString,
                        onValueChange = onAltTextChange,
                        placeholder = { Text("Describe media for visually impaired users...", fontSize = 10.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        textStyle = TextStyle(fontSize = 11.sp)
                    )
                }
            }
        }

        // ── 3B. Categorization, Location, Hashtags & Mentions ──────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Lucide.Shapes, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(15.dp))
                        Text("Category", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ALGO_CATEGORIES) { cat ->
                            val isSel = selectedCategoryTag == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) Color(0xFF1696B2) else (if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF1F5F9)))
                                    .border(BorderStroke(1.dp, if (isSel) Color(0xFF1696B2) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))), RoundedCornerShape(12.dp))
                                    .clickable { onCategorySelect(cat) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(cat, color = if (isSel) Color.White else colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Location Geotag Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Lucide.MapPin, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(15.dp))
                            Text("Location", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(locationString, color = Color(0xFF1696B2), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Zurich, Switzerland", "Tokyo, Japan", "New York, USA", "London, UK", "Berlin, Germany")) { loc ->
                            val isCur = locationString == loc
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCur) Color(0xFFF5882E) else (if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF1F5F9)))
                                    .clickable { onLocationChange(loc) }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(loc, color = if (isCur) Color.White else colors.textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Hashtags
                var tagInput by remember { mutableStateOf("") }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            placeholder = { Text("Add discovery tags...", fontSize = 11.sp) },
                            leadingIcon = { Icon(Lucide.Hash, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(14.dp)) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(42.dp),
                            textStyle = TextStyle(fontSize = 11.sp)
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1696B2))
                                .clickable {
                                    if (tagInput.isNotBlank()) {
                                        onAddHashtag(tagInput)
                                        tagInput = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Plus, contentDescription = "Add Tag", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (hashtags.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            hashtags.forEach { tag ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFEDF2F7))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("#$tag", color = colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Icon(
                                        Lucide.X,
                                        contentDescription = "Remove",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(12.dp).clickable { onRemoveHashtag(tag) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Mentions
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Lucide.AtSign, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(15.dp))
                            Text("Mentions & Collaborators", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF5882E).copy(alpha = 0.15f))
                                .clickable { onOpenMentionModal() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.UserPlus, contentDescription = "Add", tint = Color(0xFFF5882E), modifier = Modifier.size(13.dp))
                        }
                    }

                    if (mentionedUsers.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            mentionedUsers.forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (colors.isDark) Color(0xFF1E212B) else Color(0xFFEDF2F7))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("@${user.username}", color = colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Icon(
                                        Lucide.X,
                                        contentDescription = "Remove",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(12.dp).clickable { onRemoveMention(user) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 3C. Audience Visibility Grid ──────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Lucide.Eye, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(15.dp))
                    Text("Audience Visibility", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("public", "followers", "friends", "private").forEach { aud ->
                        val isSel = audienceVisibility == aud
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) Color(0xFF1696B2) else (if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF8FAFC)))
                                .border(BorderStroke(1.dp, if (isSel) Color(0xFF1696B2) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))), RoundedCornerShape(12.dp))
                                .clickable { onAudienceSelect(aud) }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(aud.replaceFirstChar { it.uppercaseChar() }, color = if (isSel) Color.White else colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── 3D & 3E. Collapsible Advanced Settings (Permissions, Schedule & Remix) ─
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Collapsible Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAdvancedSettings() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Lucide.SlidersHorizontal, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(16.dp))
                        Text("Advanced settings", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFEDF2F7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Defaults", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            imageVector = if (isAdvancedSettingsOpen) Lucide.ChevronUp else Lucide.ChevronDown,
                            contentDescription = "Expand",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Expandable Body
                if (isAdvancedSettingsOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SettingToggleRow(
                            title = "Hide Like Count",
                            subtitle = "Only you can see total likes on this post",
                            checked = hideLikesCount,
                            onCheckedChange = onToggleHideLikes,
                            icon = Lucide.HeartOff
                        )

                        SettingToggleRow(
                            title = "Disable Comments",
                            subtitle = "Prevent replies and comments on this creation",
                            checked = turnOffComments,
                            onCheckedChange = onToggleComments,
                            icon = Lucide.MessageSquareOff
                        )

                        SettingToggleRow(
                            title = "Disable Remix & Duets",
                            subtitle = "Prevent other creators from remixing this media",
                            checked = turnOffRemix,
                            onCheckedChange = onToggleRemix,
                            icon = Lucide.CopySlash
                        )

                        SettingToggleRow(
                            title = "Include Original Audio",
                            subtitle = "Publish original recorded soundtrack for discovery",
                            checked = includeOriginalAudio,
                            onCheckedChange = onToggleAudio,
                            icon = Lucide.Disc
                        )

                        SettingToggleRow(
                            title = "Allow Downloads",
                            subtitle = "Let users save original high-res media locally",
                            checked = allowDownloads,
                            onCheckedChange = onToggleDownloads,
                            icon = Lucide.Download
                        )

                        SettingToggleRow(
                            title = "High Quality Upload",
                            subtitle = "Upload at full bitrate without downscaling",
                            checked = highQualityUpload,
                            onCheckedChange = onToggleHighQuality,
                            icon = Lucide.Sparkles
                        )

                        // Schedule Release
                        SettingToggleRow(
                            title = "Schedule Release",
                            subtitle = "Publish automatically at a future time",
                            checked = scheduleReleaseEnabled,
                            onCheckedChange = onToggleScheduleRelease,
                            icon = Lucide.CalendarClock
                        )

                        if (scheduleReleaseEnabled) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Tomorrow, 10:00 AM", "Tomorrow, 06:00 PM", "Weekend, 12:00 PM").forEach { time ->
                                    val isCur = scheduleDateTime == time
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isCur) Color(0xFFF5882E) else (if (colors.isDark) Color(0xFF1E212B) else Color(0xFFF1F5F9)))
                                            .clickable { onScheduleDateTimeChange(time) }
                                            .padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        Text(time, color = if (isCur) Color.White else colors.textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Remix Attribution
                        SettingToggleRow(
                            title = "Attach Remix Attribution",
                            subtitle = "Credit original author and link project hierarchy",
                            checked = remixAttributionEnabled,
                            onCheckedChange = onToggleRemixAttribution,
                            icon = Lucide.GitBranch
                        )
                    }
                }
            }
        }
    }
}


// ============================================================================
// STEP 4: SAFETY SCAN, CLOUD UPLOAD & COMPLETION STAGE
// ============================================================================

@Composable
fun Step4UploadAndShareStage(
    studioMode: CreatorStudioMode,
    slides: List<StudioSlide>,
    currentSlideIndex: Int,
    selectedRatioId: String,
    postTitle: String,
    locationString: String,
    categoryTag: String,
    audienceVisibility: String,
    uploadProgress: Float,
    uploadStage: Int,
    isUploadFinished: Boolean,
    generatedDeepLink: String,
    errorMessage: String?,
    onCopyDeepLink: () -> Unit,
    onShareLink: () -> Unit,
    onDoneClick: () -> Unit,
    onRetryUpload: () -> Unit,
    onShareTargetClick: (ShareTargetItem) -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    val activeSlide = slides.getOrNull(currentSlideIndex) ?: slides.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 4A. Live 4-Stage Stepper Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1696B2).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.CloudUpload, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("CLOUD PIPELINE", color = Color(0xFF1696B2), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                            Text(
                                if (isUploadFinished) "Publication Successful" else "Publishing Creation...",
                                color = colors.textMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "${(uploadProgress * 100).toInt()}%",
                        color = Color(0xFFF5882E),
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Master Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF1F5F9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(uploadProgress.coerceIn(0f, 1f))
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1696B2), Color(0xFFF5882E))
                                )
                            )
                    )
                }

                // 4 Pipeline Stages
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Stage 1
                    PipelineStageRow(
                        title = "Safety & NSFW Verification",
                        subtitle = "Automated media inspection",
                        icon = Lucide.ShieldCheck,
                        isDone = uploadStage > 1 || isUploadFinished,
                        isActive = uploadStage == 1 && !isUploadFinished
                    )

                    // Stage 2
                    PipelineStageRow(
                        title = "Transcoding & 1:1 Crop",
                        subtitle = "Generating adaptive streams",
                        icon = Lucide.Cpu,
                        isDone = uploadStage > 2 || isUploadFinished,
                        isActive = uploadStage == 2 && !isUploadFinished
                    )

                    // Stage 3
                    PipelineStageRow(
                        title = "Cloud Storage Upload",
                        subtitle = "Streaming binary chunks",
                        icon = Lucide.Upload,
                        isDone = uploadStage > 3 || isUploadFinished,
                        isActive = uploadStage == 3 && !isUploadFinished
                    )

                    // Stage 4
                    PipelineStageRow(
                        title = "Firestore Index & Deep Link",
                        subtitle = "Writing document to feed",
                        icon = Lucide.Link,
                        isDone = isUploadFinished,
                        isActive = uploadStage == 4 && !isUploadFinished
                    )
                }

                // Error Message if failed
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(errorMessage, color = Color(0xFFB91C1C), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text(
                                    "Retry Upload",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onRetryUpload() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4B. Completion & Live Post Card (When upload succeeds)
        if (isUploadFinished) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Deep Link Share Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(20.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Published Live", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(generatedDeepLink, color = Color(0xFF1696B2), fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Copy Link Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1696B2).copy(alpha = 0.12f))
                                .clickable { onCopyDeepLink() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Copy, contentDescription = "Copy Link", tint = Color(0xFF1696B2), modifier = Modifier.size(16.dp))
                        }
                    }

                    // Share-to sheet (Instagram / Facebook / YouTube style)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "SHARE TO",
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
                            SHARE_TARGETS.forEach { target ->
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onShareTargetClick(target) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (colors.isDark) target.bgTint.copy(alpha = 0.2f) else target.bgTint),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = target.icon,
                                            contentDescription = target.label,
                                            tint = target.iconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = target.label,
                                        color = colors.textMain,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Live Post Preview Card (Matches reference HTML left phone screen)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Hero Image with Badges
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0F172A))
                        ) {
                            if (activeSlide != null) {
                                SlidePreviewContent(slide = activeSlide, modifier = Modifier.fillMaxSize())
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${studioMode.name} · $selectedRatioId",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.9f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(categoryTag, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Creator Avatar Stack & Audience
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1696B2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("CS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Chroma Creator", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF1696B2).copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Lucide.Globe, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(10.dp))
                                    Text(audienceVisibility.replaceFirstChar { it.uppercaseChar() }, color = Color(0xFF1696B2), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Title & Location
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = postTitle.ifBlank { "New Creation" },
                                color = colors.textMain,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Lucide.MapPin, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(11.dp))
                                Text(locationString, color = colors.textMuted, fontSize = 11.sp)
                            }
                        }

                        // 4-Column Live Engagement Simulation Row (Count-up animation)
                        var animTrigger by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            animTrigger = true
                        }
                        val liveViews by animateIntAsState(targetValue = if (animTrigger) 1240 else 0, animationSpec = tween(1200, easing = LinearOutSlowInEasing), label = "views")
                        val liveLikes by animateIntAsState(targetValue = if (animTrigger) 248 else 0, animationSpec = tween(1000, easing = LinearOutSlowInEasing), label = "likes")
                        val liveComments by animateIntAsState(targetValue = if (animTrigger) 42 else 0, animationSpec = tween(800, easing = LinearOutSlowInEasing), label = "comments")
                        val liveShares by animateIntAsState(targetValue = if (animTrigger) 18 else 0, animationSpec = tween(600, easing = LinearOutSlowInEasing), label = "shares")

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (colors.isDark) Color(0xFF1E2230) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatPillItem(icon = Lucide.Eye, count = "$liveViews", label = "Views", tint = Color(0xFF1696B2))
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)))
                                StatPillItem(icon = Lucide.Heart, count = "$liveLikes", label = "Likes", tint = Color(0xFFEF4444))
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)))
                                StatPillItem(icon = Lucide.MessageCircle, count = "$liveComments", label = "Comments", tint = Color(0xFFF5882E))
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)))
                                StatPillItem(icon = Lucide.Share2, count = "$liveShares", label = "Shares", tint = Color(0xFF8B5CF6))
                            }
                        }

                        // Bottom Actions: Share & Done
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF1F5F9))
                                    .clickable { onShareLink() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Lucide.Share2, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(16.dp))
                                    Text("Share", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            PrimaryGradientButton(
                                onClick = onDoneClick,
                                modifier = Modifier.weight(1.5f).height(44.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text("View Live Post", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
fun PipelineStageRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDone: Boolean,
    isActive: Boolean
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF8FAFC))
            .border(
                1.dp,
                if (isActive) Color(0xFFF5882E) else (if (colors.isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFE2E8F0)),
                RoundedCornerShape(14.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    if (isDone) Color(0xFF10B981)
                    else if (isActive) Color(0xFFF5882E)
                    else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDone) Lucide.Check else icon,
                contentDescription = null,
                tint = if (isDone || isActive) Color.White else colors.textMuted,
                modifier = Modifier.size(15.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = colors.textMuted, fontSize = 10.sp)
        }

        Text(
            text = if (isDone) "Done" else if (isActive) "Running..." else "Waiting",
            color = if (isDone) Color(0xFF10B981) else if (isActive) Color(0xFFF5882E) else colors.textMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ============================================================================
// MODAL 1: SAVED DRAFTS DRAWER MODAL
// ============================================================================

@Composable
fun SavedDraftsDrawerModal(
    drafts: List<CreatorPostDraft>,
    onSelectDraft: (CreatorPostDraft) -> Unit,
    onDeleteDraft: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Lucide.Bookmark, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(18.dp))
                    Text("Saved Drafts (${drafts.size})", color = colors.textMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.X, contentDescription = "Close", tint = colors.textMain, modifier = Modifier.size(16.dp))
                }
            }

            if (drafts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Lucide.Bookmark, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(36.dp))
                        Text("No saved drafts found", color = colors.textMuted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(drafts) { draft ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF8FAFC))
                                .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                .clickable { onSelectDraft(draft) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFF1696B2).copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(draft.mode.uppercase(), color = Color(0xFF1696B2), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(draft.title.ifBlank { "Untitled Draft" }, color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                Text(sdf.format(Date(draft.timestamp)), color = colors.textMuted, fontSize = 10.sp)
                            }

                            // Delete draft button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                    .clickable { onDeleteDraft(draft.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Trash2, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// MODAL 2: MENTION USER SEARCH MODAL
// ============================================================================

@Composable
fun MentionUserSearchModal(
    discoverRepository: DiscoverRepository,
    selectedUsers: List<UserProfile>,
    onToggleUser: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    var searchQuery by remember { mutableStateOf("") }
    var suggestedUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(searchQuery) {
        try {
            suggestedUsers = if (searchQuery.isBlank()) {
                discoverRepository.getSuggestedCreators(15)
            } else {
                discoverRepository.searchUsers(searchQuery)
            }
        } catch (_: Exception) {
            suggestedUsers = emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Lucide.AtSign, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(18.dp))
                    Text("Mention Creators (${selectedUsers.size}/5)", color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1696B2)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Done", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Search Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF1F5F9))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = colors.textMain, fontSize = 12.sp),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text("Search creators...", color = colors.textMuted, fontSize = 12.sp)
                        }
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Clear",
                        tint = colors.textMuted,
                        modifier = Modifier.size(14.dp).clickable { searchQuery = "" }
                    )
                }
            }

            // User List
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(suggestedUsers) { user ->
                    val isSelected = selectedUsers.any { it.uid == user.uid }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) (if (colors.isDark) Color(0xFF2E1A47) else Color(0xFFF3E8FF)) else Color.Transparent)
                            .clickable { onToggleUser(user) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text(user.displayName.ifBlank { user.username }, color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("@${user.username}", color = colors.textMuted, fontSize = 10.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF1696B2) else (if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


// ============================================================================
// MODAL 3: AUDIO TRACK & SOUND MIXER SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrackMixerModalSheet(
    origVolume: Float,
    soundtrackVolume: Float,
    selectedTrack: AudioTrackItem?,
    tracksList: List<AudioTrackItem> = FEATURED_SOUNDTRACKS,
    onOrigVolumeChange: (Float) -> Unit,
    onSoundtrackVolumeChange: (Float) -> Unit,
    onSelectTrack: (AudioTrackItem) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (colors.isDark) Color(0xFF161922) else Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (colors.isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Lucide.Music, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(20.dp))
                    Text("Audio Track & Mixer", color = colors.textMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF232734) else Color(0xFFF1F5F9))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.X, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                }
            }

            // Volume Balance Sliders
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (colors.isDark) Color(0xFF1E2230) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Original Audio Slider
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Lucide.Mic, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(13.dp))
                                Text("Original", color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("${origVolume.toInt()}%", color = colors.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = origVolume,
                            onValueChange = onOrigVolumeChange,
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF1696B2),
                                activeTrackColor = Color(0xFF1696B2)
                            )
                        )
                    }

                    // Soundtrack Volume Slider
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Lucide.Music, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(13.dp))
                                Text("Soundtrack", color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("${soundtrackVolume.toInt()}%", color = colors.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = soundtrackVolume,
                            onValueChange = onSoundtrackVolumeChange,
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFF5882E),
                                activeTrackColor = Color(0xFFF5882E)
                            )
                        )
                    }
                }
            }

            // Track list title
            Text("Featured Soundtracks", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            // Tracks list
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tracksList) { track ->
                    val isSelected = selectedTrack?.id == track.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTrack(track) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            if (colors.isDark) Color(0xFF1E283D) else Color(0xFFEFF6FF)
                        } else {
                            if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF8FAFC)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF007AFF) else if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF007AFF) else Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isSelected) Lucide.Check else Lucide.Play,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(track.title, color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${track.artist} • ${track.duration}", color = colors.textMuted, fontSize = 10.sp)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(track.genre, color = Color(0xFF3B82F6), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

// ============================================================================
// MODAL 4: DISCARD DRAFT CONFIRMATION SHEET (Apple HIG 3D Trash Bin)
// ============================================================================

@Composable
fun DiscardDraftConfirmationModal(
    onSaveDraft: () -> Unit,
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val trashState = rememberAppleTrashState()
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onKeepEditing,
        modifier = Modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(28.dp),
        containerColor = if (colors.isDark) Color(0xFF161922) else Color.White,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Apple HIG 3D Cupertino Blue Trash Bin
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scope.launch {
                                trashState.triggerDeleteAnimation()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AppleTrashBinIcon(
                        state = trashState,
                        modifier = Modifier.size(96.dp)
                    )
                }

                Text(
                    text = "Save or Toss Creation?",
                    color = colors.textMain,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Store your current work in DraftPostRepository or toss into the bin.",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Action 1: Save Draft Button
                PrimaryGradientButton(
                    onClick = onSaveDraft,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Lucide.Bookmark, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Save Draft", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action 2: Toss & Discard Button
                Button(
                    onClick = {
                        scope.launch {
                            trashState.triggerDeleteAnimation()
                            delay(500)
                            onDiscard()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.isDark) Color(0xFF37171C) else Color(0xFFFFF1F2),
                        contentColor = Color(0xFFE11D48)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Lucide.Trash2, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(15.dp))
                        Text("Toss & Discard (Spring)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action 3: Keep Editing
                TextButton(
                    onClick = onKeepEditing,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("Keep Editing", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {}
    )
}

// ============================================================================
// MODAL 5: FULL LIVE POST VIEW MODAL
// ============================================================================

@Composable
fun FullLivePostViewModal(
    postTitle: String,
    postDescription: String,
    locationString: String,
    categoryTag: String,
    hashtags: List<String>,
    activeSlide: StudioSlide?,
    deepLinkUrl: String,
    onDismiss: () -> Unit,
    onReadyForNewUpload: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (colors.isDark) Color(0xFF0F1117) else Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Hero Media Preview (340dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(Color.Black)
                ) {
                    if (activeSlide != null) {
                        SlidePreviewContent(slide = activeSlide, modifier = Modifier.fillMaxSize())
                    }

                    // Back / Close Icon
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.ArrowLeft, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(18.dp))
                    }

                    // Floating Category Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(categoryTag, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Rounded Sheet Content (-20dp offset effect)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = if (colors.isDark) Color(0xFF161922) else Color.White,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Author info & Verified Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1696B2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("CS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Published by", color = colors.textMuted, fontSize = 11.sp)
                                        Text("@you", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Icon(Lucide.Sparkles, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(12.dp))
                                    }
                                    Text("Just now • Live on Feed", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Post Title & Location
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = postTitle.ifBlank { "Untitled Post" },
                                color = colors.textMain,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (locationString.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Lucide.MapPin, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(13.dp))
                                    Text(locationString, color = colors.textMuted, fontSize = 12.sp)
                                }
                            }
                        }

                        // Caption & Description
                        if (postDescription.isNotBlank()) {
                            Text(
                                text = postDescription,
                                color = colors.textMain,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }

                        // Hashtags
                        if (hashtags.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                hashtags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (colors.isDark) Color(0xFF232734) else Color(0xFFF1F5F9))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("#$tag", color = Color(0xFF1696B2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Social Stats Simulation Row
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (colors.isDark) Color(0xFF1E2230) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                StatPillItem(icon = Lucide.Heart, count = "0", label = "Likes", tint = Color(0xFFEF4444))
                                StatPillItem(icon = Lucide.MessageSquare, count = "0", label = "Comments", tint = Color(0xFF3B82F6))
                                StatPillItem(icon = Lucide.Share2, count = "0", label = "Shares", tint = Color(0xFF10B981))
                                StatPillItem(icon = Lucide.Eye, count = "1", label = "Views", tint = Color(0xFFF5882E))
                            }
                        }

                        // Deep link Share Bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (colors.isDark) Color(0xFF1E2230) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Lucide.Link, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                                Text(
                                    text = deepLinkUrl,
                                    color = colors.textMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF007AFF))
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(deepLinkUrl))
                                            Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Copy", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Bottom Actions: Done / New Upload
                        PrimaryGradientButton(
                            onClick = onReadyForNewUpload,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Lucide.Sparkles, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Studio Ready for New Creation", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPillItem(icon: ImageVector, count: String, label: String, tint: Color) {
    val colors = LocalOscellionColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Text(count, color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = colors.textMuted, fontSize = 9.sp)
    }
}


// ============================================================================
// MODAL: UNIFIED POST PAYLOAD & COMPOSE INSPECTOR (macOS Window Style)
// ============================================================================

@Composable
fun UnifiedPostInspectorModal(
    studioMode: CreatorStudioMode,
    currentStep: StudioStep,
    selectedRatioId: String,
    slidesCount: Int,
    postTitle: String,
    postDescription: String,
    categoryTag: String,
    locationString: String,
    hashtags: List<String>,
    mentionedUsers: List<String>,
    audienceVisibility: String,
    hideLikesCount: Boolean,
    turnOffComments: Boolean,
    turnOffRemix: Boolean,
    includeOriginalAudio: Boolean,
    allowDownloads: Boolean,
    highQualityUpload: Boolean,
    scheduleReleaseEnabled: Boolean,
    scheduleDateTime: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("json") } // "json" or "kt"

    val collectionTarget = when (studioMode) {
        CreatorStudioMode.FLASH -> "flash_videos"
        CreatorStudioMode.POST -> "community_posts (photos)"
        CreatorStudioMode.WORK -> "community_posts (vector_work)"
    }

    val jsonPayload = """{
  "postType": "${studioMode.name.lowercase()}",
  "targetCollection": "$collectionTarget",
  "title": "${postTitle.ifBlank { "Untitled Creation" }}",
  "description": "${postDescription.replace(""", "\\"").take(80)}...",
  "aspectRatio": "$selectedRatioId",
  "categoryTag": "$categoryTag",
  "location": "$locationString",
  "hashtags": [${hashtags.joinToString(", ") { "\"$it\"" }}],
  "mentions": [${mentionedUsers.joinToString(", ") { "\"$it\"" }}],
  "audience": "$audienceVisibility",
  "settings": {
    "hideLikesCount": $hideLikesCount,
    "turnOffComments": $turnOffComments,
    "turnOffRemix": $turnOffRemix,
    "includeOriginalAudio": $includeOriginalAudio,
    "allowDownloads": $allowDownloads,
    "highQualityUpload": $highQualityUpload,
    "scheduleEnabled": $scheduleReleaseEnabled,
    "scheduleDateTime": "$scheduleDateTime"
  },
  "slidesCount": $slidesCount,
  "currentStep": "${currentStep.title}",
  "status": "ready_for_cloud_pipeline"
}"""

    val composeCode = """// Jetpack Compose Model Snapshot
val postPayload = UnifiedPostPayload(
    mode = CreatorStudioMode.${studioMode.name},
    title = "${postTitle.ifBlank { "Untitled" }}",
    aspectRatio = "$selectedRatioId",
    category = "$categoryTag",
    audience = "$audienceVisibility",
    slidesCount = $slidesCount,
    hideLikes = $hideLikesCount,
    turnOffComments = $turnOffComments
)"""

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {}
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // macOS Window Toolbar (Traffic lights, tabs, copy button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Traffic lights
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFFF5F56)).clickable { onDismiss() })
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                    }

                    // Mode Switcher Tabs
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedTab == "json") Color(0xFF1E293B) else Color.Transparent)
                                .clickable { selectedTab = "json" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Payload.json", color = if (selectedTab == "json") Color.White else Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedTab == "kt") Color(0xFF1E293B) else Color.Transparent)
                                .clickable { selectedTab = "kt" }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Compose.kt", color = if (selectedTab == "kt") Color.White else Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Copy Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF007AFF))
                            .clickable {
                                val textToCopy = if (selectedTab == "json") jsonPayload else composeCode
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Lucide.Copy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(11.dp))
                            Text("Copy", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Summary Pill Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E293B)).padding(8.dp)
                    ) {
                        Column {
                            Text("Target Collection", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                            Text(collectionTarget.take(16), color = Color(0xFF38B8D1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E293B)).padding(8.dp)
                    ) {
                        Column {
                            Text("Aspect Ratio", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                            Text(selectedRatioId, color = Color(0xFFF5882E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E293B)).padding(8.dp)
                    ) {
                        Column {
                            Text("Media Count", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                            Text("$slidesCount items", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Code Preformatted Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF030712))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (selectedTab == "json") jsonPayload else composeCode,
                        color = if (selectedTab == "json") Color(0xFF38BDF8) else Color(0xFFA7F3D0),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
