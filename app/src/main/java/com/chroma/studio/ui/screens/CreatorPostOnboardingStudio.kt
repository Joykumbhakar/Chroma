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
// DATA MODELS & ENUMS
// ============================================================================

enum class CreatorStudioMode {
    POST,  // Photo Carousel & Collage Post
    FLASH, // 9:16 Vertical Short Reel
    WORK   // Chroma Studio Saved Vector Work
}

enum class StudioStep(val stepNumber: Int, val title: String) {
    STEP_1_MEDIA(1, "Step 1 · Media & Edit"),
    STEP_2_ASPECT_COVER(2, "Step 2 · Aspect & Cover"),
    STEP_3_DETAILS(3, "Step 3 · Details & Meta"),
    STEP_4_UPLOAD(4, "Step 4 · Upload & Share")
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
    StudioRatioConfig("4:3", "4:3", "Classic (4:3)", 4f, 3f, Lucide.Tv, 32.dp, 24.dp),
    StudioRatioConfig("16:9", "16:9", "Landscape (16:9)", 16f, 9f, Lucide.RectangleHorizontal, 36.dp, 20.dp),
    StudioRatioConfig("5:4", "5:4", "Portrait (5:4)", 5f, 4f, Lucide.Frame, 28.dp, 23.dp),
    StudioRatioConfig("9:16", "9:16", "Story/Flash (9:16)", 9f, 16f, Lucide.Smartphone, 20.dp, 36.dp)
)

val ALGO_CATEGORIES = listOf(
    "Science",
    "Art",
    "Educational",
    "Inspiration",
    "Casually",
    "Jobs",
    "Others..."
)

val PHOTO_FILTER_PRESETS = listOf(
    "Original",
    "Vivid",
    "Warm",
    "Cool",
    "Onyx",
    "Pastel",
    "Cyberpunk",
    "Drama"
)

fun formatDurationTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    val tenths = (ms % 1000L) / 100L
    return "%02d:%02d.%d".format(mins, secs, tenths)
}

fun formatTimeMinSec(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "%02d:%02d".format(mins, secs)
}

