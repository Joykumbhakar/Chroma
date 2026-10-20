@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.chroma.studio.ui.screens

import com.chroma.studio.ui.components.ChromaPillButton
import com.chroma.studio.ui.components.ChromaPillVariant
import com.chroma.studio.ui.components.LiquidGlassProgressBar
import com.chroma.studio.ui.components.AiWaveSphereIcon
import com.chroma.studio.ui.components.ApplePhotosGraphic
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import com.chroma.studio.ui.components.FlatDoubleTapHeartOverlay
import com.chroma.studio.ui.components.rememberDoubleTapLikeState
import com.chroma.studio.ui.components.PostMediaCarousel
import com.chroma.studio.ui.components.PostScrollDotsIndicator
import com.chroma.studio.ui.components.resolvePostMediaSlides
import com.chroma.studio.ui.components.isVideoPost
import com.chroma.studio.ui.components.extractVideoUrlFromPost
import androidx.compose.foundation.pager.rememberPagerState
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.chroma.studio.model.UserStoryGroup
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.Story
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.DiscoverViewModel
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.*
import com.composables.icons.lucide.Images
import com.composables.icons.lucide.Video
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.Palette
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.compose.ui.window.Dialog
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// Fluid Cubic Bézier transitions matching creator_feed_mobile_app_ui.html
private val FluidCubicBezier = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val PopoverBezier = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

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
    onCreateFlash: () -> Unit = onCreatePost,
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

    var hasPendingNewDrops by remember { mutableStateOf(true) }
    val refreshProgressAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var hasUnreadNotifications by remember { mutableStateOf(true) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    fun showToast(msg: String) {
        coroutineScope.launch {
            toastMessage = msg
            kotlinx.coroutines.delay(2400)
            if (toastMessage == msg) {
                toastMessage = null
            }
        }
    }

    fun triggerFeedRefresh() {
        if (isRefreshing) return
        coroutineScope.launch {
            showCreateMenu = false
            refreshProgressAnim.snapTo(0f)
            refreshProgressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
            hasPendingNewDrops = false
            onRefresh()
            listState.animateScrollToItem(0)
            kotlinx.coroutines.delay(120)
            refreshProgressAnim.snapTo(0f)
            showToast("Feed refreshed • Synced latest creations")
        }
    }

    // Community Feed: All real creations, filtered dynamically by search query
    val filteredWorks = remember(works, searchQuery) {
        if (searchQuery.isBlank()) {
            works
        } else {
            works.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.authorUsername.contains(searchQuery, ignoreCase = true) ||
                it.authorDisplayName.contains(searchQuery, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

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
        BackHandler(enabled = isSearchExpanded) {
            isSearchExpanded = false
            searchQuery = ""
        }
        BackHandler(enabled = showCreateMenu) {
            showCreateMenu = false
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Creator Feed Header (No App Logo, Only + Icon, Heart/Notification Icon, Refresh Icon, and Smooth Expanding Search Bar)
            CreatorFeedHeader(
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isPlusMenuOpen = showCreateMenu,
                hasUnreadNotifications = hasUnreadNotifications,
                isRefreshing = isRefreshing || refreshProgressAnim.value > 0f,
                hasPendingNewDrops = hasPendingNewDrops,
                refreshProgress = refreshProgressAnim.value,
                onTogglePlusMenu = { showCreateMenu = !showCreateMenu },
                onToggleNotificationDrawer = {
                    showCreateMenu = false
                    hasUnreadNotifications = false
                    onNavigateToNotifications()
                },
                onRefreshFeed = { triggerFeedRefresh() },
                onExpandSearch = {
                    showCreateMenu = false
                    isSearchExpanded = true
                },
                onCollapseSearch = {
                    isSearchExpanded = false
                    searchQuery = ""
                },
                onSearchQueryChange = { searchQuery = it },
                createMenuContent = {
                    PlusCreatePopoverMenu(
                        expanded = showCreateMenu,
                        onDismissRequest = { showCreateMenu = false },
                        onSelectOption = { optionTitle ->
                            showCreateMenu = false
                            when (optionTitle) {
                                "Post", "Create new Post" -> onCreatePost()
                                "Flash", "Create new Flash(Video)" -> onCreateFlash()
                                "Story", "Add New Story" -> {
                                    mediaPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                "Work", "Create or post work" -> onPublishWork()
                            }
                        }
                    )
                }
            )

            // Pull to Refresh / Active Refreshing Indicator
            AnimatedVisibility(
                visible = isRefreshing,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OscellionLoadingSpinner(color = Color(0xFF007AFF), modifier = Modifier.size(24.dp))
                }
            }

            if (isLoading) {
                LazyColumn(modifier = Modifier.fillMaxSize(), userScrollEnabled = false) {
                    item {
                        StoriesSection(stories = emptyList(), isLoading = true)
                    }
                    items(3) {
                        SkeletonPost()
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Stories Section (@Composable fun StoriesSection in DiscoverScreen.kt)
                    item(key = "stories") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (colors.isDark) Color(0xFF16161A) else Color.White)
                                .padding(top = 4.dp)
                        ) {

                            StoriesSection(
                                stories = stories,
                                isLoading = false,
                                hasMyStory = myStories.isNotEmpty(),
                                myStoryAvatarUrl = myProfile?.avatarUrl ?: "",
                                myStoryGender = myProfile?.gender,
                                onMyStoryClick = {
                                    if (myStories.isNotEmpty()) {
                                        storyViewerInitialIndex = -1
                                        showStoryViewer = true
                                    } else {
                                        mediaPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                },
                                onAddStoryClick = {
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
                                LiquidGlassProgressBar(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .padding(horizontal = 16.dp, vertical = 2.dp)
                                )
                            }
                            HorizontalDivider(
                                color = colors.glassBorder,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    // Feed Posts List
                    if (filteredWorks.isEmpty()) {
                        item(key = "empty_state") {
                            val photoState = rememberAppleGraphicAnimationState()
                            AppleEmptyTabState(
                                graphic = {
                                    ApplePhotosGraphic(
                                        modifier = Modifier.size(140.dp),
                                        state = photoState,
                                        enableClickToAnimate = true
                                    )
                                },
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
                                onRemixWork = {
                                    discoverViewModel?.trackRemix(work.id)
                                    onRemixWork(work)
                                },
                                onRemixClick = {
                                    discoverViewModel?.trackRemix(work.id)
                                    onRemixWork(work)
                                },
                                onDeleteWork = { onDeleteWork(work.id) },
                                onArchiveWork = { workId -> discoverViewModel?.archiveWork(workId) },
                                onEditWork = { workId, title, desc -> onEditWork(workId, title, desc) },
                                onNavigateToOriginal = { authorId, postId -> onAuthorClick(authorId, postId) },
                                onReportClick = { workId -> discoverViewModel?.reportWork(workId, "Flagged from feed options") },
                                onMentionClick = { mention ->
                                    searchQuery = mention.removePrefix("@")
                                    isSearchExpanded = true
                                },
                                onTagClick = { tag ->
                                    searchQuery = tag
                                    isSearchExpanded = true
                                },
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
                                        OscellionLoadingSpinner(
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "Loading more creations...",
                                            color = colors.textMuted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        } else {
                            item(key = "end_of_feed") {
                                EndOfFeedIndicator()
                            }
                        }
                    }
                }
            }
        }

        // Story Viewer Overlay
        if (showStoryViewer && (stories.isNotEmpty() || myStories.isNotEmpty()) && storyViewModel != null) {
            StoryViewerScreen(
                storyGroups = stories,
                initialGroupIndex = storyViewerInitialIndex,
                currentUserId = currentUserId,
                storyViewModel = storyViewModel,
                likedStoryIds = likedStoryIds,
                myStories = myStories,
                currentUserProfile = myProfile,
                onClose = { showStoryViewer = false },
                onAuthorClick = { userId ->
                    onAuthorClick(userId, null)
                    showStoryViewer = false
                }
            )
        }

        // RECTANGULAR TOAST NOTIFICATION (No capsule, no glow)
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (colors.isDark) Color(0xFF1E293B) else Color(0xFF0F172A), RectangleShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = toastMessage.orEmpty(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(16.dp)
                )
            }
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

    LiquidGlassCard(
        modifier = Modifier
            .width(160.dp)
            .height(126.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val isVideo = isVideoPost(work)

            if (isVideo) {
                val thumb = work.thumbnailUrl?.takeIf { it.isNotBlank() } ?: work.imageUrl
                if (!thumb.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(thumb).crossfade(true).build(),
                        contentDescription = work.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Icon(
                    imageVector = Lucide.Play,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center)
                )
            } else if (work.postType == "image" && work.imageUrl != null) {
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

            // Scrim Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Top-Left Format Pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                val label = when {
                    isVideo -> "🎬 Flash"
                    layers != null && layers!!.isNotEmpty() -> "✨ Vector"
                    else -> "🖼️ Art"
                }
                Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }

            // Bottom Info Column
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(ChromaIcons.HeartFilled, null, tint = Color(0xFFFF2D55), modifier = Modifier.size(11.dp))
                    Text(
                        "${work.likesCount}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    work.title.ifEmpty { "Untitled" },
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

object LucideComposeIcons {
    val MoreHorizontal: ImageVector by lazy {
        ImageVector.Builder("MoreHorizontal", 20.dp, 20.dp, 24f, 24f).apply {
            listOf(5f, 12f, 19f).forEach { cx ->
                path(fill = SolidColor(Color(0xFF334155))) {
                    moveTo(cx, 10.2f)
                    arcTo(1.8f, 1.8f, 0f, true, true, cx, 13.8f)
                    arcTo(1.8f, 1.8f, 0f, true, true, cx, 10.2f)
                    close()
                }
            }
        }.build()
    }

    val Download: ImageVector by lazy {
        ImageVector.Builder("Download", 20.dp, 20.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color(0xFF334155)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 15f)
                verticalLineTo(19f)
                arcTo(2f, 2f, 0f, false, true, 19f, 21f)
                horizontalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 3f, 19f)
                verticalLineTo(15f)
                moveTo(7f, 10f)
                lineTo(12f, 15f)
                lineTo(17f, 10f)
                moveTo(12f, 15f)
                verticalLineTo(3f)
            }
        }.build()
    }

    fun bookmark(isSaved: Boolean, savedColor: Color = Color(0xFF007AFF), outlineColor: Color = Color(0xFF334155)): ImageVector {
        val stroke = if (isSaved) savedColor else outlineColor
        val fill = if (isSaved) SolidColor(savedColor) else null
        return ImageVector.Builder(if (isSaved) "BookmarkFilled" else "BookmarkOutline", 20.dp, 20.dp, 24f, 24f).apply {
            path(
                fill = fill,
                stroke = SolidColor(stroke),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 21f)
                lineTo(12f, 16f)
                lineTo(5f, 21f)
                verticalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 7f, 3f)
                horizontalLineTo(17f)
                arcTo(2f, 2f, 0f, false, true, 19f, 5f)
                close()
            }
        }.build()
    }

    val LayoutGrid: ImageVector by lazy {
        ImageVector.Builder("LayoutGrid", 16.dp, 16.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color(0xFF334155)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 3f); horizontalLineTo(10f); verticalLineTo(10f); horizontalLineTo(3f); close()
                moveTo(14f, 3f); horizontalLineTo(21f); verticalLineTo(10f); horizontalLineTo(14f); close()
                moveTo(14f, 14f); horizontalLineTo(21f); verticalLineTo(21f); horizontalLineTo(14f); close()
                moveTo(3f, 14f); horizontalLineTo(10f); verticalLineTo(21f); horizontalLineTo(3f); close()
            }
        }.build()
    }

    val VideoClapper: ImageVector by lazy {
        ImageVector.Builder("VideoClapper", 16.dp, 16.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color(0xFF334155)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f); horizontalLineTo(20f); verticalLineTo(20f); horizontalLineTo(4f); close()
                moveTo(4f, 9f); horizontalLineTo(20f)
                moveTo(9f, 4f); lineTo(7f, 9f)
                moveTo(15f, 4f); lineTo(13f, 9f)
            }
        }.build()
    }

    val Briefcase: ImageVector by lazy {
        ImageVector.Builder("Briefcase", 16.dp, 16.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color(0xFF334155)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 7f); horizontalLineTo(21f); verticalLineTo(20f); horizontalLineTo(3f); close()
                moveTo(8f, 7f); verticalLineTo(4f); horizontalLineTo(16f); verticalLineTo(7f)
            }
        }.build()
    }

    val TrendingUp: ImageVector by lazy {
        ImageVector.Builder("TrendingUp", 14.dp, 14.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color(0xFFD97706)),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 7f)
                lineTo(13.5f, 15.5f)
                lineTo(8.5f, 10.5f)
                lineTo(2f, 17f)
                moveTo(16f, 7f)
                horizontalLineTo(22f)
                verticalLineTo(13f)
            }
        }.build()
    }

    val MessageCircle: ImageVector by lazy {
        ImageVector.Builder("MessageCircle", 16.dp, 16.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color(0xFF334155)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 11.5f)
                arcTo(8.38f, 8.38f, 0f, false, true, 12.5f, 20f)
                lineTo(3f, 21f)
                lineTo(4f, 11.5f)
                arcTo(8.5f, 8.5f, 0f, true, true, 21f, 11.5f)
                close()
            }
        }.build()
    }

    val UserPlus: ImageVector by lazy {
        ImageVector.Builder("UserPlus", 16.dp, 16.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color(0xFF334155)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 21f); verticalLineTo(19f)
                arcTo(4f, 4f, 0f, false, false, 12f, 15f)
                horizontalLineTo(5f)
                arcTo(4f, 4f, 0f, false, false, 1f, 19f)
                verticalLineTo(21f)
                moveTo(8.5f, 3f)
                arcTo(4f, 4f, 0f, true, true, 8.5f, 11f)
                arcTo(4f, 4f, 0f, true, true, 8.5f, 3f)
                moveTo(20f, 8f); verticalLineTo(14f)
                moveTo(23f, 11f); horizontalLineTo(17f)
            }
        }.build()
    }

    val RefreshCw: ImageVector by lazy {
        ImageVector.Builder("RefreshCw", 20.dp, 20.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color(0xFF334155)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 2f); verticalLineTo(8f); horizontalLineTo(15f)
                moveTo(3f, 12f)
                arcTo(9f, 9f, 0f, false, true, 18.36f, 5.64f)
                lineTo(21f, 8f)
                moveTo(3f, 22f); verticalLineTo(16f); horizontalLineTo(9f)
                moveTo(21f, 12f)
                arcTo(9f, 9f, 0f, false, true, 5.64f, 18.36f)
                lineTo(3f, 16f)
            }
        }.build()
    }
}

