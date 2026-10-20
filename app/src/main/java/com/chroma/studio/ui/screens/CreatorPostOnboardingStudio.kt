@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.chroma.studio.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.compose.ui.graphics.asImageBitmap
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.widget.Toast
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.DraftPostRepository
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.ChromaWork
import com.chroma.studio.model.CreatorPostDraft
import com.chroma.studio.model.FlashMediaType
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.FlashViewModel
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong


fun formatDurationTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

// ============================================================================
// DATA MODELS & CONSTANTS
// ============================================================================

enum class CreatorStudioMode {
    POST,  // Image Post with Google Photos-style editor
    FLASH, // Video with video trimmer
    WORK   // Saved vector/gradient creations & drafts
}

enum class StudioStep {
    STEP_0_PRE_TOOL,
    STEP_1_PREVIEW,
    STEP_2_DETAILS,
    STEP_3_UPLOADING
}

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
    StudioRatioConfig("16:9", "16:9", "Landscape (16:9)", 16f, 9f, Lucide.RectangleHorizontal, 36.dp, 20.dp),
    StudioRatioConfig("9:16", "9:16", "Vertical (9:16)", 9f, 16f, Lucide.Smartphone, 20.dp, 36.dp),
    StudioRatioConfig("4:3", "4:3", "Standard (4:3)", 4f, 3f, Lucide.Tv, 32.dp, 24.dp),
    StudioRatioConfig("5:4", "5:4", "Portrait (5:4)", 5f, 4f, Lucide.Frame, 28.dp, 23.dp),
    StudioRatioConfig("1:1", "1:1", "Square (1:1)", 1f, 1f, Lucide.Square, 28.dp, 28.dp)
)

val ALGO_CATEGORIES = listOf(
    "Science",
    "Art",
    "Educational",
    "Casually",
    "Job/Opportunities",
    "Inspiration",
    "Others..."
)

