@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.chroma.studio.ui.screens

import com.chroma.studio.ui.components.ChromaPillButton
import com.chroma.studio.ui.components.ChromaPillVariant
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.R
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.DiscoverViewModel
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    val weeks = days / 7

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${weeks}w ago"
    }
}

object LayerCache {
    private val cache = android.util.LruCache<String, List<GradientLayer>>(100)
    fun get(url: String): List<GradientLayer>? = cache.get(url)
    fun put(url: String, layers: List<GradientLayer>) { cache.put(url, layers) }
}

@Composable
fun DiscoverScreen(
    works: List<DiscoverPost>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    isRefreshing: Boolean,
    likedWorkIds: Set<String>,
    savedWorkIds: Set<String> = emptySet(),
    repository: WorkRepository,
    discoverViewModel: DiscoverViewModel? = null,
    chatViewModel: com.chroma.studio.viewmodel.ChatViewModel,
    storyViewModel: StoryViewModel? = null,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRequireLogin: () -> Unit,
    onToggleLike: (String, String) -> Unit,
    onAuthorClick: (String, String?) -> Unit,
    onDownloadWork: (DiscoverPost) -> Unit,
    onRemixWork: (DiscoverPost) -> Unit,
    onDeleteWork: (String) -> Unit,
    onEditWork: (String, String, String) -> Unit,
    onCreatePost: () -> Unit,
    onPublishWork: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onStoryViewerChange: ((Boolean) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    var searchQuery by remember { mutableStateOf("") }
    val stories by (storyViewModel?.storyGroups ?: chatViewModel.stories).collectAsState()
    val myStories by (storyViewModel?.myStories ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val likedStoryIds by (storyViewModel?.likedStoryIds ?: kotlinx.coroutines.flow.MutableStateFlow(emptySet())).collectAsState()
    val isUploadingStory by (storyViewModel?.isUploading ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val trendingWorks by (discoverViewModel?.trendingWorks ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val isTrendingLoading by (discoverViewModel?.isTrendingLoading ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val context = LocalContext.current

    var showStoryViewer by remember { mutableStateOf(false) }
    LaunchedEffect(showStoryViewer) {
        onStoryViewerChange?.invoke(showStoryViewer)
    }
    var storyViewerInitialIndex by remember { mutableIntStateOf(0) }
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var myProfile by remember { mutableStateOf<com.chroma.studio.model.UserProfile?>(null) }
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            val repo = DiscoverRepository()
            myProfile = repo.getUserProfile(currentUserId)
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { storyViewModel?.uploadStory(context, it) }
    }

    // Unified Feed: all types of posts shown together in one place
    val filteredWorks = remember(works, searchQuery) {
        if (searchQuery.isBlank()) works
        else works.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.authorUsername.contains(searchQuery, ignoreCase = true) ||
            it.authorDisplayName.contains(searchQuery, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
        }
    }

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoading && !isLoadingMore) {
            onLoadMore()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Apple Frosted Glass Top Navigation Bar
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg.copy(alpha = 0.9f))
                    .border(0.5.dp, colors.glassBorder)
                    .padding(top = statusBarTop + 6.dp, bottom = 10.dp, start = 16.dp, end = 16.dp)
            ) {
                if (isSearchExpanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search creations, tags, creators...", color = Color(0xFF8E8E93), fontSize = 14.sp) },
                        leadingIcon = { Icon(Lucide.Search, contentDescription = "Search", tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            Icon(
                                Lucide.X,
                                contentDescription = "Close",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        isSearchExpanded = false
                                        searchQuery = ""
                                    }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.glassBgHover,
                            unfocusedContainerColor = colors.glassBgHover,
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = colors.textMain,
                            unfocusedTextColor = colors.textMain
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Title + Apple Sub-Badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Oscellion",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textMain,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = ".",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF007AFF)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x14007AFF))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Community",
                                    color = Color(0xFF007AFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Actions (Create + Search + Notifications)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Create Button with Apple Glass Menu
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colors.glassBgHover)
                                        .border(1.dp, colors.glassBorder, CircleShape)
                                        .clickable { showCreateMenu = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.Plus, contentDescription = "Create", tint = colors.textMain, modifier = Modifier.size(18.dp))
                                }

                                DropdownMenu(
                                    expanded = showCreateMenu,
                                    onDismissRequest = { showCreateMenu = false },
                                    modifier = Modifier.background(colors.bg)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("New Image Post", color = colors.textMain, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            showCreateMenu = false
                                            onCreatePost()
                                        },
                                        leadingIcon = { Icon(Lucide.Image, null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp)) }
                                    )
                                    HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp)
                                    DropdownMenuItem(
                                        text = { Text("Publish Canvas Work", color = colors.textMain, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            showCreateMenu = false
                                            onPublishWork()
                                        },
                                        leadingIcon = { Icon(Lucide.Briefcase, null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp)) }
                                    )
                                    HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp)
                                    DropdownMenuItem(
                                        text = { Text("Add Story Moment", color = colors.textMain, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            showCreateMenu = false
                                            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                        leadingIcon = { Icon(Lucide.Camera, null, tint = Color(0xFF32ADE6), modifier = Modifier.size(18.dp)) }
                                    )
                                }
                            }

                            // Search Trigger
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.glassBgHover)
                                    .border(1.dp, colors.glassBorder, CircleShape)
                                    .clickable { isSearchExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Search, contentDescription = "Search", tint = colors.textMain, modifier = Modifier.size(18.dp))
                            }

                            // Notifications / Activity
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.glassBgHover)
                                    .border(1.dp, colors.glassBorder, CircleShape)
                                    .clickable { onNavigateToNotifications() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Heart, contentDescription = "Activity", tint = colors.textMain, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                LazyColumn(modifier = Modifier.fillMaxSize(), userScrollEnabled = false) {
                    item {
                        StoriesSection(stories = emptyList(), isLoading = true)
                    }
                    items(3) {
                        SkeletonPost()
                        Spacer(modifier = Modifier.fillMaxWidth().height(8.dp).background(if (colors.isDark) Color(0xFF000000) else Color(0xFFE5E5EA)))
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Stories Section
                    item(key = "stories") {
                        StoriesSection(
                            stories = stories,
                            isLoading = false,
                            hasMyStory = myStories.isNotEmpty(),
                            myStoryAvatarUrl = myProfile?.avatarUrl ?: "",
                            myStoryGender = myProfile?.gender,
                            onMyStoryClick = {
                                mediaPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onStoryClick = { groupIndex ->
                                storyViewerInitialIndex = groupIndex
                                showStoryViewer = true
                            }
                        )
                        if (isUploadingStory) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = Color(0xFF007AFF)
                            )
                        }
                    }

                    // Trending Section (Vision Pro Spatial Glass Carousel)
                    if (isTrendingLoading || trendingWorks.isNotEmpty()) {
                        item(key = "trending_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Lucide.Flame, null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp))
                                Text("Trending Creations", color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        item(key = "trending_row") {
                            if (isTrendingLoading) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    items(4, key = { "trendsk_$it" }) {
                                        Box(
                                            modifier = Modifier
                                                .width(150.dp)
                                                .height(118.dp)
                                                .clip(androidx.compose.ui.graphics.RectangleShape)
                                                .background(colors.glassBgHover.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    items(trendingWorks, key = { "trend_${it.id}" }) { trendWork ->
                                        TrendingCard(
                                            work = trendWork,
                                            repository = repository,
                                            onClick = { onAuthorClick(trendWork.creatorId, trendWork.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Feed Posts List
                    if (filteredWorks.isEmpty()) {
                        item(key = "empty_state") {
                            AppleEmptyTabState(
                                icon = Lucide.Sparkles,
                                title = "No Posts Found",
                                subtitle = if (searchQuery.isNotBlank()) "Try searching for a different creator or keyword."
                                else "Be the first to share an inspiring creation!"
                            )
                        }
                    } else {
                        items(filteredWorks, key = { it.id }) { work ->
                            FeedPostCard(
                                work = work,
                                isLiked = likedWorkIds.contains(work.id),
                                isSaved = savedWorkIds.contains(work.id),
                                repository = repository,
                                onRequireLogin = onRequireLogin,
                                onAuthorClick = { authorId -> onAuthorClick(authorId, null) },
                                onLikeClick = {
                                    val authUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                    if (!authUserId.isNullOrBlank()) {
                                        onToggleLike(work.id, authUserId)
                                    } else {
                                        onRequireLogin()
                                    }
                                },
                                onDownloadClick = {
                                    discoverViewModel?.trackDownload(work.id)
                                    onDownloadWork(work)
                                },
                                onRemixWork = { onRemixWork(work) },
                                onDeleteWork = { onDeleteWork(work.id) },
                                onEditWork = { workId, title, desc -> onEditWork(workId, title, desc) },
                                onNavigateToOriginal = { authorId, postId -> onAuthorClick(authorId, postId) },
                                onShareClick = {
                                    discoverViewModel?.trackShare(work.id)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Check out '${work.title}' by @${work.authorUsername} on Oscellion: Oscellion://post/${work.id}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Post"))
                                },
                                onSaveClick = { discoverViewModel?.toggleSave(work.id) },
                                onUpdateVisibility = { hideLikes, hideShares, hideRemixes, hideDownloads, hideSaves ->
                                    discoverViewModel?.updateCounterVisibility(
                                        postId = work.id,
                                        hideLikesCount = hideLikes,
                                        hideSharesCount = hideShares,
                                        hideRemixesCount = hideRemixes,
                                        hideDownloadsCount = hideDownloads,
                                        hideSavesCount = hideSaves
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.fillMaxWidth().height(8.dp).background(if (colors.isDark) Color(0xFF000000) else Color(0xFFE5E5EA)))
                        }

                        // End Point Stretch Loader / All Caught Up indicator
                        if (isLoadingMore) {
                            item(key = "end_stretch_loader") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color(0xFF007AFF),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            "Loading more creations...",
                                            color = Color(0xFF8E8E93),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        } else {
                            item(key = "end_of_feed") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Lucide.Sparkles, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                                        Text(
                                            "You're all caught up",
                                            color = Color(0xFF8E8E93),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Story Viewer Overlay
        if (showStoryViewer && stories.isNotEmpty() && storyViewModel != null) {
            StoryViewerScreen(
                storyGroups = stories,
                initialGroupIndex = storyViewerInitialIndex,
                currentUserId = currentUserId,
                storyViewModel = storyViewModel,
                likedStoryIds = likedStoryIds,
                myStories = myStories,
                onClose = { showStoryViewer = false },
                onAuthorClick = { userId ->
                    onAuthorClick(userId, null)
                    showStoryViewer = false
                }
            )
        }
    }
}


@Composable
fun TrendingCard(
    work: DiscoverPost,
    repository: WorkRepository,
    onClick: () -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    var layers by remember(work.jsonUrl) { mutableStateOf<List<GradientLayer>?>(LayerCache.get(work.jsonUrl)) }

    LaunchedEffect(work.jsonUrl) {
        if (layers == null && work.jsonUrl.isNotBlank()) {
            layers = withContext(Dispatchers.IO) {
                val parsed = repository.deserializeLayers(work.jsonUrl)
                LayerCache.put(work.jsonUrl, parsed)
                parsed
            }
        }
    }

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(118.dp)
            .clickable { onClick() },
        shape = androidx.compose.ui.graphics.RectangleShape,
        colors = CardDefaults.cardColors(containerColor = colors.glassBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.glassBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (work.postType == "image" && work.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(work.imageUrl).crossfade(true).build(),
                    contentDescription = work.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (layers != null && layers!!.isNotEmpty()) {
                CanvasPreview(
                    layers = layers!!,
                    shape = "full",
                    borderColor = Color.Transparent,
                    isStatic = true,
                    timeMillisOverride = 5000L,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(ChromaIcons.HeartFilled, null, tint = Color(0xFFFF2D55), modifier = Modifier.size(11.dp))
                    Text("${work.likesCount}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    work.title.ifEmpty { "Untitled" },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FeedPostCard(
    work: DiscoverPost,
    isLiked: Boolean,
    isSaved: Boolean = false,
    repository: WorkRepository,
    onRequireLogin: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onRemixClick: () -> Unit = {},
    onRemixWork: ((DiscoverPost) -> Unit)? = null,
    onDeleteWork: (String) -> Unit,
    onArchiveWork: ((String) -> Unit)? = null,
    onEditWork: (String, String, String) -> Unit,
    onNavigateToOriginal: (String, String) -> Unit,
    onShareClick: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,
    onReportClick: ((String) -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onUpdateVisibility: ((hideLikes: Boolean, hideShares: Boolean, hideRemixes: Boolean, hideDownloads: Boolean, hideSaves: Boolean) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isOwner = currentUserId != null && currentUserId == work.creatorId
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showVisibilitySettings by remember { mutableStateOf(false) }
    var editTitle by remember(work.id) { mutableStateOf(work.title) }
    var editDescription by remember(work.id) { mutableStateOf(work.description) }

    var showBigHeart by remember { mutableStateOf(false) }
    val bigHeartScale by animateFloatAsState(
        targetValue = if (showBigHeart) 1.25f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "big_heart_scale"
    )
    val bigHeartAlpha by animateFloatAsState(
        targetValue = if (showBigHeart) 0.85f else 0f,
        animationSpec = tween(220),
        label = "big_heart_alpha"
    )

    var layers by remember(work.jsonUrl) { mutableStateOf<List<GradientLayer>?>(LayerCache.get(work.jsonUrl)) }

    LaunchedEffect(work.jsonUrl) {
        if (layers == null && work.jsonUrl.isNotBlank()) {
            layers = withContext(Dispatchers.IO) {
                val parsed = repository.deserializeLayers(work.jsonUrl)
                LayerCache.put(work.jsonUrl, parsed)
                parsed
            }
        }
    }

    var resolvedAvatarUrl by remember(work.id) { mutableStateOf(work.authorAvatarUrl) }
    var resolvedDisplayName by remember(work.id) { mutableStateOf(if (work.authorDisplayName.isNotBlank()) work.authorDisplayName else work.authorUsername) }
    var resolvedVerified by remember(work.id) { mutableStateOf(work.authorVerified) }

    LaunchedEffect(work.creatorId) {
        if (work.authorAvatarUrl.isBlank() && work.creatorId.isNotBlank()) {
            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(work.creatorId).get().await()
                resolvedAvatarUrl = doc.getString("avatarUrl") ?: ""
                resolvedDisplayName = doc.getString("displayName")?.takeIf { it.isNotBlank() } ?: doc.getString("username") ?: work.authorUsername
                resolvedVerified = doc.getBoolean("verified") ?: false
            } catch (_: Exception) {}
        }
    }

    val isRemixPost = !work.remixedFromAuthorUsername.isNullOrEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
    ) {
        // Header Row (Avatar + Name + Handle + Time + Menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAuthorClick(work.creatorId) }
            ) {
                IosAvatarView(
                    name = resolvedDisplayName,
                    avatarUrl = resolvedAvatarUrl,
                    size = 42.dp
                )

                Spacer(Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = resolvedDisplayName,
                            color = colors.textMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (resolvedVerified) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.verification_badge),
                                contentDescription = "Verified",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "@${work.authorUsername}",
                            color = Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(" • ", color = Color(0xFF8E8E93), fontSize = 12.sp)
                        Text(
                            text = getTimeAgo(work.timestamp),
                            color = Color(0xFF8E8E93),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 3-Dot Options Button
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Lucide.EllipsisVertical, contentDescription = "Options", tint = Color(0xFF8E8E93), modifier = Modifier.size(18.dp))
                }

                if (showMenu) {
                    val items = if (isOwner) {
                        listOf(
                            IosActionSheetItem("Counter Privacy", false) { showMenu = false; showVisibilitySettings = true },
                            IosActionSheetItem("Edit Post", false) { showMenu = false; showEditDialog = true },
                            IosActionSheetItem("Delete", true) { showMenu = false; showDeleteConfirm = true }
                        )
                    } else {
                        listOf(
                            IosActionSheetItem("Share", false) { showMenu = false; onShareClick?.invoke() }
                        )
                    }
                    IosActionSheet(
                        items = items,
                        onDismiss = { showMenu = false }
                    )
                }
            }
        }

        // Remixed Attribution Banner if applicable
        if (isRemixPost) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x14007AFF))
                    .clickable {
                        if (!work.remixedFromAuthorId.isNullOrBlank() && !work.remixedFromPostId.isNullOrBlank()) {
                            onNavigateToOriginal(work.remixedFromAuthorId!!, work.remixedFromPostId!!)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Lucide.HeartHandshake, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(14.dp))
                    Text(
                        text = "Remixed from @${work.remixedFromAuthorUsername}",
                        color = Color(0xFF007AFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Media Viewport - Sharp edge-to-edge (0dp corner radius) with Double-Tap Heart Animation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(androidx.compose.ui.graphics.RectangleShape)
                .background(Color.Black.copy(alpha = 0.05f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                            if (!uid.isNullOrBlank()) {
                                if (!isLiked) onLikeClick()
                            } else {
                                onRequireLogin()
                            }
                            showBigHeart = true
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (work.postType == "image" && !work.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(work.imageUrl).crossfade(true).build(),
                    contentDescription = work.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (layers != null && layers!!.isNotEmpty()) {
                CanvasPreview(
                    layers = layers!!,
                    shape = "full",
                    borderColor = Color.Transparent,
                    isStatic = true,
                    timeMillisOverride = 5000L,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Instagram-style double tap reaction
            if (bigHeartScale > 0f) {
                Image(
                    painter = painterResource(id = R.drawable.heart_like),
                    contentDescription = null,
                    modifier = Modifier
                        .size(110.dp)
                        .scale(bigHeartScale)
                        .graphicsLayer { alpha = bigHeartAlpha }
                )
                LaunchedEffect(showBigHeart) {
                    if (showBigHeart) {
                        kotlinx.coroutines.delay(650)
                        showBigHeart = false
                    }
                }
            }
        }

        // Interactive Action Bar (Like | Remix | Download | Bookmark | Share) with full counter privacy controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Like, Remix & Download Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        if (uid.isNullOrBlank()) onRequireLogin()
                        else onLikeClick()
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) ChromaIcons.HeartFilled else Lucide.Heart,
                        contentDescription = if (isLiked) "Liked" else "Like",
                        tint = if (isLiked) Color(0xFFFF2D55) else colors.textMain,
                        modifier = Modifier.size(22.dp)
                    )
                    if (!work.hideLikesCount && work.likesCount > 0) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "${work.likesCount}",
                            color = if (isLiked) Color(0xFFFF2D55) else colors.textMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                                // Remix Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (currentUserId == null) onRequireLogin()
                        else if (onRemixWork != null) onRemixWork(work)
                        else onRemixClick()
                    }
                ) {
                    Icon(
                        imageVector = Lucide.HeartHandshake,
                        contentDescription = "Remix",
                        tint = colors.textMain,
                        modifier = Modifier.size(22.dp)
                    )
                    if (!work.hideRemixesCount && work.remixCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text("", color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
// Download / Save to Studio Library Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (currentUserId == null) onRequireLogin()
                        else onDownloadClick()
                    }
                ) {
                    Icon(Lucide.Download, contentDescription = "Save to Studio", tint = colors.textMain, modifier = Modifier.size(20.dp))
                    if (!work.hideDownloadsCount && work.downloadCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${work.downloadCount}",
                            color = colors.textMain.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Right: Bookmark & Share Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save / Bookmark Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onSaveClick?.invoke() }
                ) {
                    Icon(
                        imageVector = if (isSaved) Lucide.BookmarkCheck else Lucide.Bookmark,
                        contentDescription = "Bookmark",
                        tint = if (isSaved) Color(0xFF007AFF) else colors.textMain,
                        modifier = Modifier.size(20.dp)
                    )
                    if (!work.hideSavesCount && work.saveCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${work.saveCount}",
                            color = if (isSaved) Color(0xFF007AFF) else colors.textMain.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Share Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onShareClick?.invoke() }
                ) {
                    Icon(Lucide.Share2, contentDescription = "Share", tint = colors.textMain, modifier = Modifier.size(20.dp))
                    if (!work.hideSharesCount && work.shareCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${work.shareCount}",
                            color = colors.textMain.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Caption & Tags
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 12.dp)
        ) {
            if (work.title.isNotBlank()) {
                Text(
                    text = work.title,
                    color = colors.textMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (work.description.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = work.description,
                    color = colors.textMain.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            }

            if (work.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(work.tags) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.glassBgHover.copy(alpha = 0.6f))
                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text("#$tag", color = Color(0xFF007AFF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Counter Privacy Settings Bottom Sheet / Dialog
    if (showVisibilitySettings) {
        PostCountersSettingsSheet(
            work = work,
            onDismiss = { showVisibilitySettings = false },
            onSave = { hideLikes, hideShares, hideRemixes, hideDownloads, hideSaves ->
                onUpdateVisibility?.invoke(hideLikes, hideShares, hideRemixes, hideDownloads, hideSaves)
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AppleDialog(
            title = "Delete Post?",
            message = "Are you sure you want to delete this post? This action cannot be undone.",
            primaryButtonText = "Delete",
            primaryIsDestructive = true,
            onPrimaryClick = { showDeleteConfirm = false; onDeleteWork(work.id) },
            secondaryButtonText = "Cancel",
            onSecondaryClick = { showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // Edit Dialog
    if (showEditDialog) {
        GlassDialog(onDismissRequest = { showEditDialog = false }) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text("Edit Post", color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = editDescription,
                    onValueChange = { editDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChromaPillButton(
                        onClick = { showEditDialog = false },
                        variant = ChromaPillVariant.DarkGlass,
                        height = 36.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    ChromaPillButton(
                        onClick = {
                            showEditDialog = false
                            onEditWork(work.id, editTitle, editDescription)
                        },
                        variant = ChromaPillVariant.Blue,
                        height = 36.dp,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PostCountersSettingsSheet(
    work: DiscoverPost,
    onDismiss: () -> Unit,
    onSave: (hideLikes: Boolean, hideShares: Boolean, hideRemixes: Boolean, hideDownloads: Boolean, hideSaves: Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current
    var hideLikes by remember(work.id) { mutableStateOf(work.hideLikesCount) }
    var hideShares by remember(work.id) { mutableStateOf(work.hideSharesCount) }
    var hideRemixes by remember(work.id) { mutableStateOf(work.hideRemixesCount) }
    var hideDownloads by remember(work.id) { mutableStateOf(work.hideDownloadsCount) }
    var hideSaves by remember(work.id) { mutableStateOf(work.hideSavesCount) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier.padding(vertical = 12.dp).width(36.dp).height(5.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Post Counter Privacy",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain
            )
            Spacer(Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.glassBgHover).border(0.5.dp, colors.glassBorder, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column {
                    CounterToggleRow(title = "Show Likes Count", checked = !hideLikes, onCheckedChange = { hideLikes = !it })
                    HorizontalDivider(color = colors.glassBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))
                    CounterToggleRow(title = "Show Shares Count", checked = !hideShares, onCheckedChange = { hideShares = !it })
                    HorizontalDivider(color = colors.glassBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))
                    CounterToggleRow(title = "Show Remixes Count", checked = !hideRemixes, onCheckedChange = { hideRemixes = !it })
                    HorizontalDivider(color = colors.glassBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))
                    CounterToggleRow(title = "Show Downloads Count", checked = !hideDownloads, onCheckedChange = { hideDownloads = !it })
                    HorizontalDivider(color = colors.glassBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))
                    CounterToggleRow(title = "Show Saves / Bookmarks Count", checked = !hideSaves, onCheckedChange = { hideSaves = !it })
                }
            }

            Spacer(Modifier.height(24.dp))
            ChromaPillButton(
                onClick = {
                    onSave(hideLikes, hideShares, hideRemixes, hideDownloads, hideSaves)
                    onDismiss()
                },
                variant = ChromaPillVariant.Blue,
                modifier = Modifier.fillMaxWidth(),
                height = 46.dp
            ) {
                Text("Apply Settings", color = androidx.compose.ui.graphics.Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}


@Composable
private fun CounterToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        AppleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SkeletonPost() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha)))
            Spacer(Modifier.width(10.dp))
            Column {
                Box(modifier = Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = alpha)))
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.width(80.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = alpha)))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(androidx.compose.ui.graphics.RectangleShape)
                .background(Color.Gray.copy(alpha = alpha))
        )
        Spacer(Modifier.height(14.dp))
    }
}
