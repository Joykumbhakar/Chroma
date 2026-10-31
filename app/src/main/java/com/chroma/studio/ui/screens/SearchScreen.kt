@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.chroma.studio.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.R
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.components.OneUiGradientStyle
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

// ── One UI Design Tokens ──────────────────────────────────────────────────────
private val OneUiBlueLight = Color(0xFF0381FE)
private val OneUiBlueDark = Color(0xFF3897FF)
private val OneUiSurfaceLight = Color(0xFFF2F2F7)
private val OneUiSurfaceDark = Color(0xFF000000)
private val OneUiCardLight = Color(0xFFFFFFFF)
private val OneUiCardDark = Color(0xFF1C1C1E)
private val OneUiPillLight = Color(0xFFE5E5EA)
private val OneUiPillDark = Color(0xFF2C2C2E)

/**
 * Authentic Samsung One UI 7.0 Galaxy AI Dual-Star Sparkle Vector Icon.
 * Rendered using the official Galaxy AI SVG path geometry with subtle continuous breathing animation.
 */
@Composable
fun GalaxyAiSparkleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparklePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha"
    )

    val largeStarPath = remember {
        PathParser().parsePathString("M10 3C10.5 7.2 12.8 9.5 17 10C12.8 10.5 10.5 12.8 10 17C9.5 12.8 7.2 10.5 3 10C7.2 9.5 9.5 7.2 10 3Z").toPath()
    }
    val smallStarPath = remember {
        PathParser().parsePathString("M18.5 13.5C18.8 15.6 19.9 16.7 22 17C19.9 17.3 18.8 18.4 18.5 20.5C18.2 18.4 17.1 17.3 15 17C17.1 16.7 18.2 15.6 18.5 13.5Z").toPath()
    }

    val auroraGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF3B82F6), // Blue
                Color(0xFFA855F7), // Purple
                Color(0xFF06B6D4), // Cyan
                Color(0xFF22C55E)  // Emerald
            ),
            start = Offset(0f, 0f),
            end = Offset.Infinite
        )
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        val scaleX = this.size.width / 24f
        val scaleY = this.size.height / 24f
        withTransform({
            scale(scaleX, scaleY, Offset.Zero)
        }) {
            drawPath(path = largeStarPath, brush = auroraGradient)
            drawPath(path = smallStarPath, brush = auroraGradient)
        }
    }
}

/**
 * Authentic Samsung One UI 7.0 Galaxy AI Search Action Circular Button.
 * Features translucent gradient backdrop and specular rim border.
 */
@Composable
fun GalaxyAiSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    val searchPath = remember {
        PathParser().parsePathString(
            "M21.53 20.47l-3.66-3.66C19.195 15.24 20 13.214 20 11c0-4.97-4.03-9-9-9s-9 4.03-9 9 4.03 9 9 9c2.215 0 4.24-.804 5.808-2.13l3.66 3.66c.147.146.34.22.53.22s.385-.073.53-.22c.295-.293.295-.767.002-1.06zM3.5 11c0-4.135 3.365-7.5 7.5-7.5s7.5 3.365 7.5 7.5-3.365 7.5-7.5 7.5-7.5-3.365-7.5-7.5z"
        ).toPath()
    }
    val auroraBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF3B82F6),
                Color(0xFFA855F7),
                Color(0xFF06B6D4),
                Color(0xFF22C55E)
            )
        )
    }
    val bgBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFFA855F7).copy(alpha = 0.18f),
                Color(0xFF22C55E).copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFFEEF4FF),
                Color(0xFFF3EFFF)
            )
        )
    }
    val borderColor = if (isDark) Color(0xFF94A3B8).copy(alpha = 0.22f) else Color(0xFF94B3F5).copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bgBrush)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(17.dp)) {
            val scaleX = this.size.width / 24f
            val scaleY = this.size.height / 24f
            withTransform({
                scale(scaleX, scaleY, Offset.Zero)
            }) {
                drawPath(path = searchPath, brush = auroraBrush)
            }
        }
    }
}

/**
 * Pinned Samsung Galaxy AI Aurora Glow Search Bar (One UI 7.0 Spec).
 * 125deg multi-color border shell, dual-star AI sparkle, responsive search input,
 * clear action and Galaxy AI circular search button.
 */
@Composable
fun GalaxyAiAuroraSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    placeholder: String = "Search creations, styles, creators...",
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val shellGradient = if (isDark) {
        if (isFocused) {
            Brush.linearGradient(
                listOf(
                    Color(0xFF60A5FA),
                    Color(0xFFC084FC),
                    Color(0xFF22D3EE),
                    Color(0xFF4ADE80)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xFF3871E0),
                    Color(0xFFA855F7),
                    Color(0xFF06B6D4),
                    Color(0xFF22C55E)
                )
            )
        }
    } else {
        if (isFocused) {
            Brush.linearGradient(
                listOf(
                    Color(0xFF2563EB),
                    Color(0xFF9333EA),
                    Color(0xFF0891B2),
                    Color(0xFF059669)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color(0xFF3871E0),
                    Color(0xFFA855F7),
                    Color(0xFF06B6D4),
                    Color(0xFF10B981)
                )
            )
        }
    }

    val innerBgGradient = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF151A26),
                Color(0xFF10141D)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF8FAFF)
            )
        )
    }

    val innerRimBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.24f),
                Color(0xFFA855F7).copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White,
                Color(0xFFE0ECFF).copy(alpha = 0.85f)
            )
        )
    }

    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val placeholderColor = if (isDark) Color(0xFF64748B) else Color(0xFF7C8EA6)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(shellGradient)
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(CircleShape)
                .background(innerBgGradient)
                .border(1.dp, innerRimBrush, CircleShape)
                .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GalaxyAiSparkleIcon(
                size = 20.dp,
                modifier = Modifier.padding(end = 10.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = placeholderColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = InterFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 15.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(if (isDark) OneUiBlueDark else OneUiBlueLight),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                        .onFocusChanged {
                            isFocused = it.isFocused
                            onFocusChange?.invoke(it.isFocused)
                        }
                )
            }

            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Clear search",
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }

            GalaxyAiSearchButton(
                onClick = onSearchAction,
                isDark = isDark
            )
        }
    }
}

/**
 * Standard One UI Search Bar Pill Component.
 * Follows Samsung One UI Search Bar anatomy:
 * - 46.dp pill container (#E5E5EA light / #2C2C2E dark, morphing to card surface with #0381FE focus ring)
 * - Leading back arrow when in focused search mode, or magnifying glass icon
 * - Instant clear ('X') button when query is non-empty
 */