// Helper to build ColorMatrix from adjust sliders and filter
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
        "Enhance" -> 1.2f * saturation
        else -> saturation
    }
    cm.setToSaturation(filterSat)

    // 2. Contrast & Brightness
    val scale = when (filterName) {
        "Enhance" -> contrast * 1.15f
        "Onyx" -> contrast * 1.2f
        else -> contrast
    }
    val b = when (filterName) {
        "Enhance" -> brightness + 10f
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

// ============================================================================
// MAIN CREATOR POST ONBOARDING STUDIO SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorPostOnboardingStudioScreen(
    initialMode: CreatorStudioMode = CreatorStudioMode.POST,
    initialDraftId: String? = null,
    initialImportedUris: List<Uri> = emptyList(),
    workRepository: WorkRepository,
    discoverRepository: DiscoverRepository,
    flashViewModel: FlashViewModel? = null,
    onNavigateBack: () -> Unit,
    onPostSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val draftRepo = remember { com.chroma.studio.data.DraftPostRepository(context) }
    var currentDraftId by remember { mutableStateOf(initialDraftId) }

    var studioMode by remember { mutableStateOf(initialMode) }
    var currentStep by remember { mutableStateOf(StudioStep.STEP_0_PRE_TOOL) }

    // Multi vs Single mode toggle
    var isMultipleMode by remember { mutableStateOf(false) }

    // Slides state
    val slides = remember { mutableStateListOf<StudioSlide>() }
    var currentSlideIndex by remember { mutableIntStateOf(0) }

    // Ratio state (Default 4:3 as in HTML, or 9:16 for Flash)
    var selectedRatioId by remember {
        mutableStateOf(if (initialMode == CreatorStudioMode.FLASH) "9:16" else "4:3")
    }
    var showThirdsGridBriefly by remember { mutableStateOf(false) }

    // Active Tool Tab in Step 1 (0: Ratio, 1: Thumbnail Preview)
    var activeToolTab by remember { mutableIntStateOf(0) }

    // Profile Thumbnail Preview Modal State
    var showThumbModal by remember { mutableStateOf(false) }
    var thumbCropOffset by remember { mutableFloatStateOf(50f) } // 0 to 100%
    var customThumbUri by remember { mutableStateOf<Uri?>(null) }
    var customThumbSlideIndex by remember { mutableIntStateOf(0) }
    var selectedCoverFrameMs by remember { mutableLongStateOf(0L) }

    // Video Post Specific Controls
    var audienceVisibility by remember { mutableStateOf("public") } // "public", "followers", "private"
    var includeOriginalAudio by remember { mutableStateOf(true) }
    var allowDownloads by remember { mutableStateOf(true) }
    var highQualityUpload by remember { mutableStateOf(true) }

    // Step 2 Form States
    var postTitle by remember { mutableStateOf("") }
    var postDescription by remember { mutableStateOf("") }
    var selectedCategoryTag by remember { mutableStateOf<String?>(null) }
    var customCategoryTag by remember { mutableStateOf("") }
    val hashtags = remember { mutableStateListOf<String>() }
    var hashtagInput by remember { mutableStateOf("") }
    val mentionedUsers = remember { mutableStateListOf<UserProfile>() }
    var showMentionModal by remember { mutableStateOf(false) }

    // Privacy & Action Controls
    var hideLikesCount by remember { mutableStateOf(false) }
    var turnOffComments by remember { mutableStateOf(false) }
    var turnOffRemix by remember { mutableStateOf(false) }

    // Step 3 Uploading States
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStageText by remember { mutableStateOf("Syncing details") }
    var isUploadFinished by remember { mutableStateOf(false) }
    var uploadErrorMessage by remember { mutableStateOf<String?>(null) }

    // Flash-specific trim parameters
    var flashTrimStartMs by remember { mutableLongStateOf(0L) }
    var flashTrimEndMs by remember { mutableLongStateOf(15000L) }
    var flashVideoDurationMs by remember { mutableLongStateOf(15000L) }

    // Auto-probe actual video duration when video slide is available
    LaunchedEffect(slides.firstOrNull()?.videoUri) {
        val vUri = slides.firstOrNull()?.videoUri ?: return@LaunchedEffect
        if (flashVideoDurationMs <= 0L || flashVideoDurationMs == 15000L) {
            val dur = com.chroma.studio.utils.VideoProcessor.getVideoDurationMs(context, vUri)
            if (dur > 0L) {
                flashVideoDurationMs = dur
                if (flashTrimEndMs <= 0L || flashTrimEndMs == 15000L) {
                    flashTrimEndMs = dur
                }
            }
        }
    }

    // Work Chooser Bottom Sheet / Dialog State
    var showWorkChooserSheet by remember { mutableStateOf(false) }

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
            // Go to Google Photos-style editor
            currentStep = StudioStep.STEP_0_PRE_TOOL
        } else if (slides.isEmpty()) {
            onNavigateBack()
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            slides.clear()
            slides.add(
                StudioSlide(
                    videoUri = uri,
                    label = "Flash Video"
                )
            )
            currentSlideIndex = 0
            currentStep = StudioStep.STEP_0_PRE_TOOL
        } else if (slides.isEmpty()) {
            onNavigateBack()
        }
    }

    val customCoverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customThumbUri = uri
        }
    }

    // Load Draft if initialDraftId is provided, or initialize with imported URIs, or launch picker if slides are empty
    LaunchedEffect(initialDraftId, initialImportedUris) {
        if (initialImportedUris.isNotEmpty()) {
            slides.clear()
            initialImportedUris.forEachIndexed { idx, uri ->
                slides.add(StudioSlide(videoUri = uri, label = "Clip ${idx + 1}"))
            }
            if (slides.isNotEmpty()) {
                currentSlideIndex = 0
            }
        } else if (!initialDraftId.isNullOrBlank()) {
            val draft = draftRepo.getDraftById(initialDraftId)
            if (draft != null) {
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
                        isMultipleMode = slides.size > 1
                    }
                    else -> { // post
                        draft.imageUris.forEachIndexed { idx, uriStr ->
                            slides.add(StudioSlide(imageUri = Uri.parse(uriStr), label = "Slide ${idx + 1}"))
                        }
                        isMultipleMode = slides.size > 1
                    }
                }

                if (slides.isNotEmpty()) {
                    currentSlideIndex = 0
                    currentStep = if (studioMode == CreatorStudioMode.FLASH) {
                        StudioStep.STEP_0_PRE_TOOL
                    } else {
                        StudioStep.STEP_1_PREVIEW
                    }
                }
            }
        } else if (slides.isEmpty()) {
            when (initialMode) {
                CreatorStudioMode.POST -> {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                CreatorStudioMode.FLASH -> {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
                CreatorStudioMode.WORK -> {
                    currentStep = StudioStep.STEP_0_PRE_TOOL
                }
            }
        }
    }


    // Function to save current studio state as Draft
    fun saveCurrentDraft(showToast: Boolean = true) {
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
        if (showToast) {
            Toast.makeText(context, "Draft saved successfully", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to trigger Step 3 Publishing
    fun executePublish() {
        currentStep = StudioStep.STEP_3_UPLOADING
        uploadProgress = 0.05f
        uploadStageText = "Syncing details"
        isUploadFinished = false
        uploadErrorMessage = null

        scope.launch {
            try {
                when (studioMode) {
                    CreatorStudioMode.POST -> {
                        uploadStageText = "Processing images"
                        uploadProgress = 0.2f
                        val activeSlides = if (isMultipleMode) slides.toList() else listOfNotNull(slides.getOrNull(currentSlideIndex))
                        val bytesList = mutableListOf<ByteArray>()

                        withContext(Dispatchers.IO) {
                            for (slide in activeSlides) {
                                slide.imageUri?.let { uri ->
                                    val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                                    if (bitmap != null) {
                                        val stream = ByteArrayOutputStream()
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                                        bytesList.add(stream.toByteArray())
                                    }
                                }
                            }
                        }

                        uploadStageText = "Uploading to Cloudinary"
                        uploadProgress = 0.5f

                        val success = withContext(Dispatchers.IO) {
                            discoverRepository.uploadMultiImagePost(
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
                        }

                        if (success) {
                            currentDraftId?.let { draftRepo.deleteDraft(it) }
                            uploadProgress = 1f
                            uploadStageText = "Published successfully!"
                            isUploadFinished = true
                        } else {
                            uploadErrorMessage = "Failed to upload post. Please try again."
                        }
                    }

                    CreatorStudioMode.WORK -> {
                        uploadStageText = "Compressing work vector layers"
                        uploadProgress = 0.3f
                        val activeSlides = if (isMultipleMode) slides.toList() else listOfNotNull(slides.getOrNull(currentSlideIndex))
                        val works = activeSlides.mapNotNull { it.work }

                        uploadStageText = "Publishing to community"
                        uploadProgress = 0.7f

                        val success = withContext(Dispatchers.IO) {
                            discoverRepository.publishMultiWork(
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
                        }

                        if (success) {
                            currentDraftId?.let { draftRepo.deleteDraft(it) }
                            uploadProgress = 1f
                            uploadStageText = "Published successfully!"
                            isUploadFinished = true
                        } else {
                            uploadErrorMessage = "Failed to publish work."
                        }
                    }

                    CreatorStudioMode.FLASH -> {
                        val videoSlide = slides.firstOrNull { it.videoUri != null }
                        val videoUri = videoSlide?.videoUri
                        if (videoUri == null) {
                            uploadErrorMessage = "No video available to publish."
                        } else {
                            // Step 1: Trim if necessary, otherwise pass through without re-encoding
                            uploadStageText = "Preparing video…"
                            uploadProgress = 0.10f
                            val trimmed = withContext(Dispatchers.IO) {
                                val shouldTrim = flashTrimStartMs > 100L || (flashTrimEndMs > 0L && flashTrimEndMs < flashVideoDurationMs - 100L)
                                if (shouldTrim) {
                                    com.chroma.studio.utils.VideoProcessor.trimVideo(
                                        context, videoUri, flashTrimStartMs, flashTrimEndMs
                                    )
                                } else {
                                    if (videoUri.scheme == "file") {
                                        java.io.File(videoUri.path ?: "")
                                    } else {
                                        val tempFile = java.io.File(context.cacheDir, "ready_post_${System.currentTimeMillis()}.mp4")
                                        try {
                                            context.contentResolver.openInputStream(videoUri)?.use { input ->
                                                java.io.FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                                            }
                                            if (tempFile.exists() && tempFile.length() > 0L) tempFile else null
                                        } catch (_: Throwable) { null }
                                    }
                                }
                            } ?: run {
                                uploadErrorMessage = "Video processing failed."
                                return@launch
                            }

                            // Step 2: Compress
                            uploadStageText = "Compressing video…"
                            uploadProgress = 0.30f
                            val compressed = withContext(Dispatchers.IO) {
                                com.chroma.studio.utils.VideoProcessor.compressVideo(
                                    context, android.net.Uri.fromFile(trimmed)
                                )
                            } ?: trimmed // fallback to trimmed if compression fails

                            // Step 3: Extract or use custom cover thumbnail
                            uploadStageText = "Generating cover thumbnail…"
                            uploadProgress = 0.50f
                            val thumbBitmap = withContext(Dispatchers.IO) {
                                if (customThumbUri != null) {
                                    try {
                                        val stream = context.contentResolver.openInputStream(customThumbUri!!)
                                        BitmapFactory.decodeStream(stream)
                                    } catch (_: Throwable) {
                                        null
                                    }
                                } else {
                                    val pos = if (selectedCoverFrameMs > 0L) {
                                        (selectedCoverFrameMs - flashTrimStartMs).coerceAtLeast(0L)
                                    } else 0L
                                    com.chroma.studio.utils.VideoProcessor.extractThumbnail(
                                        context, android.net.Uri.fromFile(compressed), pos
                                    )
                                }
                            }
                            val thumbUrl: String = if (thumbBitmap != null) {
                                val baos = ByteArrayOutputStream()
                                thumbBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, baos)
                                withContext(Dispatchers.IO) {
                                    com.chroma.studio.data.CloudinaryClient.uploadFlashThumbnail(baos.toByteArray()) ?: ""
                                }
                            } else ""

                            // Step 4: Upload video to Cloudinary
                            uploadStageText = "Uploading video…"
                            uploadProgress = 0.60f
                            val videoUrl = withContext(Dispatchers.IO) {
                                com.chroma.studio.data.CloudinaryClient.uploadVideoFromUri(
                                    context = context,
                                    uri = android.net.Uri.fromFile(compressed),
                                    folder = "community_videos",
                                    onProgress = { p -> uploadProgress = 0.60f + p * 0.30f }
                                )
                            } ?: run {
                                uploadErrorMessage = "Video upload failed. Check your connection and try again."
                                return@launch
                            }

                            // Cleanup temp files
                            withContext(Dispatchers.IO) {
                                if (compressed != trimmed) compressed.delete()
                                trimmed.delete()
                            }

                            // Step 5: Write to community_works Firestore
                            uploadStageText = "Publishing to Community…"
                            uploadProgress = 0.93f
                            val success = withContext(Dispatchers.IO) {
                                discoverRepository.uploadCommunityVideoPost(
                                    videoUrl = videoUrl,
                                    thumbnailUrl = thumbUrl,
                                    caption = if (postTitle.isNotBlank()) postTitle else postDescription,
                                    description = postDescription.trim(),
                                    aspectRatio = selectedRatioId,
                                    tags = hashtags.toList(),
                                    mentions = mentionedUsers.map { it.username },
                                    duration = flashTrimEndMs - flashTrimStartMs,
                                    categoryTag = selectedCategoryTag ?: customCategoryTag.ifBlank { null },
                                    hideLikesCount = hideLikesCount,
                                    turnOffComments = turnOffComments,
                                    turnOffRemix = turnOffRemix,
                                    hideDownloadsCount = !allowDownloads
                                )
                            }

                            if (success) {
                                currentDraftId?.let { draftRepo.deleteDraft(it) }
                                uploadProgress = 1f
                                uploadStageText = "Video post published!"
                                isUploadFinished = true
                            } else {
                                uploadErrorMessage = "Failed to publish video. Please try again."
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                uploadErrorMessage = e.message ?: "Upload encountered an error"
            }
        }
    }

    // Thirds grid briefly on ratio change
    fun updateRatio(newRatioId: String) {
        selectedRatioId = newRatioId
        showThirdsGridBriefly = true
        scope.launch {
            delay(1200)
            showThirdsGridBriefly = false
        }
    }

    // Back handling
    BackHandler {
        when (currentStep) {
            StudioStep.STEP_0_PRE_TOOL -> onNavigateBack()
            StudioStep.STEP_1_PREVIEW -> {
                // Return to pre-tool or exit
                if (slides.isNotEmpty()) {
                    currentStep = StudioStep.STEP_0_PRE_TOOL
                } else {
                    onNavigateBack()
                }
            }
            StudioStep.STEP_2_DETAILS -> {
                currentStep = StudioStep.STEP_1_PREVIEW
            }
            StudioStep.STEP_3_UPLOADING -> {
                onPostSuccess()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        when (currentStep) {
            // ================================================================
            // STEP 0: PRE-STEPS (IMAGE EDITOR / VIDEO TRIMMER / WORK CHOOSER)
            // ================================================================
            StudioStep.STEP_0_PRE_TOOL -> {
                when (studioMode) {
                    CreatorStudioMode.POST -> {
                        val currentSlide = slides.getOrNull(currentSlideIndex)
                        if (currentSlide?.imageUri != null) {
                            GooglePhotosStyleImageEditor(
                                slide = currentSlide,
                                onApply = { updatedSlide ->
                                    slides[currentSlideIndex] = updatedSlide
                                    currentStep = StudioStep.STEP_1_PREVIEW
                                },
                                onClose = {
                                    if (slides.size <= 1) {
                                        onNavigateBack()
                                    } else {
                                        currentStep = StudioStep.STEP_1_PREVIEW
                                    }
                                }
                            )
                        } else {
                            // Picker fallback
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("No photo selected", color = Color(0xFF64748B), fontSize = 14.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            imagePickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                    ) {
                                        Text("Select Photo")
                                    }
                                }
                            }
                        }
                    }

                    CreatorStudioMode.FLASH -> {
                        val videoSlide = slides.firstOrNull { it.videoUri != null }
                        // Open the FCP-style editor — passes any pre-selected URI,
                        // or null so the user can pick from inside the editor.
                        FinalCutProVideoEditorScreen(
                            initialUri = videoSlide?.videoUri,
                            initialUris = slides.mapNotNull { it.videoUri },
                            onDone = { startMs, endMs, uris ->
                                // Update trim params
                                flashTrimStartMs = startMs
                                flashTrimEndMs = endMs
                                flashVideoDurationMs = endMs - startMs
                                // Sync all URIs back to slides
                                slides.clear()
                                uris.forEachIndexed { idx, uri ->
                                    slides.add(StudioSlide(videoUri = uri, label = "Clip ${idx + 1}"))
                                }
                                currentStep = StudioStep.STEP_1_PREVIEW
                            },
                            onBack = { onNavigateBack() }
                        )
                    }

                    CreatorStudioMode.WORK -> {
                        WorkDraftPickerScreen(
                            workRepository = workRepository,
                            selectedWorkIds = slides.mapNotNull { it.work?.id }.toSet(),
                            onWorksSelected = { selectedWorks ->
                                slides.clear()
                                selectedWorks.forEachIndexed { idx, work ->
                                    slides.add(StudioSlide(work = work, label = "Work ${idx + 1}"))
                                    if (idx == 0 && postTitle.isBlank()) {
                                        postTitle = work.name
                                        postDescription = work.description
                                    }
                                }
                                isMultipleMode = selectedWorks.size > 1
                                currentStep = StudioStep.STEP_1_PREVIEW
                            },
                            onBack = { onNavigateBack() }
                        )
                    }
                }
            }

            // ================================================================
            // STEP 1: MAIN POST PREVIEW, RATIO & THUMBNAIL TOOLS
            // ================================================================
            StudioStep.STEP_1_PREVIEW -> {
                Step1PreviewAndToolsStage(
                    studioMode = studioMode,
                    slides = slides,
                    currentSlideIndex = currentSlideIndex,
                    isMultipleMode = isMultipleMode,
                    selectedRatioId = selectedRatioId,
                    showThirdsGrid = showThirdsGridBriefly,
                    activeToolTab = activeToolTab,
                    videoDurationMs = flashVideoDurationMs,
                    onSlideChange = { currentSlideIndex = it },
                    onToggleMultipleMode = {
                        isMultipleMode = it
                        if (!it && slides.isNotEmpty()) {
                            currentSlideIndex = 0
                        }
                    },
                    onAddMediaClick = {
                        when (studioMode) {
                            CreatorStudioMode.POST -> {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            CreatorStudioMode.FLASH -> {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                            CreatorStudioMode.WORK -> {
                                showWorkChooserSheet = true
                            }
                        }
                    },
                    onSelectRatio = { updateRatio(it) },
                    onSelectToolTab = { activeToolTab = it },
                    onOpenThumbnailModal = { showThumbModal = true },
                    onEditVideo = { currentStep = StudioStep.STEP_0_PRE_TOOL },
                    onCloseStudio = { onNavigateBack() },
                    onSaveAsDraft = { saveCurrentDraft(showToast = true) },
                    onNext = { currentStep = StudioStep.STEP_2_DETAILS }
                )
            }

            // ================================================================
            // STEP 2: POST DETAILS & MENTIONS
            // ================================================================
            StudioStep.STEP_2_DETAILS -> {
                Step2PostDetailsStage(
                    studioMode = studioMode,
                    slides = slides,
                    currentSlideIndex = currentSlideIndex,
                    selectedRatioId = selectedRatioId,
                    postTitle = postTitle,
                    postDescription = postDescription,
                    selectedCategoryTag = selectedCategoryTag,
                    customCategoryTag = customCategoryTag,
                    hashtags = hashtags,
                    mentionedUsers = mentionedUsers,
                    customCoverUri = customThumbUri,
                    selectedCoverFrameMs = selectedCoverFrameMs,
                    videoDurationMs = flashVideoDurationMs,
                    audienceVisibility = audienceVisibility,
                    includeOriginalAudio = includeOriginalAudio,
                    allowDownloads = allowDownloads,
                    highQualityUpload = highQualityUpload,
                    hideLikesCount = hideLikesCount,
                    turnOffComments = turnOffComments,
                    turnOffRemix = turnOffRemix,
                    onTitleChange = { if (it.length <= 100) postTitle = it },
                    onDescriptionChange = { if (it.length <= 1500) postDescription = it },
                    onSelectCategory = { selectedCategoryTag = if (selectedCategoryTag == it) null else it },
                    onCustomCategoryChange = { customCategoryTag = it },
                    onAddCustomCategory = {
                        if (customCategoryTag.isNotBlank()) {
                            selectedCategoryTag = customCategoryTag.trim()
                            customCategoryTag = ""
                        }
                    },
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
                    onToggleIncludeOriginalAudio = { includeOriginalAudio = it },
                    onToggleAllowDownloads = { allowDownloads = it },
                    onToggleHighQualityUpload = { highQualityUpload = it },
                    onToggleHideLikes = { hideLikesCount = it },
                    onToggleTurnOffComments = { turnOffComments = it },
                    onToggleTurnOffRemix = { turnOffRemix = it },
                    onSaveAsDraft = { saveCurrentDraft(showToast = true) },
                    onEditCover = { showThumbModal = true },
                    onEditVideo = { currentStep = StudioStep.STEP_0_PRE_TOOL },
                    onBack = { currentStep = StudioStep.STEP_1_PREVIEW },
                    onPost = { executePublish() }
                )
            }

            // ================================================================
            // STEP 3: BACKGROUND UPLOADING OVERLAY
            // ================================================================
            StudioStep.STEP_3_UPLOADING -> {
                Step3UploadingOverlayStage(
                    studioMode = studioMode,
                    slides = slides,
                    currentSlideIndex = currentSlideIndex,
                    selectedRatioId = selectedRatioId,
                    uploadProgress = uploadProgress,
                    uploadStageText = uploadStageText,
                    isUploadFinished = isUploadFinished,
                    errorMessage = uploadErrorMessage,
                    onExitScreen = {
                        onPostSuccess()
                    },
                    onExitApp = {
                        (context as? Activity)?.finishAffinity()
                    },
                    onCreateAnotherPost = {
                        slides.clear()
                        currentStep = StudioStep.STEP_0_PRE_TOOL
                        postTitle = ""
                        postDescription = ""
                        hashtags.clear()
                        mentionedUsers.clear()
                        when (studioMode) {
                            CreatorStudioMode.POST -> {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            CreatorStudioMode.FLASH -> {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                            CreatorStudioMode.WORK -> {}
                        }
                    }
                )
            }
        }

        // ====================================================================
        // MODAL 1: PROFILE THUMBNAIL PREVIEW MODAL ("Only shows in profile")
        // ====================================================================
        if (showThumbModal) {
            ThumbnailPreviewModal(
                studioMode = studioMode,
                slides = slides,
                selectedRatioId = selectedRatioId,
                selectedSlideIndex = customThumbSlideIndex,
                cropOffsetPercent = thumbCropOffset,
                customCoverUri = customThumbUri,
                selectedCoverFrameMs = selectedCoverFrameMs,
                videoDurationMs = flashVideoDurationMs,
                onCropOffsetChange = { thumbCropOffset = it },
                onSelectSlideIndex = { customThumbSlideIndex = it },
                onSelectCoverFrameMs = { selectedCoverFrameMs = it },
                onUploadCustomCover = {
                    customCoverPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClearCustomCover = { customThumbUri = null },
                onDismiss = { showThumbModal = false }
            )
        }

        // ====================================================================
        // MODAL 2: MENTION USER SEARCH MODAL (MAX 5 USERS)
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
        // MODAL 3: WORK CHOOSER SHEET (When user taps '+' in Step 1 for Work)
        // ====================================================================
        if (showWorkChooserSheet) {
            WorkDraftPickerModalSheet(
                workRepository = workRepository,
                selectedWorkIds = slides.mapNotNull { it.work?.id }.toSet(),
                onAddWorks = { additionalWorks ->
                    additionalWorks.forEach { work ->
                        if (slides.none { it.work?.id == work.id }) {
                            slides.add(StudioSlide(work = work, label = "Work ${slides.size + 1}"))
                        }
                    }
                    if (slides.size > 1) {
                        isMultipleMode = true
                    }
                    showWorkChooserSheet = false
                },
                onDismiss = { showWorkChooserSheet = false }
            )
        }
    }
}

// ============================================================================
// STEP 0A: GOOGLE PHOTOS STYLE IMAGE EDITOR
// ============================================================================

@Composable
fun GooglePhotosStyleImageEditor(
    slide: StudioSlide,
    onApply: (StudioSlide) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var rotationAngle by remember { mutableFloatStateOf(slide.rotationDegrees) }
    var brightness by remember { mutableFloatStateOf(slide.brightness) }
    var contrast by remember { mutableFloatStateOf(slide.contrast) }
    var saturation by remember { mutableFloatStateOf(slide.saturation) }
    var warmth by remember { mutableFloatStateOf(slide.warmth) }
    var activeFilterName by remember { mutableStateOf(slide.filterName) }

    // Google Photos Bottom Tabs: 0: Crop/Rotate, 1: Adjust, 2: Filters
    var editorTab by remember { mutableIntStateOf(0) }
    // Adjust sub-tool: "Brightness", "Contrast", "Saturation", "Warmth"
    var adjustSubTool by remember { mutableStateOf("Brightness") }

    val computedColorMatrix = remember(brightness, contrast, saturation, warmth, activeFilterName) {
        buildStudioColorMatrix(brightness, contrast, saturation, warmth, activeFilterName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Lucide.X, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                text = "Edit Photo",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = {
                    onApply(
                        slide.copy(
                            rotationDegrees = rotationAngle,
                            brightness = brightness,
                            contrast = contrast,
                            saturation = saturation,
                            warmth = warmth,
                            filterName = activeFilterName
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Done", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        // Center Preview Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(slide.imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Editing preview",
                colorFilter = ColorFilter.colorMatrix(computedColorMatrix),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(rotationZ = rotationAngle)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        }

        // Tool Adjustment Panels (Google Photos style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(vertical = 12.dp)
        ) {
            when (editorTab) {
                // 1. Crop & Rotate Tab
                0 -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Lucide.RotateCw, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Rotate 90°", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                rotationAngle = 0f
                                brightness = 0f
                                contrast = 1f
                                saturation = 1f
                                warmth = 0f
                                activeFilterName = "Original"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset All", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // 2. Adjust Sliders Tab
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sub-tool chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val adjustTools = listOf("Brightness", "Contrast", "Saturation", "Warmth")
                            items(adjustTools) { tool ->
                                val isSelected = adjustSubTool == tool
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) Color(0xFF2563EB) else Color(0xFF334155))
                                        .clickable { adjustSubTool = tool }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tool,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Slider control for active sub-tool
                        when (adjustSubTool) {
                            "Brightness" -> {
                                SliderWithLabel(
                                    label = "Brightness",
                                    value = brightness,
                                    valueRange = -100f..100f,
                                    onValueChange = { brightness = it },
                                    displayFormatter = { "%+.0f".format(it) }
                                )
                            }
                            "Contrast" -> {
                                SliderWithLabel(
                                    label = "Contrast",
                                    value = contrast,
                                    valueRange = 0.5f..2.0f,
                                    onValueChange = { contrast = it },
                                    displayFormatter = { "%.2fx".format(it) }
                                )
                            }
                            "Saturation" -> {
                                SliderWithLabel(
                                    label = "Saturation",
                                    value = saturation,
                                    valueRange = 0f..3f,
                                    onValueChange = { saturation = it },
                                    displayFormatter = { "%.2fx".format(it) }
                                )
                            }
                            "Warmth" -> {
                                SliderWithLabel(
                                    label = "Warmth",
                                    value = warmth,
                                    valueRange = -100f..100f,
                                    onValueChange = { warmth = it },
                                    displayFormatter = { "%+.0f".format(it) }
                                )
                            }
                        }
                    }
                }

                // 3. Filters Tab
                2 -> {
                    val filterPresets = listOf("Original", "Enhance", "Vivid", "Warm", "Cool", "Onyx")
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filterPresets) { preset ->
                            val isSelected = activeFilterName == preset
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { activeFilterName = preset }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF475569),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.Sparkles,
                                        contentDescription = preset,
                                        tint = if (isSelected) Color(0xFF2563EB) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color(0xFF60A5FA) else Color(0xFFCBD5E1),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Tabs (Crop / Adjust / Filters)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .border(BorderStroke(1.dp, Color(0xFF1E293B)))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val bottomTabs = listOf(
                Triple(0, "Crop", Lucide.Crop),
                Triple(1, "Adjust", Lucide.SlidersHorizontal),
                Triple(2, "Filters", Lucide.Sparkles)
            )

            bottomTabs.forEach { (index, title, icon) ->
                val isSelected = editorTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { editorTab = index }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderWithLabel(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    displayFormatter: (Float) -> String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
            Text(displayFormatter(value), color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF2563EB),
                activeTrackColor = Color(0xFF2563EB),
                inactiveTrackColor = Color(0xFF475569)
            )
        )
    }
}

// ============================================================================
// STEP 0B: FLASH VIDEO TRIMMER STAGE
// ============================================================================

@Composable
fun FlashVideoTrimmerStage(
    videoUri: Uri,
    initialStartMs: Long = 0L,
    initialEndMs: Long = 15000L,
    onTrimmed: (startMs: Long, endMs: Long, totalDurationMs: Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var trimStartMs by remember { mutableLongStateOf(initialStartMs) }
    var trimEndMs by remember { mutableLongStateOf(initialEndMs) }
    var durationMs by remember { mutableLongStateOf(15000L) }
    var isPlaying by remember { mutableStateOf(true) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val d = duration.coerceAtLeast(1000L)
                        durationMs = d
                        if (trimEndMs > d || trimEndMs == 15000L) {
                            trimEndMs = d.coerceAtMost(30000L)
                        }
                    }
                }
            })
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Keep playback looping inside trimmed window
    LaunchedEffect(exoPlayer, trimStartMs, trimEndMs) {
        while (true) {
            delay(100)
            if (exoPlayer.currentPosition >= trimEndMs) {
                exoPlayer.seekTo(trimStartMs)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1D24))
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color(0xFF1E212B))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Lucide.ArrowLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text("Trim Flash", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Button(
                onClick = {
                    exoPlayer.pause()
                    onTrimmed(trimStartMs, trimEndMs, durationMs)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Next", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Video Player Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF111317))
                .clickable {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        exoPlayer.play()
                        isPlaying = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
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
                modifier = Modifier.fillMaxSize()
            )

            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Play, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }

        // Trimming Timeline Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E212B))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val trimSeconds = ((trimEndMs - trimStartMs) / 1000L).coerceAtLeast(1L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Range: %02d:%02d - %02d:%02d".format(
                    (trimStartMs / 1000) / 60, (trimStartMs / 1000) % 60,
                    (trimEndMs / 1000) / 60, (trimEndMs / 1000) % 60
                ), color = Color(0xFF8E98A8), fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)

                Text(
                    text = "${trimSeconds}s duration (Max 30s)",
                    color = if (trimSeconds > 30) Color(0xFFEF4444) else Color(0xFF007AFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Draggable Range Trimmer Bar
            TrimmerTimelineBar(
                durationMs = durationMs,
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                onRangeChanged = { start, end ->
                    trimStartMs = start
                    trimEndMs = end
                    exoPlayer.seekTo(start)
                }
            )
        }
    }
}

@Composable
private fun TrimmerTimelineBar(
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    onRangeChanged: (Long, Long) -> Unit
) {
    val density = LocalDensity.current
    var barWidthPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF334155))
            .onSizeChanged { barWidthPx = it.width.toFloat() }
    ) {
        if (barWidthPx > 0f && durationMs > 0L) {
            val startFrac = (trimStartMs.toFloat() / durationMs).coerceIn(0f, 1f)
            val endFrac = (trimEndMs.toFloat() / durationMs).coerceIn(0f, 1f)
            val rangeWidth = (endFrac - startFrac).coerceAtLeast(0.05f)

            // Selected Range Box (Yellow highlight)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(rangeWidth)
                    .offset(x = with(density) { (startFrac * barWidthPx).toDp() })
                    .background(Color(0xFFFFD60A).copy(alpha = 0.25f))
                    .border(2.dp, Color(0xFFFFD60A))
            )

            // Start Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(18.dp)
                    .offset(x = with(density) { (startFrac * barWidthPx - 9.dp.toPx()).toDp() })
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFD60A))
                    .pointerInput(barWidthPx, durationMs) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val newFrac = ((startFrac * barWidthPx + dragAmount) / barWidthPx).coerceIn(0f, endFrac - 0.05f)
                            val newStart = (newFrac * durationMs).roundToLong()
                            onRangeChanged(newStart, trimEndMs)
                        }
                    }
            )

            // End Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(18.dp)
                    .offset(x = with(density) { (endFrac * barWidthPx - 9.dp.toPx()).toDp() })
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFD60A))
                    .pointerInput(barWidthPx, durationMs) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val newFrac = ((endFrac * barWidthPx + dragAmount) / barWidthPx).coerceIn(startFrac + 0.05f, 1f)
                            val newEnd = (newFrac * durationMs).roundToLong()
                            onRangeChanged(trimStartMs, newEnd)
                        }
                    }
            )
        }
    }
}

