package com.chroma.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.BottomDrawer
import com.chroma.studio.ui.components.ChromaAppBar
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.ui.components.ContrastCheckerBadge
import com.chroma.studio.ui.components.DeveloperHandoffModal
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.chrisbanes.haze.haze
import com.chroma.studio.ui.components.LocalHazeState
import com.chroma.studio.ui.components.MeshBlobHandlesOverlay
import com.chroma.studio.ui.theme.OscellionTheme
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.ChromaViewModel
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.ChromaWork
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.chroma.studio.ui.components.getAvatarPresetDrawableRes
import com.chroma.studio.ui.components.getDefaultAvatarDrawableRes
import com.chroma.studio.ui.components.generateQrBitmap
import com.composables.icons.lucide.*
import com.composables.icons.lucide.BadgeCheck
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.QrCode
import androidx.compose.material3.ExperimentalMaterial3Api

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_WORK_ID = "work_id"
        const val EXTRA_WORK_NAME = "work_name"
        const val EXTRA_WORK_DESCRIPTION = "work_description"
        const val EXTRA_LAYERS_JSON = "layers_json"
        const val EXTRA_CANVAS_SHAPE = "canvas_shape"
        const val EXTRA_IS_HOME_BACKGROUND = "is_home_background"
        const val EXTRA_IS_PASS_CARD_MODE = "is_pass_card_mode"
        const val EXTRA_PASS_USER_ID = "pass_user_id"
        const val EXTRA_PASS_DISPLAY_NAME = "pass_display_name"
        const val EXTRA_PASS_USERNAME = "pass_username"
        const val EXTRA_PASS_AVATAR = "pass_avatar"
        const val EXTRA_PASS_ROLE = "pass_role"
        const val EXTRA_PASS_VERIFIED = "pass_verified"
        const val EXTRA_PASS_GENDER = "pass_gender"
        // Remix metadata extras
        const val EXTRA_REMIX_POST_ID = "remix_post_id"
        const val EXTRA_REMIX_AUTHOR_ID = "remix_author_id"
        const val EXTRA_REMIX_AUTHOR_USERNAME = "remix_author_username"
        const val EXTRA_REMIX_AUTHOR_DISPLAY_NAME = "remix_author_display_name"
        const val EXTRA_REMIX_AUTHOR_AVATAR = "remix_author_avatar"
        const val EXTRA_REMIX_AUTHOR_VERIFIED = "remix_author_verified"
    }

    private val vm: ChromaViewModel by viewModels()
    private lateinit var repository: WorkRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        enableEdgeToEdge()
        repository = WorkRepository(applicationContext)

        // If opened with an existing work, load it into the VM (only on first create)
        if (savedInstanceState == null) {
            val isHomeBackground = intent.getBooleanExtra(EXTRA_IS_HOME_BACKGROUND, false)
            vm.isHomeBackgroundMode = isHomeBackground

            val isPassCard = intent.getBooleanExtra(EXTRA_IS_PASS_CARD_MODE, false)
            vm.isPassCardMode = isPassCard
            if (isPassCard) {
                val displayName = intent.getStringExtra(EXTRA_PASS_DISPLAY_NAME) ?: "Creator"
                val username = intent.getStringExtra(EXTRA_PASS_USERNAME) ?: "creator"
                val avatar = intent.getStringExtra(EXTRA_PASS_AVATAR) ?: ""
                val role = intent.getStringExtra(EXTRA_PASS_ROLE)
                val verified = intent.getBooleanExtra(EXTRA_PASS_VERIFIED, false)
                val gender = intent.getStringExtra(EXTRA_PASS_GENDER)

                vm.passUserProfile = com.chroma.studio.model.UserProfile(
                    uid = intent.getStringExtra(EXTRA_PASS_USER_ID) ?: "",
                    displayName = displayName,
                    username = username,
                    avatarUrl = avatar,
                    role = role ?: "",
                    verified = verified,
                    gender = gender ?: ""
                )
                vm.setShape("rounded")

                val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val savedPassLayers = prefs.getString("custom_pass_card_layers", null)
                if (savedPassLayers != null && intent.getStringExtra(EXTRA_LAYERS_JSON) == null) {
                    val work = ChromaWork(
                        id = "pass_card_background",
                        name = "Profile Pass Background",
                        description = "",
                        layersJson = savedPassLayers,
                        canvasShape = "rounded"
                    )
                    vm.loadFromWork(work, repository)
                }
            }

            val workId = intent.getStringExtra(EXTRA_WORK_ID)
            val layersJson = intent.getStringExtra(EXTRA_LAYERS_JSON)
            
            if (isHomeBackground && layersJson != null) {
                // Load existing custom home background
                val work = ChromaWork(
                    id = "home_background",
                    name = "Home Background",
                    description = "",
                    layersJson = layersJson,
                    canvasShape = intent.getStringExtra(EXTRA_CANVAS_SHAPE) ?: "full"
                )
                vm.loadFromWork(work, repository)
            } else if (workId != null) {
                val work = ChromaWork(
                    id = workId,
                    name = intent.getStringExtra(EXTRA_WORK_NAME) ?: "Untitled",
                    description = intent.getStringExtra(EXTRA_WORK_DESCRIPTION) ?: "",
                    layersJson = layersJson ?: "[]",
                    canvasShape = intent.getStringExtra(EXTRA_CANVAS_SHAPE) ?: "rounded",
                    remixedFromPostId = intent.getStringExtra(EXTRA_REMIX_POST_ID),
                    remixedFromAuthorId = intent.getStringExtra(EXTRA_REMIX_AUTHOR_ID),
                    remixedFromAuthorUsername = intent.getStringExtra(EXTRA_REMIX_AUTHOR_USERNAME),
                    remixedFromAuthorDisplayName = intent.getStringExtra(EXTRA_REMIX_AUTHOR_DISPLAY_NAME),
                    remixedFromAuthorAvatar = intent.getStringExtra(EXTRA_REMIX_AUTHOR_AVATAR),
                    remixedFromAuthorVerified = intent.getBooleanExtra(EXTRA_REMIX_AUTHOR_VERIFIED, false)
                )
                vm.loadFromWork(work, repository)
            }
        }

        val workNameFromIntent = intent.getStringExtra(EXTRA_WORK_NAME) ?: "Untitled"

        setContent {
            OscellionTheme(darkTheme = vm.isDarkMode) {
                OscellionApp(vm, workNameFromIntent, repository)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Auto-save work whenever editor goes to background
        val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        vm.saveWork(repository, prefs)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OscellionApp(vm: ChromaViewModel, workName: String, repository: com.chroma.studio.data.WorkRepository) {
    val colors = LocalOscellionColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    // derivedStateOf prevents the entire OscellionApp from re-running just because
    // vm.layers changed — only recomposes when the found activeLayer object itself changes.
    val activeLayer by remember { derivedStateOf { vm.layers.find { it.id == vm.activeLayerId } } }
    val hazeState = LocalHazeState.current

    // Hoist blobDragOverrides above layout branch so it isn't duplicated
    val blobDragOverrides = remember { mutableStateMapOf<Int, Offset>() }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isDesktop = configuration.screenWidthDp > 800
    
    val initialShowAIGenerator = (context as? android.app.Activity)?.intent?.getBooleanExtra("EXTRA_START_AI_GENERATOR", false) == true
    var showAIGenerator by remember { mutableStateOf(initialShowAIGenerator) }
    var showPublishDialog by remember { mutableStateOf(false) }
    var publishStatus by remember { mutableStateOf(com.chroma.studio.ui.screens.ShareStatus.IDLE) }
    val publishScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {

        if (isDesktop) {
            // Desktop 3-column layout
            Row(modifier = Modifier.fillMaxSize().padding(top = 76.dp, bottom = 16.dp)) {
                // Left Panel: Global FX
                Box(modifier = Modifier.width(320.dp).fillMaxHeight().padding(start = 16.dp, end = 8.dp)) {
                    com.chroma.studio.ui.components.GlassPanel(modifier = Modifier.fillMaxSize(), cornerRadius = 16) {
                        com.chroma.studio.ui.components.GlobalFxPanel(vm = vm, modifier = Modifier.fillMaxSize())
                    }
                }

                // Center: Canvas Area
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().haze(hazeState),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        val allShapes = listOf("rounded" to "Card", "circle" to "Circle", "full" to "Full", "text" to "Text")
                        val shapes = if (vm.isHomeBackgroundMode) {
                            allShapes.filter { it.first == "full" }
                        } else {
                            allShapes
                        }
                        val shapesState = remember { shapes }
                        shapesState.forEach { (key, label) -> ShapePill(label, key == vm.canvasShape) { vm.setShape(key) } }
                    }

                    var reactOffset by remember { mutableStateOf(Offset.Zero) }
                    var boxSize by remember { mutableStateOf(IntSize.Zero) }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .onSizeChanged { boxSize = it }
                            .pointerInput(vm.mouseReactivity) {
                                if (!vm.mouseReactivity) {
                                    reactOffset = Offset.Zero
                                    return@pointerInput
                                }
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        val pos = event.changes.firstOrNull()?.position
                                        if (pos != null && boxSize.width > 0 && boxSize.height > 0) {
                                            val nx = pos.x / boxSize.width
                                            val ny = pos.y / boxSize.height
                                            reactOffset = Offset((nx - 0.5f) * -80f, (ny - 0.5f) * -80f)
                                        }
                                    } while (event.changes.any { it.pressed })
                                    reactOffset = Offset.Zero
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        CanvasPreview(
                            layers = vm.layers,
                            shape = vm.canvasShape,
                            borderColor = colors.glassBorder,
                            colorBlindMode = vm.colorBlindMode,
                            postFxMode = vm.postFxMode,
                            animStatus = vm.globalAnimStatus,
                            animStyle = vm.globalAnimStyle,
                            animSpeed = vm.globalAnimSpeed,
                            animAmount = vm.globalAnimAmount,
                            reactOffset = reactOffset,
                            blobDragOverrides = blobDragOverrides,
                            textContent = vm.textPreviewContent,
                            onTextContentChange = { vm.updateTextPreviewContent(it) }
                        )
                        val layer = activeLayer
                        if (layer != null && (layer.type == com.chroma.studio.model.LayerType.BLOB ||
                                layer.type == com.chroma.studio.model.LayerType.LIQUID ||
                                layer.type == com.chroma.studio.model.LayerType.MESH)) {
                            MeshBlobHandlesOverlay(
                                layer = layer,
                                onBlobTap = { idx -> vm.setActiveBlob(layer.id, idx) },
                                onBlobDrag = { idx, pos ->
                                    blobDragOverrides[idx] = pos  // local update only — no recompose of layer stack
                                },
                                onBlobDragEnd = { idx, finalPos ->
                                    vm.setBlobPosition(layer.id, idx, finalPos)
                                    blobDragOverrides.remove(idx)
                                },
                                onBlobScale = { idx, zoom ->
                                    val b = layer.blobs[idx]
                                    vm.updateBlobParam(layer.id, idx, "width", (b.width * zoom).coerceIn(10f, 300f))
                                    vm.updateBlobParam(layer.id, idx, "height", (b.height * zoom).coerceIn(10f, 300f))
                                },
                                onPointDrag = { idx, pos -> vm.setMeshPoint(layer.id, idx, pos) },
                                modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f)
                            )
                        }
                        if (vm.isPassCardMode && vm.passUserProfile != null) {
                            PassCardLiveOverlay(
                                profile = vm.passUserProfile!!,
                                userId = vm.passUserProfile!!.uid,
                                modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f)
                            )
                        }
                    }

                    if (vm.contrastCheckerEnabled) {
                        ContrastCheckerBadge(
                            layers = vm.layers,
                            onAutoFix = vm::autoFixContrast,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }

                // Right Panel: Layers
                Box(modifier = Modifier.width(320.dp).fillMaxHeight().padding(start = 8.dp, end = 16.dp)) {
                    com.chroma.studio.ui.components.LayersPanel(
                        layers = vm.layers,
                        activeLayerId = vm.activeLayerId,
                        vm = vm,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            ChromaAppBar(
                isDarkMode = vm.isDarkMode,
                onToggleDarkMode = vm::toggleDarkMode,
                onAddLayer = vm::addLayer,
                onRandomize = vm::randomize,
                canUndo = vm.canUndo,
                canRedo = vm.canRedo,
                onUndo = vm::undo,
                onRedo = vm::redo,
                onExport = vm::toggleExportModal,
                isRemix = vm.isRemix,
                onPublish = { showPublishDialog = true },
                onSave = { 
                    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    vm.saveWork(repository, prefs) 
                    if (vm.isPassCardMode) {
                        com.chroma.studio.ui.components.ToastManager.showToast("Profile Pass background applied!")
                    } else {
                        com.chroma.studio.ui.components.ToastManager.showToast("Project saved")
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

        } else {
            // Mobile layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(hazeState)
                    .padding(top = 76.dp, bottom = 76.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // mobile shape pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    val allShapes = listOf("rounded" to "Card", "circle" to "Circle", "full" to "Full", "text" to "Text")
                    val shapes = if (vm.isHomeBackgroundMode || vm.isPassCardMode) {
                        allShapes.filter { it.first == "rounded" || it.first == "full" }
                    } else {
                        allShapes
                    }
                    val shapesState = remember { shapes }
                    shapesState.forEach { (key, label) -> ShapePill(label, key == vm.canvasShape) { vm.setShape(key) } }
                }

                var reactOffset by remember { mutableStateOf(Offset.Zero) }
                var boxSize by remember { mutableStateOf(IntSize.Zero) }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .onSizeChanged { boxSize = it }
                        .pointerInput(vm.mouseReactivity) {
                            if (!vm.mouseReactivity) {
                                reactOffset = Offset.Zero
                                return@pointerInput
                            }
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val pos = event.changes.firstOrNull()?.position
                                    if (pos != null && boxSize.width > 0 && boxSize.height > 0) {
                                        val nx = pos.x / boxSize.width
                                        val ny = pos.y / boxSize.height
                                        reactOffset = Offset((nx - 0.5f) * -80f, (ny - 0.5f) * -80f)
                                    }
                                } while (event.changes.any { it.pressed })
                                reactOffset = Offset.Zero
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CanvasPreview(
                        layers = vm.layers,
                        shape = vm.canvasShape,
                        borderColor = colors.glassBorder,
                        colorBlindMode = vm.colorBlindMode,
                        postFxMode = vm.postFxMode,
                        animStatus = vm.globalAnimStatus,
                        animStyle = vm.globalAnimStyle,
                        animSpeed = vm.globalAnimSpeed,
                        animAmount = vm.globalAnimAmount,
                        reactOffset = reactOffset,
                        blobDragOverrides = blobDragOverrides,
                        textContent = vm.textPreviewContent,
                        onTextContentChange = { vm.updateTextPreviewContent(it) }
                    )
                    val layer = activeLayer
                    if (layer != null && (layer.type == com.chroma.studio.model.LayerType.BLOB ||
                            layer.type == com.chroma.studio.model.LayerType.LIQUID ||
                            layer.type == com.chroma.studio.model.LayerType.MESH)) {
                        MeshBlobHandlesOverlay(
                            layer = layer,
                            onBlobTap = { idx -> vm.setActiveBlob(layer.id, idx) },
                            onBlobDrag = { idx, pos ->
                                blobDragOverrides[idx] = pos
                            },
                            onBlobDragEnd = { idx, finalPos ->
                                vm.setBlobPosition(layer.id, idx, finalPos)
                                blobDragOverrides.remove(idx)
                            },
                            onBlobScale = { idx, zoom ->
                                val b = layer.blobs[idx]
                                vm.updateBlobParam(layer.id, idx, "width", (b.width * zoom).coerceIn(10f, 300f))
                                vm.updateBlobParam(layer.id, idx, "height", (b.height * zoom).coerceIn(10f, 300f))
                            },
                            onPointDrag = { idx, pos -> vm.setMeshPoint(layer.id, idx, pos) },
                            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f)
                        )
                    }

                    if (vm.isPassCardMode && vm.passUserProfile != null) {
                        PassCardLiveOverlay(
                            profile = vm.passUserProfile!!,
                            userId = vm.passUserProfile!!.uid,
                            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 2f)
                        )
                    }
                }

                if (vm.contrastCheckerEnabled) {
                    ContrastCheckerBadge(
                        layers = vm.layers,
                        onAutoFix = vm::autoFixContrast,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }

            ChromaAppBar(
                isDarkMode = vm.isDarkMode,
                onToggleDarkMode = vm::toggleDarkMode,
                onAddLayer = vm::addLayer,
                onRandomize = vm::randomize,
                canUndo = vm.canUndo,
                canRedo = vm.canRedo,
                onUndo = vm::undo,
                onRedo = vm::redo,
                onExport = vm::toggleExportModal,
                isRemix = vm.isRemix,
                onPublish = { showPublishDialog = true },
                onSave = { 
                    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    vm.saveWork(repository, prefs) 
                    if (vm.isPassCardMode) {
                        com.chroma.studio.ui.components.ToastManager.showToast("Profile Pass background applied!")
                    } else {
                        com.chroma.studio.ui.components.ToastManager.showToast("Project saved")
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            BottomDrawer(
                vm = vm,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        
        if (vm.showExportModal) {
            DeveloperHandoffModal(
                vm = vm,
                workName = workName,
                onClose = vm::toggleExportModal
            )
        }
        
        com.chroma.studio.ui.components.ChromaToastHost()
        
        if (showPublishDialog) {
            com.chroma.studio.ui.screens.ShareToDiscoverDialog(
                initialName = vm.currentWorkName,
                initialDescription = vm.currentWorkDescription,
                status = publishStatus,
                isVerified = false,
                onDismiss = { 
                    if (publishStatus != com.chroma.studio.ui.screens.ShareStatus.PUBLISHING) {
                        showPublishDialog = false
                        publishStatus = com.chroma.studio.ui.screens.ShareStatus.IDLE
                    }
                },
                onConfirm = { title, desc, aspectRatio ->
                    publishStatus = com.chroma.studio.ui.screens.ShareStatus.PUBLISHING
                    publishScope.launch {
                        try {
                            // Save the current state before publishing
                            val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            val savedWork = vm.saveWork(repository, prefs)
                            if (savedWork != null) {
                                val workToPublish = savedWork.copy(name = title, description = desc)
                                val discoverRepo = com.chroma.studio.data.DiscoverRepository()
                                discoverRepo.publishWork(workToPublish, aspectRatio)
                                publishStatus = com.chroma.studio.ui.screens.ShareStatus.SUCCESS
                            } else {
                                publishStatus = com.chroma.studio.ui.screens.ShareStatus.ERROR
                            }
                        } catch (e: Exception) {
                            publishStatus = com.chroma.studio.ui.screens.ShareStatus.ERROR
                            com.chroma.studio.ui.components.ToastManager.showToast("Failed: ${e.localizedMessage}")
                        }
                    }
                }
            )
        }
        
        if (showAIGenerator) {
            com.chroma.studio.ui.screens.AIGenerateModal(
                onDismiss = { showAIGenerator = false },
                onApplyLayers = { generatedLayers ->
                    vm.setLayersFromAI(generatedLayers)
                }
            )
        }
    }
}

@Composable
private fun ShapePill(label: String, active: Boolean, onClick: () -> Unit) {
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
            .clickable(indication = null, interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }) { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun PassCardLiveOverlay(
    profile: com.chroma.studio.model.UserProfile,
    userId: String,
    modifier: Modifier = Modifier
) {
    val profileShareUrl = "https://chromastudio.app/u/${profile.username.ifBlank { userId }}"
    val qrBitmap = remember(profileShareUrl) {
        com.chroma.studio.ui.components.generateQrBitmap(profileShareUrl, sizePx = 280, foregroundColor = android.graphics.Color.BLACK)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Avatar + Name + Role + Username
            Column(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f))
                        .border(1.5.dp, androidx.compose.ui.graphics.Color.White, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val presetRes = remember(profile.avatarUrl) { getAvatarPresetDrawableRes(profile.avatarUrl) }
                    val defaultAvatarRes = remember(profile.gender) { getDefaultAvatarDrawableRes(profile.gender) }
                    val resolvedDrawable = presetRes ?: if (profile.avatarUrl.isBlank()) defaultAvatarRes else null

                    if (resolvedDrawable != null) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = resolvedDrawable),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else if (profile.avatarUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.default_avatar_apple),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }

                // Name & Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = profile.displayName.ifBlank { "Creator" },
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (profile.verified) {
                        androidx.compose.material3.Icon(
                            imageVector = com.composables.icons.lucide.Lucide.BadgeCheck,
                            contentDescription = "Verified",
                            tint = androidx.compose.ui.graphics.Color(0xFF007AFF),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Role
                val roleText = profile.role
                if (!roleText.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            com.composables.icons.lucide.Lucide.Sparkles,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(9.5.dp)
                        )
                        Text(
                            text = roleText,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "@${profile.username.ifBlank { "creator" }}",
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Right: QR Code badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (qrBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(54.dp)
                        )
                    } else {
                        androidx.compose.material3.Icon(
                            com.composables.icons.lucide.Lucide.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    Text(
                        text = "CONNECT",
                        color = androidx.compose.ui.graphics.Color(0xFF86868B),
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Bottom Brand
        Text(
            text = "CHROMA STUDIO • CREATOR PASS",
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.65f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
