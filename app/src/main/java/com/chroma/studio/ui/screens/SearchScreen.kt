@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.chroma.studio.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.R
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.SearchHistoryManager
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.DiscoverViewModel
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SearchScope(val label: String) {
    ALL("All"),
    CREATORS("Creators"),
    ARTWORKS("Artworks"),
    TAGS("Tags")
}

private val EXPLORE_CATEGORIES = listOf(
    "All",
    "Trending",
    "Gradients",
    "Photos",
    "3D Meshes",
    "Glassmorphism",
    "Minimal",
    "Cyberpunk",
    "Aurora"
)

@Composable
fun SearchScreen(
    repository: WorkRepository? = null,
    discoverViewModel: DiscoverViewModel? = null,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPost: ((creatorId: String, postId: String) -> Unit)? = null,
    onRemixWork: ((DiscoverPost) -> Unit)? = null,
    onBack: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val discoverVm = discoverViewModel ?: viewModel<DiscoverViewModel>()
    val context = LocalContext.current
    val historyManager = remember { SearchHistoryManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedScope by remember { mutableStateOf(SearchScope.ALL) }

    var userResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isSearchingUsers by remember { mutableStateOf(false) }
    var searchHistory by remember { mutableStateOf(historyManager.getSearchHistory()) }

    val allWorks by discoverVm.works.collectAsState()
    val trendingWorks by discoverVm.trendingWorks.collectAsState()
    val isWorksLoading by discoverVm.isLoading.collectAsState()

    var selectedPreviewPost by remember { mutableStateOf<DiscoverPost?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Initial load
    LaunchedEffect(Unit) {
        if (allWorks.isEmpty()) {
            discoverVm.loadWorks()
        }
        if (trendingWorks.isEmpty()) {
            discoverVm.loadTrending()
        }
    }

    // Debounced User Search
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            userResults = emptyList()
            isSearchingUsers = false
            return@LaunchedEffect
        }
        isSearchingUsers = true
        delay(200)
        try {
            userResults = withContext(Dispatchers.IO) {
                discoverVm.repository.searchUsers(q)
            }
        } catch (_: Exception) {
            userResults = emptyList()
        } finally {
            isSearchingUsers = false
        }
    }

    // Filtered Artworks by Query & Category
    val filteredArtworks = remember(allWorks, trendingWorks, searchQuery, selectedCategory) {
        val baseList = when (selectedCategory) {
            "Trending" -> if (trendingWorks.isNotEmpty()) trendingWorks else allWorks
            "Gradients" -> allWorks.filter { it.postType == "work" }
            "Photos" -> allWorks.filter { it.postType == "image" }
            "3D Meshes" -> allWorks.filter { it.tags.any { t -> t.contains("mesh", true) || t.contains("3d", true) } }
            "Glassmorphism" -> allWorks.filter { it.tags.any { t -> t.contains("glass", true) } }
            "Minimal" -> allWorks.filter { it.tags.any { t -> t.contains("minimal", true) } }
            "Cyberpunk" -> allWorks.filter { it.tags.any { t -> t.contains("cyber", true) } }
            "Aurora" -> allWorks.filter { it.tags.any { t -> t.contains("aurora", true) } }
            else -> allWorks
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().removePrefix("#")
            allWorks.filter {
                it.title.contains(q, ignoreCase = true) ||
                it.description.contains(q, ignoreCase = true) ||
                it.authorUsername.contains(q, ignoreCase = true) ||
                it.authorDisplayName.contains(q, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(q, ignoreCase = true) }
            }
        }
    }

    // Matching Community Tags from all posts
    val matchingTags = remember(searchQuery, allWorks) {
        val uniqueTags = allWorks.flatMap { it.tags }.filter { it.isNotBlank() }.distinct()
        if (searchQuery.isBlank()) {
            uniqueTags.take(15)
        } else {
            val q = searchQuery.trim().removePrefix("#").lowercase()
            uniqueTags.filter { it.lowercase().contains(q) }.take(15)
        }
    }

    val isSearchActive = searchQuery.isNotBlank()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    fun handleUserClick(user: UserProfile) {
        historyManager.addSearch(user)
        searchHistory = historyManager.getSearchHistory()
        onNavigateToProfile(user.uid)
    }

    fun handleRefresh() {
        coroutineScope.launch {
            isRefreshing = true
            discoverVm.loadWorks()
            discoverVm.loadTrending()
            delay(600)
            isRefreshing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Header Bar (Apple Large Title) ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg.copy(alpha = 0.95f))
                    .padding(top = statusBarTop + 8.dp, bottom = 8.dp, start = 16.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isSearchActive) "Search" else "Explore",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = InterFontFamily,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = if (isSearchActive) "Find creators, styles & artworks" else "Discover inspiring gradients & art",
                        fontSize = 12.5.sp,
                        color = Color(0xFF8E8E93),
                        fontFamily = InterFontFamily
                    )
                }

                IconButton(
                    onClick = { handleRefresh() },
                    modifier = Modifier.size(38.dp)
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isRefreshing) 360f else 0f,
                        animationSpec = if (isRefreshing) infiniteRepeatable(tween(800, easing = LinearEasing)) else tween(300),
                        label = "rot"
                    )
                    Icon(
                        imageVector = Lucide.RotateCw,
                        contentDescription = "Refresh",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Main Staggered Grid Content ───────────────────────────────────
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp, start = 14.dp, end = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 12.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                // ── 1. Search Bar (Cupertino Style) ───────────────────────────
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA))
                            .border(
                                width = 0.5.dp,
                                color = if (isDark) Color(0x33FFFFFF) else Color(0x14000000),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Lucide.Search,
                                contentDescription = null,
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search creators, #tags, artworks...",
                                        color = Color(0xFF8E8E93),
                                        fontSize = 15.sp,
                                        fontFamily = InterFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = colors.textMain,
                                        fontSize = 15.sp,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF007AFF)),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8E8E93).copy(alpha = 0.5f))
                                        .clickable {
                                            searchQuery = ""
                                            focusManager.clearFocus()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.X,
                                        contentDescription = "Clear",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 2A. In Search Mode: Scope Bar (All / Creators / Artworks / Tags) ──
                if (isSearchActive) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchScope.values().forEach { scope ->
                                val isSelected = scope == selectedScope
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) Color(0xFF007AFF)
                                            else if (isDark) Color(0xFF1C1C1E)
                                            else Color(0xFFE5E5EA)
                                        )
                                        .clickable { selectedScope = scope },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = scope.label,
                                        color = if (isSelected) Color.White else colors.textMain,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }
                        }
                    }

                    // ── Search Mode: Creators Section ────────────────────────
                    if (selectedScope == SearchScope.ALL || selectedScope == SearchScope.CREATORS) {
                        if (isSearchingUsers) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF007AFF),
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        } else if (userResults.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = "Creators (${userResults.size})",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = InterFontFamily,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(
                                items = userResults,
                                key = { "search_user_${it.uid}" },
                                span = { StaggeredGridItemSpan.FullLine }
                            ) { user ->
                                CreatorSearchResultCard(
                                    user = user,
                                    colors = colors,
                                    onClick = { handleUserClick(user) }
                                )
                            }
                        } else if (selectedScope == SearchScope.CREATORS && !isSearchingUsers) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                AppleEmptyTabState(
                                    icon = Lucide.Users,
                                    title = "No Creators Found",
                                    subtitle = "No creator matched \"$searchQuery\""
                                )
                            }
                        }
                    }

                    // ── Search Mode: Matching Tags ────────────────────────────
                    if ((selectedScope == SearchScope.ALL || selectedScope == SearchScope.TAGS) && matchingTags.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text(
                                    text = "Tags & Topics",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = InterFontFamily
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(matchingTags) { tag ->
                                        val cleanTag = tag.removePrefix("#")
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                                                .border(0.5.dp, Color(0xFF007AFF).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                                .clickable { searchQuery = cleanTag }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "#$cleanTag",
                                                color = Color(0xFF007AFF),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = InterFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Search Mode: Artworks Header ─────────────────────────
                    if (selectedScope == SearchScope.ALL || selectedScope == SearchScope.ARTWORKS) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Artworks (${filteredArtworks.size})",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }

                        if (filteredArtworks.isEmpty() && !isSearchingUsers && userResults.isEmpty() && selectedScope == SearchScope.ALL) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                AppleEmptyTabState(
                                    icon = Lucide.SearchX,
                                    title = "No Results Found",
                                    subtitle = "We couldn't find anything matching \"$searchQuery\""
                                )
                            }
                        } else if (filteredArtworks.isEmpty() && selectedScope == SearchScope.ARTWORKS) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                AppleEmptyTabState(
                                    icon = Lucide.Palette,
                                    title = "No Artworks Found",
                                    subtitle = "Try searching for different keywords or styles."
                                )
                            }
                        } else {
                            items(filteredArtworks, key = { "search_art_${it.id}" }) { work ->
                                val aspectRatio = when (work.id.hashCode() % 3) {
                                    0 -> 0.85f
                                    1 -> 1.0f
                                    else -> 1.25f
                                }
                                ExploreWorkGridCard(
                                    work = work,
                                    aspectRatio = aspectRatio,
                                    repository = repository,
                                    onClick = { selectedPreviewPost = work }
                                )
                            }
                        }
                    }
                } else {
                    // ── 2B. In Explore Mode: Category Filter Pills ─────────────
                    item(span = StaggeredGridItemSpan.FullLine) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            items(EXPLORE_CATEGORIES) { filter ->
                                val isSelected = filter == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF007AFF)
                                            else if (isDark) Color(0xFF1C1C1E)
                                            else Color(0xFFE5E5EA)
                                        )
                                        .border(
                                            width = if (isSelected) 0.dp else 0.5.dp,
                                            color = if (isSelected) Color.Transparent else colors.glassBorder,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedCategory = filter }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = filter,
                                        color = if (isSelected) Color.White else colors.textMain,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }
                        }
                    }

                    // ── Explore Mode: Recent Searches Row ────────────────────
                    if (searchHistory.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent Searches",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        fontFamily = InterFontFamily
                                    )
                                    Text(
                                        text = "Clear All",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF007AFF),
                                        fontFamily = InterFontFamily,
                                        modifier = Modifier.clickable {
                                            historyManager.clearHistory()
                                            searchHistory = emptyList()
                                        }
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(searchHistory, key = { "hist_${it.uid}" }) { recentUser ->
                                        Row(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA))
                                                .border(0.5.dp, colors.glassBorder, CircleShape)
                                                .clickable { handleUserClick(recentUser) }
                                                .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            IosAvatarView(
                                                name = recentUser.displayName.ifBlank { recentUser.username },
                                                avatarUrl = recentUser.avatarUrl,
                                                size = 24.dp
                                            )
                                            Text(
                                                text = recentUser.displayName.ifBlank { "@${recentUser.username}" },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textMain,
                                                fontFamily = InterFontFamily
                                            )
                                            Icon(
                                                imageVector = Lucide.X,
                                                contentDescription = "Remove",
                                                tint = Color(0xFF8E8E93),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        historyManager.removeSearch(recentUser.uid)
                                                        searchHistory = historyManager.getSearchHistory()
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Explore Mode: Spotlight Featured Creator Banner ────────
                    if (trendingWorks.isNotEmpty() && selectedCategory == "All") {
                        val topPost = trendingWorks.first()
                        item(span = StaggeredGridItemSpan.FullLine) {
                            SpotlightCreatorBanner(
                                post = topPost,
                                colors = colors,
                                onClick = {
                                    handleUserClick(
                                        UserProfile(
                                            uid = topPost.creatorId,
                                            username = topPost.authorUsername,
                                            displayName = topPost.authorDisplayName,
                                            avatarUrl = topPost.authorAvatarUrl,
                                            verified = topPost.authorVerified
                                        )
                                    )
                                }
                            )
                        }
                    }

                    // ── Explore Mode: Community Trending Tags ─────────────────
                    if (matchingTags.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                Text(
                                    text = "Trending Tags",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = InterFontFamily
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(matchingTags) { tag ->
                                        val cleanTag = tag.removePrefix("#")
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA))
                                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                                .clickable { searchQuery = cleanTag }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("#", color = Color(0xFF007AFF), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                            Spacer(Modifier.width(3.dp))
                                            Text(cleanTag, color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Explore Mode: Creations Header ────────────────────────
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedCategory == "All") "Explore Creations" else "$selectedCategory Creations",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = InterFontFamily
                            )
                            Text(
                                text = "${filteredArtworks.size} items",
                                fontSize = 12.5.sp,
                                color = Color(0xFF8E8E93),
                                fontFamily = InterFontFamily
                            )
                        }
                    }

                    // ── Explore Mode: Staggered Grid Items ───────────────────
                    if (isWorksLoading && filteredArtworks.isEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            ExploreSkeletonGrid()
                        }
                    } else if (filteredArtworks.isEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            AppleEmptyTabState(
                                icon = Lucide.Palette,
                                title = "No Creations Here",
                                subtitle = "No creations found in category \"$selectedCategory\"."
                            )
                        }
                    } else {
                        items(filteredArtworks, key = { "explore_work_${it.id}" }) { work ->
                            val aspectRatio = when (work.id.hashCode() % 3) {
                                0 -> 0.85f
                                1 -> 1.0f
                                else -> 1.25f
                            }

                            ExploreWorkGridCard(
                                work = work,
                                aspectRatio = aspectRatio,
                                repository = repository,
                                onClick = { selectedPreviewPost = work }
                            )
                        }
                    }
                }
            }
        }

        // ── Artwork Quick Look & Action Sheet Modal ────────────────────────────
        if (selectedPreviewPost != null) {
            val post = selectedPreviewPost!!
            ExploreQuickLookSheet(
                post = post,
                repository = repository,
                onDismiss = { selectedPreviewPost = null },
                onNavigateToProfile = {
                    val uid = post.creatorId
                    selectedPreviewPost = null
                    onNavigateToProfile(uid)
                },
                onNavigateToPost = {
                    val creatorId = post.creatorId
                    val postId = post.id
                    selectedPreviewPost = null
                    onNavigateToPost?.invoke(creatorId, postId)
                },
                onRemix = {
                    val workToRemix = post
                    selectedPreviewPost = null
                    onRemixWork?.invoke(workToRemix)
                }
            )
        }
    }
}

