package com.chroma.studio.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.ChromaWork
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.ui.theme.LocalChromaColors
import com.chroma.studio.ui.components.glossyBorder
import com.chroma.studio.ui.components.SmallSwitch
import com.composables.icons.lucide.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalChromaColors.current
    val bg = if (selected) colors.primary.copy(alpha = 0.15f) else Color.Transparent
    val tint = if (selected) colors.primary else colors.textMuted
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    works: List<ChromaWork>,
    selectedWorkIds: Set<String>,
    selectionMode: Boolean,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onNewWork: () -> Unit,
    onOpenWork: (ChromaWork) -> Unit,
    onRenameWork: (String, String, String) -> Unit, // id, name, desc
    onDuplicateWork: (ChromaWork) -> Unit,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSoftDelete: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit,
    repository: WorkRepository
) {
    val colors = LocalChromaColors.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredWorks = remember(works, searchQuery) {
        if (searchQuery.isBlank()) works
        else works.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.description.contains(searchQuery, ignoreCase = true) 
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.bg,
                modifier = Modifier.width(300.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
                    Text(
                        "Chroma Studio",
                        color = colors.textMain,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 32.dp, top = 24.dp, start = 8.dp)
                    )

                    DrawerItem(Icons.Default.Home, "Home", true) { scope.launch { drawerState.close() } }
                    DrawerItem(Lucide.Settings, "Settings & Preferences", false) { 
                        scope.launch { drawerState.close() }
                        onNavigateToSettings() 
                    }
                    DrawerItem(Lucide.Accessibility, "Accessibility", false) { 
                        scope.launch { drawerState.close() }
                        onNavigateToSettings() 
                    }
                    DrawerItem(Lucide.Trash2, "Trash", false) { 
                        scope.launch { drawerState.close() }
                        onNavigateToTrash() 
                    }
                    
                    Spacer(Modifier.weight(1f))
                    
                    // Dark mode toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onToggleDarkMode() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(if (isDarkMode) Lucide.Moon else Lucide.Sun, "Theme", tint = colors.textMain, modifier = Modifier.size(20.dp))
                            Text(
                                "Dark Mode",
                                color = colors.textMain,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                        SmallSwitch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleDarkMode() }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ChromaAnimatedBackground()

            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                // ── Top bar ──
                if (selectionMode) {
                    // Contextual Action Bar for Selection Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.primary)
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
                        IconButton(onClick = onDeleteSelected) {
                            Icon(Lucide.Trash2, "Delete Selected", tint = Color.White)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Icon(Lucide.Menu, "Menu", tint = colors.textMain)
                            }
                            Column {
                                Text(
                                    "My Works",
                                    color = colors.textMain,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    "${works.size} ${if (works.size == 1) "work" else "works"}",
                                    color = colors.textMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // ── New Work button ──
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .glossyBorder(RoundedCornerShape(14.dp), colors)
                                .background(colors.primary.copy(alpha = 0.85f))
                                .clickable { onNewWork() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Lucide.Plus, contentDescription = "New", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("New", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // ── Search Bar ──
                if (works.isNotEmpty() && !selectionMode) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = colors.textMain, fontSize = 14.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .glossyBorder(RoundedCornerShape(14.dp), colors)
                            .background(colors.glassBg)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Lucide.Search, null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text("Search works...", color = colors.textMuted, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        Lucide.X, 
                                        null, 
                                        tint = colors.textMuted, 
                                        modifier = Modifier.size(16.dp).clickable { searchQuery = "" }
                                    )
                                }
                            }
                        }
                    )
                }

                // ── Work grid ──
                if (works.isEmpty()) {
                    EmptyState(onNewWork = onNewWork)
                } else if (filteredWorks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No works found for \"$searchQuery\"", color = colors.textMuted, fontSize = 15.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredWorks, key = { it.id }) { work ->
                            val isSelected = selectedWorkIds.contains(work.id)
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + scaleIn(initialScale = 0.92f)
                            ) {
                                WorkCard(
                                    work = work,
                                    isSelected = isSelected,
                                    selectionMode = selectionMode,
                                    repository = repository,
                                    onClick = { 
                                        if (selectionMode) onToggleSelection(work.id) 
                                        else onOpenWork(work) 
                                    },
                                    onLongClick = { onToggleSelection(work.id) },
                                    onRename = { name, desc -> onRenameWork(work.id, name, desc) },
                                    onDuplicate = { onDuplicateWork(work) },
                                    onSoftDelete = { onSoftDelete(work.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkCard(
    work: ChromaWork,
    isSelected: Boolean,
    selectionMode: Boolean,
    repository: WorkRepository,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: (String, String) -> Unit,
    onDuplicate: () -> Unit,
    onSoftDelete: () -> Unit
) {
    val colors = LocalChromaColors.current
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(if (isSelected) 0.92f else 1f)
    val borderWidth = if (isSelected) 3.dp else 1.dp
    val borderColor = if (isSelected) colors.primary else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
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
            // ── Live Preview thumbnail ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                val layers = remember(work.layersJson) { repository.deserializeLayers(work.layersJson) }
                if (layers.isEmpty()) {
                    Text(
                        work.name.take(1).uppercase(),
                        color = Color.Black.copy(alpha = 0.2f),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    CanvasPreview(
                        layers = layers,
                        shape = work.canvasShape,
                        borderColor = Color.Transparent,
                        modifier = Modifier.fillMaxSize()
                    )
                }

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

            // ── Info ──
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        work.name,
                        color = colors.textMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                                    text = { Text("Rename / Edit Info", fontSize = 13.sp, color = colors.textMain) },
                                    leadingIcon = { Icon(Lucide.Pencil, null, tint = colors.textMain, modifier = Modifier.size(15.dp)) },
                                    onClick = { showMenu = false; showRenameDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate", fontSize = 13.sp, color = colors.textMain) },
                                    leadingIcon = { Icon(Lucide.Copy, null, tint = colors.textMain, modifier = Modifier.size(15.dp)) },
                                    onClick = { showMenu = false; onDuplicate() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Move to Trash", fontSize = 13.sp, color = Color(0xFFEF4444)) },
                                    leadingIcon = { Icon(Lucide.Trash2, null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp)) },
                                    onClick = { showMenu = false; onSoftDelete() }
                                )
                            }
                        }
                    }
                }

                if (work.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        work.description,
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                val dateFormat = remember { java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault()) }
                val dateString = remember(work.lastModifiedAt) { dateFormat.format(java.util.Date(work.lastModifiedAt)) }
                
                Spacer(Modifier.height(4.dp))
                Text(
                    dateString,
                    color = colors.textMuted.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            initialName = work.name,
            initialDescription = work.description,
            onConfirm = { name, desc -> onRename(name, desc); showRenameDialog = false },
            onDismiss = { showRenameDialog = false }
        )
    }
}

// Keep the existing RenameDialog, EmptyState, and ChromaAnimatedBackground
@Composable
fun RenameDialog(
    initialName: String,
    initialDescription: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalChromaColors.current
    var name by remember { mutableStateOf(initialName) }
    var desc by remember { mutableStateOf(initialDescription) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFE6F0FA)) // Match the Trash screen dialog
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Edit Work Info",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description (optional)", fontSize = 12.sp) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                            .clickable { onConfirm(name, desc) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save", color = Color(0xFF2563EB), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(onNewWork: () -> Unit) {
    val colors = LocalChromaColors.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(colors.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.Paintbrush, "Empty", tint = colors.primary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "No works yet",
            color = colors.textMain,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Create a new canvas to start designing.",
            color = colors.textMuted,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNewWork,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.glossyBorder(RoundedCornerShape(12.dp), colors),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Create New Work", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun ChromaAnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val t1 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(11000, easing = EaseInOutSine), RepeatMode.Reverse), label = "t1")
    val t2 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(8500, easing = EaseInOutSine), RepeatMode.Reverse), label = "t2")
    val t3 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(13000, easing = EaseInOutSine), RepeatMode.Reverse), label = "t3")
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4FF))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val b1x = w * (0.15f + 0.25f * t1)
            val b1y = h * (0.10f + 0.20f * t2)
            val b1r = minOf(w, h) * 0.55f
            drawCircle(Brush.radialGradient(listOf(Color(0x885E35CA), Color(0x00000000)), Offset(b1x, b1y), b1r), b1r, Offset(b1x, b1y))

            val b2x = w * (0.55f + 0.30f * t2)
            val b2y = h * (0.30f + 0.25f * t1)
            val b2r = minOf(w, h) * 0.50f
            drawCircle(Brush.radialGradient(listOf(Color(0x7700BCD4), Color(0x00000000)), Offset(b2x, b2y), b2r), b2r, Offset(b2x, b2y))

            val b3x = w * (0.30f + 0.20f * t3)
            val b3y = h * (0.65f + 0.20f * t2)
            val b3r = minOf(w, h) * 0.45f
            drawCircle(Brush.radialGradient(listOf(Color(0x66EC407A), Color(0x00000000)), Offset(b3x, b3y), b3r), b3r, Offset(b3x, b3y))
        }
        Box(modifier = Modifier.fillMaxSize().background(Color(0xAAFFFFFF)))
    }
}
