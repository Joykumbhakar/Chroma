package com.chroma.studio.ui.screens

import androidx.compose.runtime.Composable
import com.chroma.studio.viewmodel.StoryViewModel
import kotlinx.serialization.Serializable

@Serializable
data class StatusFormatState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStroke: Boolean = false,
    val isHighlight: Boolean = false,
    val headingLevel: Int = 1, // 1 = Normal, 2 = Large/H1, 3 = H2
    val textColorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#FF1F1C2C", // Default background
    val title: String = "",
    val tag: String = "",
    val mood: String = "",
    val fontFamily: String = "Sans", // "Sans", "Serif", "Mono", "Cursive"
    val textAlign: String = "left", // "left", "center", "right"
    val isQuote: Boolean = false,
    val gradientPreset: String = "midnight",
    val highlightColorHex: String = "#FFD60A"
)

/**
 * StatusEditorScreen delegates to the modern full-screen Blog Writer StoryEditorScreen.
 */
@Composable
fun StatusEditorScreen(
    storyViewModel: StoryViewModel,
    onNavigateBack: () -> Unit,
    onStatusPosted: () -> Unit
) {
    StoryEditorScreen(
        storyViewModel = storyViewModel,
        onNavigateBack = onNavigateBack,
        onStoryPosted = onStatusPosted
    )
}