// ============================================================================
// STEP 0C: WORK & DRAFT PICKER SCREEN
// ============================================================================

@Composable
fun WorkDraftPickerScreen(
    workRepository: WorkRepository,
    selectedWorkIds: Set<String>,
    onWorksSelected: (List<ChromaWork>) -> Unit,
    onBack: () -> Unit
) {
    val allWorks = remember { workRepository.loadActiveWorks() }
    val selected = remember { mutableStateListOf<ChromaWork>() }

    LaunchedEffect(allWorks) {
        if (selected.isEmpty()) {
            allWorks.filter { selectedWorkIds.contains(it.id) }.forEach { selected.add(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Lucide.ArrowLeft, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(22.dp))
            }
            Text(
                text = "Choose Work & Drafts",
                color = Color(0xFF0F172A),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = { onWorksSelected(selected.toList()) },
                enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    disabledContainerColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Continue (${selected.size})", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (allWorks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Lucide.Palette, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No saved works or drafts found", color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Create a new gradient work on Home canvas and save it first.", color = Color(0xFF64748B), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allWorks) { work ->
                    val isChecked = selected.any { it.id == work.id }
                    WorkCardItem(
                        work = work,
                        workRepository = workRepository,
                        isSelected = isChecked,
                        onClick = {
                            if (isChecked) {
                                selected.removeAll { it.id == work.id }
                            } else {
                                selected.add(work)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkCardItem(
    work: ChromaWork,
    workRepository: WorkRepository,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val layers = remember(work.layersJson) {
        workRepository.deserializeLayers(work.layersJson)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
        ),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                CanvasPreview(
                    layers = layers,
                    shape = work.canvasShape,
                    borderColor = Color.Transparent,
                    modifier = Modifier.fillMaxSize()
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = work.name.ifBlank { "Untitled Work" },
                color = Color(0xFF0F172A),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${layers.size} layers • ${work.canvasShape}",
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
        }
    }
}

// Work Draft Picker Sheet (Inside Step 1 when user clicks '+')
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDraftPickerModalSheet(
    workRepository: WorkRepository,
    selectedWorkIds: Set<String>,
    onAddWorks: (List<ChromaWork>) -> Unit,
    onDismiss: () -> Unit
) {
    val allWorks = remember { workRepository.loadActiveWorks() }
    val selected = remember { mutableStateListOf<ChromaWork>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add from Saved Works & Drafts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Button(
                    onClick = { onAddWorks(selected.toList()) },
                    enabled = selected.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Add (${selected.size})", color = Color.White, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                items(allWorks) { work ->
                    val isChecked = selected.any { it.id == work.id } || selectedWorkIds.contains(work.id)
                    WorkCardItem(
                        work = work,
                        workRepository = workRepository,
                        isSelected = isChecked,
                        onClick = {
                            if (!selectedWorkIds.contains(work.id)) {
                                if (isChecked) selected.removeAll { it.id == work.id }
                                else selected.add(work)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ============================================================================
// STEP 1: PREVIEW, SCROLL DOTS, RATIO & THUMBNAIL TOOLS
// ============================================================================

@Composable
fun Step1PreviewAndToolsStage(
    studioMode: CreatorStudioMode,
    slides: List<StudioSlide>,
    currentSlideIndex: Int,
    isMultipleMode: Boolean,
    selectedRatioId: String,
    showThirdsGrid: Boolean,
    activeToolTab: Int,
    videoDurationMs: Long = 0L,
    onSlideChange: (Int) -> Unit,
    onToggleMultipleMode: (Boolean) -> Unit,
    onAddMediaClick: () -> Unit,
    onSelectRatio: (String) -> Unit,
    onSelectToolTab: (Int) -> Unit,
    onOpenThumbnailModal: () -> Unit,
    onEditVideo: () -> Unit,
    onCloseStudio: () -> Unit,
    onSaveAsDraft: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val currentRatio = STUDIO_RATIO_CONFIGS.firstOrNull { it.id == selectedRatioId }
        ?: STUDIO_RATIO_CONFIGS[2] // 4:3 default

    val currentSlide = slides.getOrNull(currentSlideIndex)
    val isVideoSlide = currentSlide?.videoUri != null

    var isVideoPlaying by remember { mutableStateOf(false) }
    var isVideoMuted by remember { mutableStateOf(false) }

    val exoPlayer = remember(currentSlide?.videoUri) {
        if (currentSlide?.videoUri != null) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(currentSlide.videoUri))
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 1f
                prepare()
            }
        } else null
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    LaunchedEffect(isVideoPlaying, isVideoMuted) {
        exoPlayer?.let { player ->
            player.volume = if (isVideoMuted) 0f else 1f
            if (isVideoPlaying) player.play() else player.pause()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // ── Top Header ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(if (colors.isDark) Color(0xFF111827) else Color.White)
                .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCloseStudio) {
                Icon(Lucide.X, contentDescription = "Close", tint = colors.textMuted, modifier = Modifier.size(20.dp))
            }

            Text(
                text = when (studioMode) {
                    CreatorStudioMode.POST -> "New Post"
                    CreatorStudioMode.FLASH -> "New Flash Video"
                    CreatorStudioMode.WORK -> "New Work"
                },
                color = colors.textMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveAsDraft,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textMain)
                ) {
                    Text("Draft", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = {
                        exoPlayer?.pause()
                        isVideoPlaying = false
                        onNext()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Next", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Content Body ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode Controls (Multiple vs Single + Add Media Icon Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (colors.isDark) Color(0xFF1F2937) else Color(0xFFF1F5F9))
                        .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isMultipleMode) Color(0xFF2563EB) else Color.Transparent)
                            .clickable { onToggleMultipleMode(true) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Multiple (${slides.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isMultipleMode) Color.White else colors.textMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isMultipleMode) Color(0xFF2563EB) else Color.Transparent)
                            .clickable { onToggleMultipleMode(false) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Single",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (!isMultipleMode) Color.White else colors.textMuted
                        )
                    }
                }

                // Add Media (+) Icon Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2563EB))
                        .clickable(onClick = onAddMediaClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Plus, contentDescription = "Add Media", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(color = if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
            Spacer(Modifier.height(12.dp))

            // ── Main Post Preview Stage (Dynamic Aspect Ratio Box) ───────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated Aspect Ratio Box
                    Box(
                        modifier = Modifier
                            .aspectRatio(currentRatio.wRatio / currentRatio.hRatio)
                            .fillMaxHeight(0.96f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clickable {
                                if (isVideoSlide) {
                                    isVideoPlaying = !isVideoPlaying
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isVideoSlide && isVideoPlaying && exoPlayer != null) {
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
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (currentSlide != null) {
                            SlidePreviewContent(
                                slide = currentSlide,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Play/Pause indicator for video
                        if (isVideoSlide) {
                            if (!isVideoPlaying) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.Play,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp).padding(start = 3.dp)
                                    )
                                }
                            }

                            // Top-Right Mute Toggle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { isVideoMuted = !isVideoMuted },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVideoMuted) Lucide.VolumeX else Lucide.Volume2,
                                    contentDescription = "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Bottom-Right Duration Pill
                            if (videoDurationMs > 0L) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.65f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Lucide.Video, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                        Text(
                                            text = formatDurationTime(videoDurationMs),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Bottom-Left "Trim & Edit" Shortcut
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .clickable {
                                        exoPlayer?.pause()
                                        isVideoPlaying = false
                                        onEditVideo()
                                    },
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Lucide.Scissors, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Trim & Edit",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Rule-of-thirds grid overlay
                        if (showThirdsGrid) {
                            ThirdsGridOverlay(modifier = Modifier.fillMaxSize())
                        }

                        // Left / Right Carousel Chevrons
                        if (isMultipleMode && slides.size > 1) {
                            if (currentSlideIndex > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 8.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.9f))
                                        .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                                        .clickable { onSlideChange(currentSlideIndex - 1) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.ChevronLeft, contentDescription = "Previous", tint = Color(0xFF1E293B), modifier = Modifier.size(16.dp))
                                }
                            }

                            if (currentSlideIndex < slides.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 8.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.9f))
                                        .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                                        .clickable { onSlideChange(currentSlideIndex + 1) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.ChevronRight, contentDescription = "Next", tint = Color(0xFF1E293B), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Scroll Dots Under Preview
                if (slides.size > 1) {
                    Row(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .height(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        slides.forEachIndexed { index, _ ->
                            val isActive = index == currentSlideIndex
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(if (isActive) 18.dp else 6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isActive) Color(0xFF2563EB) else (if (colors.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1)))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
            Spacer(Modifier.height(12.dp))

            // ── Tools To Adjust (Tab Buttons: Ratio vs Thumbnail Preview) ────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ratio Tab Button
                Button(
                    onClick = { onSelectToolTab(0) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeToolTab == 0) Color(0xFF2563EB) else (if (colors.isDark) Color(0xFF1F2937) else Color.White),
                        contentColor = if (activeToolTab == 0) Color.White else colors.textMain
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (activeToolTab == 0) Color(0xFF2563EB) else (if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1)))
                ) {
                    Icon(Lucide.Proportions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ratio (${currentRatio.label})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Thumbnail Preview Tab Button
                Button(
                    onClick = { onSelectToolTab(1) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeToolTab == 1) Color(0xFF2563EB) else (if (colors.isDark) Color(0xFF1F2937) else Color.White),
                        contentColor = if (activeToolTab == 1) Color.White else colors.textMain
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (activeToolTab == 1) Color(0xFF2563EB) else (if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1)))
                ) {
                    Icon(Lucide.Scan, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cover & Profile Grid", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── PANEL A: 5 Required Aspect Ratio Cards ───────────────────────
            if (activeToolTab == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Select aspect ratio", color = colors.textMuted, fontSize = 12.sp)
                        Text(currentRatio.description, color = Color(0xFF2563EB), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    // 5 Graphic Ratio Cards (16:9, 9:16, 4:3, 5:4, 1:1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        STUDIO_RATIO_CONFIGS.forEach { config ->
                            val isSelected = config.id == selectedRatioId
                            RatioCardOption(
                                config = config,
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = { onSelectRatio(config.id) }
                            )
                        }
                    }
                }
            }

            // ── PANEL B: Profile Thumbnail Summary Card ──────────────────────
            if (activeToolTab == 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (colors.isDark) Color(0xFF1F2937) else Color(0xFFF8FAFC))
                        .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF2563EB), RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentSlide != null) {
                                SlidePreviewContent(slide = currentSlide, modifier = Modifier.fillMaxSize())
                            }
                        }

                        Column {
                            Text("Profile Grid Thumbnail", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Visible in 1:1 profile grid & feed", color = Color(0xFF2563EB), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Button(
                        onClick = onOpenThumbnailModal,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Edit Cover", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RatioCardOption(
    config: StudioRatioConfig,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) (if (colors.isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEFF6FF)) else (if (colors.isDark) Color(0xFF1F2937) else Color.White))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF2563EB) else (if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(config.boxWidthDp, config.boxHeightDp)
                    .clip(RoundedCornerShape(3.dp))
                    .border(
                        1.5.dp,
                        if (isSelected) Color(0xFF2563EB) else (if (colors.isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF94A3B8)),
                        RoundedCornerShape(3.dp)
                    )
                    .background(if (isSelected) (if (colors.isDark) Color(0xFF1E293B) else Color.White) else (if (colors.isDark) Color(0xFF111827) else Color(0xFFF8FAFC))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = config.label,
                    tint = if (isSelected) Color(0xFF2563EB) else colors.textMuted,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = config.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color(0xFF2563EB) else colors.textMain
        )
    }
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
            val cm = remember(slide) {
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
private fun ThirdsGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gridColor = Color(0xFF2563EB).copy(alpha = 0.45f)
        val strokeWidth = 1.5f

        // Two vertical lines
        drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth)
        drawLine(gridColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth)

        // Two horizontal lines
        drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth)
        drawLine(gridColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth)
    }
}

// ============================================================================
// STEP 2: POST DETAILS STAGE
// ============================================================================

@Composable
fun Step2PostDetailsStage(
    studioMode: CreatorStudioMode,
    slides: List<StudioSlide>,
    currentSlideIndex: Int,
    selectedRatioId: String,
    postTitle: String,
    postDescription: String,
    selectedCategoryTag: String?,
    customCategoryTag: String,
    hashtags: List<String>,
    mentionedUsers: List<UserProfile>,
    customCoverUri: Uri?,
    selectedCoverFrameMs: Long,
    videoDurationMs: Long,
    audienceVisibility: String,
    includeOriginalAudio: Boolean,
    allowDownloads: Boolean,
    highQualityUpload: Boolean,
    hideLikesCount: Boolean,
    turnOffComments: Boolean,
    turnOffRemix: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onCustomCategoryChange: (String) -> Unit,
    onAddCustomCategory: () -> Unit,
    onAddHashtag: (String) -> Unit,
    onRemoveHashtag: (String) -> Unit,
    onOpenMentionSearch: () -> Unit,
    onRemoveMention: (UserProfile) -> Unit,
    onAudienceVisibilityChange: (String) -> Unit,
    onToggleIncludeOriginalAudio: (Boolean) -> Unit,
    onToggleAllowDownloads: (Boolean) -> Unit,
    onToggleHighQualityUpload: (Boolean) -> Unit,
    onToggleHideLikes: (Boolean) -> Unit,
    onToggleTurnOffComments: (Boolean) -> Unit,
    onToggleTurnOffRemix: (Boolean) -> Unit,
    onSaveAsDraft: () -> Unit,
    onEditCover: () -> Unit,
    onEditVideo: () -> Unit,
    onBack: () -> Unit,
    onPost: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val currentRatio = STUDIO_RATIO_CONFIGS.firstOrNull { it.id == selectedRatioId }
        ?: STUDIO_RATIO_CONFIGS[2]
    val currentSlide = slides.getOrNull(currentSlideIndex)
    val isVideo = studioMode == CreatorStudioMode.FLASH || currentSlide?.videoUri != null
    var localTagInput by remember { mutableStateOf("") }

    val popularHashtagSuggestions = remember {
        listOf("video", "motion", "art", "creative", "cinematic", "viral", "chroma", "editing", "design")
    }

    var coverThumbnailBmp by remember(customCoverUri, selectedCoverFrameMs, currentSlide?.videoUri) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(customCoverUri, selectedCoverFrameMs, currentSlide?.videoUri) {
        if (customCoverUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val stream = context.contentResolver.openInputStream(customCoverUri)
                    coverThumbnailBmp = BitmapFactory.decodeStream(stream)
                } catch (_: Throwable) {
                    coverThumbnailBmp = null
                }
            }
        } else if (currentSlide?.videoUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, currentSlide.videoUri)
                    val timeUs = selectedCoverFrameMs * 1000L
                    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 320, 400)
                    } else {
                        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                    retriever.release()
                    coverThumbnailBmp = bmp
                } catch (_: Throwable) {
                    coverThumbnailBmp = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // ── Top Header ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(if (colors.isDark) Color(0xFF111827) else Color.White)
                .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onBack)
            ) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = "Back",
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Back",
                    color = colors.textMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = if (isVideo) "Video Details" else "Post Details",
                color = colors.textMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveAsDraft,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textMain)
                ) {
                    Text("Draft", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = onPost,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(if (isVideo) "Publish Video" else "Post", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Details Form Body ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. MEDIA & COVER CARD ────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (colors.isDark) Color(0xFF1F2937) else Color.White,
                border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini Cover Box
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(96.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverThumbnailBmp != null) {
                            Image(
                                bitmap = coverThumbnailBmp!!.asImageBitmap(),
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (currentSlide != null) {
                            SlidePreviewContent(slide = currentSlide, modifier = Modifier.fillMaxSize())
                        }

                        // Duration pill in corner
                        if (isVideo && videoDurationMs > 0L) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = formatDurationTime(videoDurationMs),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Right Info & Action Buttons
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF2563EB).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = currentRatio.label,
                                    color = Color(0xFF2563EB),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (isVideo) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "VIDEO",
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (customCoverUri != null) "Custom cover photo active" else if (isVideo) "Frame cover selected" else "Slide thumbnail",
                            color = colors.textMuted,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onEditCover,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF2563EB)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Lucide.Image, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Cover", color = Color(0xFF2563EB), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            if (isVideo) {
                                OutlinedButton(
                                    onClick = onEditVideo,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Lucide.Scissors, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit Video", color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            // ── 2. TITLE & CAPTION CARD ───────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (colors.isDark) Color(0xFF1F2937) else Color.White,
                border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Main Title", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("${postTitle.length} / 100", color = colors.textMuted, fontSize = 11.sp)
                        }

                        Spacer(Modifier.height(6.dp))
                        BasicTextField(
                            value = postTitle,
                            onValueChange = onTitleChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                .background(if (colors.isDark) Color(0xFF111827) else Color(0xFFF8FAFC))
                                .padding(10.dp),
                            textStyle = TextStyle(color = colors.textMain, fontSize = 13.sp),
                            decorationBox = { innerTextField ->
                                if (postTitle.isEmpty()) {
                                    Text("Enter video title (e.g. Cinematic Motion Reel)...", color = colors.textMuted, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )
                    }

                    // Description / Caption
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Caption & Description", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("${postDescription.length} / 1500", color = colors.textMuted, fontSize = 11.sp)
                        }

                        Spacer(Modifier.height(6.dp))
                        BasicTextField(
                            value = postDescription,
                            onValueChange = onDescriptionChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                .background(if (colors.isDark) Color(0xFF111827) else Color(0xFFF8FAFC))
                                .padding(10.dp),
                            textStyle = TextStyle(color = colors.textMain, fontSize = 13.sp),
                            decorationBox = { innerTextField ->
                                if (postDescription.isEmpty()) {
                                    Text("Write a caption, tell the story behind your creation...", color = colors.textMuted, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )
                    }

                    // Quick Hashtag Suggestions Bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Lucide.Hash, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                            Text("Quick Hashtags:", color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(popularHashtagSuggestions) { tag ->
                                val isSelected = hashtags.contains(tag)
                                Surface(
                                    modifier = Modifier.clickable {
                                        if (isSelected) onRemoveHashtag(tag) else onAddHashtag(tag)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF2563EB) else (if (colors.isDark) Color(0xFF111827) else Color(0xFFEFF6FF)),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else (if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFBFDBFE)))
                                ) {
                                    Text(
                                        text = "#$tag",
                                        color = if (isSelected) Color.White else Color(0xFF1D4ED8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Active Hashtags List
                    if (hashtags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            hashtags.forEach { tag ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (colors.isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF))
                                        .border(1.dp, if (colors.isDark) Color(0xFF2563EB).copy(alpha = 0.5f) else Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("#$tag", color = if (colors.isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Icon(
                                        imageVector = Lucide.X,
                                        contentDescription = "Remove",
                                        tint = if (colors.isDark) Color(0xFF60A5FA) else Color(0xFF60A5FA),
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clickable { onRemoveHashtag(tag) }
                                    )
                                }
                            }
                        }
                    }

                    // Custom Hashtag input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = localTagInput,
                            onValueChange = { localTagInput = it },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (localTagInput.isNotBlank()) {
                                    onAddHashtag(localTagInput)
                                    localTagInput = ""
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                .background(if (colors.isDark) Color(0xFF111827) else Color(0xFFF8FAFC))
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            textStyle = TextStyle(color = colors.textMain, fontSize = 12.sp),
                            decorationBox = { innerTextField ->
                                if (localTagInput.isEmpty()) {
                                    Text("Add custom hashtag and tap +", color = colors.textMuted, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2563EB))
                                .clickable {
                                    if (localTagInput.isNotBlank()) {
                                        onAddHashtag(localTagInput)
                                        localTagInput = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Plus, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // ── 3. SOUND & AUDIO ATTRIBUTION CARD (WHEN VIDEO) ───────────────
            if (isVideo) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (colors.isDark) Color(0xFF1F2937) else Color.White,
                    border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Music, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                            }

                            Column {
                                Text("Original Audio", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Include original audio stream in video post", color = colors.textMuted, fontSize = 11.sp)
                            }
                        }

                        Switch(
                            checked = includeOriginalAudio,
                            onCheckedChange = onToggleIncludeOriginalAudio,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF8B5CF6),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = if (colors.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }

            // ── 4. AUDIENCE & VISIBILITY CARD ────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (colors.isDark) Color(0xFF1F2937) else Color.White,
                border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Who Can Watch This Video", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val visOptions = listOf(
                            Triple("public", "Public", Lucide.Globe),
                            Triple("followers", "Followers", Lucide.Users),
                            Triple("private", "Only Me", Lucide.Lock)
                        )

                        visOptions.forEach { (id, label, icon) ->
                            val isSel = audienceVisibility == id
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onAudienceVisibilityChange(id) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) (if (colors.isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEFF6FF)) else (if (colors.isDark) Color(0xFF111827) else Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF2563EB) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)))
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSel) Color(0xFF2563EB) else colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color(0xFF2563EB) else colors.textMain
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 5. POST TYPE / CATEGORY TAG ──────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (colors.isDark) Color(0xFF1F2937) else Color.White,
                border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text("Category Tag (Discovery Algorithm)", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Used by the recommendation feed to match viewers", color = colors.textMuted, fontSize = 11.sp)
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ALGO_CATEGORIES.forEach { category ->
                            val isSelected = selectedCategoryTag == category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF2563EB) else (if (colors.isDark) Color(0xFF111827) else Color(0xFFF1F5F9)))
                                    .border(1.dp, if (isSelected) Color(0xFF2563EB) else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)), RoundedCornerShape(6.dp))
                                    .clickable { onSelectCategory(category) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = category,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else colors.textMain
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = customCategoryTag,
                            onValueChange = onCustomCategoryChange,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                .background(if (colors.isDark) Color(0xFF111827) else Color(0xFFF8FAFC))
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            textStyle = TextStyle(color = colors.textMain, fontSize = 12.sp),
                            decorationBox = { innerTextField ->
                                if (customCategoryTag.isEmpty()) {
                                    Text("Specify other category...", color = colors.textMuted, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2563EB))
                                .clickable(onClick = onAddCustomCategory),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Plus, contentDescription = "Add Category", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // ── 6. MENTION USERS (MAX 5) ──────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (colors.isDark) Color(0xFF1F2937) else Color.White,
                border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Mention Collaborators", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Tag featured creators (e.g. @username)", color = colors.textMuted, fontSize = 11.sp)
                        }
                        Text("${mentionedUsers.size} / 5 Max", color = Color(0xFF2563EB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (mentionedUsers.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            mentionedUsers.forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (colors.isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF))
                                        .border(1.dp, if (colors.isDark) Color(0xFF2563EB).copy(alpha = 0.5f) else Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Text("@${user.username}", color = if (colors.isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Icon(
                                        imageVector = Lucide.X,
                                        contentDescription = "Remove",
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onRemoveMention(user) }
                                    )
                                }
                            }
                        }
                    }

                    // Search Trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (colors.isDark) Color(0xFF111827) else Color(0xFFF8FAFC))
                            .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenMentionSearch)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Search users to mention...", color = colors.textMuted, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // ── 7. ADVANCED POST & VIDEO SETTINGS ─────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (colors.isDark) Color(0xFF1F2937) else Color.White,
                border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Advanced Post Settings", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                    // Option 1: Hide Like Count
                    SettingToggleRow(
                        title = "Hide Like Count",
                        subtitle = "Only you will see total likes on this post",
                        checked = hideLikesCount,
                        onCheckedChange = onToggleHideLikes
                    )

                    HorizontalDivider(color = if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))

                    // Option 2: Turn Off Comments
                    SettingToggleRow(
                        title = "Turn Off Comments",
                        subtitle = "No one will be able to comment on this post",
                        checked = turnOffComments,
                        onCheckedChange = onToggleTurnOffComments
                    )

                    HorizontalDivider(color = if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))

                    // Option 3: Turn Off Remix
                    SettingToggleRow(
                        title = "Turn Off Remix",
                        subtitle = "Others won't be able to remix this content",
                        checked = turnOffRemix,
                        onCheckedChange = onToggleTurnOffRemix
                    )

                    if (isVideo) {
                        HorizontalDivider(color = if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))

                        // Option 4: Allow Video Downloads
                        SettingToggleRow(
                            title = "Allow Video Downloads",
                            subtitle = "Viewers can save and download your video",
                            checked = allowDownloads,
                            onCheckedChange = onToggleAllowDownloads
                        )

                        HorizontalDivider(color = if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF1F5F9))

                        // Option 5: High-Quality Upload
                        SettingToggleRow(
                            title = "High-Quality Upload",
                            subtitle = "Preserve maximum 1080p bitrate and motion clarity",
                            checked = highQualityUpload,
                            onCheckedChange = onToggleHighQualityUpload
                        )
                    }
                }
            }

            // ── 8. ACTION BUTTONS ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveAsDraft,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textMain)
                ) {
                    Icon(Lucide.FileText, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Draft", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onPost,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(if (isVideo) Lucide.Video else Lucide.Share2, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isVideo) "Publish Video" else "Share Post", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = colors.textMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (colors.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1)
            )
        )
    }
}