/**
 * End of Feed Indicator matching exact HTML:
 * SVG circle with sinusoidal wave + "You're all caught up"
 */
@Composable
fun EndOfFeedIndicator(modifier: Modifier = Modifier) {
    val colors = LocalOscellionColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val scale = size.minDimension / 24f
                val strokePx = 1.8f * scale

                drawCircle(
                    color = Color(0xFF007AFF),
                    radius = 9f * scale,
                    center = Offset(12f * scale, 12f * scale),
                    style = Stroke(width = strokePx)
                )

                val wavePath = Path().apply {
                    moveTo(4.5f * scale, 12f * scale)
                    cubicTo(7f * scale, 9f * scale, 9.5f * scale, 15f * scale, 12f * scale, 12f * scale)
                    cubicTo(14.5f * scale, 9f * scale, 17f * scale, 15f * scale, 19.5f * scale, 12f * scale)
                }
                drawPath(
                    path = wavePath,
                    color = Color(0xFF007AFF),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "You're all caught up",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textMuted
            )
        }
    }
}

data class CreatorProfileInfo(
    val id: String = "",
    val name: String,
    val username: String,
    val verified: Boolean,
    val avatarColor: Color = Color(0xFF0F172A),
    val avatarUrl: String? = null,
    val initials: String,
    val roleTitle: String,
    val bio: String,
    val followers: String,
    val following: String,
    val totalRemixes: Int,
    val isFollowing: Boolean = false
)

/**
 * Creator Feed Header matching exact specification:
 * - No app logo: ONLY + Icon, Bell Notification Icon, Expanding Search Bar
 * - Smooth spring animated expansion of search bar
 * - Left icons slide out smoothly when search bar expands
 */
