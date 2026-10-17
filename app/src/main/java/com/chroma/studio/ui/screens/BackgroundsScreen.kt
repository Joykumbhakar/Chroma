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
import com.composables.icons.lucide.Lucide
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.chroma.studio.ui.components.glossyBorder

import com.chroma.studio.ui.components.ApplePhotosGraphic
import com.chroma.studio.ui.components.AppleEmptyStateView
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import com.chroma.studio.ui.graphics3d.Apple3DGraphicId
import com.chroma.studio.ui.graphics3d.Apple3DGraphicIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundsScreen(
    works: List<ChromaWork>,
    repository: com.chroma.studio.data.WorkRepository,
    onBack: () -> Unit,
    onSetAsBackground: (ChromaWork) -> Unit
) {
    val colors = LocalOscellionColors.current
    val backgroundWorks = remember(works) { works.filter { it.isHomeBackground } }

    Scaffold(
        topBar = {
            com.chroma.studio.ui.components.ChromaScreenHeader(
                title = "Oscellion",
                badgeText = "Backgrounds",
                showBackButton = true,
                onBack = onBack
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
                    subtitle = "Create a new 'Full' shape work in the studio to set it as your dynamic home background."
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
                items(backgroundWorks, key = { it.id }) { work ->
                    BackgroundCard(
                        work = work,
                        repository = repository,
                        onClick = { onSetAsBackground(work) }
                    )
                }
            }
        }
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
