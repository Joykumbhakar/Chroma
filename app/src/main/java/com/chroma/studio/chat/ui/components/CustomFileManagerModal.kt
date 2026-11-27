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
import com.chroma.studio.chat.data.formatReadableFileSize
import com.chroma.studio.chat.data.getFileBadgeVisual
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
 * Scans device storage for real documents, PDFs, archives, and media files,
 * supports system file & cloud picking (`OpenMultipleDocuments`), live search,
 * category filters, multi-file selection with total size guard, and optional captions.
 */
@Composable
fun CustomFileManagerModal(
    onDismiss: () -> Unit,
    onSendFiles: (List<ChatFileItem>, String) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalChatColors.current

    var files by remember { mutableStateOf<List<DeviceFileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var captionText by remember { mutableStateOf("") }
    val selectedFiles = remember { mutableStateListOf<DeviceFileItem>() }

    val systemDocPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val dateNow = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
            var skippedOversized = 0
            val newlyPicked = uris.mapNotNull { uri ->
                try {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {}

                    var fileName = "Document"
                    var fileSize = -1L
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: "Document"
                            if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                                fileSize = cursor.getLong(sizeIndex)
                            }
                        }
                    }
                    if (fileSize <= 0L) {
                        fileSize = ChatMediaLimiter.getMediaSizeBytes(context, uri)
                    }
                    if (fileSize > ChatMediaLimiter.MAX_FILE_SIZE_BYTES) {
                        skippedOversized++
                        return@mapNotNull null
                    }
                    val ext = fileName.substringAfterLast(".", "FILE").uppercase()
                    val readableSize = formatReadableFileSize(fileSize).ifBlank { "File" }
                    DeviceFileItem(
                        id = uri.toString(),
                        name = fileName,
                        size = readableSize,
                        sizeBytes = fileSize.coerceAtLeast(0L),
                        uri = uri,
                        extension = ext,
                        dateModified = dateNow,
                        path = uri.toString()
                    )
                } catch (_: Exception) {
                    null
                }
            }
            if (skippedOversized > 0) {
                ToastManager.showToast("Skipped $skippedOversized file(s) exceeding 100 MB limit")
            }
            if (newlyPicked.isNotEmpty()) {
                val existingIds = files.mapTo(mutableSetOf()) { it.id }
                val toPrepend = newlyPicked.filter { it.id !in existingIds }
                if (toPrepend.isNotEmpty()) {
                    files = toPrepend + files
                }
                newlyPicked.forEach { item ->
                    if (selectedFiles.none { it.id == item.id }) {
                        selectedFiles.add(item)
                    }
                }
                selectedCategory = "All"
                SoundManager.playReaction()
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

    val categories = listOf("All", "PDF", "Docs", "Sheets", "Archives", "Audio", "Media")

    val filteredFiles = remember(files, searchQuery, selectedCategory) {
        files.filter { file ->
            val cat = getFileBadgeVisual(file.extension).category
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                else -> cat.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                file.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    file.extension.contains(searchQuery.trim(), ignoreCase = true)
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
                .imePadding()
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

            // Quick System & Cloud Picker Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.iosBlue.copy(alpha = if (colors.isLight) 0.08f else 0.14f))
                    .border(0.5.dp, colors.iosBlue.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .clickable {
                        SoundManager.playReaction()
                        systemDocPickerLauncher.launch(arrayOf("*/*"))
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.iosBlue.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorFileIcon(tint = colors.iosBlue, size = 17.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Browse Device, Downloads & Cloud",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textDark
                    )
                    Text(
                        text = "Send PDF, DOCX, ZIP, APK, or uncompressed media up to 100 MB",
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Open",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.iosBlue
                )
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

                        val badgeVisual = getFileBadgeVisual(file.extension)
                        val badgeColor = Color(badgeVisual.accentColorHex)

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
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Optional caption input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isLight) Color(0xFFF2F2F7) else Color(0xFF2C2C2E))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = captionText,
                            onValueChange = { captionText = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.5.sp, color = colors.textDark),
                            cursorBrush = SolidColor(colors.iosBlue),
                            decorationBox = { inner ->
                                if (captionText.isEmpty()) {
                                    Text("Add a message with ${selectedFiles.size} file${if (selectedFiles.size > 1) "s" else ""}...", fontSize = 13.5.sp, color = colors.textMuted)
                                }
                                inner()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedFiles.size} file${if (selectedFiles.size > 1) "s" else ""} selected",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textDark
                            )
                            val totalBytes = selectedFiles.sumOf { it.sizeBytes }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${formatFileSize(totalBytes)} / 100 MB",
                                    fontSize = 11.5.sp,
                                    color = colors.textMuted
                                )
                                Text(
                                    text = "Clear",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.iosBlue,
                                    modifier = Modifier.clickable { selectedFiles.clear() }
                                )
                            }
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
                                onSendFiles(chatFiles, captionText.trim())
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

    // Also check Downloads and Documents directories directly
    listOf(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    ).forEach { publicDir ->
        try {
            if (publicDir != null && publicDir.exists()) {
                publicDir.listFiles()?.filter { it.isFile && !it.name.startsWith(".") }?.take(25)?.forEach { file ->
                    val ext = file.name.substringAfterLast(".", "FILE").uppercase()
                    val sizeBytes = file.length()
                    if (items.none { it.path == file.absolutePath || (it.name == file.name && it.sizeBytes == sizeBytes) }) {
                        items.add(
                            DeviceFileItem(
                                id = file.absolutePath,
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
        } catch (_: Exception) {}
    }

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
