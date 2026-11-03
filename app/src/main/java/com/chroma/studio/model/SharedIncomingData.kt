package com.chroma.studio.model

import android.net.Uri

/**
 * Encapsulates data received from external apps via Android Share Sheet (ACTION_SEND / ACTION_SEND_MULTIPLE).
 */
data class SharedIncomingData(
    val uris: List<Uri> = emptyList(),
    val text: String? = null,
    val mimeType: String? = null,
    val subject: String? = null
) {
    val hasUris: Boolean get() = uris.isNotEmpty()
    val hasText: Boolean get() = !text.isNullOrBlank()

    val isSingleItem: Boolean get() = uris.size == 1

    val isImage: Boolean
        get() = mimeType?.startsWith("image/") == true ||
                uris.any { it.toString().endsWith(".jpg", true) || it.toString().endsWith(".jpeg", true) || it.toString().endsWith(".png", true) || it.toString().endsWith(".webp", true) }

    val isVideo: Boolean
        get() = mimeType?.startsWith("video/") == true ||
                uris.any { it.toString().endsWith(".mp4", true) || it.toString().endsWith(".mov", true) || it.toString().endsWith(".mkv", true) || it.toString().endsWith(".webm", true) }

    val isAudio: Boolean
        get() = mimeType?.startsWith("audio/") == true ||
                uris.any { it.toString().endsWith(".mp3", true) || it.toString().endsWith(".wav", true) || it.toString().endsWith(".m4a", true) || it.toString().endsWith(".aac", true) || it.toString().endsWith(".ogg", true) }

    val isMedia: Boolean get() = isImage || isVideo

    val displayHeadline: String
        get() = when {
            isAudio -> if (uris.size > 1) "${uris.size} Audio Tracks" else "Audio Track"
            isVideo -> if (uris.size > 1) "${uris.size} Videos" else "Video Clip"
            isImage -> if (uris.size > 1) "${uris.size} Photos" else "Photo"
            hasUris -> if (uris.size > 1) "${uris.size} Files" else "File Attachment"
            hasText -> "Shared Snippet"
            else -> "Shared Content"
        }

    val displayDetail: String
        get() = when {
            hasUris && hasText -> "${text?.take(40)}... (${uris.size} attached)"
            hasText -> text?.take(60) ?: ""
            uris.size > 1 -> "Select how you would like to import or share these ${uris.size} items in Oscellion"
            isAudio -> "Import to Audio Studio & Trimmer or share with a friend"
            isVideo -> "Open in Video Editor, post to community feed, or send in chat"
            isImage -> "Publish as new post, edit in Studio, or send in chat"
            else -> "Choose an action in Oscellion"
        }
}
