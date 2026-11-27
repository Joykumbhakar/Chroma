@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.chroma.studio.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.chroma.studio.ui.theme.LightPalette
import com.chroma.studio.ui.theme.DarkPalette
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.viewmodel.DiscoverViewModel
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── One UI Design Tokens (Delegated to OscellionPalette tokens) ──────────────────────────────────────────────────────
private val OneUiBlueLight get() = LightPalette.primary
private val OneUiBlueDark get() = DarkPalette.primary
private val OneUiSurfaceLight get() = LightPalette.cardSurfaceElevated
private val OneUiSurfaceDark get() = DarkPalette.bg
private val OneUiCardLight get() = LightPalette.cardSurface
private val OneUiCardDark get() = DarkPalette.cardSurface
private val OneUiPillLight get() = LightPalette.inputBg
private val OneUiPillDark get() = DarkPalette.inputBg

/**
 * Apple-style Specular Glass Rim Border Brush.
 * Simulates top-left light catch and subtle bottom-right refraction across dark and light surfaces.
 */
@Composable
fun rememberAppleStyleBorderBrush(
    isDark: Boolean,
    isFocused: Boolean = false,
    accentColor: Color = OneUiBlueLight
): Brush = remember(isDark, isFocused, accentColor) {
    if (isFocused) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.95f),
                Color.White.copy(alpha = if (isDark) 0.55f else 0.90f),
                accentColor.copy(alpha = 0.70f)
            ),
            start = Offset(0f, 0f),
            end = Offset(900f, 900f)
        )
    } else if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.03f),
                Color.White.copy(alpha = 0.16f)
            ),
            start = Offset(0f, 0f),
            end = Offset(800f, 800f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color.Black.copy(alpha = 0.07f),
                Color.Black.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.75f)
            ),
            start = Offset(0f, 0f),
            end = Offset(800f, 800f)
        )
    }
}

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
                Color(0xFF0381FE), // Blue
                Color(0xFF0072DE), // Brand Cobalt
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
                Color(0xFF0381FE),
                Color(0xFF0072DE),
                Color(0xFF06B6D4),
                Color(0xFF22C55E)
            )
        )
    }
    val bgBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF0381FE).copy(alpha = 0.18f),
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
    val colors = LocalOscellionColors.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }

    val shellGradient = remember(colors.isDark, isFocused, colors.primary) {
        if (isFocused) {
            Brush.linearGradient(
                listOf(
                    colors.primary,
                    Color(0xFF38BDF8),
                    Color(0xFF22D3EE),
                    Color(0xFF4ADE80)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    colors.primary.copy(alpha = 0.55f),
                    Color(0xFF06B6D4).copy(alpha = 0.45f),
                    Color(0xFF22C55E).copy(alpha = 0.45f)
                )
            )
        }
    }

    val containerBg = if (isFocused || query.isNotEmpty()) colors.cardSurfaceElevated else colors.inputBg
    val textColor = colors.textMain
    val placeholderColor = colors.textMuted.copy(alpha = 0.78f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 6.dp),
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
                        fontSize = 14.5.sp,
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
                        fontSize = 14.5.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        onSearchAction()
                    }),
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
                        .clip(RoundedCornerShape(6.dp))
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
                onClick = {
                    keyboardController?.hide()
                    onSearchAction()
                },
                isDark = colors.isDark
            )
        }
    }
}