// ============================================================================
// STEP 3: BACKGROUND UPLOADING OVERLAY STAGE
// ============================================================================

@Composable
fun Step3UploadingOverlayStage(
    studioMode: CreatorStudioMode,
    slides: List<StudioSlide>,
    currentSlideIndex: Int,
    selectedRatioId: String,
    uploadProgress: Float,
    uploadStageText: String,
    isUploadFinished: Boolean,
    errorMessage: String?,
    onExitScreen: () -> Unit,
    onExitApp: () -> Unit,
    onCreateAnotherPost: () -> Unit
) {
    val currentSlide = slides.getOrNull(currentSlideIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Tag
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(0.dp, Color.Transparent))
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = when (studioMode) {
                    CreatorStudioMode.POST -> "Uploading Post"
                    CreatorStudioMode.FLASH -> "Uploading Flash"
                    CreatorStudioMode.WORK -> "Uploading Work"
                },
                color = Color(0xFF2563EB),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            // Thumbnail with checkmark
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                if (currentSlide != null) {
                    SlidePreviewContent(slide = currentSlide, modifier = Modifier.fillMaxSize())
                }

                if (isUploadFinished) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2563EB).copy(alpha = 0.88f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isUploadFinished) "Post Uploaded!" else "Uploading Your Post...",
                    color = Color(0xFF0F172A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Aspect Ratio: $selectedRatioId", color = Color(0xFF64748B), fontSize = 12.sp)
            }

            // Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val animatedProgress by animateFloatAsState(targetValue = uploadProgress, label = "progress")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(Color(0xFF2563EB))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(uploadStageText, color = Color(0xFF475569), fontSize = 12.sp)
                    Text("${(animatedProgress * 100).toInt()}%", color = Color(0xFF2563EB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF2F2))
                        .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(errorMessage, color = Color(0xFFDC2626), fontSize = 12.sp)
                }
            }

            // Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Your post is uploading in the background. You can exit this screen or exit the app now.",
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bottom Exit / Create Another Buttons
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isUploadFinished) {
                Button(
                    onClick = onCreateAnotherPost,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Create Another Post", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onExitScreen,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text("Exit Screen", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onExitApp,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Exit App", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

// ============================================================================
// MODAL 1: THUMBNAIL PREVIEW MODAL
// ============================================================================

@Composable
fun ThumbnailPreviewModal(
    studioMode: CreatorStudioMode,
    slides: List<StudioSlide>,
    selectedRatioId: String,
    selectedSlideIndex: Int,
    cropOffsetPercent: Float,
    customCoverUri: Uri?,
    selectedCoverFrameMs: Long,
    videoDurationMs: Long,
    onCropOffsetChange: (Float) -> Unit,
    onSelectSlideIndex: (Int) -> Unit,
    onSelectCoverFrameMs: (Long) -> Unit,
    onUploadCustomCover: () -> Unit,
    onClearCustomCover: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val activeSlide = slides.getOrNull(selectedSlideIndex)
    val isVideo = studioMode == CreatorStudioMode.FLASH || activeSlide?.videoUri != null
    val activeVideoUri = slides.firstOrNull { it.videoUri != null }?.videoUri

    var scrubberMs by remember(selectedCoverFrameMs) {
        mutableFloatStateOf(selectedCoverFrameMs.toFloat())
    }
    var scrubbedFrameBmp by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(scrubberMs, activeVideoUri) {
        if (activeVideoUri == null) return@LaunchedEffect
        delay(50)
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, activeVideoUri)
                val timeUs = (scrubberMs * 1000f).toLong()
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 480, 480)
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                retriever.release()
                if (bmp != null) scrubbedFrameBmp = bmp
            } catch (_: Throwable) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(if (colors.isDark) Color(0xFF111827) else Color.White)
                    .border(BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Lucide.X, contentDescription = "Close", tint = colors.textMuted, modifier = Modifier.size(20.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isVideo) "Video Cover & Thumbnail" else "Profile Thumbnail Preview", color = colors.textMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Visible in feed & profile grid", color = Color(0xFF2563EB), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Done", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info note
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (colors.isDark) Color(0xFF1F2937) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = "Your post uses $selectedRatioId in feeds. Select the poster frame or upload a custom image. Adjust the 1:1 square crop for your profile grid below.",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // 1:1 Profile Crop Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (colors.isDark) Color(0xFF1F2937) else Color.White,
                    border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1:1 Profile Grid Preview", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Crop position: ${cropOffsetPercent.toInt()}%", color = Color(0xFF2563EB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, Color(0xFF2563EB), RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customCoverUri != null) {
                                AsyncImage(
                                    model = customCoverUri,
                                    contentDescription = "Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (scrubbedFrameBmp != null) {
                                Image(
                                    bitmap = scrubbedFrameBmp!!.asImageBitmap(),
                                    contentDescription = "Cover Frame",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (activeSlide != null) {
                                SlidePreviewContent(slide = activeSlide, modifier = Modifier.fillMaxSize())
                            }

                            ThirdsGridOverlay(modifier = Modifier.fillMaxSize())
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Adjust visible 1:1 crop offset", color = colors.textMuted, fontSize = 11.sp)
                            Slider(
                                value = cropOffsetPercent,
                                onValueChange = onCropOffsetChange,
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF2563EB),
                                    activeTrackColor = Color(0xFF2563EB)
                                )
                            )
                        }
                    }
                }

                // Video Frame Scrubber (When Video)
                if (isVideo && activeVideoUri != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (colors.isDark) Color(0xFF1F2937) else Color.White,
                        border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Icon(Lucide.Film, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                    Text("Select Frame from Video", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Text(
                                    text = "${formatDurationTime(scrubberMs.toLong())} / ${formatDurationTime(videoDurationMs)}",
                                    color = Color(0xFF2563EB),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Slider(
                                value = scrubberMs,
                                onValueChange = {
                                    scrubberMs = it
                                    onSelectCoverFrameMs(it.toLong())
                                },
                                valueRange = 0f..videoDurationMs.toFloat().coerceAtLeast(1000f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF2563EB),
                                    activeTrackColor = Color(0xFF2563EB)
                                )
                            )

                            Button(
                                onClick = {
                                    onSelectCoverFrameMs(scrubberMs.toLong())
                                    onClearCustomCover()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (customCoverUri == null) Color(0xFF2563EB) else (if (colors.isDark) Color(0xFF111827) else Color(0xFFF1F5F9)),
                                    contentColor = if (customCoverUri == null) Color.White else colors.textMain
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Lucide.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Use This Frame as Cover", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Custom Cover Photo
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (colors.isDark) Color(0xFF1F2937) else Color.White,
                    border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Upload Custom Cover Image", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Use any photo from your gallery", color = colors.textMuted, fontSize = 11.sp)
                            }

                            Button(
                                onClick = onUploadCustomCover,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Lucide.Upload, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Upload", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (customCoverUri != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Lucide.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Text("Custom Photo Cover Active", color = Color(0xFF047857), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }

                                Text(
                                    text = "Reset to Video",
                                    color = Color(0xFFEF4444),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { onClearCustomCover() }
                                )
                            }
                        }
                    }
                }

                // Slide Selector for Multi-Slide Photos
                if (!isVideo && slides.size > 1) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (colors.isDark) Color(0xFF1F2937) else Color.White,
                        border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Choose Slide for Thumbnail", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(slides.indices.toList()) { index ->
                                    val slide = slides[index]
                                    val isSelected = index == selectedSlideIndex
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF2563EB) else (if (colors.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFCBD5E1)),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onSelectSlideIndex(index) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SlidePreviewContent(slide = slide, modifier = Modifier.fillMaxSize())
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
// MODAL 2: MENTION USER SEARCH MODAL (MAX 5)
// ============================================================================

