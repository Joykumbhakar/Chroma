package com.chroma.studio.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.data.*
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWallpaperPickerSheet(
    settingsManager: ChatSettingsManager,
    roomId: String?,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val sheetBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) Color.White else Color(0xFF1C1C1E)
    val textSecondary = Color(0xFF8E8E93)
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0x20000000)

    val initialConfig by settingsManager.getWallpaperForRoom(roomId).collectAsState()
    val chatTheme by settingsManager.theme.collectAsState()
    val chatTextSize by settingsManager.textSize.collectAsState()

    var previewConfig by remember(initialConfig) { mutableStateOf(initialConfig) }
    var selectedCategory by remember { mutableIntStateOf(0) }
    val categories = listOf("Solids", "Gradients", "Doodles", "Photos", "Themes", "Bubbles")

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            previewConfig = previewConfig.copy(
                type = WallpaperType.GALLERY,
                value = uri.toString()
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Studio Appearance & Wallpaper",
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                IconButton(onClick = onDismiss) {
                    Icon(Lucide.X, null, tint = textSecondary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            // Mini Interactive Chat Preview with dynamic styling
            val previewBubbleShape = when (previewConfig.bubbleStyle) {
                BubbleStyle.ROUNDED -> RoundedCornerShape(20.dp)
                BubbleStyle.MINIMAL -> RoundedCornerShape(10.dp)
                else -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
            }
            val previewOutgoingShape = when (previewConfig.bubbleStyle) {
                BubbleStyle.ROUNDED -> RoundedCornerShape(20.dp)
                BubbleStyle.MINIMAL -> RoundedCornerShape(10.dp)
                else -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            }

            val previewIncomingBg = if (previewConfig.customIncomingColor != null) {
                try { SolidColor(Color(android.graphics.Color.parseColor(previewConfig.customIncomingColor))) } catch (e: Exception) { SolidColor(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE9E9EB)) }
            } else {
                SolidColor(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE9E9EB))
            }

            val previewOutgoingBg = if (previewConfig.customOutgoingColor != null) {
                try { SolidColor(Color(android.graphics.Color.parseColor(previewConfig.customOutgoingColor))) } catch (e: Exception) { SolidColor(Color(0xFF0A84FF)) }
            } else {
                Brush.linearGradient(
                    when (chatTheme) {
                        ChatTheme.OCEAN -> listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                        ChatTheme.SUNSET -> listOf(Color(0xFFFF5E3A), Color(0xFFFF2A68))
                        ChatTheme.EMERALD -> listOf(Color(0xFF34C759), Color(0xFF00C7BE))
                        ChatTheme.LAVENDER -> listOf(Color(0xFFAF52DE), Color(0xFF5E5CE6))
                        ChatTheme.MONOCHROME -> listOf(Color(0xFF3A3A3C), Color(0xFF1C1C1E))
                        else -> listOf(Color(0xFF0A84FF), Color(0xFF005CE6))
                    }
                )
            }

            val previewIncomingTextColor = if (previewConfig.customIncomingTextColor != null) {
                try { Color(android.graphics.Color.parseColor(previewConfig.customIncomingTextColor)) } catch (e: Exception) { if (isDark) Color.White else Color.Black }
            } else {
                if (isDark) Color.White else Color.Black
            }

            val previewOutgoingTextColor = if (previewConfig.customOutgoingTextColor != null) {
                try { Color(android.graphics.Color.parseColor(previewConfig.customOutgoingTextColor)) } catch (e: Exception) { Color.White }
            } else {
                Color.White
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            ) {
                // Background Preview
                ChatWallpaperBackground(config = previewConfig)

                // Sample Message Bubbles
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Incoming Bubble
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .shadow(2.dp, previewBubbleShape)
                            .clip(previewBubbleShape)
                            .background(previewIncomingBg)
                            .border(0.5.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f), previewBubbleShape)
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "Hey! How does this new glass theme look?",
                            color = previewIncomingTextColor,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            fontFamily = InterFontFamily
                        )
                    }

                    // Outgoing Bubble
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .fillMaxWidth(0.72f)
                            .shadow(3.dp, previewOutgoingShape, spotColor = Color.Black.copy(alpha = 0.25f))
                            .clip(previewOutgoingShape)
                            .background(previewOutgoingBg)
                            .border(0.5.dp, Color.White.copy(alpha = 0.22f), previewOutgoingShape)
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "Looks rich, deep, and tactile.",
                            color = previewOutgoingTextColor,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Category Selector
            IosSegmentedControl(
                items = categories,
                selectedIndex = selectedCategory,
                onItemSelected = { selectedCategory = it }
            )

            Spacer(Modifier.height(18.dp))

            // Tab 0: WhatsApp Solids
            if (selectedCategory == 0) {
                Column {
                    Text("Metallic & Solid Palette", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = InterFontFamily)
                    Spacer(Modifier.height(10.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ChatWallpaperPresets.solidColors) { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.LightGray }
                            val isSelected = previewConfig.type == WallpaperType.SOLID && previewConfig.value == hex

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF0A84FF) else Color(0x33000000),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        previewConfig = previewConfig.copy(
                                            type = WallpaperType.SOLID,
                                            value = hex
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Lucide.Check, null, tint = if (hex.startsWith("#1") || hex.startsWith("#0") || hex.startsWith("#2")) Color.White else Color(0xFF0A84FF), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Tab 1: Gradients
            if (selectedCategory == 1) {
                Column {
                    Text("Apple Spatial & Studio Gradients", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = InterFontFamily)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(ChatWallpaperPresets.gradientPresets) { (key, colorsList) ->
                            val isSelected = previewConfig.type == WallpaperType.GRADIENT && previewConfig.value == key

                            Box(
                                modifier = Modifier
                                    .width(68.dp)
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.verticalGradient(colorsList))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        previewConfig = previewConfig.copy(
                                            type = WallpaperType.GRADIENT,
                                            value = key
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Lucide.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Tab 2: WhatsApp Doodles
            if (selectedCategory == 2) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("WhatsApp Doodle Pattern", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = InterFontFamily)
                            Text("Subtle hand-drawn chat doodles", fontSize = 12.sp, color = textSecondary, fontFamily = InterFontFamily)
                        }
                        AppleSwitch(
                            checked = previewConfig.type == WallpaperType.DOODLE || previewConfig.doodleEnabled,
                            onCheckedChange = { enabled ->
                                previewConfig = if (enabled) {
                                    previewConfig.copy(
                                        type = WallpaperType.DOODLE,
                                        value = if (previewConfig.value.isBlank()) "#EFEAE2" else previewConfig.value,
                                        doodleEnabled = true
                                    )
                                } else {
                                    previewConfig.copy(
                                        type = WallpaperType.SOLID,
                                        doodleEnabled = false
                                    )
                                }
                            }
                        )
                    }

                    // Doodle Opacity Slider
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Doodle Intensity", fontSize = 13.sp, color = textSecondary, fontFamily = InterFontFamily)
                            Text("${(previewConfig.doodleOpacity * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary, fontFamily = InterFontFamily)
                        }
                        Spacer(Modifier.height(4.dp))
                        AppleSlider(
                            value = previewConfig.doodleOpacity,
                            onValueChange = { previewConfig = previewConfig.copy(doodleOpacity = it) },
                            valueRange = 0.05f..0.35f
                        )
                    }

                    // Base Color for Doodle
                    Text("Doodle Base Color", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = InterFontFamily)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("#EFEAE2", "#D7E6DB", "#D3E5F3", "#E2DEF2", "#F4DFDF", "#1E242B", "#0F0F14")) { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.LightGray }
                            val isSelected = previewConfig.value == hex

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color(0xFF0A84FF) else Color(0x33000000), CircleShape)
                                    .clickable {
                                        previewConfig = previewConfig.copy(
                                            type = WallpaperType.DOODLE,
                                            value = hex,
                                            doodleEnabled = true
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Lucide.Check, null, tint = if (hex.startsWith("#1") || hex.startsWith("#0")) Color.White else Color(0xFF0A84FF), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Tab 3: Custom Photos
            if (selectedCategory == 3) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF))
                    ) {
                        Icon(Lucide.Camera, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Choose Photo from Gallery", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }

                    // Dimming Slider
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Wallpaper Dimming", fontSize = 13.sp, color = textSecondary, fontFamily = InterFontFamily)
                            Text("${(previewConfig.dimAmount * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary, fontFamily = InterFontFamily)
                        }
                        Spacer(Modifier.height(4.dp))
                        AppleSlider(
                            value = previewConfig.dimAmount,
                            onValueChange = { previewConfig = previewConfig.copy(dimAmount = it) },
                            valueRange = 0.0f..0.60f
                        )
                    }

                    // Blur Slider
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Frosted Glass Blur", fontSize = 13.sp, color = textSecondary, fontFamily = InterFontFamily)
                            Text("${previewConfig.blurAmount.toInt()} dp", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary, fontFamily = InterFontFamily)
                        }
                        Spacer(Modifier.height(4.dp))
                        AppleSlider(
                            value = previewConfig.blurAmount,
                            onValueChange = { previewConfig = previewConfig.copy(blurAmount = it) },
                            valueRange = 0.0f..20.0f
                        )
                    }
                }
            }

            // Tab 4: Themes & Text Size
            if (selectedCategory == 4) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Bubble Theme Color", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = InterFontFamily)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ChatTheme.values()) { theme ->
                            FilterChip(
                                selected = theme == chatTheme,
                                onClick = { settingsManager.setTheme(theme) },
                                label = { Text(theme.name, fontFamily = InterFontFamily) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Text("Text Size", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = InterFontFamily)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ChatTextSize.values()) { size ->
                            FilterChip(
                                selected = size == chatTextSize,
                                onClick = { settingsManager.setTextSize(size) },
                                label = { Text(size.name, fontFamily = InterFontFamily) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Tab 5: Bubbles
            if (selectedCategory == 5) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Bubble Style", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = InterFontFamily)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BubbleStyle.values()) { style ->
                            FilterChip(
                                selected = style == previewConfig.bubbleStyle,
                                onClick = { previewConfig = previewConfig.copy(bubbleStyle = style) },
                                label = { Text(style.name, fontFamily = InterFontFamily) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Text("Custom Colors (Overrides Theme)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = InterFontFamily)
                    
                    Text("Incoming Bubble Color", fontSize = 13.sp, color = textSecondary, fontFamily = InterFontFamily)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = previewConfig.customIncomingColor == null,
                                onClick = { previewConfig = previewConfig.copy(customIncomingColor = null) },
                                label = { Text("Default", fontFamily = InterFontFamily) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        items(ChatWallpaperPresets.solidColors) { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.LightGray }
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(color).border(if (previewConfig.customIncomingColor == hex) 3.dp else 1.dp, if (previewConfig.customIncomingColor == hex) Color(0xFF0A84FF) else Color(0x33000000), CircleShape)
                                .clickable { previewConfig = previewConfig.copy(customIncomingColor = hex) }
                            )
                        }
                    }

                    Text("Outgoing Bubble Color", fontSize = 13.sp, color = textSecondary, fontFamily = InterFontFamily)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = previewConfig.customOutgoingColor == null,
                                onClick = { previewConfig = previewConfig.copy(customOutgoingColor = null) },
                                label = { Text("Default", fontFamily = InterFontFamily) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        items(ChatWallpaperPresets.solidColors) { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.LightGray }
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(color).border(if (previewConfig.customOutgoingColor == hex) 3.dp else 1.dp, if (previewConfig.customOutgoingColor == hex) Color(0xFF0A84FF) else Color(0x33000000), CircleShape)
                                .clickable { previewConfig = previewConfig.copy(customOutgoingColor = hex) }
                            )
                        }
                    }
                    Text("Text Colors", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = InterFontFamily)
                    
                    Text("Incoming Text Color", fontSize = 13.sp, color = textSecondary, fontFamily = InterFontFamily)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = previewConfig.customIncomingTextColor == null,
                                onClick = { previewConfig = previewConfig.copy(customIncomingTextColor = null) },
                                label = { Text("Default", fontFamily = InterFontFamily) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        items(listOf("#FFFFFF", "#000000", "#FF3B30", "#007AFF", "#34C759", "#FF9500")) { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.LightGray }
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(color).border(if (previewConfig.customIncomingTextColor == hex) 3.dp else 1.dp, if (previewConfig.customIncomingTextColor == hex) Color(0xFF0A84FF) else Color(0x33000000), CircleShape)
                                .clickable { previewConfig = previewConfig.copy(customIncomingTextColor = hex) }
                            )
                        }
                    }

                    Text("Outgoing Text Color", fontSize = 13.sp, color = textSecondary, fontFamily = InterFontFamily)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = previewConfig.customOutgoingTextColor == null,
                                onClick = { previewConfig = previewConfig.copy(customOutgoingTextColor = null) },
                                label = { Text("Default", fontFamily = InterFontFamily) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        items(listOf("#FFFFFF", "#000000", "#FF3B30", "#007AFF", "#34C759", "#FF9500")) { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.LightGray }
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(color).border(if (previewConfig.customOutgoingTextColor == hex) 3.dp else 1.dp, if (previewConfig.customOutgoingTextColor == hex) Color(0xFF0A84FF) else Color(0x33000000), CircleShape)
                                .clickable { previewConfig = previewConfig.copy(customOutgoingTextColor = hex) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Apply for this chat
                LiquidGlassPrimaryButton(
                    onClick = {
                        settingsManager.setWallpaperForRoom(previewConfig, roomId)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text(if (roomId != null) "Set Appearance for This Chat" else "Save Appearance", fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = InterFontFamily, color = Color.White)
                }

                // Apply as global default
                if (roomId != null) {
                    LiquidGlassSecondaryButton(
                        onClick = {
                            settingsManager.setWallpaperForRoom(previewConfig, null)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Set as Default for All Chats", color = Color.Black, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, fontSize = 13.5.sp)
                    }
                }

                // Reset Button
                TextButton(
                    onClick = {
                        settingsManager.resetWallpaper(roomId)
                        previewConfig = WallpaperConfig(WallpaperType.AURORA)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Default Aurora Space", color = Color(0xFFFF3B30), fontSize = 13.sp, fontFamily = InterFontFamily)
                }
            }
        }
    }
}
