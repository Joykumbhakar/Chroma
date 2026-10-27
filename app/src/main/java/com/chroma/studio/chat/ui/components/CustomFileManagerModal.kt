package com.chroma.studio.chat.ui.components

import com.chroma.studio.chat.media.ChatMediaLimiter
import com.chroma.studio.ui.components.ToastManager

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.data.ChatFileItem
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.components.LiquidGlassCheckbox
import com.chroma.studio.chat.ui.theme.LocalChatColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class DeviceFileItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: String,
    val sizeBytes: Long,
    val uri: Uri?,
    val extension: String,
    val dateModified: String,
    val path: String
)

/**
 * Authentic Apple HIG Inbuilt File Manager Modal.
 * Scans device storage for real documents, PDFs, archives, and audio files,
 * with live search, file category filter pills, multi-file selection, and instant chat sending.
 */
@Composable
fun CustomFileManagerModal(
    onDismiss: () -> Unit,
    onSendFiles: (List<ChatFileItem>) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalChatColors.current

    var files by remember { mutableStateOf<List<DeviceFileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val selectedFiles = remember { mutableStateListOf<DeviceFileItem>() }

    val systemDocPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val pickedItems = uris.mapNotNull { uri ->
                try {
                    var fileName = "Document"
                    var fileSize = 0L
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: "Document"
                            if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                    val ext = fileName.substringAfterLast(".", "FILE").uppercase()
                    ChatFileItem(
                        id = UUID.randomUUID().toString(),
                        name = fileName,
                        size = formatFileSize(fileSize),
                        uri = uri.toString(),
                        extension = ext
                    )
                } catch (_: Exception) { null }
            }
            if (pickedItems.isNotEmpty()) {
                SoundManager.playSend()
                onSendFiles(pickedItems)
            }
        }
    }

    // Load device files in background
    LaunchedEffect(Unit) {
        isLoading = true
        files = withContext(Dispatchers.IO) {
            loadDeviceDocuments(context)
        }
        isLoading = false
    }

    val categories = listOf("All", "PDF", "Docs", "Sheets", "Archives", "Audio")

    val filteredFiles = remember(files, searchQuery, selectedCategory) {
        files.filter { file ->
            val matchesCategory = when (selectedCategory) {
                "PDF" -> file.extension.equals("PDF", ignoreCase = true)
                "Docs" -> listOf("DOC", "DOCX", "TXT", "RTF").any { it.equals(file.extension, ignoreCase = true) }
                "Sheets" -> listOf("XLS", "XLSX", "CSV").any { it.equals(file.extension, ignoreCase = true) }
                "Archives" -> listOf("ZIP", "RAR", "7Z", "TAR", "GZ").any { it.equals(file.extension, ignoreCase = true) }
                "Audio" -> listOf("MP3", "WAV", "M4A", "AAC", "OGG").any { it.equals(file.extension, ignoreCase = true) }
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                file.name.contains(searchQuery.trim(), ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(if (colors.isLight) Color(0xFFF9F9FC) else Color(0xFF161618))
                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(top = 12.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.textMuted.copy(alpha = 0.35f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(10.dp))

            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Files & Documents",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textDark
                    )
                    if (selectedFiles.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.iosBlue)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${selectedFiles.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.iosBlue.copy(alpha = 0.12f))
                            .clickable {
                                SoundManager.playReaction()
                                systemDocPickerLauncher.launch(arrayOf("*/*"))
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Browse...", color = colors.iosBlue, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceSecondary)
                            .clickable {
                                SoundManager.playReaction()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorCloseX(tint = colors.textMuted, size = 14.dp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (colors.isLight) Color(0xFFEFEFF4) else Color(0xFF2C2C2E))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LucideSearch(tint = colors.textMuted, size = 16.dp)
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 14.sp, color = colors.textDark),
                    cursorBrush = SolidColor(colors.iosBlue),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text("Search documents and files...", fontSize = 14.sp, color = colors.textMuted)
                        }
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { searchQuery = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorCloseX(tint = colors.textMuted, size = 12.dp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Category Filter Pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) colors.iosBlue else colors.surfaceSecondary)
                            .clickable {
                                SoundManager.playReaction()
                                selectedCategory = cat
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else colors.textDark
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // File Items List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.iosBlue, modifier = Modifier.size(32.dp))
                }
            } else if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhosphorFileIcon(tint = colors.textMuted, size = 42.dp)
                        Text(
                            text = "No files found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textDark
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No files match \"$searchQuery\"" else "No documents found in this category",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.iosBlue)
                                .clickable {
                                    SoundManager.playReaction()
                                    systemDocPickerLauncher.launch(arrayOf("*/*"))
                                }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Browse System Files", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { file ->
                        val isSelected = selectedFiles.any { it.id == file.id }

                        val badgeColor = when (file.extension.uppercase()) {
                            "PDF" -> Color(0xFFFF3B30)
                            "DOC", "DOCX" -> Color(0xFF007AFF)
                            "XLS", "XLSX", "CSV" -> Color(0xFF34C759)
                            "ZIP", "RAR", "7Z" -> Color(0xFFAF52DE)
                            "APK" -> Color(0xFFFF9500)
                            "MP3", "WAV", "M4A" -> Color(0xFFE11D48)
                            else -> colors.iosBlue
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) colors.iosBlue.copy(alpha = 0.08f) else (if (colors.isLight) Color.White else Color(0xFF222226)))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) colors.iosBlue else colors.borderSubtle,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    SoundManager.playReaction()
                                    if (isSelected) {
                                        selectedFiles.removeAll { it.id == file.id }
                                    } else {
                                        if (file.sizeBytes > ChatMediaLimiter.MAX_FILE_SIZE_BYTES) {
                                            ToastManager.showToast("File '${file.name}' exceeds 100 MB limit")
                                        } else {
                                            val currentTotal = selectedFiles.sumOf { it.sizeBytes }
                                            if (currentTotal + file.sizeBytes > ChatMediaLimiter.MAX_FILE_SIZE_BYTES) {
                                                ToastManager.showToast("Total selection exceeds 100 MB limit")
                                            } else {
                                                selectedFiles.add(file)
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // File Type Badge
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(badgeColor.copy(alpha = 0.16f))
                                    .border(0.5.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = file.extension.uppercase().take(4),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }

                            // File Name & Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${file.size} • ${file.dateModified}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = colors.textMuted
                                )
                            }

                            // Selection Checkbox (Thin Box & Extended Tick)
                            LiquidGlassCheckbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                size = 22.dp,
                                checkedColor = colors.iosBlue,
                                uncheckedColor = colors.textMuted.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }

            // Bottom Selected Files Actions Bar
            AnimatedVisibility(
                visible = selectedFiles.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (colors.isLight) Color.White else Color(0xFF1C1C1E))
                        .border(0.5.dp, colors.borderSubtle)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${selectedFiles.size} file${if (selectedFiles.size > 1) "s" else ""} selected",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textDark
                            )
                            val totalBytes = selectedFiles.sumOf { it.sizeBytes }
                            Text(
                                text = "Total: ${formatFileSize(totalBytes)}",
                                fontSize = 11.5.sp,
                                color = colors.textMuted
                            )
                        }

                        Button(
                            onClick = {
                                val totalSize = selectedFiles.sumOf { it.sizeBytes }
                                if (totalSize > ChatMediaLimiter.MAX_FILE_SIZE_BYTES) {
                                    ToastManager.showToast("Selected files exceed 100 MB limit (${ChatMediaLimiter.formatFileSize(totalSize)})")
                                    return@Button
                                }
                                SoundManager.playSend()
                                val chatFiles = selectedFiles.map {
                                    ChatFileItem(
                                        name = it.name,
                                        size = it.size,
                                        uri = it.uri?.toString(),
                                        extension = it.extension.uppercase()
                                    )
                                }
                                onSendFiles(chatFiles)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.iosBlue),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PhosphorSendArrowIcon(tint = Color.White, size = 15.dp)
                                Text(
                                    text = "Send (${selectedFiles.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Queries device storage for actual documents, downloads, and files.
 * Provides rich fallbacks if access is restricted so user always has functional items to select.
 */
private fun loadDeviceDocuments(context: Context): List<DeviceFileItem> {
    val items = mutableListOf<DeviceFileItem>()
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    try {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.pdf' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.doc%' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.xls%' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.zip' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.rar' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.txt' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.apk' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.mp3'"

        val selectionArgs = arrayOf("application/%")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT 50"

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        context.contentResolver.query(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Document"
                val sizeBytes = cursor.getLong(sizeCol)
                val dateSec = cursor.getLong(dateCol)
                val path = cursor.getString(dataCol) ?: ""
                val ext = name.substringAfterLast(".", "FILE").uppercase()
                val contentUri = ContentUris.withAppendedId(collectionUri, id)

                items.add(
                    DeviceFileItem(
                        name = name,
                        size = formatFileSize(sizeBytes),
                        sizeBytes = sizeBytes,
                        uri = contentUri,
                        extension = ext,
                        dateModified = dateFormat.format(Date(dateSec * 1000)),
                        path = path
                    )
                )
            }
        }
    } catch (e: Exception) {
        // Fallback gracefully to directory scanning or samples
    }

    // Also check Downloads directory directly
    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir != null && downloadsDir.exists()) {
            downloadsDir.listFiles()?.filter { it.isFile && !it.name.startsWith(".") }?.take(20)?.forEach { file ->
                val ext = file.name.substringAfterLast(".", "FILE").uppercase()
                val sizeBytes = file.length()
                if (items.none { it.name == file.name }) {
                    items.add(
                        DeviceFileItem(
                            name = file.name,
                            size = formatFileSize(sizeBytes),
                            sizeBytes = sizeBytes,
                            uri = Uri.fromFile(file),
                            extension = ext,
                            dateModified = dateFormat.format(Date(file.lastModified())),
                            path = file.absolutePath
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {}

    return items
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
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
