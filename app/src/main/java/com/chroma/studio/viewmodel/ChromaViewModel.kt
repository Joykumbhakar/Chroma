package com.chroma.studio.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.palette.graphics.Palette
import com.chroma.studio.model.AnimStatus
import com.chroma.studio.model.AnimStyle
import com.chroma.studio.model.ChromaBlendMode
import com.chroma.studio.model.ColorBlindMode
import com.chroma.studio.model.ColorStop
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.model.LayerType
import com.chroma.studio.model.PostProcessingFx
import kotlin.random.Random

enum class MobileTab { LAYERS, GLOBAL_FX }
enum class DrawerLevel { COLLAPSED, MID, FULL }

/**
 * Mirrors the responsibilities of the `ChromaApp` / layer-stack logic in index.html:
 * addLayer(), randomize(), delete/select/reorder layers, canvas shape + dark mode toggle,
 * plus history.save() (undo/redo), the mobile tab switcher, and global FX / color-blind state.
 *
 * `layers` is a Compose `SnapshotStateList` and `GradientLayer` is fully immutable — every
 * mutation replaces an item via `.copy()` rather than mutating a field in place, so the
 * Compose snapshot system actually observes the change and recomposes. (A plain `MutableList`
 * of a mutable data class would silently fail to trigger recomposition on field writes.)
 */
class ChromaViewModel : ViewModel() {

    var isDarkMode by mutableStateOf(false)
        private set

    val layers = mutableStateListOf(
        GradientLayer(
            name = "Base",
            type = LayerType.LINEAR,
            angle = 135f,
            stops = listOf(
                ColorStop(color = Color(0xFF4F46E5), position = 0f),
                ColorStop(color = Color(0xFF818CF8), position = 100f)
            ),
            expanded = true
        )
    )

    var activeLayerId by mutableStateOf(layers.first().id)
        private set

    // "Card" / "Circle" / "Full" / "Text" — matches the mobile shape pills (#m-shape-*)
    var canvasShape by mutableStateOf("rounded")
        private set

    var mobileTab by mutableStateOf(MobileTab.LAYERS)
        private set

    var drawerLevel by mutableStateOf(DrawerLevel.MID)
        private set

    var colorBlindMode by mutableStateOf(ColorBlindMode.NONE)
        private set

    var contrastCheckerEnabled by mutableStateOf(false)
        private set

    var postFxMode by mutableStateOf(PostProcessingFx.NONE)
        private set

    val halftoneEnabled: Boolean get() = postFxMode == PostProcessingFx.HALFTONE
    val ditherEnabled: Boolean get() = postFxMode == PostProcessingFx.DITHER

    var mouseReactivity by mutableStateOf(false)
        private set

    var globalAnimStatus by mutableStateOf(AnimStatus.STOPPED)
        private set

    var globalAnimStyle by mutableStateOf(AnimStyle.DRIFT)
        private set

    var globalAnimSpeed by mutableStateOf(50f)
        private set

    var globalAnimAmount by mutableStateOf(50f)
        private set

    var promptText by mutableStateOf("")
        private set

    var textPreviewContent by mutableStateOf("CHROMA")
        private set

    // id of the layer pending delete confirmation, null when the modal is hidden
    var pendingDeleteLayerId by mutableStateOf<String?>(null)
        private set

    var dontAskDeleteAgain by mutableStateOf(false)
        private set

    var showExportModal by mutableStateOf(false)
        private set

    // ---- history.save() port: simple undo/redo over full layer-stack snapshots ----
    private val undoStack = ArrayDeque<List<GradientLayer>>()
    private val redoStack = ArrayDeque<List<GradientLayer>>()
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    private fun snapshot(): List<GradientLayer> = layers.toList()

    private fun saveHistory() {
        undoStack.addLast(snapshot())
        if (undoStack.size > 30) undoStack.removeFirst()
        redoStack.clear()
        canUndo = undoStack.isNotEmpty()
        canRedo = false
    }

    private fun replaceAllLayers(newLayers: List<GradientLayer>) {
        layers.clear()
        layers.addAll(newLayers)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(snapshot())
        replaceAllLayers(undoStack.removeLast())
        if (layers.none { it.id == activeLayerId }) activeLayerId = layers.first().id
        canUndo = undoStack.isNotEmpty()
        canRedo = true
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(snapshot())
        replaceAllLayers(redoStack.removeLast())
        if (layers.none { it.id == activeLayerId }) activeLayerId = layers.first().id
        canUndo = true
        canRedo = redoStack.isNotEmpty()
    }