fun buildStudioColorMatrix(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    filterName: String
): ColorMatrix {
    val cm = ColorMatrix()

    // 1. Saturation
    val filterSat = when (filterName) {
        "Onyx" -> 0f
        "Vivid" -> 1.5f * saturation
        "Pastel" -> 0.85f * saturation
        "Cyberpunk" -> 1.6f * saturation
        "Drama" -> 1.25f * saturation
        else -> saturation
    }
    cm.setToSaturation(filterSat)

    // 2. Contrast & Brightness
    val scale = when (filterName) {
        "Vivid" -> contrast * 1.15f
        "Onyx" -> contrast * 1.25f
        "Cyberpunk" -> contrast * 1.2f
        "Drama" -> contrast * 1.3f
        else -> contrast
    }
    val b = when (filterName) {
        "Pastel" -> brightness + 15f
        "Drama" -> brightness - 10f
        else -> brightness
    }
    val translate = (-0.5f * scale + 0.5f) * 255f + b

    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
    cm.timesAssign(contrastMatrix)

    // 3. Warmth / Temperature
    val warmVal = when (filterName) {
        "Warm" -> warmth + 35f
        "Cool" -> warmth - 35f
        "Cyberpunk" -> warmth - 25f
        else -> warmth
    }
    if (warmVal != 0f) {
        val warmthMatrix = ColorMatrix(
            floatArrayOf(
                1f + (warmVal * 0.002f), 0f, 0f, 0f, warmVal * 0.4f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f - (warmVal * 0.002f), 0f, -warmVal * 0.4f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.timesAssign(warmthMatrix)
    }

    return cm
}

@Composable
fun SlidePreviewContent(slide: StudioSlide, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    when {
        slide.work != null -> {
            val repo = remember { WorkRepository(context) }
            val layers = remember(slide.work.layersJson) { repo.deserializeLayers(slide.work.layersJson) }
            CanvasPreview(
                layers = layers,
                shape = slide.work.canvasShape,
                borderColor = Color.Transparent,
                modifier = modifier
            )
        }
        slide.imageUri != null -> {
            val cm = remember(slide.brightness, slide.contrast, slide.saturation, slide.warmth, slide.filterName) {
                buildStudioColorMatrix(slide.brightness, slide.contrast, slide.saturation, slide.warmth, slide.filterName)
            }
            AsyncImage(
                model = ImageRequest.Builder(context).data(slide.imageUri).crossfade(true).build(),
                contentDescription = null,
                colorFilter = ColorFilter.colorMatrix(cm),
                modifier = modifier.graphicsLayer(rotationZ = slide.rotationDegrees),
                contentScale = ContentScale.Crop
            )
        }
        slide.videoUri != null -> {
            var videoThumbnail by remember(slide.videoUri) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(slide.videoUri) {
                withContext(Dispatchers.IO) {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, slide.videoUri)
                        val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            retriever.getScaledFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 480, 640)
                        } else {
                            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        }
                        retriever.release()
                        if (bmp != null) videoThumbnail = bmp
                    } catch (_: Throwable) {}
                }
            }

            Box(modifier = modifier.background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
                if (videoThumbnail != null) {
                    Image(
                        bitmap = videoThumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Play,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp).padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ThirdsGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.dp.toPx()
        val gridColor = Color.White.copy(alpha = 0.65f)
        val shadowColor = Color.Black.copy(alpha = 0.35f)

        // Vertical lines at 1/3 and 2/3
        val colWidth = size.width / 3f
        for (i in 1..2) {
            val x = colWidth * i
            // Subtle shadow line
            drawLine(
                color = shadowColor,
                start = Offset(x + 1f, 0f),
                end = Offset(x + 1f, size.height),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth
            )
        }

        // Horizontal lines at 1/3 and 2/3
        val rowHeight = size.height / 3f
        for (i in 1..2) {
            val y = rowHeight * i
            drawLine(
                color = shadowColor,
                start = Offset(0f, y + 1f),
                end = Offset(size.width, y + 1f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth
            )
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.textMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFF5882E),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (colors.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1)
            )
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
                            selectedSoundtrackTitle = selectedSoundtrack?.title
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
                            onOpenMentionSearch = { showMentionModal = true },
                            onRemoveMention = { mentionedUsers.remove(it) },
                            onAudienceVisibilityChange = { audienceVisibility = it },
                            onToggleHideLikes = { hideLikesCount = it },
                            onToggleTurnOffComments = { turnOffComments = it },
                            onToggleTurnOffRemix = { turnOffRemix = it },
                            onToggleIncludeOriginalAudio = { includeOriginalAudio = it },
                            onToggleAllowDownloads = { allowDownloads = it },
                            onToggleHighQualityUpload = { highQualityUpload = it },
                            scheduleReleaseEnabled = scheduleReleaseEnabled,
                            scheduleDateTime = scheduleDateTime,
                            remixAttributionEnabled = remixAttributionEnabled,
                            onToggleScheduleRelease = { scheduleReleaseEnabled = it },
                            onScheduleDateTimeChange = { scheduleDateTime = it },
                            onToggleRemixAttribution = { remixAttributionEnabled = it },
                            onSaveDraft = { saveCurrentDraft(showToast = true) },
                            onPublishClick = { startUploadPipeline() }
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
                            onRetryUpload = { startUploadPipeline() }
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
// STICKY HEADER COMPONENT (Matches exact HTML reference layout)
// ============================================================================

// ============================================================================
// OSCELION DYNAMIC ISLAND (Matches HTML Reference Layout)
// ============================================================================

@Composable
fun OscellionDynamicIsland(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    title: String = "Oscellion Studio",
    subtitle: String = "Ready • UnifiedPostPayload synced",
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
            .padding(top = 4.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(islandWidth)
                .height(islandHeight)
                .clickable { onToggleExpand() },
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0F1117),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isExpanded) {
                    // Compact View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF5AC8FA)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("O", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Text("Oscellion", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dotPulse"
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34D399).copy(alpha = pulseAlpha))
                            )
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF18181B))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            )
                        }
                    }
                } else {
                    // Expanded View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF18181B))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Sparkles, contentDescription = null, tint = Color(0xFF5AC8FA), modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF064E3B))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(badgeText, color = Color(0xFF34D399), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Text(subtitle, color = Color(0xFFD1D5DB), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudioStickyHeader(
    studioMode: CreatorStudioMode,
    currentStep: StudioStep,
    draftsCount: Int,
    onBackClick: () -> Unit,
    onSwitchMode: (CreatorStudioMode) -> Unit,
    onOpenDrafts: () -> Unit,
    onNextStep: () -> Unit,
    onJumpToStep: (StudioStep) -> Unit
) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (colors.isDark) Color(0xFF141720) else Color.White)
            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Top Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Circular Back Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                    .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0), CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.ArrowLeft,
                    contentDescription = "Back",
                    tint = colors.textMain,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Center: 3-Mode Switcher Pill
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))
                    .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0), CircleShape)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash Tab
                val isFlash = studioMode == CreatorStudioMode.FLASH
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isFlash) (if (colors.isDark) Color(0xFF1E293B) else Color.White) else Color.Transparent)
                        .clickable { onSwitchMode(CreatorStudioMode.FLASH) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Zap,
                        contentDescription = null,
                        tint = if (isFlash) Color(0xFFF5882E) else colors.textMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        "Flash",
                        color = if (isFlash) colors.textMain else colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isFlash) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Photo Tab
                val isPhoto = studioMode == CreatorStudioMode.POST
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isPhoto) (if (colors.isDark) Color(0xFF1E293B) else Color.White) else Color.Transparent)
                        .clickable { onSwitchMode(CreatorStudioMode.POST) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Camera,
                        contentDescription = null,
                        tint = if (isPhoto) Color(0xFF1696B2) else colors.textMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        "Photo",
                        color = if (isPhoto) colors.textMain else colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isPhoto) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Work Tab
                val isWork = studioMode == CreatorStudioMode.WORK
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isWork) (if (colors.isDark) Color(0xFF1E293B) else Color.White) else Color.Transparent)
                        .clickable { onSwitchMode(CreatorStudioMode.WORK) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Layers,
                        contentDescription = null,
                        tint = if (isWork) Color(0xFF8B5CF6) else colors.textMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        "Work",
                        color = if (isWork) colors.textMain else colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isWork) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // Right: Saved Drafts Bookmark Icon + Circular Next Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Drafts Button with Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                        .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0), CircleShape)
                        .clickable { onOpenDrafts() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Bookmark,
                        contentDescription = "Drafts",
                        tint = colors.textMain,
                        modifier = Modifier.size(16.dp)
                    )
                    if (draftsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1696B2))
                                .border(1.5.dp, if (colors.isDark) Color(0xFF141720) else Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$draftsCount",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Next / Publish Circular Action Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFB923C), Color(0xFFF5882E))
                            )
                        )
                        .clickable { onNextStep() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentStep == StudioStep.STEP_3_DETAILS) Lucide.Send else Lucide.ArrowRight,
                        contentDescription = "Continue",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Step Progress Subtitle & Database Collection Pill
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = when (currentStep) {
                        StudioStep.STEP_1_MEDIA -> Lucide.SlidersHorizontal
                        StudioStep.STEP_2_ASPECT_COVER -> Lucide.Frame
                        StudioStep.STEP_3_DETAILS -> Lucide.PenTool
                        StudioStep.STEP_4_UPLOAD -> Lucide.CloudUpload
                    },
                    contentDescription = null,
                    tint = Color(0xFF1696B2),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = currentStep.title,
                    color = Color(0xFF1696B2),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Target Database Collection Pill
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))
                    .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0), CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Lucide.Database,
                    contentDescription = null,
                    tint = Color(0xFF1696B2),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = if (studioMode == CreatorStudioMode.FLASH) "flash_videos" else "community_posts",
                    color = colors.textMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // 4-Segment Interactive Step Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            StudioStep.values().forEach { step ->
                val isActive = step.stepNumber <= currentStep.stepNumber
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isActive) Color(0xFFF5882E)
                            else (if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0))
                        )
                        .clickable { onJumpToStep(step) }
                )
            }
        }
    }
}