// ── Creator Search Result Card ────────────────────────────────────────────────
@Composable
private fun CreatorSearchResultCard(
    user: UserProfile,
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.glassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                IosAvatarView(name = user.displayName, avatarUrl = user.avatarUrl, size = 44.dp)

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName.ifBlank { user.username },
                            color = colors.textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (user.verified) {
                            Spacer(Modifier.width(4.dp))
                            Image(
                                painter = painterResource(id = R.drawable.verification_badge),
                                contentDescription = "Verified",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "@${user.username}",
                        color = Color(0xFF8E8E93),
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily
                    )
                    if (user.bio.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = user.bio,
                            color = colors.textMain.copy(alpha = 0.7f),
                            fontSize = 11.5.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF007AFF))
                    .clickable { onClick() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "View",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

// ── Spotlight Creator Banner ──────────────────────────────────────────────────
@Composable
private fun SpotlightCreatorBanner(
    post: DiscoverPost,
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF007AFF).copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF007AFF).copy(alpha = 0.12f), Color(0xFF5856D6).copy(alpha = 0.06f), Color.Transparent)
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IosAvatarView(
                        name = post.authorDisplayName,
                        avatarUrl = post.authorAvatarUrl,
                        size = 46.dp
                    )

                    Column {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFF9500).copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "FEATURED CREATOR",
                                color = Color(0xFFFF9500),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = InterFontFamily
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorDisplayName,
                                color = colors.textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                fontFamily = InterFontFamily
                            )
                            if (post.authorVerified) {
                                Spacer(Modifier.width(4.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.verification_badge),
                                    contentDescription = "Verified",
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text(
                            text = "@${post.authorUsername}",
                            color = Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Profile", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Icon(Lucide.ArrowRight, null, tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                }
            }
        }
    }
}

