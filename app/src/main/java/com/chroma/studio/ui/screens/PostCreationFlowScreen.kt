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
import androidx.compose.foundation.isSystemInDarkTheme
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
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.OneUiIconBadge
import com.chroma.studio.ui.components.OneUiSettingsGroup
import com.chroma.studio.ui.components.OscellionGalleryLayout
import com.chroma.studio.ui.components.GalleryMediaTypeFilter
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.utils.compressImageToUnder50KB
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 3-Stage Instagram-style Post Creation & Onboarding Flow
 * Following Samsung One UI 7.0 design principles.
 */
enum class PostCreationStep {
    MEDIA_PICKER,   // Stage 1: Select photo from gallery or camera
    QUICK_FILTER,   // Stage 2: Fast Instagram-style filters & basic adjustment (+ Pro Editor portal)
    POST_DETAILS    // Stage 3: Caption, tags, creator mentions, location, audience & publishing
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
        description = "Deep shadows and cool cyan highlights",
        matrix = floatArrayOf(
            1.12f, 0f, 0.08f, 0f, 8f,
            0f, 1.15f, 0.05f, 0f, 10f,
            0.08f, 0.05f, 1.25f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    QuickFilterPreset(
        id = "mono",
        name = "Noir Mono",
        description = "High-contrast black & white film",
        matrix = floatArrayOf(
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    QuickFilterPreset(
        id = "warm_sunset",
        name = "Warm Glow",
        description = "Golden hour amber sunlight",
        matrix = floatArrayOf(
            1.22f, 0.06f, 0f, 0f, 15f,
            0.05f, 1.10f, 0f, 0f, 8f,
            0f, 0f, 0.88f, 0f, -8f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    QuickFilterPreset(
        id = "cool_breeze",
        name = "Cool Cyan",
        description = "Atmospheric chill and modern blue hue",
        matrix = floatArrayOf(
            0.92f, 0f, 0.08f, 0f, -5f,
            0f, 1.05f, 0.12f, 0f, 6f,
            0.05f, 0.10f, 1.22f, 0f, 18f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    QuickFilterPreset(
        id = "matte_fade",
        name = "Matte Fade",
        description = "Muted editorial finish with lifted shadows",
        matrix = floatArrayOf(
            0.95f, 0.05f, 0.05f, 0f, 20f,
            0.05f, 0.95f, 0.05f, 0f, 20f,
            0.05f, 0.05f, 0.95f, 0f, 20f,
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
    initialPhotoUri: Uri? = null,
    onFinish: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val discoverRepo = remember { DiscoverRepository() }

    // Navigation Step
    var currentStep by remember {
        mutableStateOf(
            if (initialPhotoUri != null) PostCreationStep.QUICK_FILTER
            else PostCreationStep.MEDIA_PICKER
        )
    }

    // Media & Editing States
    var rawPhotoUri by remember { mutableStateOf<Uri?>(initialPhotoUri) }
    var editedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAspectRatio by remember { mutableStateOf("4:5") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var brightnessOffset by remember { mutableFloatStateOf(0f) } // -50 to +50
    var contrastScale by remember { mutableFloatStateOf(1f) }    // 0.7 to 1.3

    // Pro Advanced Editor Portal Gate
    var showAdvanceEditor by remember { mutableStateOf(false) }

    // Post Details Input States
    var caption by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(listOf("art", "creative")) }
    var tagInput by remember { mutableStateOf("") }
    var mentions by remember { mutableStateOf(emptyList<String>()) }
    var mentionInput by remember { mutableStateOf("") }
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
        // Apply brightness offset
        base[4] += brightnessOffset
        base[9] += brightnessOffset
        base[14] += brightnessOffset
        // Apply contrast scale
        base[0] *= contrastScale
        base[6] *= contrastScale
        base[12] *= contrastScale
        ColorMatrix(base)
    }

    // Export quick-filtered image to cache file if needed
    suspend fun renderFilteredBitmap(sourceUri: Uri): Uri = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (selectedFilterIndex == 0 && brightnessOffset == 0f && contrastScale == 1f) {
                return@withContext sourceUri
            }

            val resultBitmap = Bitmap.createBitmap(
                originalBitmap.width,
                originalBitmap.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = AndroidCanvas(resultBitmap)
            val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                val androidMatrix = AndroidColorMatrix(activeColorMatrix.values)
                colorFilter = ColorMatrixColorFilter(androidMatrix)
            }
            canvas.drawBitmap(originalBitmap, 0f, 0f, paint)

            val cacheFile = File(context.cacheDir, "post_filter_${UUID.randomUUID()}.jpg")
            FileOutputStream(cacheFile).use { out ->
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            Uri.fromFile(cacheFile)
        } catch (_: Exception) {
            sourceUri
        }
    }

    // Share / Publish Post
    fun executePublish() {
        val currentPhoto = editedPhotoUri ?: rawPhotoUri
        if (currentPhoto == null) {
            Toast.makeText(context, "Please select a photo first", Toast.LENGTH_SHORT).show()
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(context, "Please log in to share posts", Toast.LENGTH_SHORT).show()
            return
        }

        isPublishing = true
        scope.launch {
            try {
                // 1. Render filtered bitmap if applicable
                val finalToUploadUri = if (editedPhotoUri != null) editedPhotoUri!! else renderFilteredBitmap(currentPhoto)

                // 2. Compress image for high-speed delivery
                val imageBytes = withContext(Dispatchers.IO) {
                    compressImageToUnder50KB(context, finalToUploadUri)
                } ?: throw Exception("Failed to process image")

                // 3. Upload and publish via DiscoverRepository
                val success = withContext(Dispatchers.IO) {
                    discoverRepo.uploadImagePost(
                        imageBytes = imageBytes,
                        caption = caption.ifBlank { "Untitled creation" },
                        description = if (locationInput.isNotBlank()) "📍 $locationInput" else "",
                        aspectRatio = selectedAspectRatio,
                        tags = tags,
                        mentions = mentions
                    )
                }

                if (success) {
                    SoundManager.playSend()
                    Toast.makeText(context, "Post shared successfully!", Toast.LENGTH_SHORT).show()
                    onFinish()
                } else {
                    Toast.makeText(context, "Upload failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isPublishing = false
            }
        }
    }

    // Back handling
    BackHandler {
        if (showAdvanceEditor) {
            showAdvanceEditor = false
        } else {
            when (currentStep) {
                PostCreationStep.POST_DETAILS -> currentStep = PostCreationStep.QUICK_FILTER
                PostCreationStep.QUICK_FILTER -> {
                    if (initialPhotoUri != null) onClose()
                    else currentStep = PostCreationStep.MEDIA_PICKER
                }
                PostCreationStep.MEDIA_PICKER -> onClose()
            }
        }
    }

    // If pro editor is opened, render it modally
    if (showAdvanceEditor && (rawPhotoUri != null || editedPhotoUri != null)) {
        val targetUri = editedPhotoUri ?: rawPhotoUri!!
        ChromaPhotoEditorScreen(
            photoUri = targetUri,
            exportButtonLabel = "Done",
            onExportSuccess = { proEditedUri ->
                editedPhotoUri = proEditedUri
                showAdvanceEditor = false
                SoundManager.playReaction()
            },
            onBack = {
                showAdvanceEditor = false
            }
        )
        return
    }

    val screenBg = if (isDark) Color(0xFF000000) else Color(0xFFF2F4F7)

    Scaffold(
        containerColor = screenBg,
        topBar = {
            OneUiPostCreationTopBar(
                currentStep = currentStep,
                onBack = {
                    when (currentStep) {
                        PostCreationStep.POST_DETAILS -> currentStep = PostCreationStep.QUICK_FILTER
                        PostCreationStep.QUICK_FILTER -> {
                            if (initialPhotoUri != null) onClose()
                            else currentStep = PostCreationStep.MEDIA_PICKER
                        }
                        PostCreationStep.MEDIA_PICKER -> onClose()
                    }
                },
                onNext = {
                    when (currentStep) {
                        PostCreationStep.MEDIA_PICKER -> {
                            if (rawPhotoUri != null) {
                                SoundManager.playReaction()
                                currentStep = PostCreationStep.QUICK_FILTER
                            }
                        }
                        PostCreationStep.QUICK_FILTER -> {
                            SoundManager.playReaction()
                            currentStep = PostCreationStep.POST_DETAILS
                        }
                        PostCreationStep.POST_DETAILS -> executePublish()
                    }
                },
                isNextEnabled = when (currentStep) {
                    PostCreationStep.MEDIA_PICKER -> rawPhotoUri != null
                    PostCreationStep.QUICK_FILTER -> true
                    PostCreationStep.POST_DETAILS -> !isPublishing
                },
                isPublishing = isPublishing,
                isDark = isDark
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentStep) {
                PostCreationStep.MEDIA_PICKER -> {
                    // STAGE 1: ONE UI SQUIRCLE GALLERY PICKER
                    OscellionGalleryLayout(
                        title = "New Post",
                        badgeText = "",
                        maxSelection = 10,
                        allowedMediaFilter = GalleryMediaTypeFilter.PHOTOS,
                        showCameraTile = true,
                        onPhotosSelected = { uris ->
                            val selected = uris.firstOrNull()
                            if (selected != null) {
                                rawPhotoUri = selected
                                editedPhotoUri = null
                                SoundManager.playReaction()
                                currentStep = PostCreationStep.QUICK_FILTER
                            }
                        },
                        onClose = onClose
                    )
                }

                PostCreationStep.QUICK_FILTER -> {
                    // STAGE 2: INSTANT FILTERS & LIGHTWEIGHT EDITS (+ PRO EDITOR GATEWAY)
                    PostQuickFilterView(
                        photoUri = editedPhotoUri ?: rawPhotoUri!!,
                        aspectRatio = selectedAspectRatio,
                        onAspectRatioChange = { selectedAspectRatio = it },
                        selectedFilterIndex = selectedFilterIndex,
                        onFilterSelect = { selectedFilterIndex = it },
                        colorMatrix = activeColorMatrix,
                        brightnessOffset = brightnessOffset,
                        onBrightnessChange = { brightnessOffset = it },
                        contrastScale = contrastScale,
                        onContrastChange = { contrastScale = it },
                        onOpenAdvanceEditor = {
                            SoundManager.playReaction()
                            showAdvanceEditor = true
                        },
                        isDark = isDark
                    )
                }

                PostCreationStep.POST_DETAILS -> {
                    // STAGE 3: POST INFORMATION INPUTS & PUBLISHING
                    PostDetailsInputView(
                        previewUri = editedPhotoUri ?: rawPhotoUri!!,
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
                        location = locationInput,
                        onLocationChange = { locationInput = it },
                        audience = selectedAudience,
                        onAudienceChange = { selectedAudience = it },
                        allowRemix = allowRemix,
                        onAllowRemixChange = { allowRemix = it },
                        highQuality = highQualityUpload,
                        onHighQualityChange = { highQualityUpload = it },
                        isPublishing = isPublishing,
                        onPublish = { executePublish() },
                        isDark = isDark
                    )
                }
            }
        }
    }
}

/**
 * Top Navigation Bar with Step Pill Indicator
 */
@Composable
private fun OneUiPostCreationTopBar(
    currentStep: PostCreationStep,
    onBack: () -> Unit,
    onNext: () -> Unit,
    isNextEnabled: Boolean,
    isPublishing: Boolean,
    isDark: Boolean
) {
    val barBg = if (isDark) Color(0xFF000000) else Color(0xFFF2F4F7)
    val textMain = if (isDark) Color.White else Color(0xFF0F172A)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(barBg)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back / Close Icon Button (One UI Circular Badge)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF161A22) else Color.White)
                .clickable {
                    SoundManager.playReaction()
                    onBack()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (currentStep == PostCreationStep.MEDIA_PICKER) Lucide.X else Lucide.ChevronLeft,
                contentDescription = "Back",
                tint = textMain,
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
                PostCreationStep.QUICK_FILTER -> 1
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
                                isActive -> Color(0xFF2563EB)
                                isDone -> Color(0xFF2563EB).copy(alpha = 0.5f)
                                else -> if (isDark) Color(0xFF262C38) else Color(0xFFCBD5E1)
                            }
                        )
                )
            }
        }

        // Action Button: "Next" or "Share" (One UI Galaxy Blue Pill)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (isNextEnabled) Color(0xFF2563EB)
                    else (if (isDark) Color(0xFF1E2430) else Color(0xFFE2E8F0))
                )
                .clickable(enabled = isNextEnabled) {
                    onNext()
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPublishing) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = when (currentStep) {
                        PostCreationStep.POST_DETAILS -> "Share"
                        else -> "Next"
                    },
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isNextEnabled) Color.White else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)),
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