@Composable
fun OneUiSearchBarPill(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    isFocusedMode: Boolean,
    onExitFocusMode: () -> Unit,
    isDark: Boolean,
    accentColor: Color,
    placeholder: String = "Search artworks, styles, creators...",
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var hasFocus by remember { mutableStateOf(false) }

    val pillBgColor by animateColorAsState(
        targetValue = when {
            hasFocus || isFocusedMode -> if (isDark) OneUiCardDark else OneUiCardLight
            else -> if (isDark) OneUiPillDark else OneUiPillLight
        },
        animationSpec = tween(200),
        label = "pillBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (hasFocus) accentColor else Color.Transparent,
        animationSpec = tween(200),
        label = "pillBorder"
    )

    val textColor = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
    val hintColor = if (isDark) Color(0xFF98989D) else Color(0xFF6C6C70)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CircleShape)
            .background(pillBgColor)
            .border(1.5.dp, borderColor, CircleShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isFocusedMode) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onExitFocusMode() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = "Back",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Icon(
                imageVector = Lucide.Search,
                contentDescription = "Search",
                tint = hintColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    color = hintColor,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = InterFontFamily
                ),
                cursorBrush = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .onFocusChanged {
                        hasFocus = it.isFocused
                        onFocusChange?.invoke(it.isFocused)
                    }
            )
        }

        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f))
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = "Clear query",
                    tint = textColor,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Authentic One UI Checkbox Indicator for the "More options" menu.
 * "In One UI, we indicate when an option is on or off in the More options menu with a checkbox."
 */
