package com.chroma.studio.chat.data

import java.util.Locale

data class FileBadgeVisual(
    val extensionLabel: String,
    val accentColorHex: Long,
    val typeDescription: String,
    val category: String
)

/**
 * Resolves consistent badge typography, accent color, type label, and category
 * for a file extension or filename across pickers and chat bubbles.
 */
fun getFileBadgeVisual(extensionOrFileName: String?): FileBadgeVisual {
    val raw = extensionOrFileName?.trim().orEmpty()
    val ext = if (raw.contains('.')) raw.substringAfterLast('.') else raw
    val upper = ext.trim().uppercase(Locale.US)
    val label = upper.takeIf { it.isNotBlank() }?.take(4) ?: "FILE"

    return when (upper) {
        "PDF" -> FileBadgeVisual(label, 0xFFFF3B30, "PDF Document", "PDF")
        "DOC", "DOCX" -> FileBadgeVisual(label, 0xFF0381FE, "Word Document", "Docs")
        "TXT", "MD", "RTF" -> FileBadgeVisual(label, 0xFF5856D6, "Text Document", "Docs")
        "XLS", "XLSX", "CSV" -> FileBadgeVisual(label, 0xFF34C759, "Spreadsheet", "Sheets")
        "PPT", "PPTX", "KEY" -> FileBadgeVisual(label, 0xFFFF6B35, "Presentation", "Docs")
        "ZIP", "RAR", "7Z", "TAR", "GZ" -> FileBadgeVisual(label, 0xFF0284C7, "Archive", "Archives")
        "APK" -> FileBadgeVisual(label, 0xFFFF9500, "Android Package", "Archives")
        "MP3", "WAV", "M4A", "AAC", "OGG", "FLAC" -> FileBadgeVisual(label, 0xFFE11D48, "Audio File", "Audio")
        "MP4", "MOV", "MKV", "AVI", "WEBM", "3GP" -> FileBadgeVisual(label, 0xFFAF52DE, "Video File", "Media")
        "JPG", "JPEG", "PNG", "WEBP", "GIF", "HEIC", "SVG" -> FileBadgeVisual(label, 0xFF00C7BE, "Image File", "Media")
        "JSON", "XML", "YAML", "YML" -> FileBadgeVisual(label, 0xFF0EA5E9, "Data File", "Docs")
        else -> FileBadgeVisual(label, 0xFF0381FE, "Document", "Other")
    }
}

/**
 * Maps a filename or extension to its canonical MIME type for Android viewer/share intents.
 */
fun resolveMimeTypeForFileName(fileName: String?, fallbackExt: String? = null): String {
    val ext = (fileName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() && it != fileName }
        ?: fallbackExt.orEmpty())
        .trim()
        .lowercase(Locale.US)

    return when (ext) {
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "csv" -> "text/csv"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "zip" -> "application/zip"
        "rar" -> "application/x-rar-compressed"
        "7z" -> "application/x-7z-compressed"
        "tar" -> "application/x-tar"
        "gz" -> "application/gzip"
        "txt", "log", "md" -> "text/plain"
        "json" -> "application/json"
        "xml" -> "application/xml"
        "apk" -> "application/vnd.android.package-archive"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        "mp4", "m4v" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "webm" -> "video/webm"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> "*/*"
    }
}

/**
 * Formats byte count into a concise human-readable file size string, returning empty string when unknown.
 */
fun formatReadableFileSize(bytes: Long?): String {
    if (bytes == null || bytes < 0L) return ""
    if (bytes == 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * Sanitizes an arbitrary user filename into a storage-safe object key while keeping its extension.
 */
fun sanitizeStorageFileName(originalName: String?, fallbackExt: String = "bin"): String {
    val trimmed = originalName?.trim().orEmpty()
    val rawExt = if (trimmed.contains('.')) trimmed.substringAfterLast('.') else fallbackExt
    val cleanExt = rawExt.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").ifBlank { "bin" }
    val rawBase = if (trimmed.contains('.')) trimmed.substringBeforeLast('.') else trimmed
    val cleanBase = rawBase
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_', '.', '-')
        .take(64)
        .ifBlank { "document" }
    return "$cleanBase.$cleanExt"
}

/**
 * Resolves the display filename and optional separate caption for both structured file messages
 * (which set [storedFileName]) and legacy file messages (where the filename lived in [storedText]).
 */
fun resolveFileMessageNameAndCaption(
    storedFileName: String?,
    storedText: String?
): Pair<String, String?> {
    val cleanText = storedText?.takeIf { it.isNotBlank() && it != "🔒 Message encrypted" }
    val cleanName = storedFileName?.takeIf { it.isNotBlank() }
    return if (cleanName != null) {
        val caption = cleanText?.takeIf { it != cleanName }
        cleanName to caption
    } else {
        (cleanText ?: "Document") to null
    }
}

/**
 * Formats a concise file preview string for chat lists and reply banners.
 */
fun formatFileRoomPreview(fileName: String?, fallbackText: String?): String {
    val (resolvedName, _) = resolveFileMessageNameAndCaption(fileName, fallbackText)
    return "📄 $resolvedName"
}
