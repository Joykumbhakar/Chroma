package com.chroma.studio.chat.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import com.chroma.studio.ui.components.GalleryLayoutMode
import com.chroma.studio.ui.components.GalleryMediaTypeFilter
import com.chroma.studio.ui.components.OscellionGalleryLayout

/**
 * Custom In-App Gallery Picker Modal for Chat, Stories, and Messaging.
 * Powered by the fixed unified OscellionGalleryLayout in BOTTOM_SHEET mode.
 */
@Composable
fun CustomGalleryPickerModal(
    onDismiss: () -> Unit,
    onLaunchCamera: () -> Unit,
    onRequestMediaPermission: () -> Unit,
    onSendMedia: (List<Uri>, String?) -> Unit
) {
    OscellionGalleryLayout(
        layoutMode = GalleryLayoutMode.BOTTOM_SHEET,
        title = "Gallery",
        badgeText = "Oscellion",
        maxSelection = 10,
        allowedMediaFilter = GalleryMediaTypeFilter.ALL,
        showCameraTile = true,
        showCaptionInput = true,
        onLaunchCamera = onLaunchCamera,
        onSendMediaWithCaption = onSendMedia,
        onClose = onDismiss
    )
}
