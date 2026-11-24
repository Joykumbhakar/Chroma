package com.chroma.studio.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.model.Story
import com.chroma.studio.model.StoryHighlight
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHighlightFromArchiveSheet(
    storyViewModel: StoryViewModel,
    onDismiss: () -> Unit,
    onHighlightCreated: (StoryHighlight) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val oscellionColors = LocalOscellionColors.current
    val brandBlue = Color(0xFF0381FE)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val archiveStories by storyViewModel.storyArchive.collectAsState()
    val isArchiveLoading by storyViewModel.isArchiveLoading.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) } // 1: Pick Stories, 2: Cover & Title
    val selectedStoryIds = remember { mutableStateListOf<String>() }
    var highlightTitle by remember { mutableStateOf("") }
    var selectedCoverUrl by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showCoverSelectorModal by remember { mutableStateOf(false) }

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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        windowInsets = WindowInsets(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep == 1) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                    ) {
                        Text("Cancel", fontSize = 15.sp)
                    }

                    Text(
                        text = "New Highlight",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = {
                            if (selectedStoryIds.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Set default cover from first media story if not set
                                val archiveMap = archiveStories.associateBy { it.id }
                                val firstMediaStory = selectedStoryIds.mapNotNull { archiveMap[it] }
                                    .firstOrNull { it.mediaUrl.isNotBlank() && it.mediaUrl != "text:story" }
                                if (selectedCoverUrl.isBlank() && firstMediaStory != null) {
                                    selectedCoverUrl = firstMediaStory.mediaUrl
                                }
                                currentStep = 2
                            }
                        },
                        enabled = selectedStoryIds.isNotEmpty(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = brandBlue,
                            disabledContentColor = Color.White.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = if (selectedStoryIds.isEmpty()) "Next" else "Next (${selectedStoryIds.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            currentStep = 1
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronLeft,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Title & Cover",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = {
                            val title = highlightTitle.trim().ifBlank { "Highlights" }
                            val cover = selectedCoverUrl.ifBlank {
                                val archiveMap = archiveStories.associateBy { it.id }
                                selectedStoryIds.mapNotNull { archiveMap[it] }
                                    .firstOrNull { it.mediaUrl.isNotBlank() && it.mediaUrl != "text:story" }
                                    ?.mediaUrl ?: ""
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isSaving = true
                            storyViewModel.createHighlight(
                                title = title,
                                coverUrl = cover,
                                storyIds = selectedStoryIds.toList()
                            ) { created ->
                                isSaving = false
                                if (created != null) {
                                    ToastManager.showToast("Highlight created ✨")
                                    onHighlightCreated(created)
                                    coroutineScope.launch {
                                        delay(250)
                                        onDismiss()
                                    }
                                } else {
                                    ToastManager.showToast("Failed to create highlight")
                                }
                            }
                        },
                        enabled = !isSaving,
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
            }

            Divider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "HighlightStepTransition"
            ) { step ->
                if (step == 1) {
                    // Step 1: Stories Archive Selection Grid
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Select past stories to include in this highlight album.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                        )

                        if (isArchiveLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = brandBlue,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.5.dp
                                )
                            }
                        } else if (archiveStories.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No past stories in your archive yet.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(archiveStories, key = { it.id }) { storyItem ->
                                    val isSelected = selectedStoryIds.contains(storyItem.id)
                                    val selectionIndex = if (isSelected) selectedStoryIds.indexOf(storyItem.id) + 1 else 0

                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(0.72f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF20232A))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (isSelected) {
                                                    selectedStoryIds.remove(storyItem.id)
                                                } else {
                                                    selectedStoryIds.add(storyItem.id)
                                                }
                                            }
                                    ) {
                                        if (storyItem.mediaUrl.isNotBlank() && storyItem.mediaUrl != "text:story") {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(storyItem.mediaUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Story Thumbnail",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF0072DE).copy(alpha = 0.25f))
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = storyItem.textContent.ifBlank { "Text Story" },
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    maxLines = 4,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }

                                        // Dark gradient overlay for visibility
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    if (isSelected) Color.Black.copy(alpha = 0.25f)
                                                    else Color.Transparent
                                                )
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
                                                Text(
                                                    text = selectionIndex.toString(),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Step 2: Cover & Title Customization
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(24.dp))

                        // Circular Cover Preview
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF20232A)),
                            contentAlignment = Alignment.Center
                        ) {
                            val activeCover = selectedCoverUrl.ifBlank {
                                archiveStories.firstOrNull { it.id == selectedStoryIds.firstOrNull() }?.mediaUrl ?: ""
                            }

                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(activeCover)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Highlight Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Edit Cover Button
                        TextButton(
                            onClick = {
                                showCoverSelectorModal = true
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = brandBlue)
                        ) {
                            Text(
                                text = "Edit Cover",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // Title Text Field
                        OutlinedTextField(
                            value = highlightTitle,
                            onValueChange = { highlightTitle = it.take(24) },
                            placeholder = {
                                Text(
                                    text = "Highlights",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 15.sp
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1E2127),
                                unfocusedContainerColor = Color(0xFF1E2127),
                                focusedBorderColor = brandBlue,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = brandBlue
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(30.dp))

                        // Stories preview row
                        Text(
                            text = "${selectedStoryIds.size} stories selected",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(Modifier.height(8.dp))

                        val selectedStoriesList = remember(selectedStoryIds.toList(), archiveStories) {
                            val map = archiveStories.associateBy { it.id }
                            selectedStoryIds.mapNotNull { map[it] }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(selectedStoriesList, key = { it.id }) { s ->
                                Box(
                                    modifier = Modifier
                                        .size(60.dp, 80.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF22252C))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(s.mediaUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        // Modal to choose cover from selected stories
                        if (showCoverSelectorModal) {
                            AlertDialog(
                                onDismissRequest = { showCoverSelectorModal = false },
                                containerColor = Color(0xFF1C1E24),
                                title = {
                                    Text(
                                        text = "Choose Cover Frame",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(selectedStoriesList, key = { it.id }) { s ->
                                            Box(
                                                modifier = Modifier
                                                    .size(70.dp, 100.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF2A2D35))
                                                    .clickable {
                                                        selectedCoverUrl = s.mediaUrl
                                                        showCoverSelectorModal = false
                                                    }
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(s.mediaUrl)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                if (selectedCoverUrl == s.mediaUrl) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .border(2.dp, brandBlue, RoundedCornerShape(8.dp))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showCoverSelectorModal = false }) {
                                        Text("Done", color = brandBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
