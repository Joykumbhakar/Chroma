package com.chroma.studio.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint as AndroidPaint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.StoryRepository
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.ChromaWork
import com.chroma.studio.model.FlashMediaType
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.AppleSwitch
import com.chroma.studio.ui.components.OneUiIconBadge
import com.chroma.studio.ui.components.OneUiSettingsGroup
import com.chroma.studio.ui.components.OscellionGalleryLayout
import com.chroma.studio.ui.components.GalleryMediaTypeFilter
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.utils.VideoProcessor
import com.chroma.studio.utils.compressImageToUnder50KB
import com.chroma.studio.viewmodel.FlashViewModel
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Universal Post Types for Chroma Studio Creator System.
 */
enum class PostType(val id: String, val title: String, val subtitle: String, val icon: ImageVector) {
    PHOTO("photo", "Post", "Multi-Image & Carousel", Lucide.Image),
    FLASH("flash", "Flash", "Short-Form Video & Flashes", Lucide.Film),
    STORY("story", "Story", "Ephemeral 24h Status", Lucide.CirclePlus),
    WORK("work", "Work", "Portfolio Showcase & Masterwork", Lucide.Palette)
}

/**
 * 3-Stage Universal Post Creation Flow
 * Following Samsung One UI 7.0 design principles.
 */
enum class PostCreationStep {
    MEDIA_PICKER,   // Stage 1: Select media source / project
    IMAGE_EDITOR,   // Stage 2: Dedicated Apple/One UI Editor & Studio Preview
    POST_DETAILS    // Stage 3: Title/Caption, metadata, tags, audience, permissions & publishing
}

/**
 * Quick Filter Presets
 */
data class QuickFilterPreset(
    val id: String,
    val name: String,
    val description: String,
    val matrix: FloatArray
)

