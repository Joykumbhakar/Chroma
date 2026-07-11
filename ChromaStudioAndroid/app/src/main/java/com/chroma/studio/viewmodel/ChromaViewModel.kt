package com.chroma.studio.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.chroma.studio.model.ChromaBlendMode
import com.chroma.studio.model.ColorStop
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.model.LayerType
import kotlin.random.Random

/**
 * Mirrors the responsibilities of the `ChromaApp` / layer-stack logic in index.html:
 * addLayer(), randomize(), delete/select/reorder layers, canvas shape + dark mode toggle.
 */
class ChromaViewModel : ViewModel() {

    var isDarkMode by mutableStateOf(false)
        private set

    var layers = mutableListOf(
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
        private set

    var activeLayerId by mutableStateOf(layers.first().id)
        private set

    // "Card" / "Circle" / "Full" / "Text" — matches the mobile shape pills (#m-shape-*)
    var canvasShape by mutableStateOf("rounded")
        private set

    private var refreshTick by mutableStateOf(0)
    private fun bump() { refreshTick++ }

    fun toggleDarkMode() { isDarkMode = !isDarkMode }

    fun setShape(shape: String) { canvasShape = shape }

    fun selectLayer(id: String) { activeLayerId = id }

    fun toggleExpanded(id: String) {
        layers.find { it.id == id }?.let { it.expanded = !it.expanded }
        bump()
    }

    fun addLayer() {
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
        bump()
    }

    fun deleteLayer(id: String) {
        if (layers.size <= 1) return // JS keeps at least one layer
        layers.removeAll { it.id == id }
        if (activeLayerId == id) activeLayerId = layers.first().id
        bump()
    }

    fun updateLayer(id: String, block: (GradientLayer) -> Unit) {
        layers.find { it.id == id }?.let(block)
        bump()
    }

    fun moveLayer(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val item = layers.removeAt(fromIndex)
        layers.add(toIndex, item)
        bump()
    }

    // Direct port of _performRandomize() in index.html
    fun randomize() {
        val types = LayerType.entries.filter { it != LayerType.MESH } // mesh gizmo not modeled here
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
        }.toMutableList()
        layers = newLayers
        activeLayerId = newLayers.first().id
        bump()
    }

    private fun randomColor() = Color(
        red = Random.nextFloat(),
        green = Random.nextFloat(),
        blue = Random.nextFloat(),
        alpha = 1f
    )
}
