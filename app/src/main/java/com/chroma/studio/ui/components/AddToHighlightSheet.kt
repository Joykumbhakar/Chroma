package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToHighlightSheet(
    story: Story,
    storyViewModel: StoryViewModel,
    onDismiss: () -> Unit,
    onHighlightAdded: (String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val oscellionColors = LocalOscellionColors.current
    val brandBlue = Color(0xFF0381FE)
    val deepRoyal = Color(0xFF0072DE)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val highlights by storyViewModel.highlights.collectAsState()

    var isCreatingNew by remember { mutableStateOf(false) }
    var newHighlightTitle by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotBlank()) {
            storyViewModel.loadHighlights(currentUid)
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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCreatingNew) "New Highlight" else "Add to Highlights",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        if (isCreatingNew) {
                            isCreatingNew = false
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = !isCreatingNew,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Text(
                        text = "Save this story permanently to your profile highlights reel.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // + New Highlight Button
                        item(key = "new_highlight_btn") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(68.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isCreatingNew = true
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF23252B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.Plus,
                                        contentDescription = "New Highlight",
                                        tint = brandBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "New",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Existing Highlights
                        items(highlights, key = { it.id }) { highlight ->
                            val isAlreadyAdded = highlight.storyIds.contains(story.id)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(68.dp)
                                    .clickable {
                                        if (isAlreadyAdded) {
                                            ToastManager.showToast("Already in ${highlight.title}")
                                        } else {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isSaving = true
                                            storyViewModel.addStoryToHighlight(
                                                highlightId = highlight.id,
                                                storyId = story.id
                                            ) { success ->
                                                isSaving = false
                                                if (success) {
                                                    ToastManager.showToast("Added to ${highlight.title} ✨")
                                                    onHighlightAdded(highlight.title)
                                                    coroutineScope.launch {
                                                        delay(300)
                                                        onDismiss()
                                                    }
                                                } else {
                                                    ToastManager.showToast("Failed to add to highlight")
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.size(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val coverToDisplay = highlight.coverUrl.ifBlank { story.mediaUrl }

                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(coverToDisplay)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = highlight.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2A2D35))
                                    )

                                    if (isAlreadyAdded) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Lucide.Check,
                                                contentDescription = "Added",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = highlight.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isCreatingNew,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Preview circular cover
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF23252B)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(story.mediaUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Highlight Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newHighlightTitle,
                        onValueChange = { newHighlightTitle = it.take(24) },
                        placeholder = {
                            Text(
                                text = "Highlight Name (e.g. Highlights)",
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
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val title = newHighlightTitle.trim().ifBlank { "Highlights" }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isSaving = true
                            storyViewModel.createHighlight(
                                title = title,
                                coverUrl = story.mediaUrl,
                                storyIds = listOf(story.id)
                            ) { created ->
                                isSaving = false
                                if (created != null) {
                                    ToastManager.showToast("Created highlight '$title' ✨")
                                    onHighlightAdded(title)
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        brandBlue,
                                        deepRoyal
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Add to Highlights",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
