package com.chroma.studio.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

/**
 * Aspect ratio configurations for photo editing.
 */
enum class PhotoAspectRatio(val label: String, val ratio: Float?, val subtitle: String) {
    FREE("Original", null, "Uncropped Native"),
    SQUARE("1:1", 1f, "Square Feed Post"),
    PORTRAIT_4_5("4:5", 4f / 5f, "Instagram Portrait"),
    STORY_9_16("9:16", 9f / 16f, "Story / Flash Fullscreen"),
    LANDSCAPE_16_9("16:9", 16f / 9f, "Widescreen / Banner"),
    PORTRAIT_3_4("3:4", 3f / 4f, "Classic Social Post")
}

/**
 * FlashPhotoEditorScreen delegator.
 * Delegates all photo editing to the flagship unified ChromaPhotoEditorScreen.
 * Eliminates ~2,400 lines of duplicate editor code to keep the app lightweight and consistent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashPhotoEditorScreen(
    photoUris: List<Uri>,
    initialSlideIndex: Int = 0,
    title: String = "Edit Photo",
    initialAspectRatio: PhotoAspectRatio = PhotoAspectRatio.FREE,
    nextButtonLabel: String = "Next",
    onNext: (filterName: String, thumbnailBitmap: Bitmap?, durationPerSlideMs: Long) -> Unit = { _, _, _ -> },
    onExportUris: (List<Uri>) -> Unit = {},
    onSaveDraft: (filterName: String) -> Unit = {},
    onBack: () -> Unit
) {
    ChromaPhotoEditorScreen(
        photoUri = photoUris.getOrNull(initialSlideIndex) ?: Uri.EMPTY,
        photoUris = photoUris,
        initialIndex = initialSlideIndex,
        exportButtonLabel = nextButtonLabel,
        onExportSuccess = { singleUri ->
            onExportUris(listOf(singleUri))
        },
        onExportAllSuccess = { allUris ->
            onExportUris(allUris)
        },
        onBack = onBack
    )
}