// ============================================================================
// STEP 1: MEDIA INGESTION & PRE-TOOL EDITING STAGE
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
    onSelectWork: (ChromaWork) -> Unit
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (studioMode) {
            // ────────────────────────────────────────────────────────────────
            // 1A. FLASH VIDEO MODE
            // ────────────────────────────────────────────────────────────────
            CreatorStudioMode.FLASH -> {
                // Source Selector Card
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
                                    Text("MP4 · 9:16", color = colors.textMuted, fontSize = 10.sp)
                                }
                            }

                            // Record Camera Button
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
                                    Icon(Lucide.Camera, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(16.dp))
                                }
                                Column {
                                    Text("Record", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Max 30.0s", color = colors.textMuted, fontSize = 10.sp)
                                }
                            }

                            // Audio Button
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (colors.isDark) Color(0xFF0F172A) else Color(0xFF0F172A))
                                    .clickable { onOpenAudioMixer() },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Lucide.Music, contentDescription = null, tint = Color(0xFFFB923C), modifier = Modifier.size(16.dp))
                                    Text("Audio", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 9:16 Vertical Reel Player & Vertical Tool Rail
                item {
                    val activeVideoUri = slides.firstOrNull { it.videoUri != null }?.videoUri
                    var isPlaying by remember { mutableStateOf(true) }
                    var currentPositionMs by remember { mutableLongStateOf(flashTrimStartMs) }

                    val exoPlayer = remember(activeVideoUri) {
                        if (activeVideoUri != null) {
                            ExoPlayer.Builder(context).build().apply {
                                setMediaItem(MediaItem.fromUri(activeVideoUri))
                                repeatMode = Player.REPEAT_MODE_ALL
                                volume = if (flashIsMuted) 0f else 1f
                                playbackParameters = androidx.media3.common.PlaybackParameters(flashPlaybackSpeed)
                                prepare()
                                playWhenReady = true
                            }
                        } else null
                    }

                    DisposableEffect(exoPlayer) {
                        onDispose { exoPlayer?.release() }
                    }

                    LaunchedEffect(flashPlaybackSpeed, flashIsMuted) {
                        exoPlayer?.let { player ->
                            player.playbackParameters = androidx.media3.common.PlaybackParameters(flashPlaybackSpeed)
                            player.volume = if (flashIsMuted) 0f else 1f
                        }
                    }

                    // Loop within trimmed window
                    LaunchedEffect(exoPlayer, flashTrimStartMs, flashTrimEndMs) {
                        while (true) {
                            delay(100)
                            exoPlayer?.let { player ->
                                currentPositionMs = player.currentPosition
                                if (player.currentPosition >= flashTrimEndMs || player.currentPosition < flashTrimStartMs) {
                                    player.seekTo(flashTrimStartMs)
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 9:16 Video Player Box
                            Box(
                                modifier = Modifier
                                    .width(200.dp)
                                    .aspectRatio(9f / 16f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                                    .clickable {
                                        exoPlayer?.let {
                                            if (it.isPlaying) {
                                                it.pause()
                                                isPlaying = false
                                            } else {
                                                it.play()
                                                isPlaying = true
                                            }
                                        }
                                    }
                            ) {
                                if (exoPlayer != null) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                player = exoPlayer
                                                useController = false
                                                layoutParams = FrameLayout.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize().graphicsLayer(
                                            scaleX = if (flashIsFlipped) -1f else 1f
                                        )
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Lucide.Video, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                                            Spacer(Modifier.height(8.dp))
                                            Text("Tap Gallery to select MP4", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                        }
                                    }
                                }

                                // Top Floating Pills
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Lucide.Smartphone, contentDescription = null, tint = Color(0xFFFB923C), modifier = Modifier.size(10.dp))
                                        Text("9:16", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("${flashPlaybackSpeed}x", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Center Play Button Overlay
                                if (!isPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.85f))
                                            .align(Alignment.Center),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Play, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp).padding(start = 2.dp))
                                    }
                                }

                                // Bottom Audio Marquee & Timecode
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
                                            text = "Flash 9:16",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            // Vertical Icon Tool Rail
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
                                        contentDescription = "Flip",
                                        tint = if (flashIsFlipped) Color(0xFFF5882E) else colors.textMain,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Auto-snap 15s
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (colors.isDark) Color(0xFF141720) else Color.White)
                                        .clickable { onSnapFifteenSec() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.Sparkles,
                                        contentDescription = "15s Snap",
                                        tint = Color(0xFFF5882E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Interactive Timeline Scrubber & Trim Controls
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF8FAFC))
                                .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val durationSec = ((flashTrimEndMs - flashTrimStartMs) / 1000f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Lucide.Scissors, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(14.dp))
                                    Text("Timeline Trim", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "${formatTimeMinSec(flashTrimStartMs)} - ${formatTimeMinSec(flashTrimEndMs)} (%.1fs)".format(durationSec),
                                    color = Color(0xFF1696B2),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Start & End Trim Sliders
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Trim In (Start)", color = colors.textMuted, fontSize = 10.sp)
                                    Text(formatDurationTime(flashTrimStartMs), color = colors.textMain, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                Slider(
                                    value = flashTrimStartMs.toFloat(),
                                    onValueChange = { newStart ->
                                        val s = newStart.toLong().coerceIn(0L, (flashTrimEndMs - 1000L).coerceAtLeast(0L))
                                        onUpdateTrim(s, flashTrimEndMs)
                                    },
                                    valueRange = 0f..flashVideoDurationMs.toFloat().coerceAtLeast(1000f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFF5882E),
                                        activeTrackColor = Color(0xFFF5882E)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Trim Out (End)", color = colors.textMuted, fontSize = 10.sp)
                                    Text(formatDurationTime(flashTrimEndMs), color = colors.textMain, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                Slider(
                                    value = flashTrimEndMs.toFloat(),
                                    onValueChange = { newEnd ->
                                        val e = newEnd.toLong().coerceIn((flashTrimStartMs + 1000L), flashVideoDurationMs)
                                        onUpdateTrim(flashTrimStartMs, e)
                                    },
                                    valueRange = 0f..flashVideoDurationMs.toFloat().coerceAtLeast(1000f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF1696B2),
                                        activeTrackColor = Color(0xFF1696B2)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ────────────────────────────────────────────────────────────────
            // 1B. PHOTO POST MODE
            // ────────────────────────────────────────────────────────────────
            CreatorStudioMode.POST -> {
                // Source & Carousel Bar
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gallery Upload
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
                                Text("Gallery", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Camera Capture
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
                                Text("Camera", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Carousel Slides Count Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1696B2))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Lucide.Layers, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "${slides.size.coerceAtLeast(1)}/10",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Live Photo Canvas & Mini Strip
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Main Photo Preview Stage (4:3 aspect)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0F172A))
                        ) {
                            if (activeSlide?.imageUri != null) {
                                val cm = if (showOriginal) ColorMatrix() else remember(
                                    activeSlide.brightness, activeSlide.contrast,
                                    activeSlide.saturation, activeSlide.warmth, activeSlide.filterName
                                ) {
                                    buildStudioColorMatrix(
                                        activeSlide.brightness, activeSlide.contrast,
                                        activeSlide.saturation, activeSlide.warmth, activeSlide.filterName
                                    )
                                }

                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(activeSlide.imageUri).crossfade(true).build(),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.colorMatrix(cm),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(rotationZ = activeSlide.rotationDegrees),
                                    contentScale = ContentScale.Crop
                                )

                                // Vignette shadow overlay
                                if (activeSlide.vignette > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = activeSlide.vignette / 100f * 0.75f))
                                                )
                                            )
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Lucide.Images, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text("Tap Gallery to select photos", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                }
                            }

                            // Top Floating Controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Slide Index Badge
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${(currentSlideIndex + 1).coerceAtMost(slides.size.coerceAtLeast(1))} / ${slides.size.coerceAtLeast(1)}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Icon Action Tools
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Compare Eye Button
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable { showOriginal = !showOriginal },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Eye, contentDescription = "Original", tint = Color.White, modifier = Modifier.size(15.dp))
                                    }

                                    // Rotate 90
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable { onRotatePhoto() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.RotateCw, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(15.dp))
                                    }

                                    // Flip Horizontal
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable { onFlipPhoto() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.FlipHorizontal, contentDescription = "Flip", tint = Color.White, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }

                            // Mini Carousel Thumbnails Row inside preview bottom
                            if (slides.size > 1) {
                                LazyRow(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    itemsIndexed(slides) { idx, s ->
                                        val isSel = idx == currentSlideIndex
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(
                                                    if (isSel) 2.dp else 1.dp,
                                                    if (isSel) Color(0xFFF5882E) else Color.White.copy(alpha = 0.4f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { onSlideChange(idx) }
                                        ) {
                                            SlidePreviewContent(slide = s, modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }

                        // Filter Presets Strip
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Lucide.Sparkles, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(14.dp))
                                    Text("Color Presets", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "Reset",
                                    color = Color(0xFFF5882E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        onSelectFilterPreset("Original")
                                        onUpdatePhotoAdjustments(0f, 1f, 1f, 0f, 0f, 0f)
                                    }
                                )
                            }

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(PHOTO_FILTER_PRESETS) { preset ->
                                    val isSel = (activeSlide?.filterName ?: "Original") == preset
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSel) Color(0xFF1696B2)
                                                else (if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF1F5F9))
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) Color(0xFF1696B2)
                                                else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onSelectFilterPreset(preset) }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = preset,
                                            color = if (isSel) Color.White else colors.textMain,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // 6 Fine-Tuning Sliders
                        val curBrightness = activeSlide?.brightness ?: 0f
                        val curContrast = activeSlide?.contrast ?: 1f
                        val curSaturation = activeSlide?.saturation ?: 1f
                        val curWarmth = activeSlide?.warmth ?: 0f
                        val curVignette = activeSlide?.vignette ?: 0f
                        val curSharpness = activeSlide?.sharpness ?: 0f

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF8FAFC))
                                .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Brightness
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Brightness", color = colors.textMuted, fontSize = 11.sp)
                                    Text("${curBrightness.toInt()}", color = colors.textMain, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Slider(
                                    value = curBrightness,
                                    onValueChange = { b -> onUpdatePhotoAdjustments(b, curContrast, curSaturation, curWarmth, curVignette, curSharpness) },
                                    valueRange = -100f..100f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFF5882E), activeTrackColor = Color(0xFFF5882E))
                                )
                            }

                            // Contrast
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Contrast", color = colors.textMuted, fontSize = 11.sp)
                                    Text("%.1fx".format(curContrast), color = colors.textMain, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Slider(
                                    value = curContrast,
                                    onValueChange = { c -> onUpdatePhotoAdjustments(curBrightness, c, curSaturation, curWarmth, curVignette, curSharpness) },
                                    valueRange = 0.5f..2.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF1696B2), activeTrackColor = Color(0xFF1696B2))
                                )
                            }

                            // Saturation
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Saturation", color = colors.textMuted, fontSize = 11.sp)
                                    Text("%.1fx".format(curSaturation), color = colors.textMain, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Slider(
                                    value = curSaturation,
                                    onValueChange = { s -> onUpdatePhotoAdjustments(curBrightness, curContrast, s, curWarmth, curVignette, curSharpness) },
                                    valueRange = 0f..2.5f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFB923C), activeTrackColor = Color(0xFFFB923C))
                                )
                            }

                            // Warmth
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Warmth / Tint", color = colors.textMuted, fontSize = 11.sp)
                                    Text("${curWarmth.toInt()}", color = colors.textMain, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Slider(
                                    value = curWarmth,
                                    onValueChange = { w -> onUpdatePhotoAdjustments(curBrightness, curContrast, curSaturation, w, curVignette, curSharpness) },
                                    valueRange = -100f..100f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFF5882E), activeTrackColor = Color(0xFFF5882E))
                                )
                            }

                            // Vignette
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Vignette", color = colors.textMuted, fontSize = 11.sp)
                                    Text("${curVignette.toInt()}%", color = colors.textMain, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Slider(
                                    value = curVignette,
                                    onValueChange = { v -> onUpdatePhotoAdjustments(curBrightness, curContrast, curSaturation, curWarmth, v, curSharpness) },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF1696B2), activeTrackColor = Color(0xFF1696B2))
                                )
                            }
                        }
                    }
                }
            }

            // ────────────────────────────────────────────────────────────────
            // 1C. CHROMA WORK MODE
            // ────────────────────────────────────────────────────────────────
            CreatorStudioMode.WORK -> {
                item {
                    val activeWork = slides.getOrNull(currentSlideIndex)?.work
                    var allWorks by remember { mutableStateOf<List<ChromaWork>>(emptyList()) }

                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            val list = workRepository.loadActiveWorks()
                            withContext(Dispatchers.Main) {
                                allWorks = list
                                if (activeWork == null && list.isNotEmpty()) {
                                    onSelectWork(list.first())
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                            .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header with Live Canvas controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Lucide.Layers, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp))
                                Text("Chroma Work Canvas", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (workShowGrain) Color(0xFF8B5CF6) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)))
                                        .clickable { onToggleWorkGrain() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.Sparkles, contentDescription = null, tint = if (workShowGrain) Color.White else colors.textMain, modifier = Modifier.size(15.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                                        .clickable { onRotateWorkMesh() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.Compass, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(15.dp))
                                }
                            }
                        }

                        // Live Canvas Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0F172A))
                                .graphicsLayer(rotationZ = workMeshAngle)
                        ) {
                            if (activeWork != null) {
                                val layers = remember(activeWork.layersJson) {
                                    workRepository.deserializeLayers(activeWork.layersJson)
                                }
                                CanvasPreview(
                                    layers = layers,
                                    shape = activeWork.canvasShape,
                                    borderColor = Color.Transparent,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Top Info Badges
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("activeWorks", color = Color(0xFFBAE6FD), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.85f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("1:1 Snapshot", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Room SQLite Saved Works Project Chooser
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Search Input
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF1F5F9))
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Lucide.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(8.dp))
                                    BasicTextField(
                                        value = workSearchQuery,
                                        onValueChange = onWorkSearchChange,
                                        modifier = Modifier.weight(1f),
                                        textStyle = TextStyle(color = colors.textMain, fontSize = 12.sp),
                                        decorationBox = { inner ->
                                            if (workSearchQuery.isEmpty()) {
                                                Text("Search saved works...", color = colors.textMuted, fontSize = 12.sp)
                                            }
                                            inner()
                                        }
                                    )
                                }

                                // Series Pack Toggle
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (workIsSeriesMode) Color(0xFF8B5CF6) else (if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF1F5F9)))
                                        .clickable { onToggleWorkSeries() }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (workIsSeriesMode) "Series Pack" else "Single",
                                        color = if (workIsSeriesMode) Color.White else colors.textMain,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Works Grid
                            val filtered = allWorks.filter { it.name.contains(workSearchQuery, ignoreCase = true) }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filtered) { work ->
                                    val isSelected = slides.any { it.work?.id == work.id }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) (if (colors.isDark) Color(0xFF2E1A47) else Color(0xFFF3E8FF)) else (if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF8FAFC)))
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF8B5CF6) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable { onSelectWork(work) }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF0F172A))
                                        ) {
                                            val layers = remember(work.layersJson) { workRepository.deserializeLayers(work.layersJson) }
                                            CanvasPreview(layers = layers, shape = work.canvasShape, borderColor = Color.Transparent, modifier = Modifier.fillMaxSize())
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(work.name, color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(work.canvasShape.replaceFirstChar { it.uppercase() } + " Canvas", color = colors.textMuted, fontSize = 9.sp)
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


// ============================================================================
// STEP 2: ASPECT RATIO, CAROUSEL REORDERING & 1:1 PROFILE COVER STAGE
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
    onSelectRatio: (String) -> Unit,
    onToggleThirdsGrid: () -> Unit,
    onToggleWatermark: () -> Unit,
    onCropOffsetChange: (Float, Float) -> Unit,
    onCenterCrop: () -> Unit,
    onCoverPickerModeChange: (String) -> Unit,
    onScrubCoverFrame: (Long) -> Unit,
    onPickCustomCover: () -> Unit,
    onSelectSlide: (Int) -> Unit,
    onRemoveSlide: (Int) -> Unit
) {
    val colors = LocalOscellionColors.current
    val activeSlide = slides.getOrNull(currentSlideIndex) ?: slides.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 2A. Aspect Ratio Selector & Compositional Overlays Card
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Lucide.Frame, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(15.dp))
                        Text("Aspect Ratio & Grid", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Watermark Stamp Toggle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (showWatermark) Color(0xFFF5882E) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)))
                                .clickable { onToggleWatermark() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Award, contentDescription = "Watermark", tint = if (showWatermark) Color.White else colors.textMain, modifier = Modifier.size(15.dp))
                        }

                        // 3x3 Rule-of-Thirds Grid Toggle
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (showThirdsGrid) Color(0xFF1696B2) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)))
                                .clickable { onToggleThirdsGrid() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Lucide.Grid3x3, contentDescription = null, tint = if (showThirdsGrid) Color.White else colors.textMain, modifier = Modifier.size(13.dp))
                                Text("3x3", color = if (showThirdsGrid) Color.White else colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 5 Aspect Ratio Chips
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
                                    else (if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF1F5F9))
                                )
                                .border(
                                    1.dp,
                                    if (isSel) Color(0xFF1696B2)
                                    else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectRatio(config.id) }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = config.icon,
                                contentDescription = null,
                                tint = if (isSel) Color.White else colors.textMain,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = config.label,
                                color = if (isSel) Color.White else colors.textMain,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 2B. Direct-Draggable 1:1 Profile Crop Box Stage
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Lucide.Crop, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(16.dp))
                        Text("Drag 1:1 Profile Cover", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFEBF7FA))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "X:${cropOffsetX.toInt()}% Y:${cropOffsetY.toInt()}%",
                                color = Color(0xFF1696B2),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Center Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                                .clickable { onCenterCrop() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Focus, contentDescription = "Center", tint = colors.textMain, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Interactive Framing Stage
                var stageSize by remember { mutableStateOf(IntSize.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                        .onSizeChanged { stageSize = it },
                    contentAlignment = Alignment.Center
                ) {
                    if (activeSlide != null) {
                        SlidePreviewContent(
                            slide = activeSlide,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Watermark badge
                    if (showWatermark) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Lucide.Sparkles, contentDescription = null, tint = Color(0xFFFB923C), modifier = Modifier.size(10.dp))
                                Text("Chroma Studio", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 3x3 Rule of Thirds
                    if (showThirdsGrid) {
                        ThirdsGridOverlay(modifier = Modifier.fillMaxSize())
                    }

                    // Direct-Draggable 1:1 Square Crop Box
                    val boxSizeDp = 150.dp
                    val density = LocalDensity.current
                    val boxSizePx = with(density) { boxSizeDp.toPx() }

                    val maxX = (stageSize.width - boxSizePx).coerceAtLeast(0f)
                    val maxY = (stageSize.height - boxSizePx).coerceAtLeast(0f)

                    val offsetX = (cropOffsetX / 100f) * maxX
                    val offsetY = (cropOffsetY / 100f) * maxY

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .size(boxSizeDp)
                            .border(2.dp, Color(0xFFF5882E), RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5882E).copy(alpha = 0.15f))
                            .pointerInput(stageSize) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (maxX > 0f && maxY > 0f) {
                                        val newX = (offsetX + dragAmount.x).coerceIn(0f, maxX)
                                        val newY = (offsetY + dragAmount.y).coerceIn(0f, maxY)
                                        onCropOffsetChange(
                                            (newX / maxX * 100f).coerceIn(0f, 100f),
                                            (newY / maxY * 100f).coerceIn(0f, 100f)
                                        )
                                    }
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF5882E))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Lucide.Move, contentDescription = null, tint = Color.White, modifier = Modifier.size(9.dp))
                                Text("1:1", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Precision Sliders & Live 1:1 Profile Feed Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Slider X
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Crop X Alignment", color = colors.textMuted, fontSize = 10.sp)
                            Text("${cropOffsetX.toInt()}%", color = colors.textMain, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = cropOffsetX,
                            onValueChange = { onCropOffsetChange(it, cropOffsetY) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFF5882E), activeTrackColor = Color(0xFFF5882E))
                        )

                        // Slider Y
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Crop Y Alignment", color = colors.textMuted, fontSize = 10.sp)
                            Text("${cropOffsetY.toInt()}%", color = colors.textMain, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = cropOffsetY,
                            onValueChange = { onCropOffsetChange(cropOffsetX, it) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF1696B2), activeTrackColor = Color(0xFF1696B2))
                        )
                    }

                    // Live 1:1 Profile Mini Card
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("1:1 Grid", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, Color(0xFFF5882E), RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                        ) {
                            if (activeSlide != null) {
                                SlidePreviewContent(slide = activeSlide, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }

                // Flash Video Specific Cover Frame Scrubber
                if (studioMode == CreatorStudioMode.FLASH) {
                    Spacer(Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF8FAFC))
                            .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Lucide.Video, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(15.dp))
                                Text("Cover Source", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (coverPickerMode == "frame") (if (colors.isDark) Color(0xFF161922) else Color.White) else Color.Transparent)
                                        .clickable { onCoverPickerModeChange("frame") }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("Frame", color = if (coverPickerMode == "frame") colors.textMain else colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (coverPickerMode == "custom") (if (colors.isDark) Color(0xFF161922) else Color.White) else Color.Transparent)
                                        .clickable { onCoverPickerModeChange("custom") }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("Custom", color = if (coverPickerMode == "custom") colors.textMain else colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (coverPickerMode == "frame") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Scrub video poster frame", color = colors.textMuted, fontSize = 10.sp)
                                    Text(formatDurationTime(selectedCoverFrameMs), color = Color(0xFF1696B2), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = selectedCoverFrameMs.toFloat(),
                                    onValueChange = { onScrubCoverFrame(it.toLong()) },
                                    valueRange = 0f..flashVideoDurationMs.toFloat().coerceAtLeast(1000f),
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF1696B2), activeTrackColor = Color(0xFF1696B2))
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (colors.isDark) Color(0xFF1A2634) else Color(0xFFEBF7FA))
                                    .border(1.dp, Color(0xFF1696B2).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { onPickCustomCover() }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Lucide.Upload, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(16.dp))
                                    Text("Upload High-Res Cover Image", color = Color(0xFF1696B2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Icon(Lucide.ChevronRight, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // 2C. Multi-Slide Carousel Strip (Photo & Work)
        if (slides.size > 1) {
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
                            Icon(Lucide.Layers, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(15.dp))
                            Text("Carousel Slides", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Text("${slides.size} slides", color = Color(0xFF1696B2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(slides) { idx, s ->
                            val isSel = idx == currentSlideIndex
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(96.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        if (isSel) 2.dp else 1.dp,
                                        if (isSel) Color(0xFFF5882E) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectSlide(idx) }
                            ) {
                                SlidePreviewContent(slide = s, modifier = Modifier.fillMaxSize())

                                // Slide order badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text("${idx + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }

                                // Delete icon
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .clickable { onRemoveSlide(idx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.X, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(11.dp))
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
// STEP 3: POST DETAILS, METADATA & ADVANCED PRIVACY SETTINGS STAGE
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
    onOpenMentionSearch: () -> Unit,
    onRemoveMention: (UserProfile) -> Unit,
    onAudienceVisibilityChange: (String) -> Unit,
    onToggleHideLikes: (Boolean) -> Unit,
    onToggleTurnOffComments: (Boolean) -> Unit,
    onToggleTurnOffRemix: (Boolean) -> Unit,
    onToggleIncludeOriginalAudio: (Boolean) -> Unit,
    onToggleAllowDownloads: (Boolean) -> Unit,
    onToggleHighQualityUpload: (Boolean) -> Unit,
    onSaveDraft: () -> Unit,
    onPublishClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    var showAltTextBox by remember { mutableStateOf(false) }
    var hashtagInput by remember { mutableStateOf("") }
    val activeSlide = slides.getOrNull(currentSlideIndex) ?: slides.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 3A. Small Hero Summary Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(20.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mini preview box
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                ) {
                    if (activeSlide != null) {
                        SlidePreviewContent(slide = activeSlide, modifier = Modifier.fillMaxSize())
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF1696B2).copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (studioMode) {
                                    CreatorStudioMode.FLASH -> "FLASH · $selectedRatioId"
                                    CreatorStudioMode.WORK -> "WORK · $selectedRatioId"
                                    CreatorStudioMode.POST -> "PHOTO · $selectedRatioId"
                                },
                                color = Color(0xFF1696B2),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (selectedCategoryTag != null) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFF5882E).copy(alpha = 0.15f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(selectedCategoryTag, color = Color(0xFFF5882E), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = postTitle.ifBlank { "Untitled Creation" },
                        color = colors.textMain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 3B. Headline & Story (Title & Description)
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
                        Icon(Lucide.PenTool, contentDescription = null, tint = Color(0xFF1696B2), modifier = Modifier.size(15.dp))
                        Text("Headline & Story", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Text("${postTitle.length} / 100", color = colors.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                // Title Input
                BasicTextField(
                    value = postTitle,
                    onValueChange = onTitleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF8FAFC))
                        .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    textStyle = TextStyle(color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    decorationBox = { inner ->
                        if (postTitle.isEmpty()) {
                            Text("Give your creation a title...", color = colors.textMuted, fontSize = 13.sp)
                        }
                        inner()
                    }
                )

                // Caption Textarea
                BasicTextField(
                    value = postDescription,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF8FAFC))
                        .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    textStyle = TextStyle(color = colors.textMain, fontSize = 12.sp),
                    decorationBox = { inner ->
                        if (postDescription.isEmpty()) {
                            Text("Write a caption, tell the story behind this work...", color = colors.textMuted, fontSize = 12.sp)
                        }
                        inner()
                    }
                )

                // Caption Action Toolbar (#, @, Bold, Alt-text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Insert #
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1696B2).copy(alpha = 0.12f))
                                .clickable { onAddHashtag("chroma") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Hash, contentDescription = "#", tint = Color(0xFF1696B2), modifier = Modifier.size(14.dp))
                        }

                        // Insert @
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5882E).copy(alpha = 0.12f))
                                .clickable { onOpenMentionSearch() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.AtSign, contentDescription = "@", tint = Color(0xFFF5882E), modifier = Modifier.size(14.dp))
                        }

                        // Bold
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                                .clickable { onDescriptionChange("$postDescription **bold**") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Bold, contentDescription = "Bold", tint = colors.textMain, modifier = Modifier.size(14.dp))
                        }

                        // Alt text button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9))
                                .clickable { showAltTextBox = !showAltTextBox }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Alt", color = colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("${postDescription.length} / 1,500", color = colors.textMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                // Expandable Alt text field
                if (showAltTextBox) {
                    BasicTextField(
                        value = altTextString,
                        onValueChange = onAltTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF1F5F9))
                            .padding(8.dp),
                        textStyle = TextStyle(color = colors.textMain, fontSize = 11.sp),
                        decorationBox = { inner ->
                            if (altTextString.isEmpty()) {
                                Text("Describe image for visually impaired users...", color = colors.textMuted, fontSize = 11.sp)
                            }
                            inner()
                        }
                    )
                }
            }
        }

        // 3C. Categorization & Location
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ALGO_CATEGORIES.forEach { cat ->
                            val isSel = selectedCategoryTag == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSel) Color(0xFF1696B2)
                                        else (if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF1F5F9))
                                    )
                                    .clickable { onCategorySelect(cat) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSel) Color.White else colors.textMain,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (selectedCategoryTag == "Others...") {
                        BasicTextField(
                            value = customCategoryTag,
                            onValueChange = onCustomCategoryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF1F5F9))
                                .padding(8.dp),
                            textStyle = TextStyle(color = colors.textMain, fontSize = 11.sp),
                            decorationBox = { inner ->
                                if (customCategoryTag.isEmpty()) {
                                    Text("Enter custom category tag...", color = colors.textMuted, fontSize = 11.sp)
                                }
                                inner()
                            }
                        )
                    }
                }

                // Location Tag Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Location", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF8FAFC))
                            .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Lucide.MapPin, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = locationString,
                            onValueChange = onLocationChange,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = colors.textMain, fontSize = 12.sp),
                            decorationBox = { inner ->
                                if (locationString.isEmpty()) {
                                    Text("Add location...", color = colors.textMuted, fontSize = 12.sp)
                                }
                                inner()
                            }
                        )
                    }
                }
            }
        }

        // 3D. Hashtags & Mentions
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
                // Hashtags
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Hashtags", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = hashtagInput,
                            onValueChange = { hashtagInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF1F5F9))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            textStyle = TextStyle(color = colors.textMain, fontSize = 12.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (hashtagInput.isNotBlank()) {
                                    onAddHashtag(hashtagInput)
                                    hashtagInput = ""
                                }
                            }),
                            decorationBox = { inner ->
                                if (hashtagInput.isEmpty()) {
                                    Text("Add tag (e.g. art)...", color = colors.textMuted, fontSize = 12.sp)
                                }
                                inner()
                            }
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1696B2))
                                .clickable {
                                    if (hashtagInput.isNotBlank()) {
                                        onAddHashtag(hashtagInput)
                                        hashtagInput = ""
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Add", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (hashtags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            hashtags.forEach { tag ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1696B2).copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("#$tag", color = Color(0xFF1696B2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Icon(
                                        imageVector = Lucide.X,
                                        contentDescription = "Remove",
                                        tint = Color(0xFF1696B2),
                                        modifier = Modifier.size(11.dp).clickable { onRemoveHashtag(tag) }
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
                        Text("Mentions", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "+ Mention Creator",
                            color = Color(0xFFF5882E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onOpenMentionSearch() }
                        )
                    }

                    if (mentionedUsers.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            mentionedUsers.forEach { u ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF5882E).copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("@${u.username}", color = Color(0xFFF5882E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Icon(
                                        imageVector = Lucide.X,
                                        contentDescription = "Remove",
                                        tint = Color(0xFFF5882E),
                                        modifier = Modifier.size(11.dp).clickable { onRemoveMention(u) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3E. Audience Visibility
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
                Text("Audience Visibility", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val audOptions = listOf(
                        "public" to "Public",
                        "followers" to "Followers",
                        "friends" to "Close Friends",
                        "private" to "Private"
                    )

                    audOptions.forEach { (id, label) ->
                        val isSel = audienceVisibility == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSel) Color(0xFF1696B2)
                                    else (if (colors.isDark) Color(0xFF1A1D27) else Color(0xFFF1F5F9))
                                )
                                .clickable { onAudienceVisibilityChange(id) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color.White else colors.textMain,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 3F. Advanced Settings iOS Switches
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Advanced Settings", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                SettingToggleRow("Hide Likes & Views", "Only you will see total counts", hideLikesCount, onToggleHideLikes)
                SettingToggleRow("Turn Off Comments", "Disable responses on this creation", turnOffComments, onToggleTurnOffComments)
                SettingToggleRow("Disable Remix / Reuse", "Prevent re-mixing and duets", turnOffRemix, onToggleTurnOffRemix)
                SettingToggleRow("Include Original Audio", "Make soundtrack reusable by community", includeOriginalAudio, onToggleIncludeOriginalAudio)
                SettingToggleRow("Allow Downloads", "Let other users save video", allowDownloads, onToggleAllowDownloads)
                SettingToggleRow("High Quality Upload", "Upload in full uncompressed resolution", highQualityUpload, onToggleHighQualityUpload)

                // Schedule Post Release
                SettingToggleRow(
                    title = "Schedule Release",
                    subtitle = "Auto-publish at a set time",
                    checked = scheduleReleaseEnabled,
                    onCheckedChange = onToggleScheduleRelease
                )

                if (scheduleReleaseEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isDark) Color(0xFF1E2230) else Color(0xFFF1F5F9))
                            .clickable {
                                val nextTime = when (scheduleDateTime) {
                                    "Tomorrow, 10:00 AM" -> "Tomorrow, 06:00 PM"
                                    "Tomorrow, 06:00 PM" -> "Saturday, 12:00 PM"
                                    else -> "Tomorrow, 10:00 AM"
                                }
                                onScheduleDateTimeChange(nextTime)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Lucide.Calendar, contentDescription = null, tint = Color(0xFFF5882E), modifier = Modifier.size(14.dp))
                            Text(scheduleDateTime, color = colors.textMain, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        Text("Tap to change", color = Color(0xFF1696B2), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Remix Attribution
                SettingToggleRow(
                    title = "Remix Attribution",
                    subtitle = "Link source creator in payload",
                    checked = remixAttributionEnabled,
                    onCheckedChange = onToggleRemixAttribution
                )
            }
        }

        // 3G. Action Footer
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save Draft Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0xFF161922) else Color.White)
                        .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1), CircleShape)
                        .clickable { onSaveDraft() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Bookmark, contentDescription = "Save Draft", tint = Color(0xFF1696B2), modifier = Modifier.size(20.dp))
                }

                // Publish Creation Primary Button
                PrimaryGradientButton(
                    onClick = onPublishClick,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Lucide.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Publish Creation", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
    onRetryUpload: () -> Unit
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
                                    Text(audienceVisibility.capitalize(Locale.ROOT), color = Color(0xFF1696B2), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
