package com.chroma.studio.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.data.DraftPostRepository
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.CreatorPostDraft
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftsListScreen(
    draftRepo: DraftPostRepository? = null,
    workRepository: WorkRepository? = null,
    onOpenDraft: (CreatorPostDraft) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val actualDraftRepo = draftRepo ?: remember { DraftPostRepository(context) }
    val actualWorkRepo = workRepository ?: remember { WorkRepository(context) }
    val colors = LocalOscellionColors.current
    val drafts by actualDraftRepo.draftsFlow.collectAsState()
    var draftToDelete by remember { mutableStateOf<CreatorPostDraft?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(colors.bg)
                    .border(BorderStroke(1.dp, colors.glassBorder))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = "Back",
                        tint = colors.textMain,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Drafts",
                        color = colors.textMain,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (drafts.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2563EB))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${drafts.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Balance spacing
                Spacer(Modifier.width(36.dp))
            }
        },
        containerColor = colors.bg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (drafts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.FileText,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No saved drafts yet",
                            color = colors.textMain,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "When creating a Post, Flash, or Work, tap 'Save as Draft' to pause your work and resume editing anytime here.",
                            color = colors.textMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.fillMaxWidth(0.85f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(drafts, key = { it.id }) { draft ->
                        DraftCardItem(
                            draft = draft,
                            workRepository = actualWorkRepo,
                            onClick = { onOpenDraft(draft) },
                            onDelete = { draftToDelete = draft }
                        )
                    }
                }
            }

            // Confirmation Dialog for deleting draft
            draftToDelete?.let { draft ->
                AlertDialog(
                    onDismissRequest = { draftToDelete = null },
                    title = {
                        Text("Delete Draft?", color = colors.textMain, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(
                            "Are you sure you want to delete '${draft.title.ifBlank { "Untitled Draft" }}'? This cannot be undone.",
                            color = colors.textMuted
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                actualDraftRepo.deleteDraft(draft.id)
                                draftToDelete = null
                            }
                        ) {
                            Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { draftToDelete = null }) {
                            Text("Cancel", color = colors.textMuted)
                        }
                    },
                    containerColor = if (colors.isDark) Color(0xFF1E293B) else Color.White
                )
            }
        }
    }
}

@Composable
private fun DraftCardItem(
    draft: CreatorPostDraft,
    workRepository: WorkRepository,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current

    val formattedDate = remember(draft.timestamp) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(draft.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (colors.isDark) Color(0xFF1E293B) else Color.White
        ),
        border = BorderStroke(1.dp, if (colors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Preview Box
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                when (draft.mode.lowercase()) {
                    "work" -> {
                        val workId = draft.workIds.firstOrNull()
                        val work = remember(workId) {
                            if (workId != null) workRepository.loadActiveWorks().firstOrNull { it.id == workId } else null
                        }
                        if (work != null) {
                            val layers = remember(work.layersJson) { workRepository.deserializeLayers(work.layersJson) }
                            CanvasPreview(
                                layers = layers,
                                shape = work.canvasShape,
                                borderColor = Color.Transparent,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Lucide.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    "flash" -> {
                        Icon(Lucide.Video, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    else -> {
                        val firstUri = draft.imageUris.firstOrNull()
                        if (firstUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(Uri.parse(firstUri)).crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Lucide.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            // Info Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Mode and Ratio Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badgeColor = when (draft.mode.lowercase()) {
                        "flash" -> Color(0xFFEF4444)
                        "work" -> Color(0xFF10B981)
                        else -> Color(0xFF2563EB)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = draft.mode.uppercase(),
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF64748B).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = draft.aspectRatio,
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (draft.hideLikeCount) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Likes hidden", color = Color(0xFF8B5CF6), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Title
                Text(
                    text = draft.title.ifBlank { "Untitled Draft" },
                    color = colors.textMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Description excerpt
                if (draft.description.isNotBlank()) {
                    Text(
                        text = draft.description,
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }

                // Privacy badges if comments or remix are disabled
                if (draft.turnOffComments || draft.turnOffRemix) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (draft.turnOffComments) {
                            Text("• Comments off", color = Color(0xFFEF4444), fontSize = 10.sp)
                        }
                        if (draft.turnOffRemix) {
                            Text("• Remix off", color = Color(0xFFF59E0B), fontSize = 10.sp)
                        }
                    }
                }

                // Date
                Text(
                    text = formattedDate,
                    color = colors.textMuted.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }

            // Delete Action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Lucide.Trash2,
                    contentDescription = "Delete Draft",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