/**
 * Standard One UI Search Bar Pill Component using theme tokens.
 * Features:
 * - 46.dp One UI capsule search bar with subtle border only when focused/active
 * - Leading back arrow when in focused search mode, or magnifying glass icon
 * - Instant clear ('X') button when query is non-empty, or quick '✦' AI mode chip
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
    onQuickAiToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var hasFocus by remember { mutableStateOf(false) }

    val pillScale by animateFloatAsState(
        targetValue = if (hasFocus) 1.008f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "pillScale"
    )

    val pillBgColor by animateColorAsState(
        targetValue = if (hasFocus || isFocusedMode) colors.cardSurface else colors.inputBg,
        animationSpec = tween(180),
        label = "pillBg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            hasFocus -> accentColor.copy(alpha = 0.45f)
            isFocusedMode -> colors.cardBorder
            else -> colors.cardBorderSubtle
        },
        animationSpec = tween(180),
        label = "pillBorder"
    )

    val textColor = colors.textMain
    val hintColor = colors.textMuted.copy(alpha = 0.82f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .graphicsLayer {
                scaleX = pillScale
                scaleY = pillScale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(pillBgColor)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isFocusedMode) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(colors.textMain.copy(alpha = 0.06f))
                    .clickable {
                        keyboardController?.hide()
                        onExitFocusMode()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = "Back",
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Icon(
                imageVector = Lucide.Search,
                contentDescription = "Search",
                tint = if (hasFocus) accentColor else hintColor,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp)
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
                    fontSize = 14.5.sp,
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
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFontFamily
                ),
                cursorBrush = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    onSearchAction()
                }),
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
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
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
        } else if (onQuickAiToggle != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .clickable { onQuickAiToggle() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GalaxyAiSparkleIcon(size = 13.dp)
                Text(
                    text = "AI",
                    color = accentColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
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
    val cleanQuery = query.trim().removePrefix("#").removePrefix("@").trim()
    if (cleanQuery.isBlank() || fullText.isBlank()) {
        buildAnnotatedString { append(fullText) }
    } else {
        val lowerFull = fullText.lowercase()
        val tokens = cleanQuery.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
            .distinct()

        val intervals = mutableListOf<IntRange>()
        for (token in tokens) {
            var startIndex = 0
            while (startIndex < fullText.length) {
                val matchIdx = lowerFull.indexOf(token, startIndex)
                if (matchIdx == -1) break
                intervals.add(matchIdx until (matchIdx + token.length))
                startIndex = matchIdx + token.length
            }
        }

        if (intervals.isEmpty()) {
            buildAnnotatedString { append(fullText) }
        } else {
            intervals.sortBy { it.first }
            val mergedIntervals = mutableListOf<IntRange>()
            var current = intervals[0]
            for (i in 1 until intervals.size) {
                val next = intervals[i]
                if (next.first <= current.last + 1) {
                    current = current.first..maxOf(current.last, next.last)
                } else {
                    mergedIntervals.add(current)
                    current = next
                }
            }
            mergedIntervals.add(current)

            buildAnnotatedString {
                var currentIndex = 0
                for (range in mergedIntervals) {
                    if (range.first > currentIndex) {
                        append(fullText.substring(currentIndex, range.first))
                    }
                    withStyle(
                        SpanStyle(
                            color = highlightColor,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(fullText.substring(range.first, minOf(range.last + 1, fullText.length)))
                    }
                    currentIndex = range.last + 1
                }
                if (currentIndex < fullText.length) {
                    append(fullText.substring(currentIndex))
                }
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
    LATEST("Latest"),
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
    onRequireLogin: () -> Unit = {},
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
    var showClearHistoryConfirmDialog by remember { mutableStateOf(false) }

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

    // Debounced User Search with instant local cache + parallel case-insensitive Firestore lookup
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            userResults = emptyList()
            isSearchingUsers = false
            return@LaunchedEffect
        }
        val cleanQ = q.removePrefix("@").trim()
        if (cleanQ.isNotBlank()) {
            // Instant 0ms local match from in-memory works to eliminate perceived latency
            val localMatches = allWorks.mapNotNull { work ->
                if (work.creatorId.isBlank()) null
                else {
                    val uName = work.authorUsername
                    val dName = work.authorDisplayName
                    val matches = uName.contains(cleanQ, ignoreCase = true) ||
                                  dName.contains(cleanQ, ignoreCase = true)
                    if (matches) {
                        UserProfile(
                            uid = work.creatorId,
                            username = work.authorUsername,
                            displayName = work.authorDisplayName,
                            avatarUrl = work.authorAvatarUrl,
                            verified = work.authorVerified
                        )
                    } else null
                }
            }.distinctBy { it.uid }

            if (localMatches.isNotEmpty()) {
                userResults = localMatches
            } else if (userResults.isNotEmpty()) {
                userResults = emptyList()
            }
        }
        isSearchingUsers = true
        delay(150)
        try {
            val remoteResults = withContext(Dispatchers.IO) {
                discoverVm.repository.searchUsers(q)
            }
            if (remoteResults.isNotEmpty()) {
                val remoteIds = remoteResults.map { it.uid }.toSet()
                val merged = remoteResults + userResults.filter { it.uid !in remoteIds }
                userResults = merged
            } else if (userResults.isEmpty()) {
                userResults = emptyList()
            }
        } catch (_: Exception) {
            // Keep local matches if any, else empty
        } finally {
            isSearchingUsers = false
        }
    }

    // Filtered Artworks by Query, Category & One UI Sort Order
    val filteredArtworks = remember(allWorks, trendingWorks, searchQuery, selectedCategory, sortOrder) {
        val baseList = when (selectedCategory) {
            "Trending" -> if (trendingWorks.isNotEmpty()) trendingWorks else allWorks
            "Gradients" -> allWorks.filter { it.postType == "work" || it.title.contains("gradient", true) || it.tags.any { t -> t.contains("gradient", true) } }
            "Photos" -> allWorks.filter { it.postType == "image" || !it.imageUrl.isNullOrBlank() }
            "3D Meshes" -> allWorks.filter { it.title.contains("mesh", true) || it.title.contains("3d", true) || it.tags.any { t -> t.contains("mesh", true) || t.contains("3d", true) } }
            "Glassmorphism" -> allWorks.filter { it.title.contains("glass", true) || it.tags.any { t -> t.contains("glass", true) } }
            "Minimal" -> allWorks.filter { it.title.contains("minimal", true) || it.tags.any { t -> t.contains("minimal", true) } }
            "Cyberpunk" -> allWorks.filter { it.title.contains("cyber", true) || it.title.contains("neon", true) || it.tags.any { t -> t.contains("cyber", true) || t.contains("neon", true) } }
            "Aurora" -> allWorks.filter { it.title.contains("aurora", true) || it.tags.any { t -> t.contains("aurora", true) } }
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
            OneUiSortOrder.LATEST -> queriedList.sortedByDescending { it.timestamp }
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

    // BackHandler: Hardware/system back dismisses search first
    BackHandler(enabled = isSearchActive) {
        searchQuery = ""
        isSearchFieldFocused = false
        focusManager.clearFocus()
    }

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

    // ── Samsung One UI 6/7 Expanded Header Metrics (Matching Community Tab) ────
    val maxViewingHeight = 240.dp
    val maxCollapsePx = remember(density) { with(density) { maxViewingHeight.toPx() } }

    // Restore exact collapsed/expanded fraction from SharedPreferences (default: collapsed = 1f)
    var collapseOffsetPx by remember(maxCollapsePx) {
        val savedFraction = prefs.getFloat("oneui_search_appbar_collapse_fraction", 1f).coerceIn(0f, 1f)
        mutableFloatStateOf(savedFraction * maxCollapsePx)
    }

    // Persist state whenever the user leaves the screen or finishes snapping
    DisposableEffect(Unit) {
        onDispose {
            val fraction = if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx).coerceIn(0f, 1f)
            prefs.edit().putFloat("oneui_search_appbar_collapse_fraction", fraction).apply()
        }
    }

    val collapseProgress by remember {
        derivedStateOf {
            if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx).coerceIn(0f, 1f)
        }
    }

    // Samsung One UI 6/7 Expanded Header Metrics (Collapses smoothly on scroll)
    val currentViewingHeight = (maxViewingHeight * (1f - collapseProgress)).coerceAtLeast(0.dp)
    val largeTitleAlpha = (1f - (collapseProgress / 0.40f)).coerceIn(0f, 1f)
    val subtitleAlpha = (1f - (collapseProgress / 0.30f)).coerceIn(0f, 1f)
    val compactTitleAlpha = ((collapseProgress - 0.45f) / 0.55f).coerceIn(0f, 1f)

    fun animateSnapTo(targetFraction: Float) {
        val clamped = targetFraction.coerceIn(0f, 1f)
        val targetPx = clamped * maxCollapsePx
        prefs.edit().putFloat("oneui_search_appbar_collapse_fraction", clamped).apply()
        coroutineScope.launch {
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

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            animateSnapTo(1f)
        }
    }

    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()

    // NestedScrollConnection with One UI Magnetic Auto-Snap Physics
    var snapJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val nestedScrollConnection = remember(maxCollapsePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag && snapJob?.isActive == true) {
                    snapJob?.cancel()
                }
                val deltaY = available.y
                // When user scrolls down (finger moves up -> deltaY < 0): collapse viewing area first
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
                val isAtTop = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                // When user scrolls up (finger moves down -> deltaY > 0) and list is at the very top: expand header
                if (deltaY > 0f && collapseOffsetPx > 0f && isAtTop) {
                    val newOffset = (collapseOffsetPx - deltaY).coerceIn(0f, maxCollapsePx)
                    val consumedY = -(newOffset - collapseOffsetPx)
                    collapseOffsetPx = newOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val progress = if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx)
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
                    val isAtTop = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                    val targetPx = when {
                        available.y < -250f -> maxCollapsePx
                        available.y > 250f && isAtTop -> 0f
                        progress >= 0.45f -> maxCollapsePx
                        else -> 0f
                    }
                    val targetFraction = if (maxCollapsePx <= 0f) 0f else (targetPx / maxCollapsePx).coerceIn(0f, 1f)
                    prefs.edit().putFloat("oneui_search_appbar_collapse_fraction", targetFraction).apply()
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

    // Theme & One UI Palette Tokens (from LocalOscellionColors)
    val headerBgColor = colors.bg
    val screenBgColor = colors.bg
    val textPrimary = colors.textMain
    val textDark = colors.textMain
    val textMuted = colors.textMuted
    val cardBgColor = colors.cardSurface
    val dividerColor = colors.cardBorderSubtle
    val activeAccent = if (gradientStyle == OneUiGradientStyle.SOLID) colors.primary else gradientStyle.primaryColor
    val keyboardController = LocalSoftwareKeyboardController.current
    val hasScrolledGrid by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 8
        }
    }
    val targetHeaderDividerAlpha = when {
        isSearchActive -> 1f
        hasScrolledGrid -> 1f
        collapseProgress > 0.80f -> ((collapseProgress - 0.80f) / 0.20f).coerceIn(0f, 1f)
        else -> 0f
    }
    val animatedHeaderDividerAlpha by animateFloatAsState(
        targetValue = targetHeaderDividerAlpha,
        animationSpec = tween(durationMillis = 180),
        label = "headerHrAlpha"
    )

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
            resolvedHeaderTitle.length > 28 -> 26.sp
            resolvedHeaderTitle.length > 18 -> 32.sp
            else -> 38.sp
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
            .nestedScroll(nestedScrollConnection)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Samsung One UI Header (Exact same layout & styling as Community Tab) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBgColor)
                    .statusBarsPadding()
            ) {
                // 1. EXPANDED VIEWING AREA (Matches Samsung One UI Settings & Community Tab reference geometry)
                if (currentViewingHeight > 0.dp && !isSearchActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(currentViewingHeight)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = { snapJob?.cancel() },
                                    onDragEnd = {
                                        val progress = if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx)
                                        val targetFraction = if (progress >= 0.45f) 1f else 0f
                                        animateSnapTo(targetFraction)
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        collapseOffsetPx = (collapseOffsetPx - dragAmount).coerceIn(0f, maxCollapsePx)
                                    }
                                )
                            },
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
                                        translationY = -collapseProgress * with(density) { 24.dp.toPx() }
                                        val scale = 1f - (collapseProgress * 0.06f)
                                        scaleX = scale
                                        scaleY = scale
                                    }
                            ) {
                                Text(
                                    text = resolvedHeaderTitle,
                                    fontSize = extendedTitleFontSize,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.6).sp,
                                    color = textPrimary,
                                    textAlign = TextAlign.Center
                                )

                                val subtitleText = if (isSearchActive) "Search creations & creators"
                                else if (useDynamicTitle) "${selectedCategory.lowercase().replaceFirstChar { it.uppercase() }} creations"
                                else "Discover creations & creators"

                                if (subtitleText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = subtitleText,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = textMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. COMPACT ACTION BAR (Pinned 52dp height, One UI 16dp keyline)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // LEFT: Compact title (fades in when collapsing, matching One UI)
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .graphicsLayer { alpha = if (isSearchActive) 1f else compactTitleAlpha },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSearchActive) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    isSearchFieldFocused = false
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.ArrowLeft,
                                    contentDescription = "Exit search",
                                    tint = textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = resolvedHeaderTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // RIGHT: More options action button
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showMoreOptionsMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.EllipsisVertical,
                                    contentDescription = "More options",
                                    tint = textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreOptionsMenu,
                                onDismissRequest = { showMoreOptionsMenu = false },
                                offset = DpOffset(x = 0.dp, y = 6.dp),
                                modifier = Modifier
                                    .width(240.dp)
                                    .background(
                                        color = colors.cardSurface,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = colors.cardBorderSubtle,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Text(
                                    text = "Explore options",
                                    color = textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
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
                                    thickness = 0.5.dp,
                                    color = dividerColor,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Refresh Feed",
                                            color = textPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Lucide.RotateCw, null, tint = activeAccent, modifier = Modifier.size(16.dp))
                                    },
                                    onClick = {
                                        showMoreOptionsMenu = false
                                        handleRefresh()
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Theme: ${gradientStyle.label}",
                                            color = textPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (gradientStyle == OneUiGradientStyle.SOLID) colors.primary
                                                    else gradientStyle.orbColor
                                                )
                                        )
                                    },
                                    onClick = {
                                        cycleGradientStyle()
                                    }
                                )

                                if (searchHistory.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Clear Search History",
                                                color = colors.error,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Lucide.Trash2, null, tint = colors.error, modifier = Modifier.size(16.dp))
                                        },
                                        onClick = {
                                            showMoreOptionsMenu = false
                                            showClearHistoryConfirmDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. PINNED SEARCH BAR (One UI Interaction Dock under compact bar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 10.dp)
                ) {
                    if (useGalaxyAiSearchBar) {
                        GalaxyAiAuroraSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearchAction = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
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
                            onSearchAction = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
                            isFocusedMode = isSearchFieldFocused || searchQuery.isNotBlank(),
                            onExitFocusMode = {
                                searchQuery = ""
                                isSearchFieldFocused = false
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
                            isDark = isDark,
                            accentColor = activeAccent,
                            placeholder = "Search artworks, styles, creators...",
                            focusRequester = searchFocusRequester,
                            onFocusChange = { focused ->
                                isSearchFieldFocused = focused
                            },
                            onQuickAiToggle = {
                                useGalaxyAiSearchBar = true
                                prefs.edit().putBoolean("oneui_search_galaxy_ai_bar", true).apply()
                            }
                        )
                    }
                }

                // 4. SAMSUNG ONE UI HEADER HAIRLINE DIVIDER (Fixed alpha scaling — crisp 0.5dp hr line)
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = colors.textMain.copy(
                        alpha = (if (isDark) 0.12f else 0.08f) * animatedHeaderDividerAlpha
                    )
                )
            }

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
                    OscellionLoadingSpinner(color = activeAccent, modifier = Modifier.size(20.dp))
                }
            }

            // ── 5. Main Staggered Grid Content (One UI 26.dp Focus Blocks & 22.dp Cards) ──
            LazyVerticalStaggeredGrid(
                state = gridState,
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isSearchActive) {
                    // ── 5A. In Search Mode: One UI Scope Filter Subtabs ────
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchScope.entries.forEach { scope ->
                                val isSelected = scope == selectedScope
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) activeAccent else colors.inputBg)
                                        .clickable { selectedScope = scope },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = scope.label,
                                        color = if (isSelected) colors.onPrimary else textDark,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }
                        }
                    }

                    // ── Search Mode: Open Borderless Recent Searches & Suggested Prompts ──
                    if (searchQuery.isBlank()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
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
                                                showClearHistoryConfirmDialog = true
                                            }
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(searchHistory, key = { "hist_${it.uid}" }) { recentUser ->
                                            Row(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(colors.inputBg)
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
                                                if (recentUser.verified) {
                                                    VerifiedBadge(
                                                        size = 12.dp,
                                                        tone = recentUser.verifiedTone,
                                                        userId = recentUser.uid
                                                    )
                                                }
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
                    }

                    // ── Search Mode: Creators Section ────────────────────────
                    if (selectedScope == SearchScope.ALL || selectedScope == SearchScope.CREATORS) {
                        if (isSearchingUsers && userResults.isEmpty()) {
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 6.dp, top = 6.dp, bottom = 2.dp, end = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Creators (${userResults.size})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textMuted,
                                        fontFamily = InterFontFamily
                                    )
                                    if (isSearchingUsers) {
                                        CircularProgressIndicator(
                                            color = activeAccent,
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 1.5.dp
                                        )
                                    }
                                }
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

                    // ── Search Mode: Matching Tags (One UI 26.dp Focus Block) ────
                    if ((selectedScope == SearchScope.ALL || selectedScope == SearchScope.TAGS) && matchingTags.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(colors.sheetCornerRadius))
                                    .background(cardBgColor)
                                    .border(0.5.dp, dividerColor, RoundedCornerShape(colors.sheetCornerRadius))
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

                    // ── Search Mode: Artworks Header & Grid ──────────────────
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
                                            discoverVm.toggleLike(work.id, uid, forceLike = true)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // ── 5B. In Explore Mode: One UI Category Subtabs ────
                    item(span = StaggeredGridItemSpan.FullLine) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            items(EXPLORE_CATEGORIES) { filter ->
                                val isSelected = filter == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) activeAccent else colors.inputBg)
                                        .clickable {
                                            selectedCategory = filter
                                            SoundManager.playTap(context)
                                        }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (filter == "Trending") {
                                            AppleTrendingFireIcon(modifier = Modifier.size(15.dp))
                                        } else if (filter == "Photos") {
                                            ApplePhotosIcon(modifier = Modifier.size(15.dp))
                                        }
                                        Text(
                                            text = filter,
                                            color = if (isSelected) colors.onPrimary else textDark,
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

                    // ── Explore Mode: Community Trending Tags (One UI 26.dp Focus Block) ──
                    if (matchingTags.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(colors.sheetCornerRadius))
                                    .background(cardBgColor)
                                    .border(0.5.dp, dividerColor, RoundedCornerShape(colors.sheetCornerRadius))
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
                                                .background(colors.inputBg)
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

                    // ── Explore Mode: Creations Subheader with Inline Sort Control ──
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selectedCategory == "Trending") {
                                    AppleTrendingFireIcon(modifier = Modifier.size(17.dp))
                                }
                                Text(
                                    text = if (selectedCategory == "All") "Explore Creations" else "$selectedCategory Creations",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textMuted,
                                    fontFamily = InterFontFamily
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(cardBgColor)
                                        .border(0.5.dp, dividerColor, CircleShape)
                                        .clickable {
                                            val orders = OneUiSortOrder.entries
                                            val nextSort = orders[(orders.indexOf(sortOrder) + 1) % orders.size]
                                            sortOrder = nextSort
                                            prefs.edit().putString("oneui_search_sort_order", nextSort.name).apply()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.ArrowUpDown,
                                        contentDescription = "Sort order",
                                        tint = activeAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = sortOrder.label,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = activeAccent,
                                        fontFamily = InterFontFamily
                                    )
                                }
                                Text(
                                    text = "${filteredArtworks.size} ${if (filteredArtworks.size == 1) "item" else "items"}",
                                    fontSize = 12.sp,
                                    color = textMuted,
                                    fontFamily = InterFontFamily
                                )
                            }
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
                                        discoverVm.toggleLike(work.id, uid, forceLike = true)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showClearHistoryConfirmDialog) {
            com.chroma.studio.ui.components.IosAlertDialog(
                title = "Clear Search History?",
                message = "Remove all recent searches from this device?",
                confirmText = "Clear History",
                isDestructive = true,
                onConfirm = {
                    showClearHistoryConfirmDialog = false
                    historyManager.clearHistory()
                    searchHistory = emptyList()
                },
                dismissText = "Cancel",
                onDismiss = { showClearHistoryConfirmDialog = false }
            )
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
                    if (!uid.isNullOrBlank() && !com.chroma.studio.data.SavedAccountManager.isGuest(context)) {
                        val willBeLiked = !likedWorkIds.contains(post.id)
                        discoverVm.toggleLike(post.id, uid)
                        selectedPreviewPost = selectedPreviewPost?.let { p ->
                            if (p.id == post.id) {
                                val newLikedBy = if (willBeLiked) (p.likedBy + uid).distinct() else (p.likedBy - uid)
                                p.copy(
                                    likesCount = maxOf(0, p.likesCount + if (willBeLiked) 1 else -1),
                                    likedBy = newLikedBy
                                )
                            } else p
                        }
                    } else {
                        onRequireLogin()
                    }
                },
                onForceLike = {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (!uid.isNullOrBlank() && !com.chroma.studio.data.SavedAccountManager.isGuest(context)) {
                        discoverVm.toggleLike(post.id, uid, forceLike = true)
                        selectedPreviewPost = selectedPreviewPost?.let { p ->
                            if (p.id == post.id) {
                                val newLikedBy = (p.likedBy + uid).distinct()
                                val delta = if (p.likedBy.contains(uid)) 0 else 1
                                p.copy(
                                    likesCount = p.likesCount + delta,
                                    likedBy = newLikedBy
                                )
                            } else p
                        }
                    } else {
                        onRequireLogin()
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
                    if (com.chroma.studio.data.SavedAccountManager.isGuest(context)) {
                        onRequireLogin()
                    } else {
                        onRemixWork?.invoke(workToRemix)
                    }
                }
            )
        }
    }
}

// ── Creator Search Result Card (One UI 26.dp Focus Block + Live Highlighting) ──
@Composable
private fun CreatorSearchResultCard(
    user: UserProfile,
    searchQuery: String = "",
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    accentColor: Color = OneUiBlueLight,
    onClick: () -> Unit
) {
    val displayName = user.displayName.ifBlank { user.username }
    val cardShape = RoundedCornerShape(colors.sheetCornerRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.cardSurface)
            .border(0.5.dp, colors.cardBorderSubtle, cardShape)
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
                val searchUserPresence = com.chroma.studio.data.OnlineStatusManager.rememberUserPresence(user.uid)
                val searchUserIsOnline = searchUserPresence.online && user.showOnlineStatus
                IosAvatarView(
                    name = user.displayName,
                    avatarUrl = user.avatarUrl,
                    size = 44.dp,
                    isOnline = searchUserIsOnline
                )

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
                            VerifiedBadge(
                                size = 14.dp,
                                tone = user.verifiedTone,
                                userId = user.uid
                            )
                        }
                    }
                    Text(
                        text = rememberHighlightedText("@${user.username}", searchQuery, accentColor),
                        color = colors.textMuted,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        fontFamily = InterFontFamily
                    )
                    if (user.bio.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = rememberHighlightedText(user.bio, searchQuery, accentColor),
                            color = colors.textMain.copy(alpha = 0.72f),
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
                    .background(accentColor)
                    .clickable { onClick() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "View",
                    color = colors.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

// ── Spotlight Creator Banner (Borderless Flat Apple Section) ────────────────
@Composable
private fun SpotlightCreatorBanner(
    post: DiscoverPost,
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    accentColor: Color = OneUiBlueLight,
    onClick: () -> Unit
) {
    val bannerShape = RoundedCornerShape(colors.cardCornerRadius)
    val bannerBg = colors.cardSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(bannerShape)
            .background(bannerBg)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF9500), Color(0xFFFF2D55), accentColor)
                            )
                        )
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IosAvatarView(
                        name = post.authorDisplayName,
                        avatarUrl = post.authorAvatarUrl,
                        size = 48.dp
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFFF9500).copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = "✦ Featured Creator",
                                color = Color(0xFFFF9500),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorDisplayName.ifBlank { post.authorUsername },
                            color = colors.textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.authorVerified) {
                            Spacer(Modifier.width(4.dp))
                            VerifiedBadge(
                                size = 14.dp,
                                tone = post.authorVerifiedTone,
                                userId = post.creatorId
                            )
                        }
                    }
                    Text(
                        text = "@${post.authorUsername} • ${post.title.ifBlank { "Studio Masterwork" }}",
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable { onClick() }
                    .padding(horizontal = 15.dp, vertical = 7.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Studio",
                    color = colors.onPrimary,
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
    onToggleLike: (() -> Unit)? = null,
    onForceLike: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var localIsLiked by remember(post.id, isLiked) { mutableStateOf(isLiked) }
    LaunchedEffect(post.id, isLiked) {
        localIsLiked = isLiked
    }

    val effectiveJson = remember(post.jsonUrl, post.workUrls) {
        post.jsonUrl.ifBlank { post.workUrls.firstOrNull().orEmpty() }
    }
    var layers by remember(effectiveJson) { mutableStateOf<List<GradientLayer>?>(LayerCache.get(effectiveJson)) }
    val doubleTapLikeState = rememberDoubleTapLikeState()

    val mediaSlides = remember(post) { resolvePostMediaSlides(post) }
    val isVideo = remember(post) { isVideoPost(post) }
    val hasMultiSlides = mediaSlides.size > 1
    var isFeedMuted by remember { mutableStateOf(false) }

    var showOverlayChrome by remember { mutableStateOf(true) }
    var showDetailsDrawer by remember { mutableStateOf(false) }
    var isFitContent by remember { mutableStateOf(true) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var dismissDragOffsetY by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
        zoomScale = nextScale
        panOffset = if (nextScale > 1.02f) {
            panOffset + panChange
        } else {
            Offset.Zero
        }
    }

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
                delay(180)
                onDismiss()
                action?.invoke()
            }
        }
    }

    BackHandler {
        if (showDetailsDrawer) {
            showDetailsDrawer = false
        } else if (zoomScale > 1.05f) {
            zoomScale = 1f
            panOffset = Offset.Zero
        } else {
            closeWithAnimation(null)
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
        label = "fullScreenAlpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "fullScreenScale"
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

    val displaySubtitle = remember(post.trendingTag, post.postType, isRemixPost, isVideo) {
        when {
            !post.trendingTag.isNullOrBlank() -> post.trendingTag
            isRemixPost -> "Community Studio Remix"
            isVideo -> "Flash Video"
            post.postType == "image" -> "Community Photography"
            else -> "Original Studio Masterwork"
        }
    }

    val displayDesc = remember(post.description) {
        if (post.description.isNotBlank()) post.description
        else "Experience this creative masterwork crafted with layered vector art and fluid effects on Chroma Studio."
    }

    val currentUserId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
    val effectiveLikes = remember(post.likesCount, post.likedBy, localIsLiked, currentUserId) {
        val hasUserLikedInPost = !currentUserId.isNullOrBlank() && post.likedBy.contains(currentUserId)
        val delta = when {
            localIsLiked && !hasUserLikedInPost -> 1
            !localIsLiked && hasUserLikedInPost -> -1
            else -> 0
        }
        maxOf(0, post.likesCount + delta)
    }

    val resolvedImageUrl = remember(post) {
        post.imageUrl?.takeIf { it.isNotBlank() }
            ?: post.imageUrls.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: post.thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: post.workUrls.firstOrNull { it.startsWith("http") && !it.endsWith(".json") }
    }

    val triggerDoubleTapLike: (Offset) -> Unit = { offset ->
        if (!localIsLiked) {
            localIsLiked = true
            likeBounced = true
            if (onForceLike != null) {
                onForceLike()
            } else {
                onToggleLike?.invoke()
            }
        }
        doubleTapLikeState.trigger(offset)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Dialog(
        onDismissRequest = { closeWithAnimation(null) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // Pure One UI Dark Gallery Viewer Stage (#000000 edge-to-edge)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = contentAlpha))
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = contentScale
                    scaleY = contentScale
                    translationY = dismissDragOffsetY
                }
        ) {
            // ── 1. TRUE FULL-SCREEN MEDIA VIEWPORT ────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!isVideo && !hasMultiSlides && !resolvedImageUrl.isNullOrBlank()) {
                            Modifier.transformable(state = transformState)
                        } else Modifier
                    )
                    .pointerInput(post.id, zoomScale, showDetailsDrawer) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dismissDragOffsetY > 140f && zoomScale <= 1.02f) {
                                    closeWithAnimation(null)
                                } else {
                                    dismissDragOffsetY = 0f
                                }
                            },
                            onDragCancel = { dismissDragOffsetY = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                if (zoomScale <= 1.02f && !showDetailsDrawer && (dragAmount > 0f || dismissDragOffsetY > 0f)) {
                                    dismissDragOffsetY = (dismissDragOffsetY + dragAmount).coerceAtLeast(0f)
                                }
                            }
                        )
                    }
                    .pointerInput(post.id, isVideo, hasMultiSlides) {
                        detectTapGestures(
                            onTap = {
                                if (showDetailsDrawer) {
                                    showDetailsDrawer = false
                                } else {
                                    showOverlayChrome = !showOverlayChrome
                                }
                            },
                            onDoubleTap = { offset ->
                                if (zoomScale > 1.05f && !isVideo && !hasMultiSlides && !resolvedImageUrl.isNullOrBlank()) {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    triggerDoubleTapLike(offset)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if ((isVideo || hasMultiSlides) && repository != null && mediaSlides.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { mediaSlides.size })
                    PostMediaCarousel(
                        work = post,
                        slides = mediaSlides,
                        pagerState = pagerState,
                        repository = repository,
                        colors = colors,
                        doubleTapLikeState = doubleTapLikeState,
                        onDoubleTap = triggerDoubleTapLike,
                        onVideoClick = {
                            if (showDetailsDrawer) showDetailsDrawer = false
                            else showOverlayChrome = !showOverlayChrome
                        },
                        isInViewport = true,
                        isFeedMuted = isFeedMuted,
                        onToggleFeedMute = { isFeedMuted = !isFeedMuted },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!resolvedImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(resolvedImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = cleanTitle,
                        contentScale = if (isFitContent) ContentScale.Fit else ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            }
                    )
                } else if (layers != null && layers!!.isNotEmpty()) {
                    CanvasPreview(
                        layers = layers!!,
                        shape = "full",
                        borderColor = Color.Transparent,
                        isStatic = false,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF090D16), Color(0xFF111827), colors.primary.copy(alpha = 0.45f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        OscellionLoadingSpinner(color = colors.primary, modifier = Modifier.size(34.dp))
                    }
                }

                FlatDoubleTapHeartOverlay(
                    state = doubleTapLikeState,
                    modifier = Modifier.fillMaxSize(),
                    heartSize = 104.dp
                )
            }

            // ── 2. FLOATING TOP ONE UI BAR (Auto-hides on canvas tap) ────────
            AnimatedVisibility(
                visible = showOverlayChrome && !showDetailsDrawer,
                enter = fadeIn(tween(180)) + slideInVertically { -it / 2 },
                exit = fadeOut(tween(150)) + slideOutVertically { -it / 2 },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Back / Close Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.48f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                closeWithAnimation(null)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.ArrowLeft,
                            contentDescription = "Close full screen",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center Status Pill
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.48f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when {
                            post.isTrendingMasterwork || post.likesCount > 50 -> {
                                AppleTrendingFireIcon(modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Trending",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily
                                )
                            }
                            isVideo -> {
                                Icon(Lucide.Play, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Flash Video",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily
                                )
                            }
                            isRemixPost -> {
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
                                Text(
                                    text = "Remix",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily
                                )
                            }
                            else -> {
                                Icon(Lucide.Sparkles, null, tint = colors.primary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Explore",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }

                    // Right Controls: Fit/Fill Toggle (for single images) + Share
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isVideo && !hasMultiSlides && !resolvedImageUrl.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.48f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                                    .clickable {
                                        if (zoomScale > 1.05f) {
                                            zoomScale = 1f
                                            panOffset = Offset.Zero
                                        } else {
                                            isFitContent = !isFitContent
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFitContent && zoomScale <= 1.05f) Lucide.Maximize2 else Lucide.Minimize2,
                                    contentDescription = if (isFitContent) "Fill screen" else "Fit screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.48f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── 3. BOTTOM FLOATING INTERACTION DOCK (Thumb-Reachable One UI Controls) ──
            AnimatedVisibility(
                visible = showOverlayChrome && !showDetailsDrawer,
                enter = fadeIn(tween(180)) + slideInVertically { it / 2 },
                exit = fadeOut(tween(150)) + slideOutVertically { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f), Color.Black.copy(alpha = 0.92f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Creator Attribution Row (Avatar + Name + @username + VerifiedBadge + Details toggle)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    closeWithAnimation { onNavigateToProfile() }
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            IosAvatarView(
                                name = post.authorDisplayName.ifBlank { post.authorUsername },
                                avatarUrl = post.authorAvatarUrl,
                                size = 40.dp
                            )
                            Column(verticalArrangement = Arrangement.spacedBy((-1).dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = post.authorDisplayName.ifBlank { post.authorUsername.ifBlank { "Creator" } },
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = InterFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (post.authorVerified) {
                                        Spacer(Modifier.width(4.dp))
                                        VerifiedBadge(
                                            size = 13.5.dp,
                                            tone = post.authorVerifiedTone,
                                            userId = post.creatorId
                                        )
                                    }
                                }
                                Text(
                                    text = "@${post.authorUsername.ifBlank { "creator" }}",
                                    color = Color.White.copy(alpha = 0.68f),
                                    fontSize = 12.sp,
                                    fontFamily = InterFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                .clickable { showDetailsDrawer = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Lucide.Info, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Details",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily
                            )
                        }
                    }

                    // Title & Subtle Caption
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDetailsDrawer = true },
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = cleanTitle,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$displaySubtitle • ${post.effectiveCommentsCount} comments • ${post.remixCount} remixes",
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Primary Thumb-Zone Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Clean borderless Like action
                        Row(
                            modifier = Modifier
                                .height(46.dp)
                                .graphicsLayer {
                                    scaleX = likeScale
                                    scaleY = likeScale
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    localIsLiked = !localIsLiked
                                    likeBounced = true
                                    onToggleLike?.invoke()
                                }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = ChromaIcons.HeartFilled,
                                contentDescription = "Like",
                                tint = if (localIsLiked) Color(0xFFFF2D55) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = effectiveLikes.toString(),
                                color = if (localIsLiked) Color(0xFFFF2D55) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily
                            )
                        }

                        // Primary Remix Action
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .height(46.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                                .clickable {
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
                                        strokeColor = colors.onPrimary,
                                        heartFillColor = null
                                    ),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "Remix Creation",
                                    color = colors.onPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }

                        // View Post / Comments Action
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .height(46.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                .clickable {
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
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "View Post",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // ── 4. EXPANDABLE ONE UI DETAILS SHEET OVERLAY ────────────────────
            AnimatedVisibility(
                visible = showDetailsDrawer,
                enter = slideInVertically { it } + fadeIn(tween(180)),
                exit = slideOutVertically { it } + fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = colors.sheetCornerRadius,
                        topEnd = colors.sheetCornerRadius
                    ),
                    color = colors.cardSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // One UI Sheet Drag Handle + Close
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(colors.textMuted.copy(alpha = 0.35f))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showDetailsDrawer = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.X, "Close details", tint = colors.textMain, modifier = Modifier.size(15.dp))
                            }
                        }

                        // Creator Profile Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
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
                                    size = 44.dp
                                )
                                Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = post.authorDisplayName.ifBlank { post.authorUsername.ifBlank { "Creator" } },
                                            color = colors.textMain,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = InterFontFamily
                                        )
                                        if (post.authorVerified) {
                                            Spacer(Modifier.width(4.dp))
                                            VerifiedBadge(
                                                size = 14.dp,
                                                tone = post.authorVerifiedTone,
                                                userId = post.creatorId
                                            )
                                        }
                                    }
                                    Text(
                                        text = "@${post.authorUsername.ifBlank { "creator" }}",
                                        color = colors.textMuted,
                                        fontSize = 12.5.sp,
                                        fontFamily = InterFontFamily
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 13.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "View Profile",
                                    color = colors.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }

                        // Title & Subtitle
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = cleanTitle,
                                color = colors.textMain,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily
                            )
                            Text(
                                text = displaySubtitle,
                                color = colors.textMuted,
                                fontSize = 12.5.sp,
                                fontFamily = InterFontFamily
                            )
                        }

                        // Engagement Focus Blocks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val statShape = RoundedCornerShape(14.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(statShape)
                                    .background(colors.inputBg)
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(ChromaIcons.HeartFilled, null, tint = Color(0xFFFF2D55), modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "$effectiveLikes ${if (effectiveLikes == 1) "Like" else "Likes"}",
                                        color = colors.textMain,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(statShape)
                                    .background(colors.inputBg)
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(Lucide.MessageCircle, null, tint = colors.primary, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "${post.effectiveCommentsCount} ${if (post.effectiveCommentsCount == 1) "Comment" else "Comments"}",
                                        color = colors.textMain,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(statShape)
                                    .background(colors.inputBg)
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = rememberLucideHeartHandshakeIcon(
                                            isRemixed = false,
                                            strokeColor = colors.primary,
                                            heartFillColor = null
                                        ),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${post.remixCount} ${if (post.remixCount == 1) "Remix" else "Remixes"}",
                                        color = colors.textMain,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Text(
                            text = displayDesc,
                            color = colors.textMain.copy(alpha = 0.85f),
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            fontFamily = InterFontFamily
                        )

                        if (post.tags.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(post.tags) { tag ->
                                    val cleanTag = tag.removePrefix("#")
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(colors.primary.copy(alpha = 0.12f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "#$cleanTag",
                                            color = colors.primary,
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
    val haptic = LocalHapticFeedback.current
    val doubleTapLikeState = rememberDoubleTapLikeState()
    var localIsLiked by remember(work.id, isLiked) { mutableStateOf(isLiked) }
    var layers by remember(work.jsonUrl) { mutableStateOf<List<GradientLayer>?>(LayerCache.get(work.jsonUrl)) }
    val isVideo = remember(work) { isVideoPost(work) }
    val thumbUrl = remember(work) {
        work.thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: work.imageUrl?.takeIf { it.isNotBlank() }
            ?: work.imageUrls.firstOrNull()?.takeIf { it.isNotBlank() }
    }
    LaunchedEffect(work.id, isLiked) {
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

    val outerShape = RoundedCornerShape(22.dp)
    val mediaShape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(outerShape)
            .background(colors.cardSurface)
            .border(0.5.dp, colors.cardBorderSubtle, outerShape)
            .padding(7.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(mediaShape)
                .background(colors.inputBg)
                .pointerInput(work.id) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onDoubleTap = { offset ->
                            if (!localIsLiked) {
                                localIsLiked = true
                                onDoubleTapLike?.invoke()
                            }
                            doubleTapLikeState.trigger(offset)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
        ) {
            if (isVideo && !thumbUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = work.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (work.postType == "image" && !thumbUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbUrl)
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
            } else if (!thumbUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = work.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(colors.primary.copy(alpha = 0.55f), Color(0xFF5856D6).copy(alpha = 0.55f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AiWaveSphereIcon(modifier = Modifier.size(28.dp))
                }
            }

            // Subtle Bottom Scrim for Contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f))
                        )
                    )
            )

            // Top-Left Floating Category / Trending Badge
            val badgeLabel = remember(work.isTrendingMasterwork, work.likesCount, work.tags) {
                when {
                    work.isTrendingMasterwork || work.likesCount > 120 -> "Trending"
                    work.tags.isNotEmpty() -> "#${work.tags.first().removePrefix("#")}"
                    else -> null
                }
            }
            if (badgeLabel != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.52f))
                        .padding(horizontal = 8.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily
                    )
                }
            }

            // Top-Right Media Type Indicator (Video Play / Multi-Slide / Expand Hint)
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Play,
                        contentDescription = "Flash Video",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else if (work.imageUrls.size > 1 || work.workUrls.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Layers,
                        contentDescription = "Carousel",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            FlatDoubleTapHeartOverlay(
                state = doubleTapLikeState,
                modifier = Modifier.fillMaxSize(),
                heartSize = 65.dp
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = rememberHighlightedText(work.title.ifBlank { "Creation" }, searchQuery, accentColor),
            color = colors.textMain,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 5.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f)
            ) {
                IosAvatarView(
                    name = work.authorDisplayName.ifBlank { work.authorUsername },
                    avatarUrl = work.authorAvatarUrl,
                    size = 18.dp
                )
                Text(
                    text = rememberHighlightedText("@${work.authorUsername.ifBlank { "creator" }}", searchQuery, accentColor),
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (work.authorVerified) {
                    VerifiedBadge(
                        size = 11.dp,
                        tone = work.authorVerifiedTone,
                        userId = work.creatorId
                    )
                }
            }

            val currentUserId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
            val effectiveLikes = remember(work.likesCount, work.likedBy, localIsLiked, currentUserId) {
                val hasUserLikedInPost = !currentUserId.isNullOrBlank() && work.likedBy.contains(currentUserId)
                val delta = when {
                    localIsLiked && !hasUserLikedInPost -> 1
                    !localIsLiked && hasUserLikedInPost -> -1
                    else -> 0
                }
                maxOf(0, work.likesCount + delta)
            }
            if (effectiveLikes > 0 || localIsLiked) {
                Row(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = ChromaIcons.HeartFilled,
                        contentDescription = null,
                        tint = if (localIsLiked) Color(0xFFFF2D55) else colors.textMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = effectiveLikes.toString(),
                        color = if (localIsLiked) Color(0xFFFF2D55) else colors.textMuted,
                        fontSize = 10.5.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}