package com.chroma.studio.chat.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import com.chroma.studio.ui.components.GalleryLayoutMode
import com.chroma.studio.ui.components.OscellionGalleryLayout

/**
 * Custom In-App Gallery Picker Modal for Chat, Stories, and Messaging.
 * Powered by Samsung Native One UI OscellionGalleryLayout in BottomSheet mode.
 */
@Composable
fun CustomGalleryPickerModal(
    onDismiss: () -> Unit,
    onLaunchCamera: () -> Unit = {},
    onRequestMediaPermission: () -> Unit = {},
    onEditMedia: ((List<Uri>, Int, String?) -> Unit)? = null,
    onSendMedia: (List<Uri>, String?) -> Unit
) {
    OscellionGalleryLayout(
        layoutMode = GalleryLayoutMode.BOTTOM_SHEET,
        title = "Attach Media",
        badgeText = "",
        maxSelection = 10,
        showCaptionInput = true,
        showCameraTile = true,
        showAlbumsTab = false,
        onSendMediaWithCaption = onSendMedia,
        onEditMediaWithCaption = onEditMedia,
        onLaunchCamera = onLaunchCamera,
        onClose = onDismiss
    )
}
