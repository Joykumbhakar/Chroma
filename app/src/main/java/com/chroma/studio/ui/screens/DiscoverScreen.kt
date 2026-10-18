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
            // Standard Cupertino Frosted Glass Top Navigation Header
            ChromaScreenHeader(
                title = "Oscellion",
                badgeText = "Community",
                badgeColor = Color(0xFF007AFF),
                isSearchExpanded = isSearchExpanded,
                searchContent = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassBgHover)
                            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Lucide.Search,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search creations, tags, creators...",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = colors.textMain,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF007AFF)),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8E8E93).copy(alpha = 0.5f))
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Lucide.X,
                                    contentDescription = "Clear",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = "Cancel",
                            color = Color(0xFF007AFF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                isSearchExpanded = false
                                searchQuery = ""
                            }
                        )
                    }
                },
                actions = {
                    // Create Action Button with Apple Liquid Glass Menu
                    Box {
                        ChromaHeaderActionButton(
                            icon = Lucide.Plus,
                            contentDescription = "Create",
                            onClick = { showCreateMenu = true }
                        )

                        DropdownMenu(
                            expanded = showCreateMenu,
                            onDismissRequest = { showCreateMenu = false },
                            modifier = Modifier
                                .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.15f))
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (colors.isDark) Color(0xFF1E1E20) else Color(0xFFFAFAFA))
                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Image Post", color = colors.textMain, fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                                onClick = {
                                    showCreateMenu = false
                                    onCreatePost()
                                },
                                leadingIcon = { Icon(Lucide.Image, null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp)) }
                            )
                            HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp)
                            DropdownMenuItem(
                                text = { Text("Publish Canvas Work", color = colors.textMain, fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                                onClick = {
                                    showCreateMenu = false
                                    onPublishWork()
                                },
                                leadingIcon = { Icon(Lucide.Briefcase, null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp)) }
                            )
                            HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp)
                            DropdownMenuItem(
                                text = { Text("Add Story Moment", color = colors.textMain, fontWeight = FontWeight.Medium, fontSize = 14.sp) },
                                onClick = {
                                    showCreateMenu = false
                                    mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                leadingIcon = { Icon(Lucide.Camera, null, tint = Color(0xFF32ADE6), modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }

                    // Search Trigger
                    ChromaHeaderActionButton(
                        icon = Lucide.Search,
                        contentDescription = "Search",
                        onClick = { isSearchExpanded = true }
                    )

                    // Notifications / Activity
                    ChromaHeaderActionButton(
                        icon = Lucide.Heart,
                        contentDescription = "Activity",
                        onClick = { onNavigateToNotifications() }
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
                    // Stories & Moments Tray
                    item(key = "stories") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp)
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
                        }
                        if (isUploadingStory) {
                            LiquidGlassProgressBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .padding(horizontal = 16.dp, vertical = 2.dp)
                            )
                        }
                        HorizontalDivider(
                            color = colors.glassBorder.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }

                    // Trending Section (Vision Pro Spatial Glass Carousel)
                    if (isTrendingLoading || trendingWorks.isNotEmpty()) {
                        item(key = "trending_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Lucide.Flame, null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp))
                                    Text(
                                        "Trending Creations",
                                        color = colors.textMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        letterSpacing = (-0.3).sp
                                    )
                                }
                                if (trendingWorks.isNotEmpty()) {
                                    Text(
                                        "${trendingWorks.size} Top Artworks",
                                        color = Color(0xFF8E8E93),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
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
                                                .width(160.dp)
                                                .height(126.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(colors.glassBgHover.copy(alpha = 0.6f))
                                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(18.dp))
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
                        item(key = "trending_divider") {
                            HorizontalDivider(
                                color = if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                                thickness = 0.5.dp
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
                                onRemixWork = { onRemixWork(work) },
                                onDeleteWork = { onDeleteWork(work.id) },
                                onEditWork = { workId, title, desc -> onEditWork(workId, title, desc) },
                                onNavigateToOriginal = { authorId, postId -> onAuthorClick(authorId, postId) },
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
                                        AiWaveSphereIcon(modifier = Modifier.size(16.dp))
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
            .width(160.dp)
            .height(126.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (colors.isDark) Color(0x33FFFFFF) else Color(0x18000000)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                val label = if (layers != null && layers!!.isNotEmpty()) "✨ Vector" else "🖼️ Art"
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
    val isOwner = currentUserId != null && currentUserId == work.creatorId
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showVisibilitySettings by remember { mutableStateOf(false) }
    var editTitle by remember(work.id) { mutableStateOf(work.title) }
    var editDescription by remember(work.id) { mutableStateOf(work.description) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    val doubleTapLikeState = rememberDoubleTapLikeState()

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
            .padding(top = 4.dp)
    ) {
        // Header Row (Avatar + Name + Handle + Time + Menu)
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
                                fontSize = 14.5.sp,
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
                                fontSize = 11.5.sp
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
                                IosActionSheetItem("Share Creation", false) { showMenu = false; onShareClick?.invoke() },
                                IosActionSheetItem("Remix Artwork", false) {
                                    showMenu = false
                                    if (currentUserId == null) onRequireLogin()
                                    else if (onRemixWork != null) onRemixWork(work)
                                    else onRemixClick()
                                },
                                IosActionSheetItem("Save to Studio Library", false) {
                                    showMenu = false
                                    if (currentUserId == null) onRequireLogin()
                                    else onDownloadClick()
                                },
                                IosActionSheetItem("Save / Bookmark", false) { showMenu = false; onSaveClick?.invoke() }
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x18007AFF))
                        .border(0.5.dp, Color(0x33007AFF), RoundedCornerShape(10.dp))
                        .clickable {
                            val origAuthorId = work.remixedFromAuthorId
                            val origPostId = work.remixedFromPostId
                            if (!origAuthorId.isNullOrBlank() && !origPostId.isNullOrBlank()) {
                                onNavigateToOriginal(origAuthorId, origPostId)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Lucide.HeartHandshake, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(15.dp))
                        Text(
                            text = "Remixed from @${work.remixedFromAuthorUsername}",
                            color = Color(0xFF007AFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Media Viewport - Contained Picture-Frame with Rounded Corners and Double-Tap Heart Animation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (colors.isDark) Color(0xFF101012) else Color(0xFFE5E5EA))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                if (!uid.isNullOrBlank()) {
                                    if (!isLiked) onLikeClick()
                                } else {
                                    onRequireLogin()
                                }
                                doubleTapLikeState.trigger(offset)
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

                // Top-Left Floating Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    val label = if (layers != null && layers!!.isNotEmpty()) "✨ Vector Mesh" else "🖼️ Artwork"
                    Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Top-Right Floating Badge (Layers count)
                if (layers != null && layers!!.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("${layers!!.size} Layers", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Flat, smooth, clean double tap heart reaction
                FlatDoubleTapHeartOverlay(
                    state = doubleTapLikeState,
                    modifier = Modifier.fillMaxSize()
                )
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
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                if (uid.isNullOrBlank()) onRequireLogin()
                                else onLikeClick()
                            }
                            .padding(4.dp)
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

                    // Remix Action with accurate count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (currentUserId == null) onRequireLogin()
                                else if (onRemixWork != null) onRemixWork(work)
                                else onRemixClick()
                            }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.HeartHandshake,
                            contentDescription = "Remix",
                            tint = colors.textMain,
                            modifier = Modifier.size(22.dp)
                        )
                        if (!work.hideRemixesCount && work.remixCount > 0) {
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "${work.remixCount}",
                                color = colors.textMain.copy(alpha = 0.75f),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Download / Save to Studio Library Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (currentUserId == null) onRequireLogin()
                                else onDownloadClick()
                            }
                            .padding(4.dp)
                    ) {
                        Icon(
                            Lucide.Download,
                            contentDescription = "Save to Studio",
                            tint = colors.textMain,
                            modifier = Modifier.size(21.dp)
                        )
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
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSaveClick?.invoke() }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Lucide.BookmarkCheck else Lucide.Bookmark,
                            contentDescription = "Bookmark",
                            tint = if (isSaved) Color(0xFF007AFF) else colors.textMain,
                            modifier = Modifier.size(21.dp)
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
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onShareClick?.invoke() }
                            .padding(4.dp)
                    ) {
                        Icon(
                            Lucide.Share2,
                            contentDescription = "Share",
                            tint = colors.textMain,
                            modifier = Modifier.size(20.dp)
                        )
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
                    .padding(bottom = 14.dp)
            ) {
                if (work.title.isNotBlank()) {
                    Text(
                        text = work.title,
                        color = colors.textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        letterSpacing = (-0.2).sp
                    )
                }

                if (work.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    val isLong = work.description.length > 90
                    val displayDesc = if (isLong && !isDescriptionExpanded) work.description.take(90).trimEnd() + "..." else work.description

                    Text(
                        text = buildAnnotatedString {
                            append(displayDesc)
                            if (isLong && !isDescriptionExpanded) {
                                append(" ")
                                withStyle(SpanStyle(color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)) {
                                    append("more")
                                }
                            }
                        },
                        color = colors.textMain.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.clickable(enabled = isLong) {
                            isDescriptionExpanded = !isDescriptionExpanded
                        }
                    )
                }

                if (work.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(work.tags) { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF007AFF).copy(alpha = 0.10f))
                                    .border(0.5.dp, Color(0xFF007AFF).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .clickable { onTagClick?.invoke(tag) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "#$tag",
                                    color = Color(0xFF007AFF),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // HR Line (Horizontal Rule Divider separating posts)
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
            )
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
                        variant = ChromaPillVariant.Secondary,
                        height = 36.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Cancel", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                .padding(horizontal = 14.dp)
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(16.dp))
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