@Composable
fun CreatorFeedHeader(
    isSearchExpanded: Boolean,
    searchQuery: String,
    isPlusMenuOpen: Boolean,
    hasUnreadNotifications: Boolean,
    isRefreshing: Boolean = false,
    hasPendingNewDrops: Boolean = false,
    refreshProgress: Float = 0f,
    onTogglePlusMenu: () -> Unit,
    onToggleNotificationDrawer: () -> Unit,
    onRefreshFeed: () -> Unit = {},
    onExpandSearch: () -> Unit,
    onCollapseSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    createMenuContent: @Composable () -> Unit
) {
    val colors = LocalOscellionColors.current
    val plusRotation by animateFloatAsState(
        targetValue = if (isPlusMenuOpen) 45f else 0f,
        animationSpec = tween(300, easing = FluidCubicBezier),
        label = "plusRotation"
    )

    // Smooth continuous spinning rotation when refreshing
    val infiniteTransition = rememberInfiniteTransition(label = "refreshSpin")
    val spinningRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinningRotation"
    )
    val idleRotation by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(300, easing = FluidCubicBezier),
        label = "idleRotation"
    )
    val refreshRotation = if (isRefreshing) spinningRotation else idleRotation

    // Left action icons slide-out transitions matching web reference (.header-icons-slide-out)
    val leftGroupOffset by animateDpAsState(
        targetValue = if (isSearchExpanded) (-68).dp else 0.dp,
        animationSpec = tween(380, easing = FluidCubicBezier),
        label = "leftGroupOffset"
    )
    val leftGroupAlpha by animateFloatAsState(
        targetValue = if (isSearchExpanded) 0f else 1f,
        animationSpec = tween(260, easing = LinearEasing),
        label = "leftGroupAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.glassBg)
            .statusBarsPadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val maxHeaderWidth = maxWidth

            // 1. LEFT ACTION ICONS (+ Icon, Bell/Notification Icon, and Refresh Feed Icon)
            Row(
                modifier = Modifier
                    .offset(x = leftGroupOffset)
                    .alpha(leftGroupAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1.1 + ICON (Opens Create Menu with 45° rotation)
                Box {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isPlusMenuOpen) colors.glassBgHover else Color.Transparent)
                            .clickable(enabled = !isSearchExpanded, onClick = onTogglePlusMenu),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Plus,
                            contentDescription = "Create Content",
                            tint = colors.textMain,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(plusRotation)
                        )
                    }

                    createMenuContent()
                }

                // 1.2 NOTIFICATION / BELL ICON (Navigates directly to Activity Notifications)
                Box(modifier = Modifier.size(36.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable(enabled = !isSearchExpanded, onClick = onToggleNotificationDrawer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Bell,
                            contentDescription = "Notifications",
                            tint = colors.textMain,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (hasUnreadNotifications) {
                        Box(
                            modifier = Modifier
                                .size(7.5.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-3).dp, y = 3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48))
                        )
                    }
                }

                // 1.3 REFRESH FEED ICON BUTTON (Spins & fetches latest studio works)
                Box(modifier = Modifier.size(36.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable(enabled = !isSearchExpanded, onClick = onRefreshFeed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LucideComposeIcons.RefreshCw,
                            contentDescription = "Refresh Feed",
                            tint = if (isRefreshing) colors.primary else colors.textMain,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(refreshRotation)
                        )
                    }
                    if (hasPendingNewDrops && !isRefreshing) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-3).dp, y = 3.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                        )
                    }
                }
            }

            // 2. SEARCH ICON & SMOOTH EXPANDING SEARCH BAR
            val searchBarWidth by animateDpAsState(
                targetValue = if (isSearchExpanded) maxHeaderWidth else 36.dp,
                animationSpec = tween(380, easing = FluidCubicBezier),
                label = "searchBarWidth"
            )
            val searchBgColor by animateColorAsState(
                targetValue = if (isSearchExpanded) {
                    if (colors.isDark) Color(0xFF141416) else Color(0xFFF1F5F9)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(300),
                label = "searchBgColor"
            )
            val searchBorderColor by animateColorAsState(
                targetValue = if (isSearchExpanded) colors.glassBorder else Color.Transparent,
                animationSpec = tween(300),
                label = "searchBorderColor"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(searchBarWidth)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(searchBgColor)
                    .border(0.5.dp, searchBorderColor, CircleShape)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Trigger / Leading Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (!isSearchExpanded) onExpandSearch()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Search,
                            contentDescription = "Search",
                            tint = if (isSearchExpanded) colors.primary else colors.textMain,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Expanding Input Field & Close Button
                    if (isSearchExpanded) {
                        val focusRequester = remember { FocusRequester() }
                        val keyboardController = LocalSoftwareKeyboardController.current

                        LaunchedEffect(Unit) {
                            delay(160)
                            focusRequester.requestFocus()
                        }

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.textMain,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .padding(horizontal = 4.dp),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search creations, @creators, #tags...",
                                        color = colors.textMuted.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                                innerTextField()
                            }
                        )

                        // Close (X) / Clear button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (searchQuery.isNotEmpty()) {
                                        onSearchQueryChange("")
                                    } else {
                                        keyboardController?.hide()
                                        onCollapseSearch()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = if (searchQuery.isNotEmpty()) "Clear Search" else "Close Search",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Animated Top Feed Refresh Progress Line (Matches #feedRefreshBarContainer)
        if (isRefreshing || refreshProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(if (colors.isDark) Color(0xFF1E1E24) else Color(0xFFF1F5F9))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(refreshProgress.coerceIn(0f, 1f))
                        .background(colors.primary)
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)
    }
}

/**
 * + Icon Create Popover Menu with exact 4 options and icons:
 * 1. Post (Photo gallery Lucide icon)
 * 2. Flash (Video Lucide icon)
 * 3. Story (Story circle plus Lucide icon)
 * 4. Work (Painting palette Lucide icon)
 */
@Composable
fun PlusCreatePopoverMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSelectOption: (title: String) -> Unit
) {
    val colors = LocalOscellionColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = 0.dp, y = 8.dp),
        modifier = Modifier
            .width(168.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(colors.glassBg)
            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(16.dp))
    ) {
        val menuItems = listOf(
            Pair("Post", Lucide.Images),
            Pair("Flash", Lucide.Video),
            Pair("Story", Lucide.CirclePlus),
            Pair("Work", Lucide.Palette)
        )

        menuItems.forEachIndexed { index, (title, icon) ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = colors.textMain,
                        modifier = Modifier.size(19.dp)
                    )
                },
                onClick = {
                    onSelectOption(title)
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            )
            if (index < menuItems.lastIndex) {
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.4f))
            }
        }
    }
}

/**
 * Exact Lucide `heart-handshake` vector icon:
 * Four distinct stroke segments representing the clasping hands and outer heart contour.
 */
@Composable
fun rememberLucideHeartHandshakeIcon(
    isRemixed: Boolean,
    strokeColor: Color = if (isRemixed) Color(0xFF007AFF) else LocalOscellionColors.current.textMain,
    heartFillColor: Color? = if (isRemixed) Color(0xFF007AFF).copy(alpha = 0.15f) else null
): ImageVector {
    val heartFillBrush = heartFillColor?.let { SolidColor(it) }

    return remember(isRemixed, strokeColor, heartFillColor) {
        ImageVector.Builder(
            name = if (isRemixed) "LucideHeartHandshakeFilled" else "LucideHeartHandshakeOutline",
            defaultWidth = 22.dp,
            defaultHeight = 22.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer Heart Contour Path
            path(
                fill = heartFillBrush,
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 14f)
                curveTo(20.49f, 12.54f, 22f, 10.79f, 22f, 8.5f)
                arcTo(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16.5f, 3f)
                curveTo(14.74f, 3f, 13.5f, 3.5f, 12f, 5f)
                curveTo(10.5f, 3.5f, 9.26f, 3f, 7.5f, 3f)
                arcTo(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, 8.5f)
                curveTo(2f, 10.8f, 3.5f, 12.55f, 5f, 14f)
                lineTo(12f, 21f)
                close()
            }
            // Clasping Handshake Inner Path
            path(
                fill = null,
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 5f)
                lineTo(9.04f, 7.96f)
                arcTo(2.17f, 2.17f, 0f, isMoreThanHalf = false, isPositiveArc = false, 9.04f, 11.04f)
                curveTo(9.86f, 11.86f, 11.17f, 11.89f, 12.04f, 11.11f)
                lineTo(14.11f, 9.21f)
                arcTo(2.82f, 2.82f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17.9f, 9.21f)
                lineTo(20.86f, 11.87f)
            }
            // Lower Finger Line 1: m18 15-2-2
            path(
                fill = null,
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 15f)
                lineTo(16f, 13f)
            }
            // Lower Finger Line 2: m15 18-2-2
            path(
                fill = null,
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 18f)
                lineTo(13f, 16f)
            }
        }.build()
    }
}

