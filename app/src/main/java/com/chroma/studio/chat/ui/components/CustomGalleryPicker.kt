package com.chroma.studio.chat.ui.components

import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.chroma.studio.gallery.GalleryLayoutMode
import com.chroma.studio.gallery.OscellionGallery
import com.chroma.studio.ui.theme.LocalOscellionColors

/**
 * Custom In-App Gallery Picker Modal for Chat, Stories, and Messaging.
 * Powered by the OscellionGallery (new) in BottomSheet mode.
 *
 * The [onLaunchCamera] parameter is kept for API compatibility but is no longer
 * used – the new OscellionGallery handles camera internally via its camera tile.
 */
@Composable
fun CustomGalleryPickerModal(
    onDismiss: () -> Unit,
    onLaunchCamera: () -> Unit = {},
    onRequestMediaPermission: () -> Unit = {},
    onSendMedia: (List<Uri>, String?) -> Unit
) {
    val appColors = LocalOscellionColors.current
    val isDark = appColors.isDark

    OscellionGallery(
        mode = GalleryLayoutMode.BottomSheet,
        maxSelection = 10,
        showCaptionInput = true,
        showCameraTile = true,
        darkTheme = isDark,
        onConfirm = { items, caption ->
            onSendMedia(items.map { it.uri }, caption.ifBlank { null })
        },
        onClose = onDismiss,
        onSystemPicked = { uris ->
            onSendMedia(uris, null)
        },
    )
}
