package com.chroma.studio.chat.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.media.GalleryMediaItem
import com.chroma.studio.chat.media.MediaStoreHelper
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.chat.util.PermissionHelper

@Composable
fun CustomGalleryPickerModal(
    onDismiss: () -> Unit,
    onLaunchCamera: () -> Unit,
    onRequestMediaPermission: () -> Unit,
    onSendMedia: (List<Uri>, String?) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalChatColors.current

    var hasPermission by remember {
        mutableStateOf(PermissionHelper.hasMediaPermission(context))
    }
    var mediaItems by remember { mutableStateOf<List<GalleryMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val selectedUris = remember { mutableStateListOf<Uri>() }
    var selectedAlbum by remember { mutableStateOf("All") }
    var captionText by remember { mutableStateOf("") }

    // Load media when permission is granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            mediaItems = MediaStoreHelper.loadDeviceMedia(context)
            isLoading = false
        } else {
            isLoading = false
        }
    }

    val albums = remember(mediaItems) {
        val list = mutableListOf("All")
        list.addAll(mediaItems.map { it.bucketName }.distinct())
        list
    }

    val filteredItems = remember(mediaItems, selectedAlbum) {
        if (selectedAlbum == "All") mediaItems
        else mediaItems.filter { it.bucketName == selectedAlbum }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
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
                        text = "Gallery & Photos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textDark
                    )
                    if (selectedUris.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.iosBlue)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${selectedUris.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
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

            Spacer(Modifier.height(10.dp))

            // Album Filter Tabs
            if (hasPermission && albums.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(albums) { album ->
                        val isSelected = selectedAlbum == album
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) colors.iosBlue else colors.surfaceSecondary)
                                .clickable {
                                    SoundManager.playReaction()
                                    selectedAlbum = album
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = album,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else colors.textDark
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Main Content Area
            if (!hasPermission) {
                // Permission Request Fallback State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(colors.iosBlueSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            PhosphorPhotoIcon(tint = colors.iosBlue, size = 30.dp)
                        }
                        Text(
                            text = "Allow Access to Photos",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textDark
                        )
                        Text(
                            text = "ChatApp needs photo access so you can send images and videos to your friends.",
                            fontSize = 13.sp,
                            color = colors.textMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = {
                                SoundManager.playReaction()
                                onRequestMediaPermission()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.iosBlue),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("Grant Access", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.iosBlue, modifier = Modifier.size(32.dp))
                }
            } else {
                // Grid of Media Items
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Item 0: Live Camera Action Tile
                    item {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(if (colors.isLight) Color(0xFFE5E5EA) else Color(0xFF2C2C2E))
                                .clickable {
                                    SoundManager.playReaction()
                                    onLaunchCamera()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PhosphorCameraIcon(tint = colors.iosBlue, size = 28.dp)
                                Text(
                                    text = "Camera",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textDark
                                )
                            }
                        }
                    }

                    // Media items
                    items(filteredItems) { item ->
                        val isSelected = selectedUris.contains(item.uri)
                        val selectIndex = if (isSelected) selectedUris.indexOf(item.uri) + 1 else null

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    SoundManager.playReaction()
                                    if (isSelected) {
                                        selectedUris.remove(item.uri)
                                    } else {
                                        selectedUris.add(item.uri)
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Video Duration Indicator
                            if (item.isVideo) {
                                val sec = (item.durationMs / 1000) % 60
                                val min = (item.durationMs / 1000) / 60
                                val timeStr = "$min:${sec.toString().padStart(2, '0')}"
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = timeStr,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Selection Counter Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) colors.iosBlue else Color.Black.copy(alpha = 0.35f)
                                    )
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected && selectIndex != null) {
                                    Text(
                                        text = "$selectIndex",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Selected Media Actions Bar
            AnimatedVisibility(
                visible = selectedUris.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (colors.isLight) Color.White else Color(0xFF1C1C1E))
                        .border(0.5.dp, colors.borderSubtle)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Preview strip
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedUris) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .clickable { selectedUris.remove(uri) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    PhosphorCloseX(tint = Color.White, size = 8.dp)
                                }
                            }
                        }
                    }

                    // Send Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedUris.size} item${if (selectedUris.size > 1) "s" else ""} selected",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted
                        )

                        Button(
                            onClick = {
                                SoundManager.playReaction()
                                onSendMedia(selectedUris.toList(), captionText.ifBlank { null })
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.iosBlue),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 9.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PhosphorSendArrowIcon(tint = Color.White, size = 15.dp)
                                Text(
                                    text = "Send (${selectedUris.size})",
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

