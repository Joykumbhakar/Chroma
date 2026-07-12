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
import com.chroma.studio.ui.theme.ChromaTheme
import com.chroma.studio.ui.theme.LocalChromaColors
import com.chroma.studio.viewmodel.ChromaViewModel

import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.ChromaWork

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_WORK_ID = "work_id"
        const val EXTRA_WORK_NAME = "work_name"
        const val EXTRA_WORK_DESCRIPTION = "work_description"
        const val EXTRA_LAYERS_JSON = "layers_json"
        const val EXTRA_CANVAS_SHAPE = "canvas_shape"
        const val EXTRA_IS_HOME_BACKGROUND = "is_home_background"
    }

    private val vm: ChromaViewModel by viewModels()
    private lateinit var repository: WorkRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = WorkRepository(applicationContext)

        // If opened with an existing work, load it into the VM (only on first create)
        if (savedInstanceState == null) {
            val isHomeBackground = intent.getBooleanExtra(EXTRA_IS_HOME_BACKGROUND, false)
            vm.isHomeBackgroundMode = isHomeBackground

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
                    canvasShape = intent.getStringExtra(EXTRA_CANVAS_SHAPE) ?: "rounded"
                )
                vm.loadFromWork(work, repository)
            }
        }

        val workNameFromIntent = intent.getStringExtra(EXTRA_WORK_NAME) ?: "Untitled"

        setContent {
            ChromaTheme(darkTheme = vm.isDarkMode) {
                ChromaStudioApp(vm, workNameFromIntent, repository)
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


@Composable
fun ChromaStudioApp(vm: ChromaViewModel, workName: String, repository: com.chroma.studio.data.WorkRepository) {
    val colors = LocalChromaColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    // derivedStateOf prevents the entire ChromaStudioApp from re-running just because
    // vm.layers changed — only recomposes when the found activeLayer object itself changes.
    val activeLayer by remember { derivedStateOf { vm.layers.find { it.id == vm.activeLayerId } } }
    val hazeState = LocalHazeState.current

    // Hoist blobDragOverrides above layout branch so it isn't duplicated
    val blobDragOverrides = remember { mutableStateMapOf<Int, Offset>() }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isDesktop = configuration.screenWidthDp > 800
    
    val initialShowAIGenerator = (context as? android.app.Activity)?.intent?.getBooleanExtra("EXTRA_START_AI_GENERATOR", false) == true
    var showAIGenerator by remember { mutableStateOf(initialShowAIGenerator) }

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
                    val shapes = if (vm.isHomeBackgroundMode) {
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
                onSave = { 
                    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    vm.saveWork(repository, prefs) 
                    com.chroma.studio.ui.components.ToastManager.showToast("Project saved")
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
    val colors = LocalChromaColors.current
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
