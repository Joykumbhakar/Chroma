package com.chroma.studio.chat.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.chat.data.SettingsManager
import com.chroma.studio.chat.data.WallpaperType
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.ChatWallpaperBackground
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.ui.components.OscellionGreenCheck
import com.chroma.studio.ui.components.OscellionMorphProcessingDialog
import com.chroma.studio.ui.theme.InterFontFamily
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Flagship One UI / Apple Glass Chat Wallpaper Gallery Modal.
 * - Interactive Real-Time Live Chat Preview with mock bubbles and immediate contrast updates.
 * - Real-Time Dimming Slider with sun icon and live percentage readout.
 * - Granular Category Filters: "All", "HD Wallpapers", "Devices & OS", "Nature & Landscapes", "Abstract", "Custom Photos".
 * - Dedicated Custom Photo Import from device gallery, keeping previously imported wallpapers.
 * - Seamless Apply action featuring the official Oscellion green checkmark morph transition.
 */
@Composable
fun ChatWallpaperGalleryModal(
    currentWallpaper: WallpaperType = WallpaperType.DEFAULT,
    currentDimmingAlpha: Float = 0.15f,
    currentCustomUri: String? = null,
    settingsManager: SettingsManager? = null,
    onSelectWallpaper: (WallpaperType, Float, String?) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val colors = LocalChatColors.current

    val manager = remember(context, settingsManager) {
        settingsManager ?: SettingsManager.getInstance(context)
    }

    var previewWallpaper by remember { mutableStateOf(currentWallpaper) }
    var previewDimming by remember { mutableFloatStateOf(currentDimmingAlpha) }
    var previewCustomUri by remember { mutableStateOf(currentCustomUri ?: manager.customWallpaperUri) }

    var customWallpapers by remember { mutableStateOf(manager.getCustomWallpapers()) }
    var selectedCategory by remember { mutableStateOf("All") }

    var isApplying by remember { mutableStateOf(false) }
    var isApplySuccess by remember { mutableStateOf(false) }

    val categories = listOf(
        "All",
        "HD Wallpapers",
        "Devices & OS",
        "Nature & Landscapes",
        "Abstract",
        "Custom Photos"
    )

    // Enhanced Oscellion Gallery for importing custom user wallpapers
    val galleryLauncher = com.chroma.studio.ui.components.rememberOscellionMediaPicker(
        title = "Choose Chat Wallpaper",
        mediaFilter = com.chroma.studio.ui.components.GalleryMediaTypeFilter.PHOTOS,
        showAlbumsTab = true
    ) { uri ->
        val uriStr = uri.toString()
        manager.addCustomWallpaper(uriStr)
        customWallpapers = manager.getCustomWallpapers()
        previewCustomUri = uriStr
        SoundManager.playReaction()
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val builtInWallpapers = remember { WallpaperType.entries }
    val filteredBuiltIn = remember(selectedCategory) {
        if (selectedCategory == "All") {
            builtInWallpapers
        } else if (selectedCategory == "Custom Photos") {
            emptyList()
        } else {
            builtInWallpapers.filter { it.category == selectedCategory }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (!isApplying) onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(if (colors.isLight) Color(0xFFF9FAFB) else Color(0xFF141519))
                .border(
                    width = 1.dp,
                    color = if (colors.isLight) Color(0xFFE5E7EB) else Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // Drag Indicator Handle
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (colors.isLight) Color(0xFFD1D5DB) else Color.White.copy(alpha = 0.22f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(14.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Chat Wallpaper",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (colors.isLight) Color(0xFF111827) else Color.White,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        text = "Choose a background with live contrast preview",
                        fontSize = 12.5.sp,
                        color = if (colors.isLight) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                        fontFamily = InterFontFamily
                    )
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Close",
                        tint = if (colors.isLight) Color(0xFF374151) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // =========================================================================
            // 1. INTERACTIVE REAL-TIME LIVE CHAT PREVIEW CARD
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = if (colors.isLight) Color(0xFFE5E7EB) else Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .shadow(12.dp, RoundedCornerShape(20.dp))
            ) {
                // Background Wallpaper Canvas (Live Realtime Reaction)
                ChatWallpaperBackground(
                    wallpaperType = previewWallpaper,
                    dimmingAlpha = previewDimming,
                    customUri = previewCustomUri,
                    modifier = Modifier.fillMaxSize()
                )

                // Mock Chat Bubbles on Top for Live Contrast Testing
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Incoming mock bubble
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                            .background(if (colors.isLight) Color(0xFFFFFFFF).copy(alpha = 0.92f) else Color(0xFF26262B).copy(alpha = 0.88f))
                            .border(
                                0.5.dp,
                                if (colors.isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Alex • 10:42 AM",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0381FE)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "How does this wallpaper look in chat? ✨",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (colors.isLight) Color(0xFF1F2937) else Color.White
                            )
                        }
                    }

                    // Outgoing mock bubble
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                            .background(Color(0xFF0381FE))
                            .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Ultra crisp and super clear! 👌",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Lucide.CheckCheck,
                            contentDescription = "Read",
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Top Right "LIVE PREVIEW" Pill Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(OscellionGreenCheck)
                        )
                        Text(
                            text = "LIVE PREVIEW",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // =========================================================================
            // 2. REAL-TIME DIMMING SLIDER
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (colors.isLight) Color(0xFFF3F4F6) else Color.White.copy(alpha = 0.06f))
                    .border(
                        1.dp,
                        if (colors.isLight) Color(0xFFE5E7EB) else Color.White.copy(alpha = 0.09f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Lucide.Sun,
                    contentDescription = "Dimming Icon",
                    tint = if (colors.isLight) Color(0xFF4B5563) else Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = "Dimming",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (colors.isLight) Color(0xFF374151) else Color.White.copy(alpha = 0.85f),
                    fontFamily = InterFontFamily
                )

                Slider(
                    value = previewDimming,
                    onValueChange = { previewDimming = it },
                    valueRange = 0f..0.70f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF0381FE),
                        activeTrackColor = Color(0xFF0381FE),
                        inactiveTrackColor = if (colors.isLight) Color(0xFFD1D5DB) else Color.White.copy(alpha = 0.15f)
                    )
                )

                Text(
                    text = "${(previewDimming * 100).roundToInt()}%",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isLight) Color(0xFF111827) else Color.White,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(Modifier.height(14.dp))

            // =========================================================================
            // 3. CATEGORY FILTER TABS
            // =========================================================================
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    val pillShape = RoundedCornerShape(999.dp)
                    Box(
                        modifier = Modifier
                            .clip(pillShape)
                            .background(
                                if (isSelected) Color(0xFF0381FE)
                                else if (colors.isLight) Color(0xFFE5E7EB)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF3B82F6) else Color.Transparent,
                                shape = pillShape
                            )
                            .clickable {
                                SoundManager.playTap(context)
                                selectedCategory = cat
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else if (colors.isLight) Color(0xFF374151) else Color.White.copy(alpha = 0.85f),
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // =========================================================================
            // 4. WALLPAPERS GRID (Includes + Custom Photo Card, Custom Wallpapers & Presets)
            // =========================================================================
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                // Card 1: Add Custom Photo Tile
                if (selectedCategory == "All" || selectedCategory == "Custom Photos") {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(118.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (colors.isLight) Color(0xFFF3F4F6) else Color.White.copy(alpha = 0.06f))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF0381FE).copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    SoundManager.playTap(context)
                                    galleryLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.ImagePlus,
                                    contentDescription = "Add custom photo",
                                    tint = Color(0xFF0381FE),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Add Photo",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0381FE),
                                    fontFamily = InterFontFamily
                                )
                                Text(
                                    text = "From Gallery",
                                    fontSize = 9.sp,
                                    color = if (colors.isLight) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }

                    // Previously Imported Custom Wallpapers
                    items(customWallpapers) { customUri ->
                        val isSelected = previewCustomUri == customUri
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.04f else 1f,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
                            label = "customWpScale"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(118.dp)
                                .scale(scale)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF0381FE) else if (colors.isLight) Color(0xFFE5E7EB) else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    SoundManager.playTap(context)
                                    previewCustomUri = customUri
                                }
                        ) {
                            AsyncImage(
                                model = customUri,
                                contentDescription = "Custom Wallpaper",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Label Badge at bottom
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    ),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    text = "Custom",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 5.dp),
                                    fontFamily = InterFontFamily
                                )
                            }

                            // Active Selection Emerald Check Badge (Top Right)
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(OscellionGreenCheck)
                                        .border(1.dp, Color.White, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // Quick Delete Badge (Top Left)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .clickable {
                                        SoundManager.playTap(context)
                                        manager.deleteCustomWallpaper(customUri)
                                        customWallpapers = manager.getCustomWallpapers()
                                        if (previewCustomUri == customUri) {
                                            previewCustomUri = customWallpapers.firstOrNull()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = "Delete",
                                    tint = Color.White.copy(alpha = 0.90f),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }

                // Built-in Wallpaper Presets
                items(filteredBuiltIn) { wp ->
                    val isSelected = previewCustomUri == null && previewWallpaper == wp
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.04f else 1f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
                        label = "presetWpScale"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp)
                            .scale(scale)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF0381FE) else if (colors.isLight) Color(0xFFE5E7EB) else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                SoundManager.playTap(context)
                                previewCustomUri = null
                                previewWallpaper = wp
                            }
                    ) {
                        AsyncImage(
                            model = wp.drawableRes,
                            contentDescription = wp.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Title Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                                    )
                                ),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = wp.title,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    fontFamily = InterFontFamily
                                )
                                Text(
                                    text = wp.category,
                                    fontSize = 8.5.sp,
                                    color = Color.White.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }

                        // Active Selection Emerald Check Badge (Top Right)
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(OscellionGreenCheck)
                                    .border(1.dp, Color.White, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // =========================================================================
            // 5. BOTTOM ACTIONS ROW
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Reset / Default Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (colors.isLight) Color(0xFFE5E7EB) else Color.White.copy(alpha = 0.10f))
                        .clickable(enabled = !isApplying) {
                            previewWallpaper = WallpaperType.DEFAULT
                            previewCustomUri = null
                            previewDimming = 0.15f
                            SoundManager.playTap(context)
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reset",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (colors.isLight) Color(0xFF374151) else Color.White,
                        fontFamily = InterFontFamily
                    )
                }

                // Apply Button
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0381FE))
                        .clickable(enabled = !isApplying) {
                            isApplying = true
                            isApplySuccess = false
                            SoundManager.playReaction()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                manager.wallpaperType = previewWallpaper
                                manager.wallpaperDimming = previewDimming
                                manager.customWallpaperUri = previewCustomUri
                                delay(350)
                                isApplySuccess = true
                                delay(700)
                                isApplying = false
                                onSelectWallpaper(previewWallpaper, previewDimming, previewCustomUri)
                                onClose()
                            }
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = "Apply",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Set as Wallpaper",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }
        }

        // Official Oscellion Morph Processing Dialog upon applying wallpaper
        OscellionMorphProcessingDialog(
            visible = isApplying,
            isCompleted = isApplySuccess,
            loadingText = "Setting Wallpaper...",
            completedText = "Wallpaper Applied"
        )
    }
}
