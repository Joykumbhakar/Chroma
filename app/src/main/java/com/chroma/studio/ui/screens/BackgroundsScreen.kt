package com.chroma.studio.ui.screens
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.chroma.studio.ui.components.AppleDialog
import com.chroma.studio.ui.components.ChromaHeaderActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundsScreen(
    works: List<ChromaWork>,
    repository: com.chroma.studio.data.WorkRepository,
    isCustomBackgroundActive: Boolean = false,
    onBack: () -> Unit,
    onSetAsBackground: (ChromaWork) -> Unit,
    onResetToDefault: () -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    val backgroundWorks = remember(works) { works.filter { it.isHomeBackground } }

    var workToConfirm by remember { mutableStateOf<ChromaWork?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            com.chroma.studio.ui.components.ChromaScreenHeader(
                title = "Oscellion",
                badgeText = "Backgrounds",
                showBackButton = true,
                onBack = onBack,
                actions = {
                    if (isCustomBackgroundActive) {
                        ChromaHeaderActionButton(
                            icon = Lucide.RotateCcw,
                            contentDescription = "Reset to Default Background",
                            tint = colors.textMuted,
                            onClick = { showResetDialog = true }
                        )
                    }
                }
            )
        },
        containerColor = colors.bg
    ) { padding ->
        if (backgroundWorks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                val photosState = rememberAppleGraphicAnimationState()
                AppleEmptyStateView(
                    graphic = {
                        ApplePhotosGraphic(
                            modifier = Modifier.size(160.dp),
                            state = photosState,
                            enableClickToAnimate = true
                        )
                    },
                    title = "No Custom Backgrounds",
                    subtitle = if (isCustomBackgroundActive) {
                        "A custom background is currently active. You can restore the system default animated gradient anytime."
                    } else {
                        "Create a new 'Full' shape work in the studio to set it as your dynamic home background."
                    },
                    actionButtonText = if (isCustomBackgroundActive) "Reset to Default" else "Back to Works",
                    actionButtonIcon = if (isCustomBackgroundActive) Lucide.RotateCcw else Lucide.ArrowLeft,
                    onActionClick = {
                        if (isCustomBackgroundActive) {
                            showResetDialog = true
                        } else {
                            onBack()
                        }
                    }
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                if (isCustomBackgroundActive) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .glossyBorder(RoundedCornerShape(18.dp), colors)
                                .background(colors.glassBg)
                                .clickable { showResetDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Lucide.RotateCcw,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "Custom Background Active",
                                        color = colors.textMain,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Tap to restore system default gradient",
                                        color = colors.textMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                "Reset",
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                items(backgroundWorks, key = { it.id }) { work ->
                    BackgroundCard(
                        work = work,
                        repository = repository,
                        onClick = { workToConfirm = work }
                    )
                }
            }
        }
    }

    // Set Background Confirmation Dialog
    if (workToConfirm != null) {
        val work = workToConfirm!!
        AppleDialog(
            title = "Set as Home Background?",
            message = "Do you want to set \"${work.name}\" as your Home screen background?",
            primaryButtonText = "Set Background",
            onPrimaryClick = {
                val target = work
                workToConfirm = null
                onSetAsBackground(target)
            },
            secondaryButtonText = "Cancel",
            onSecondaryClick = { workToConfirm = null },
            onDismiss = { workToConfirm = null }
        )
    }

    // Reset to Default Confirmation Dialog
    if (showResetDialog) {
        AppleDialog(
            title = "Reset to Default Background?",
            message = "Are you sure you want to remove your custom Home background and restore the default animated gradient?",
            primaryButtonText = "Reset",
            primaryIsDestructive = true,
            onPrimaryClick = {
                showResetDialog = false
                onResetToDefault()
            },
            secondaryButtonText = "Cancel",
            onSecondaryClick = { showResetDialog = false },
            onDismiss = { showResetDialog = false }
        )
    }
}

@Composable
private fun BackgroundCard(
    work: ChromaWork,
    repository: com.chroma.studio.data.WorkRepository,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, colors.glassBorder, RoundedCornerShape(22.dp))
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
                animStatus = com.chroma.studio.model.AnimStatus.PAUSED,
                animStyle = com.chroma.studio.model.AnimStyle.DRIFT,
                animSpeed = 0f,
                animAmount = 0f,
                reactOffset = androidx.compose.ui.geometry.Offset.Zero,
                textContent = "CHROMA",
                onTextContentChange = {},
                blobDragOverrides = emptyMap(),
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                work.name,
                color = colors.textMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
