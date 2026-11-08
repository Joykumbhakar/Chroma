package com.chroma.studio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.chroma.studio.ui.components.VerifiedBadge

import androidx.compose.animation.* 
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.chroma.studio.ui.theme.StoryFonts
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chroma.studio.ui.components.LiquidGlassSecondaryButton
import com.chroma.studio.ui.components.IosAlertDialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.R
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.NotificationDispatcher
import com.chroma.studio.model.NotificationActionType
import com.chroma.studio.model.Story
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.UserStoryGroup
import com.chroma.studio.chat.ui.components.StoryAnalyticsModal
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.components.MarkdownText
import com.chroma.studio.ui.components.OnlyFriendsManagementSheet
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val StoryViewerJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }


data class FlatStoryItem(
    val group: UserStoryGroup,
    val story: Story,
    val groupIndex: Int,
    val indexInGroup: Int,
    val totalInGroup: Int
)

@Composable
private fun StorySlideContent(
    story: Story,
    modifier: Modifier = Modifier
) {
    val media = story.mediaUrl
    val hasImage = media.isNotBlank() && !media.startsWith("text:") && !media.startsWith("markdown://")
    val isExplicitText = media.startsWith("text:") || media.startsWith("markdown://")
    val hasText = story.textContent.isNotBlank() || isExplicitText

    val textBody = when {
        story.textContent.isNotBlank() -> story.textContent
        media.startsWith("text:") && media != "text:story" -> media.removePrefix("text:")
        media.startsWith("markdown://") -> media.removePrefix("markdown://")
        else -> ""
    }

    val formatState = remember(story.decompressedTextFormat) {
        try {
            val resolvedFormat = story.decompressedTextFormat
            if (resolvedFormat.isNotBlank() && resolvedFormat != "{}") {
                StoryViewerJson.decodeFromString<StatusFormatState>(resolvedFormat)
            } else {
                StatusFormatState()
            }
        } catch (e: Exception) {
            StatusFormatState()
        }
    }

    val gradientTheme = StoryGradientThemes.find { it.id.equals(formatState.gradientPreset, ignoreCase = true) }
    val backgroundBrush = remember(gradientTheme, formatState.gradientPreset, story.backgroundColor, formatState.backgroundColorHex) {
        when {
            gradientTheme != null && formatState.gradientPreset != "None" -> {
                Brush.verticalGradient(gradientTheme.colors)
            }
            story.backgroundColor.isNotBlank() -> {
                val solidColor = try { Color(android.graphics.Color.parseColor(story.backgroundColor)) } catch (e: Exception) { Color(0xFF0F0C20) }
                Brush.verticalGradient(listOf(solidColor, solidColor))
            }
            formatState.backgroundColorHex.isNotBlank() && formatState.backgroundColorHex != "#FF1F1C2C" -> {
                val solidColor = try { Color(android.graphics.Color.parseColor(formatState.backgroundColorHex)) } catch (e: Exception) { Color(0xFF0F0C20) }
                Brush.verticalGradient(listOf(solidColor, solidColor))
            }
            else -> {
                Brush.verticalGradient(StoryGradientThemes.first().colors)
            }
        }
    }

    val textColor = remember(hasImage, gradientTheme, formatState.textColorHex) {
        if (hasImage) {
            Color.White
        } else {
            try {
                Color(android.graphics.Color.parseColor(formatState.textColorHex))
            } catch (e: Exception) {
                gradientTheme?.defaultTextColor ?: Color.White
            }
        }
    }

    val highlightColor = remember(formatState.isHighlight, formatState.highlightColorHex) {
        if (formatState.isHighlight) {
            try {
                Color(android.graphics.Color.parseColor(formatState.highlightColorHex)).copy(alpha = 0.35f)
            } catch (e: Exception) {
                Color(0xFFFFD60A).copy(alpha = 0.35f)
            }
        } else {
            Color.Transparent
        }
    }

    val context = LocalContext.current
    val font = remember(context, formatState.fontFamily) {
        StoryFonts.getFontFamily(context, formatState.fontFamily)
    }

    val align = when (formatState.textAlign) {
        "center" -> TextAlign.Center
        "right" -> TextAlign.Right
        else -> TextAlign.Left
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (hasImage) {
                    Modifier.background(Color.Black)
                } else {
                    Modifier.background(backgroundBrush)
                }
            )
    ) {
        // 1. Render Background Image if present
        if (hasImage) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(media)
                    .crossfade(true)
                    .build(),
                contentDescription = "Story Media",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Protective scrim overlay matching Story Editor preview
            if (hasText) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.38f),
                                    Color.Black.copy(alpha = 0.65f)
                                )
                            )
                        )
                )
            }
        }

        // 2. Render Formatted Story Content matching the preview in Story Editor
        if (hasText) {
            val rawTitle = remember(formatState.title, textBody) {
                if (formatState.title.isNotBlank()) {
                    formatState.title
                } else if (textBody.startsWith("# ")) {
                    textBody.substringBefore("\n\n").removePrefix("# ").trim()
                } else {
                    ""
                }
            }

            val bodyOnly = remember(rawTitle, textBody) {
                if (rawTitle.isNotBlank() && textBody.startsWith("# ")) {
                    textBody.substringAfter("\n\n").trim()
                } else {
                    textBody.trim()
                }
            }

            val headlineStyle = TextStyle(
                fontFamily = font,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = align
            )

            val bodyTextStyle = TextStyle(
                fontFamily = font,
                fontSize = when (formatState.headingLevel) {
                    2 -> 20.sp
                    3 -> 17.sp
                    else -> 15.sp
                },
                fontWeight = if (formatState.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (formatState.isItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = when {
                    formatState.isUnderline && formatState.isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                    formatState.isUnderline -> TextDecoration.Underline
                    formatState.isStrikethrough -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                },
                color = textColor,
                textAlign = align,
                background = highlightColor,
                lineHeight = when (formatState.headingLevel) {
                    2 -> 28.sp
                    3 -> 24.sp
                    else -> 22.sp
                },
                drawStyle = if (formatState.isStroke) Stroke(
                    miter = 8f,
                    width = 2.5f,
                    join = StrokeJoin.Round
                ) else androidx.compose.ui.graphics.drawscope.Fill
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 76.dp, bottom = 84.dp, start = 20.dp, end = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = if (formatState.textAlign == "center") Alignment.CenterHorizontally else if (formatState.textAlign == "right") Alignment.End else Alignment.Start
            ) {
                // Category Tag Badge
                val selectedCatItem = StoryCategories.find { it.name.equals(formatState.tag, ignoreCase = true) }
                if (selectedCatItem != null || formatState.tag.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .align(if (align == TextAlign.Center) Alignment.CenterHorizontally else if (align == TextAlign.Right) Alignment.End else Alignment.Start)
                            .clip(RoundedCornerShape(6.dp))
                            .background(textColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (selectedCatItem != null) {
                            Icon(
                                imageVector = selectedCatItem.icon,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = selectedCatItem?.name ?: formatState.tag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // Headline / Title
                if (rawTitle.isNotBlank()) {
                    Text(
                        text = rawTitle,
                        style = headlineStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(textColor.copy(alpha = 0.15f))
                    )
                    Spacer(Modifier.height(10.dp))
                }

                // Body Content / Quote
                if (formatState.isQuote) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(textColor.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                Brush.verticalGradient(listOf(Color(0xFF0381FE), Color(0xFF0072DE))),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "\"",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = bodyOnly,
                            style = bodyTextStyle.copy(fontStyle = FontStyle.Italic),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else if (bodyOnly.isNotBlank()) {
                    Text(
                        text = bodyOnly,
                        style = bodyTextStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Mood Badge
                val selectedMoodItem = StoryMoods.find { it.name.equals(formatState.mood, ignoreCase = true) }
                if (selectedMoodItem != null || formatState.mood.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .align(if (align == TextAlign.Center) Alignment.CenterHorizontally else if (align == TextAlign.Right) Alignment.End else Alignment.Start)
                            .clip(RoundedCornerShape(6.dp))
                            .background(textColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (selectedMoodItem != null) {
                            Icon(
                                imageVector = selectedMoodItem.icon,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = selectedMoodItem?.name ?: formatState.mood,
                            fontSize = 10.5.sp,
                            color = textColor.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun StoryViewerScreen(
    storyGroups: List<UserStoryGroup>,
    initialGroupIndex: Int,
    currentUserId: String,
    storyViewModel: StoryViewModel,
    likedStoryIds: Set<String>,
    myStories: List<Story>,
    currentUserProfile: UserProfile? = null,
    onClose: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onReplySend: ((authorId: String, text: String, storyId: String?, storyThumbnailUrl: String?) -> Unit)? = null,
    initialStoryId: String? = null
) {
    val liveStoryGroups by storyViewModel.storyGroups.collectAsState()
    val liveMyStories by storyViewModel.myStories.collectAsState()
    val effectiveStoryGroups = if (liveStoryGroups.isNotEmpty()) liveStoryGroups else storyGroups
    val effectiveMyStories = if (liveMyStories.isNotEmpty()) liveMyStories else myStories

    val allGroups = remember(effectiveStoryGroups, effectiveMyStories, currentUserId, currentUserProfile, initialGroupIndex, initialStoryId) {
        val shouldPrependMyStory = (initialGroupIndex < 0 || (initialStoryId != null && effectiveMyStories.any { it.id == initialStoryId })) && effectiveMyStories.isNotEmpty()
        if (shouldPrependMyStory) {
            listOf(
                UserStoryGroup(
                    userId = currentUserId,
                    userProfile = currentUserProfile?.copy(
                        displayName = currentUserProfile.displayName.ifBlank { "Your Story" }
                    ) ?: UserProfile(uid = currentUserId, displayName = "Your Story"),
                    stories = effectiveMyStories,
                    allSeen = true
                )
            ) + effectiveStoryGroups.filter { it.userId != currentUserId }
        } else {
            effectiveStoryGroups
        }
    }

    val allFlatStories = remember(allGroups) {
        allGroups.flatMapIndexed { groupIdx, group ->
            group.stories.mapIndexed { storyIdx, story ->
                FlatStoryItem(
                    group = group,
                    story = story,
                    groupIndex = groupIdx,
                    indexInGroup = storyIdx,
                    totalInGroup = group.stories.size
                )
            }
        }
    }

    if (allFlatStories.isEmpty()) {
        LaunchedEffect(Unit) {
            onClose()
        }
        return
    }

    val initialPageIndex = remember(allFlatStories, initialStoryId, initialGroupIndex) {
        if (!initialStoryId.isNullOrBlank()) {
            val exactIdx = allFlatStories.indexOfFirst { it.story.id == initialStoryId }
            if (exactIdx >= 0) return@remember exactIdx
        }
        val targetGroup = if (initialGroupIndex < 0) 0 else initialGroupIndex.coerceIn(0, maxOf(0, allGroups.size - 1))
        val idx = allFlatStories.indexOfFirst { it.groupIndex == targetGroup }
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { allFlatStories.size }
    )

    LaunchedEffect(initialStoryId) {
        if (!initialStoryId.isNullOrBlank()) {
            val exactIdx = allFlatStories.indexOfFirst { it.story.id == initialStoryId }
            if (exactIdx >= 0 && exactIdx != pagerState.currentPage) {
                pagerState.scrollToPage(exactIdx)
            }
        }
    }

    var isPaused by remember { mutableStateOf(false) }
    var showViewers by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showOnlyFriendsSheet by remember { mutableStateOf(false) }
    var showOnlyFriendsInfoDialog by remember { mutableStateOf(false) }
    var quickReplyText by remember { mutableStateOf("") }
    var showQuickReactions by remember { mutableStateOf(false) }
    var isReplyInputFocused by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible
    val isReplying = quickReplyText.isNotEmpty() || isReplyInputFocused || showQuickReactions || isImeVisible

    BackHandler(enabled = isReplying) {
        quickReplyText = ""
        isReplyInputFocused = false
        showQuickReactions = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val scope = rememberCoroutineScope()
    val storyDurationMs = 5000L

    val currentStoryItem = allFlatStories.getOrNull(pagerState.currentPage)
    val currentGroup = currentStoryItem?.group
    val currentStory = currentStoryItem?.story
    val currentStoryIndexInGroup = currentStoryItem?.indexInGroup ?: 0
    val storiesForCurrentGroup = currentGroup?.stories ?: emptyList()
    val isOwner = currentStory != null && (currentStory.userId == currentUserId || currentGroup?.userId == currentUserId)
    val vmLikedStoryIds by storyViewModel.likedStoryIds.collectAsState()
    val isLiked = currentStory != null && (vmLikedStoryIds.contains(currentStory.id) || currentStory.likedBy.contains(currentUserId))

    var doubleTapHeartVisible by remember { mutableStateOf(false) }
    val doubleTapHeartScale = remember { Animatable(0.2f) }
    val doubleTapHeartAlpha = remember { Animatable(0f) }

    val isScrollInProgress = pagerState.isScrollInProgress
    val effectivePaused = isPaused || isScrollInProgress || showViewers || showDeleteConfirm || showOnlyFriendsSheet || showOnlyFriendsInfoDialog || isReplying

    val progress = remember(pagerState.currentPage) { Animatable(0f) }

    LaunchedEffect(pagerState.currentPage) {
        progress.snapTo(0f)
    }

    LaunchedEffect(pagerState.currentPage, effectivePaused) {
        if (!effectivePaused && allFlatStories.isNotEmpty()) {
            val currentFraction = progress.value.coerceIn(0f, 1f)
            val remainingFraction = 1f - currentFraction
            val remainingDuration = kotlin.math.round(storyDurationMs * remainingFraction).toInt().coerceAtLeast(50)

            val result = progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = remainingDuration, easing = LinearEasing)
            )
            if (result.endReason == AnimationEndReason.Finished && progress.value >= 0.999f) {
                // Auto advance with smooth 3D animation
                if (pagerState.currentPage < allFlatStories.size - 1) {
                    pagerState.animateScrollToPage(
                        page = pagerState.currentPage + 1,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                } else {
                    onClose()
                }
            }
        } else {
            progress.stop()
        }
    }

    LaunchedEffect(currentStory?.id) {
        if (currentStory != null && !isOwner) {
            storyViewModel.markViewed(currentStory.id)
        }
    }

    val likeScale = remember { Animatable(1f) }
    LaunchedEffect(isLiked) {
        if (isLiked) {
            likeScale.animateTo(1.4f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
            likeScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        LaunchedEffect(view) {
            val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            window?.let {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(it, false)
                androidx.core.view.WindowInsetsControllerCompat(it, view).apply {
                    hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                    systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // ── 3D Smooth Story Pager ──────────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val storyItem = allFlatStories.getOrNull(page) ?: return@HorizontalPager
                val density = LocalDensity.current.density

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val offsetFromCurrent = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
                            cameraDistance = 20f * density

                            when {
                                offsetFromCurrent < -1f || offsetFromCurrent > 1f -> {
                                    alpha = 0f
                                }
                                offsetFromCurrent < 0f -> {
                                    // Moving left / turning away around right edge
                                    transformOrigin = TransformOrigin(pivotFractionX = 1f, pivotFractionY = 0.5f)
                                    val clamped = offsetFromCurrent.coerceIn(-1f, 0f)
                                    rotationY = 90f * clamped
                                    val scale = 1f + (0.08f * clamped)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f + (0.2f * clamped)
                                }
                                offsetFromCurrent > 0f -> {
                                    // Moving in from right / turning in around left edge
                                    transformOrigin = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0.5f)
                                    val clamped = offsetFromCurrent.coerceIn(0f, 1f)
                                    rotationY = 90f * clamped
                                    val scale = 1f - (0.08f * clamped)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - (0.2f * clamped)
                                }
                                else -> {
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    rotationY = 0f
                                    scaleX = 1f
                                    scaleY = 1f
                                    alpha = 1f
                                }
                            }
                        }
                        .pointerInput(page, allFlatStories.size) {
                            detectTapGestures(
                                onPress = {
                                    isPaused = true
                                    tryAwaitRelease()
                                    isPaused = false
                                },
                                onDoubleTap = {
                                    val currentItem = allFlatStories.getOrNull(page)
                                    currentItem?.story?.let { story ->
                                        storyViewModel.forceLike(story.id)
                                        doubleTapHeartVisible = true
                                        scope.launch {
                                            doubleTapHeartScale.snapTo(0.25f)
                                            doubleTapHeartAlpha.snapTo(1f)
                                            launch {
                                                doubleTapHeartScale.animateTo(
                                                    targetValue = 1.35f,
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                                )
                                            }
                                            kotlinx.coroutines.delay(450)
                                            doubleTapHeartAlpha.animateTo(0f, tween(250))
                                            doubleTapHeartVisible = false
                                        }
                                    }
                                },
                                onTap = { offset ->
                                    if (isReplying) {
                                        quickReplyText = ""
                                        isReplyInputFocused = false
                                        showQuickReactions = false
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        return@detectTapGestures
                                    }
                                    if (showViewers || showDeleteConfirm || showOnlyFriendsSheet || showOnlyFriendsInfoDialog) return@detectTapGestures
                                    val totalWidth = size.width
                                    if (offset.x < totalWidth * 0.33f) {
                                        // Previous slide with smooth 3D animation
                                        scope.launch {
                                            if (pagerState.currentPage > 0) {
                                                pagerState.animateScrollToPage(
                                                    page = pagerState.currentPage - 1,
                                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                                )
                                            }
                                        }
                                    } else {
                                        // Next slide with smooth 3D animation
                                        scope.launch {
                                            if (pagerState.currentPage < allFlatStories.size - 1) {
                                                pagerState.animateScrollToPage(
                                                    page = pagerState.currentPage + 1,
                                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                                )
                                            } else {
                                                onClose()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    StorySlideContent(story = storyItem.story)

                    // Floating Bursting Heart on Double-Tap
                    if (doubleTapHeartVisible && pagerState.currentPage == page) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ChromaIcons.HeartFilled,
                                contentDescription = null,
                                tint = Color(0xFFFF2D55),
                                modifier = Modifier
                                    .size(105.dp)
                                    .scale(doubleTapHeartScale.value)
                                    .alpha(doubleTapHeartAlpha.value)
                            )
                        }
                    }

                    // Dynamic 3D lighting shadow overlay during cube rotation
                    val offsetFromCurrent = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction
                    val shadowAlpha = (kotlin.math.abs(offsetFromCurrent) * 0.45f).coerceIn(0f, 0.5f)
                    if (shadowAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = shadowAlpha))
                        )
                    }
                }
            }

            val currentStoryFormatState = remember(currentStory) {
                try {
                    val resolvedFormat = currentStory?.decompressedTextFormat ?: ""
                    if (resolvedFormat.isNotBlank() && resolvedFormat != "{}") {
                        StoryViewerJson.decodeFromString<StatusFormatState>(resolvedFormat)
                    } else {
                        StatusFormatState()
                    }
                } catch (e: Exception) {
                    StatusFormatState()
                }
            }

            val currentHasImage = remember(currentStory) {
                val media = currentStory?.mediaUrl ?: ""
                media.isNotBlank() && !media.startsWith("text:") && !media.startsWith("markdown://")
            }

            val topBarTextColor = remember(currentHasImage, currentStoryFormatState) {
                if (currentHasImage) {
                    Color.White
                } else {
                    val gradientTheme = StoryGradientThemes.find { it.id.equals(currentStoryFormatState.gradientPreset, ignoreCase = true) }
                    if (gradientTheme != null) {
                        gradientTheme.defaultTextColor
                    } else {
                        try {
                            Color(android.graphics.Color.parseColor(currentStoryFormatState.textColorHex))
                        } catch (e: Exception) {
                            Color.White
                        }
                    }
                }
            }

            // ── Top Bar ───────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 12.dp, end = 12.dp)
            ) {
                // Segmented Progress Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    storiesForCurrentGroup.forEachIndexed { index, _ ->
                        val segmentProgress = when {
                            index < currentStoryIndexInGroup -> 1f
                            index == currentStoryIndexInGroup -> progress.value
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(topBarTextColor.copy(alpha = 0.35f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(segmentProgress)
                                    .background(topBarTextColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // User Header Row
                currentGroup?.let { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (!isOwner) {
                                        onAuthorClick(group.userId)
                                    }
                                }
                        ) {
                            IosAvatarView(
                                name = group.userProfile.displayName,
                                avatarUrl = group.userProfile.avatarUrl,
                                size = 38.dp,
                                gender = group.userProfile.gender
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isOwner) "Your Story" else group.userProfile.displayName.ifBlank { "User" },
                                    color = topBarTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (group.userProfile.verified && !isOwner) {
                                    Spacer(Modifier.width(4.dp))
                                    VerifiedBadge(size = 12.dp)
                                }
                            }
                            currentStory?.let { story ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = storyFormatTimeAgo(story.timestamp),
                                        color = topBarTextColor.copy(alpha = 0.75f),
                                        fontSize = 11.5.sp
                                    )
                                    if (story.audienceType == "only_friends") {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(99.dp))
                                                .background(Color(0xFF00C853))
                                                .clickable {
                                                    if (isOwner) {
                                                        showOnlyFriendsSheet = true
                                                    } else {
                                                        showOnlyFriendsInfoDialog = true
                                                    }
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Lucide.Star,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(9.5.dp)
                                                )
                                                Text(
                                                    text = "Only Friend",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Owner Delete Action
                        if (isOwner && currentStory != null) {
                            Icon(
                                Lucide.Trash2,
                                contentDescription = "Delete Story",
                                tint = Color(0xFFFF453A),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { showDeleteConfirm = true }
                            )
                            Spacer(Modifier.width(12.dp))
                        }

                        // Close Button
                        Icon(
                            Lucide.X,
                            contentDescription = "Close",
                            tint = topBarTextColor,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onClose() }
                        )
                    }
                }
            }

            // ── Bottom Controls ───────────────────────────────────────────────────────
            if (currentStory != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    if (isOwner) {
                        // Admin: Viewers & Likes pill button
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                                                .clickable { showViewers = true }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Lucide.Eye, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "${maxOf(currentStory.views, currentStory.viewedBy.size)} views",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(ChromaIcons.HeartFilled, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "${maxOf(currentStory.likes, currentStory.likedBy.size)}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(Lucide.ChevronUp, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        }
                    } else {
                        // Viewer: Quick Message & Like Bar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (showQuickReactions) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("🔥", "❤️", "😂", "😍", "😮", "👏").forEach { emoji ->
                                        Text(
                                            text = emoji,
                                            fontSize = 22.sp,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    val authorId = currentStory?.userId ?: currentGroup?.userId ?: ""
                                                    val storyId = currentStory?.id ?: ""
                                                    val mediaUrl = currentStory?.mediaUrl
                                                    val thumbUrl = if (!mediaUrl.isNullOrBlank() && mediaUrl != "text:story") mediaUrl else if (!currentStory?.textContent.isNullOrBlank()) "text:${currentStory?.textContent}" else mediaUrl
                                                    if (authorId.isNotBlank()) {
                                                        onReplySend?.invoke(authorId, emoji, storyId, thumbUrl)
                                                        scope.launch {
                                                            NotificationDispatcher.sendNotification(
                                                                targetUserId = authorId,
                                                                type = NotificationActionType.STORY_REACTION,
                                                                actionText = "reacted $emoji to your story",
                                                                detailText = emoji,
                                                                targetPreviewUrl = thumbUrl,
                                                                targetId = storyId
                                                            )
                                                        }
                                                    }
                                                    ToastManager.showToast("Sent $emoji reaction")
                                                    showQuickReactions = false
                                                    isReplyInputFocused = false
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                }
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(Color.White.copy(alpha = 0.18f))
                                                                                .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BasicTextField(
                                            value = quickReplyText,
                                            onValueChange = { quickReplyText = it },
                                            singleLine = true,
                                            textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                                            cursorBrush = SolidColor(Color.White),
                                            decorationBox = { innerTextField ->
                                                if (quickReplyText.isEmpty()) {
                                                    Text(
                                                        "Send message...",
                                                        color = Color.White.copy(alpha = 0.65f),
                                                        fontSize = 13.5.sp
                                                    )
                                                }
                                                innerTextField()
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .onFocusChanged { isReplyInputFocused = it.isFocused }
                                        )

                                        if (quickReplyText.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    val reply = quickReplyText.trim()
                                                    val authorId = currentStory?.userId ?: currentGroup?.userId ?: ""
                                                    val storyId = currentStory?.id ?: ""
                                                    val mediaUrl = currentStory?.mediaUrl
                                                    val thumbUrl = if (!mediaUrl.isNullOrBlank() && mediaUrl != "text:story") mediaUrl else if (!currentStory?.textContent.isNullOrBlank()) "text:${currentStory?.textContent}" else mediaUrl
                                                    if (authorId.isNotBlank() && reply.isNotBlank()) {
                                                        onReplySend?.invoke(authorId, reply, storyId, thumbUrl)
                                                        scope.launch {
                                                            NotificationDispatcher.sendNotification(
                                                                targetUserId = authorId,
                                                                type = NotificationActionType.STORY_REPLY,
                                                                actionText = "replied to your story",
                                                                detailText = reply,
                                                                targetPreviewUrl = thumbUrl,
                                                                targetId = storyId
                                                            )
                                                        }
                                                    }
                                                    ToastManager.showToast("Reply sent!")
                                                    quickReplyText = ""
                                                    isReplyInputFocused = false
                                                    showQuickReactions = false
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Lucide.Send, contentDescription = "Send", tint = Color(0xFF0381FE), modifier = Modifier.size(16.dp))
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { showQuickReactions = !showQuickReactions },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Lucide.Smile, contentDescription = "Reactions", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }

                                // Filled Heart Like Button
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .scale(likeScale.value)
                                        .clip(CircleShape)
                                        .background(if (isLiked) Color(0xFFFF2D55).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.18f))
                                        .border(
                                            0.5.dp,
                                            if (isLiked) Color(0xFFFF2D55).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.35f),
                                            CircleShape
                                        )
                                        .clickable {
                                            currentStory.let { storyViewModel.toggleLike(it.id) }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = ChromaIcons.HeartFilled,
                                        contentDescription = if (isLiked) "Unlike" else "Like",
                                        tint = if (isLiked) Color(0xFFFF2D55) else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Admin Story Insights & Analytics Modal (Apple HIG Style) ───────────────
            if (showViewers && currentStory != null) {
                StoryAnalyticsModal(
                    story = currentStory,
                    onClose = { showViewers = false },
                    onDeleteStory = { storyId ->
                        storyViewModel.deleteStory(storyId)
                        showViewers = false
                        ToastManager.showToast("Story deleted")
                        onClose()
                    }
                )
            }

            // ── Delete Confirmation Dialog (Apple HIG Style) ──────────────────────────
            if (showDeleteConfirm && currentStory != null) {
                IosAlertDialog(
                    title = "Delete Story?",
                    message = "This will permanently remove this story for all viewers.",
                    confirmText = "Delete",
                    isDestructive = true,
                    onConfirm = {
                        storyViewModel.deleteStory(currentStory.id)
                        showDeleteConfirm = false
                        ToastManager.showToast("Story deleted")
                        onClose()
                    },
                    dismissText = "Cancel",
                    onDismiss = { showDeleteConfirm = false }
                )
            }

            // ── Only Friend Management Sheet (Owner) ──────────────────────────────────
            if (showOnlyFriendsSheet) {
                OnlyFriendsManagementSheet(
                    storyViewModel = storyViewModel,
                    onDismissRequest = { showOnlyFriendsSheet = false }
                )
            }

            // ── Only Friend Info Dialog (Viewer) ──────────────────────────────────────
            if (showOnlyFriendsInfoDialog) {
                val authorName = currentGroup?.userProfile?.displayName?.ifBlank { "this user" } ?: "this user"
                IosAlertDialog(
                    title = "Only Friend",
                    message = "Only people on $authorName's Only Friend list can see this story. They won't know you saw this.",
                    confirmText = "Got it",
                    onConfirm = { showOnlyFriendsInfoDialog = false },
                    dismissText = null,
                    onDismiss = { showOnlyFriendsInfoDialog = false },
                    customContent = {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

// ── Admin Viewers Sheet Component ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminStoryViewersSheet(
    story: Story,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var viewerProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoadingProfiles by remember { mutableStateOf(true) }

    LaunchedEffect(story.id) {
        if (story.viewedBy.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val repo = DiscoverRepository()
                val profiles = story.viewedBy.mapNotNull { uid ->
                    try { repo.getUserProfile(uid) } catch (e: Exception) { null }
                }
                viewerProfiles = profiles
                isLoadingProfiles = false
            }
        } else {
            isLoadingProfiles = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Story Insights",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${story.viewedBy.size} Viewers · ${story.likedBy.size} Likes",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Lucide.Trash2, contentDescription = "Delete", tint = Color(0xFFFF453A))
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isLoadingProfiles) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0381FE), modifier = Modifier.size(24.dp))
                }
            } else if (viewerProfiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No viewers yet", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewerProfiles) { profile ->
                        val hasLiked = story.likedBy.contains(profile.uid)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IosAvatarView(
                                    name = profile.displayName,
                                    avatarUrl = profile.avatarUrl,
                                    size = 38.dp,
                                    gender = profile.gender
                                )
                                Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
                                    Text(profile.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 16.sp)
                                    Text("@${profile.username}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, lineHeight = 14.sp)
                                }
                            }

                            if (hasLiked) {
                                Icon(
                                    ChromaIcons.HeartFilled,
                                    contentDescription = "Liked",
                                    tint = Color(0xFFFF2D55),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun goNextStory(
    currentStoryIndex: Int,
    currentGroupIndex: Int,
    storiesInGroup: Int,
    totalGroups: Int,
    onStoryChange: (Int) -> Unit,
    onGroupChange: (Int) -> Unit,
    onClose: () -> Unit
) {
    if (currentStoryIndex < storiesInGroup - 1) {
        onStoryChange(currentStoryIndex + 1)
    } else if (currentGroupIndex < totalGroups - 1) {
        onGroupChange(currentGroupIndex + 1)
    } else {
        onClose()
    }
}

private fun storyFormatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "Yesterday"
    }
}
