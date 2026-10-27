package com.chroma.studio.ui.screens

import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.chroma.studio.ui.components.GalleryLayoutMode
import com.chroma.studio.ui.components.GalleryMediaTypeFilter
import com.chroma.studio.ui.components.OscellionGalleryLayout

/**
 * Fullscreen Flash Media Picker Screen.
 * Uses the fixed unified OscellionGalleryLayout in FULL_SCREEN mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashPickerScreen(
    onVideoSelected: (Uri) -> Unit,
    onPhotosSelected: (List<Uri>) -> Unit,
    onBack: () -> Unit
) {
    OscellionGalleryLayout(
        layoutMode = GalleryLayoutMode.FULL_SCREEN,
        title = "Gallery",
        badgeText = "Oscellion",
        maxSelection = 10,
        allowedMediaFilter = GalleryMediaTypeFilter.ALL,
        showCameraTile = true,
        showCaptionInput = false,
        onVideoSelected = onVideoSelected,
        onPhotosSelected = onPhotosSelected,
        onClose = onBack
    )
}
