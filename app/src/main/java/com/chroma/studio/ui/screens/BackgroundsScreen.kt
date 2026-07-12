package com.chroma.studio.ui.screens

import androidx.compose.foundation.background
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
import com.chroma.studio.ui.theme.LocalChromaColors
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.chroma.studio.ui.components.glossyBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundsScreen(
    works: List<ChromaWork>,
    repository: com.chroma.studio.data.WorkRepository,
    onBack: () -> Unit,
    onSetAsBackground: (ChromaWork) -> Unit
) {
    val colors = LocalChromaColors.current
    val backgroundWorks = remember(works) { works.filter { it.isHomeBackground } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backgrounds", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textMain) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Lucide.ChevronLeft, "Back", tint = colors.textMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.bg
                )
            )
        },
        containerColor = colors.bg
    ) { padding ->
        if (backgroundWorks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No custom backgrounds found.\n\nCreate a new 'Full' shape work to set it as your home background.",
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(32.dp)
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
    val colors = LocalChromaColors.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .glossyBorder(RoundedCornerShape(18.dp), colors)
            .background(colors.glassBg)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
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
