package com.chroma.studio.ui.screens
import com.chroma.studio.ui.components.OneUiViewingArea
import com.chroma.studio.ui.components.OneUiStickyTopBar
import com.chroma.studio.ui.components.rememberOneUiGradientStyle
import com.chroma.studio.ui.components.GlassIconButton

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.ChromaWork
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.chroma.studio.ui.components.glossyBorder

import com.chroma.studio.ui.components.ApplePhotosGraphic
import com.chroma.studio.ui.components.AppleEmptyStateView
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import com.chroma.studio.ui.graphics3d.Apple3DGraphicId
import com.chroma.studio.ui.graphics3d.Apple3DGraphicIcon
import com.composables.icons.lucide.RotateCcw
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Palette
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.chroma.studio.ui.components.AppleDialog
import com.chroma.studio.ui.components.ChromaHeaderActionButton
import com.chroma.studio.ui.components.ToastManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundsScreen(
    works: List<ChromaWork>,
    repository: com.chroma.studio.data.WorkRepository,
    isCustomBackgroundActive: Boolean = false,
    currentCustomBgLayersJson: String? = null,
    onBack: () -> Unit,
    onSetAsBackground: (ChromaWork) -> Unit,
    onResetToDefault: () -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    val (gradientStyle, cycleGradient) = rememberOneUiGradientStyle()
    val backgroundWorks = remember(works) { works.filter { it.isHomeBackground } }

    var workToConfirm by remember { mutableStateOf<ChromaWork?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OneUiStickyTopBar(
                title = "Wallpapers",
                onBack = onBack,
                gradientStyle = gradientStyle,
                onCycleGradient = cycleGradient,
                actions = {
                    if (isCustomBackgroundActive) {
                        IconButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.RotateCcw,
                                contentDescription = "Reset to Default Background",
                                tint = colors.textMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            )
        },
        containerColor = colors.bg
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 145.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            // Section Header & One UI Viewing Area
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OneUiViewingArea(
                        title = "Wallpapers",
                        subtitle = if (isCustomBackgroundActive) "Custom wallpaper active on Home" else "Default dynamic gradient active",
                        gradientStyle = gradientStyle,
                        onCycleGradient = cycleGradient,
                        height = 145.dp
                    )
                    Text(
                        text = "SELECT A DESIGN TO SET AS YOUR DYNAMIC HOME SCREEN BACKGROUND",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted.copy(alpha = 0.8f),
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // 1. System Default Background Card
            item(key = "system_default_background") {
                DefaultBackgroundCard(
                    repository = repository,
                    isActive = !isCustomBackgroundActive,
                    onClick = {
                        if (isCustomBackgroundActive) {
                            showResetDialog = true
                        } else {
                            ToastManager.showToast("Default gradient is already active")
                        }
                    }
                )
            }

            // 2. Custom Background Cards
            items(backgroundWorks, key = { it.id }) { work ->
                val isActive = currentCustomBgLayersJson != null && work.layersJson == currentCustomBgLayersJson
                BackgroundCard(
                    work = work,
                    repository = repository,
                    isActive = isActive,
                    onClick = {
                        if (isActive) {
                            ToastManager.showToast("\"${work.name}\" is already active")
                        } else {
                            workToConfirm = work
                        }
                    }
                )
            }

            // 3. Informative prompt when no custom works exist
            if (backgroundWorks.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                            .background(colors.glassBg)
                            .clickable { onBack() }
                            .padding(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Palette,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "Design Custom Backgrounds",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Text(
                                text = "Create a work with 'Full' canvas shape in the studio and it will automatically appear here as a wallpaper option.",
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Set Background Confirmation Dialog with Live Animated Preview
    if (workToConfirm != null) {
        val work = workToConfirm!!
        val previewLayers = remember(work.layersJson) { repository.deserializeLayers(work.layersJson) }
        AppleDialog(
            title = "Set as Home Background?",
            message = "Apply \"${work.name}\" to your Home screen?",
            primaryButtonText = "Set Background",
            onPrimaryClick = {
                val target = work
                workToConfirm = null
                onSetAsBackground(target)
            },
            secondaryButtonText = "Cancel",
            onSecondaryClick = { workToConfirm = null },
            onDismiss = { workToConfirm = null },
            customContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                ) {
                    CanvasPreview(
                        layers = previewLayers,
                        shape = work.canvasShape,
                        borderColor = Color.Transparent,
                        colorBlindMode = com.chroma.studio.model.ColorBlindMode.NONE,
                        postFxMode = com.chroma.studio.model.PostProcessingFx.NONE,
                        animStatus = com.chroma.studio.model.AnimStatus.PLAYING,
                        animStyle = com.chroma.studio.model.AnimStyle.DRIFT,
                        animSpeed = 35f,
                        animAmount = 35f,
                        reactOffset = androidx.compose.ui.geometry.Offset.Zero,
                        textContent = "",
                        onTextContentChange = {},
                        blobDragOverrides = emptyMap(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        )
    }

    // Reset to Default Confirmation Dialog with Live Animated Default Preview
    if (showResetDialog) {
        val defaultLayers = remember { repository.deserializeLayers(DEFAULT_HOME_BG_JSON) }
        AppleDialog(
            title = "Reset to Default?",
            message = "Restore the original animated gradient background for your Home screen?",
            primaryButtonText = "Reset",
            primaryIsDestructive = true,
            onPrimaryClick = {
                showResetDialog = false
                onResetToDefault()
            },
            secondaryButtonText = "Cancel",
            onSecondaryClick = { showResetDialog = false },
            onDismiss = { showResetDialog = false },
            customContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                ) {
                    CanvasPreview(
                        layers = defaultLayers,
                        shape = "full",
                        borderColor = Color.Transparent,
                        colorBlindMode = com.chroma.studio.model.ColorBlindMode.NONE,
                        postFxMode = com.chroma.studio.model.PostProcessingFx.NONE,
                        animStatus = com.chroma.studio.model.AnimStatus.PLAYING,
                        animStyle = com.chroma.studio.model.AnimStyle.DRIFT,
                        animSpeed = 35f,
                        animAmount = 35f,
                        reactOffset = androidx.compose.ui.geometry.Offset.Zero,
                        textContent = "",
                        onTextContentChange = {},
                        blobDragOverrides = emptyMap(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        )
    }
}

@Composable
private fun DefaultBackgroundCard(
    repository: com.chroma.studio.data.WorkRepository,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val defaultLayers = remember { repository.deserializeLayers(DEFAULT_HOME_BG_JSON) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) colors.primary else colors.glassBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .background(colors.glassBg)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
        ) {
            CanvasPreview(
                layers = defaultLayers,
                shape = "full",
                borderColor = Color.Transparent,
                colorBlindMode = com.chroma.studio.model.ColorBlindMode.NONE,
                postFxMode = com.chroma.studio.model.PostProcessingFx.NONE,
                animStatus = com.chroma.studio.model.AnimStatus.PLAYING,
                animStyle = com.chroma.studio.model.AnimStyle.DRIFT,
                animSpeed = 30f,
                animAmount = 30f,
                reactOffset = androidx.compose.ui.geometry.Offset.Zero,
                textContent = "",
                onTextContentChange = {},
                blobDragOverrides = emptyMap(),
                modifier = Modifier.fillMaxSize()
            )

            if (isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF007AFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Active",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "System Default",
                    color = colors.textMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = if (isActive) "Current Background" else "Original Gradient",
                    color = if (isActive) colors.primary else colors.textMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BackgroundCard(
    work: ChromaWork,
    repository: com.chroma.studio.data.WorkRepository,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) colors.primary else colors.glassBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .background(colors.glassBg)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
        ) {
            CanvasPreview(
                layers = remember(work.layersJson) { repository.deserializeLayers(work.layersJson) },
                shape = work.canvasShape,
                borderColor = Color.Transparent,
                colorBlindMode = com.chroma.studio.model.ColorBlindMode.NONE,
                postFxMode = com.chroma.studio.model.PostProcessingFx.NONE,
                animStatus = if (isActive) com.chroma.studio.model.AnimStatus.PLAYING else com.chroma.studio.model.AnimStatus.PAUSED,
                animStyle = com.chroma.studio.model.AnimStyle.DRIFT,
                animSpeed = 30f,
                animAmount = 30f,
                reactOffset = androidx.compose.ui.geometry.Offset.Zero,
                textContent = "CHROMA",
                onTextContentChange = {},
                blobDragOverrides = emptyMap(),
                modifier = Modifier.fillMaxSize()
            )

            if (isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF007AFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Active",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = work.name,
                    color = colors.textMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = if (isActive) "Current Background" else "Custom Artwork",
                    color = if (isActive) colors.primary else colors.textMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
