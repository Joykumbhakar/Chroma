package com.chroma.studio.model

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class LayerType(val label: String, val icon: String) {
    LINEAR("Linear", "ph-arrow-up-right"),
    RADIAL("Radial", "ph-circle"),
    CONIC("Conic", "ph-cone"),
    MESH("Mesh", "ph-grid-four"),
    BLOB("Blob", "ph-drop"),
    AURORA("Aurora", "ph-sparkle")
}

enum class ChromaBlendMode(val label: String, val compose: BlendMode) {
    NORMAL("Normal", BlendMode.SrcOver),
    MULTIPLY("Multiply", BlendMode.Multiply),
    SCREEN("Screen", BlendMode.Screen),
    OVERLAY("Overlay", BlendMode.Overlay),
    DARKEN("Darken", BlendMode.Darken),
    LIGHTEN("Lighten", BlendMode.Lighten),
    COLOR_DODGE("Color Dodge", BlendMode.ColorDodge),
    DIFFERENCE("Difference", BlendMode.Difference)
}

data class ColorStop(
    val id: String = UUID.randomUUID().toString(),
    val color: Color,
    val position: Float // 0f..100f, matches `pos` in the JS stop model
)

data class GradientLayer(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var type: LayerType = LayerType.LINEAR,
    var blendMode: ChromaBlendMode = ChromaBlendMode.NORMAL,
    var opacity: Float = 1f,       // 0..1
    var angle: Float = 90f,        // degrees, used for LINEAR
    var centerX: Float = 50f,      // % , used for RADIAL/CONIC/MESH/BLOB
    var centerY: Float = 50f,      // %
    var visible: Boolean = true,
    var expanded: Boolean = false,
    var stops: List<ColorStop> = listOf(
        ColorStop(color = Color(0xFF4F46E5), position = 0f),
        ColorStop(color = Color(0xFF818CF8), position = 100f)
    )
)

// Apple-style presets, ported from the `preset-grid` data in index.html's JS
data class GradientPreset(val name: String, val colors: List<Color>, val type: LayerType)

val ApplePresets = listOf(
    GradientPreset("Sunset", listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)), LayerType.LINEAR),
    GradientPreset("Ocean", listOf(Color(0xFF2E3192), Color(0xFF1BFFFF)), LayerType.LINEAR),
    GradientPreset("Candy", listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)), LayerType.LINEAR),
    GradientPreset("Mint", listOf(Color(0xFF00B09B), Color(0xFF96C93D)), LayerType.LINEAR),
    GradientPreset("Peach", listOf(Color(0xFFFFECD2), Color(0xFFFCB69F)), LayerType.RADIAL),
    GradientPreset("Aurora", listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)), LayerType.AURORA),
    GradientPreset("Grape", listOf(Color(0xFF7F00FF), Color(0xFFE100FF)), LayerType.CONIC),
    GradientPreset("Fire", listOf(Color(0xFFF83600), Color(0xFFF9D423)), LayerType.LINEAR)
)
