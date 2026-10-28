package com.chroma.studio.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * X/Twitter & Facebook inspired Remix & Quote Repost Bottom Sheet.
 * Allows quoting any Flash Video, Photo post, or artwork with custom Remixed Title & Description.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemixQuoteRepostSheet(
    post: DiscoverPost,
    onDismiss: () -> Unit,
    onRepostSuccess: (DiscoverPost) -> Unit
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var remixTitle by remember(post.id) {
        mutableStateOf(
            if (post.title.isNotBlank()) "Remix: ${post.title}" else "Remix of @${post.authorUsername}"
        )
    }
    var remixDescription by remember(post.id) { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }

    var myProfile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        try {
            myProfile = DiscoverRepository.getInstance().getUserProfile(uid)
        } catch (_: Exception) {}
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isPublishing) onDismiss() },
        sheetState = sheetState,
        containerColor = if (colors.isDark) Color(0xFF16161A) else Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Gray.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isPublishing
                ) {
                    Text(
                        text = "Cancel",
                        color = colors.textMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Lucide.HeartHandshake,
                        contentDescription = "Remix",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Remix Post",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                Button(
                    onClick = {
                        if (currentUser == null) {
                            ToastManager.showToast("Please sign in to remix posts")
                            return@Button
                        }
                        isPublishing = true
                        coroutineScope.launch {
                            try {
                                val remixed = DiscoverRepository.getInstance().repostWithRemix(
                                    originalPost = post,
                                    remixTitle = remixTitle.trim(),
                                    remixDescription = remixDescription.trim()
                                )
                                onRepostSuccess(remixed)
                            } catch (e: Exception) {
                                isPublishing = false
                                ToastManager.showToast(e.message ?: "Failed to publish remix")
                            }
                        }
                    },
                    enabled = !isPublishing && remixTitle.isNotBlank(),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF),
                        disabledContainerColor = Color(0xFF007AFF).copy(alpha = 0.4f)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Repost",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Current User Info Banner (Like Twitter / Facebook "Repasting as...")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val userAvatar = myProfile?.avatarUrl?.ifBlank { null } ?: currentUser?.photoUrl?.toString()
                if (!userAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(userAvatar).crossfade(true).build(),
                        contentDescription = "My Avatar",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(0.5.dp, colors.glassBorder, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.User,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Posting as @${myProfile?.username ?: currentUser?.displayName ?: "creator"}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain
                    )
                    Text(
                        text = "Shared to community feed & your remix portfolio",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
            }

            // Remixed Title Input
            OutlinedTextField(
                value = remixTitle,
                onValueChange = { remixTitle = it },
                label = { Text("Remix Title") },
                placeholder = { Text("Give your remix a catchy title...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedBorderColor = if (colors.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
                    focusedContainerColor = if (colors.isDark) Color(0xFF1C1C20) else Color(0xFFF9F9FB),
                    unfocusedContainerColor = if (colors.isDark) Color(0xFF1C1C20) else Color(0xFFF9F9FB),
                    focusedLabelColor = Color(0xFF007AFF)
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Lucide.Sparkles,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )

            // Remixed Description / Commentary Input
            OutlinedTextField(
                value = remixDescription,
                onValueChange = { remixDescription = it },
                label = { Text("Your Commentary (Optional)") },
                placeholder = { Text("Add your thoughts, insights, reactions or tags (#remix, #art)...") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedBorderColor = if (colors.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
                    focusedContainerColor = if (colors.isDark) Color(0xFF1C1C20) else Color(0xFFF9F9FB),
                    unfocusedContainerColor = if (colors.isDark) Color(0xFF1C1C20) else Color(0xFFF9F9FB),
                    focusedLabelColor = Color(0xFF007AFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Quoted Post Attachment (Twitter / Facebook Quote Card)
            Text(
                text = "QUOTED ORIGINAL POST",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val quoteCardShape = RoundedCornerShape(14.dp)
            val isVideo = post.postType == "video" || !post.videoUrl.isNullOrBlank()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(quoteCardShape)
                    .background(if (colors.isDark) Color(0xFF1F1F24) else Color(0xFFF5F5F8))
                    .border(
                        0.75.dp,
                        if (colors.isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f),
                        quoteCardShape
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Original Author Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!post.authorAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(post.authorAvatarUrl).crossfade(true).build(),
                                contentDescription = post.authorUsername,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = post.authorUsername.take(1).uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                            }
                        }

                        Text(
                            text = post.authorDisplayName.ifBlank { post.authorUsername },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )

                        Text(
                            text = "@${post.authorUsername}",
                            fontSize = 11.5.sp,
                            color = colors.textMuted
                        )

                        if (post.authorVerified) {
                            VerifiedBadge(size = 12.dp)
                        }
                    }

                    // Post Type Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isVideo) Color(0xFFFF2D55).copy(alpha = 0.12f)
                                else Color(0xFF007AFF).copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isVideo) Lucide.Play else Lucide.Image,
                            contentDescription = null,
                            tint = if (isVideo) Color(0xFFFF2D55) else Color(0xFF007AFF),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = if (isVideo) "Flash Video" else if (post.postType == "image") "Photo" else "Artwork",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isVideo) Color(0xFFFF2D55) else Color(0xFF007AFF)
                        )
                    }
                }

                // Original Post Title & Description
                if (post.title.isNotBlank()) {
                    Text(
                        text = post.title,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (post.description.isNotBlank()) {
                    Text(
                        text = post.description,
                        fontSize = 11.5.sp,
                        color = colors.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Media Preview Poster
                val mediaUrl = post.thumbnailUrl?.takeIf { it.isNotBlank() }
                    ?: post.imageUrl?.takeIf { it.isNotBlank() }
                    ?: post.imageUrls.firstOrNull()

                if (!mediaUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(mediaUrl).crossfade(true).build(),
                            contentDescription = "Original media",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (isVideo) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Play,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