    fun toggleDarkMode() { isDarkMode = !isDarkMode }

    fun setShape(shape: String) { canvasShape = shape }

    fun switchMobileTab(tab: MobileTab) { mobileTab = tab }

    fun updateDrawerLevel(level: DrawerLevel) { drawerLevel = level }

    fun cycleDrawerLevel() {
        drawerLevel = when (drawerLevel) {
            DrawerLevel.COLLAPSED -> DrawerLevel.MID
            DrawerLevel.MID -> DrawerLevel.FULL
            DrawerLevel.FULL -> DrawerLevel.COLLAPSED
        }
    }

    fun updateColorBlindMode(mode: ColorBlindMode) { colorBlindMode = mode }

    fun toggleContrastChecker() { contrastCheckerEnabled = !contrastCheckerEnabled }

    fun updatePostFxMode(mode: PostProcessingFx) { postFxMode = mode }
    
    fun toggleMouseReactivity(enabled: Boolean) { mouseReactivity = enabled }
    fun setAnimStatus(status: AnimStatus) { globalAnimStatus = status }
    fun setAnimStyle(style: AnimStyle) { globalAnimStyle = style }
    fun updateGlobalAnimSpeed(speed: Float) { globalAnimSpeed = speed }
    fun updateGlobalAnimAmount(amount: Float) { globalAnimAmount = amount }
    fun updatePromptText(text: String) { promptText = text }
    fun updateTextPreviewContent(text: String) { textPreviewContent = text }

    fun selectLayer(id: String) { activeLayerId = id }

    fun toggleExpanded(id: String) {
        updateLayer(id) { it.copy(expanded = !it.expanded) }
    }

    fun addLayer() {
        saveHistory()
        val n = layers.size + 1
        val newLayer = GradientLayer(
            name = "Layer $n",
            type = LayerType.LINEAR,
            stops = listOf(
                ColorStop(color = randomColor(), position = 0f),
                ColorStop(color = randomColor(), position = 100f)
            ),
            expanded = true
        )
        layers.add(0, newLayer)
        activeLayerId = newLayer.id
    }

    fun showDeleteLayerConfirmation(layerId: String) {
        if (dontAskDeleteAgain) {
            deleteLayer(layerId)
        } else {
            pendingDeleteLayerId = layerId
        }
    }

    fun cancelDeleteLayer() { pendingDeleteLayerId = null }

    fun confirmDeleteLayer() {
        pendingDeleteLayerId?.let { deleteLayer(it) }
        pendingDeleteLayerId = null
    }

    fun updateDontAskDeleteAgain(value: Boolean) {
        dontAskDeleteAgain = value
    }

    fun toggleExportModal() {
        showExportModal = !showExportModal
    }

    fun deleteLayer(id: String) {
        if (layers.size <= 1) return // JS keeps at least one layer
        saveHistory()
        layers.removeAll { it.id == id }
        if (activeLayerId == id) activeLayerId = layers.first().id
    }

    /**
     * Replaces the layer with `id` by applying [block] to an immutable copy and writing the
     * result back into the SnapshotStateList at its index — this index-based `set()` is what
     * Compose's snapshot system actually observes, unlike mutating a field on the old instance.
     */
    fun updateLayer(id: String, recordHistory: Boolean = false, block: (GradientLayer) -> GradientLayer) {
        val index = layers.indexOfFirst { it.id == id }
        if (index < 0) return
        if (recordHistory) saveHistory()
        layers[index] = block(layers[index])
    }

