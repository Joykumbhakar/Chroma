package com.chroma.studio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.ChromaWork
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.ui.theme.LocalChromaColors
import com.chroma.studio.ui.components.glossyBorder
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.RefreshCcw
import com.composables.icons.lucide.X
import com.composables.icons.lucide.Check
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    deletedWorks: List<ChromaWork>,
    selectedWorkIds: Set<String>,
    selectionMode: Boolean,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onRestoreSelected: () -> Unit,
    repository: WorkRepository,
    onRestore: (String) -> Unit,
    onPermanentDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalChromaColors.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var singleDeleteId by remember { mutableStateOf<String?>(null) }
    var deletingWorkIds by remember { mutableStateOf(emptySet<String>()) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            if (selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.primary)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClearSelection) {
                            Icon(Lucide.X, "Cancel", tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${selectedWorkIds.size} Selected",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row {
                        IconButton(onClick = onRestoreSelected) {
                            Icon(Lucide.RefreshCcw, "Restore Selected", tint = Color.White)
                        }
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Lucide.Trash2, "Delete Selected", tint = Color.White)
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = { Text("Trash", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.textMain) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Lucide.ChevronLeft, "Back", tint = colors.textMain)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.bg
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        containerColor = colors.bg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (deletedWorks.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Lucide.Trash2, "Empty Trash", modifier = Modifier.size(64.dp), tint = colors.textMuted.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("Trash is empty", color = colors.textMuted, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(deletedWorks, key = { it.id }) { work ->
                        val isSelected = selectedWorkIds.contains(work.id)
                        AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.9f)) {
                            TrashCard(
                                work = work,
                                isSelected = isSelected,
                                isDeleting = deletingWorkIds.contains(work.id),
                                selectionMode = selectionMode,
                                repository = repository,
                                onClick = { 
                                    if (selectionMode) onToggleSelection(work.id) 
                                },
                                onLongClick = { onToggleSelection(work.id) },
                                onRestore = { onRestore(work.id) },
                                onDelete = { singleDeleteId = work.id; showDeleteConfirmDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        val count = singleDeleteId?.let { 1 } ?: selectedWorkIds.size
        DeleteConfirmationDialog(
            count = count,
            onConfirm = { 
                if (singleDeleteId != null) {
                    val id = singleDeleteId!!
                    deletingWorkIds = deletingWorkIds + id
                    showDeleteConfirmDialog = false
                    singleDeleteId = null
                    coroutineScope.launch {
                        delay(300)
                        onPermanentDelete(id)
                        deletingWorkIds = deletingWorkIds - id
                    }
                } else {
                    val idsToDelete = selectedWorkIds.toSet()
                    deletingWorkIds = deletingWorkIds + idsToDelete
                    showDeleteConfirmDialog = false
                    coroutineScope.launch {
                        delay(300)
                        onDeleteSelected()
                        deletingWorkIds = deletingWorkIds - idsToDelete
                    }
                }
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                singleDeleteId = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrashCard(
    work: ChromaWork,
    isSelected: Boolean,
    isDeleting: Boolean,
    selectionMode: Boolean,
    repository: WorkRepository,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalChromaColors.current
    var showMenu by remember { mutableStateOf(false) }
    
    val deleteAlpha by animateFloatAsState(if (isDeleting) 0f else 1f, tween(300), label = "alpha")
    val deleteScale by animateFloatAsState(if (isDeleting) 0.5f else 1f, tween(300), label = "scale")
    val baseScale by animateFloatAsState(if (isSelected) 0.92f else 1f, label = "baseScale")
    val scale = baseScale * deleteScale
    
    val borderWidth = if (isSelected) 3.dp else 1.dp
    val borderColor = if (isSelected) colors.primary else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(deleteAlpha)
            .clip(RoundedCornerShape(18.dp))
            .glossyBorder(RoundedCornerShape(18.dp), colors)
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .background(colors.glassBg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                val layers = remember(work.layersJson) { repository.deserializeLayers(work.layersJson) }
                if (layers.isNotEmpty()) {
                    CanvasPreview(layers = layers, shape = work.canvasShape, borderColor = Color.Transparent, isStatic = true, modifier = Modifier.fillMaxSize())
                }
                
                // Dark overlay to indicate it's deleted
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.primary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(colors.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Check, "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    work.name,
                    color = colors.textMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                if (!selectionMode) {
                    Box {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { showMenu = true }
                        )
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .glossyBorder(RoundedCornerShape(12.dp), colors)
                                .background(colors.glassBg, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Restore", fontSize = 13.sp, color = colors.textMain) },
                                leadingIcon = { Icon(Lucide.RefreshCcw, null, tint = colors.textMain, modifier = Modifier.size(15.dp)) },
                                onClick = { showMenu = false; onRestore() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Permanently", fontSize = 13.sp, color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Lucide.Trash2, null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp)) },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalChromaColors.current
    var dontAsk by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFE6F0FA)) // A soft blue-white matching the screenshot
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Delete Permanently",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Are you sure you want to permanently delete ${if (count == 1) "this work" else "these $count works"}?\nThis action cannot be undone.",
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { dontAsk = !dontAsk }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (dontAsk) Color(0xFFE5E7EB) else Color.Transparent)
                            .border(1.dp, Color(0xFFA0AAB4), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dontAsk) {
                            Icon(Lucide.Check, null, modifier = Modifier.size(10.dp), tint = Color(0xFF4B5563))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Don't ask me again", fontSize = 13.sp, color = Color(0xFF6B7280))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Horizontal Divider
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFCBD5E1)))
                
                Row(
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = Color(0xFF2563EB), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    // Vertical Divider
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color(0xFFCBD5E1)))
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Delete", color = Color(0xFFEF4444), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

