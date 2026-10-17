package com.chroma.studio.ui.screens
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.ChromaHeaderActionButton
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.components.GlassTextButton
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.GlassButton

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.chroma.studio.model.FlashMediaType
import com.chroma.studio.ui.components.ApplePhotosGraphic
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaItem(
    val uri: Uri,
    val name: String,
    val type: FlashMediaType,
    val durationMs: Long = 0L, // 0 for photos
    val albumName: String = ""
)

enum class GalleryFilter(val label: String) {
    RECENT("Recent"), VIDEOS("Videos Only"), PHOTOS("Photos Only"),
    ALBUMS("Albums"), FOLDERS("Folders")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashPickerScreen(
    onVideoSelected: (Uri) -> Unit,
    onPhotosSelected: (List<Uri>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current

    // Permission state
    var hasVideoPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    var hasImagePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasVideoPermission = results[Manifest.permission.READ_MEDIA_VIDEO] == true
            || results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        hasImagePermission = results[Manifest.permission.READ_MEDIA_IMAGES] == true
            || results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
    }

    var filter by remember { mutableStateOf(GalleryFilter.RECENT) }
    var filterExpanded by remember { mutableStateOf(false) }

    // Media items
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoadingMedia by remember { mutableStateOf(false) }

    // Multi-select for photos
    var selectedPhotoUris by remember { mutableStateOf<Set<Uri>>(emptySet()) }

    // Load media when permission granted or filter changes
    LaunchedEffect(hasVideoPermission, hasImagePermission, filter) {
        if (!hasVideoPermission && !hasImagePermission) return@LaunchedEffect
        isLoadingMedia = true
        mediaItems = withContext(Dispatchers.IO) {
            loadMediaItems(context, filter, hasVideoPermission, hasImagePermission)
        }
        isLoadingMedia = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        ChromaScreenHeader(
            title = "Oscellion",
            badgeText = "Select Media",
            showBackButton = true,
            onBack = onBack,
            actions = {
                if (selectedPhotoUris.isNotEmpty()) {
                    ChromaHeaderActionButton(
                        text = "Next (${selectedPhotoUris.size})",
                        icon = Lucide.ArrowRight,
                        onClick = { onPhotosSelected(selectedPhotoUris.toList()) }
                    )
                }
            }
        )

        // ── Permission gate ───────────────────────────────────────────────────
        if (!hasVideoPermission && !hasImagePermission) {
            PermissionRequestCard(
                onRequest = {
                    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    permissionLauncher.launch(perms)
                }
            )
            return@Column
        }

        // ── Filter dropdown ───────────────────────────────────────────────────
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { filterExpanded = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(filter.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Icon(Lucide.ChevronDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
                expanded = filterExpanded,
                onDismissRequest = { filterExpanded = false },
                modifier = Modifier
                    .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.15f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1C1C1E))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            ) {
                GalleryFilter.values().forEach { f ->
                    DropdownMenuItem(
                        text = {
                            Text(f.label, color = if (f == filter) Color(0xFF007AFF) else Color.White, fontWeight = FontWeight.Medium)
                        },
                        onClick = {
                            filter = f
                            filterExpanded = false
                            selectedPhotoUris = emptySet()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Grid ─────────────────────────────────────────────────────────────
        if (isLoadingMedia) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                OscellionLoadingSpinner(color = Color(0xFF007AFF))
            }
        } else if (mediaItems.isEmpty()) {
            val emptyState = rememberAppleGraphicAnimationState()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ApplePhotosGraphic(
                        modifier = Modifier.size(130.dp),
                        state = emptyState,
                        enableClickToAnimate = true
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No media found", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(mediaItems) { item ->
                    val isSelected = item.uri in selectedPhotoUris
                    val selectionIndex = selectedPhotoUris.toList().indexOf(item.uri) + 1

                    Box(
                        modifier = Modifier
                            .aspectRatio(9f / 16f)
                            .clickable {
                                if (item.type == FlashMediaType.VIDEO) {
                                    if (item.durationMs <= 30_000L) {
                                        onVideoSelected(item.uri)
                                    } else {
                                        // Over 30s — still open editor for trimming
                                        onVideoSelected(item.uri)
                                    }
                                } else {
                                    // Photo: multi-select up to 10
                                    if (isSelected) {
                                        selectedPhotoUris = selectedPhotoUris - item.uri
                                    } else if (selectedPhotoUris.size < 10) {
                                        selectedPhotoUris = selectedPhotoUris + item.uri
                                    }
                                }
                            }
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Video duration badge
                        if (item.type == FlashMediaType.VIDEO && item.durationMs > 0L) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    formatDuration(item.durationMs),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Video type indicator
                        if (item.type == FlashMediaType.VIDEO) {
                            Icon(
                                Lucide.Play,
                                null,
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(14.dp)
                            )
                        }

                        // Photo selection overlay
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF007AFF).copy(alpha = 0.3f))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF007AFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$selectionIndex", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (item.type == FlashMediaType.PHOTO && selectedPhotoUris.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestCard(onRequest: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Lucide.FolderOpen, null, tint = Color(0xFF007AFF), modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Access Your Media",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Oscellion needs access to your photos and videos to let you upload Flash content.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(32.dp))
            GlassButton(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Lucide.Shield, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Allow Access", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

private fun loadMediaItems(
    context: android.content.Context,
    filter: GalleryFilter,
    hasVideoPermission: Boolean,
    hasImagePermission: Boolean
): List<MediaItem> {
    val items = mutableListOf<MediaItem>()

    // Query videos
    if (hasVideoPermission && filter != GalleryFilter.PHOTOS) {
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        val videoCursor = context.contentResolver.query(
            videoUri, videoProjection, null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )
        videoCursor?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(videoUri, id)
                items.add(
                    MediaItem(
                        uri = contentUri,
                        name = cursor.getString(nameCol) ?: "",
                        type = FlashMediaType.VIDEO,
                        durationMs = cursor.getLong(durCol),
                        albumName = cursor.getString(albumCol) ?: ""
                    )
                )
            }
        }
    }

    // Query images
    if (hasImagePermission && filter != GalleryFilter.VIDEOS) {
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val imageCursor = context.contentResolver.query(
            imageUri, imageProjection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        imageCursor?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(imageUri, id)
                items.add(
                    MediaItem(
                        uri = contentUri,
                        name = cursor.getString(nameCol) ?: "",
                        type = FlashMediaType.PHOTO,
                        albumName = cursor.getString(albumCol) ?: ""
                    )
                )
            }
        }
    }

    // Sort by date added (videos and images interleaved for RECENT)
    return when (filter) {
        GalleryFilter.RECENT -> items.shuffled().take(200) // Show mixed recent
        GalleryFilter.VIDEOS -> items.filter { it.type == FlashMediaType.VIDEO }
        GalleryFilter.PHOTOS -> items.filter { it.type == FlashMediaType.PHOTO }
        GalleryFilter.ALBUMS -> items.sortedBy { it.albumName }
        GalleryFilter.FOLDERS -> items.sortedBy { it.albumName }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
