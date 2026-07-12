package com.chroma.studio.model

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import java.util.UUID

// ──────────────────────────────────────────────────────
// Persisted work (saved in SharedPreferences)
// ──────────────────────────────────────────────────────
data class ChromaWork(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val layersJson: String = "[]",  // Gson-serialized List<GradientLayer>
    val canvasShape: String = "rounded",
    val isHomeBackground: Boolean = false,
    val isDeleted: Boolean = false
)

enum class LayerType(val label: String, val icon: String) {
    LINEAR("Linear", "ph-arrow-up-right"),
    RADIAL("Radial", "ph-circle"),
    CONIC("Conic", "ph-cone"),
    MESH("Mesh", "ph-grid-four"),
    BLOB("Blob", "ph-drop"),
    AURORA("Aurora", "ph-sparkle"),
    LIQUID("Liquid", "ph-drop")
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

enum class ColorBlindMode(val label: String) {
    NONE("Normal Vision"),
    PROTANOPIA("Protanopia (Red-Blind)"),
    DEUTERANOPIA("Deuteranopia (Green-Blind)"),
    TRITANOPIA("Tritanopia (Blue-Blind)")
}

enum class PostProcessingFx(val label: String) {
    NONE("None (Clean)"),
    GRAIN("Film Grain"),
    HALFTONE("Retro Halftone"),
    DITHER("Brutalist Dither")
}

enum class AnimStyle(val label: String) {
    DRIFT("Drift (Pan Position)"),
    PULSE("Pulse (Scale & Opacity)"),
    HUE("Hue Shift (Color Rotation)"),
    BREATHE("Breathe (Size Expansion)")
}

enum class AnimStatus { STOPPED, PLAYING, PAUSED }


data class ColorStop(
    val id: String = UUID.randomUUID().toString(),
    val color: Color,
    val position: Float // 0f..100f, matches `pos` in the JS stop model
)

/**
 * Mirrors the JS blob object: { x, y, width, height, feather, opacity }
 * x/y are % positions (0..100), width/height are % of canvas size (10..200),
 * feather is 0..100, opacity is 0..1, rotation is degrees.
 */
data class BlobPoint(
    val x: Float = 50f,
    val y: Float = 50f,
    val width: Float = 40f,
    val height: Float = 40f,
    val feather: Float = 100f,
    val opacity: Float = 1f,
    val rotation: Float = 0f    // degrees, applied via canvas transform
)

data class GradientLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: LayerType = LayerType.LINEAR,
    val blendMode: ChromaBlendMode = ChromaBlendMode.NORMAL,
    val opacity: Float = 1f,       // 0..1
    val angle: Float = 90f,        // degrees, used for LINEAR
    val centerX: Float = 50f,      // % , used for RADIAL/CONIC
    val centerY: Float = 50f,      // %
    val visible: Boolean = true,
    val expanded: Boolean = false,
    val animated: Boolean = false,
    val animSpeed: Float = 50f,     // 1..100
    val animIntensity: Float = 50f, // 0..100
    val repeatPattern: Boolean = false,
    val width: Float = 70f,         // for RADIAL width
    val height: Float = 70f,        // for RADIAL height
    val feather: Float = 100f,      // global fallback
    val waveSpeed: Float = 50f,
    val complexity: Float = 50f,
    val brightness: Float = 50f,
    val columns: Int = 3,
    val rows: Int = 3,
    val hasBaseBackground: Boolean = false,
    val blobBgColor: Color = Color.Black,
    val activeBlobIdx: Int = 0,
    // Per-blob settings matching the JS { x, y, width, height, feather, opacity } structure
    val blobs: List<BlobPoint> = listOf(
        BlobPoint(x = 25f, y = 40f, width = 40f, height = 40f),
        BlobPoint(x = 75f, y = 60f, width = 40f, height = 40f)
    ),
    // Mesh control points (non-blob layers still use this)
    val meshPoints: List<androidx.compose.ui.geometry.Offset> = listOf(
        androidx.compose.ui.geometry.Offset(25f, 40f),
        androidx.compose.ui.geometry.Offset(75f, 60f)
    ),
    val stops: List<ColorStop> = listOf(
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
