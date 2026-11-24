package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.model.Story
import com.chroma.studio.model.StoryHighlight
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHighlightSheet(
    highlight: StoryHighlight,
    storyViewModel: StoryViewModel,
    onDismiss: () -> Unit,
    onHighlightUpdated: () -> Unit = {},
    onHighlightDeleted: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val oscellionColors = LocalOscellionColors.current
    val brandBlue = Color(0xFF0381FE)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val archiveStories by storyViewModel.storyArchive.collectAsState()

    var highlightTitle by remember { mutableStateOf(highlight.title) }
    var selectedCoverUrl by remember { mutableStateOf(highlight.coverUrl) }
    val activeStoryIds = remember { mutableStateListOf<String>().apply { addAll(highlight.storyIds) } }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Selected, 1: Add from Archive
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotBlank()) {
            storyViewModel.loadStoryArchive(currentUid)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141416),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                ) {
                    Text("Cancel", fontSize = 15.sp)
                }

                Text(
                    text = "Edit Highlight",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = {
                        val title = highlightTitle.trim().ifBlank { "Highlights" }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isSaving = true
                        storyViewModel.updateHighlight(
                            highlightId = highlight.id,
                            title = title,
                            coverUrl = selectedCoverUrl,
                            storyIds = activeStoryIds.toList()
                        ) { success ->
                            isSaving = false
                            if (success) {
                                ToastManager.showToast("Highlight updated ✨")
                                onHighlightUpdated()
                                coroutineScope.launch {
                                    delay(200)
                                    onDismiss()
                                }
                            } else {
                                ToastManager.showToast("Failed to update highlight")
                            }
                        }
                    },
                    enabled = !isSaving && activeStoryIds.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = brandBlue,
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (isSaving) "Saving..." else "Done",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Cover + Title editor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22252C)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(selectedCoverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Highlight Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(Modifier.width(16.dp))

                OutlinedTextField(
                    value = highlightTitle,
                    onValueChange = { highlightTitle = it.take(24) },
                    placeholder = {
                        Text(
                            text = "Highlight Name",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1F2228),
                        unfocusedContainerColor = Color(0xFF1F2228),
                        focusedBorderColor = brandBlue,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = brandBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // Segmented Tabs: "Selected" vs "Stories" (Archive)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = brandBlue,
                        height = 2.5.dp
                    )
                },
                divider = {
                    Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 0
                    },
                    text = {
                        Text(
                            text = "Selected (${activeStoryIds.size})",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.6f)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 1
                    },
                    text = {
                        Text(
                            text = "Stories / Archive",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.6f)
                        )
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Tab Content
            val storiesForGrid = if (selectedTab == 0) {
                // Show stories that are currently selected in highlight
                val archiveMap = archiveStories.associateBy { it.id }
                activeStoryIds.mapNotNull { archiveMap[it] }
            } else {
                // Show all archive stories
                archiveStories
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (storiesForGrid.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedTab == 0) "No stories in this highlight." else "No archive stories.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(storiesForGrid, key = { it.id }) { storyItem ->
                            val isSelected = activeStoryIds.contains(storyItem.id)

                            Box(
                                modifier = Modifier
                                    .aspectRatio(0.72f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF20232A))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isSelected) {
                                            activeStoryIds.remove(storyItem.id)
                                        } else {
                                            activeStoryIds.add(storyItem.id)
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(storyItem.mediaUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Selection Badge (Top Right)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) brandBlue
                                            else Color.Black.copy(alpha = 0.45f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Lucide.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Destructive Delete Highlight Option
            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteConfirmDialog = true
                    }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Lucide.Trash2,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF453A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Delete Highlight",
                    color = Color(0xFFFF453A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Delete Confirmation Alert
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    containerColor = Color(0xFF1E2127),
                    title = {
                        Text(
                            text = "Delete Highlight?",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "This will remove the highlight from your profile. All stories will still remain saved in your archive.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirmDialog = false
                                storyViewModel.deleteHighlight(highlight.id) { success ->
                                    if (success) {
                                        ToastManager.showToast("Highlight deleted")
                                        onHighlightDeleted()
                                        onDismiss()
                                    } else {
                                        ToastManager.showToast("Failed to delete highlight")
                                    }
                                }
                            }
                        ) {
                            Text("Delete", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                )
            }
        }
    }
}
