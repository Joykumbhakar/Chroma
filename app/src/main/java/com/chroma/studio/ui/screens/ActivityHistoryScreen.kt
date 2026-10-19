package com.chroma.studio.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.components.AiWaveSphereIcon
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.DiscoverViewModel
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ActivityTab(val title: String) {
    LIKED("Liked"),
    SAVED("Saved"),
    SHARED("Shared")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(
    userId: String,
    discoverVm: DiscoverViewModel,
    repository: WorkRepository,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
    onNavigateToUser: (String) -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var selectedTab by remember { mutableStateOf(ActivityTab.LIKED) }
    var isGridView by remember { mutableStateOf(false) }

    val likedWorks by discoverVm.likedWorks.collectAsState()
    val savedWorks by discoverVm.savedWorks.collectAsState()
    val sharedWorks by discoverVm.sharedWorks.collectAsState()
    val isLoading by discoverVm.isActivityHistoryLoading.collectAsState()
    val likedWorkIds by discoverVm.likedWorkIds.collectAsState()
    val savedWorkIds by discoverVm.savedWorkIds.collectAsState()

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(userId, selectedTab) {
        when (selectedTab) {
            ActivityTab.LIKED -> discoverVm.loadLikedWorks(userId)
            ActivityTab.SAVED -> discoverVm.loadSavedWorks(userId)
            ActivityTab.SHARED -> discoverVm.loadSharedWorks(userId)
        }
    }

    val currentList = when (selectedTab) {
        ActivityTab.LIKED -> likedWorks
        ActivityTab.SAVED -> savedWorks
        ActivityTab.SHARED -> sharedWorks
    }

    val screenBg = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)

    Scaffold(
        topBar = {
            ChromaScreenHeader(
                title = "Oscellion",
                badgeText = "Activity",
                showBackButton = true,
                onBack = onBack,
                actions = {
                    // Grid / Feed toggle
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.glassBgHover)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Lucide.List else Lucide.LayoutGrid,
                            contentDescription = if (isGridView) "Feed View" else "Grid View",
                            tint = colors.textMain,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        },
        containerColor = screenBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Segment Tab Row (Instagram Style) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.glassBgHover.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActivityTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val tabIcon = when (tab) {
                        ActivityTab.LIKED -> Lucide.Heart
                        ActivityTab.SAVED -> Lucide.Bookmark
                        ActivityTab.SHARED -> Lucide.Share2
                    }
                    val count = when (tab) {
                        ActivityTab.LIKED -> likedWorks.size
                        ActivityTab.SAVED -> savedWorks.size
                        ActivityTab.SHARED -> sharedWorks.size
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) colors.bg else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF007AFF) else colors.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color(0xFF007AFF) else colors.textMuted,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (count > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF007AFF).copy(alpha = 0.15f) else colors.glassBorder.copy(alpha = 0.3f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = count.toString(),
                                        color = if (isSelected) Color(0xFF007AFF) else colors.textMuted,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Content Area ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (isLoading && currentList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        OscellionLoadingSpinner(color = Color(0xFF007AFF))
                    }
                } else if (currentList.isEmpty()) {
                    val emptyGraphicId = when (selectedTab) {
                        ActivityTab.LIKED -> com.chroma.studio.ui.graphics3d.Apple3DGraphicId.PHOTO_GALLERY
                        ActivityTab.SAVED -> com.chroma.studio.ui.graphics3d.Apple3DGraphicId.SMART_FOLDER
                        ActivityTab.SHARED -> com.chroma.studio.ui.graphics3d.Apple3DGraphicId.TELEMETRY_CHART
                    }
                    val emptyTitle = when (selectedTab) {
                        ActivityTab.LIKED -> "No Liked Posts"
                        ActivityTab.SAVED -> "No Saved Posts"
                        ActivityTab.SHARED -> "No Shared Posts"
                    }
                    val emptyDesc = when (selectedTab) {
                        ActivityTab.LIKED -> "When you like posts across the community, they'll appear here."
                        ActivityTab.SAVED -> "Save posts and creations to easily find them again anytime."
                        ActivityTab.SHARED -> "Creations and posts you share will be listed here."
                    }

                    var isIconOpen by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        com.chroma.studio.ui.graphics3d.Apple3DGraphicIcon(
                            id = emptyGraphicId,
                            isOpen = isIconOpen,
                            onClick = { isIconOpen = !isIconOpen },
                            modifier = Modifier.size(105.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = emptyTitle,
                            color = colors.textMain,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = emptyDesc,
                            color = colors.textMuted,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    if (isGridView) {
                        // ── 3-Column Visual Grid ──
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            items(currentList, key = { it.id }) { work ->
                                ActivityGridItem(
                                    work = work,
                                    repository = repository,
                                    onClick = {
                                        // Toggle into feed view centered on or open post
                                        isGridView = false
                                    }
                                )
                            }
                        }
                    } else {
                        // ── Feed Post List View ──
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(currentList, key = { it.id }) { work ->
                                FeedPostCard(
                                    work = work,
                                    isLiked = likedWorkIds.contains(work.id),
                                    isSaved = savedWorkIds.contains(work.id),
                                    repository = repository,
                                    onRequireLogin = onRequireLogin,
                                    onLikeClick = {
                                        if (currentUserId != null) {
                                            discoverVm.toggleLike(work.id, currentUserId)
                                        } else onRequireLogin()
                                    },
                                    onSaveClick = {
                                        if (currentUserId != null) {
                                            discoverVm.toggleSave(work.id)
                                        } else onRequireLogin()
                                    },
                                    onShareClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Check out this creation by @${work.authorUsername} on Chroma Studio: https://chromastudio.app/p/${work.id}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Post"))
                                        if (currentUserId != null) {
                                            discoverVm.recordShare(work.id, currentUserId)
                                        }
                                    },
                                    onAuthorClick = { authorId -> onNavigateToUser(authorId) },
                                    onDownloadClick = { discoverVm.trackDownload(work.id) },
                                    onRemixClick = { discoverVm.trackRemix(work.id) },
                                    onDeleteWork = { workId -> discoverVm.deleteWork(workId) },
                                    onArchiveWork = { workId -> discoverVm.archiveWork(workId) },
                                    onEditWork = { workId, title, desc -> discoverVm.updateWork(workId, title, desc) },
                                    onNavigateToOriginal = { _, _ -> }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityGridItem(
    work: DiscoverPost,
    repository: WorkRepository,
    onClick: () -> Unit
) {
    var layers by remember(work.jsonUrl) { mutableStateOf<List<GradientLayer>?>(null) }

    LaunchedEffect(work.jsonUrl) {
        if (work.postType == "work" && work.jsonUrl.isNotBlank()) {
            layers = withContext(Dispatchers.IO) {
                try {
                    repository.deserializeLayers(work.jsonUrl)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(Color(0xFF1C1C1E))
            .clickable { onClick() }
    ) {
        if (work.postType == "image" && !work.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = work.imageUrl,
                contentDescription = work.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (layers != null) {
            CanvasPreview(
                layers = layers!!,
                shape = "full",
                borderColor = Color.Transparent,
                isStatic = true,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E)))),
                contentAlignment = Alignment.Center
            ) {
                AiWaveSphereIcon(
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Likes indicator overlay
        if (work.likesCount > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Lucide.Heart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "${work.likesCount}",
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