// ── Explore Quick Look & Action Sheet Modal ────────────────────────────────────
@Composable
private fun ExploreQuickLookSheet(
    post: DiscoverPost,
    repository: WorkRepository?,
    onDismiss: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPost: () -> Unit,
    onRemix: () -> Unit
) {
    val colors = LocalOscellionColors.current
    var layers by remember(post.jsonUrl) { mutableStateOf<List<GradientLayer>?>(LayerCache.get(post.jsonUrl)) }

    LaunchedEffect(post.jsonUrl) {
        if (layers == null && post.jsonUrl.isNotBlank() && repository != null) {
            layers = withContext(Dispatchers.IO) {
                val parsed = repository.deserializeLayers(post.jsonUrl)
                LayerCache.put(post.jsonUrl, parsed)
                parsed
            }
        }
    }

    val cleanTitle = remember(post.title) {
        var t = post.title.trim()
        if (t.contains("(Imported)", ignoreCase = true)) {
            t = t.replace(Regex("(?i)(\\s*\\(imported\\))+"), "").trim()
            if (t.isEmpty()) "Untitled Artwork" else "$t (Imported)"
        } else if (t.isEmpty()) {
            "Untitled Artwork"
        } else {
            t
        }
    }

    GlassDialog(onDismissRequest = onDismiss, cornerRadius = 26) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Author details & circular close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateToProfile() }
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                ) {
                    IosAvatarView(name = post.authorDisplayName, avatarUrl = post.authorAvatarUrl, size = 40.dp)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorDisplayName.ifBlank { "Creator" },
                                color = colors.textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = InterFontFamily
                            )
                            if (post.authorVerified) {
                                Spacer(Modifier.width(4.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.verification_badge),
                                    contentDescription = "Verified",
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "@${post.authorUsername.ifBlank { "creator" }}",
                                color = Color(0xFF8E8E93),
                                fontSize = 12.sp,
                                fontFamily = InterFontFamily
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF007AFF).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("Artist", color = Color(0xFF007AFF), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }

                // Apple Circular Close Button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA).copy(alpha = 0.85f),
                            CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Lucide.X,
                        contentDescription = "Close",
                        tint = colors.textMain.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Large Media Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(androidx.compose.ui.graphics.RectangleShape)
                    .border(0.5.dp, Color.Black.copy(alpha = 0.08f), androidx.compose.ui.graphics.RectangleShape)
                    .background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center
            ) {
                if (post.postType == "image" && !post.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(post.imageUrl).crossfade(true).build(),
                        contentDescription = post.title,
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

                // Top-Left Format Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    val label = if (layers != null && layers!!.isNotEmpty()) "✨ Vector Mesh" else "🖼️ Artwork"
                    Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                }

                // Top-Right Layer Pill if available
                if (layers != null && layers!!.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("${layers!!.size} Layers", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Title & Description
            Text(
                text = cleanTitle,
                color = colors.textMain,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                fontFamily = InterFontFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (post.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = post.description,
                    color = colors.textMain.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }

            // Stats row
            if ((!post.hideLikesCount && post.likesCount > 0) || (!post.hideRemixesCount && post.remixCount > 0)) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!post.hideLikesCount && post.likesCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Lucide.Heart, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(13.dp))
                            Text("${post.likesCount}", color = colors.textMain.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                        }
                    }
                    if (!post.hideRemixesCount && post.remixCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Lucide.Layers, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(13.dp))
                            Text("${post.remixCount} remixes", color = colors.textMain.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Remix in Studio Button
                Button(
                    onClick = onRemix,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.15f)
                        .height(46.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Lucide.Sparkles, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Remix in Studio",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // View Post Button
                OutlinedButton(
                    onClick = onNavigateToPost,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (colors.isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6)
                    ),
                    modifier = Modifier
                        .weight(0.95f)
                        .height(46.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Lucide.Globe, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "View Post",
                            color = colors.textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ── Skeleton Loader for Explore Grid ──────────────────────────────────────────
@Composable
private fun ExploreSkeletonGrid() {
    val infiniteTransition = rememberInfiniteTransition(label = "sk_explore")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sk_alpha"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp)
                        .clip(androidx.compose.ui.graphics.RectangleShape)
                        .background(Color.Gray.copy(alpha = alpha))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp)
                        .clip(androidx.compose.ui.graphics.RectangleShape)
                        .background(Color.Gray.copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
fun ExploreWorkGridCard(
    work: DiscoverPost,
    aspectRatio: Float,
    repository: WorkRepository?,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    var layers by remember(work.jsonUrl) { mutableStateOf<List<GradientLayer>?>(LayerCache.get(work.jsonUrl)) }

    LaunchedEffect(work.jsonUrl) {
        if (layers == null && work.jsonUrl.isNotBlank() && repository != null) {
            layers = withContext(Dispatchers.IO) {
                val parsed = repository.deserializeLayers(work.jsonUrl)
                LayerCache.put(work.jsonUrl, parsed)
                parsed
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(androidx.compose.ui.graphics.RectangleShape)
                .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA))
                .border(0.5.dp, colors.glassBorder, androidx.compose.ui.graphics.RectangleShape)
        ) {
            if (work.postType == "image" && !work.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(work.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = work.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF007AFF).copy(alpha = 0.6f), Color(0xFF5856D6).copy(alpha = 0.6f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Sparkles, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = work.title.ifBlank { "Creation" },
            color = colors.textMain,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "@${work.authorUsername.ifBlank { "creator" }}",
                color = Color(0xFF8E8E93),
                fontSize = 11.5.sp,
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (work.likesCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Heart,
                        contentDescription = null,
                        tint = Color(0xFFFF2D55),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = work.likesCount.toString(),
                        color = Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
