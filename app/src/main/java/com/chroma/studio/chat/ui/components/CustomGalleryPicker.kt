package com.chroma.studio.chat.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import com.chroma.studio.ui.components.GalleryLayoutMode
import com.chroma.studio.ui.components.GalleryMediaTypeFilter
import com.chroma.studio.ui.components.OscellionGalleryLayout

/**
 * Custom In-App Gallery Picker Modal for Chat, Stories, and Messaging.
 * Powered by Unified OscellionGalleryLayout with usage-tailored defaults.
 */
@Composable
fun CustomGalleryPickerModal(
    onDismiss: () -> Unit,
    onLaunchCamera: () -> Unit = {},
    onRequestMediaPermission: () -> Unit = {},
    title: String = "Attach Media",
    maxSelection: Int = 10,
    allowedMediaFilter: GalleryMediaTypeFilter = GalleryMediaTypeFilter.ALL,
    showCaptionInput: Boolean = true,
    showCameraTile: Boolean = true,
    showAlbumsTab: Boolean = true,
    lockMediaFilter: Boolean = false,
    singleTapImmediate: Boolean = (maxSelection == 1 && !showCaptionInput),
    submitLabel: String? = null,
    onEditMedia: ((List<Uri>, Int, String?) -> Unit)? = null,
    onSendMedia: (List<Uri>, String?) -> Unit
) {
    OscellionGalleryLayout(
        layoutMode = GalleryLayoutMode.BOTTOM_SHEET,
        title = title,
        badgeText = "",
        maxSelection = maxSelection,
        allowedMediaFilter = allowedMediaFilter,
        showCameraTile = showCameraTile,
        showCaptionInput = showCaptionInput,
        showAlbumsTab = showAlbumsTab,
        lockMediaFilter = lockMediaFilter,
        singleTapImmediate = singleTapImmediate,
        submitLabel = submitLabel,
        onSendMediaWithCaption = onSendMedia,
        onEditMediaWithCaption = onEditMedia,
        onLaunchCamera = onLaunchCamera,
        onClose = onDismiss
    )
}