@Composable
private fun OneUiMenuCheckboxItem(
    title: String,
    checked: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = InterFontFamily,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )

        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) accentColor else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (checked) accentColor else Color(0xFF8E8E93),
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Helper to highlight matching query substrings in One UI Blue (#0381FE / #3897FF)
 */
@Composable
private fun rememberHighlightedText(
    fullText: String,
    query: String,
    highlightColor: Color
) = remember(fullText, query, highlightColor) {
    val cleanQuery = query.trim().removePrefix("#")
    if (cleanQuery.isBlank()) {
        buildAnnotatedString { append(fullText) }
    } else {
        buildAnnotatedString {
            val lowerFull = fullText.lowercase()
            val lowerQ = cleanQuery.lowercase()
            var startIndex = 0
            while (startIndex < fullText.length) {
                val matchIndex = lowerFull.indexOf(lowerQ, startIndex)
                if (matchIndex == -1) {
                    append(fullText.substring(startIndex))
                    break
                }
                if (matchIndex > startIndex) {
                    append(fullText.substring(startIndex, matchIndex))
                }
                withStyle(
                    SpanStyle(
                        color = highlightColor,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(fullText.substring(matchIndex, matchIndex + cleanQuery.length))
                }
                startIndex = matchIndex + cleanQuery.length
            }
        }
    }
}

private enum class SearchScope(val label: String) {
    ALL("All"),
    CREATORS("Creators"),
    ARTWORKS("Artworks"),
    TAGS("Tags")
}

private enum class OneUiSortOrder(val label: String) {
    POPULAR("Popular"),
    MOST_LIKED("Most Liked"),
    MOST_REMIXED("Most Remixed")
}

private val SUGGESTED_SEARCH_PROMPTS = listOf(
    "Aurora mesh",
    "Glassmorphism",
    "Cyberpunk neon",
    "Minimal gradient",
    "3D abstract"
)

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
    val searchFocusRequester = remember { FocusRequester() }

    val prefs = remember { context.getSharedPreferences("chat_ui_preferences", Context.MODE_PRIVATE) }

    // ── Persisted One UI App Bar Options & State Retention ────────────────────
    // "If the user leaves the app and comes back, then they'll find the app bar
    // in the exact same condition that they left it in, either expanded or collapsed."
    var useDynamicTitle by remember {
        mutableStateOf(prefs.getBoolean("oneui_search_dynamic_title", false))
    }
    var useTextActionButtons by remember {
        mutableStateOf(prefs.getBoolean("oneui_search_text_actions", false))
    }
    var useGalaxyAiSearchBar by remember {
        mutableStateOf(prefs.getBoolean("oneui_search_galaxy_ai_bar", false))
    }
    var sortOrder by remember {
        val savedSort = prefs.getString("oneui_search_sort_order", OneUiSortOrder.POPULAR.name)
        mutableStateOf(
            try {
                OneUiSortOrder.valueOf(savedSort ?: OneUiSortOrder.POPULAR.name)
            } catch (_: Exception) {
                OneUiSortOrder.POPULAR
            }
        )
    }
    var showMoreOptionsMenu by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFieldFocused by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedScope by remember { mutableStateOf(SearchScope.ALL) }

    var userResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isSearchingUsers by remember { mutableStateOf(false) }
    var searchHistory by remember { mutableStateOf(historyManager.getSearchHistory()) }

    val allWorks by discoverVm.works.collectAsState()
    val trendingWorks by discoverVm.trendingWorks.collectAsState()
    val likedWorkIds by discoverVm.likedWorkIds.collectAsState()
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

    // Filtered Artworks by Query, Category & One UI Sort Order
    val filteredArtworks = remember(allWorks, trendingWorks, searchQuery, selectedCategory, sortOrder) {
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

        val queriedList = if (searchQuery.isBlank()) {
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

        when (sortOrder) {
            OneUiSortOrder.POPULAR -> queriedList
            OneUiSortOrder.MOST_LIKED -> queriedList.sortedByDescending { it.likesCount }
            OneUiSortOrder.MOST_REMIXED -> queriedList.sortedByDescending { it.remixCount }
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

    val isSearchActive = searchQuery.isNotBlank() || isSearchFieldFocused

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
            SoundManager.playRefresh(context)
        }
    }

    val density = LocalDensity.current
    var gradientStyle by remember {
        val savedName = prefs.getString("one_ui_gradient_style", OneUiGradientStyle.GALAXY_AI.name)
        mutableStateOf(
            try {
                OneUiGradientStyle.valueOf(savedName ?: "GALAXY_AI")
            } catch (_: Exception) {
                OneUiGradientStyle.GALAXY_AI
            }
        )
    }

    fun cycleGradientStyle() {
        val styles = OneUiGradientStyle.entries
        val next = styles[(styles.indexOf(gradientStyle) + 1) % styles.size]
        gradientStyle = next
        prefs.edit().putString("one_ui_gradient_style", next.name).apply()
        SoundManager.playReaction()
    }

    // ── One UI Extended App Bar Height & State Persistence ────────────────────
    val expandedHeightDp: Dp = if (useDynamicTitle) 216.dp else 192.dp
    val maxCollapsePx = remember(density, expandedHeightDp) { with(density) { expandedHeightDp.toPx() } }

    // Restore exact collapsed/expanded fraction from SharedPreferences
    var collapseOffsetPx by remember(maxCollapsePx) {
        val savedFraction = prefs.getFloat("oneui_search_appbar_collapse_fraction", 0f).coerceIn(0f, 1f)
        mutableFloatStateOf(savedFraction * maxCollapsePx)
    }

    // Persist state whenever the user leaves the screen or finishes snapping
    DisposableEffect(Unit) {
        onDispose {
            val fraction = if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx).coerceIn(0f, 1f)
            prefs.edit().putFloat("oneui_search_appbar_collapse_fraction", fraction).apply()
        }
    }

    // 0.0f = Extended App Bar, 1.0f = Condensed Standard App Bar
    val collapseProgress = remember(collapseOffsetPx, maxCollapsePx) {
        if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx).coerceIn(0f, 1f)
    }

    // Authentic Samsung One UI Two-Stage Title Crossfade + Expanded Gradient Alpha
    val largeTitleAlpha = (1f - (collapseProgress / 0.45f)).coerceIn(0f, 1f)
    val subtitleAlpha = (1f - (collapseProgress / 0.35f)).coerceIn(0f, 1f)
    val compactTitleAlpha = ((collapseProgress - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val gradientAlpha = if (gradientStyle == OneUiGradientStyle.SOLID) {
        0f
    } else {
        (1f - (collapseProgress / 0.85f)).coerceIn(0f, 1f)
    }

    fun animateSnapTo(targetFraction: Float) {
        val clamped = targetFraction.coerceIn(0f, 1f)
        val targetPx = clamped * maxCollapsePx
        prefs.edit().putFloat("oneui_search_appbar_collapse_fraction", clamped).apply()
        coroutineScope.launch {
            animate(
                initialValue = collapseOffsetPx,
                targetValue = targetPx,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { value, _ ->
                collapseOffsetPx = value
            }
        }
    }

    // NestedScrollConnection with One UI Magnetic Auto-Snap Physics
    val nestedScrollConnection = remember(maxCollapsePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                // When user scrolls down (finger moves up -> deltaY < 0): condense extended app bar first
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
                val deltaY = available.y
                // When user scrolls up (finger moves down -> deltaY > 0): expand app bar again
                if (deltaY > 0f && collapseOffsetPx > 0f) {
                    val newOffset = (collapseOffsetPx - deltaY).coerceIn(0f, maxCollapsePx)
                    val consumedY = -(newOffset - collapseOffsetPx)
                    collapseOffsetPx = newOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val progress = if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx)
                if (progress > 0.02f && progress < 0.98f) {
                    val targetPx = when {
                        available.y < -350f -> maxCollapsePx
                        available.y > 350f -> 0f
                        progress >= 0.5f -> maxCollapsePx
                        else -> 0f
                    }
                    val targetFraction = if (maxCollapsePx <= 0f) 0f else (targetPx / maxCollapsePx).coerceIn(0f, 1f)
                    prefs.edit().putFloat("oneui_search_appbar_collapse_fraction", targetFraction).apply()
                    animate(
                        initialValue = collapseOffsetPx,
                        targetValue = targetPx,
                        animationSpec = spring(
                            dampingRatio = 0.86f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { value, _ ->
                        collapseOffsetPx = value
                    }
                }
                return Velocity.Zero
            }
        }
    }

    // One UI Colors
    val screenBgColor = if (!isDark) OneUiSurfaceLight else OneUiSurfaceDark
    val cardBgColor = if (!isDark) OneUiCardLight else OneUiCardDark
    val cardBorderColor = if (!isDark) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.07f)
    val dividerColor = if (!isDark) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)
    val textDark = if (!isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val textMuted = if (!isDark) Color(0xFF6C6C70) else Color(0xFF98989D)
    val activeAccent = if (gradientStyle == OneUiGradientStyle.SOLID) {
        if (isDark) OneUiBlueDark else OneUiBlueLight
    } else {
        gradientStyle.primaryColor
    }

    val currentExpandedHeightDp = with(density) {
        (maxCollapsePx - collapseOffsetPx).coerceAtLeast(0f).toDp()
    }
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()

    // Dynamic Title text & auto-resize handling per One UI guidelines
    val resolvedHeaderTitle = remember(isSearchActive, searchQuery, useDynamicTitle, filteredArtworks.size, selectedCategory) {
        when {
            searchQuery.isNotBlank() -> "Results for \"${searchQuery.trim()}\""
            isSearchActive -> "Search Chroma"
            useDynamicTitle -> "${filteredArtworks.size} creations in $selectedCategory"
            else -> "Explore"
        }
    }

    // Auto-resize font size when title text is long (per One UI Title specification)
    val extendedTitleFontSize = remember(resolvedHeaderTitle) {
        when {
            resolvedHeaderTitle.length > 28 -> 22.sp
            resolvedHeaderTitle.length > 18 -> 26.sp
            else -> 32.sp
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
            .nestedScroll(nestedScrollConnection)
    ) {
        // 1. Chroma Studio One UI System Gradient Backdrop
        ChromaOneUiHeaderBackdrop(
            expandedHeightDp = expandedHeightDp,
            collapseProgress = collapseProgress,
            gradientAlpha = gradientAlpha,
            isDark = isDark,
            screenBgColor = screenBgColor
        )

        Column(modifier = Modifier.fillMaxSize()) {
            val toolbarBgAlpha = (collapseProgress * 0.94f).coerceIn(0f, 0.94f)

            // Top Status Bar Spacer
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(screenBgColor.copy(alpha = toolbarBgAlpha))
            )

            // =========================================================================
            // 2. ONE UI EXTENDED APP BAR VIEWING AREA
            // "Normally, the title of the app or page appears right in the center of the
            // expanded app bar. Any additional info or notifications are shown below.
            // Dynamic titles can have up to two action buttons below them."
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentExpandedHeightDp),
                contentAlignment = Alignment.Center
            ) {
                if (largeTitleAlpha > 0.01f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .graphicsLayer {
                                alpha = largeTitleAlpha
                                translationY = -collapseProgress * with(density) { 18.dp.toPx() }
                                val scale = 1f - (collapseProgress * 0.06f)
                                scaleX = scale
                                scaleY = scale
                            }
                    ) {
                        // [2] Extended Centered Title (Auto-resizing + Ellipsis fallback)
                        Text(
                            text = resolvedHeaderTitle,
                            fontSize = extendedTitleFontSize,
                            fontWeight = if (useDynamicTitle) FontWeight.Bold else FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                            color = textDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        // Subheading beneath the title
                        Text(
                            text = if (isSearchActive) {
                                "${userResults.size} creators • ${filteredArtworks.size} artworks • ${matchingTags.size} tags"
                            } else {
                                "${filteredArtworks.size} artworks • ${trendingWorks.size} trending now"
                            },
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
                        )

                        // Up to 2 Action Buttons below Dynamic Title (One UI Specification)
                        if (useDynamicTitle) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
                            ) {
                                // Dynamic Title Action Button 1
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(activeAccent)
                                        .clickable {
                                            selectedCategory = if (selectedCategory == "Trending") "All" else "Trending"
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (selectedCategory == "Trending") "Show All" else "Trending (${trendingWorks.size})",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = InterFontFamily
                                    )
                                }

                                // Dynamic Title Action Button 2: Cycle Sort Order
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f))
                                        .clickable {
                                            val orders = OneUiSortOrder.entries
                                            val nextSort = orders[(orders.indexOf(sortOrder) + 1) % orders.size]
                                            sortOrder = nextSort
                                            prefs.edit().putString("oneui_search_sort_order", nextSort.name).apply()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Sort: ${sortOrder.label}",
                                        color = textDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. STICKY CONDENSED ONE UI APP BAR (56.dp Standard Height)
            // Anatomy: [1] Back  [2] Condensed Title  [3] Action buttons (<=3)  [4] More
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(screenBgColor.copy(alpha = toolbarBgAlpha))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: [1] Back Navigation + [2] Condensed Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        // [1] Back Button
                        IconButton(
                            onClick = {
                                when {
                                    searchQuery.isNotBlank() || isSearchFieldFocused -> {
                                        searchQuery = ""
                                        isSearchFieldFocused = false
                                        focusManager.clearFocus()
                                    }
                                    collapseProgress > 0.6f -> {
                                        animateSnapTo(0f)
                                    }
                                    else -> {
                                        onBack()
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.ChevronLeft,
                                contentDescription = "Navigate back",
                                tint = textDark,
                                modifier = Modifier.size(23.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // [2] Condensed Title (Fades in smoothly as Extended App Bar collapses)
                        Text(
                            text = resolvedHeaderTitle,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer {
                                alpha = compactTitleAlpha
                                translationY = (1f - compactTitleAlpha) * with(density) { 4.dp.toPx() }
                            }
                        )
                    }

                    // Right: [3] Action Buttons (Max 3 recommended) & [4] More Options Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // [3A] Primary Action Button: Search (Focuses Search Bar & condenses header)
                        if (useTextActionButtons) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        animateSnapTo(1f)
                                        coroutineScope.launch {
                                            delay(60)
                                            try {
                                                searchFocusRequester.requestFocus()
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Search",
                                    color = activeAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    animateSnapTo(1f)
                                    coroutineScope.launch {
                                        delay(60)
                                        try {
                                            searchFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Search,
                                    contentDescription = "Search",
                                    tint = textDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // [3B] Secondary Action Button: Refresh
                        if (useTextActionButtons) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { handleRefresh() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isRefreshing) "Syncing..." else "Refresh",
                                    color = activeAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily
                                )
                            }
                        } else {
                            val rotation by animateFloatAsState(
                                targetValue = if (isRefreshing) 360f else 0f,
                                animationSpec = if (isRefreshing) infiniteRepeatable(tween(800, easing = LinearEasing)) else tween(300),
                                label = "refreshRotation"
                            )
                            IconButton(
                                onClick = { handleRefresh() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.RotateCw,
                                    contentDescription = "Refresh",
                                    tint = textDark,
                                    modifier = Modifier
                                        .size(19.dp)
                                        .graphicsLayer { rotationZ = rotation }
                                )
                            }
                        }

                        // [3C] Theme Style Pill Action Button (Hidden when text buttons are wide to respect space)
                        if (!useTextActionButtons) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (!isDark) Color.Black.copy(0.06f) else Color.White.copy(0.10f))
                                    .clickable { cycleGradientStyle() }
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (gradientStyle == OneUiGradientStyle.SOLID) OneUiBlueLight
                                                else gradientStyle.orbColor
                                            )
                                    )
                                    Text(
                                        text = gradientStyle.label,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textDark
                                    )
                                }
                            }
                        }

                        // [4] More Options Button (Far Right) + One UI Checkbox Menu
                        Box {
                            IconButton(
                                onClick = { showMoreOptionsMenu = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.EllipsisVertical,
                                    contentDescription = "More options",
                                    tint = textDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreOptionsMenu,
                                onDismissRequest = { showMoreOptionsMenu = false },
                                offset = DpOffset(x = 0.dp, y = 6.dp),
                                modifier = Modifier
                                    .width(250.dp)
                                    .background(
                                        color = if (isDark) Color(0xFF2C2C2E) else Color.White,
                                        shape = RoundedCornerShape(22.dp)
                                    )
                            ) {
                                Text(
                                    text = "ONE UI APP BAR OPTIONS",
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )

                                OneUiMenuCheckboxItem(
                                    title = "Dynamic sentence title",
                                    checked = useDynamicTitle,
                                    accentColor = activeAccent,
                                    isDark = isDark,
                                    onCheckedChange = { checked ->
                                        useDynamicTitle = checked
                                        prefs.edit().putBoolean("oneui_search_dynamic_title", checked).apply()
                                    }
                                )

                                OneUiMenuCheckboxItem(
                                    title = "Text action buttons",
                                    checked = useTextActionButtons,
                                    accentColor = activeAccent,
                                    isDark = isDark,
                                    onCheckedChange = { checked ->
                                        useTextActionButtons = checked
                                        prefs.edit().putBoolean("oneui_search_text_actions", checked).apply()
                                    }
                                )

                                OneUiMenuCheckboxItem(
                                    title = "Aurora glowing search bar",
                                    checked = useGalaxyAiSearchBar,
                                    accentColor = activeAccent,
                                    isDark = isDark,
                                    onCheckedChange = { checked ->
                                        useGalaxyAiSearchBar = checked
                                        prefs.edit().putBoolean("oneui_search_galaxy_ai_bar", checked).apply()
                                    }
                                )

                                HorizontalDivider(
                                    color = dividerColor,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Text(
                                    text = "SORT CREATIONS BY",
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )

                                OneUiSortOrder.entries.forEach { option ->
                                    OneUiMenuCheckboxItem(
                                        title = option.label,
                                        checked = sortOrder == option,
                                        accentColor = activeAccent,
                                        isDark = isDark,
                                        onCheckedChange = {
                                            sortOrder = option
                                            prefs.edit().putString("oneui_search_sort_order", option.name).apply()
                                        }
                                    )
                                }

                                HorizontalDivider(
                                    color = dividerColor,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                // Expand / Collapse Quick Action
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showMoreOptionsMenu = false
                                            animateSnapTo(if (collapseProgress > 0.5f) 0f else 1f)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (collapseProgress > 0.5f) "Expand App Bar" else "Condense App Bar",
                                        color = textDark,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = InterFontFamily
                                    )
                                    Icon(
                                        imageVector = if (collapseProgress > 0.5f) Lucide.ChevronDown else Lucide.ChevronUp,
                                        contentDescription = null,
                                        tint = textMuted,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // =====================================================================
                // 4. PINNED ONE UI SEARCH BAR (Reachability-Optimized Header Bar)
                // =====================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    if (useGalaxyAiSearchBar) {
                        GalaxyAiAuroraSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearchAction = { focusManager.clearFocus() },
                            isDark = isDark,
                            placeholder = "Search creations, styles, creators...",
                            focusRequester = searchFocusRequester,
                            onFocusChange = { focused ->
                                isSearchFieldFocused = focused
                            }
                        )
                    } else {
                        OneUiSearchBarPill(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearchAction = { focusManager.clearFocus() },
                            isFocusedMode = isSearchFieldFocused || searchQuery.isNotBlank(),
                            onExitFocusMode = {
                                searchQuery = ""
                                isSearchFieldFocused = false
                                focusManager.clearFocus()
                            },
                            isDark = isDark,
                            accentColor = activeAccent,
                            placeholder = "Search artworks, styles, creators...",
                            focusRequester = searchFocusRequester,
                            onFocusChange = { focused ->
                                isSearchFieldFocused = focused
                            }
                        )
                    }
                }

                if (collapseProgress > 0.85f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.6.dp)
                            .background(dividerColor)
                    )
                }
            }

            // ── 5. Main Staggered Grid Content (One UI 26.dp Squircle Containers) ──
                        LazyVerticalStaggeredGrid(
                state = gridState,
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(top = 6.dp, bottom = 120.dp, start = 14.dp, end = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isSearchActive) {
                    // ── Search Mode: Scope Bar (All / Creators / Artworks / Tags) ──
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchScope.entries.forEach { scope ->
                                val isSelected = scope == selectedScope
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(17.dp))
                                        .background(
                                            if (isSelected) activeAccent
                                            else cardBgColor
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.Transparent else cardBorderColor,
                                            RoundedCornerShape(17.dp)
                                        )
                                        .clickable { selectedScope = scope },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = scope.label,
                                        color = if (isSelected) Color.White else textDark,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }
                        }
                    }

                    if (searchQuery.isBlank()) {
                        // ── Search Mode: Recent Searches & Suggested Prompts (One UI 26.dp Squircle Card) ──
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(cardBgColor)
                                    .border(1.dp, cardBorderColor, RoundedCornerShape(26.dp))
                                    .padding(16.dp)
                            ) {
                                if (searchHistory.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recent searches",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textMuted,
                                            fontFamily = InterFontFamily
                                        )
                                        Text(
                                            text = "Clear all",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = activeAccent,
                                            fontFamily = InterFontFamily,
                                            modifier = Modifier.clickable {
                                                historyManager.clearHistory()
                                                searchHistory = emptyList()
                                            }
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(searchHistory, key = { "hist_${it.uid}" }) { recentUser ->
                                            Row(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(if (!isDark) OneUiSurfaceLight else OneUiPillDark)
                                                    .clickable { handleUserClick(recentUser) }
                                                    .padding(start = 5.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
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
                                                    color = textDark,
                                                    fontFamily = InterFontFamily
                                                )
                                                Icon(
                                                    imageVector = Lucide.X,
                                                    contentDescription = "Remove",
                                                    tint = textMuted,
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
                                    Spacer(Modifier.height(14.dp))
                                }

                                Text(
                                    text = "Suggested searches",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textMuted,
                                    fontFamily = InterFontFamily
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(SUGGESTED_SEARCH_PROMPTS) { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(activeAccent.copy(alpha = 0.10f))
                                                .border(1.dp, activeAccent.copy(alpha = 0.22f), CircleShape)
                                                .clickable { searchQuery = suggestion }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Lucide.Sparkles,
                                                contentDescription = null,
                                                tint = activeAccent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = suggestion,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = activeAccent,
                                                fontFamily = InterFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ── Search Mode: Query Active ────────────────────────────
                        // Creators Section
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
                                            color = activeAccent,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            } else if (userResults.isNotEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    Text(
                                        text = "Creators (${userResults.size})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textMuted,
                                        fontFamily = InterFontFamily,
                                        modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 2.dp)
                                    )
                                }
                                items(
                                    items = userResults,
                                    key = { "search_user_${it.uid}" },
                                    span = { StaggeredGridItemSpan.FullLine }
                                ) { user ->
                                    CreatorSearchResultCard(
                                        user = user,
                                        searchQuery = searchQuery,
                                        colors = colors,
                                        accentColor = activeAccent,
                                        onClick = { handleUserClick(user) }
                                    )
                                }
                            } else if (selectedScope == SearchScope.CREATORS && !isSearchingUsers && searchQuery.isNotBlank()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    val emptyState = rememberAppleGraphicAnimationState()
                                    AppleEmptyTabState(
                                        graphic = {
                                            AppleHeartHandshakeGraphic(
                                                modifier = Modifier.size(140.dp),
                                                state = emptyState,
                                                enableClickToAnimate = true
                                            )
                                        },
                                        title = "No Creators Found",
                                        subtitle = "No creator matched \"$searchQuery\""
                                    )
                                }
                            }
                        }

                        // Matching Tags Section
                        if ((selectedScope == SearchScope.ALL || selectedScope == SearchScope.TAGS) && matchingTags.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(cardBgColor)
                                        .border(1.dp, cardBorderColor, RoundedCornerShape(26.dp))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Tags & Topics",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textMuted,
                                        fontFamily = InterFontFamily
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(matchingTags) { tag ->
                                            val cleanTag = tag.removePrefix("#")
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(activeAccent.copy(alpha = 0.12f))
                                                    .border(1.dp, activeAccent.copy(alpha = 0.25f), CircleShape)
                                                    .clickable { searchQuery = cleanTag }
                                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                            ) {
                                                Text(
                                                    text = rememberHighlightedText("#$cleanTag", searchQuery, activeAccent),
                                                    color = activeAccent,
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

                        // Artworks Section
                        if (selectedScope == SearchScope.ALL || selectedScope == SearchScope.ARTWORKS) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Artworks (${filteredArtworks.size})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textMuted,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }

                            if (filteredArtworks.isEmpty() && !isSearchingUsers && userResults.isEmpty() && selectedScope == SearchScope.ALL && searchQuery.isNotBlank()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    val emptyState = rememberAppleGraphicAnimationState()
                                    AppleEmptyTabState(
                                        graphic = {
                                            AppleTrendingFireGraphic(
                                                modifier = Modifier.size(140.dp),
                                                state = emptyState,
                                                enableClickToAnimate = true
                                            )
                                        },
                                        title = "No Results Found",
                                        subtitle = "We couldn't find anything matching \"$searchQuery\""
                                    )
                                }
                            } else if (filteredArtworks.isEmpty() && selectedScope == SearchScope.ARTWORKS && searchQuery.isNotBlank()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    val emptyState = rememberAppleGraphicAnimationState()
                                    AppleEmptyTabState(
                                        graphic = {
                                            AppleTrendingFireGraphic(
                                                modifier = Modifier.size(140.dp),
                                                state = emptyState,
                                                enableClickToAnimate = true
                                            )
                                        },
                                        title = "No Artworks Found",
                                        subtitle = "Try searching for different keywords or styles."
                                    )
                                }
                            } else {
                                items(filteredArtworks, key = { "search_art_${it.id}" }) { work ->
                                    val aspectRatio = when (work.id.hashCode() % 3) {
                                        0 -> 0.78f
                                        1 -> 0.90f
                                        else -> 1.0f
                                    }
                                    val isLiked = likedWorkIds.contains(work.id)
                                    ExploreWorkGridCard(
                                        work = work,
                                        aspectRatio = aspectRatio,
                                        repository = repository,
                                        isLiked = isLiked,
                                        searchQuery = searchQuery,
                                        accentColor = activeAccent,
                                        onClick = { selectedPreviewPost = work },
                                        onDoubleTapLike = {
                                            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                            if (!uid.isNullOrBlank()) {
                                                discoverVm.toggleLike(work.id, uid)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ── 5B. In Explore Mode: One UI Category Filter Pills ────
                    item(span = StaggeredGridItemSpan.FullLine) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            items(EXPLORE_CATEGORIES) { filter ->
                                val isSelected = filter == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) activeAccent
                                            else cardBgColor
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.Transparent else cardBorderColor,
                                            CircleShape
                                        )
                                        .clickable { selectedCategory = filter }
                                        .padding(horizontal = 15.dp, vertical = 7.5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        if (filter == "Trending") {
                                            AppleTrendingFireIcon(modifier = Modifier.size(15.dp))
                                        } else if (filter == "Photos") {
                                            ApplePhotosIcon(modifier = Modifier.size(15.dp))
                                        }
                                        Text(
                                            text = filter,
                                            color = if (isSelected) Color.White else textDark,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            fontFamily = InterFontFamily
                                        )
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
                                accentColor = activeAccent,
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

                    // ── Explore Mode: Community Trending Tags (One UI 26.dp Card) ──
                    if (matchingTags.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(cardBgColor)
                                    .border(1.dp, cardBorderColor, RoundedCornerShape(26.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Trending Tags",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textMuted,
                                    fontFamily = InterFontFamily
                                )
                                Spacer(Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(matchingTags) { tag ->
                                        val cleanTag = tag.removePrefix("#")
                                        Row(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (!isDark) OneUiSurfaceLight else OneUiPillDark)
                                                .clickable { searchQuery = cleanTag }
                                                .padding(horizontal = 13.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("#", color = activeAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                            Spacer(Modifier.width(3.dp))
                                            Text(cleanTag, color = textDark, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
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
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selectedCategory == "Trending") {
                                    AppleTrendingFireIcon(modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = if (selectedCategory == "All") "Explore Creations" else "$selectedCategory Creations",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textMuted,
                                    fontFamily = InterFontFamily
                                )
                            }
                            Text(
                                text = "${filteredArtworks.size} items",
                                fontSize = 12.5.sp,
                                color = textMuted,
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
                            val emptyState = rememberAppleGraphicAnimationState()
                            AppleEmptyTabState(
                                graphic = {
                                    if (selectedCategory.equals("Photos", ignoreCase = true)) {
                                        ApplePhotosGraphic(
                                            modifier = Modifier.size(150.dp),
                                            state = emptyState,
                                            enableClickToAnimate = true
                                        )
                                    } else {
                                        AppleTrendingFireGraphic(
                                            modifier = Modifier.size(150.dp),
                                            state = emptyState,
                                            enableClickToAnimate = true
                                        )
                                    }
                                },
                                title = "No Creations Here",
                                subtitle = "No creations found in category \"$selectedCategory\"."
                            )
                        }
                    } else {
                        items(filteredArtworks, key = { "explore_work_${it.id}" }) { work ->
                            val aspectRatio = when (work.id.hashCode() % 3) {
                                0 -> 0.78f
                                1 -> 0.90f
                                else -> 1.0f
                            }

                            val isLiked = likedWorkIds.contains(work.id)
                            ExploreWorkGridCard(
                                work = work,
                                aspectRatio = aspectRatio,
                                repository = repository,
                                isLiked = isLiked,
                                searchQuery = searchQuery,
                                accentColor = activeAccent,
                                onClick = { selectedPreviewPost = work },
                                onDoubleTapLike = {
                                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                    if (!uid.isNullOrBlank()) {
                                        discoverVm.toggleLike(work.id, uid)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        // ── Artwork Quick Look & Action Sheet Modal ────────────────────────────
        if (selectedPreviewPost != null) {
            val post = selectedPreviewPost!!
            val isLiked = likedWorkIds.contains(post.id)
            ExploreQuickLookSheet(
                post = post,
                repository = repository,
                isLiked = isLiked,
                onToggleLike = {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (!uid.isNullOrBlank()) {
                        discoverVm.toggleLike(post.id, uid)
                    }
                },
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

// ── Creator Search Result Card (One UI 26.dp Squircle Card + Live Highlighting) ──
@Composable
private fun CreatorSearchResultCard(
    user: UserProfile,
    searchQuery: String = "",
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    accentColor: Color = OneUiBlueLight,
    onClick: () -> Unit
) {
    val isDark = colors.isDark
    val displayName = user.displayName.ifBlank { user.username }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(if (isDark) OneUiCardDark else OneUiCardLight)
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.04f),
                RoundedCornerShape(26.dp)
            )
            .clickable { onClick() }
            .padding(15.dp)
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
                IosAvatarView(name = user.displayName, avatarUrl = user.avatarUrl, size = 44.dp)

                Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = rememberHighlightedText(displayName, searchQuery, accentColor),
                            color = colors.textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 17.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (user.verified) {
                            Spacer(Modifier.width(4.dp))
                            VerifiedBadge(size = 14.dp)
                        }
                    }
                    Text(
                        text = rememberHighlightedText("@${user.username}", searchQuery, accentColor),
                        color = Color(0xFF8E8E93),
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
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
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable { onClick() }
                    .padding(horizontal = 15.dp, vertical = 7.dp)
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

// ── Spotlight Creator Banner (One UI 26.dp Squircle Container) ────────────────
@Composable
private fun SpotlightCreatorBanner(
    post: DiscoverPost,
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    accentColor: Color = OneUiBlueLight,
    onClick: () -> Unit
) {
    val isDark = colors.isDark
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(if (isDark) OneUiCardDark else OneUiCardLight)
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.04f),
                RoundedCornerShape(26.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF9500).copy(alpha = 0.15f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
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
                            text = post.authorDisplayName.ifBlank { post.authorUsername },
                            color = colors.textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = InterFontFamily
                        )
                        if (post.authorVerified) {
                            Spacer(Modifier.width(4.dp))
                            VerifiedBadge(size = 13.dp)
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
                    .background(accentColor)
                    .clickable { onClick() }
                    .padding(horizontal = 15.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "View",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
fun ExploreQuickLookSheet(
    post: DiscoverPost,
    repository: WorkRepository?,
    onDismiss: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPost: () -> Unit,
    onRemix: () -> Unit,
    isLiked: Boolean = false,
    onToggleLike: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var localIsLiked by remember(post.id, isLiked) { mutableStateOf(isLiked) }
    var localLikesCountDelta by remember(post.id) { mutableIntStateOf(0) }
    val effectiveJson = remember(post.jsonUrl, post.workUrls) {
        post.jsonUrl.ifBlank { post.workUrls.firstOrNull().orEmpty() }
    }
    var layers by remember(effectiveJson) { mutableStateOf<List<GradientLayer>?>(LayerCache.get(effectiveJson)) }
    val doubleTapLikeState = rememberDoubleTapLikeState()

    var isVisible by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val closeWithAnimation: (action: (() -> Unit)?) -> Unit = { action ->
        if (!isClosing) {
            isClosing = true
            isVisible = false
            scope.launch {
                delay(200)
                onDismiss()
                action?.invoke()
            }
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "contentAlpha"
    )

    val sheetSlideY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 350f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 380f
        ),
        label = "sheetSlideY"
    )

    val topPillSlideY by animateFloatAsState(
        targetValue = if (isVisible) 0f else -60f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "topPillSlideY"
    )

    val topButtonsScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 420f
        ),
        label = "topButtonsScale"
    )

    val backButtonInteractionSource = remember { MutableInteractionSource() }
    val isBackButtonPressed by backButtonInteractionSource.collectIsPressedAsState()
    val backButtonPressScale by animateFloatAsState(
        targetValue = if (isBackButtonPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "backPressScale"
    )

    val shareButtonInteractionSource = remember { MutableInteractionSource() }
    val isShareButtonPressed by shareButtonInteractionSource.collectIsPressedAsState()
    val shareButtonPressScale by animateFloatAsState(
        targetValue = if (isShareButtonPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "sharePressScale"
    )

    val remixButtonInteractionSource = remember { MutableInteractionSource() }
    val isRemixButtonPressed by remixButtonInteractionSource.collectIsPressedAsState()
    val remixButtonPressScale by animateFloatAsState(
        targetValue = if (isRemixButtonPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "remixPressScale"
    )

    val viewPostButtonInteractionSource = remember { MutableInteractionSource() }
    val isViewPostButtonPressed by viewPostButtonInteractionSource.collectIsPressedAsState()
    val viewPostButtonPressScale by animateFloatAsState(
        targetValue = if (isViewPostButtonPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "viewPostPressScale"
    )

    var likeBounced by remember { mutableStateOf(false) }
    val likeScale by animateFloatAsState(
        targetValue = if (likeBounced) 1.28f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium),
        finishedListener = { likeBounced = false },
        label = "likeScale"
    )

    LaunchedEffect(effectiveJson) {
        if (layers == null && effectiveJson.isNotBlank() && repository != null) {
            layers = withContext(Dispatchers.IO) {
                val parsed = repository.deserializeLayers(effectiveJson)
                LayerCache.put(effectiveJson, parsed)
                parsed
            }
        }
    }

    val cleanTitle = remember(post.title) {
        val t = post.title.trim()
        if (t.isNotBlank()) t else "Untitled Masterwork"
    }

    val isRemixPost = remember(post.title, post.remixedFromAuthorUsername, post.remixedFromAuthorDisplayName) {
        !post.remixedFromAuthorUsername.isNullOrBlank() ||
        !post.remixedFromAuthorDisplayName.isNullOrBlank() ||
        post.title.startsWith("Remix", ignoreCase = true)
    }

    val displaySubtitle = remember(post.trendingTag, post.postType, isRemixPost) {
        when {
            !post.trendingTag.isNullOrBlank() -> post.trendingTag
            isRemixPost -> "Community Studio Remix"
            post.postType == "video" -> "Trending Flash Video"
            post.postType == "image" -> "Community Photography"
            else -> "Original Studio Masterwork"
        }
    }

    val displayDesc = remember(post.description) {
        if (post.description.isNotBlank()) post.description
        else "Experience this creative masterwork crafted with layered vector art and fluid effects on Chroma Studio."
    }

    val effectiveLikes = (post.likesCount + localLikesCountDelta).coerceAtLeast(0)

    Dialog(
        onDismissRequest = { closeWithAnimation(null) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f * contentAlpha))
                .graphicsLayer { alpha = contentAlpha }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── UPPER HALF: HERO MEDIA PREVIEW ──────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                        .background(Color(0xFF0F172A))
                        .pointerInput(post.id) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (!localIsLiked) {
                                        localIsLiked = true
                                        localLikesCountDelta += 1
                                        likeBounced = true
                                        onToggleLike?.invoke()
                                    }
                                    doubleTapLikeState.trigger(offset)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                        }
                ) {
                    val resolvedImageUrl = remember(post) {
                        post.imageUrl?.takeIf { it.isNotBlank() }
                            ?: post.imageUrls.firstOrNull()?.takeIf { it.isNotBlank() }
                            ?: post.thumbnailUrl?.takeIf { it.isNotBlank() }
                            ?: post.workUrls.firstOrNull { it.startsWith("http") && !it.endsWith(".json") }
                    }

                    if (!resolvedImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(resolvedImageUrl)
                                .crossfade(true)
                                .build(),
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
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF1E3A8A))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Sparkles,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }

                    FlatDoubleTapHeartOverlay(
                        state = doubleTapLikeState,
                        modifier = Modifier.fillMaxSize(),
                        heartSize = 100.dp
                    )

                    // Top-Left Floating Circular Back Button
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 18.dp, top = 14.dp)
                            .size(42.dp)
                            .graphicsLayer {
                                scaleX = topButtonsScale * backButtonPressScale
                                scaleY = topButtonsScale * backButtonPressScale
                            }
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(
                                interactionSource = backButtonInteractionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                closeWithAnimation(null)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.ArrowLeft,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Top-Right Floating Circular Share Button
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(end = 18.dp, top = 14.dp)
                            .size(42.dp)
                            .align(Alignment.TopEnd)
                            .graphicsLayer {
                                scaleX = topButtonsScale * shareButtonPressScale
                                scaleY = topButtonsScale * shareButtonPressScale
                            }
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(
                                interactionSource = shareButtonInteractionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "Check out '${post.title}' by @${post.authorUsername} on Oscellion: https://oscellion.vercel.app/post/${post.id}"
                                    )
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Masterwork"))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Share2,
                            contentDescription = "Share",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Top-Center Dynamic Status Pill
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 16.dp)
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                translationY = topPillSlideY
                            }
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xEE161A26))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            if (post.isTrendingMasterwork || post.likesCount > 50) {
                                AppleTrendingFireIcon(modifier = Modifier.size(15.dp))
                                Text(
                                    text = "Trending Masterwork • $effectiveLikes ${if (effectiveLikes == 1) "Like" else "Likes"}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                )
                            } else if (!post.remixedFromAuthorUsername.isNullOrBlank() || !post.remixedFromAuthorDisplayName.isNullOrBlank()) {
                                val originalOwner = post.remixedFromAuthorDisplayName?.takeIf { it.isNotBlank() } ?: "@${post.remixedFromAuthorUsername.orEmpty()}"
                                Icon(
                                    imageVector = rememberLucideHeartHandshakeIcon(
                                        isRemixed = false,
                                        strokeColor = Color.White,
                                        heartFillColor = null
                                    ),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(14.dp)
                                )
                                val isSelfRemix = originalOwner == post.authorDisplayName || originalOwner == "@${post.authorUsername}"
                                Text(
                                    text = if (isSelfRemix) "Community Remix • $effectiveLikes ${if (effectiveLikes == 1) "Like" else "Likes"}"
                                           else "Remix of $originalOwner",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Lucide.Sparkles,
                                    contentDescription = null,
                                    tint = Color(0xFFF5882E),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Featured Creation • $effectiveLikes ${if (effectiveLikes == 1) "Like" else "Likes"}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                )
                            }
                        }
                    }
                }

                // ── LOWER HALF: ROUNDED SHEET CARD ──
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 520.dp)
                        .offset(y = (-32).dp)
                        .graphicsLayer {
                            translationY = sheetSlideY
                        },
                    shape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp),
                    color = if (colors.isDark) Color(0xFF141721) else Color.White,
                    shadowElevation = 20.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    closeWithAnimation { onNavigateToProfile() }
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IosAvatarView(
                                    name = post.authorDisplayName.ifBlank { post.authorUsername },
                                    avatarUrl = post.authorAvatarUrl,
                                    size = 46.dp
                                )

                                Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = post.authorDisplayName.ifBlank { post.authorUsername.ifBlank { "Creator" } },
                                            color = if (colors.isDark) Color.White else Color(0xFF0F172A),
                                            fontSize = 15.sp,
                                            lineHeight = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (post.authorVerified || post.likesCount > 50) {
                                            Spacer(Modifier.width(4.dp))
                                            VerifiedBadge(size = 14.dp, userId = post.creatorId)
                                        }
                                    }
                                    Text(
                                        text = "@${post.authorUsername.ifBlank { "creator" }}",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.5.sp,
                                        lineHeight = 14.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(OneUiBlueLight.copy(alpha = 0.12f))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        closeWithAnimation { onNavigateToProfile() }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "View Profile",
                                    color = OneUiBlueLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = cleanTitle,
                                color = if (colors.isDark) Color.White else Color(0xFF0F172A),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.3).sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (!post.trendingTag.isNullOrBlank()) Lucide.Flame else Lucide.Sparkles,
                                    contentDescription = null,
                                    tint = Color(0xFFF5882E),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = displaySubtitle,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val statPillShape = RoundedCornerShape(14.dp)
                            val statBorder = BorderStroke(0.5.dp, if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(statPillShape)
                                    .background(if (colors.isDark) Color(0xFF1E2330) else Color(0xFFF8FAFC))
                                    .border(statBorder, statPillShape)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.Heart,
                                        contentDescription = null,
                                        tint = Color(0xFFFF2D55),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    val likeLabel = if (effectiveLikes == 1) "Like" else "Likes"
                                    Text(
                                        text = "$effectiveLikes $likeLabel",
                                        color = if (colors.isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(statPillShape)
                                    .background(if (colors.isDark) Color(0xFF1E2330) else Color(0xFFF8FAFC))
                                    .border(statBorder, statPillShape)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.MessageCircle,
                                        contentDescription = null,
                                        tint = OneUiBlueLight,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    val commentLabel = if (post.effectiveCommentsCount == 1) "Comment" else "Comments"
                                    Text(
                                        text = "${post.effectiveCommentsCount} $commentLabel",
                                        color = if (colors.isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(statPillShape)
                                    .background(if (colors.isDark) Color(0xFF1E2330) else Color(0xFFF8FAFC))
                                    .border(statBorder, statPillShape)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = rememberLucideHeartHandshakeIcon(
                                            isRemixed = false,
                                            strokeColor = Color(0xFF8B5CF6),
                                            heartFillColor = null
                                        ),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    val remixLabel = if (post.remixCount == 1) "Remix" else "Remixes"
                                    Text(
                                        text = "${post.remixCount} $remixLabel",
                                        color = if (colors.isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Text(
                            text = displayDesc,
                            color = if (colors.isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        if (post.tags.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(post.tags) { tag ->
                                    val cleanTag = tag.removePrefix("#")
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(OneUiBlueLight.copy(alpha = 0.10f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "#$cleanTag",
                                            color = OneUiBlueLight,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        scaleX = likeScale
                                        scaleY = likeScale
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        if (localIsLiked) Color(0xFFFF2D55).copy(alpha = 0.12f)
                                        else (if (colors.isDark) Color(0xFF1E2330) else Color(0xFFF1F5F9))
                                    )
                                    .border(
                                        1.dp,
                                        if (localIsLiked) Color(0xFFFF2D55).copy(alpha = 0.35f)
                                        else (if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)),
                                        CircleShape
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val nextLiked = !localIsLiked
                                        localIsLiked = nextLiked
                                        localLikesCountDelta += if (nextLiked) 1 else -1
                                        likeBounced = true
                                        onToggleLike?.invoke()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Heart,
                                    contentDescription = "Like",
                                    tint = if (localIsLiked) Color(0xFFFF2D55) else (if (colors.isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF334155)),
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1.15f)
                                    .height(50.dp)
                                    .graphicsLayer {
                                        scaleX = remixButtonPressScale
                                        scaleY = remixButtonPressScale
                                    }
                                    .shadow(
                                        8.dp,
                                        CircleShape,
                                        ambientColor = Color(0xFFF5882E).copy(alpha = 0.25f),
                                        spotColor = Color(0xFFF5882E).copy(alpha = 0.40f)
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFFA9848), Color(0xFFF5882E))
                                        )
                                    )
                                    .clickable(
                                        interactionSource = remixButtonInteractionSource,
                                        indication = null
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        closeWithAnimation { onRemix() }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = rememberLucideHeartHandshakeIcon(
                                            isRemixed = false,
                                            strokeColor = Color.White,
                                            heartFillColor = null
                                        ),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Remix Creation",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(0.95f)
                                    .height(50.dp)
                                    .graphicsLayer {
                                        scaleX = viewPostButtonPressScale
                                        scaleY = viewPostButtonPressScale
                                    }
                                    .clip(CircleShape)
                                    .background(if (colors.isDark) Color(0xFF1E2330) else Color(0xFFF1F5F9))
                                    .border(
                                        0.5.dp,
                                        if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                                        CircleShape
                                    )
                                    .clickable(
                                        interactionSource = viewPostButtonInteractionSource,
                                        indication = null
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        closeWithAnimation { onNavigateToPost() }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.MessageCircle,
                                        contentDescription = null,
                                        tint = colors.textMain,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "View Post",
                                        color = colors.textMain,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.navigationBarsPadding())
                        Spacer(Modifier.height(12.dp))
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
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Gray.copy(alpha = alpha))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp)
                        .clip(RoundedCornerShape(22.dp))
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
    isLiked: Boolean = false,
    searchQuery: String = "",
    accentColor: Color = OneUiBlueLight,
    onClick: () -> Unit,
    onDoubleTapLike: (() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val haptic = LocalHapticFeedback.current
    val doubleTapLikeState = rememberDoubleTapLikeState()
    var localIsLiked by remember(work.id, isLiked) { mutableStateOf(isLiked) }
    var localLikesCountDelta by remember(work.id) { mutableIntStateOf(0) }
    var layers by remember(work.jsonUrl) { mutableStateOf<List<GradientLayer>?>(LayerCache.get(work.jsonUrl)) }

    LaunchedEffect(isLiked) {
        localIsLiked = isLiked
    }

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
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) OneUiCardDark else OneUiCardLight)
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
                RoundedCornerShape(22.dp)
            )
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(17.dp))
                .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                .pointerInput(work.id) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onDoubleTap = { offset ->
                            if (!localIsLiked) {
                                localIsLiked = true
                                localLikesCountDelta += 1
                                onDoubleTapLike?.invoke()
                            }
                            doubleTapLikeState.trigger(offset)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
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
                                listOf(OneUiBlueLight.copy(alpha = 0.6f), Color(0xFF5856D6).copy(alpha = 0.6f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AiWaveSphereIcon(modifier = Modifier.size(28.dp))
                }
            }

            FlatDoubleTapHeartOverlay(
                state = doubleTapLikeState,
                modifier = Modifier.fillMaxSize(),
                heartSize = 65.dp
            )
        }

        Spacer(Modifier.height(7.dp))

        Text(
            text = rememberHighlightedText(work.title.ifBlank { "Creation" }, searchQuery, accentColor),
            color = colors.textMain,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rememberHighlightedText("@${work.authorUsername.ifBlank { "creator" }}", searchQuery, accentColor),
                color = Color(0xFF8E8E93),
                fontSize = 11.5.sp,
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            val effectiveLikes = (work.likesCount + localLikesCountDelta).coerceAtLeast(0)
            if (effectiveLikes > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Heart,
                        contentDescription = null,
                        tint = if (localIsLiked) Color(0xFFFF2D55) else Color(0xFF8E8E93),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = effectiveLikes.toString(),
                        color = if (localIsLiked) Color(0xFFFF2D55) else Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}