@Composable
fun MentionUserSearchModal(
    discoverRepository: DiscoverRepository,
    selectedUsers: List<UserProfile>,
    onToggleUser: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var suggestedUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        isSearching = true
        try {
            suggestedUsers = if (searchQuery.isBlank()) {
                discoverRepository.getSuggestedCreators(15)
            } else {
                discoverRepository.searchUsers(searchQuery)
            }
        } catch (_: Exception) {
            suggestedUsers = emptyList()
        } finally {
            isSearching = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFE2E8F0)))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Lucide.ArrowLeft, contentDescription = "Back", tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mention Users", color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Select up to 5 users", color = Color(0xFF64748B), fontSize = 11.sp)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Done", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Search input & selected pill row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .border(BorderStroke(1.dp, Color(0xFFE2E8F0)))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Input with Lucide Search & Clear
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Lucide.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 13.sp),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search users (e.g. @thejoykumbhakar)...", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Clear",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Selected users:", color = Color(0xFF64748B), fontSize = 12.sp)
                    Text("${selectedUsers.size} / 5 selected", color = Color(0xFF2563EB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (selectedUsers.size >= 5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF6FF))
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Lucide.Info, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                        Text("Maximum of 5 users reached. Tap Done to continue.", color = Color(0xFF1E40AF), fontSize = 11.sp)
                    }
                }

                // Selected User Pills
                if (selectedUsers.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        selectedUsers.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("@${user.username}", color = Color(0xFF1D4ED8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = "Remove",
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { onToggleUser(user) }
                                )
                            }
                        }
                    }
                }
            }

            // User Directory List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(suggestedUsers) { user ->
                    val isSelected = selectedUsers.any { it.uid == user.uid }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleUser(user) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Column {
                                Text(user.displayName.ifBlank { user.username }, color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("@${user.username}", color = Color(0xFF64748B), fontSize = 12.sp)
                                if (user.bio.isNotBlank()) {
                                    Text(user.bio, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(
                                    1.5.dp,
                                    if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                    CircleShape
                                )
                                .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}
