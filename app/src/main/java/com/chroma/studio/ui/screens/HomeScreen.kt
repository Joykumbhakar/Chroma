package com.chroma.studio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.ChromaWork
import com.chroma.studio.ui.components.glossyBorder
import com.chroma.studio.ui.theme.LocalChromaColors
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// ──────────────────────────────────────────────────────
//  Animated Background
// ──────────────────────────────────────────────────────

@Composable
fun ChromaAnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    // Three independently drifting blobs
    val t1 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(11000, easing = EaseInOutSine), RepeatMode.Reverse), label = "t1")
    val t2 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(8500, easing = EaseInOutSine), RepeatMode.Reverse), label = "t2")
    val t3 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(13000, easing = EaseInOutSine), RepeatMode.Reverse), label = "t3")
    val hueShift by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart), label = "hue")
    val scale by infiniteTransition.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(7000, easing = EaseInOutSine), RepeatMode.Reverse), label = "scale")

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4FF))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAnimatedBlobs(t1, t2, t3, hueShift, scale)
        }
        // Frosted glass overlay to soften the blobs
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAAFFFFFF))
        )
    }
}

private fun DrawScope.drawAnimatedBlobs(t1: Float, t2: Float, t3: Float, hueShift: Float, scale: Float) {
    val w = size.width
    val h = size.height

    // Blob 1 — purple/indigo, top-left drift
    val b1x = w * (0.15f + 0.25f * t1)
    val b1y = h * (0.10f + 0.20f * t2)
    val b1r = minOf(w, h) * 0.55f * scale
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x885E35CA), Color(0x00000000)),
            center = Offset(b1x, b1y),
            radius = b1r
        ),
        radius = b1r,
        center = Offset(b1x, b1y)
    )

    // Blob 2 — cyan/teal, center-right
    val b2x = w * (0.55f + 0.30f * t2)
    val b2y = h * (0.30f + 0.25f * t1)
    val b2r = minOf(w, h) * 0.50f * scale
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x7700BCD4), Color(0x00000000)),
            center = Offset(b2x, b2y),
            radius = b2r
        ),
        radius = b2r,
        center = Offset(b2x, b2y)
    )

    // Blob 3 — warm rose/pink, bottom
    val b3x = w * (0.30f + 0.20f * t3)
    val b3y = h * (0.65f + 0.20f * t2)
    val b3r = minOf(w, h) * 0.45f * scale
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x66EC407A), Color(0x00000000)),
            center = Offset(b3x, b3y),
            radius = b3r
        ),
        radius = b3r,
        center = Offset(b3x, b3y)
    )
}

// ──────────────────────────────────────────────────────
//  Home Screen
// ──────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    works: List<ChromaWork>,
    onNewWork: () -> Unit,
    onOpenWork: (ChromaWork) -> Unit,
    onDeleteWork: (String) -> Unit,
    onRenameWork: (String, String, String) -> Unit   // id, name, desc
) {
    val colors = LocalChromaColors.current

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Animated gradient background ──
        ChromaAnimatedBackground()

        // ── Content ──
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            // ── Top bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Chroma Studio",
                        color = Color(0xFF1A1A2E),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        "${works.size} ${if (works.size == 1) "work" else "works"}",
                        color = Color(0xFF6B7280),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // ── New Work button ──
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .glossyBorder(RoundedCornerShape(14.dp), colors)
                        .background(Color(0xFF5E35CA))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onNewWork() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Lucide.Plus, contentDescription = "New", tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("New Work", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Work grid / empty state ──
            if (works.isEmpty()) {
                EmptyState(onNewWork = onNewWork)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(works, key = { it.id }) { work ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + scaleIn(initialScale = 0.92f)
                        ) {
                            WorkCard(
                                work = work,
                                onOpen = { onOpenWork(work) },
                                onDelete = { onDeleteWork(work.id) },
                                onRename = { name, desc -> onRenameWork(work.id, name, desc) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────
//  Work Card
// ──────────────────────────────────────────────────────

@Composable
fun WorkCard(
    work: ChromaWork,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String, String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0x22000000), RoundedCornerShape(18.dp))
            .background(Color(0xEEFFFFFF))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onOpen() }
    ) {
        Column {
            // ── Preview thumbnail placeholder ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF5E35CA), Color(0xFF00BCD4))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    work.name.take(1).uppercase(),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black
                )
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
                        color = Color(0xFF1A1A2E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        Icon(
                            Lucide.Menu,
                            contentDescription = "Options",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showMenu = true }
                        )
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename / Edit Info", fontSize = 13.sp) },
                                leadingIcon = { Icon(Lucide.Pencil, null, modifier = Modifier.size(15.dp)) },
                                onClick = { showMenu = false; showRenameDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", fontSize = 13.sp, color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Lucide.Trash2, null, tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp)) },
                                onClick = { showMenu = false; showDeleteDialog = true }
                            )
                        }
                    }
                }

                if (work.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        work.description,
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Dates ──
                val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

                WorkDateRow("Created", work.createdAt, dateFmt, timeFmt)
                Spacer(Modifier.height(2.dp))
                WorkDateRow("Modified", work.lastModifiedAt, dateFmt, timeFmt)
            }
        }
    }

    // ── Rename Dialog ──
    if (showRenameDialog) {
        RenameDialog(
            initialName = work.name,
            initialDescription = work.description,
            onConfirm = { name, desc -> onRename(name, desc); showRenameDialog = false },
            onDismiss = { showRenameDialog = false }
        )
    }

    // ── Delete Confirm Dialog ──
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Work", fontWeight = FontWeight.Bold) },
            text = { Text("Delete \"${work.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ──────────────────────────────────────────────────────
//  Rename Dialog
// ──────────────────────────────────────────────────────

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Work Info", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.glassBorder
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)", fontSize = 12.sp) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.glassBorder
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.ifBlank { "Untitled" }, desc) }) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ──────────────────────────────────────────────────────
//  Empty state
// ──────────────────────────────────────────────────────

@Composable
fun EmptyState(onNewWork: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0x185E35CA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.Plus, contentDescription = null, tint = Color(0xFF5E35CA), modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "No works yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Create your first gradient masterpiece\nand it will appear here.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF5E35CA))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onNewWork() }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Lucide.Plus, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text("Create New Work", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

// ──────────────────────────────────────────────────────
//  Date row helper
// ──────────────────────────────────────────────────────

@Composable
private fun WorkDateRow(
    label: String,
    timestamp: Long,
    dateFmt: SimpleDateFormat,
    timeFmt: SimpleDateFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF9CA3AF), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Text(
            "${dateFmt.format(Date(timestamp))} · ${timeFmt.format(Date(timestamp))}",
            color = Color(0xFF9CA3AF),
            fontSize = 10.sp
        )
    }
}