/**
 * Stage 2: Quick Filter, Aspect Ratio & Pro Editor Portal
 */
@Composable
private fun PostQuickFilterView(
    photoUri: Uri,
    aspectRatio: String,
    onAspectRatioChange: (String) -> Unit,
    selectedFilterIndex: Int,
    onFilterSelect: (Int) -> Unit,
    colorMatrix: ColorMatrix,
    brightnessOffset: Float,
    onBrightnessChange: (Float) -> Unit,
    contrastScale: Float,
    onContrastChange: (Float) -> Unit,
    onOpenAdvanceEditor: () -> Unit,
    isDark: Boolean
) {
    val scrollState = rememberScrollState()
    val textMain = if (isDark) Color.White else Color(0xFF0F172A)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Preview Canvas (Aspect Ratio adaptive)
        val ratioFloat = when (aspectRatio) {
            "1:1" -> 1f
            "4:5" -> 0.8f
            "16:9" -> 1.777f
            else -> 0.8f
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratioFloat.coerceIn(0.6f, 1.8f))
                .clip(RoundedCornerShape(26.dp))
                .background(if (isDark) Color(0xFF161A22) else Color.White)
                .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Preview",
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(colorMatrix),
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Aspect Ratio Selectors (Border-less White Capsules)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val ratios = listOf("1:1" to "Square", "4:5" to "Portrait", "16:9" to "Landscape")
            ratios.forEach { (ratioKey, label) ->
                val isSelected = aspectRatio == ratioKey
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isSelected) Color(0xFF2563EB)
                            else (if (isDark) Color(0xFF161A22) else Color.White)
                        )
                        .clickable {
                            SoundManager.playReaction()
                            onAspectRatioChange(ratioKey)
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$ratioKey $label",
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else textMuted,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }

        // 3. ✨ THE PRO-EDITOR GATEWAY BUTTON ✨
        // Clean One UI 26dp squircle card inviting users to pro tools without forcing it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2563EB).copy(alpha = if (isDark) 0.18f else 0.10f),
                            Color(0xFF4F46E5).copy(alpha = if (isDark) 0.14f else 0.06f)
                        )
                    )
                )
                .clickable { onOpenAdvanceEditor() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OneUiIconBadge(
                        icon = Lucide.Sparkles,
                        iconColor = Color(0xFF2563EB)
                    )
                    Column {
                        Text(
                            text = "Open advance Photo editor",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textMain,
                            fontFamily = InterFontFamily
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Curves, HSL mix, vector draw, stickers & text",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = textMuted,
                            fontFamily = InterFontFamily
                        )
                    }
                }

                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 4. Instant Filter Presets Carousel (Instagram-style)
        OneUiSettingsGroup(
            title = "Instant Filters",
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(QUICK_FILTER_PRESETS) { index, preset ->
                    val isSelected = index == selectedFilterIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable {
                                SoundManager.playReaction()
                                onFilterSelect(index)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .background(if (isDark) Color(0xFF1E2430) else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = preset.name,
                                contentScale = ContentScale.Crop,
                                colorFilter = ColorFilter.colorMatrix(ColorMatrix(preset.matrix)),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = preset.name,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF2563EB) else textMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 5. Quick Tweak Sliders (Brightness & Contrast)
        OneUiSettingsGroup(
            title = "Quick Adjust",
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Brightness Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Lucide.Sun, contentDescription = null, tint = textMuted, modifier = Modifier.size(14.dp))
                            Text("Brightness", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textMain)
                        }
                        Text("${brightnessOffset.toInt()}", fontSize = 12.sp, color = textMuted)
                    }
                    Slider(
                        value = brightnessOffset,
                        onValueChange = onBrightnessChange,
                        valueRange = -50f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF2563EB),
                            activeTrackColor = Color(0xFF2563EB)
                        )
                    )
                }

                // Contrast Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Lucide.SlidersHorizontal, contentDescription = null, tint = textMuted, modifier = Modifier.size(14.dp))
                            Text("Contrast", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textMain)
                        }
                        Text("${((contrastScale - 1f) * 100).toInt()}%", fontSize = 12.sp, color = textMuted)
                    }
                    Slider(
                        value = contrastScale,
                        onValueChange = onContrastChange,
                        valueRange = 0.7f..1.3f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF2563EB),
                            activeTrackColor = Color(0xFF2563EB)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Stage 3: Post Details Input & Information Form
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PostDetailsInputView(
    previewUri: Uri,
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
    location: String,
    onLocationChange: (String) -> Unit,
    audience: String,
    onAudienceChange: (String) -> Unit,
    allowRemix: Boolean,
    onAllowRemixChange: (Boolean) -> Unit,
    highQuality: Boolean,
    onHighQualityChange: (Boolean) -> Unit,
    isPublishing: Boolean,
    onPublish: () -> Unit,
    isDark: Boolean
) {
    val scrollState = rememberScrollState()
    val textMain = if (isDark) Color.White else Color(0xFF0F172A)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF161A22) else Color.White
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. One UI Viewing Header Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "New Post",
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = textMain,
                fontFamily = InterFontFamily
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Share your creation with the community",
                fontSize = 13.sp,
                color = textMuted,
                fontFamily = InterFontFamily
            )
        }

        // 2. Hero Post Preview & Caption Group (26.dp Squircle)
        OneUiSettingsGroup(title = "") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Squircle Thumbnail
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1E2430) else Color(0xFFE2E8F0))
                ) {
                    AsyncImage(
                        model = previewUri,
                        contentDescription = "Post Thumbnail",
                        contentScale = ContentScale.Crop,
                        colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(it) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Aspect Ratio Pill Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = aspectRatio,
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
                                text = "Write a caption or thoughts...",
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
        }

        // 3. Creative Discovery Tags Group
        OneUiSettingsGroup(title = "Tags & Topics") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Active Tags Chips Flow
                if (tags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isDark) Color(0xFF1E2430) else Color(0xFFF1F5F9))
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
                                        color = Color(0xFF2563EB)
                                    )
                                    Icon(
                                        Lucide.X,
                                        contentDescription = null,
                                        tint = textMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Add Custom Tag Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = tagInput,
                        onValueChange = onTagInputChange,
                        placeholder = { Text("Add a tag (#digital, #photo...)", fontSize = 13.sp, color = textMuted) },
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
                            focusedContainerColor = if (isDark) Color(0xFF1E2430) else Color(0xFFF8FAFC),
                            unfocusedContainerColor = if (isDark) Color(0xFF1E2430) else Color(0xFFF8FAFC),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF2563EB))
                            .clickable {
                                if (tagInput.isNotBlank()) {
                                    onAddTag(tagInput)
                                    onTagInputChange("")
                                    focusManager.clearFocus()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.Plus, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // Suggested Quick Tags
                val popularSuggestions = listOf("art", "photography", "3d", "cinematic", "minimal", "vector")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    popularSuggestions.filter { !tags.contains(it) }.forEach { pTag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isDark) Color(0xFF12141A) else Color.White)
                                .clickable { onAddTag(pTag) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ #$pTag",
                                fontSize = 11.5.sp,
                                color = textMuted,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }
            }
        }

        // 4. Tag People & Mentions Group
        OneUiSettingsGroup(title = "Tag People & Mentions") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Active Mentions Chips Flow
                if (mentions.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        mentions.forEach { mention ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isDark) Color(0xFF1E2430) else Color(0xFFF1F5F9))
                                    .clickable { onRemoveMention(mention) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "@$mention",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2563EB)
                                    )
                                    Icon(Lucide.X, contentDescription = null, tint = textMuted, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // Mention Search Input
                TextField(
                    value = mentionInput,
                    onValueChange = onMentionInputChange,
                    placeholder = { Text("Search creator by @username...", fontSize = 13.sp, color = textMuted) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF1E2430) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E2430) else Color(0xFFF8FAFC),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Autocomplete Creator Dropdown
                if (suggestedCreators.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF12141A) else Color(0xFFF8FAFC))
                            .padding(vertical = 4.dp)
                    ) {
                        suggestedCreators.take(4).forEach { creator ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddMention(creator.username)
                                        onMentionInputChange("")
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (creator.displayName.firstOrNull() ?: creator.username.firstOrNull() ?: 'C').uppercase(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = creator.displayName.ifBlank { creator.username },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textMain
                                    )
                                    Text(
                                        text = "@${creator.username}",
                                        fontSize = 11.5.sp,
                                        color = textMuted
                                    )
                                }

                                Icon(Lucide.Plus, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // 5. Location Group
        OneUiSettingsGroup(title = "Location") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OneUiIconBadge(icon = Lucide.MapPin, iconColor = Color(0xFFEF4444))

                    TextField(
                        value = location,
                        onValueChange = onLocationChange,
                        placeholder = { Text("Add location...", fontSize = 13.sp, color = textMuted) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Location Suggestions
                val locationSuggestions = listOf("Chroma Studio", "Tokyo, Japan", "New York, NY", "London, UK")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    locationSuggestions.forEach { loc ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isDark) Color(0xFF12141A) else Color.White)
                                .clickable { onLocationChange(loc) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(loc, fontSize = 11.5.sp, color = textMuted)
                        }
                    }
                }
            }
        }

        // 6. Audience & Privacy Group
        OneUiSettingsGroup(title = "Audience & Sharing") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Audience Segmented Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Who can view", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = textMain)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Public", "Followers", "Only Friends").forEach { opt ->
                            val isSelected = audience == opt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (isSelected) Color(0xFF2563EB)
                                        else (if (isDark) Color(0xFF1E2430) else Color.White)
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
                                    color = if (isSelected) Color.White else textMuted
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)

                // Allow Remix Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Remixing", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textMain)
                        Text("Other creators can build upon this visual", fontSize = 11.5.sp, color = textMuted)
                    }
                    Switch(
                        checked = allowRemix,
                        onCheckedChange = onAllowRemixChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2563EB)
                        )
                    )
                }

                HorizontalDivider(color = if (isDark) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)

                // High Quality Upload Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("High Quality Upload", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textMain)
                        Text("Uploads using studio color depth profile", fontSize = 11.5.sp, color = textMuted)
                    }
                    Switch(
                        checked = highQuality,
                        onCheckedChange = onHighQualityChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2563EB)
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 7. Bottom Share Action Button (One UI Galaxy Blue 52dp Pill)
        Button(
            onClick = onPublish,
            enabled = !isPublishing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            if (isPublishing) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Publishing Post...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Lucide.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share Post", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
