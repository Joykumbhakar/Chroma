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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.chroma.studio.ui.components.AppleTrashBinIcon
import com.chroma.studio.ui.components.rememberAppleTrashState
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private fun Modifier.gradientTint(brush: Brush): Modifier = this
    .graphicsLayer { alpha = 0.99f }
    .drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(brush, blendMode = BlendMode.SrcIn)
        }
    }

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

    val buttonCorner = CircleShape

    val primaryGradient = Brush.linearGradient(
        listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
    )
    val dangerGradient = Brush.linearGradient(
        listOf(Color(0xFFDC2626), Color(0xFFB91C1C))
    )

    val selectIconGradient = Brush.linearGradient(
        listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFFA855F7))
    )
    val restoreIconGradient = Brush.linearGradient(
        listOf(Color(0xFF34D399), Color(0xFF06B6D4), Color(0xFF3B82F6))
    )
    val dangerIconGradient = Brush.linearGradient(
        listOf(Color(0xFFFF6B6B), Color(0xFFEF4444), Color(0xFFDC2626))
    )
    val subtleIconGradient = Brush.linearGradient(
        listOf(colors.textMuted, colors.textMuted.copy(alpha = 0.6f))
    )

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
                title = if (selectionMode) {
                    "${selectedWorkIds.size} Selected"
                } else if (deletedWorks.isNotEmpty()) {
                    "Trash (${deletedWorks.size})"
                } else {
                    "Trash"
                },
                badgeText = null, // No pill badge - clean text used in title
                showBackButton = true,
                circularBackButton = false,
                onBack = {
                    if (selectionMode) onClearSelection() else onBack()
                },
                actions = {
                    if (selectionMode) {
                        val allSelected = deletedWorks.isNotEmpty() && selectedWorkIds.size >= deletedWorks.size
                        // Deselect All / Select All Icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    if (allSelected) onClearSelection() else onSelectAll?.invoke()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.CheckCheck,
                                contentDescription = if (allSelected) "Deselect All" else "Select All",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .gradientTint(if (allSelected) selectIconGradient else subtleIconGradient)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        // Restore Selected Icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { onRestoreSelected() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.RefreshCcw,
                                contentDescription = "Restore",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(19.dp)
                                    .gradientTint(restoreIconGradient)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        // Delete Selected Icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { showDeleteConfirmDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Trash2,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .gradientTint(dangerIconGradient)
                            )
                        }
                    } else if (deletedWorks.isNotEmpty()) {
                        // Quick Select Icon Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { onSelectAll?.invoke() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.ListCheck,
                                contentDescription = "Select",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(21.dp)
                                    .gradientTint(selectIconGradient)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        // Empty Trash Icon Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { showEmptyTrashDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Trash2,
                                contentDescription = "Empty Trash",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .gradientTint(dangerIconGradient)
                            )
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
                val emptyTrashState = rememberAppleTrashState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AppleTrashBinIcon(
                        modifier = Modifier.size(175.dp),
                        state = emptyTrashState,
                        enableClickToAnimate = true
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Trash is Empty",
                        color = colors.textMain,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Works you delete will be kept here safely before permanent removal.",
                        color = colors.textMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(22.dp))
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(buttonCorner)
                            .background(primaryGradient)
                            .border(0.75.dp, Color.White.copy(alpha = 0.3f), buttonCorner)
                            .clickable { onBack() }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.ArrowLeft,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Back to Works",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                        items(deletedWorks, key = { it.id }) { work ->
                            val isSelected = selectedWorkIds.contains(work.id)
                            AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.94f)) {
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
                        delay(200)
                        onPermanentDelete(id)
                        deletingWorkIds = deletingWorkIds - id
                    }
                } else {
                    val idsToDelete = selectedWorkIds.toSet()
                    deletingWorkIds = deletingWorkIds + idsToDelete
                    showDeleteConfirmDialog = false
                    coroutineScope.launch {
                        delay(200)
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
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { showEmptyTrashDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}) {
                    com.chroma.studio.ui.graphics3d.AppleHigDialogCard(
                        graphicId = com.chroma.studio.ui.graphics3d.Apple3DGraphicId.TRASH_BIN,
                        title = "Empty Trash?",
                        description = "This action cannot be undone.",
                        cancelText = "Cancel",
                        confirmText = "Empty Trash",
                        onCancel = { showEmptyTrashDialog = false },
                        onConfirm = {
                            showEmptyTrashDialog = false
                            coroutineScope.launch {
                                val allIds = deletedWorks.map { it.id }.toSet()
                                deletingWorkIds = deletingWorkIds + allIds
                                delay(200)
                                onEmptyTrash?.invoke()
                                deletingWorkIds = emptySet()
                            }
                        }
                    )
                }
            }
        }
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
    
    val deleteAlpha by animateFloatAsState(if (isDeleting) 0f else 1f, tween(200), label = "alpha")
    val deleteScale by animateFloatAsState(if (isDeleting) 0.6f else 1f, tween(200), label = "scale")
    val baseScale by animateFloatAsState(if (isSelected) 0.95f else 1f, label = "baseScale")
    val scale = baseScale * deleteScale
    
    val cardCorner = RoundedCornerShape(12.dp)
    val checkCorner = RoundedCornerShape(6.dp)

    val borderWidth = if (isSelected) 2.dp else 0.75.dp
    val borderColor = if (isSelected) colors.primary else colors.glassBorder

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateString = remember(work.lastModifiedAt) { dateFormat.format(java.util.Date(work.lastModifiedAt)) }
    val layers = remember(work.layersJson) { repository.deserializeLayers(work.layersJson) }

    val primaryGradient = Brush.linearGradient(
        listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
    )
    val restoreIconGradient = Brush.linearGradient(
        listOf(Color(0xFF34D399), Color(0xFF06B6D4), Color(0xFF3B82F6))
    )
    val dangerIconGradient = Brush.linearGradient(
        listOf(Color(0xFFFF6B6B), Color(0xFFEF4444), Color(0xFFDC2626))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(deleteAlpha)
            .shadow(
                elevation = if (isSelected) 6.dp else 1.5.dp,
                shape = cardCorner,
                ambientColor = Color(0x14000000),
                spotColor = Color(0x1F000000)
            )
            .clip(cardCorner)
            .glossyBorder(cardCorner, colors)
            .border(borderWidth, borderColor, cardCorner)
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
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
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

                // Clean thin outlined trash icon in top-right (NO pill/capsule badge)
                Icon(
                    imageVector = Lucide.Trash2,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(9.dp)
                        .size(15.dp)
                        .gradientTint(dangerIconGradient)
                )

                // Selection check indicator (Clean square with 6.dp radius, thin glass border)
                if (selectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(primaryGradient, checkCorner)
                                    .border(0.75.dp, Color.White, checkCorner),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), checkCorner)
                                    .border(1.dp, Color.White.copy(alpha = 0.7f), checkCorner)
                            )
                        }
                    }
                }
            }

            // Bottom metadata and actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 11.dp, vertical = 9.dp),
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
                    // Clean Text line: "Deleted • X layers • Date" (NO pill badge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Deleted",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "•",
                            color = colors.textMuted.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${layers.size} ${if (layers.size == 1) "layer" else "layers"}",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "•",
                            color = colors.textMuted.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = dateString,
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
                
                if (!selectionMode) {
                    Box {
                        // Action menu button: Clean floating icon button (no background)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { showMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "Options",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.15f))
                                .clip(RoundedCornerShape(24.dp))
                                .glossyBorder(RoundedCornerShape(24.dp), colors)
                                .background(colors.glassBg, RoundedCornerShape(24.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Restore", fontSize = 13.sp, color = colors.textMain) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Lucide.RefreshCcw,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .gradientTint(restoreIconGradient)
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
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .gradientTint(dangerIconGradient)
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
    val deleteDialogTrashState = rememberAppleTrashState()
    LaunchedEffect(Unit) {
        deleteDialogTrashState.triggerDeleteAnimation()
    }
    com.chroma.studio.ui.components.IosAlertDialog(
        title = "Delete Permanently",
        message = "Are you sure you want to permanently delete ${if (count == 1) "this work" else "these $count works"}?\nThis action cannot be undone.",
        confirmText = "Delete",
        onConfirm = onConfirm,
        dismissText = "Cancel",
        onDismiss = onDismiss,
        isDestructive = true,
        customContent = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AppleTrashBinIcon(
                    modifier = Modifier.size(86.dp),
                    state = deleteDialogTrashState,
                    enableClickToAnimate = true
                )
            }
        }
    )
}