    fun moveLayer(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in layers.indices || toIndex !in layers.indices) return
        val item = layers.removeAt(fromIndex)
        layers.add(toIndex, item)
    }

    fun applyPreset(colors: List<Color>, type: LayerType) {
        updateLayer(activeLayerId, recordHistory = true) { layer ->
            layer.copy(
                type = type,
                stops = colors.mapIndexed { i, c ->
                    ColorStop(color = c, position = if (colors.size == 1) 0f else i * 100f / (colors.size - 1))
                }
            )
        }
    }

    fun setAnimSpeed(id: String, value: Float) = updateLayer(id) { it.copy(animSpeed = value) }
    fun setAnimIntensity(id: String, value: Float) = updateLayer(id) { it.copy(animIntensity = value) }
    fun setAnimated(id: String, value: Boolean) = updateLayer(id, recordHistory = true) { it.copy(animated = value) }

    fun setMeshPoint(layerId: String, pointIndex: Int, newPos: Offset) {
        updateLayer(layerId) { layer ->
            val updated = layer.meshPoints.toMutableList()
            if (pointIndex in updated.indices) updated[pointIndex] = newPos
            layer.copy(meshPoints = updated)
        }
    }

    // ---- Blob engine methods — mirrors app.setActiveBlob / addBlob / removeBlob / updateBlobParams ----

    fun setActiveBlob(layerId: String, idx: Int) {
        updateLayer(layerId) { it.copy(activeBlobIdx = idx) }
    }

    fun addBlob(layerId: String) {
        saveHistory()
        updateLayer(layerId) { layer ->
            val newBlob = com.chroma.studio.model.BlobPoint(
                x = 20f + Random.nextFloat() * 60f,
                y = 20f + Random.nextFloat() * 60f,
                width = 30f + Random.nextFloat() * 50f,
                height = 30f + Random.nextFloat() * 50f,
                feather = 100f,
                opacity = 1f
            )
            val newStop = com.chroma.studio.model.ColorStop(
                color = randomColor(),
                position = 100f
            )
            layer.copy(
                blobs = layer.blobs + newBlob,
                stops = layer.stops + newStop,
                activeBlobIdx = layer.blobs.size
            )
        }
    }

    fun removeBlob(layerId: String, idx: Int) {
        val layer = layers.firstOrNull { it.id == layerId } ?: return
        if (layer.blobs.size <= 1) return
        saveHistory()
        updateLayer(layerId) { l ->
            val newBlobs = l.blobs.toMutableList().also { it.removeAt(idx) }
            val newStops = l.stops.toMutableList().also { if (idx in it.indices) it.removeAt(idx) }
            l.copy(
                blobs = newBlobs,
                stops = newStops,
                activeBlobIdx = maxOf(0, l.activeBlobIdx - 1).coerceAtMost(newBlobs.size - 1)
            )
        }
    }

    /** Update a single blob's position (called by drag handles) */
    fun setBlobPosition(layerId: String, blobIdx: Int, newPos: Offset) {
        updateLayer(layerId) { layer ->
            val updated = layer.blobs.toMutableList()
            if (blobIdx in updated.indices) {
                updated[blobIdx] = updated[blobIdx].copy(x = newPos.x, y = newPos.y)
            }
            layer.copy(blobs = updated, activeBlobIdx = blobIdx)
        }
    }

    /** Update a per-blob property (width, height, feather, opacity, rotation) */
    fun updateBlobParam(layerId: String, blobIdx: Int, prop: String, value: Float) {
        updateLayer(layerId) { layer ->
            val updated = layer.blobs.toMutableList()
            if (blobIdx in updated.indices) {
                val b = updated[blobIdx]
                updated[blobIdx] = when (prop) {
                    "width"    -> b.copy(width = value)
                    "height"   -> b.copy(height = value)
                    "feather"  -> b.copy(feather = value)
                    "opacity"  -> b.copy(opacity = value)
                    "rotation" -> b.copy(rotation = value)
                    else -> b
                }
            }
            layer.copy(blobs = updated)
        }
    }


    fun regenerateMesh(layerId: String) {
        updateLayer(layerId) { layer ->
            val random = java.util.Random()
            val newPoints = List(layer.columns * layer.rows) {
                androidx.compose.ui.geometry.Offset(random.nextFloat() * 100f, random.nextFloat() * 100f)
            }
            layer.copy(meshPoints = newPoints)
        }
    }


    /**
     * Port of autoFixContrast(): nudges the frontmost layer's first stop toward black/white
     * in steps until it clears the 4.5:1 AA threshold against the backmost layer's last stop,
     * matching the wand button (#btn-fix-contrast) in index.html.
     */
    fun autoFixContrast() {
        val front = layers.lastOrNull() ?: return
        val backColor = layers.firstOrNull()?.stops?.lastOrNull()?.color ?: Color.Black
        val frontStop = front.stops.firstOrNull() ?: return

        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(frontStop.color.toArgb(), hsv)
        val backLum = 0.2126 * backColor.red + 0.7152 * backColor.green + 0.0722 * backColor.blue
        val towardWhite = backLum < 0.5

        var value = hsv[2]
        for (i in 0 until 20) {
            val candidate = Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], value)))
            val ratio = com.chroma.studio.ui.components.contrastRatio(candidate, backColor)
            if (ratio >= 4.5) break
            value = (value + if (towardWhite) 0.05f else -0.05f).coerceIn(0f, 1f)
        }
        val fixed = Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], value)))

        updateLayer(front.id, recordHistory = true) { layer ->
            layer.copy(stops = layer.stops.map { if (it.id == frontStop.id) it.copy(color = fixed) else it })
        }
    }

    // Direct port of _performRandomize() in index.html
    fun randomize() {
        saveHistory()
        val types = LayerType.entries.filter { it != LayerType.MESH } // mesh gizmo simplified, see README
        val blends = listOf(ChromaBlendMode.NORMAL, ChromaBlendMode.MULTIPLY, ChromaBlendMode.SCREEN, ChromaBlendMode.OVERLAY)
        val n = 2 + Random.nextInt(2)
        val newLayers = (0 until n).map { i ->
            GradientLayer(
                name = "Mix ${i + 1}",
                type = types.random(),
                blendMode = if (i == 0) ChromaBlendMode.NORMAL else blends.random(),
                stops = listOf(
                    ColorStop(color = randomColor(), position = 0f),
                    ColorStop(color = randomColor(), position = 50f),
                    ColorStop(color = randomColor(), position = 100f)
                )
            )
        }
        replaceAllLayers(newLayers)
        activeLayerId = newLayers.first().id
    }

    private fun randomColor() = Color(
        red = Random.nextFloat(),
        green = Random.nextFloat(),
        blue = Random.nextFloat(),
        alpha = 1f
    )

    fun generateAiPalette(prompt: String) {
        if (prompt.isBlank()) return
        saveHistory()
        val lowerPrompt = prompt.lowercase()
        val colors = when {
            "cyberpunk" in lowerPrompt || "neon" in lowerPrompt -> listOf(Color(0xFFFF007F), Color(0xFF00FFFF), Color(0xFF7F00FF))
            "sunset" in lowerPrompt -> listOf(Color(0xFFFF512F), Color(0xFFF09819), Color(0xFFDD2476))
            "forest" in lowerPrompt || "nature" in lowerPrompt -> listOf(Color(0xFF11998E), Color(0xFF38EF7D), Color(0xFF006400))
            "ocean" in lowerPrompt || "sea" in lowerPrompt -> listOf(Color(0xFF2E3192), Color(0xFF1BFFFF), Color(0xFF00008B))
            "fire" in lowerPrompt || "flame" in lowerPrompt -> listOf(Color(0xFFF83600), Color(0xFFF9D423))
            "space" in lowerPrompt || "galaxy" in lowerPrompt -> listOf(Color(0xFF000000), Color(0xFF434343), Color(0xFF8A2387), Color(0xFFE94057))
            else -> listOf(randomColor(), randomColor(), randomColor())
        }
        
        val newLayer = GradientLayer(
            name = "AI: ${prompt.take(10)}",
            type = LayerType.AURORA,
            stops = colors.mapIndexed { i, c ->
                ColorStop(color = c, position = if (colors.size <= 1) 0f else i * 100f / (colors.size - 1))
            },
            expanded = true
        )
        layers.add(0, newLayer)
        activeLayerId = newLayer.id
    }

    fun generatePaletteFromImage(bitmap: android.graphics.Bitmap) {
        androidx.palette.graphics.Palette.from(bitmap).generate { palette ->
            palette?.let {
                val extractedColors = mutableListOf<Color>()
                it.vibrantSwatch?.rgb?.let { rgb -> extractedColors.add(Color(rgb)) }
                it.darkVibrantSwatch?.rgb?.let { rgb -> extractedColors.add(Color(rgb)) }
                it.lightVibrantSwatch?.rgb?.let { rgb -> extractedColors.add(Color(rgb)) }
                it.mutedSwatch?.rgb?.let { rgb -> extractedColors.add(Color(rgb)) }
                it.darkMutedSwatch?.rgb?.let { rgb -> extractedColors.add(Color(rgb)) }
                
                if (extractedColors.isEmpty()) {
                    extractedColors.addAll(listOf(randomColor(), randomColor()))
                }
                
                saveHistory()
                val takenColors = extractedColors.take(4)
                val newLayer = GradientLayer(
                    name = "Extracted Image",
                    type = LayerType.MESH,
                    stops = takenColors.mapIndexed { i, c ->
                        ColorStop(color = c, position = if (takenColors.size <= 1) 0f else i * 100f / (takenColors.size - 1))
                    },
                    expanded = true
                )
                layers.add(0, newLayer)
                activeLayerId = newLayer.id
            }
        }
    }
}
