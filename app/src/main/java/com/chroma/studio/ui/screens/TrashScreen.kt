package com.chroma.studio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.ChromaWork
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.glossyBorder
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

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
    onBack: () -> Unit,
    onSelectAll: (() -> Unit)? = null,
    onEmptyTrash: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var singleDeleteId by remember { mutableStateOf<String?>(null) }
    var deletingWorkIds by remember { mutableStateOf(emptySet<String>()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        onRefresh?.invoke()
    }

    DisposableEffect(Unit) {
        onDispose {
            onClearSelection()
        }
    }

    Scaffold(
        topBar = {
            ChromaScreenHeader(
                title = if (selectionMode) "${selectedWorkIds.size} Selected" else "Trash",
                badgeText = if (selectionMode) null else if (deletedWorks.isNotEmpty()) "${deletedWorks.size}" else null,
                badgeColor = Color(0xFFFF3B30),
                showBackButton = true,
                onBack = {
                    if (selectionMode) onClearSelection() else onBack()
                },
                actions = {
                    if (selectionMode) {
                        val allSelected = deletedWorks.isNotEmpty() && selectedWorkIds.size >= deletedWorks.size
                        // Deselect All / Select All
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (allSelected) colors.primary.copy(alpha = 0.22f) else colors.glassBgHover)
                                .border(1.dp, if (allSelected) colors.primary else colors.glassBorder, CircleShape)
                                .clickable {
                                    if (allSelected) onClearSelection() else onSelectAll?.invoke()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.CheckCheck,
                                contentDescription = if (allSelected) "Deselect All" else "Select All",
                                tint = if (allSelected) colors.primary else colors.textMain,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        // Restore Selected
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.glassBgHover)
                                .border(1.dp, colors.glassBorder, CircleShape)
                                .clickable { onRestoreSelected() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.RefreshCcw,
                                contentDescription = "Restore Selected",
                                tint = colors.textMain,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        // Delete Selected
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.4f), CircleShape)
                                .clickable { showDeleteConfirmDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Trash2,
                                contentDescription = "Delete Selected",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    } else if (deletedWorks.isNotEmpty()) {
                        // Quick Select button
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.glassBgHover)
                                .border(1.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                                .clickable { onSelectAll?.invoke() }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select",
                                color = colors.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        // Empty Trash button
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF3B30).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .clickable { showEmptyTrashDialog = true }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Trash2,
                                    contentDescription = "Empty",
                                    tint = Color(0xFFFF3B30),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Empty",
                                    color = Color(0xFFFF3B30),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            )
        },
        containerColor = colors.bg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (deletedWorks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30).copy(alpha = 0.08f))
                            .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Trash2,
                            contentDescription = "Empty Trash",
                            modifier = Modifier.size(42.dp),
                            tint = Color(0xFFFF3B30).copy(alpha = 0.75f)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Trash is Empty",
                        color = colors.textMain,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Works you delete will be kept here safely before permanent removal.",
                        color = colors.textMuted,
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassBgHover)
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                            .clickable { onBack() }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.ArrowLeft,
                                contentDescription = null,
                                tint = colors.textMain,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Back to Works",
                                color = colors.textMain,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Informative Cupertino Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassBg)
                            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Info,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Items in Trash can be restored to your studio or permanently removed.",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(deletedWorks, key = { it.id }) { work ->
                            val isSelected = selectedWorkIds.contains(work.id)
                            AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.92f)) {
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
                        delay(250)
                        onPermanentDelete(id)
                        deletingWorkIds = deletingWorkIds - id
                    }
                } else {
                    val idsToDelete = selectedWorkIds.toSet()
                    deletingWorkIds = deletingWorkIds + idsToDelete
                    showDeleteConfirmDialog = false
                    coroutineScope.launch {
                        delay(250)
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

    if (showEmptyTrashDialog) {
        com.chroma.studio.ui.components.IosAlertDialog(
            title = "Empty Trash",
            message = "Are you sure you want to permanently delete all ${deletedWorks.size} items in Trash?\nThis action cannot be undone.",
            confirmText = "Empty Trash",
            onConfirm = {
                showEmptyTrashDialog = false
                coroutineScope.launch {
                    val allIds = deletedWorks.map { it.id }.toSet()
                    deletingWorkIds = deletingWorkIds + allIds
                    delay(250)
                    onEmptyTrash?.invoke()
                    deletingWorkIds = emptySet()
                }
            },
            dismissText = "Cancel",
            onDismiss = { showEmptyTrashDialog = false },
            isDestructive = true
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
    val colors = LocalOscellionColors.current
    var showMenu by remember { mutableStateOf(false) }
    
    val deleteAlpha by animateFloatAsState(if (isDeleting) 0f else 1f, tween(250), label = "alpha")
    val deleteScale by animateFloatAsState(if (isDeleting) 0.6f else 1f, tween(250), label = "scale")
    val baseScale by animateFloatAsState(if (isSelected) 0.94f else 1f, label = "baseScale")
    val scale = baseScale * deleteScale
    
    val borderWidth = if (isSelected) 2.5.dp else 1.dp
    val borderColor = if (isSelected) colors.primary else Color.Transparent

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateString = remember(work.lastModifiedAt) { dateFormat.format(java.util.Date(work.lastModifiedAt)) }
    val layers = remember(work.layersJson) { repository.deserializeLayers(work.layersJson) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(deleteAlpha)
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x1F000000)
            )
            .clip(RoundedCornerShape(18.dp))
            .glossyBorder(RoundedCornerShape(18.dp), colors)
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .background(colors.glassBg)
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onClick()
                    } else {
                        showMenu = true
                    }
                },
                onLongClick = onLongClick
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.22f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E1E26), Color(0xFF141418))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (layers.isEmpty()) {
                    Text(
                        work.name.take(1).uppercase(),
                        color = Color.White.copy(alpha = 0.2f),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    CanvasPreview(
                        layers = layers,
                        shape = work.canvasShape,
                        borderColor = Color.Transparent,
                        isStatic = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Dark scrim overlay for deleted item
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))

                // Top-right Deleted Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color(0x40FF3B30), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Trash2,
                        contentDescription = null,
                        tint = Color(0xFFFF453A),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "Deleted",
                        color = Color(0xFFFF453A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Selection check indicator
                if (selectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(colors.primary, CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.75f), CircleShape)
                            )
                        }
                    }
                }
            }

            // Bottom metadata and actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Layers,
                            contentDescription = null,
                            tint = colors.textMuted.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "${layers.size} ${if (layers.size == 1) "layer" else "layers"} • $dateString",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
                
                if (!selectionMode) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.glassBgHover.copy(alpha = 0.6f))
                                .border(0.5.dp, colors.glassBorder, CircleShape)
                                .clickable { showMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "Options",
                                tint = colors.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .glossyBorder(RoundedCornerShape(14.dp), colors)
                                .background(colors.glassBg, RoundedCornerShape(14.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Restore", fontSize = 13.sp, color = colors.textMain) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Lucide.RefreshCcw,
                                        contentDescription = null,
                                        tint = colors.textMain,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                onClick = { showMenu = false; onRestore() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Permanently", fontSize = 13.sp, color = Color(0xFFEF4444)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Lucide.Trash2,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
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
    com.chroma.studio.ui.components.IosAlertDialog(
        title = "Delete Permanently",
        message = "Are you sure you want to permanently delete ${if (count == 1) "this work" else "these $count works"}?\nThis action cannot be undone.",
        confirmText = "Delete",
        onConfirm = onConfirm,
        dismissText = "Cancel",
        onDismiss = onDismiss,
        isDestructive = true
    )
}