/**
 * Scalloped Verified Seal Badge (12-point star with crisp white checkmark)
 */
@Composable
fun ScallopedVerifiedBadge(
    modifier: Modifier = Modifier.size(15.dp),
    badgeColor: Color = Color(0xFF007AFF)
) {
    Canvas(modifier = modifier) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f

        val sealPath = Path().apply {
            moveTo(12f * scaleX, 1.8f * scaleY)
            lineTo(14.6f * scaleX, 3.7f * scaleY)
            lineTo(17.8f * scaleX, 3.5f * scaleY)
            lineTo(19.1f * scaleX, 6.4f * scaleY)
            lineTo(21.8f * scaleX, 8.2f * scaleY)
            lineTo(20.9f * scaleX, 11.3f * scaleY)
            lineTo(22.2f * scaleX, 14.2f * scaleY)
            lineTo(19.6f * scaleX, 16.2f * scaleY)
            lineTo(19.1f * scaleX, 19.4f * scaleY)
            lineTo(15.9f * scaleX, 19.8f * scaleY)
            lineTo(13.6f * scaleX, 22.1f * scaleY)
            lineTo(10.4f * scaleX, 22.1f * scaleY)
            lineTo(8.1f * scaleX, 19.8f * scaleY)
            lineTo(4.9f * scaleX, 19.4f * scaleY)
            lineTo(4.4f * scaleX, 16.2f * scaleY)
            lineTo(1.8f * scaleX, 14.2f * scaleY)
            lineTo(3.1f * scaleX, 11.3f * scaleY)
            lineTo(2.2f * scaleX, 8.2f * scaleY)
            lineTo(4.9f * scaleX, 6.4f * scaleY)
            lineTo(6.2f * scaleX, 3.5f * scaleY)
            lineTo(9.4f * scaleX, 3.7f * scaleY)
            close()
        }
        drawPath(path = sealPath, color = badgeColor)

        val checkPath = Path().apply {
            moveTo(9.0f * scaleX, 12.2f * scaleY)
            lineTo(11.1f * scaleX, 14.3f * scaleY)
            lineTo(15.4f * scaleX, 9.8f * scaleY)
        }
        drawPath(
            path = checkPath,
            color = Color.White,
            style = Stroke(
                width = 2.2f * scaleX,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

enum class DrawerRole { USER, ADMIN }

fun parseAspectRatio(ratioStr: String?): Float {
    val clean = ratioStr?.trim()?.lowercase() ?: return 4f / 3f
    return when {
        clean == "1:1" || clean == "1/1" || clean == "square" -> 1f / 1f
        clean == "4:3" || clean == "4/3" -> 4f / 3f
        clean == "5:4" || clean == "5/4" -> 5f / 4f
        clean == "4:5" || clean == "4/5" -> 4f / 5f
        clean == "3:4" || clean == "3/4" -> 3f / 4f
        clean == "16:9" || clean == "16/9" -> 16f / 9f
        clean == "9:16" || clean == "9/16" -> 9f / 16f
        clean == "21:9" || clean == "21/9" || clean == "cinematic" -> 21f / 9f
        else -> 4f / 3f
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
    onTagClick: ((String) -> Unit)? = null,
    onUpdateVisibility: ((hideLikes: Boolean, hideShares: Boolean, hideRemixes: Boolean, hideDownloads: Boolean, hideSaves: Boolean) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isOwner = !currentUserId.isNullOrBlank() && currentUserId == work.creatorId
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showEditModal by remember { mutableStateOf(false) }
    var showVisibilitySettings by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAnalyticsModal by remember { mutableStateOf(false) }
    var showCreatorProfileModal by remember { mutableStateOf(false) }
    var isFollowingCreator by remember(work.creatorId) { mutableStateOf(false) }
    var showOriginalPostModal by remember { mutableStateOf(false) }
    var activeRole by remember(isOwner) { mutableStateOf(if (isOwner) DrawerRole.ADMIN else DrawerRole.USER) }

    var editTitle by remember(work.id) { mutableStateOf(work.title) }
    var editDescription by remember(work.id) { mutableStateOf(work.description) }
    var isTitleExpanded by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    // 1. LIKE
    val doubleTapLikeState = rememberDoubleTapLikeState()
    val currentIsLiked by rememberUpdatedState(isLiked)
    val currentOnLikeClick by rememberUpdatedState(onLikeClick)
    val currentOnRequireLogin by rememberUpdatedState(onRequireLogin)

    val initialLiked = isLiked || (!currentUserId.isNullOrBlank() && work.likedBy.contains(currentUserId))
    var locallyLiked by remember(work.id, isLiked, work.likedBy) { mutableStateOf(initialLiked) }
    var localLikesCountDelta by remember(work.id) { mutableIntStateOf(0) }
    LaunchedEffect(isLiked, work.likedBy) {
        locallyLiked = isLiked || (!currentUserId.isNullOrBlank() && work.likedBy.contains(currentUserId))
        localLikesCountDelta = 0
    }

    // 2. REMIX
    var locallyRemixed by remember(work.id) {
        mutableStateOf(currentUserId != null && work.remixedBy.contains(currentUserId))
    }
    var localRemixCountDelta by remember(work.id) { mutableIntStateOf(0) }
    LaunchedEffect(work.remixedBy) {
        if (currentUserId != null && work.remixedBy.contains(currentUserId)) {
            locallyRemixed = true
            localRemixCountDelta = 0
        }
    }

    // 3. DOWNLOAD
    var locallyDownloaded by remember(work.id) {
        mutableStateOf(currentUserId != null && work.downloadedBy.contains(currentUserId))
    }
    var localDownloadCountDelta by remember(work.id) { mutableIntStateOf(0) }
    LaunchedEffect(work.downloadedBy) {
        if (currentUserId != null && work.downloadedBy.contains(currentUserId)) {
            locallyDownloaded = true
            localDownloadCountDelta = 0
        }
    }

    // 4. SAVE
    var locallySaved by remember(work.id) { mutableStateOf(isSaved) }
    var localSaveCountDelta by remember(work.id) { mutableIntStateOf(0) }
    LaunchedEffect(isSaved) {
        locallySaved = isSaved
        localSaveCountDelta = 0
    }

    // 5. SHARE
    var locallyShared by remember(work.id) {
        mutableStateOf(currentUserId != null && work.sharedBy.contains(currentUserId))
    }
    var localShareCountDelta by remember(work.id) { mutableIntStateOf(0) }
    LaunchedEffect(work.sharedBy) {
        if (currentUserId != null && work.sharedBy.contains(currentUserId)) {
            locallyShared = true
            localShareCountDelta = 0
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val mediaSlides = remember(work.id, work.imageUrls, work.workUrls, work.imageUrl, work.jsonUrl) {
        resolvePostMediaSlides(work)
    }
    val pagerState = rememberPagerState {
        mediaSlides.size.coerceAtLeast(1)
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
            .background(if (colors.isDark) Color(0xFF16161A) else Color.White)
            .padding(top = 4.dp)
    ) {
        // 1. POST HEADER: Small Profile Avatar, Name + Scalloped Verified Badge + Small Remixed Tag, @username, Time + 3-Dot Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { showCreatorProfileModal = true }
            ) {
                IosAvatarView(
                    name = resolvedDisplayName,
                    avatarUrl = resolvedAvatarUrl,
                    size = 38.dp
                )

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = resolvedDisplayName,
                            color = colors.textMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (resolvedVerified) {
                            ScallopedVerifiedBadge()
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "@${work.authorUsername}",
                            color = Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = getTimeAgo(work.timestamp),
                    color = Color(0xFF8E8E93),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = LucideComposeIcons.MoreHorizontal,
                        contentDescription = "Options",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. MEDIA VIEWPORT: Strictly WITHOUT corner radius (RectangleShape), 1:1, 4:3, 5:4, 16:9, 9:16
        val parsedRatio = parseAspectRatio(work.aspectRatio)
        val isVerticalVideo = work.aspectRatio.trim() == "9:16"

        if (isVerticalVideo) {
            // For 9:16 posts we must produce a deterministic pixel height so the Column
            // places the action-buttons row correctly below the media.
            // Conflicting .heightIn(max=540.dp).aspectRatio() causes Compose to report an
            // incorrect intrinsic height to the parent Column → action buttons overlap.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidthDp = maxWidth
                // Height = width × (16/9), capped at 540dp so tall phones don't fill the screen
                val clampedHeightDp = minOf(cardWidthDp * 16f / 9f, 540.dp)
                PostMediaCarousel(
                    work = work,
                    slides = mediaSlides,
                    pagerState = pagerState,
                    repository = repository,
                    colors = colors,
                    doubleTapLikeState = doubleTapLikeState,
                    onDoubleTap = { offset ->
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid.isNullOrBlank()) {
                            currentOnRequireLogin()
                        } else {
                            if (!currentIsLiked && !locallyLiked) {
                                locallyLiked = true
                                localLikesCountDelta = 1
                                currentOnLikeClick()
                            }
                        }
                        doubleTapLikeState.trigger(offset)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(clampedHeightDp)
                )
            }
        } else {
            PostMediaCarousel(
                work = work,
                slides = mediaSlides,
                pagerState = pagerState,
                repository = repository,
                colors = colors,
                doubleTapLikeState = doubleTapLikeState,
                onDoubleTap = { offset ->
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid.isNullOrBlank()) {
                        currentOnRequireLogin()
                    } else {
                        // Double tap ONLY likes. It never unlikes on subsequent double-taps!
                        if (!currentIsLiked && !locallyLiked) {
                            locallyLiked = true
                            localLikesCountDelta = 1
                            currentOnLikeClick()
                        }
                    }
                    doubleTapLikeState.trigger(offset)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(parsedRatio)
            )
        }

        // 3. ACTION BUTTONS ROW:
        // Left: Heart (Like), Lucide HeartHandshake (Remix), Download
        // Center: Animated Scroll Dots Indicator (for multi-image / multi-work posts)
        // Right: Save (Bookmark), Share
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Left Action Group: Like, Remix, Download
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Like Button
                val effectiveLiked = locallyLiked
                val displayLikesCount = (work.likesCount + localLikesCountDelta).coerceAtLeast(0)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid.isNullOrBlank()) {
                                onRequireLogin()
                            } else {
                                val nextLiked = !locallyLiked
                                locallyLiked = nextLiked
                                localLikesCountDelta = if (nextLiked) {
                                    if (!isLiked) 1 else 0
                                } else {
                                    if (isLiked) -1 else 0
                                }
                                onLikeClick()
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (effectiveLiked) ChromaIcons.HeartFilled else Lucide.Heart,
                        contentDescription = if (effectiveLiked) "Liked" else "Like",
                        tint = if (effectiveLiked) Color(0xFFFF2D55) else colors.textMain,
                        modifier = Modifier.size(22.dp)
                    )
                    if (!work.hideLikesCount && displayLikesCount > 0) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "$displayLikesCount",
                            color = if (effectiveLiked) Color(0xFFFF2D55) else colors.textMain,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 2. Remix Button (HeartShape Handshake)
                if (!work.turnOffRemix) {
                    val alreadyRemixed = currentUserId != null && work.remixedBy.contains(currentUserId)
                    val effectiveRemixed = locallyRemixed || alreadyRemixed
                    val displayRemixCount = (work.remixCount + localRemixCountDelta).coerceAtLeast(0)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val uid = FirebaseAuth.getInstance().currentUser?.uid
                                if (uid.isNullOrBlank()) {
                                    onRequireLogin()
                                } else {
                                    if (!locallyRemixed && (currentUserId == null || !work.remixedBy.contains(currentUserId))) {
                                        locallyRemixed = true
                                        localRemixCountDelta = 1
                                    }
                                    if (onRemixWork != null) onRemixWork(work)
                                    else onRemixClick()
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = rememberLucideHeartHandshakeIcon(
                                isRemixed = effectiveRemixed || displayRemixCount > 0,
                                strokeColor = if (effectiveRemixed) Color(0xFF007AFF) else colors.textMain,
                                heartFillColor = if (effectiveRemixed) Color(0xFF007AFF).copy(alpha = 0.15f) else null
                            ),
                            contentDescription = "Remix",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                        if (!work.hideRemixesCount && displayRemixCount > 0) {
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "$displayRemixCount",
                                color = if (effectiveRemixed) Color(0xFF007AFF) else colors.textMain.copy(alpha = 0.85f),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 3. Download Button
                val alreadyDownloaded = currentUserId != null && work.downloadedBy.contains(currentUserId)
                val effectiveDownloaded = locallyDownloaded || alreadyDownloaded
                val displayDownloadCount = (work.downloadCount + localDownloadCountDelta).coerceAtLeast(0)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid.isNullOrBlank()) {
                                onRequireLogin()
                            } else {
                                if (!locallyDownloaded && (currentUserId == null || !work.downloadedBy.contains(currentUserId))) {
                                    locallyDownloaded = true
                                    localDownloadCountDelta = 1
                                }
                                onDownloadClick()
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = LucideComposeIcons.Download,
                        contentDescription = "Save to Studio",
                        tint = if (effectiveDownloaded) Color(0xFF007AFF) else colors.textMain,
                        modifier = Modifier.size(22.dp)
                    )
                    if (!work.hideDownloadsCount && displayDownloadCount > 0) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "$displayDownloadCount",
                            color = if (effectiveDownloaded) Color(0xFF007AFF) else colors.textMain.copy(alpha = 0.85f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Centered Scroll Dots Indicator for Multi-Image / Multi-Work Posts
            if (mediaSlides.size > 1) {
                PostScrollDotsIndicator(
                    totalDots = mediaSlides.size,
                    selectedIndex = pagerState.currentPage,
                    isDark = colors.isDark,
                    onDotClick = { targetIndex ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetIndex)
                        }
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Right Action Group: Save & Share
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 4. Save / Bookmark Button
                val effectiveSaved = locallySaved
                val displaySaveCount = (work.saveCount + localSaveCountDelta).coerceAtLeast(0)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid.isNullOrBlank()) {
                                onRequireLogin()
                            } else {
                                val nextSaved = !locallySaved
                                locallySaved = nextSaved
                                localSaveCountDelta = if (nextSaved) {
                                    if (!isSaved) 1 else 0
                                } else {
                                    if (isSaved) -1 else 0
                                }
                                onSaveClick?.invoke()
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = LucideComposeIcons.bookmark(
                            isSaved = effectiveSaved,
                            savedColor = Color(0xFF007AFF),
                            outlineColor = colors.textMain
                        ),
                        contentDescription = "Bookmark",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                    if (!work.hideSavesCount && displaySaveCount > 0) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "$displaySaveCount",
                            color = if (effectiveSaved) Color(0xFF007AFF) else colors.textMain.copy(alpha = 0.85f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 5. Share Button
                val alreadyShared = currentUserId != null && work.sharedBy.contains(currentUserId)
                val effectiveShared = locallyShared || alreadyShared
                val displayShareCount = (work.shareCount + localShareCountDelta).coerceAtLeast(0)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid.isNullOrBlank()) {
                                onRequireLogin()
                            } else {
                                if (!locallyShared && (currentUserId == null || !work.sharedBy.contains(currentUserId))) {
                                    locallyShared = true
                                    localShareCountDelta = 1
                                }
                                onShareClick?.invoke()
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Share2,
                        contentDescription = "Share",
                        tint = if (effectiveShared) Color(0xFF007AFF) else colors.textMain,
                        modifier = Modifier.size(22.dp)
                    )
                    if (!work.hideSharesCount && displayShareCount > 0) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "$displayShareCount",
                            color = if (effectiveShared) Color(0xFF007AFF) else colors.textMain.copy(alpha = 0.85f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 4. METADATA: Expandable Title (Max 2 lines + ....), Expandable Description (Max 1 line + ...read more), #Hashtags, @Mentions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Main Title (Max 2 lines then .... with smooth expand animation)
            if (work.title.isNotBlank()) {
                val needsTitleExpand = work.title.length > 85
                val shortTitle = if (needsTitleExpand) work.title.take(85).trim() else work.title

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(380, easing = FluidCubicBezier))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (needsTitleExpand) isTitleExpanded = !isTitleExpanded
                        }
                ) {
                    Text(
                        text = buildAnnotatedString {
                            if (!needsTitleExpand || isTitleExpanded) {
                                append(work.title)
                            } else {
                                append(shortTitle)
                                withStyle(
                                    SpanStyle(
                                        color = Color(0xFF007AFF),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                ) {
                                    append(" ....")
                                }
                            }
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        lineHeight = 18.5.sp,
                        maxLines = if (isTitleExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Description (Max 1 line then ...read more with smooth expand animation)
            if (work.description.isNotBlank()) {
                val needsDescExpand = work.description.length > 55
                val shortDesc = if (needsDescExpand) work.description.take(55).trim() else work.description

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(380, easing = FluidCubicBezier))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (needsDescExpand) isDescriptionExpanded = !isDescriptionExpanded
                        }
                ) {
                    Text(
                        text = buildAnnotatedString {
                            if (!needsDescExpand || isDescriptionExpanded) {
                                append(work.description)
                                if (needsDescExpand) {
                                    withStyle(
                                        SpanStyle(
                                            color = if (colors.isDark) Color(0xFF71717A) else Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    ) {
                                        append(" (show less)")
                                    }
                                }
                            } else {
                                append(shortDesc)
                                withStyle(
                                    SpanStyle(
                                        color = if (colors.isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) {
                                    append(" ...read more")
                                }
                            }
                        },
                        fontSize = 12.sp,
                        color = colors.textMain.copy(alpha = 0.82f),
                        lineHeight = 16.5.sp,
                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tags, Mentions, Trending (Clean reference inline text style without capsules or pill borders)
            val trendingLabel = work.trendingTag ?: if (work.tags.any { it.contains("trend", ignoreCase = true) }) "Trending" else null
            val hasTagsOrMentions = work.tags.isNotEmpty() || work.mentions.isNotEmpty() || trendingLabel != null
            if (hasTagsOrMentions) {
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (trendingLabel != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val target = work.tags.firstOrNull() ?: trendingLabel
                                onTagClick?.invoke(target)
                            }
                        ) {
                            Icon(
                                imageVector = LucideComposeIcons.TrendingUp,
                                contentDescription = null,
                                tint = if (colors.isDark) Color(0xFFF59E0B) else Color(0xFFB45309),
                                modifier = Modifier.size(13.5.dp)
                            )
                            Spacer(Modifier.width(3.5.dp))
                            Text(
                                text = trendingLabel,
                                color = if (colors.isDark) Color(0xFFF59E0B) else Color(0xFFB45309),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    work.tags.forEach { rawTag ->
                        val cleanTag = if (rawTag.startsWith("#")) rawTag else "#$rawTag"
                        Text(
                            text = cleanTag,
                            color = Color(0xFF007AFF),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onTagClick?.invoke(cleanTag)
                            }
                        )
                    }

                    work.mentions.forEach { rawMention ->
                        val handle = if (rawMention.startsWith("@")) rawMention else "@$rawMention"
                        Text(
                            text = handle,
                            color = if (colors.isDark) Color(0xFFA1A1AA) else Color(0xFF334155),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onMentionClick?.invoke(handle)
                            }
                        )
                    }
                }
            }
        }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = if (colors.isDark) Color(0xFF24242A) else Color(0xFFE5E5EA)
            )
            Spacer(Modifier.height(4.dp))
        }

    // 3-Dot Options Bottom Sheet (Opens from bottom like bottom drawer, with Normal User & Admin modes)
    if (showMenu) {
        PostOptionsDrawerSheet(
            work = work,
            isOwner = isOwner,
            activeRole = if (isOwner) activeRole else DrawerRole.USER,
            onRoleChange = { newRole -> if (isOwner) activeRole = newRole },
            onDismiss = { showMenu = false },
            onInterested = { showMenu = false },
            onNotInterested = { showMenu = false },
            onViewProfile = {
                showMenu = false
                showCreatorProfileModal = true
            },
            onAboutPost = {
                showMenu = false
                showAboutDialog = true
            },
            onReport = {
                showMenu = false
                onReportClick?.invoke(work.id)
            },
            onBlockUser = {
                showMenu = false
                showBlockConfirm = true
            },
            onCounterPrivacy = {
                showMenu = false
                if (isOwner) showVisibilitySettings = true
            },
            onEditPost = {
                showMenu = false
                if (isOwner) showEditModal = true
            },
            onViewAnalytics = {
                showMenu = false
                if (isOwner) showAnalyticsModal = true
            },
            onArchive = {
                showMenu = false
                if (isOwner) onArchiveWork?.invoke(work.id)
            },
            onDeletePost = {
                showMenu = false
                if (isOwner) showDeleteConfirm = true
            }
        )
    }

    // Original Post Inspector Modal/Drawer (Opens when Remixed tag or attribution is clicked)
    if (showOriginalPostModal) {
        OriginalPostInspectorSheet(
            work = work,
            onDismiss = { showOriginalPostModal = false },
            onNavigateToOriginal = onNavigateToOriginal,
            onRemixWork = onRemixWork
        )
    }

    // Counter Privacy Settings Bottom Sheet
    if (showVisibilitySettings) {
        PostCountersSettingsSheet(
            work = work,
            onDismiss = { showVisibilitySettings = false },
            onSave = { hideLikes, hideShares, hideRemixes, hideDownloads, hideSaves ->
                onUpdateVisibility?.invoke(hideLikes, hideShares, hideRemixes, hideDownloads, hideSaves)
            }
        )
    }

    // About This Post Dialog
    if (showAboutDialog) {
        AppleDialog(
            title = "About this Post",
            message = "Author: $resolvedDisplayName (@${work.authorUsername})\n" +
                      "Status: ${if (resolvedVerified) "Verified Creator ✓" else "Standard Account"}\n" +
                      "Format: ${work.postType.replaceFirstChar { it.uppercase() }} (${work.aspectRatio})\n" +
                      "Published: ${getTimeAgo(work.timestamp)}\n" +
                      "Post ID: ${work.id}",
            primaryButtonText = "Done",
            onPrimaryClick = { showAboutDialog = false },
            onDismiss = { showAboutDialog = false }
        )
    }

    // Owner Analytics & Telemetry Modal (2x2 Telemetry Grid)
    if (showAnalyticsModal) {
        OwnerAnalyticsModal(
            work = work,
            onDismiss = { showAnalyticsModal = false }
        )
    }

    // Live Edit Post Modal (Admin Role)
    if (showEditModal) {
        EditPostModal(
            work = work,
            onDismiss = { showEditModal = false },
            onSaveEdits = { ratio, title, desc ->
                showEditModal = false
                if (isOwner) {
                    onEditWork(work.id, title, desc)
                }
            }
        )
    }

    // Creator Studio Profile Modal
    if (showCreatorProfileModal) {
        val totalCreatorRemixes = maxOf(work.remixCount, 1)
        val profileInfo = CreatorProfileInfo(
            id = work.creatorId,
            name = resolvedDisplayName,
            username = work.authorUsername,
            verified = resolvedVerified,
            avatarUrl = resolvedAvatarUrl.takeIf { it.isNotBlank() },
            initials = (if (resolvedDisplayName.isNotBlank()) resolvedDisplayName else work.authorUsername).take(2).uppercase(),
            roleTitle = if (work.postType.equals("video", ignoreCase = true) || work.aspectRatio == "9:16") "Motion & Shader Director" else "Multidisciplinary Visual Creator",
            bio = "Building open-source creative artifacts, zero-radius visual studies, and remixable node graphs on ChromaStudio.",
            followers = "${(work.likesCount * 3 + 1200) / 1000}.${((work.likesCount * 3) % 1000) / 100}K",
            following = "342",
            totalRemixes = totalCreatorRemixes,
            isFollowing = isFollowingCreator
        )
        CreatorProfileModal(
            profile = profileInfo,
            creatorPosts = listOf(work),
            onToggleFollow = {
                isFollowingCreator = !isFollowingCreator
            },
            onViewFullProfile = {
                onAuthorClick(work.creatorId)
            },
            onDismiss = { showCreatorProfileModal = false }
        )
    }

    // Block User Confirmation Dialog
    if (showBlockConfirm) {
        AppleDialog(
            title = "Block @${work.authorUsername}?",
            message = "They won't be able to message you or interact with your creations. You can unblock them at any time in Settings.",
            primaryButtonText = "Block User",
            primaryIsDestructive = true,
            onPrimaryClick = { showBlockConfirm = false },
            secondaryButtonText = "Cancel",
            onSecondaryClick = { showBlockConfirm = false },
            onDismiss = { showBlockConfirm = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AppleDialog(
            title = "Delete Post?",
            message = "Are you sure you want to delete this post? This action cannot be undone.",
            primaryButtonText = "Delete",
            primaryIsDestructive = true,
            onPrimaryClick = {
                showDeleteConfirm = false
                if (isOwner) {
                    onDeleteWork(work.id)
                }
            },
            secondaryButtonText = "Cancel",
            onSecondaryClick = { showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

/**
 * Owner Analytics & Telemetry Modal
 * Matches creator_feed_android_ui_jetpack_compose (1).kt and creator_feed_mobile_app_ui.html:
 * 2x2 Telemetry Grid (Total Reach, Remix Forks, Likes & Reactions, Asset Downloads)
 */
@Composable
fun OwnerAnalyticsModal(
    work: DiscoverPost,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val totalReach = remember(work.id) {
        work.likesCount * 3 + work.downloadCount * 4 + work.shareCount * 6 + work.remixCount * 8 + 380
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (colors.isDark) Color(0xFF1C1C1E) else Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, colors.glassBorder, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OWNER TELEMETRY & ANALYTICS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${work.title.ifBlank { "Artwork" }} (${work.aspectRatio})",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Close Analytics",
                            tint = colors.textMain,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // 2x2 Telemetry Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (colors.isDark) Color(0xFF252528) else Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Text("TOTAL REACH", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(numberFormat.format(totalReach), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                        Text("+18.4% vs avg", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (colors.isDark) Color(0xFF1E3A8A).copy(alpha = 0.25f) else Color(0xFFEFF6FF), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Text("REMIX FORKS", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(numberFormat.format(work.remixCount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                        Text("High lineage velocity", fontSize = 10.sp, color = Color(0xFF8E8E93))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (colors.isDark) Color(0xFF252528) else Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Text("LIKES & REACTIONS", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(numberFormat.format(work.likesCount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                        Text("Double-tap rate 64%", fontSize = 10.sp, color = Color(0xFF8E8E93))
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (colors.isDark) Color(0xFF252528) else Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Text("ASSET DOWNLOADS", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(numberFormat.format(work.downloadCount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                        Text("4K source & stems", fontSize = 10.sp, color = Color(0xFF8E8E93))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color.White else Color(0xFF0F172A))
                        .clickable { onDismiss() }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Close Telemetry",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (colors.isDark) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

/**
 * Live Post & Frame Ratio Editor Modal (Admin Role)
 * Allows selecting from 5 permitted aspect ratios (1:1, 4:3, 5:4, 16:9, 9:16), Title, and Description
 */
@Composable
fun EditPostModal(
    work: DiscoverPost,
    onDismiss: () -> Unit,
    onSaveEdits: (ratio: String, title: String, desc: String) -> Unit
) {
    val colors = LocalOscellionColors.current
    var selectedRatio by remember { mutableStateOf(work.aspectRatio.ifBlank { "4:3" }) }
    var titleText by remember { mutableStateOf(work.title) }
    var descText by remember { mutableStateOf(work.description) }
    val ratios = listOf("1:1", "4:3", "5:4", "16:9", "9:16")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (colors.isDark) Color(0xFF1C1C1E) else Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, colors.glassBorder, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Edit Post & Frame Ratio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Aspect Ratio", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ratios.forEach { ratio ->
                        val isSelected = selectedRatio == ratio
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF007AFF) else (if (colors.isDark) Color(0xFF252528) else Color(0xFFF8FAFC)))
                                .border(1.dp, if (isSelected) Color(0xFF007AFF) else colors.glassBorder, CircleShape)
                                .clickable { selectedRatio = ratio }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ratio,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else colors.textMain
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Title", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = colors.textMain),
                    cursorBrush = SolidColor(Color(0xFF007AFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (colors.isDark) Color(0xFF252528) else Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("Description", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = colors.textMain),
                    cursorBrush = SolidColor(Color(0xFF007AFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (colors.isDark) Color(0xFF252528) else Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF1F5F9))
                            .clickable { onDismiss() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF))
                            .clickable { onSaveEdits(selectedRatio, titleText, descText) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save Changes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Creator Profile Sheet Modal
 * Displays avatar, verified badge, role title, bio, stats (Followers, Following, Remixes),
 * published studio works preview, and Follow/Following toggle button.
 */
@Composable
fun CreatorProfileModal(
    profile: CreatorProfileInfo,
    creatorPosts: List<DiscoverPost>,
    onToggleFollow: () -> Unit,
    onViewFullProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (colors.isDark) Color(0xFF1C1C1E) else Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, colors.glassBorder, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!profile.avatarUrl.isNullOrBlank()) {
                            IosAvatarView(
                                name = profile.name,
                                avatarUrl = profile.avatarUrl,
                                size = 48.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(profile.avatarColor)
                                    .border(1.5.dp, colors.glassBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(profile.initials, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(profile.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                if (profile.verified) ScallopedVerifiedBadge()
                            }
                            Text("@${profile.username.removePrefix("@")}", fontSize = 11.5.sp, color = Color(0xFF8E8E93))
                            Text(profile.roleTitle, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF007AFF))
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF1F5F9))
                    ) {
                        Icon(Lucide.X, contentDescription = "Close Profile", tint = colors.textMain, modifier = Modifier.size(15.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(profile.bio, fontSize = 11.5.sp, color = colors.textMain.copy(alpha = 0.8f), lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (colors.isDark) Color(0xFF252528) else Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(profile.followers, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                        Text("Followers", fontSize = 10.sp, color = Color(0xFF8E8E93))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(profile.following, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                        Text("Following", fontSize = 10.sp, color = Color(0xFF8E8E93))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${profile.totalRemixes}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                        Text("Remixes", fontSize = 10.sp, color = Color(0xFF8E8E93))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("PUBLISHED STUDIO WORKS (${creatorPosts.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E93))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val previewItems = creatorPosts.take(3)
                    if (previewItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (colors.isDark) Color(0xFF252528) else Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No public works yet", fontSize = 11.sp, color = Color(0xFF8E8E93))
                        }
                    } else {
                        previewItems.forEach { workItem ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                                    .padding(6.dp)
                            ) {
                                if (!workItem.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = workItem.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = workItem.aspectRatio.ifBlank { "4:3" },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (profile.isFollowing) colors.glassBgHover else colors.primary)
                            .border(0.5.dp, if (profile.isFollowing) colors.glassBorder else Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { onToggleFollow() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (profile.isFollowing) "Following" else "Follow",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (profile.isFollowing) colors.textMain else Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(colors.glassBgHover)
                            .border(0.5.dp, colors.glassBorder, CircleShape)
                            .clickable {
                                onDismiss()
                                onViewFullProfile()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "View Full Profile",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMain
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsDrawerSheet(
    work: DiscoverPost,
    isOwner: Boolean,
    activeRole: DrawerRole,
    onRoleChange: (DrawerRole) -> Unit,
    onDismiss: () -> Unit,
    onInterested: () -> Unit,
    onNotInterested: () -> Unit,
    onViewProfile: () -> Unit,
    onAboutPost: () -> Unit,
    onReport: () -> Unit,
    onBlockUser: () -> Unit,
    onCounterPrivacy: () -> Unit,
    onEditPost: () -> Unit,
    onViewAnalytics: () -> Unit,
    onArchive: () -> Unit,
    onDeletePost: () -> Unit
) {
    val colors = LocalOscellionColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg.copy(alpha = 0.98f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            val showAdminTools = isOwner && activeRole == DrawerRole.ADMIN

            // Post Context & Role Switcher Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOwner) "Manage Post" else if (work.authorDisplayName.isNotBlank()) "${work.authorDisplayName} (@${work.authorUsername})" else "@${work.authorUsername}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isOwner) "Original Post Creator • Admin Tools" else "Post options",
                        fontSize = 11.5.sp,
                        color = if (isOwner) Color(0xFF007AFF) else Color(0xFF8E8E93),
                        fontWeight = if (isOwner) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                // Segmented Role Switcher Pill: ONLY visible and accessible to the original post creator/admin
                if (isOwner) {
                    Spacer(Modifier.width(10.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassBgHover)
                            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val isUser = activeRole == DrawerRole.USER
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (!isUser) Color(0xFF007AFF) else Color.Transparent)
                                .clickable { onRoleChange(DrawerRole.ADMIN) }
                                .padding(horizontal = 11.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Admin",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (!isUser) Color.White else colors.textMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isUser) colors.glassBg else Color.Transparent)
                                .clickable { onRoleChange(DrawerRole.USER) }
                                .padding(horizontal = 11.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "User View",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isUser) colors.textMain else colors.textMuted
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)

            if (!showAdminTools) {
                // NORMAL USER OPTIONS:
                DrawerActionItem(
                    icon = Lucide.Heart,
                    title = "Interested",
                    subtitle = "Show more creations like this in your feed",
                    onClick = onInterested
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))

                DrawerActionItem(
                    icon = Lucide.X,
                    title = "Not Interested",
                    subtitle = "Show fewer creations like this in your feed",
                    onClick = onNotInterested
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))

                DrawerActionItem(
                    icon = Lucide.User,
                    title = "View Profile",
                    subtitle = "See creator works, Flash videos & bio",
                    onClick = onViewProfile
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))

                DrawerActionItem(
                    icon = Lucide.Info,
                    title = "About this post",
                    subtitle = "Creation date, format ratio & verification details",
                    onClick = onAboutPost
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))

                DrawerActionItem(
                    icon = Lucide.Flag,
                    title = "Report",
                    subtitle = "Flag this post for review",
                    tint = Color(0xFFFF9500),
                    onClick = onReport
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))

                DrawerActionItem(
                    icon = Lucide.Ban,
                    title = "Block this user",
                    subtitle = "Restrict this profile from interacting with you",
                    tint = Color(0xFFFF3B30),
                    onClick = onBlockUser
                )
            } else {
                // ADMIN OPTIONS:
                DrawerActionItem(
                    icon = Lucide.Lock,
                    title = "Counter Privacy",
                    subtitle = "Control visibility of Like, Remix & Download counts",
                    onClick = onCounterPrivacy
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))

                DrawerActionItem(
                    icon = Lucide.Pencil,
                    title = "Edit Post",
                    subtitle = "Update title, description, or tags",
                    onClick = onEditPost
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))

                DrawerActionItem(
                    icon = Lucide.Flame,
                    title = "View analytics",
                    subtitle = "Reach, remixes, downloads & saves breakdown",
                    onClick = onViewAnalytics
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))

                DrawerActionItem(
                    icon = Lucide.Info,
                    title = "About this post",
                    subtitle = "Creation date, format ratio & verification details",
                    onClick = onAboutPost
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))

                DrawerActionItem(
                    icon = Lucide.Archive,
                    title = "Archive",
                    subtitle = "Hide post from public feed while keeping stats",
                    onClick = onArchive
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))

                DrawerActionItem(
                    icon = Lucide.Trash2,
                    title = "Delete Post",
                    subtitle = "Permanently remove this post",
                    tint = Color(0xFFFF3B30),
                    onClick = onDeletePost
                )
            }

            Spacer(Modifier.height(14.dp))

            // Cancel Button
            ChromaPillButton(
                onClick = onDismiss,
                variant = ChromaPillVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
                height = 44.dp
            ) {
                Text("Cancel", color = colors.textMain, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun DrawerActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = LocalOscellionColors.current.textMain,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val effectiveTint = if (tint != colors.textMain) tint else colors.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = effectiveTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (tint != colors.textMain) tint else colors.textMain
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.textMuted
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OriginalPostInspectorSheet(
    work: DiscoverPost,
    onDismiss: () -> Unit,
    onNavigateToOriginal: (String, String) -> Unit,
    onRemixWork: ((DiscoverPost) -> Unit)?
) {
    val colors = LocalOscellionColors.current
    val origAuthorId = work.remixedFromAuthorId ?: ""
    val origPostId = work.remixedFromPostId ?: ""
    val origUsername = work.remixedFromAuthorUsername ?: "creator"
    val origDisplayName = work.remixedFromAuthorDisplayName?.takeIf { it.isNotBlank() } ?: origUsername
    val origAvatarUrl = work.remixedFromAuthorAvatar ?: ""
    val origVerified = work.remixedFromAuthorVerified

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg.copy(alpha = 0.98f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Lineage Banner Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = rememberLucideHeartHandshakeIcon(
                                isRemixed = true,
                                strokeColor = Color(0xFF007AFF),
                                heartFillColor = Color(0xFF007AFF).copy(alpha = 0.2f)
                            ),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            "REMIX LINEAGE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF),
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            "Original Master Artwork",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Lucide.X, contentDescription = "Close", tint = Color(0xFF8E8E93), modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)

            Spacer(Modifier.height(14.dp))

            // Original Creator Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.glassBgHover)
                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                    .clickable {
                        onDismiss()
                        if (origAuthorId.isNotBlank()) {
                            onNavigateToOriginal(origAuthorId, origPostId)
                        }
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosAvatarView(
                    name = origDisplayName,
                    avatarUrl = origAvatarUrl,
                    size = 44.dp
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = origDisplayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (origVerified) {
                            ScallopedVerifiedBadge()
                        }
                    }
                    Text(
                        text = "@$origUsername",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF007AFF)
                    )
                }

                Icon(
                    Lucide.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Provenance context description
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF007AFF).copy(alpha = 0.08f))
                    .border(0.5.dp, Color(0xFF007AFF).copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Seed Composition & Vectors",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF007AFF)
                    )
                    Text(
                        text = "This creation was remixed by @${work.authorUsername} using the procedural layer assets and shader parameters from @$origUsername's master artwork.",
                        fontSize = 12.sp,
                        color = colors.textMain.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Action Buttons
            ChromaPillButton(
                onClick = {
                    onDismiss()
                    if (origAuthorId.isNotBlank()) {
                        onNavigateToOriginal(origAuthorId, origPostId)
                    }
                },
                variant = ChromaPillVariant.Blue,
                modifier = Modifier.fillMaxWidth(),
                height = 44.dp
            ) {
                Text("View Original Master Work", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }

            Spacer(Modifier.height(8.dp))

            ChromaPillButton(
                onClick = {
                    onDismiss()
                    onRemixWork?.invoke(work)
                },
                variant = ChromaPillVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
                height = 42.dp
            ) {
                Text("Create New Remix", color = colors.textMain, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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

    LiquidGlassBottomSheet(
        onDismissRequest = onDismiss
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
            Text(
                "Control visibility of public engagement metrics",
                fontSize = 12.sp,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )
            
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    CounterToggleRow(
                        title = "Show Likes Count",
                        subtitle = "Others will only see \"Liked by...\"",
                        checked = !hideLikes,
                        onCheckedChange = { hideLikes = !it }
                    )
                    HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                    CounterToggleRow(
                        title = "Show Shares Count",
                        subtitle = "Keeps outbound share counts private",
                        checked = !hideShares,
                        onCheckedChange = { hideShares = !it }
                    )
                    HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                    CounterToggleRow(
                        title = "Show Remixes Count",
                        subtitle = "Disables public fork count display",
                        checked = !hideRemixes,
                        onCheckedChange = { hideRemixes = !it }
                    )
                    HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                    CounterToggleRow(
                        title = "Show Downloads Count",
                        subtitle = "Keeps download metric private",
                        checked = !hideDownloads,
                        onCheckedChange = { hideDownloads = !it }
                    )
                    HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                    CounterToggleRow(
                        title = "Show Saves / Bookmarks Count",
                        subtitle = "Only you can see save metrics",
                        checked = !hideSaves,
                        onCheckedChange = { hideSaves = !it }
                    )
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
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = colors.textMuted, fontSize = 11.5.sp, lineHeight = 14.sp)
            }
        }
        AppleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            activeTrackColor = Color(0xFF0A84FF),
            inactiveTrackColor = if (colors.isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)
        )
    }
}

@Composable
fun SkeletonPost() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    val colors = LocalOscellionColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha)))
            Spacer(Modifier.width(10.dp))
            Column {
                Box(modifier = Modifier.width(130.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = alpha)))
                Spacer(Modifier.height(5.dp))
                Box(modifier = Modifier.width(85.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray.copy(alpha = alpha)))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RectangleShape)
                .background(Color.Gray.copy(alpha = alpha))
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha)))
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha)))
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha)))
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 0.5.dp,
            color = if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
        )
    }
}
