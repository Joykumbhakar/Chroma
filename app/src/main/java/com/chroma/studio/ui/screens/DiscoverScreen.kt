@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.chroma.studio.ui.screens

import com.chroma.studio.ui.theme.InterFontFamily
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
import com.chroma.studio.ui.components.VerifiedBadge
import com.chroma.studio.ui.components.ShareStoryAudienceSheet
import com.chroma.studio.ui.components.OnlyFriendsManagementSheet
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.ui.graphics.asImageBitmap
import com.chroma.studio.data.FlashUploadManager
import com.chroma.studio.data.FlashUploadTask
import com.chroma.studio.data.UploadStatus
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
    val diff = (now - timestamp).coerceAtLeast(0L)

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

/**
 * Clean Cupertino Category Tabs for Community Feed.
 * Adheres strictly to the Chroma brand identity (Text + Icon + Animated Hr Line indicator; No Pills, No Capsules).
 */
enum class CommunityFeedTab(val label: String, val icon: ImageVector) {
    ALL("Latest", Lucide.Clock),
    FOLLOWING("Following", Lucide.Users),
    SAVED("Saved", Lucide.Bookmark),
    FLASH("Flash Videos", Lucide.Film),
    TRENDING("Trending", Lucide.Flame)
}

enum class CommunitySortOrder(val label: String, val icon: ImageVector) {
    LATEST("Latest Drops", Lucide.Clock),
    TOP_LIKED("Most Liked", Lucide.Heart),
    MOST_REMIXED("Most Remixed", Lucide.Sparkles)
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
    onStoryViewerChange: ((Boolean) -> Unit)? = null,
    onNavigateToCreateStory: (() -> Unit)? = null,
    onNavigateToDrafts: () -> Unit = {},
    onFlashVideoClick: ((String) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val uploadTask by FlashUploadManager.currentTask.collectAsState()

    LaunchedEffect(uploadTask?.status) {
        if (uploadTask?.status == UploadStatus.SUCCESS) {
            onRefresh()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val stories by (storyViewModel?.storyGroups ?: chatViewModel.stories).collectAsState()
    val myStories by (storyViewModel?.myStories ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val likedStoryIds by (storyViewModel?.likedStoryIds ?: kotlinx.coroutines.flow.MutableStateFlow(emptySet())).collectAsState()
    val isUploadingStory by (storyViewModel?.isUploading ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val trendingWorks by (discoverViewModel?.trendingWorks ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val context = LocalContext.current

    var showStoryViewer by remember { mutableStateOf(false) }
    var showStoryCreationSheet by remember { mutableStateOf(false) }
    var showStoryGalleryPicker by remember { mutableStateOf(false) }
    var showStoryCamera by remember { mutableStateOf(false) }
    var editingStoryImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingUploadStoryUri by remember { mutableStateOf<Uri?>(null) }
    var showShareStoryAudienceSheet by remember { mutableStateOf(false) }
    var showOnlyFriendsManagementSheet by remember { mutableStateOf(false) }

    val shouldHideBottomNav = showStoryViewer || showStoryCreationSheet || showStoryGalleryPicker ||
        showStoryCamera || (editingStoryImageUri != null) || showShareStoryAudienceSheet || showOnlyFriendsManagementSheet
    LaunchedEffect(shouldHideBottomNav) {
        onStoryViewerChange?.invoke(shouldHideBottomNav)
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

    var hasPendingNewDrops by remember { mutableStateOf(true) }
    val refreshProgressAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var hasUnreadNotifications by remember { mutableStateOf(true) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val density = LocalDensity.current
    val maxCollapsePx = remember(density) { with(density) { 280.dp.toPx() } }
    var collapseOffsetPx by remember { mutableFloatStateOf(0f) }
    var snapJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val nestedScrollConnection = remember(maxCollapsePx, listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag && snapJob?.isActive == true) {
                    snapJob?.cancel()
                }
                val deltaY = available.y
                // When user scrolls down (finger moves up -> deltaY < 0): collapse extended header first
                if (deltaY < 0f && collapseOffsetPx < maxCollapsePx) {
                    val newOffset = (collapseOffsetPx - deltaY).coerceIn(0f, maxCollapsePx)
                    val consumedY = -(newOffset - collapseOffsetPx)
                    collapseOffsetPx = newOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.Drag && snapJob?.isActive == true) {
                    snapJob?.cancel()
                }
                val deltaY = available.y
                // When user scrolls up (finger moves down -> deltaY > 0): expand header when at top of list
                if (deltaY > 0f && collapseOffsetPx > 0f) {
                    val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    if (isAtTop) {
                        val newOffset = (collapseOffsetPx - deltaY).coerceIn(0f, maxCollapsePx)
                        val consumedY = -(newOffset - collapseOffsetPx)
                        collapseOffsetPx = newOffset
                        return Offset(0f, consumedY)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val progress = if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx)
                // If user flings upward (scrolling feed down) while header is partially open:
                // Magnetically snap to fully collapsed before list starts scrolling,
                // ensuring stories smoothly dock right beneath the header and never slide behind it!
                if (available.y < -300f && progress < 0.99f && progress > 0.05f) {
                    snapJob?.cancel()
                    snapJob = coroutineScope.launch {
                        animate(
                            initialValue = collapseOffsetPx,
                            targetValue = maxCollapsePx,
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { value, _ ->
                            collapseOffsetPx = value
                        }
                    }
                    return Velocity(0f, available.y)
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val progress = if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx)
                if (progress > 0.01f && progress < 0.99f) {
                    val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    val targetPx = when {
                        available.y < -250f -> maxCollapsePx
                        available.y > 250f && isAtTop -> 0f
                        progress >= 0.45f -> maxCollapsePx
                        else -> 0f
                    }
                    snapJob?.cancel()
                    snapJob = coroutineScope.launch {
                        animate(
                            initialValue = collapseOffsetPx,
                            targetValue = targetPx,
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { value, _ ->
                            collapseOffsetPx = value
                        }
                    }
                }
                return Velocity.Zero
            }
        }
    }

    val collapseProgress by remember {
        derivedStateOf {
            if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx).coerceIn(0f, 1f)
        }
    }

    // Global feed audio mute state (synced across all feed videos)
    var isFeedMuted by rememberSaveable { mutableStateOf(true) }

    // Instagram-style active viewport video detection
    val activePlayingPostId by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf null

            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            val viewportHeight = viewportEnd - viewportStart
            val viewportCenter = viewportStart + viewportHeight / 2

            var bestPostId: String? = null
            var minCenterDistance = Int.MAX_VALUE

            for (item in visibleItems) {
                val key = item.key as? String ?: continue
                if (
                    key == "stories" ||
                    key == "community_upload_progress_banner" ||
                    key == "active_feed_filter_badge" ||
                    key == "trending_spotlight_reel" ||
                    key.startsWith("category_")
                ) {
                    continue
                }

                val itemTop = item.offset
                val itemBottom = item.offset + item.size
                val visibleTop = maxOf(itemTop, viewportStart)
                val visibleBottom = minOf(itemBottom, viewportEnd)
                val visibleHeight = (visibleBottom - visibleTop).coerceAtLeast(0)

                val visibleFraction = if (item.size > 0) visibleHeight.toFloat() / item.size.toFloat() else 0f
                if (visibleFraction < 0.35f && visibleHeight < 300) {
                    continue
                }

                val itemCenter = itemTop + item.size / 2
                val distance = kotlin.math.abs(itemCenter - viewportCenter)

                if (distance < minCenterDistance) {
                    minCenterDistance = distance
                    bestPostId = key
                }
            }

            bestPostId
        }
    }

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
            snapJob?.cancel()
            collapseOffsetPx = 0f
            onRefresh()
            listState.animateScrollToItem(0)
            kotlinx.coroutines.delay(120)
            refreshProgressAnim.snapTo(0f)
            com.chroma.studio.chat.sound.SoundManager.playRefresh(context)
            showToast("Feed refreshed • Synced latest creations")
        }
    }

    val followingUserIds by (discoverViewModel?.followingUserIds ?: com.chroma.studio.data.mutation.OptimisticFollowManager.followingUserIds).collectAsState()
    var selectedFeedTab by remember { mutableStateOf(CommunityFeedTab.ALL) }
    var selectedSortOrder by remember { mutableStateOf(CommunitySortOrder.LATEST) }
    var showCommunityGuidelines by remember { mutableStateOf(false) }

    // FIX 1: Ensure Trending Masterworks reel still populates with top creations when early-stage posts have < 100 likes
    val qualifyingMasterworks = remember(trendingWorks, works) {
        val strictMasterworks = (trendingWorks + works)
            .distinctBy { it.id }
            .filter { it.likesCount > 100 && it.effectiveCommentsCount >= 5 }
            .sortedByDescending { it.likesCount }

        if (strictMasterworks.isNotEmpty()) {
            strictMasterworks
        } else {
            (trendingWorks + works)
                .distinctBy { it.id }
                .sortedByDescending { it.likesCount }
                .take(8)
        }
    }

    val filteredWorks = remember(works, searchQuery, selectedFeedTab, selectedSortOrder, qualifyingMasterworks, followingUserIds, savedWorkIds) {
        val baseList = when (selectedFeedTab) {
            CommunityFeedTab.ALL -> works
            CommunityFeedTab.TRENDING -> qualifyingMasterworks
            CommunityFeedTab.FLASH -> works.filter { isVideoPost(it) || it.postType == "video" || it.aspectRatio.trim() == "9:16" }
            CommunityFeedTab.FOLLOWING -> works.filter { followingUserIds.contains(it.creatorId) }
            CommunityFeedTab.SAVED -> works.filter { savedWorkIds.contains(it.id) }
        }

        val searchFiltered = if (searchQuery.isBlank()) {
            baseList
        } else {
            val cleanQ = searchQuery.trim().removePrefix("#").removePrefix("@")
            baseList.filter {
                it.title.contains(cleanQ, ignoreCase = true) ||
                it.description.contains(cleanQ, ignoreCase = true) ||
                it.authorUsername.contains(cleanQ, ignoreCase = true) ||
                it.authorDisplayName.contains(cleanQ, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(cleanQ, ignoreCase = true) }
            }
        }

        when (selectedSortOrder) {
            CommunitySortOrder.LATEST -> searchFiltered.sortedByDescending { it.timestamp }
            CommunitySortOrder.TOP_LIKED -> searchFiltered.sortedByDescending { it.likesCount }
            CommunitySortOrder.MOST_REMIXED -> searchFiltered.sortedByDescending { it.remixCount }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (colors.isDark) Color(0xFF0E0E11) else Color(0xFFFFFFFF))
            .nestedScroll(nestedScrollConnection)
    ) {
        BackHandler(enabled = isSearchExpanded) {
            isSearchExpanded = false
            searchQuery = ""
        }
        BackHandler(enabled = showCreateMenu) {
            showCreateMenu = false
        }
        BackHandler(enabled = showStoryCamera) {
            showStoryCamera = false
        }
        BackHandler(enabled = editingStoryImageUri != null) {
            editingStoryImageUri = null
        }
        BackHandler(enabled = showStoryGalleryPicker) {
            showStoryGalleryPicker = false
        }
        BackHandler(enabled = showStoryCreationSheet) {
            showStoryCreationSheet = false
        }
        BackHandler(enabled = showStoryViewer) {
            showStoryViewer = false
        }



        val headerSubtitle = remember(selectedFeedTab, filteredWorks.size) {
            when (selectedFeedTab) {
                CommunityFeedTab.ALL -> "${filteredWorks.size} creations • Latest feed"
                CommunityFeedTab.FOLLOWING -> "From creators you follow"
                CommunityFeedTab.SAVED -> "${filteredWorks.size} saved creations"
                CommunityFeedTab.FLASH -> "Vertical motion drops"
                CommunityFeedTab.TRENDING -> "Top community masterworks"
            }
        }

        val isFeedRefreshing = isRefreshing || refreshProgressAnim.value > 0f

        Column(modifier = Modifier.fillMaxSize()) {
            // Clean Expanded Header (Main Heading + Small Sub-text ONLY) + Compact Action Bar
            CreatorFeedHeader(
                title = "Community",
                subtitle = headerSubtitle,
                collapseProgress = collapseProgress,
                selectedFeedTab = selectedFeedTab,
                onSelectFeedTab = { tab ->
                    selectedFeedTab = tab
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                },
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isPlusMenuOpen = showCreateMenu,
                hasUnreadNotifications = hasUnreadNotifications,
                isRefreshing = isFeedRefreshing,
                hasPendingNewDrops = hasPendingNewDrops,
                refreshProgress = refreshProgressAnim.value,
                onTogglePlusMenu = { showCreateMenu = !showCreateMenu },
                onToggleNotificationDrawer = {
                    showCreateMenu = false
                    hasUnreadNotifications = false
                    onNavigateToNotifications()
                },
                onRefreshFeed = { triggerFeedRefresh() },
                onExpandSearch = { isSearchExpanded = true },
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
                                    showStoryGalleryPicker = true
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
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OscellionLoadingSpinner(color = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                }
            }

            if (isLoading) {
                LazyColumn(modifier = Modifier.fillMaxSize().clipToBounds(), userScrollEnabled = false) {
                    item {
                        CommunityStoriesCard(
                            stories = emptyList(),
                            isLoading = true,
                            hasMyStory = false,
                            myStoryAvatarUrl = "",
                            myStoryGender = null,
                            myStoriesList = emptyList(),
                            isUploading = false,
                            onMyStoryClick = {},
                            onAddStoryClick = {},
                            onStoryClick = {}
                        )
                    }
                    if (uploadTask != null && uploadTask?.status != UploadStatus.IDLE) {
                        item(key = "loading_upload_progress_banner") {
                            CommunityUploadProgressBanner(
                                task = uploadTask!!,
                                onRetry = { FlashUploadManager.retry() },
                                onViewDrafts = { onNavigateToDrafts() },
                                onDismiss = { FlashUploadManager.dismiss() }
                            )
                        }
                    }
                    items(3) {
                        SkeletonPost()
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 2.dp, bottom = 110.dp),
                    modifier = Modifier.fillMaxSize().clipToBounds()
                ) {
                    // Unified Stories Section
                    item(key = "stories") {
                        CommunityStoriesCard(
                            stories = stories,
                            isLoading = false,
                            hasMyStory = myStories.isNotEmpty(),
                            myStoryAvatarUrl = myProfile?.avatarUrl ?: "",
                            myStoryGender = myProfile?.gender,
                            myStoriesList = myStories,
                            isUploading = isUploadingStory,
                            onMyStoryClick = {
                                if (myStories.isNotEmpty()) {
                                    storyViewerInitialIndex = -1
                                    showStoryViewer = true
                                } else {
                                    showStoryCreationSheet = true
                                }
                            },
                            onAddStoryClick = {
                                showStoryCreationSheet = true
                            },
                            onStoryClick = { groupIndex ->
                                storyViewerInitialIndex = groupIndex
                                showStoryViewer = true
                            }
                        )
                    }


                    // ── INSTAGRAM-STYLE FLASH VIDEO / POST UPLOAD PROGRESS BANNER ──
                    if (uploadTask != null && uploadTask?.status != UploadStatus.IDLE) {
                        item(key = "community_upload_progress_banner") {
                            CommunityUploadProgressBanner(
                                task = uploadTask!!,
                                onRetry = { FlashUploadManager.retry() },
                                onViewDrafts = { onNavigateToDrafts() },
                                onDismiss = { FlashUploadManager.dismiss() }
                            )
                            HorizontalDivider(
                                color = colors.glassBorder,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    // ── TRENDING MASTERWORKS SPOTLIGHT REEL ──
                    if ((selectedFeedTab == CommunityFeedTab.ALL || selectedFeedTab == CommunityFeedTab.TRENDING) &&
                        qualifyingMasterworks.isNotEmpty() &&
                        searchQuery.isBlank()
                    ) {
                        item(key = "trending_spotlight_reel") {
                            CommunityTrendingSpotlightReel(
                                trendingWorks = qualifyingMasterworks,
                                repository = repository,
                                onWorkClick = { work ->
                                    if (isVideoPost(work)) {
                                        onFlashVideoClick?.invoke(work.id)
                                    } else {
                                        onAuthorClick(work.creatorId, work.id)
                                    }
                                }
                            )
                        }
                    }

                    // Feed Posts List
                    if (filteredWorks.isEmpty()) {
                        item(key = "empty_state") {
                            CommunityInteractiveEmptyState(
                                selectedTab = selectedFeedTab,
                                searchQuery = searchQuery,
                                onCreatePostClick = { onCreatePost() },
                                onRefreshClick = { triggerFeedRefresh() },
                                onTopicClick = { topic ->
                                    searchQuery = topic
                                    isSearchExpanded = true
                                }
                            )
                        }
                    } else {
                        items(filteredWorks, key = { it.id }) { work ->
                            val isFollowing = followingUserIds.contains(work.creatorId)
                            FeedPostCard(
                                work = work,
                                isLiked = likedWorkIds.contains(work.id),
                                isSaved = savedWorkIds.contains(work.id),
                                isFollowingCreator = isFollowing,
                                isInViewport = (activePlayingPostId == work.id),
                                isFeedMuted = isFeedMuted,
                                onToggleFeedMute = { isFeedMuted = !isFeedMuted },
                                onToggleFollowCreator = {
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                                    if (uid.isNullOrBlank()) {
                                        onRequireLogin()
                                    } else {
                                        discoverViewModel?.toggleFollow(work.creatorId, uid)
                                    }
                                },
                                onShowGuidelines = { showCommunityGuidelines = true },
                                onShowToast = { showToast(it) },
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
                                onForceLike = {
                                    val authUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                    if (!authUserId.isNullOrBlank()) {
                                        discoverViewModel?.toggleLike(work.id, authUserId, forceLike = true)
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
                                        putExtra(Intent.EXTRA_TEXT, "Check out '${work.title}' by @${work.authorUsername} on Oscellion: https://oscellion.vercel.app/post/${work.id}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Post"))
                                },
                                onSaveClick = { discoverViewModel?.toggleSave(work.id) },
                                onFlashVideoClick = onFlashVideoClick,
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

        // Story Creation Options Bottom Sheet
        if (showStoryCreationSheet) {
            StoryCreationBottomSheet(
                onDismissRequest = { showStoryCreationSheet = false },
                onChooseFromGallery = {
                    showStoryCreationSheet = false
                    showStoryGalleryPicker = true
                },
                onTextStory = {
                    showStoryCreationSheet = false
                    onNavigateToCreateStory?.invoke()
                }
            )
        }

        // Unified In-App Oscellion Gallery for Stories
        if (showStoryGalleryPicker) {
            com.chroma.studio.chat.ui.components.CustomGalleryPickerModal(
                onDismiss = { showStoryGalleryPicker = false },
                onLaunchCamera = {
                    showStoryGalleryPicker = false
                    showStoryCamera = true
                },
                onRequestMediaPermission = {},
                onSendMedia = { uris, _ ->
                    showStoryGalleryPicker = false
                    val pickedUri = uris.firstOrNull()
                    if (pickedUri != null) {
                        editingStoryImageUri = pickedUri
                    }
                }
            )
        }

        // Professional In-App Camera for Stories
        if (showStoryCamera) {
            com.chroma.studio.chat.ui.components.IosCameraScreen(
                onDismiss = { showStoryCamera = false },
                onPhotoCaptured = { uri, _ ->
                    showStoryCamera = false
                    editingStoryImageUri = uri
                },
                onVideoCaptured = { uri, _ ->
                    showStoryCamera = false
                    pendingUploadStoryUri = uri
                    showShareStoryAudienceSheet = true
                }
            )
        }

        // Story Image Editor
        if (editingStoryImageUri != null) {
            FlashPhotoEditorScreen(
                photoUris = listOf(editingStoryImageUri!!),
                title = "Edit Story",
                initialAspectRatio = PhotoAspectRatio.STORY_9_16,
                nextButtonLabel = "Share Story",
                onExportUris = { editedUris ->
                    val finalUri = editedUris.firstOrNull() ?: editingStoryImageUri!!
                    pendingUploadStoryUri = finalUri
                    showShareStoryAudienceSheet = true
                },
                onBack = {
                    editingStoryImageUri = null
                }
            )
        }

        // Instagram-style Audience Selector for Story
        if (showShareStoryAudienceSheet && pendingUploadStoryUri != null) {
            val onlyFriends by (storyViewModel?.onlyFriendsIds ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
            ShareStoryAudienceSheet(
                onDismissRequest = {
                    showShareStoryAudienceSheet = false
                },
                onSharePublic = {
                    showShareStoryAudienceSheet = false
                    val uri = pendingUploadStoryUri ?: return@ShareStoryAudienceSheet
                    pendingUploadStoryUri = null
                    editingStoryImageUri = null
                    storyViewModel?.uploadStory(context, uri, audienceType = "public")
                    com.chroma.studio.ui.components.ToastManager.showToast("Sharing your story... 🌟")
                },
                onShareOnlyFriends = {
                    showShareStoryAudienceSheet = false
                    val uri = pendingUploadStoryUri ?: return@ShareStoryAudienceSheet
                    pendingUploadStoryUri = null
                    editingStoryImageUri = null
                    storyViewModel?.uploadStory(context, uri, audienceType = "only_friends", customAudienceIds = onlyFriends)
                    com.chroma.studio.ui.components.ToastManager.showToast("Shared with Only Friend! ⭐")
                },
                onEditOnlyFriends = {
                    showOnlyFriendsManagementSheet = true
                },
                onlyFriendsCount = onlyFriends.size
            )
        }

        // Only Friend List Management Sheet
        if (showOnlyFriendsManagementSheet && storyViewModel != null) {
            OnlyFriendsManagementSheet(
                storyViewModel = storyViewModel,
                onDismissRequest = { showOnlyFriendsManagementSheet = false }
            )
        }

        // Community Standards & Guidelines Bottom Sheet
        if (showCommunityGuidelines) {
            CommunityGuidelinesSheet(
                onDismiss = { showCommunityGuidelines = false }
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
    rankIndex: Int = 0,
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

    val cardShape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .width(96.dp)
            .height(138.dp)
            .clip(cardShape)
            .background(if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
            .border(
                width = 0.5.dp,
                color = if (colors.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                shape = cardShape
            )
            .clickable { onClick() }
    ) {
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
        } else if (!work.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(work.imageUrl).crossfade(true).build(),
                contentDescription = work.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Top specular hairline highlight
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(0.75.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Top Rank Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.5.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = Lucide.Flame,
                contentDescription = null,
                tint = Color(0xFFFF9500),
                modifier = Modifier.size(9.dp)
            )
            Text(
                text = "#${rankIndex + 1}",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.5.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Play,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(8.dp)
                )
            }
        }

        // Bottom Scrim & Compact Metrics
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = work.title.ifEmpty { "Masterwork" },
                color = Color.White,
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = ChromaIcons.HeartFilled,
                        contentDescription = null,
                        tint = Color(0xFFFF2D55),
                        modifier = Modifier.size(9.dp)
                    )
                    Text(
                        text = "${work.likesCount}",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Lucide.MessageCircle,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(9.dp)
                    )
                    Text(
                        text = "${work.effectiveCommentsCount}",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Clean Cupertino Category Tabs for Community Feed.
 * Features an animated Hr Line underline indicator beneath the selected category tab,
 * and a subtle full-width HorizontalDivider. Strictly free of pills or capsule shapes.
 */
@Composable
fun CommunityFeedTabBar(
    selectedTab: CommunityFeedTab,
    selectedSort: CommunitySortOrder,
    savedCount: Int,
    trendingCount: Int,
    onSelectTab: (CommunityFeedTab) -> Unit,
    onSelectSort: (CommunitySortOrder) -> Unit
) {
    val colors = LocalOscellionColors.current
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (colors.isDark) Color(0xFF16161A) else Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CommunityFeedTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val activeColor = if (isSelected) Color(0xFF007AFF) else colors.textMuted

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectTab(tab) }
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = activeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = tab.label,
                                color = if (isSelected) colors.textMain else colors.textMuted,
                                fontSize = 12.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (tab == CommunityFeedTab.SAVED && savedCount > 0) {
                                Text(
                                    text = "($savedCount)",
                                    color = if (isSelected) Color(0xFF007AFF) else colors.textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(5.dp))

                        val indicatorWidth by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 0.dp,
                            animationSpec = tween(280, easing = FluidCubicBezier),
                            label = "tabIndicatorWidth"
                        )
                        Box(
                            modifier = Modifier
                                .width(indicatorWidth)
                                .height(2.dp)
                                .background(if (isSelected) Color(0xFF007AFF) else Color.Transparent)
                        )
                    }
                }
            }

            // Quick Sort Dropdown Action
            Box(
                modifier = Modifier.padding(end = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showSortMenu = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Lucide.ArrowUpDown,
                        contentDescription = "Sort Feed",
                        tint = colors.textMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = when (selectedSort) {
                            CommunitySortOrder.LATEST -> "Latest"
                            CommunitySortOrder.TOP_LIKED -> "Liked"
                            CommunitySortOrder.MOST_REMIXED -> "Remixed"
                        },
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier
                        .background(colors.glassBg)
                        .border(0.5.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                ) {
                    CommunitySortOrder.values().forEach { sort ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    sort.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedSort == sort) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedSort == sort) Color(0xFF007AFF) else colors.textMain
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = sort.icon,
                                    contentDescription = sort.label,
                                    tint = if (selectedSort == sort) Color(0xFF007AFF) else colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                onSelectSort(sort)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)
    }
}

/**
 * Trending Masterworks Spotlight Showcase Reel.
 */
@Composable
fun CommunityTrendingSpotlightReel(
    trendingWorks: List<DiscoverPost>,
    repository: WorkRepository,
    onWorkClick: (DiscoverPost) -> Unit
) {
    val colors = LocalOscellionColors.current
    if (trendingWorks.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (colors.isDark) Color(0xFF141416) else Color.White)
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFFF9500).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Flame,
                        contentDescription = null,
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(11.dp)
                    )
                }
                Text(
                    text = "TRENDING",
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = "${trendingWorks.size} ${if (trendingWorks.size == 1) "drop" else "drops"}",
                fontSize = 10.5.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted
            )
        }

        Spacer(Modifier.height(5.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            itemsIndexed(trendingWorks, key = { _, work -> "trend_${work.id}" }) { index, work ->
                TrendingCard(
                    work = work,
                    rankIndex = index,
                    repository = repository,
                    onClick = { onWorkClick(work) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)
    }
}

@Composable
fun CommunityGuidelinesSheet(
    onDismiss: () -> Unit
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
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Gray.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF007AFF).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.ShieldCheck,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "COMMUNITY STANDARDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Chroma Studio Creator Code",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Icon(Lucide.X, contentDescription = "Close", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)
            Spacer(Modifier.height(14.dp))

            val guidelines = listOf(
                Triple(
                    Lucide.Sparkles,
                    "Creative Attribution",
                    "Remixed works preserve automated lineage back to original shaders."
                ),
                Triple(
                    Lucide.Shield,
                    "Visual Safety",
                    "Real-time safety compliance across all video and art drops."
                ),
                Triple(
                    Lucide.Palette,
                    "Shader & Asset Rights",
                    "Creators retain ownership of color schemes and motion parameters."
                ),
                Triple(
                    Lucide.HeartHandshake,
                    "Constructive Community",
                    "Built on mutual respect for visual directors and artists."
                )
            )

            guidelines.forEachIndexed { index, (icon, title, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(16.dp).padding(top = 1.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = desc,
                            fontSize = 11.5.sp,
                            color = colors.textMuted,
                            lineHeight = 14.sp
                        )
                    }
                }
                if (index < guidelines.size - 1) {
                    HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF007AFF))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "I Agree to Community Standards",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
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

@Composable
fun EndOfFeedIndicator(modifier: Modifier = Modifier) {
    val colors = LocalOscellionColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 16.dp),
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

@Composable
fun CreatorFeedHeader(
    title: String = "Community",
    subtitle: String = "Fresh creations & stories",
    collapseProgress: Float = 0f,
    selectedFeedTab: CommunityFeedTab = CommunityFeedTab.ALL,
    onSelectFeedTab: (CommunityFeedTab) -> Unit = {},
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
    var isFeedMenuOpen by remember { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isFeedMenuOpen) 180f else 0f,
        animationSpec = tween(260, easing = FluidCubicBezier),
        label = "chevronRotation"
    )

    val plusRotation by animateFloatAsState(
        targetValue = if (isPlusMenuOpen) 45f else 0f,
        animationSpec = tween(300, easing = FluidCubicBezier),
        label = "plusRotation"
    )

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

    val headerItemsOffset by animateDpAsState(
        targetValue = if (isSearchExpanded) (-40).dp else 0.dp,
        animationSpec = tween(380, easing = FluidCubicBezier),
        label = "headerItemsOffset"
    )
    val headerItemsAlpha by animateFloatAsState(
        targetValue = if (isSearchExpanded) 0f else 1f,
        animationSpec = tween(240, easing = LinearEasing),
        label = "headerItemsAlpha"
    )

    val clampedCollapse = collapseProgress.coerceIn(0f, 1f)
    val expandedHeight = (280.dp * (1f - clampedCollapse)).coerceAtLeast(0.dp)
    val expandedAlpha = (1f - (clampedCollapse * 1.35f)).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (colors.isDark) Color(0xFF16161A) else Color.White)
            .statusBarsPadding()
    ) {
        // EXPANDED HEADER: Strictly ONE main heading and a small sub-text (Nothing else)
        if (expandedHeight > 1.dp && !isSearchExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(expandedHeight)
                    .alpha(expandedAlpha)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 46.sp,
                    lineHeight = 50.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp,
                    color = colors.textMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // COMPACT ACTION BAR
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val maxHeaderWidth = maxWidth

            // 1. LEFT BRAND HEADING: "Oscellion" + Instagram-Style Feed Switcher Dropdown
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = headerItemsOffset)
                    .alpha(headerItemsAlpha),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isSearchExpanded) {
                                isFeedMenuOpen = !isFeedMenuOpen
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Text(
                            text = "Oscellion",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = colors.textMain
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Lucide.ChevronDown,
                            contentDescription = "Switch Feed",
                            tint = colors.textMain.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(chevronRotation)
                        )
                    }

                    DropdownMenu(
                        expanded = isFeedMenuOpen,
                        onDismissRequest = { isFeedMenuOpen = false },
                        offset = DpOffset(x = 0.dp, y = 6.dp),
                        modifier = Modifier
                            .width(200.dp)
                            .background(
                                color = if (colors.isDark) Color(0xFF16161A) else Color.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = colors.glassBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        val feedTabs = listOf(
                            CommunityFeedTab.ALL,
                            CommunityFeedTab.FOLLOWING,
                            CommunityFeedTab.SAVED,
                            CommunityFeedTab.FLASH,
                            CommunityFeedTab.TRENDING
                        )

                        feedTabs.forEachIndexed { index, tab ->
                            val isSelected = selectedFeedTab == tab
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) colors.primary else colors.textMain,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = tab.label,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) colors.primary else colors.textMain,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Lucide.Check,
                                                contentDescription = "Selected",
                                                tint = colors.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    isFeedMenuOpen = false
                                    onSelectFeedTab(tab)
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            )

                            if (index < feedTabs.lastIndex) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = colors.glassBorder.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. RIGHT ACTION ICONS: [+] [Heart/Notif] [Refresh]
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 40.dp)
                    .alpha(headerItemsAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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

                Box(modifier = Modifier.size(36.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable(enabled = !isSearchExpanded, onClick = onToggleNotificationDrawer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Heart,
                            contentDescription = "Notifications",
                            tint = colors.textMain,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (hasUnreadNotifications) {
                        Box(
                            modifier = Modifier
                                .size(8.5.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-4.5).dp, y = 4.5.dp)
                                .background(if (colors.isDark) Color(0xFF16161A) else Color.White, CircleShape)
                                .padding(1.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48))
                        )
                    }
                }

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
                                .size(8.5.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-4.5).dp, y = 4.5.dp)
                                .background(if (colors.isDark) Color(0xFF16161A) else Color.White, CircleShape)
                                .padding(1.5.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                        )
                    }
                }
            }

            // 3. SEARCH ICON & SMOOTH EXPANDING SEARCH BAR
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

            val searchShape = if (isSearchExpanded) RoundedCornerShape(10.dp) else CircleShape
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(searchBarWidth)
                    .height(36.dp)
                    .clip(searchShape)
                    .background(searchBgColor)
                    .border(0.5.dp, searchBorderColor, searchShape)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

@Composable
fun ScallopedVerifiedBadge(
    modifier: Modifier = Modifier.size(15.dp),
    size: Dp = 15.dp,
    badgeColor: Color = Color(0xFF007AFF)
) {
    VerifiedBadge(
        modifier = modifier,
        size = size,
        badgeColor = badgeColor
    )
}

fun parseAspectRatio(ratioStr: String?, postType: String = ""): Float {
    val clean = ratioStr?.trim()?.lowercase()
    if (clean.isNullOrBlank() || clean == "original" || clean == "rounded" || clean == "full") {
        return when (postType.lowercase()) {
            "video", "flash" -> 9f / 16f
            "image" -> 1f
            else -> 4f / 3f
        }
    }
    return when {
        clean == "1:1" || clean == "1/1" || clean == "square" -> 1f / 1f
        clean == "4:5" || clean == "4/5" || clean == "portrait" -> 4f / 5f
        clean == "3:4" || clean == "3/4" -> 3f / 4f
        clean == "4:3" || clean == "4/3" -> 4f / 3f
        clean == "5:4" || clean == "5/4" -> 5f / 4f
        clean == "16:9" || clean == "16/9" -> 16f / 9f
        clean == "9:16" || clean == "9/16" -> 9f / 16f
        clean == "21:9" || clean == "21/9" || clean == "cinematic" -> 21f / 9f
        else -> if (postType.lowercase() == "image") 1f else 4f / 3f
    }
}

class FeedPostModalState(
    val isOwner: Boolean
) {
    var showMenu by mutableStateOf(false)
    var showDeleteConfirm by mutableStateOf(false)
    var showBlockConfirm by mutableStateOf(false)
    var showEditModal by mutableStateOf(false)
    var showVisibilitySettings by mutableStateOf(false)
    var showAboutDialog by mutableStateOf(false)
    var showAnalyticsModal by mutableStateOf(false)
    var showCreatorProfileModal by mutableStateOf(false)
    var showOriginalPostModal by mutableStateOf(false)
}

@Composable
private fun FeedPostHeader(
    displayName: String,
    avatarUrl: String,
    username: String,
    timestamp: Long,
    verified: Boolean,
    isRemixPost: Boolean,
    remixedFromAuthorUsername: String?,
    isOwner: Boolean,
    isFollowingCreator: Boolean,
    onAvatarClick: () -> Unit,
    onRemixTagClick: () -> Unit,
    onToggleFollowCreator: (() -> Unit)?,
    onOptionsClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    // Clean fallbacks so displayName or @username are NEVER blank ("@ • 1h ago")
    val safeDisplayName = displayName.trim().ifBlank {
        username.trim().removePrefix("@").ifBlank { "Chroma Creator" }
    }
    val safeUsername = username.trim().removePrefix("@").ifBlank {
        safeDisplayName.lowercase().replace(" ", "_")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IosAvatarView(
            name = safeDisplayName,
            avatarUrl = avatarUrl,
            size = 34.dp,
            modifier = Modifier.clickable { onAvatarClick() }
        )

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onAvatarClick() },
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = safeDisplayName,
                    color = colors.textMain,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (verified) {
                    ScallopedVerifiedBadge(size = 13.dp)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "@$safeUsername",
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "•",
                    color = Color(0xFF8E8E93),
                    fontSize = 9.sp,
                    lineHeight = 11.sp
                )
                Text(
                    text = getTimeAgo(timestamp),
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                if (isRemixPost && !remixedFromAuthorUsername.isNullOrEmpty()) {
                    Text(
                        text = "•",
                        color = Color(0xFF8E8E93),
                        fontSize = 9.sp,
                        lineHeight = 11.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                            .clickable { onRemixTagClick() }
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Sparkles,
                            contentDescription = "Remix",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(9.dp)
                        )
                        Text(
                            text = "Remix",
                            color = Color(0xFF007AFF),
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                if (!isOwner) {
                    Text(
                        text = "•",
                        color = Color(0xFF8E8E93),
                        fontSize = 9.sp,
                        lineHeight = 11.sp
                    )
                    Text(
                        text = if (isFollowingCreator) "Following" else "Follow",
                        color = if (isFollowingCreator) Color(0xFF8E8E93) else Color(0xFF007AFF),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            onToggleFollowCreator?.invoke()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onOptionsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LucideComposeIcons.MoreHorizontal,
                contentDescription = "Options",
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun FeedPostActionRow(
    work: DiscoverPost,
    isLiked: Boolean,
    isSaved: Boolean,
    currentUserId: String?,
    onRequireLogin: () -> Unit,
    onLikeClick: () -> Unit,
    onRemixClick: () -> Unit,
    onRemixWork: ((DiscoverPost) -> Unit)?,
    onDownloadClick: () -> Unit,
    onSaveClick: (() -> Unit)?,
    onShareClick: (() -> Unit)?
) {
    val colors = LocalOscellionColors.current

    val initialLiked = isLiked || (!currentUserId.isNullOrBlank() && work.likedBy.contains(currentUserId))
    var locallyLiked by remember(work.id, isLiked, work.likedBy) { mutableStateOf(initialLiked) }
    var localLikesCountDelta by remember(work.id) { mutableIntStateOf(0) }
    LaunchedEffect(isLiked, work.likedBy) {
        locallyLiked = isLiked || (!currentUserId.isNullOrBlank() && work.likedBy.contains(currentUserId))
        localLikesCountDelta = 0
    }

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

    var locallySaved by remember(work.id) { mutableStateOf(isSaved) }
    var localSaveCountDelta by remember(work.id) { mutableIntStateOf(0) }
    LaunchedEffect(isSaved) {
        locallySaved = isSaved
        localSaveCountDelta = 0
    }

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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val effectiveLiked = locallyLiked
            val displayLikesCount = (work.likesCount + localLikesCountDelta).coerceAtLeast(0)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
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
                    .padding(horizontal = 3.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (effectiveLiked) ChromaIcons.HeartFilled else Lucide.Heart,
                    contentDescription = if (effectiveLiked) "Liked" else "Like",
                    tint = if (effectiveLiked) Color(0xFFFF2D55) else colors.textMain,
                    modifier = Modifier.size(20.dp)
                )
                if (!work.hideLikesCount && displayLikesCount > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$displayLikesCount",
                        color = if (effectiveLiked) Color(0xFFFF2D55) else colors.textMain,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!work.turnOffRemix) {
                val alreadyRemixed = currentUserId != null && work.remixedBy.contains(currentUserId)
                val effectiveRemixed = locallyRemixed || alreadyRemixed
                val displayRemixCount = (work.remixCount + localRemixCountDelta).coerceAtLeast(0)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
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
                        .padding(horizontal = 3.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = rememberLucideHeartHandshakeIcon(
                            isRemixed = effectiveRemixed || displayRemixCount > 0,
                            strokeColor = if (effectiveRemixed) Color(0xFF007AFF) else colors.textMain,
                            heartFillColor = if (effectiveRemixed) Color(0xFF007AFF).copy(alpha = 0.15f) else null
                        ),
                        contentDescription = "Remix",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    if (!work.hideRemixesCount && displayRemixCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$displayRemixCount",
                            color = if (effectiveRemixed) Color(0xFF007AFF) else colors.textMain.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            val alreadyDownloaded = currentUserId != null && work.downloadedBy.contains(currentUserId)
            val effectiveDownloaded = locallyDownloaded || alreadyDownloaded
            val displayDownloadCount = (work.downloadCount + localDownloadCountDelta).coerceAtLeast(0)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
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
                    .padding(horizontal = 3.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = LucideComposeIcons.Download,
                    contentDescription = "Save to Studio",
                    tint = if (effectiveDownloaded) Color(0xFF007AFF) else colors.textMain,
                    modifier = Modifier.size(20.dp)
                )
                if (!work.hideDownloadsCount && displayDownloadCount > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$displayDownloadCount",
                        color = if (effectiveDownloaded) Color(0xFF007AFF) else colors.textMain.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val effectiveSaved = locallySaved
            val displaySaveCount = (work.saveCount + localSaveCountDelta).coerceAtLeast(0)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
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
                    .padding(horizontal = 3.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = LucideComposeIcons.bookmark(
                        isSaved = effectiveSaved,
                        savedColor = Color(0xFF007AFF),
                        outlineColor = colors.textMain
                    ),
                    contentDescription = "Bookmark",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
                if (!work.hideSavesCount && displaySaveCount > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$displaySaveCount",
                        color = if (effectiveSaved) Color(0xFF007AFF) else colors.textMain.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            val alreadyShared = currentUserId != null && work.sharedBy.contains(currentUserId)
            val effectiveShared = locallyShared || alreadyShared
            val displayShareCount = (work.shareCount + localShareCountDelta).coerceAtLeast(0)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
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
                    .padding(horizontal = 3.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Lucide.Share2,
                    contentDescription = "Share",
                    tint = if (effectiveShared) Color(0xFF007AFF) else colors.textMain,
                    modifier = Modifier.size(20.dp)
                )
                if (!work.hideSharesCount && displayShareCount > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$displayShareCount",
                        color = if (effectiveShared) Color(0xFF007AFF) else colors.textMain.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedPostMetadata(
    work: DiscoverPost,
    onTagClick: ((String) -> Unit)?,
    onMentionClick: ((String) -> Unit)?,
    onAuthorClick: ((String) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    var isTitleExpanded by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    var likerProfiles by remember(work.id, work.likedBy) { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(work.id, work.likedBy) {
        if (!work.hideLikesCount && work.likedBy.isNotEmpty()) {
            try {
                likerProfiles = DiscoverRepository.getInstance().getProfilesByIds(work.likedBy, limit = 8)
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (!work.hideLikesCount && (work.likesCount > 0 || likerProfiles.isNotEmpty())) {
            val connectedLikers = remember(likerProfiles) {
                likerProfiles.map { it.toConnectedUser() }
            }
            if (connectedLikers.isNotEmpty()) {
                ConnectedUsersAvatarGroup(
                    users = connectedLikers,
                    totalCount = work.likesCount.coerceAtLeast(connectedLikers.size),
                    maxVisible = 3,
                    avatarSize = 18.dp,
                    overlapOffset = 5.dp,
                    prefixText = "Liked by",
                    suffixText = "",
                    showTextSummary = true,
                    onUserClick = { clickedUid ->
                        onAuthorClick?.invoke(clickedUid)
                    }
                )
            }
        }

        // Compact Title (Short 55-char threshold, tight line height)
        if (work.title.isNotBlank()) {
            val needsTitleExpand = work.title.length > 55
            val shortTitle = if (needsTitleExpand) work.title.take(55).trim() else work.title

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(320, easing = FluidCubicBezier))
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
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("...")
                            }
                        }
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    lineHeight = 15.5.sp,
                    maxLines = if (isTitleExpanded) 3 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Compact Description (Short 60-char threshold, tight 14.5sp line height)
        if (work.description.isNotBlank()) {
            val needsDescExpand = work.description.length > 60
            val shortDesc = if (needsDescExpand) work.description.take(60).trim() else work.description

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(320, easing = FluidCubicBezier))
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
                                        color = Color(0xFF007AFF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                ) {
                                    append(" less")
                                }
                            }
                        } else {
                            append(shortDesc)
                            withStyle(
                                SpanStyle(
                                    color = Color(0xFF007AFF),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            ) {
                                append(" ...more")
                            }
                        }
                    },
                    fontSize = 11.5.sp,
                    color = colors.textMain.copy(alpha = 0.85f),
                    lineHeight = 14.5.sp,
                    maxLines = if (isDescriptionExpanded) 4 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!work.remixedFromAuthorUsername.isNullOrEmpty()) {
            val quoteShape = RoundedCornerShape(8.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .clip(quoteShape)
                    .background(if (colors.isDark) Color(0xFF1E1E22) else Color(0xFFF4F4F7))
                    .border(
                        0.5.dp,
                        if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                        quoteShape
                    )
                    .clickable {
                        if (!work.remixedFromAuthorId.isNullOrEmpty()) {
                            onAuthorClick?.invoke(work.remixedFromAuthorId)
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Lucide.HeartHandshake,
                    contentDescription = "Remix",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(12.dp)
                )

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Remixed from",
                        fontSize = 10.5.sp,
                        lineHeight = 12.sp,
                        color = colors.textMuted
                    )
                    Text(
                        text = "@${work.remixedFromAuthorUsername}",
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF007AFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (work.remixedFromAuthorVerified) {
                        VerifiedBadge(size = 10.dp)
                    }
                }

                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        val trendingLabel = work.trendingTag ?: if (work.tags.any { it.contains("trend", ignoreCase = true) }) "Trending" else null
        val hasTagsOrMentions = work.tags.isNotEmpty() || work.mentions.isNotEmpty() || trendingLabel != null
        if (hasTagsOrMentions) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
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
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(2.5.dp))
                        Text(
                            text = trendingLabel,
                            color = if (colors.isDark) Color(0xFFF59E0B) else Color(0xFFB45309),
                            fontSize = 10.5.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                work.tags.take(4).forEach { rawTag ->
                    val cleanTag = if (rawTag.startsWith("#")) rawTag else "#$rawTag"
                    Text(
                        text = cleanTag,
                        color = Color(0xFF007AFF),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTagClick?.invoke(cleanTag)
                        }
                    )
                }

                work.mentions.take(2).forEach { rawMention ->
                    val handle = if (rawMention.startsWith("@")) rawMention else "@$rawMention"
                    Text(
                        text = handle,
                        color = if (colors.isDark) Color(0xFFA1A1AA) else Color(0xFF334155),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
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
}

@Composable
private fun FeedPostModals(
    work: DiscoverPost,
    isOwner: Boolean,
    modalState: FeedPostModalState,
    resolvedDisplayName: String,
    resolvedAvatarUrl: String,
    resolvedVerified: Boolean,
    isFollowingCreator: Boolean,
    onToggleFollowCreator: (() -> Unit)?,
    onAuthorClick: (String) -> Unit,
    onReportClick: ((String) -> Unit)?,
    onArchiveWork: ((String) -> Unit)?,
    onDeleteWork: (String) -> Unit,
    onEditWork: (String, String, String) -> Unit,
    onShowGuidelines: (() -> Unit)?,
    onShowToast: ((String) -> Unit)?,
    onNavigateToOriginal: (String, String) -> Unit,
    onRemixWork: ((DiscoverPost) -> Unit)?,
    onUpdateVisibility: ((hideLikes: Boolean, hideShares: Boolean, hideRemixes: Boolean, hideDownloads: Boolean, hideSaves: Boolean) -> Unit)?
) {
    if (modalState.showMenu) {
        PostOptionsDrawerSheet(
            work = work,
            isOwner = isOwner,
            onDismiss = { modalState.showMenu = false },
            onInterested = { modalState.showMenu = false },
            onNotInterested = { modalState.showMenu = false },
            onViewProfile = {
                modalState.showMenu = false
                modalState.showCreatorProfileModal = true
            },
            onAboutPost = {
                modalState.showMenu = false
                modalState.showAboutDialog = true
            },
            onReport = {
                modalState.showMenu = false
                onReportClick?.invoke(work.id)
            },
            onBlockUser = {
                modalState.showMenu = false
                modalState.showBlockConfirm = true
            },
            onCounterPrivacy = {
                modalState.showMenu = false
                if (isOwner) modalState.showVisibilitySettings = true
            },
            onEditPost = {
                modalState.showMenu = false
                if (isOwner) modalState.showEditModal = true
            },
            onViewAnalytics = {
                modalState.showMenu = false
                if (isOwner) modalState.showAnalyticsModal = true
            },
            onArchive = {
                modalState.showMenu = false
                if (isOwner) onArchiveWork?.invoke(work.id)
            },
            onDeletePost = {
                modalState.showMenu = false
                if (isOwner) modalState.showDeleteConfirm = true
            },
            onOpenGuidelines = {
                modalState.showMenu = false
                onShowGuidelines?.invoke()
            },
            onShowToast = onShowToast
        )
    }

    if (modalState.showOriginalPostModal) {
        OriginalPostInspectorSheet(
            work = work,
            onDismiss = { modalState.showOriginalPostModal = false },
            onNavigateToOriginal = onNavigateToOriginal,
            onRemixWork = onRemixWork
        )
    }

    if (modalState.showVisibilitySettings) {
        PostCountersSettingsSheet(
            work = work,
            onDismiss = { modalState.showVisibilitySettings = false },
            onSave = { hideLikes, hideShares, hideRemixes, hideDownloads, hideSaves ->
                onUpdateVisibility?.invoke(hideLikes, hideShares, hideRemixes, hideDownloads, hideSaves)
            }
        )
    }

    if (modalState.showAboutDialog) {
        AppleDialog(
            title = "About this Post",
            message = "Author: $resolvedDisplayName (@${work.authorUsername})\n" +
                      "Status: ${if (resolvedVerified) "Verified Creator ✓" else "Standard Account"}\n" +
                      "Format: ${work.postType.replaceFirstChar { it.uppercase() }} (${work.aspectRatio})\n" +
                      "Published: ${getTimeAgo(work.timestamp)}\n" +
                      "Post ID: ${work.id}",
            primaryButtonText = "Done",
            onPrimaryClick = { modalState.showAboutDialog = false },
            onDismiss = { modalState.showAboutDialog = false }
        )
    }

    if (modalState.showAnalyticsModal) {
        OwnerAnalyticsModal(
            work = work,
            onDismiss = { modalState.showAnalyticsModal = false }
        )
    }

    if (modalState.showEditModal) {
        EditPostModal(
            work = work,
            onDismiss = { modalState.showEditModal = false },
            onSaveEdits = { _, title, desc ->
                modalState.showEditModal = false
                if (isOwner) {
                    onEditWork(work.id, title, desc)
                }
            }
        )
    }

    if (modalState.showCreatorProfileModal) {
        val totalCreatorRemixes = maxOf(work.remixCount, 1)
        val profileInfo = CreatorProfileInfo(
            id = work.creatorId,
            name = resolvedDisplayName,
            username = work.authorUsername,
            verified = resolvedVerified,
            avatarUrl = resolvedAvatarUrl.takeIf { it.isNotBlank() },
            initials = (if (resolvedDisplayName.isNotBlank()) resolvedDisplayName else work.authorUsername).take(2).uppercase(),
            roleTitle = if (work.postType.equals("video", ignoreCase = true) || work.aspectRatio == "9:16") "Motion Director" else "Visual Creator",
            bio = "Creating procedural shaders and visual studies on ChromaStudio.",
            followers = "${(work.likesCount * 3 + 1200) / 1000}.${((work.likesCount * 3) % 1000) / 100}K",
            following = "342",
            totalRemixes = totalCreatorRemixes,
            isFollowing = isFollowingCreator
        )
        CreatorProfileModal(
            profile = profileInfo,
            creatorPosts = listOf(work),
            onToggleFollow = {
                onToggleFollowCreator?.invoke()
            },
            onViewFullProfile = {
                onAuthorClick(work.creatorId)
            },
            onDismiss = { modalState.showCreatorProfileModal = false }
        )
    }

    if (modalState.showBlockConfirm) {
        AppleDialog(
            title = "Block @${work.authorUsername}?",
            message = "They won't be able to message you or interact with your creations. You can unblock them at any time in Settings.",
            primaryButtonText = "Block User",
            primaryIsDestructive = true,
            onPrimaryClick = { modalState.showBlockConfirm = false },
            secondaryButtonText = "Cancel",
            onSecondaryClick = { modalState.showBlockConfirm = false },
            onDismiss = { modalState.showBlockConfirm = false }
        )
    }

    if (modalState.showDeleteConfirm) {
        AppleDialog(
            title = "Delete Post?",
            message = "Are you sure you want to delete this post? This action cannot be undone.",
            primaryButtonText = "Delete",
            primaryIsDestructive = true,
            onPrimaryClick = {
                modalState.showDeleteConfirm = false
                if (isOwner) {
                    onDeleteWork(work.id)
                }
            },
            secondaryButtonText = "Cancel",
            onSecondaryClick = { modalState.showDeleteConfirm = false },
            onDismiss = { modalState.showDeleteConfirm = false }
        )
    }
}

@Composable
fun FeedPostCard(
    work: DiscoverPost,
    isLiked: Boolean,
    isSaved: Boolean = false,
    isFollowingCreator: Boolean = false,
    isInViewport: Boolean = false,
    isFeedMuted: Boolean = true,
    onToggleFeedMute: (() -> Unit)? = null,
    onToggleFollowCreator: (() -> Unit)? = null,
    onShowGuidelines: (() -> Unit)? = null,
    onShowToast: ((String) -> Unit)? = null,
    repository: WorkRepository,
    onRequireLogin: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onForceLike: (() -> Unit)? = null,
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
    onFlashVideoClick: ((String) -> Unit)? = null,
    onUpdateVisibility: ((hideLikes: Boolean, hideShares: Boolean, hideRemixes: Boolean, hideDownloads: Boolean, hideSaves: Boolean) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isOwner = !currentUserId.isNullOrBlank() && currentUserId == work.creatorId

    val modalState = remember(work.id, isOwner) { FeedPostModalState(isOwner) }

    val doubleTapLikeState = rememberDoubleTapLikeState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val mediaSlides = remember(work.id, work.imageUrls, work.workUrls, work.imageUrl, work.jsonUrl) {
        resolvePostMediaSlides(work)
    }
    val pagerState = rememberPagerState {
        mediaSlides.size.coerceAtLeast(1)
    }

    var resolvedAvatarUrl by remember(work.id) { mutableStateOf(work.authorAvatarUrl) }
    var resolvedDisplayName by remember(work.id) { mutableStateOf(if (work.authorDisplayName.isNotBlank()) work.authorDisplayName else work.authorUsername) }
    var resolvedUsername by remember(work.id) { mutableStateOf(work.authorUsername) }
    var resolvedVerified by remember(work.id) { mutableStateOf(work.authorVerified) }
    // FIX 5: Resolve missing displayName OR username OR avatarUrl from Firestore/FirebaseAuth so posts never show "@ • 1h ago"
    LaunchedEffect(work.creatorId) {
        if ((work.authorAvatarUrl.isBlank() || work.authorDisplayName.isBlank() || work.authorUsername.isBlank()) && work.creatorId.isNotBlank()) {
            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(work.creatorId).get().await()
                val authUser = FirebaseAuth.getInstance().currentUser
                val isCurrentAuthUser = authUser != null && authUser.uid == work.creatorId

                val fetchedAvatar = doc.getString("avatarUrl")
                    ?.takeIf { it.isNotBlank() }
                    ?: if (isCurrentAuthUser) authUser?.photoUrl?.toString().orEmpty() else ""

                val emailPrefix = if (isCurrentAuthUser) authUser?.email?.substringBefore("@").orEmpty() else ""
                val fetchedUsername = doc.getString("username")
                    ?.takeIf { it.isNotBlank() }
                    ?: emailPrefix

                val fetchedDisplay = doc.getString("displayName")
                    ?.takeIf { it.isNotBlank() }
                    ?: if (isCurrentAuthUser) authUser?.displayName.orEmpty() else ""

                if (resolvedAvatarUrl.isBlank() && fetchedAvatar.isNotBlank()) {
                    resolvedAvatarUrl = fetchedAvatar
                }
                if (resolvedUsername.isBlank() && fetchedUsername.isNotBlank()) {
                    resolvedUsername = fetchedUsername
                }
                if (resolvedDisplayName.isBlank()) {
                    resolvedDisplayName = fetchedDisplay.ifBlank { fetchedUsername.ifBlank { "Chroma Creator" } }
                }
                resolvedVerified = doc.getBoolean("verified") ?: resolvedVerified
            } catch (_: Exception) {}
        }
    }

    val isRemixPost = !work.remixedFromAuthorUsername.isNullOrEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (colors.isDark) Color(0xFF16161A) else Color.White)
            .padding(top = 2.dp)
    ) {
        // 1. POST HEADER
        FeedPostHeader(
            displayName = resolvedDisplayName,
            avatarUrl = resolvedAvatarUrl,
            username = resolvedUsername,
            timestamp = work.timestamp,
            verified = resolvedVerified,
            isRemixPost = isRemixPost,
            remixedFromAuthorUsername = work.remixedFromAuthorUsername,
            isOwner = isOwner,
            isFollowingCreator = isFollowingCreator,
            onAvatarClick = { modalState.showCreatorProfileModal = true },
            onRemixTagClick = { modalState.showOriginalPostModal = true },
            onToggleFollowCreator = onToggleFollowCreator,
            onOptionsClick = { modalState.showMenu = true }
        )

        // 2. MEDIA VIEWPORT: Strictly WITHOUT corner radius (RectangleShape)
        val parsedRatio = parseAspectRatio(work.aspectRatio, work.postType)
        val isVertical = parsedRatio <= 9f / 16f || work.aspectRatio.trim() == "9:16" || (isVideoPost(work) && parsedRatio < 0.75f)

        if (isVertical) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidthDp = maxWidth
                val clampedHeightDp = minOf(cardWidthDp * (1f / parsedRatio.coerceAtLeast(0.5f)), 540.dp)
                PostMediaCarousel(
                    work = work,
                    slides = mediaSlides,
                    pagerState = pagerState,
                    repository = repository,
                    colors = colors,
                    doubleTapLikeState = doubleTapLikeState,
                    onVideoClick = if (isVideoPost(work) || mediaSlides.any { it is PostMediaSlide.VideoSlide }) { { onFlashVideoClick?.invoke(work.id) } } else null,
                    isInViewport = isInViewport,
                    isFeedMuted = isFeedMuted,
                    onToggleFeedMute = onToggleFeedMute,
                    onDoubleTap = { offset ->
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid.isNullOrBlank()) {
                            onRequireLogin()
                        } else {
                            if (onForceLike != null) {
                                onForceLike()
                            } else if (!isLiked) {
                                onLikeClick()
                            }
                        }
                        doubleTapLikeState.trigger(offset)
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
                onVideoClick = if (isVideoPost(work) || mediaSlides.any { it is PostMediaSlide.VideoSlide }) { { onFlashVideoClick?.invoke(work.id) } } else null,
                isInViewport = isInViewport,
                isFeedMuted = isFeedMuted,
                onToggleFeedMute = onToggleFeedMute,
                onDoubleTap = { offset ->
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid.isNullOrBlank()) {
                        onRequireLogin()
                    } else {
                        if (onForceLike != null) {
                            onForceLike()
                        } else if (!isLiked) {
                            onLikeClick()
                        }
                    }
                    doubleTapLikeState.trigger(offset)
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(parsedRatio)
            )
        }

        // 3. ACTION BUTTONS ROW
        FeedPostActionRow(
            work = work,
            isLiked = isLiked,
            isSaved = isSaved,
            currentUserId = currentUserId,
            onRequireLogin = onRequireLogin,
            onLikeClick = onLikeClick,
            onRemixClick = onRemixClick,
            onRemixWork = onRemixWork,
            onDownloadClick = onDownloadClick,
            onSaveClick = onSaveClick,
            onShareClick = onShareClick
        )

        // 4. METADATA: Title, Description, Tags, Mentions
        FeedPostMetadata(
            work = work,
            onTagClick = onTagClick,
            onMentionClick = onMentionClick,
            onAuthorClick = onAuthorClick
        )

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = if (colors.isDark) Color(0xFF24242A) else Color(0xFFE5E5EA)
        )
    }

    // MODALS & DIALOGS
    FeedPostModals(
        work = work,
        isOwner = isOwner,
        modalState = modalState,
        resolvedDisplayName = resolvedDisplayName.ifBlank { resolvedUsername.ifBlank { "Chroma Creator" } },
        resolvedAvatarUrl = resolvedAvatarUrl,
        resolvedVerified = resolvedVerified,
        isFollowingCreator = isFollowingCreator,
        onToggleFollowCreator = onToggleFollowCreator,
        onAuthorClick = onAuthorClick,
        onReportClick = onReportClick,
        onArchiveWork = onArchiveWork,
        onDeleteWork = onDeleteWork,
        onEditWork = onEditWork,
        onShowGuidelines = onShowGuidelines,
        onShowToast = onShowToast,
        onNavigateToOriginal = onNavigateToOriginal,
        onRemixWork = onRemixWork,
        onUpdateVisibility = onUpdateVisibility
    )
}

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
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (colors.isDark) Color.White else Color(0xFF0F172A))
                        .clickable { onDismiss() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Close Telemetry",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (colors.isDark) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

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
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF007AFF) else (if (colors.isDark) Color(0xFF252528) else Color(0xFFF8FAFC)))
                                .border(1.dp, if (isSelected) Color(0xFF007AFF) else colors.glassBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedRatio = ratio }
                                .padding(vertical = 7.dp),
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF1F5F9))
                            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                            .clickable { onDismiss() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF007AFF))
                            .clickable { onSaveEdits(selectedRatio, titleText, descText) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save Changes", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

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
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Lucide.X, contentDescription = "Close Profile", tint = colors.textMain, modifier = Modifier.size(18.dp))
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (profile.isFollowing) colors.glassBgHover else colors.primary)
                            .border(0.5.dp, if (profile.isFollowing) colors.glassBorder else Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.glassBgHover)
                            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(10.dp))
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
    onDeletePost: () -> Unit,
    onOpenGuidelines: (() -> Unit)? = null,
    onShowToast: ((String) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val clipboardManager = LocalClipboardManager.current

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
                    .clip(RoundedCornerShape(2.dp))
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOwner) "Manage Post" else if (work.authorDisplayName.isNotBlank()) "${work.authorDisplayName} (@${work.authorUsername})" else "@${work.authorUsername.ifBlank { "creator" }}",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isOwner) "Your post" else "Post options",
                        fontSize = 11.5.sp,
                        color = if (isOwner) Color(0xFF007AFF) else Color(0xFF8E8E93),
                        fontWeight = if (isOwner) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                // Top Corner "X" Close Button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Close",
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)

            if (!isOwner) {
                DrawerActionItem(
                    icon = Lucide.Copy,
                    title = "Copy Post Link",
                    subtitle = "Share link to this artwork with other creators",
                    onClick = {
                        clipboardManager.setText(AnnotatedString("https://oscellion.vercel.app/post/${work.id}"))
                        onShowToast?.invoke("Post link copied to clipboard")
                        onDismiss()
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))

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
                    icon = Lucide.ShieldCheck,
                    title = "Community Standards & Guidelines",
                    subtitle = "Remix rights, procedural shader licensing & safety code",
                    onClick = {
                        onDismiss()
                        onOpenGuidelines?.invoke()
                    }
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
                DrawerActionItem(
                    icon = Lucide.Copy,
                    title = "Copy Post Link",
                    subtitle = "Share link to this artwork with other creators",
                    onClick = {
                        clipboardManager.setText(AnnotatedString("https://oscellion.vercel.app/post/${work.id}"))
                        onShowToast?.invoke("Post link copied to clipboard")
                        onDismiss()
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder.copy(alpha = 0.5f))

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
                    icon = Lucide.ShieldCheck,
                    title = "Community Standards & Guidelines",
                    subtitle = "Remix rights, procedural shader licensing & safety code",
                    onClick = {
                        onDismiss()
                        onOpenGuidelines?.invoke()
                    }
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
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = effectiveTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (tint != colors.textMain) tint else colors.textMain
            )
            Spacer(Modifier.height(2.5.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                    .clip(RoundedCornerShape(2.dp))
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
                        text = "This creation was remixed by @${work.authorUsername.ifBlank { "creator" }} using the procedural layer assets and shader parameters from @$origUsername's master artwork.",
                        fontSize = 12.sp,
                        color = colors.textMain.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF007AFF))
                    .clickable {
                        onDismiss()
                        if (origAuthorId.isNotBlank()) {
                            onNavigateToOriginal(origAuthorId, origPostId)
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("View Original Master Work", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.glassBgHover)
                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                    .clickable {
                        onDismiss()
                        onRemixWork?.invoke(work)
                    }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
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

            HorizontalDivider(thickness = 0.5.dp, color = colors.glassBorder)
            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
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

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF007AFF))
                    .clickable {
                        onSave(hideLikes, hideShares, hideRemixes, hideDownloads, hideSaves)
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Apply Settings",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(16.dp))
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

@Composable
fun CommunityUploadProgressBanner(
    task: FlashUploadTask,
    onRetry: () -> Unit,
    onViewDrafts: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (colors.isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = 1.dp,
            color = when (task.status) {
                UploadStatus.FAILED -> Color(0xFFEF4444).copy(alpha = 0.45f)
                UploadStatus.SUCCESS -> Color(0xFF10B981).copy(alpha = 0.45f)
                else -> if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (colors.isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                        .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.thumbnailBitmap != null) {
                        Image(
                            bitmap = task.thumbnailBitmap.asImageBitmap(),
                            contentDescription = "Upload Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Lucide.Video,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    when (task.status) {
                        UploadStatus.SUCCESS -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        UploadStatus.FAILED -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFEF4444).copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        else -> {}
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (task.status) {
                        UploadStatus.UPLOADING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (task.title.isNotBlank()) task.title else "Posting video…",
                                    color = colors.textMain,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${(task.progress * 100).toInt()}%",
                                    color = Color(0xFF2563EB),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LiquidGlassProgressBar(
                                progress = task.progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                height = 4.dp
                            )

                            Text(
                                text = task.statusText,
                                color = colors.textMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        UploadStatus.SUCCESS -> {
                            Text(
                                text = "Posted! 🎉",
                                color = if (colors.isDark) Color(0xFF34D399) else Color(0xFF059669),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your video is now live in the Community feed",
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        }

                        UploadStatus.FAILED -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Uploading failed",
                                    color = Color(0xFFEF4444),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = "Dismiss",
                                    tint = colors.textMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onDismiss() }
                                )
                            }
                            Text(
                                text = "Uploading failed because of \"${task.errorMessage ?: "Unknown error"}\"",
                                color = if (colors.isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        UploadStatus.IDLE -> {}
                    }
                }
            }

            if (task.status == UploadStatus.FAILED) {
                HorizontalDivider(
                    color = if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.RotateCw,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Re-Upload",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = onViewDrafts,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textMain),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.FileText,
                            contentDescription = null,
                            tint = colors.textMain,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "View Saved Drafts",
                            color = colors.textMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}