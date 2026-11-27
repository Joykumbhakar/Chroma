package com.chroma.studio.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.data.SavedAccountManager
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.DiscoverViewModel
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Dedicated full-screen viewer for shared post links (`https://oscellion.vercel.app/post/{id}`,
 * `/work/{id}`, `oscellion://post/{id}`, or `oscellion://work/{id}`).
 * Works reliably for both signed-in users and guests, image/work/video posts, and cold starts.
 */
@Composable
fun PostDetailDeepLinkScreen(
    postId: String,
    repository: WorkRepository,
    discoverViewModel: DiscoverViewModel,
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onDownloadWork: (DiscoverPost) -> Unit,
    onRemixWork: (DiscoverPost) -> Unit,
    onNavigateToOriginal: (String, String) -> Unit,
    onOpenFlashReel: (String) -> Unit,
    onRequireLogin: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var post by remember(postId) { mutableStateOf<DiscoverPost?>(null) }
    var isLoading by remember(postId) { mutableStateOf(true) }
    var reloadNonce by remember(postId) { mutableIntStateOf(0) }

    val likedWorkIds by discoverViewModel.likedWorkIds.collectAsState()
    val savedWorkIds by discoverViewModel.savedWorkIds.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        ?: SavedAccountManager.getActiveAccount(context)?.uid
    val isGuest = SavedAccountManager.isGuest(context) || currentUserId.isNullOrBlank()

    LaunchedEffect(postId, reloadNonce) {
        isLoading = true
        val fetched = discoverViewModel.repository.fetchPostById(postId)
        post = fetched
        isLoading = false
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Compact Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(top = statusBarTop + 8.dp, bottom = 10.dp, start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = "Back",
                        tint = colors.textMain,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post?.title?.takeIf { it.isNotBlank() } ?: "Shared Post",
                        color = colors.textMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (post != null) {
                        val currentPost = post!!
                        val acceptedCollabs = remember(currentPost.collaborators, currentPost.collaboratorIds) {
                            com.chroma.studio.model.resolveAcceptedCollaborators(
                                currentPost.collaborators,
                                currentPost.collaboratorIds
                            )
                        }
                        val collabAttr = remember(currentPost.authorUsername, acceptedCollabs) {
                            com.chroma.studio.model.formatCollabHeaderAttribution(
                                "@${currentPost.authorUsername}",
                                acceptedCollabs.map { "@${it.username.ifBlank { it.displayName }}" }
                            )
                        }
                        Text(
                            text = if (collabAttr.connector != null && collabAttr.secondaryLabel != null) {
                                "${collabAttr.primaryLabel}${collabAttr.connector}${collabAttr.secondaryLabel}"
                            } else {
                                "@${currentPost.authorUsername}"
                            },
                            color = colors.textMuted,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (post != null && (post!!.postType == "video" || !post!!.videoUrl.isNullOrBlank())) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF0381FE))
                        .clickable { onOpenFlashReel(post!!.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Play,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Watch Flash",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)

        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val activePost = post
        var isRespondingCollab by remember(activePost?.id) { mutableStateOf(false) }
        if (activePost != null && !currentUid.isNullOrBlank() && activePost.pendingCollaboratorIds.contains(currentUid)) {
            com.chroma.studio.ui.components.PendingCollabReviewStrip(
                creatorHandle = activePost.authorUsername,
                isProcessing = isRespondingCollab,
                onAccept = {
                    isRespondingCollab = true
                    scope.launch {
                        val ok = discoverViewModel.repository.respondToCollabInvite(activePost.id, accept = true)
                        if (ok) {
                            post = discoverViewModel.repository.fetchPostById(activePost.id)
                            ToastManager.showToast("Joined collaboration!")
                        }
                        isRespondingCollab = false
                    }
                },
                onDecline = {
                    isRespondingCollab = true
                    scope.launch {
                        val ok = discoverViewModel.repository.respondToCollabInvite(activePost.id, accept = false)
                        if (ok) {
                            post = discoverViewModel.repository.fetchPostById(activePost.id)
                            ToastManager.showToast("Invite declined")
                        }
                        isRespondingCollab = false
                    }
                }
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OscellionLoadingSpinner()
                        Text(
                            text = "Opening shared post...",
                            color = colors.textMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            post == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.FileQuestion,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Post Unavailable",
                            color = colors.textMain,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "This post may have been archived, removed, or the link is incomplete.",
                            color = colors.textMuted,
                            fontSize = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { reloadNonce++ },
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text("Retry")
                            }
                            OutlinedButton(
                                onClick = onBack,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text("Back to Feed")
                            }
                        }
                    }
                }
            }

            else -> {
                val activeWork = post!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
                ) {
                    item(key = activeWork.id) {
                        FeedPostCard(
                            work = activeWork,
                            isLiked = likedWorkIds.contains(activeWork.id),
                            isSaved = savedWorkIds.contains(activeWork.id),
                            isInViewport = true,
                            repository = repository,
                            onShowToast = { msg -> ToastManager.showToast(msg) },
                            onRequireLogin = onRequireLogin,
                            onLikeClick = {
                                if (!isGuest && currentUserId != null) {
                                    val willBeLiked = !likedWorkIds.contains(activeWork.id)
                                    discoverViewModel.toggleLike(activeWork.id, currentUserId)
                                    val newLikedBy = if (willBeLiked) {
                                        (activeWork.likedBy + currentUserId).distinct()
                                    } else {
                                        activeWork.likedBy - currentUserId
                                    }
                                    val baseLikes = maxOf(activeWork.likesCount, activeWork.likedBy.size)
                                    val nextCount = when {
                                        willBeLiked && !activeWork.likedBy.contains(currentUserId) ->
                                            maxOf(baseLikes + 1, newLikedBy.size)
                                        !willBeLiked && activeWork.likedBy.contains(currentUserId) ->
                                            maxOf(0, baseLikes - 1)
                                        else -> maxOf(baseLikes, newLikedBy.size)
                                    }
                                    post = activeWork.copy(likesCount = nextCount, likedBy = newLikedBy)
                                } else {
                                    onRequireLogin()
                                }
                            },
                            onForceLike = {
                                if (!isGuest && currentUserId != null) {
                                    discoverViewModel.toggleLike(activeWork.id, currentUserId, forceLike = true)
                                    if (!activeWork.likedBy.contains(currentUserId)) {
                                        val newLikedBy = (activeWork.likedBy + currentUserId).distinct()
                                        val baseLikes = maxOf(activeWork.likesCount, activeWork.likedBy.size)
                                        post = activeWork.copy(
                                            likesCount = maxOf(baseLikes + 1, newLikedBy.size),
                                            likedBy = newLikedBy
                                        )
                                    }
                                } else {
                                    onRequireLogin()
                                }
                            },
                            onAuthorClick = { uid -> onAuthorClick(uid) },
                            onDownloadClick = {
                                if (!isGuest && currentUserId != null) {
                                    discoverViewModel.trackDownload(activeWork.id)
                                    onDownloadWork(activeWork)
                                } else {
                                    onRequireLogin()
                                }
                            },
                            onRemixWork = { w ->
                                if (!isGuest && currentUserId != null) {
                                    discoverViewModel.trackRemix(w.id)
                                    onRemixWork(w)
                                } else {
                                    onRequireLogin()
                                }
                            },
                            onRemixClick = {
                                if (!isGuest && currentUserId != null) {
                                    discoverViewModel.trackRemix(activeWork.id)
                                    onRemixWork(activeWork)
                                } else {
                                    onRequireLogin()
                                }
                            },
                            onSaveClick = {
                                if (!isGuest && currentUserId != null) {
                                    discoverViewModel.toggleSave(activeWork.id)
                                } else {
                                    onRequireLogin()
                                }
                            },
                            onShareClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Check out '${activeWork.title}' by @${activeWork.authorUsername} on Oscellion: https://oscellion.vercel.app/post/${activeWork.id}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Post"))
                                if (!isGuest && currentUserId != null) {
                                    scope.launch { discoverViewModel.trackShare(activeWork.id) }
                                }
                            },
                            onArchiveWork = { workId ->
                                discoverViewModel.archiveWork(workId)
                                onBack()
                            },
                            onDeleteWork = { workId ->
                                discoverViewModel.deleteWork(workId)
                                onBack()
                            },
                            onEditWork = { workId, title, desc ->
                                discoverViewModel.updateWork(workId, title, desc)
                                post = activeWork.copy(title = title, description = desc)
                            },
                            onNavigateToOriginal = onNavigateToOriginal
                        )
                    }
                }
            }
        }
    }
}
