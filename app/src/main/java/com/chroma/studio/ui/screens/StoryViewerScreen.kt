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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import com.chroma.studio.ui.components.AddToHighlightSheet
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.R
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.NotificationDispatcher
import com.chroma.studio.model.NotificationActionType
import com.chroma.studio.model.Story
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.UserStoryGroup
import com.chroma.studio.model.formatStoryRepostCredit
import com.chroma.studio.model.isViewerMentionedInStory
import com.chroma.studio.chat.ui.components.StoryAnalyticsModal
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.components.MarkdownText
import com.chroma.studio.ui.components.OnlyFriendsManagementSheet
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.ui.components.studioBevel
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.ShieldAlert
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
internal fun StorySlideContent(
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

                // Body Content / Quote (Clean Borderless Editorial Accent Bar)
                if (formatState.isQuote) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF0A84FF))
                        )
                        Spacer(Modifier.width(10.dp))
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
    initialStoryId: String? = null,
    isHighlightViewer: Boolean = false,
    activeHighlightTitle: String? = null
) {
    val context = LocalContext.current
    val liveStoryGroups by storyViewModel.storyGroups.collectAsState()
    val liveMyStories by storyViewModel.myStories.collectAsState()
    val deletedHighlightStoryIds = remember { mutableStateListOf<String>() }

    // Preserve explicit caller groups (e.g., profile highlight reels or standalone chat story replies)
    // while syncing live like/view counts for any overlapping story IDs.
    val effectiveStoryGroups = remember(storyGroups, liveStoryGroups, liveMyStories, isHighlightViewer, deletedHighlightStoryIds.size) {
        if (isHighlightViewer) {
            val liveStoriesById = (liveMyStories + liveStoryGroups.flatMap { it.stories }).associateBy { it.id }
            storyGroups.mapNotNull { grp ->
                val filtered = grp.stories
                    .filterNot { it.id in deletedHighlightStoryIds }
                    .map { s -> liveStoriesById[s.id] ?: s }
                if (filtered.isEmpty()) null else grp.copy(stories = filtered)
            }
        } else if (storyGroups.isEmpty()) {
            liveStoryGroups
        } else {
            val liveGroupsById = liveStoryGroups.associateBy { it.userId }
            storyGroups.map { grp -> liveGroupsById[grp.userId] ?: grp }
        }
    }
    val effectiveMyStories = remember(myStories, liveMyStories, isHighlightViewer) {
        when {
            isHighlightViewer -> emptyList()
            liveMyStories.isNotEmpty() -> liveMyStories
            else -> myStories
        }
    }

    val allGroups = remember(effectiveStoryGroups, effectiveMyStories, currentUserId, currentUserProfile, initialGroupIndex, initialStoryId, isHighlightViewer) {
        val shouldPrependMyStory = !isHighlightViewer &&
            (initialGroupIndex < 0 || (initialStoryId != null && effectiveMyStories.any { it.id == initialStoryId })) &&
            effectiveMyStories.isNotEmpty()
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
    var showHighlightSaveDialog by remember { mutableStateOf(false) }
    var showOnlyFriendsSheet by remember { mutableStateOf(false) }
    var showOnlyFriendsInfoDialog by remember { mutableStateOf(false) }
    var audioDetailStoryTarget by remember { mutableStateOf<Story?>(null) }
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
    val isLifecycleResumed = rememberStoryLifecycleResumed()

    val currentStoryItem = allFlatStories.getOrNull(pagerState.currentPage)
    val currentGroup = currentStoryItem?.group
    val currentStory = currentStoryItem?.story
    val currentStoryIndexInGroup = currentStoryItem?.indexInGroup ?: 0
    val storiesForCurrentGroup = currentGroup?.stories ?: emptyList()
    val isOwner = currentStory != null && (currentStory.userId == currentUserId || currentGroup?.userId == currentUserId)
    val vmLikedStoryIds by storyViewModel.likedStoryIds.collectAsState()
    val repostingStoryId by storyViewModel.repostingStoryId.collectAsState()
    val repostedSourceStoryIds by storyViewModel.repostedSourceStoryIds.collectAsState()
    val isLiked = currentStory != null && (vmLikedStoryIds.contains(currentStory.id) || currentStory.likedBy.contains(currentUserId))
    val isViewerMentioned = currentStory != null && isViewerMentionedInStory(
        story = currentStory,
        viewerUid = currentUserId,
        viewerUsername = currentUserProfile?.username
    )
    val alreadyRepostedThisStory = currentStory != null && (
        currentStory.id in repostedSourceStoryIds ||
            effectiveMyStories.any { it.repostedFromStoryId == currentStory.id }
        )

    val isCurrentStoryUploading = currentStory != null && (currentStory.isUploading || (currentStory.id.startsWith("temp_") && !currentStory.isFailed))
    val isCurrentStoryFailed = currentStory != null && currentStory.isFailed

    val storyDurationMs = remember(
        currentStory?.id,
        currentStory?.mediaUrl,
        currentStory?.textContent,
        currentStory?.decompressedTextFormat
    ) {
        if (currentStory == null) {
            StoryAutoAdvanceDefaults.DEFAULT_MEDIA_DURATION_MS
        } else {
            val hasVisualMedia = currentStory.mediaUrl.isNotBlank() && currentStory.mediaUrl != "text:story"
            val hasWidgets = hasInteractiveStoryWidgets(currentStory.decompressedTextFormat)
            computeStoryAutoAdvanceDurationMs(
                hasVisualMedia = hasVisualMedia,
                textContent = currentStory.textContent,
                hasInteractiveWidgets = hasWidgets
            )
        }
    }

    var doubleTapHeartVisible by remember { mutableStateOf(false) }
    val doubleTapHeartScale = remember { Animatable(0.2f) }
    val doubleTapHeartAlpha = remember { Animatable(0f) }

    val isScrollInProgress = pagerState.isScrollInProgress
    val effectivePaused = isPaused || isScrollInProgress || showViewers || showDeleteConfirm ||
        showHighlightSaveDialog || showOnlyFriendsSheet || showOnlyFriendsInfoDialog ||
        audioDetailStoryTarget != null ||
        isReplying || isCurrentStoryUploading || isCurrentStoryFailed ||
        (repostingStoryId != null) || !isLifecycleResumed

    val viewerContext = LocalContext.current
    val storyMusicPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(viewerContext).build().apply {
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            volume = 1f
        }
    }
    DisposableEffect(Unit) {
        onDispose { storyMusicPlayer.release() }
    }
    LaunchedEffect(currentStory?.id, currentStory?.musicAudioUrl, currentStory?.musicStartSec) {
        val audioUrl = currentStory?.musicAudioUrl?.takeIf { it.isNotBlank() }
        if (audioUrl != null) {
            storyMusicPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(audioUrl))
            storyMusicPlayer.prepare()
            storyMusicPlayer.seekTo((currentStory.musicStartSec * 1000L).coerceAtLeast(0L))
        } else {
            storyMusicPlayer.stop()
            storyMusicPlayer.clearMediaItems()
        }
    }
    LaunchedEffect(currentStory?.id, effectivePaused) {
        val hasAudio = !currentStory?.musicAudioUrl.isNullOrBlank()
        if (hasAudio && !effectivePaused) {
            storyMusicPlayer.playWhenReady = true
            if (!storyMusicPlayer.isPlaying) storyMusicPlayer.play()
        } else {
            storyMusicPlayer.playWhenReady = false
            if (storyMusicPlayer.isPlaying) storyMusicPlayer.pause()
        }
    }

    val progress = remember(pagerState.currentPage, currentStory?.id) { Animatable(0f) }

    LaunchedEffect(pagerState.currentPage, currentStory?.id) {
        progress.snapTo(0f)
    }

    LaunchedEffect(pagerState.currentPage, currentStory?.id, effectivePaused, storyDurationMs) {
        if (!effectivePaused && allFlatStories.isNotEmpty()) {
            val remainingDuration = computeRemainingStoryDurationMs(progress.value, storyDurationMs)
            val completed = if (remainingDuration <= 0) {
                progress.snapTo(1f)
                true
            } else {
                val result = progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = remainingDuration, easing = LinearEasing)
                )
                result.endReason == AnimationEndReason.Finished && isStoryProgressComplete(progress.value)
            }
            if (completed) {
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

                    com.chroma.studio.ui.storystudio.StoryInteractiveWidgetsOverlay(
                        storyId = storyItem.story.id,
                        storyAuthorId = storyItem.story.userId.ifBlank { storyItem.group.userId },
                        currentUserId = currentUserId,
                        decompressedTextFormat = storyItem.story.decompressedTextFormat,
                        onPauseChanged = { paused -> isPaused = paused },
                        onWidgetReply = { summary ->
                            val authorId = storyItem.story.userId.ifBlank { storyItem.group.userId }
                            val storyId = storyItem.story.id
                            val mediaUrl = storyItem.story.mediaUrl
                            val thumbUrl = if (mediaUrl.isNotBlank() && mediaUrl != "text:story") mediaUrl else null
                            if (authorId.isNotBlank() && authorId != currentUserId) {
                                onReplySend?.invoke(authorId, summary, storyId, thumbUrl)
                                scope.launch {
                                    NotificationDispatcher.sendNotification(
                                        targetUserId = authorId,
                                        type = NotificationActionType.STORY_REPLY,
                                        actionText = "interacted with your story",
                                        detailText = summary,
                                        targetPreviewUrl = thumbUrl,
                                        targetId = storyId
                                    )
                                }
                            }
                        },
                        onMentionClick = { handle ->
                            val clean = handle.trim().trimStart('@')
                            scope.launch {
                                try {
                                    val matches = withContext(Dispatchers.IO) {
                                        DiscoverRepository().searchUsers(clean)
                                    }
                                    val exact = matches.firstOrNull {
                                        it.username.equals(clean, ignoreCase = true)
                                    }
                                    if (exact != null) {
                                        onClose()
                                        onAuthorClick(exact.uid)
                                    } else {
                                        ToastManager.showToast(handle)
                                    }
                                } catch (_: Exception) {
                                    ToastManager.showToast(handle)
                                }
                            }
                        }
                    )

                    // Clean Compact Upload / Failure Overlay (zero heavy bevels or pill buttons)
                    val isSlideUploading = storyItem.story.isUploading || (storyItem.story.id.startsWith("temp_") && !storyItem.story.isFailed)
                    val isSlideFailed = storyItem.story.isFailed

                    if (isSlideUploading) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.72f))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                strokeWidth = 2.2.dp,
                                color = Color(0xFF0A84FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Sending story...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val percent = (storyItem.story.uploadProgress.coerceIn(0.1f, 1f) * 100).toInt()
                                Text(
                                    text = if (percent > 15) "Uploading • $percent%" else "Optimizing...",
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else if (isSlideFailed) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xD91C1C1E))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.ShieldAlert,
                                    contentDescription = null,
                                    tint = Color(0xFFFF453A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Upload failed",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Couldn't send story. Check connection.",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 11.5.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0A84FF))
                                        .clickable {
                                            storyViewModel.retryFailedUpload(context)
                                        }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Lucide.RotateCw,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Retry",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.14f))
                                        .clickable { showDeleteConfirm = true }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Discard",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

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
                    storiesForCurrentGroup.forEachIndexed { index, storyAtIdx ->
                        val isItemUploading = storyAtIdx.isUploading || (storyAtIdx.id.startsWith("temp_") && !storyAtIdx.isFailed)
                        val segmentProgress = when {
                            index < currentStoryIndexInGroup -> 1f
                            index == currentStoryIndexInGroup -> {
                                if (isItemUploading) {
                                    maxOf(progress.value, storyAtIdx.uploadProgress.coerceIn(0.15f, 0.95f))
                                } else {
                                    progress.value
                                }
                            }
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
                                val displayName = if (isOwner) "Your Story" else group.userProfile.displayName.ifBlank { "User" }
                                val titleText = when {
                                    isHighlightViewer && !activeHighlightTitle.isNullOrBlank() -> "$displayName • $activeHighlightTitle"
                                    isHighlightViewer && currentStory?.highlightTitle?.isNotBlank() == true -> "$displayName • ${currentStory.highlightTitle}"
                                    else -> displayName
                                }
                                Text(
                                    text = titleText,
                                    color = topBarTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (group.userProfile.verified && !isOwner) {
                                    Spacer(Modifier.width(4.dp))
                                    VerifiedBadge(
                                        size = 12.dp,
                                        tone = group.userProfile.verifiedTone
                                    )
                                }
                            }
                            currentStory?.let { story ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (story.isUploading || (story.id.startsWith("temp_") && !story.isFailed)) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            strokeWidth = 1.6.dp,
                                            color = Color(0xFF0381FE),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Sending...",
                                            color = Color(0xFF0381FE),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else if (story.isFailed) {
                                        Icon(
                                            imageVector = Lucide.ShieldAlert,
                                            contentDescription = null,
                                            tint = Color(0xFFFF453A),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Not sent",
                                            color = Color(0xFFFF453A),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Text(
                                            text = storyFormatTimeAgo(story.timestamp),
                                            color = topBarTextColor.copy(alpha = 0.75f),
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    if (story.audienceType == "only_friends") {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color(0xFF34C759))
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
                                                    text = "Only Friends",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                    val repostCredit = formatStoryRepostCredit(
                                        repostedFromUsername = story.repostedFromUsername,
                                        repostedFromDisplayName = story.repostedFromDisplayName
                                    )
                                    if (repostCredit != null) {
                                        Spacer(Modifier.width(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color.White.copy(alpha = 0.16f))
                                                .clickable(enabled = story.repostedFromUserId.isNotBlank()) {
                                                    onAuthorClick(story.repostedFromUserId)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Lucide.AtSign,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = repostCredit,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (!story.musicTitle.isNullOrBlank() || !story.musicTrackId.isNullOrBlank()) {
                                        Spacer(Modifier.width(6.dp))
                                        val musicChipLabel = buildString {
                                            append(story.musicTitle?.takeIf { it.isNotBlank() } ?: "Soundtrack")
                                            if (!story.musicArtist.isNullOrBlank()) {
                                                append(" · ").append(story.musicArtist)
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color.White.copy(alpha = 0.18f))
                                                .clickable(enabled = !story.musicTrackId.isNullOrBlank()) {
                                                    audioDetailStoryTarget = story
                                                }
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Lucide.Music,
                                                contentDescription = "Story Music",
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = musicChipLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Owner Delete Action
                        if (isOwner) {
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

            // ── Compact Bottom Controls Over Subtle Scrim (Zero Box Cards or Pills) ─────────
            if (currentStory != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
                            )
                        )
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (isOwner) {
                        val userHighlights by storyViewModel.highlights.collectAsState()
                        var isCurrentStoryHighlighted by remember(currentStory.id, currentStory.isHighlight, userHighlights) {
                            mutableStateOf(
                                isHighlightViewer ||
                                    currentStory.isHighlight ||
                                    userHighlights.any { currentStory.id in it.storyIds }
                            )
                        }

                        // Compact Borderless Admin Action Bar: Viewers/Insights + Highlight
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isCurrentStoryUploading) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF0A84FF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Uploading to stories...",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else if (isCurrentStoryFailed) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { storyViewModel.retryFailedUpload(context) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Lucide.RotateCw, contentDescription = null, tint = Color(0xFFFF453A), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Upload failed • Tap to retry",
                                        color = Color(0xFFFF453A),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // Clean open metrics trigger (tap or swipe up to open insights)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .pointerInput(isOwner) {
                                            detectVerticalDragGestures { _, dragAmount ->
                                                if (dragAmount < -15f && isOwner && !showViewers) {
                                                    showViewers = true
                                                }
                                            }
                                        }
                                        .clickable { showViewers = true }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Icon(Lucide.Eye, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Text(
                                            text = "${maxOf(currentStory.views, currentStory.viewedBy.size)}",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Icon(ChromaIcons.HeartFilled, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(15.dp))
                                        Text(
                                            text = "${maxOf(currentStory.likes, currentStory.likedBy.size)}",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Insights",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(Lucide.ChevronUp, contentDescription = "Swipe up for insights", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            // Compact Highlight Toggle
                            if (!isCurrentStoryUploading && !isCurrentStoryFailed && !currentStory.id.startsWith("temp_")) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrentStoryHighlighted) Color(0xFF34C759).copy(alpha = 0.24f) else Color.White.copy(alpha = 0.14f))
                                        .clickable { showHighlightSaveDialog = true }
                                        .padding(horizontal = 11.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentStoryHighlighted) Lucide.BookmarkCheck else Lucide.Bookmark,
                                        contentDescription = "Save to Highlights",
                                        tint = if (isCurrentStoryHighlighted) Color(0xFF30D158) else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isCurrentStoryHighlighted) "Highlighted" else "Highlight",
                                        color = if (isCurrentStoryHighlighted) Color(0xFF30D158) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (showHighlightSaveDialog) {
                            AddToHighlightSheet(
                                story = currentStory,
                                storyViewModel = storyViewModel,
                                onDismiss = { showHighlightSaveDialog = false },
                                onHighlightAdded = { _ ->
                                    isCurrentStoryHighlighted = true
                                    showHighlightSaveDialog = false
                                }
                            )
                        }
                    } else {
                        // Viewer: Mention Reshare CTA + Compact Borderless Reply & Like Bar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isViewerMentioned && currentStory != null) {
                                val isRepostingCurrent = repostingStoryId == currentStory.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.52f))
                                        .border(
                                            width = 0.75.dp,
                                            color = Color.White.copy(alpha = 0.22f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Lucide.AtSign,
                                            contentDescription = null,
                                            tint = Color(0xFF0A84FF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (alreadyRepostedThisStory) {
                                                "Added to your story"
                                            } else {
                                                "Mentioned you in this story"
                                            },
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .height(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (alreadyRepostedThisStory) {
                                                    Color(0xFF30D158).copy(alpha = 0.22f)
                                                } else {
                                                    Color(0xFF0381FE)
                                                }
                                            )
                                            .clickable(enabled = !alreadyRepostedThisStory && !isRepostingCurrent) {
                                                storyViewModel.repostMentionedStory(currentStory.id) { ok, err ->
                                                    if (ok) {
                                                        ToastManager.showToast("Added to your story!")
                                                    } else {
                                                        ToastManager.showToast(err ?: "Could not add story")
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isRepostingCurrent) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (alreadyRepostedThisStory) Lucide.Check else Lucide.Plus,
                                                    contentDescription = null,
                                                    tint = if (alreadyRepostedThisStory) Color(0xFF30D158) else Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = if (alreadyRepostedThisStory) "Added" else "Add to Story",
                                                    color = if (alreadyRepostedThisStory) Color(0xFF30D158) else Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (showQuickReactions) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("🔥", "❤️", "😂", "😍", "😮", "👏").forEach { emoji ->
                                        Text(
                                            text = emoji,
                                            fontSize = 22.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    val authorId = currentStory.userId
                                                    val storyId = currentStory.id
                                                    val mediaUrl = currentStory.mediaUrl
                                                    val thumbUrl = if (!mediaUrl.isNullOrBlank() && mediaUrl != "text:story") mediaUrl else if (!currentStory.textContent.isNullOrBlank()) "text:${currentStory.textContent}" else mediaUrl
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
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.16f))
                                        .padding(horizontal = 12.dp),
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
                                                    val authorId = currentStory.userId
                                                    val storyId = currentStory.id
                                                    val mediaUrl = currentStory.mediaUrl
                                                    val thumbUrl = if (!mediaUrl.isNullOrBlank() && mediaUrl != "text:story") mediaUrl else if (!currentStory.textContent.isNullOrBlank()) "text:${currentStory.textContent}" else mediaUrl
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
                                                Icon(Lucide.Send, contentDescription = "Send", tint = Color(0xFF0A84FF), modifier = Modifier.size(16.dp))
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

                                // Clean Borderless Like Heart Button
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .scale(likeScale.value)
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
                val isUnfinishedStory = isCurrentStoryFailed || isCurrentStoryUploading
                IosAlertDialog(
                    title = if (isUnfinishedStory) "Discard Story?" else "Delete Story?",
                    message = if (isUnfinishedStory) "This will cancel and remove this story upload." else "This will permanently remove this story for all viewers.",
                    confirmText = if (isUnfinishedStory) "Discard" else "Delete",
                    isDestructive = true,
                    onConfirm = {
                        if (currentStory.id.startsWith("temp_")) {
                            storyViewModel.discardFailedUpload(currentStory.id)
                        } else {
                            if (isHighlightViewer) {
                                deletedHighlightStoryIds.add(currentStory.id)
                            }
                            storyViewModel.deleteStory(currentStory.id)
                        }
                        showDeleteConfirm = false
                        ToastManager.showToast(if (isUnfinishedStory) "Story discarded" else "Story deleted")
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
                        Icon(
                            imageVector = Lucide.Star,
                            contentDescription = null,
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                )
            }

            audioDetailStoryTarget?.let { targetStory ->
                com.chroma.studio.ui.components.AudioDetailModal(
                    trackId = targetStory.musicTrackId,
                    fallbackTitle = targetStory.musicTitle,
                    fallbackArtist = targetStory.musicArtist,
                    fallbackCoverUrl = targetStory.musicCoverUrl,
                    fallbackAudioUrl = targetStory.musicAudioUrl,
                    onDismiss = { audioDetailStoryTarget = null },
                    onUseAudio = { track ->
                        audioDetailStoryTarget = null
                        com.chroma.studio.data.PendingMusicHandoff.set(
                            com.chroma.studio.data.AttachedMusicSelection(
                                trackId = track.id,
                                title = track.title,
                                artist = track.artist,
                                album = track.album,
                                coverUrl = track.coverUrl,
                                audioUrl = track.audioUrl,
                                startSec = track.bestPartStartSec,
                                durationSec = (track.bestPartEndSec - track.bestPartStartSec).coerceIn(5, maxOf(5, track.durationSec)),
                                totalTrackSec = track.durationSec,
                                showLyricsSticker = true
                            )
                        )
                        ToastManager.showToast("Audio selected: ${track.title}")
                        onClose()
                    },
                    onFlashClick = {
                        audioDetailStoryTarget = null
                        onClose()
                    },
                    onPostClick = { clickedPost ->
                        audioDetailStoryTarget = null
                        if (clickedPost.creatorId.isNotBlank()) {
                            onClose()
                            onAuthorClick(clickedPost.creatorId)
                        }
                    }
                )
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
    if (timestamp <= 0L) return "Highlight"
    val now = System.currentTimeMillis()
    val diff = (now - timestamp).coerceAtLeast(0L)
    val minutes = diff / 60000
    val hours = minutes / 60
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        hours < 48 -> "Yesterday"
        else -> {
            val calStory = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
            val calNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
            val pattern = if (calStory.get(java.util.Calendar.YEAR) == calNow.get(java.util.Calendar.YEAR)) {
                "MMM d"
            } else {
                "MMM d, yyyy"
            }
            java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(java.util.Date(timestamp))
        }
    }
}