private val QUICK_FILTER_PRESETS = listOf(
    QuickFilterPreset(
        id = "normal",
        name = "Normal",
        description = "Original natural look",
        matrix = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    QuickFilterPreset(
        id = "vivid",
        name = "Vivid",
        description = "Enhanced saturation & punchy vibrance",
        matrix = floatArrayOf(
            1.18f, 0f, 0f, 0f, 5f,
            0f, 1.18f, 0f, 0f, 5f,
            0f, 0f, 1.18f, 0f, 5f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    QuickFilterPreset(
        id = "film_35mm",
        name = "35mm Film",
        description = "Warm analog golden tones with soft blacks",
        matrix = floatArrayOf(
            1.15f, 0.05f, 0.02f, 0f, 12f,
            0.02f, 1.05f, 0.03f, 0f, 6f,
            0.01f, 0.04f, 0.95f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    QuickFilterPreset(
        id = "clarendon",
        name = "Clarendon",
        description = "Cool shadows with intense warm highlights",
        matrix = floatArrayOf(
            1.20f, 0f, 0f, 0f, 4f,
            0f, 1.15f, 0f, 0f, 2f,
            0f, 0f, 1.30f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    QuickFilterPreset(
        id = "mono_noir",
        name = "Mono Noir",
        description = "Deep blacks, studio contrast monochrome",
        matrix = floatArrayOf(
            0.30f, 0.59f, 0.11f, 0f, -5f,
            0.30f, 0.59f, 0.11f, 0f, -5f,
            0.30f, 0.59f, 0.11f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    QuickFilterPreset(
        id = "cyber_pop",
        name = "Cyber Pop",
        description = "Punchy high-fashion saturated tones",
        matrix = floatArrayOf(
            1.28f, -0.05f, 0.05f, 0f, 10f,
            -0.05f, 1.25f, -0.05f, 0f, 8f,
            0.05f, -0.05f, 1.30f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostCreationFlowScreen(
    initialPostType: PostType = PostType.PHOTO,
    initialMediaUri: Uri? = null,
    initialPhotoUri: Uri? = initialMediaUri,
    initialMediaUris: List<Uri> = emptyList(),
    onFinish: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val scope = rememberCoroutineScope()
    val discoverRepo = remember { DiscoverRepository() }
    val workRepo = remember { WorkRepository(context) }
    val storyVm: StoryViewModel = viewModel()
    val flashVm: FlashViewModel = viewModel()
    val flashIsUploading by flashVm.isUploading.collectAsState()
    val flashUploadProgress by flashVm.uploadProgress.collectAsState()

    // Active Post Type
    var currentPostType by remember { mutableStateOf(initialPostType) }

    val effectiveInitialUri = initialMediaUri ?: initialPhotoUri
    val effectiveInitialUris = if (initialMediaUris.isNotEmpty()) initialMediaUris else listOfNotNull(effectiveInitialUri)

    // Navigation Step
    var currentStep by remember {
        mutableStateOf(
            if (effectiveInitialUris.isNotEmpty()) PostCreationStep.IMAGE_EDITOR
            else PostCreationStep.MEDIA_PICKER
        )
    }

    // Media & Editing States
    var selectedPhotoUris by remember {
        mutableStateOf<List<Uri>>(effectiveInitialUris)
    }
    var currentPhotoIndex by remember { mutableIntStateOf(0) }
    var editedPhotoUris by remember { mutableStateOf<Map<Uri, Uri>>(emptyMap()) }
    val rawPhotoUri: Uri? by remember {
        derivedStateOf { selectedPhotoUris.getOrNull(currentPhotoIndex) ?: selectedPhotoUris.firstOrNull() }
    }
    val editedPhotoUri: Uri? by remember {
        derivedStateOf { rawPhotoUri?.let { editedPhotoUris[it] } }
    }

    // Work-specific Project selection state
    var selectedLocalWork by remember { mutableStateOf<ChromaWork?>(null) }
    var userSavedWorks by remember { mutableStateOf<List<ChromaWork>>(emptyList()) }
    LaunchedEffect(currentPostType) {
        if (currentPostType == PostType.WORK) {
            withContext(Dispatchers.IO) {
                try {
                    userSavedWorks = workRepo.loadActiveWorks()
                } catch (_: Exception) {}
            }
        }
    }

    // Story-specific states
    var useGradientCanvas by remember { mutableStateOf(false) }
    var storyGradientIndex by remember { mutableIntStateOf(0) }
    var storyHeadline by remember { mutableStateOf("") }
    var storyBody by remember { mutableStateOf("") }

    // Flash-specific states
    var flashTrimStartMs by remember { mutableLongStateOf(0L) }
    var flashTrimEndMs by remember { mutableLongStateOf(60000L) }
    var audioTrackName by remember { mutableStateOf("Original Audio") }

    // Aspect Ratio & Filters
    var selectedAspectRatio by remember {
        mutableStateOf(
            when (currentPostType) {
                PostType.PHOTO -> "4:5"
                PostType.FLASH -> "9:16"
                PostType.WORK -> "4:3"
                PostType.STORY -> "9:16"
            }
        )
    }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var brightnessOffset by remember { mutableFloatStateOf(0f) }
    var contrastScale by remember { mutableFloatStateOf(1f) }

    // Work category & details
    var workCategory by remember { mutableStateOf("Digital Art") }
    var workToolsUsed by remember { mutableStateOf(listOf("Chroma Studio")) }
    var allowDownload by remember { mutableStateOf(true) }

    // Post Details Input States
    var caption by remember { mutableStateOf("") }
    var tags by remember {
        mutableStateOf(
            when (currentPostType) {
                PostType.PHOTO -> listOf("art", "creative", "photo")
                PostType.FLASH -> listOf("flash", "flashes", "motion")
                PostType.WORK -> listOf("design", "branding", "artwork")
                PostType.STORY -> listOf("moment", "daily")
            }
        )
    }
    var tagInput by remember { mutableStateOf("") }
    var mentions by remember { mutableStateOf(emptyList<String>()) }
    var mentionInput by remember { mutableStateOf("") }
    var invitedCollaborators by remember { mutableStateOf(emptyList<com.chroma.studio.model.CollabParticipant>()) }
    var showCollabPickerSheet by remember { mutableStateOf(false) }
    var selectedMusic by remember {
        mutableStateOf<com.chroma.studio.data.AttachedMusicSelection?>(com.chroma.studio.data.PendingMusicHandoff.peek())
    }
    var showMusicPickerSheet by remember { mutableStateOf(false) }
    var locationInput by remember { mutableStateOf("") }
    var selectedAudience by remember { mutableStateOf("Public") } // Public, Followers, Only Friends
    var allowRemix by remember { mutableStateOf(true) }
    var highQualityUpload by remember { mutableStateOf(true) }

    // Creator mention suggestions
    var suggestedCreators by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    LaunchedEffect(mentionInput) {
        val q = mentionInput.trim().removePrefix("@").lowercase()
        if (q.length >= 2) {
            try {
                val creators = discoverRepo.getSuggestedCreators(8)
                suggestedCreators = creators.filter {
                    it.username.lowercase().contains(q) || it.displayName.lowercase().contains(q)
                }
            } catch (_: Exception) {
                suggestedCreators = emptyList()
            }
        } else {
            suggestedCreators = emptyList()
        }
    }

    // Publishing state
    var isPublishing by remember { mutableStateOf(false) }

    // Combined active matrix for Quick Filter
    val activeColorMatrix = remember(selectedFilterIndex, brightnessOffset, contrastScale) {
        val base = QUICK_FILTER_PRESETS[selectedFilterIndex].matrix.copyOf()
        base[4] += brightnessOffset
        base[9] += brightnessOffset
        base[14] += brightnessOffset
        base[0] *= contrastScale
        base[6] *= contrastScale
        base[12] *= contrastScale
        ColorMatrix(base)
    }

    // Share / Publish Post
    fun executePublish() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(context, "Please log in to share posts", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentPostType != PostType.STORY || !useGradientCanvas) {
            if (selectedPhotoUris.isEmpty() && selectedLocalWork == null) {
                Toast.makeText(context, "Please select media first", Toast.LENGTH_SHORT).show()
                return
            }
        }

        isPublishing = true
        scope.launch {
            try {
                when (currentPostType) {
                    PostType.PHOTO -> {
                        val success = withContext(Dispatchers.IO) {
                            if (selectedPhotoUris.size > 1) {
                                val bytesList = selectedPhotoUris.mapNotNull { rawUri ->
                                    val finalUri = editedPhotoUris[rawUri] ?: rawUri
                                    com.chroma.studio.utils.ImageUtils.compressPostImage(context, finalUri)
                                }
                                if (bytesList.isEmpty()) throw Exception("Failed to process images")
                                discoverRepo.uploadMultiImagePost(
                                    imageBytesList = bytesList,
                                    caption = caption.ifBlank { "Untitled creation" },
                                    description = if (locationInput.isNotBlank()) "📍 $locationInput" else "",
                                    aspectRatio = selectedAspectRatio,
                                    tags = tags,
                                    mentions = mentions,
                                    collaborators = invitedCollaborators,
                                    musicTrack = selectedMusic
                                )
                            } else {
                                val singleRaw = selectedPhotoUris.first()
                                val finalToUploadUri = editedPhotoUris[singleRaw] ?: singleRaw
                                val imageBytes = com.chroma.studio.utils.ImageUtils.compressPostImage(context, finalToUploadUri)
                                    ?: throw Exception("Failed to process image")
                                discoverRepo.uploadImagePost(
                                    imageBytes = imageBytes,
                                    caption = caption.ifBlank { "Untitled creation" },
                                    description = if (locationInput.isNotBlank()) "📍 $locationInput" else "",
                                    aspectRatio = selectedAspectRatio,
                                    tags = tags,
                                    mentions = mentions,
                                    collaborators = invitedCollaborators,
                                    musicTrack = selectedMusic
                                )
                            }
                        }
                        if (success) {
                            com.chroma.studio.data.PendingMusicHandoff.consume()
                            SoundManager.playSend()
                            Toast.makeText(context, "Photo post shared successfully! ✨", Toast.LENGTH_SHORT).show()
                            onFinish()
                        } else {
                            Toast.makeText(context, "Upload failed. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    PostType.FLASH -> {
                        val videoUri = editedPhotoUri ?: selectedPhotoUris.firstOrNull() ?: throw Exception("No video selected")
                        val finalCaption = caption.ifBlank { "Untitled Flash" }

                        val thumbBitmap = try {
                            VideoProcessor.extractThumbnail(context, videoUri, 100L)
                        } catch (_: Throwable) { null }

                        com.chroma.studio.data.FlashUploadManager.startUpload(
                            thumbnailBitmap = thumbBitmap,
                            title = finalCaption,
                            statusText = "Posting Flash to Community…",
                            draftId = null,
                            retryAction = { executePublish() }
                        )

                        flashVm.publishFlash(
                            context = context,
                            mediaUri = videoUri,
                            mediaType = FlashMediaType.VIDEO,
                            trimStartMs = flashTrimStartMs,
                            trimEndMs = flashTrimEndMs,
                            thumbnailBitmap = thumbBitmap,
                            caption = finalCaption,
                            hashtags = tags,
                            mentions = mentions,
                            filterName = "Normal",
                            collaborators = invitedCollaborators,
                            musicTrack = selectedMusic,
                            onSuccess = {
                                com.chroma.studio.data.PendingMusicHandoff.consume()
                                isPublishing = false
                                SoundManager.playSend()
                                Toast.makeText(context, "Flash published successfully! ⚡", Toast.LENGTH_SHORT).show()
                                onFinish()
                            },
                            onError = { err ->
                                isPublishing = false
                                Toast.makeText(context, "Flash post error: $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                        return@launch
                    }

                    PostType.WORK -> {
                        val success = withContext(Dispatchers.IO) {
                            val localWork = selectedLocalWork
                            if (localWork != null) {
                                val userProfile = discoverRepo.getUserProfile(user.uid)
                                val shared = discoverRepo.shareWork(
                                    work = localWork,
                                    authorName = userProfile?.displayName ?: (user.displayName ?: "Creator"),
                                    authorUsername = userProfile?.username ?: "creator",
                                    creatorId = user.uid,
                                    authorAvatarUrl = userProfile?.avatarUrl ?: "",
                                    authorVerified = userProfile?.verified ?: false,
                                    aspectRatio = selectedAspectRatio,
                                    collaborators = invitedCollaborators,
                                    musicTrack = selectedMusic
                                )
                                shared
                            } else {
                                val singleRaw = selectedPhotoUris.first()
                                val finalToUploadUri = editedPhotoUris[singleRaw] ?: singleRaw
                                val imageBytes = com.chroma.studio.utils.ImageUtils.compressPostImage(context, finalToUploadUri)
                                    ?: throw Exception("Failed to process artwork image")
                                val descWithTools = buildString {
                                    if (caption.isNotBlank()) append(caption).append("\n\n")
                                    append("Category: ").append(workCategory).append("\n")
                                    if (workToolsUsed.isNotEmpty()) append("Tools: ").append(workToolsUsed.joinToString(", ")).append("\n")
                                    if (locationInput.isNotBlank()) append("📍 ").append(locationInput)
                                }
                                discoverRepo.uploadImagePost(
                                    imageBytes = imageBytes,
                                    caption = caption.ifBlank { "New Masterwork" },
                                    description = descWithTools,
                                    aspectRatio = selectedAspectRatio,
                                    tags = tags + listOf(workCategory.lowercase().replace(" ", "")),
                                    mentions = mentions,
                                    collaborators = invitedCollaborators,
                                    musicTrack = selectedMusic
                                )
                            }
                        }
                        if (success) {
                            com.chroma.studio.data.PendingMusicHandoff.consume()
                            SoundManager.playSend()
                            Toast.makeText(context, "Creative Work published to Portfolio! 🎨", Toast.LENGTH_SHORT).show()
                            onFinish()
                        } else {
                            Toast.makeText(context, "Failed to publish work.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    PostType.STORY -> {
                        val targetAudience = if (selectedAudience == "Only Friends") "only_friends" else "public"
                        if (useGradientCanvas || selectedPhotoUris.isEmpty()) {
                            val currentTheme = StoryGradientThemes.getOrElse(storyGradientIndex) { StoryGradientThemes.first() }
                            val bgHex = String.format("#%06X", (0xFFFFFF and currentTheme.colors.first().hashCode()))
                            storyVm.publishTextStory(
                                textContent = if (storyHeadline.isNotBlank()) (storyHeadline + "\n" + storyBody) else caption.ifBlank { "Story Moment" },
                                textFormat = "story_v1",
                                backgroundColor = bgHex,
                                audienceType = targetAudience,
                                customAudienceIds = emptyList(),
                                musicTrack = selectedMusic
                            )
                        } else {
                            val storyUri = selectedPhotoUris.first()
                            storyVm.uploadStory(
                                context = context,
                                uri = storyUri,
                                audienceType = targetAudience,
                                textContent = caption,
                                musicTrack = selectedMusic
                            )
                        }
                        com.chroma.studio.data.PendingMusicHandoff.consume()
                        SoundManager.playSend()
                        Toast.makeText(context, "Story published! 🌟 (Expires in 24h)", Toast.LENGTH_SHORT).show()
                        onFinish()
                    }
                }
            } catch (e: Exception) {
                isPublishing = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                if (currentPostType != PostType.FLASH) {
                    isPublishing = false
                }
            }
        }
    }

    // Back handling
    BackHandler {
        when (currentStep) {
            PostCreationStep.POST_DETAILS -> currentStep = PostCreationStep.IMAGE_EDITOR
            PostCreationStep.IMAGE_EDITOR -> {
                if (effectiveInitialUri != null) onClose()
                else currentStep = PostCreationStep.MEDIA_PICKER
            }
            PostCreationStep.MEDIA_PICKER -> onClose()
        }
    }

    // Stage 2: Render Tailored Editor depending on PostType
    if (currentStep == PostCreationStep.IMAGE_EDITOR) {
        when (currentPostType) {
            PostType.PHOTO -> {
                if (rawPhotoUri != null || selectedPhotoUris.isNotEmpty()) {
                    val allUris = if (selectedPhotoUris.isNotEmpty()) selectedPhotoUris else listOfNotNull(rawPhotoUri)
                    val targetUri = editedPhotoUri ?: allUris.getOrElse(currentPhotoIndex) { allUris.first() }
                    ChromaPhotoEditorScreen(
                        photoUri = targetUri,
                        photoUris = allUris,
                        initialIndex = currentPhotoIndex,
                        exportButtonLabel = "Next",
                        onExportSuccess = { proEditedUri ->
                            val activeRaw = allUris.getOrElse(currentPhotoIndex) { allUris.first() }
                            editedPhotoUris = editedPhotoUris + (activeRaw to proEditedUri)
                            SoundManager.playReaction()
                            currentStep = PostCreationStep.POST_DETAILS
                        },
                        onExportAllSuccess = { allEditedUris ->
                            val updatedMap = editedPhotoUris.toMutableMap()
                            allUris.forEachIndexed { idx, raw ->
                                if (idx < allEditedUris.size) {
                                    updatedMap[raw] = allEditedUris[idx]
                                }
                            }
                            editedPhotoUris = updatedMap
                            SoundManager.playReaction()
                            currentStep = PostCreationStep.POST_DETAILS
                        },
                        onBack = {
                            if (effectiveInitialUri != null) onClose()
                            else currentStep = PostCreationStep.MEDIA_PICKER
                        }
                    )
                    return
                }
            }

            PostType.FLASH -> {
                if (rawPhotoUri != null || selectedPhotoUris.isNotEmpty()) {
                    val activeOriginalRaw = rawPhotoUri ?: selectedPhotoUris.first()
                    FinalCutProVideoEditorScreen(
                        initialUri = activeOriginalRaw,
                        initialUris = selectedPhotoUris,
                        onDone = { startMs, endMs, clips ->
                            val renderedUri = clips.firstOrNull() ?: activeOriginalRaw
                            selectedPhotoUris = clips.ifEmpty { listOf(renderedUri) }
                            editedPhotoUris = editedPhotoUris + (activeOriginalRaw to renderedUri)
                            flashTrimStartMs = 0L
                            flashTrimEndMs = endMs
                            SoundManager.playReaction()
                            currentStep = PostCreationStep.POST_DETAILS
                        },
                        onMusicAttached = { attached ->
                            selectedMusic = attached
                            if (attached != null) {
                                audioTrackName = "${attached.title} · ${attached.artist}"
                            }
                        },
                        onBack = {
                            if (effectiveInitialUris.isNotEmpty()) onClose()
                            else currentStep = PostCreationStep.MEDIA_PICKER
                        }
                    )
                    return
                }
            }

            PostType.WORK -> {
                WorkStudioPresentationView(
                    coverUri = editedPhotoUri ?: rawPhotoUri,
                    selectedWork = selectedLocalWork,
                    aspectRatio = selectedAspectRatio,
                    onAspectRatioChange = { selectedAspectRatio = it },
                    category = workCategory,
                    onCategoryChange = { workCategory = it },
                    tools = workToolsUsed,
                    onToggleTool = { tool ->
                        workToolsUsed = if (workToolsUsed.contains(tool)) workToolsUsed - tool else workToolsUsed + tool
                        SoundManager.playReaction()
                    },
                    onNext = {
                        SoundManager.playReaction()
                        currentStep = PostCreationStep.POST_DETAILS
                    },
                    onBack = {
                        if (effectiveInitialUri != null) onClose()
                        else currentStep = PostCreationStep.MEDIA_PICKER
                    }
                )
                return
            }

            PostType.STORY -> {
                StoryStudioStylingView(
                    backgroundUri = editedPhotoUri ?: rawPhotoUri,
                    useGradientCanvas = useGradientCanvas,
                    selectedThemeIndex = storyGradientIndex,
                    onThemeIndexChange = { storyGradientIndex = it },
                    headline = storyHeadline,
                    onHeadlineChange = { storyHeadline = it },
                    body = storyBody,
                    onBodyChange = { storyBody = it },
                    onNext = {
                        SoundManager.playReaction()
                        currentStep = PostCreationStep.POST_DETAILS
                    },
                    onBack = {
                        if (effectiveInitialUri != null) onClose()
                        else currentStep = PostCreationStep.MEDIA_PICKER
                    }
                )
                return
            }
        }
    }

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            OneUiPostCreationTopBar(
                postType = currentPostType,
                currentStep = currentStep,
                onBack = {
                    when (currentStep) {
                        PostCreationStep.POST_DETAILS -> currentStep = PostCreationStep.IMAGE_EDITOR
                        PostCreationStep.IMAGE_EDITOR -> {
                            if (effectiveInitialUri != null) onClose()
                            else currentStep = PostCreationStep.MEDIA_PICKER
                        }
                        PostCreationStep.MEDIA_PICKER -> onClose()
                    }
                },
                onNext = {
                    when (currentStep) {
                        PostCreationStep.MEDIA_PICKER -> {
                            if (selectedPhotoUris.isNotEmpty() || selectedLocalWork != null || useGradientCanvas) {
                                SoundManager.playReaction()
                                currentStep = PostCreationStep.IMAGE_EDITOR
                            }
                        }
                        PostCreationStep.IMAGE_EDITOR -> {
                            currentStep = PostCreationStep.POST_DETAILS
                        }
                        PostCreationStep.POST_DETAILS -> {
                            executePublish()
                        }
                    }
                },
                isNextEnabled = when (currentStep) {
                    PostCreationStep.MEDIA_PICKER -> selectedPhotoUris.isNotEmpty() || selectedLocalWork != null || useGradientCanvas
                    PostCreationStep.IMAGE_EDITOR -> true
                    PostCreationStep.POST_DETAILS -> !isPublishing && !(currentPostType == PostType.FLASH && flashIsUploading)
                },
                isPublishing = isPublishing || (currentPostType == PostType.FLASH && flashIsUploading)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentStep) {
                PostCreationStep.MEDIA_PICKER -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. One UI 7 Pill Segmented Tab Bar for switching Post Types
                        PostTypeSegmentedBar(
                            selectedType = currentPostType,
                            onSelectType = { newType ->
                                if (currentPostType != newType) {
                                    SoundManager.playReaction()
                                    currentPostType = newType
                                    selectedPhotoUris = emptyList()
                                    selectedLocalWork = null
                                    useGradientCanvas = false
                                }
                            }
                        )

                        // 2. Extra quick actions for WORK or STORY
                        if (currentPostType == PostType.WORK && userSavedWorks.isNotEmpty()) {
                            WorkSavedProjectsQuickStrip(
                                works = userSavedWorks,
                                selectedWork = selectedLocalWork,
                                onSelectWork = { w ->
                                    selectedLocalWork = w
                                    caption = w.name
                                    SoundManager.playReaction()
                                    currentStep = PostCreationStep.IMAGE_EDITOR
                                }
                            )
                        }

                        if (currentPostType == PostType.STORY) {
                            StoryGradientPromptCard(
                                onSelectGradient = {
                                    useGradientCanvas = true
                                    SoundManager.playReaction()
                                    currentStep = PostCreationStep.IMAGE_EDITOR
                                }
                            )
                        }

                        // 3. Media Picker Gallery with filter tailored to PostType
                        val mediaFilter = when (currentPostType) {
                            PostType.PHOTO -> GalleryMediaTypeFilter.PHOTOS
                            PostType.FLASH -> GalleryMediaTypeFilter.ALL
                            PostType.WORK -> GalleryMediaTypeFilter.PHOTOS
                            PostType.STORY -> GalleryMediaTypeFilter.ALL
                        }

                        val maxMediaCount = when (currentPostType) {
                            PostType.PHOTO -> 10
                            PostType.FLASH -> 20
                            PostType.WORK -> 4
                            PostType.STORY -> 1
                        }

                        OscellionGalleryLayout(
                            title = currentPostType.title,
                            badgeText = if (currentPostType == PostType.FLASH) "Multi-Clip Video" else "",
                            maxSelection = maxMediaCount,
                            allowedMediaFilter = mediaFilter,
                            showCameraTile = true,
                            showAlbumsTab = false,
                            showHeader = false,
                            onSelectionChanged = { uris ->
                                selectedPhotoUris = uris
                                if (uris.isNotEmpty()) {
                                    currentPhotoIndex = 0
                                    editedPhotoUris = emptyMap()
                                    useGradientCanvas = false
                                }
                            },
                            onPhotosSelected = { uris ->
                                if (uris.isNotEmpty()) {
                                    selectedPhotoUris = uris
                                    currentPhotoIndex = 0
                                    editedPhotoUris = emptyMap()
                                    useGradientCanvas = false
                                    SoundManager.playReaction()
                                    currentStep = PostCreationStep.IMAGE_EDITOR
                                }
                            },
                            onClose = onClose
                        )
                    }
                }

                PostCreationStep.IMAGE_EDITOR -> {
                    // Handled above before Scaffold
                }

                PostCreationStep.POST_DETAILS -> {
                    // STAGE 3: POST INFORMATION INPUTS & PUBLISHING
                    UniversalPostDetailsInputView(
                        postType = currentPostType,
                        previewUri = editedPhotoUri ?: rawPhotoUri,
                        selectedPhotoUris = selectedPhotoUris,
                        selectedWork = selectedLocalWork,
                        useGradientCanvas = useGradientCanvas,
                        storyGradientIndex = storyGradientIndex,
                        storyHeadline = storyHeadline,
                        storyBody = storyBody,
                        colorMatrix = if (editedPhotoUri == null) activeColorMatrix else null,
                        aspectRatio = selectedAspectRatio,
                        caption = caption,
                        onCaptionChange = { caption = it },
                        tags = tags,
                        onAddTag = { tag ->
                            val clean = tag.trim().removePrefix("#").lowercase()
                            if (clean.isNotBlank() && !tags.contains(clean)) {
                                tags = tags + clean
                                SoundManager.playReaction()
                            }
                        },
                        onRemoveTag = { tag ->
                            tags = tags - tag
                            SoundManager.playReaction()
                        },
                        tagInput = tagInput,
                        onTagInputChange = { tagInput = it },
                        mentions = mentions,
                        onAddMention = { mention ->
                            val clean = mention.trim().removePrefix("@").lowercase()
                            if (clean.isNotBlank() && !mentions.contains(clean)) {
                                mentions = mentions + clean
                                SoundManager.playReaction()
                            }
                        },
                        onRemoveMention = { mention ->
                            mentions = mentions - mention
                            SoundManager.playReaction()
                        },
                        mentionInput = mentionInput,
                        onMentionInputChange = { mentionInput = it },
                        suggestedCreators = suggestedCreators,
                        invitedCollaborators = invitedCollaborators,
                        onOpenCollabPicker = { showCollabPickerSheet = true },
                        location = locationInput,
                        onLocationChange = { locationInput = it },
                        audience = selectedAudience,
                        onAudienceChange = { selectedAudience = it },
                        allowRemix = allowRemix,
                        onAllowRemixChange = { allowRemix = it },
                        highQuality = highQualityUpload,
                        onHighQualityChange = { highQualityUpload = it },
                        audioTrackName = audioTrackName,
                        onAudioTrackChange = { audioTrackName = it },
                        selectedMusic = selectedMusic,
                        onOpenMusicPicker = { showMusicPickerSheet = true },
                        onClearMusic = { selectedMusic = null },
                        workCategory = workCategory,
                        onWorkCategoryChange = { workCategory = it },
                        allowDownload = allowDownload,
                        onAllowDownloadChange = { allowDownload = it },
                        isPublishing = isPublishing || (currentPostType == PostType.FLASH && flashIsUploading),
                        uploadProgress = if (currentPostType == PostType.FLASH) flashUploadProgress else 0f,
                        onPublish = { executePublish() }
                    )
                }
            }
        }

        if (showCollabPickerSheet) {
            com.chroma.studio.ui.components.CollabPickerModalSheet(
                currentUserUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                initialSelection = invitedCollaborators,
                repository = discoverRepo,
                onDone = { picked ->
                    invitedCollaborators = picked
                    showCollabPickerSheet = false
                },
                onDismiss = { showCollabPickerSheet = false }
            )
        }

        if (showMusicPickerSheet) {
            com.chroma.studio.ui.components.StoryMusicPickerModal(
                isOpen = true,
                initialTrackId = selectedMusic?.trackId,
                headerTitle = "Add Music",
                onDismiss = { showMusicPickerSheet = false },
                onClipConfirmed = { picked ->
                    selectedMusic = picked
                    audioTrackName = "${picked.title} · ${picked.artist}"
                    showMusicPickerSheet = false
                }
            )
        }
    }
}

/**
 * Top Navigation Bar with Step Pill Indicator & PostType Label
 */
@Composable
private fun OneUiPostCreationTopBar(
    postType: PostType,
    currentStep: PostCreationStep,
    onBack: () -> Unit,
    onNext: () -> Unit,
    isNextEnabled: Boolean,
    isPublishing: Boolean
) {
    val colors = LocalOscellionColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back / Close Icon Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.cardSurface)
                .border(0.5.dp, colors.glassBorder, CircleShape)
                .clickable {
                    SoundManager.playReaction()
                    onBack()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (currentStep == PostCreationStep.MEDIA_PICKER) Lucide.X else Lucide.ChevronLeft,
                contentDescription = "Back",
                tint = colors.textMain,
                modifier = Modifier.size(20.dp)
            )
        }

        // Center Step Indicator (One UI Segmented Pills)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val stepIndex = when (currentStep) {
                PostCreationStep.MEDIA_PICKER -> 0
                PostCreationStep.IMAGE_EDITOR -> 1
                PostCreationStep.POST_DETAILS -> 2
            }
            repeat(3) { index ->
                val isActive = index == stepIndex
                val isDone = index < stepIndex
                val pillWidth by animateFloatAsState(
                    targetValue = if (isActive) 24f else 8f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "stepPillWidth"
                )
                Box(
                    modifier = Modifier
                        .height(5.dp)
                        .width(pillWidth.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            when {
                                isActive -> colors.primary
                                isDone -> colors.primary.copy(alpha = 0.5f)
                                else -> colors.cardBorder
                            }
                        )
                )
            }
        }

        // Action Button: "Next" or Post Action (One UI Galaxy Blue Pill)
        val buttonLabel = when (currentStep) {
            PostCreationStep.POST_DETAILS -> when (postType) {
                PostType.PHOTO -> "Share"
                PostType.FLASH -> "Post Flash"
                PostType.WORK -> "Publish"
                PostType.STORY -> "Story"
            }
            else -> "Next"
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(colors.pillCornerRadius))
                .background(
                    if (isNextEnabled) colors.primary
                    else colors.cardSurfaceElevated
                )
                .border(
                    width = 0.5.dp,
                    color = if (isNextEnabled) Color.Transparent else colors.cardBorder,
                    shape = RoundedCornerShape(colors.pillCornerRadius)
                )
                .clickable(enabled = isNextEnabled) {
                    onNext()
                }
                .padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPublishing) {
                CircularProgressIndicator(
                    color = colors.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = buttonLabel,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isNextEnabled) colors.onPrimary else colors.textMuted,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

/**
 * Top Segmented Bar to Switch Post Types (Photo, Flash, Work, Story)
 */
@Composable
private fun PostTypeSegmentedBar(
    selectedType: PostType,
    onSelectType: (PostType) -> Unit
) {
    val colors = LocalOscellionColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(colors.pillCornerRadius))
            .background(colors.cardSurfaceElevated)
            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(colors.pillCornerRadius))
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PostType.entries.forEach { type ->
            val isSelected = type == selectedType
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(colors.pillCornerRadius))
                    .background(if (isSelected) colors.primary else Color.Transparent)
                    .clickable { onSelectType(type) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = type.icon,
                        contentDescription = type.title,
                        tint = if (isSelected) colors.onPrimary else colors.textMuted,
                        modifier = Modifier.size(13.5.dp)
                    )
                    Text(
                        text = type.title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) colors.onPrimary else colors.textMuted,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

/**
 * Quick Picker for User's Saved Chroma Works
 */
@Composable
private fun WorkSavedProjectsQuickStrip(
    works: List<ChromaWork>,
    selectedWork: ChromaWork?,
    onSelectWork: (ChromaWork) -> Unit
) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pick From Your Chroma Works",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMain,
                fontFamily = InterFontFamily
            )
            Text(
                text = "${works.size} projects",
                fontSize = 11.sp,
                color = colors.textMuted,
                fontFamily = InterFontFamily
            )
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(works) { work ->
                val isSelected = selectedWork?.id == work.id
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .clip(RoundedCornerShape(colors.cardCornerRadius))
                        .background(colors.cardSurface)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) colors.primary else colors.cardBorder,
                            shape = RoundedCornerShape(colors.cardCornerRadius)
                        )
                        .clickable { onSelectWork(work) }
                        .padding(10.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF6366F1), colors.primary)))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = work.name.ifBlank { "Untitled" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Canvas project",
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick Option Card to start Story with a Gradient Theme
 */
@Composable
private fun StoryGradientPromptCard(
    onSelectGradient: () -> Unit
) {
    val colors = LocalOscellionColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(colors.cardCornerRadius))
            .background(
                Brush.horizontalGradient(
                    if (colors.isDark) listOf(Color(0xFF1E1035), Color(0xFF0F0C20))
                    else listOf(Color(0xFFEEF2FF), Color(0xFFF5F3FF))
                )
            )
            .border(
                0.5.dp,
                if (colors.isDark) Color(0xFF6366F1).copy(alpha = 0.4f) else Color(0xFF6366F1).copy(alpha = 0.2f),
                RoundedCornerShape(colors.cardCornerRadius)
            )
            .clickable { onSelectGradient() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF4A00E0), Color(0xFFFF416C)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.Sparkles, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Create with Gradient Story Canvas",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMain,
                fontFamily = InterFontFamily
            )
            Text(
                text = "Type thoughts, quotes & design on live vibrant colors",
                fontSize = 11.5.sp,
                color = colors.textMuted,
                fontFamily = InterFontFamily
            )
        }
        Icon(Lucide.ChevronRight, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
    }
}

/**
 * Dedicated Presentation Studio for Creative Work Posts (Stage 2)
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun WorkStudioPresentationView(
    coverUri: Uri?,
    selectedWork: ChromaWork?,
    aspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    tools: List<String>,
    onToggleTool: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val scrollState = rememberScrollState()
    val textMain = colors.textMain
    val textMuted = colors.textMuted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Presentation Preview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (aspectRatio == "16:9") 1.777f else if (aspectRatio == "1:1") 1f else 1.333f)
                .clip(RoundedCornerShape(colors.cardCornerRadius))
                .background(colors.cardSurface)
                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(colors.cardCornerRadius))
                .shadow(12.dp, RoundedCornerShape(colors.cardCornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            if (coverUri != null) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = "Work Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF6366F1), colors.primary))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Lucide.Palette, null, tint = colors.onPrimary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = selectedWork?.name ?: "Chroma Canvas Project",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary
                        )
                    }
                }
            }
        }

        // Aspect Ratio Pills
        OneUiSettingsGroup(title = "Showcase Ratio") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("4:3", "16:9", "1:1").forEach { ratio ->
                    val isSelected = aspectRatio == ratio
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(colors.pillCornerRadius))
                            .background(if (isSelected) colors.primary else colors.cardSurfaceElevated)
                            .clickable {
                                SoundManager.playReaction()
                                onAspectRatioChange(ratio)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ratio,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) colors.onPrimary else textMuted
                        )
                    }
                }
            }
        }

        // Project Category Selector
        OneUiSettingsGroup(title = "Project Category") {
            val categories = listOf("Digital Art", "UI / UX", "3D & Motion", "Branding", "Illustration", "Typography")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = category == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(colors.pillCornerRadius))
                            .background(if (isSelected) colors.primary else colors.cardSurfaceElevated)
                            .clickable {
                                SoundManager.playReaction()
                                onCategoryChange(cat)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) colors.onPrimary else textMuted
                        )
                    }
                }
            }
        }

        // Tools Used Selector
        OneUiSettingsGroup(title = "Tools Used") {
            val toolPresets = listOf("Chroma Studio", "Figma", "Blender", "Procreate", "Photoshop", "Illustrator")
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                toolPresets.forEach { tool ->
                    val isSelected = tools.contains(tool)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(colors.pillCornerRadius))
                            .background(if (isSelected) colors.success else colors.cardSurfaceElevated)
                            .clickable { onToggleTool(tool) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tool,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) colors.onPrimary else textMuted
                        )
                    }
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(colors.pillCornerRadius),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
        ) {
            Text("Proceed to Work Details", color = colors.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Dedicated Styling Studio for 24h Story Posts (Stage 2)
 */
@Composable
private fun StoryStudioStylingView(
    backgroundUri: Uri?,
    useGradientCanvas: Boolean,
    selectedThemeIndex: Int,
    onThemeIndexChange: (Int) -> Unit,
    headline: String,
    onHeadlineChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val textMain = colors.textMain
    val textMuted = colors.textMuted
    val activeTheme = StoryGradientThemes.getOrElse(selectedThemeIndex) { StoryGradientThemes.first() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 9:16 Full Screen Live Story Card
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(390.dp)
                .clip(RoundedCornerShape(colors.sheetCornerRadius))
                .background(Brush.verticalGradient(activeTheme.colors))
                .shadow(16.dp, RoundedCornerShape(colors.sheetCornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            if (backgroundUri != null && !useGradientCanvas) {
                AsyncImage(
                    model = backgroundUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (headline.isNotBlank()) {
                    Text(
                        text = headline,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (body.isNotBlank()) {
                    Text(
                        text = body,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                } else if (headline.isBlank()) {
                    Text(
                        text = "Your Story text will appear here",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Expiration pill badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                .clip(RoundedCornerShape(colors.pillCornerRadius))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Lucide.Clock, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    Text("24h", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Gradient Theme Selector
        OneUiSettingsGroup(title = "Gradient Story Palette") {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(StoryGradientThemes) { idx, theme ->
                    val isSelected = idx == selectedThemeIndex
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(theme.colors))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                SoundManager.playReaction()
                                onThemeIndexChange(idx)
                            }
                    )
                }
            }
        }

        // Text Overlay inputs
        OneUiSettingsGroup(title = "Story Content") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = headline,
                    onValueChange = onHeadlineChange,
                    placeholder = { Text("Add headline / mood...", fontSize = 13.sp, color = textMuted) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.inputBg,
                        unfocusedContainerColor = colors.inputBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain
                    ),
                    shape = RoundedCornerShape(colors.buttonCornerRadius),
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = body,
                    onValueChange = onBodyChange,
                    placeholder = { Text("Write your story message...", fontSize = 13.sp, color = textMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.inputBg,
                        unfocusedContainerColor = colors.inputBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain
                    ),
                    shape = RoundedCornerShape(colors.buttonCornerRadius),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 120.dp)
                )
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(colors.pillCornerRadius),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
        ) {
            Text("Proceed to Story Settings", color = colors.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Universal Stage 3: Post Details Input & Information Form tailored to PostType
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UniversalPostDetailsInputView(
    postType: PostType,
    previewUri: Uri?,
    selectedPhotoUris: List<Uri> = emptyList(),
    selectedWork: ChromaWork? = null,
    useGradientCanvas: Boolean = false,
    storyGradientIndex: Int = 0,
    storyHeadline: String = "",
    storyBody: String = "",
    colorMatrix: ColorMatrix?,
    aspectRatio: String,
    caption: String,
    onCaptionChange: (String) -> Unit,
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    mentions: List<String>,
    onAddMention: (String) -> Unit,
    onRemoveMention: (String) -> Unit,
    mentionInput: String,
    onMentionInputChange: (String) -> Unit,
    suggestedCreators: List<UserProfile>,
    invitedCollaborators: List<com.chroma.studio.model.CollabParticipant> = emptyList(),
    onOpenCollabPicker: () -> Unit = {},
    location: String,
    onLocationChange: (String) -> Unit,
    audience: String,
    onAudienceChange: (String) -> Unit,
    allowRemix: Boolean,
    onAllowRemixChange: (Boolean) -> Unit,
    highQuality: Boolean,
    onHighQualityChange: (Boolean) -> Unit,
    audioTrackName: String,
    onAudioTrackChange: (String) -> Unit,
    selectedMusic: com.chroma.studio.data.AttachedMusicSelection? = null,
    onOpenMusicPicker: () -> Unit = {},
    onClearMusic: () -> Unit = {},
    workCategory: String,
    onWorkCategoryChange: (String) -> Unit,
    allowDownload: Boolean,
    onAllowDownloadChange: (Boolean) -> Unit,
    isPublishing: Boolean,
    uploadProgress: Float = 0f,
    onPublish: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val scrollState = rememberScrollState()
    val textMain = colors.textMain
    val textMuted = colors.textMuted
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. One UI Viewing Header Area tailored to PostType
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (postType) {
                    PostType.PHOTO -> "New Photo Post"
                    PostType.FLASH -> "Publish Flash Video"
                    PostType.WORK -> "Publish Creative Work"
                    PostType.STORY -> "Share 24h Story"
                },
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
                color = textMain,
                fontFamily = InterFontFamily
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = when (postType) {
                    PostType.PHOTO -> "Share your visual moments with high fidelity"
                    PostType.FLASH -> "Drop dynamic short-form video into the community feed"
                    PostType.WORK -> "Showcase your masterwork to the global creator network"
                    PostType.STORY -> "Share a fleeting moment that expires in 24 hours"
                },
                fontSize = 13.sp,
                color = textMuted,
                fontFamily = InterFontFamily
            )
        }

        // 2. Hero Post Preview & Caption Group (26.dp Squircle)
        OneUiSettingsGroup(title = "") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Squircle Thumbnail
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(colors.cardCornerRadius))
                            .background(colors.cardSurfaceElevated)
                    ) {
                        if (useGradientCanvas && postType == PostType.STORY) {
                            val activeTheme = StoryGradientThemes.getOrElse(storyGradientIndex) { StoryGradientThemes.first() }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(activeTheme.colors)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Sparkles, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        } else if (previewUri != null) {
                            AsyncImage(
                                model = previewUri,
                                contentDescription = "Post Thumbnail",
                                contentScale = ContentScale.Crop,
                                colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF0381FE)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(postType.icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }

                        // Badge on Thumbnail
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = when (postType) {
                                    PostType.FLASH -> "9:16"
                                    PostType.STORY -> "Story"
                                    else -> aspectRatio
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Multiline Caption Input
                    Column(modifier = Modifier.weight(1f)) {
                        TextField(
                            value = caption,
                            onValueChange = { if (it.length <= 1000) onCaptionChange(it) },
                            placeholder = {
                                Text(
                                    text = when (postType) {
                                        PostType.PHOTO -> "Write a caption or thoughts..."
                                        PostType.FLASH -> "Describe your flash..."
                                        PostType.WORK -> "Project title & concept..."
                                        PostType.STORY -> "Story subtext (optional)..."
                                    },
                                    color = textMuted,
                                    fontSize = 14.sp
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp, max = 140.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${caption.length}/1000",
                                fontSize = 11.sp,
                                color = textMuted
                            )
                        }
                    }
                }

                // Extra Work-specific category tag badge
                if (postType == PostType.WORK) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0381FE).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Category: $workCategory", color = Color(0xFF0381FE), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (selectedWork != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Chroma Project", color = Color(0xFF10B981), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // If multiple photos selected, display thumbnails row
                if (selectedPhotoUris.size > 1 && postType == PostType.PHOTO) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${selectedPhotoUris.size} Photos in Post",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0381FE),
                        fontFamily = InterFontFamily
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(selectedPhotoUris) { idx, uri ->
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.cardSurfaceElevated)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Photo ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .size(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Music & Soundtrack Picker (Available for Photos, Flashes, Works & Stories)
        OneUiSettingsGroup(title = "Music & Soundtrack") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenMusicPicker)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFA2D48).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    val cover = selectedMusic?.coverUrl
                    if (!cover.isNullOrBlank()) {
                        AsyncImage(
                            model = cover,
                            contentDescription = "Track art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Lucide.Music, null, tint = Color(0xFFFA2D48), modifier = Modifier.size(18.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedMusic?.title ?: "Add Music",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selectedMusic?.let { "${it.artist} · ${it.startSec}s clip (${it.durationSec}s)" }
                            ?: "Choose from Music Library",
                        fontSize = 12.sp,
                        color = textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (selectedMusic != null) {
                    IconButton(onClick = onClearMusic, modifier = Modifier.size(28.dp)) {
                        Icon(Lucide.X, contentDescription = "Clear music", tint = textMuted, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Icon(Lucide.ChevronRight, null, tint = textMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 4. Creative Discovery Tags Group (Except for Story)
        if (postType != PostType.STORY) {
            OneUiSettingsGroup(title = "Tags & Topics") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (tags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(colors.pillCornerRadius))
                                        .background(colors.cardSurfaceElevated)
                                        .clickable { onRemoveTag(tag) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.primary
                                        )
                                        Icon(Lucide.X, null, tint = textMuted, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = tagInput,
                            onValueChange = onTagInputChange,
                            placeholder = { Text("Add tag (#cinematic, #motion...)", fontSize = 13.sp, color = textMuted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (tagInput.isNotBlank()) {
                                    onAddTag(tagInput)
                                    onTagInputChange("")
                                }
                                focusManager.clearFocus()
                            }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = colors.inputBg,
                                unfocusedContainerColor = colors.inputBg,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain
                            ),
                            shape = RoundedCornerShape(colors.buttonCornerRadius),
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(colors.buttonCornerRadius))
                                .background(colors.primary)
                                .clickable {
                                    if (tagInput.isNotBlank()) {
                                        onAddTag(tagInput)
                                        onTagInputChange("")
                                        focusManager.clearFocus()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Plus, contentDescription = "Add", tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // 5. Tag People & Mentions Group (Except for Story)
        if (postType != PostType.STORY) {
            OneUiSettingsGroup(title = "Tag People & Credits") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (mentions.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            mentions.forEach { mention ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(colors.pillCornerRadius))
                                        .background(colors.cardSurfaceElevated)
                                        .clickable { onRemoveMention(mention) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("@$mention", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                                        Icon(Lucide.X, null, tint = textMuted, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    TextField(
                        value = mentionInput,
                        onValueChange = onMentionInputChange,
                        placeholder = { Text("Search creator by @username...", fontSize = 13.sp, color = textMuted) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.inputBg,
                            unfocusedContainerColor = colors.inputBg,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain
                        ),
                        shape = RoundedCornerShape(colors.buttonCornerRadius),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (suggestedCreators.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(suggestedCreators) { creator ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(colors.pillCornerRadius))
                                        .background(colors.cardSurfaceElevated)
                                        .clickable {
                                            onAddMention(creator.username)
                                            onMentionInputChange("")
                                            focusManager.clearFocus()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("@${creator.username}", fontSize = 11.5.sp, color = textMain, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Collaborators Invite Group (Posts & Flashes)
        if (postType != PostType.STORY) {
            OneUiSettingsGroup(title = "Collaboration") {
                com.chroma.studio.ui.components.CollabInviteActionRow(
                    selectedCollaborators = invitedCollaborators,
                    onClick = onOpenCollabPicker
                )
            }
        }

        // 6. Location (Photo & Work)
        if (postType == PostType.PHOTO || postType == PostType.WORK) {
            OneUiSettingsGroup(title = "Location") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextField(
                        value = location,
                        onValueChange = onLocationChange,
                        placeholder = { Text("Add location (e.g., Tokyo, Japan)", fontSize = 13.sp, color = textMuted) },
                        singleLine = true,
                        leadingIcon = { Icon(Lucide.MapPin, null, tint = colors.primary, modifier = Modifier.size(16.dp)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.inputBg,
                            unfocusedContainerColor = colors.inputBg,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain
                        ),
                        shape = RoundedCornerShape(colors.buttonCornerRadius),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 7. Audience & Privacy Controls
        OneUiSettingsGroup(title = "Audience & Sharing") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Who can view", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = textMain)

                    val options = if (postType == PostType.STORY) listOf("Public", "Only Friends") else listOf("Public", "Followers", "Only Friends")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        options.forEach { opt ->
                            val isSelected = audience == opt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(colors.pillCornerRadius))
                                    .background(
                                        if (isSelected) colors.primary
                                        else colors.cardSurfaceElevated
                                    )
                                    .clickable {
                                        SoundManager.playReaction()
                                        onAudienceChange(opt)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = opt,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) colors.onPrimary else textMuted
                                )
                            }
                        }
                    }
                }

                if (postType != PostType.STORY) {
                    HorizontalDivider(color = colors.cardBorder, thickness = 0.5.dp)

                    // Allow Remix Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAllowRemixChange(!allowRemix) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow Remixing", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textMain)
                            Text("Other creators can build upon this visual", fontSize = 11.5.sp, color = textMuted)
                        }
                        AppleSwitch(
                            checked = allowRemix,
                            onCheckedChange = onAllowRemixChange,
                            activeTrackColor = colors.primary
                        )
                    }
                }

                if (postType == PostType.WORK) {
                    HorizontalDivider(color = colors.cardBorder, thickness = 0.5.dp)

                    // Allow Asset Download Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAllowDownloadChange(!allowDownload) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow Free Download", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textMain)
                            Text("Enable community to download full-res assets", fontSize = 11.5.sp, color = textMuted)
                        }
                        AppleSwitch(
                            checked = allowDownload,
                            onCheckedChange = onAllowDownloadChange,
                            activeTrackColor = colors.success
                        )
                    }
                }

                if (postType == PostType.STORY) {
                    HorizontalDivider(color = colors.cardBorder, thickness = 0.5.dp)

                    // Story 24h Expiration Card Notice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.warning.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Clock, null, tint = colors.warning, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("24h Fleeting Moment", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textMain)
                            Text("This story disappears from your profile after 24 hours.", fontSize = 11.sp, color = textMuted)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 8. Bottom Share Action Button tailored to PostType
        val mainActionText = when (postType) {
            PostType.PHOTO -> "Share Post"
            PostType.FLASH -> "Post Flash"
            PostType.WORK -> "Publish Masterwork"
            PostType.STORY -> "Share to Story"
        }

        Button(
            onClick = onPublish,
            enabled = !isPublishing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(colors.pillCornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (postType) {
                    PostType.PHOTO -> colors.primary
                    PostType.FLASH -> Color(0xFF6366F1)
                    PostType.WORK -> colors.success
                    PostType.STORY -> Color(0xFFF43F5E)
                }
            )
        ) {
            if (isPublishing) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                val progressText = if (postType == PostType.FLASH && uploadProgress > 0f) {
                    "Publishing ${(uploadProgress * 100).toInt()}%..."
                } else {
                    "Publishing..."
                }
                Text(progressText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(postType.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(mainActionText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
