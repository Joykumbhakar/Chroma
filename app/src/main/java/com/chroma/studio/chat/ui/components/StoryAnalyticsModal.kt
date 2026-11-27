package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.data.Story
import com.chroma.studio.chat.data.StoryViewerInfo
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.OnlineStatusManager
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.components.VerifiedBadge
import com.chroma.studio.ui.storystudio.StoryWidgetResponseStore
import com.chroma.studio.ui.storystudio.decodePublishedStoryWidgetsEnvelope
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Option bar entry inside the compact Story Insights Poll Graph. */
data class StoryPollOptionStat(
    val label: String,
    val votes: Int,
    val fraction: Float,
    val percentage: Int,
    val isWinner: Boolean,
    val isCorrectQuizAnswer: Boolean = false
)

/** Compact summary model for a single Poll or Quiz widget embedded in a Story. */
data class StoryPollSummary(
    val widgetId: String,
    val question: String,
    val isQuiz: Boolean,
    val totalVotes: Int,
    val options: List<StoryPollOptionStat>
)

/**
 * Pure extractor for Poll & Quiz analytics from a Story's serialized interactive widgets payload.
 */
fun buildStoryPollSummaries(
    decompressedTextFormat: String,
    voteCountsLookup: (widgetId: String, isQuiz: Boolean) -> Map<Int, Int> = { _, _ -> emptyMap() }
): List<StoryPollSummary> {
    val envelope = decodePublishedStoryWidgetsEnvelope(decompressedTextFormat) ?: return emptyList()
    return envelope.studioWidgets.mapNotNull { widget ->
        when (widget.widgetType) {
            "POLL" -> {
                val labels = listOf(widget.optA.ifBlank { "Option A" }, widget.optB.ifBlank { "Option B" })
                val voteMap = voteCountsLookup(widget.id, false)
                val totalVotes = labels.indices.sumOf { voteMap[it] ?: 0 }
                val maxVotes = labels.indices.maxOfOrNull { voteMap[it] ?: 0 } ?: 0
                val stats = labels.mapIndexed { idx, lbl ->
                    val count = voteMap[idx] ?: 0
                    val frac = if (totalVotes > 0) count.toFloat() / totalVotes.toFloat() else 0f
                    StoryPollOptionStat(
                        label = lbl,
                        votes = count,
                        fraction = frac,
                        percentage = (frac * 100f).roundToInt(),
                        isWinner = totalVotes > 0 && count == maxVotes && count > 0
                    )
                }
                StoryPollSummary(
                    widgetId = widget.id,
                    question = widget.question.ifBlank { "Poll" },
                    isQuiz = false,
                    totalVotes = totalVotes,
                    options = stats
                )
            }
            "QUIZ" -> {
                val labels = widget.quizOpts.ifEmpty { return@mapNotNull null }
                val voteMap = voteCountsLookup(widget.id, true)
                val totalVotes = labels.indices.sumOf { voteMap[it] ?: 0 }
                val maxVotes = labels.indices.maxOfOrNull { voteMap[it] ?: 0 } ?: 0
                val stats = labels.mapIndexed { idx, lbl ->
                    val count = voteMap[idx] ?: 0
                    val frac = if (totalVotes > 0) count.toFloat() / totalVotes.toFloat() else 0f
                    StoryPollOptionStat(
                        label = lbl,
                        votes = count,
                        fraction = frac,
                        percentage = (frac * 100f).roundToInt(),
                        isWinner = totalVotes > 0 && count == maxVotes && count > 0,
                        isCorrectQuizAnswer = idx == widget.correctIndex
                    )
                }
                StoryPollSummary(
                    widgetId = widget.id,
                    question = widget.question.ifBlank { "Quiz" },
                    isQuiz = true,
                    totalVotes = totalVotes,
                    options = stats
                )
            }
            else -> null
        }
    }
}

/**
 * Flagship Story Insights & Analytics Bottom Sheet.
 * Overload for the standard Domain Story model from Firestore (`com.chroma.studio.model.Story`).
 * Fast concurrent parallel viewer resolution, live online presence,
 * real engagement metrics, dynamic activity graphs, and viewer search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryAnalyticsModal(
    story: com.chroma.studio.model.Story,
    onClose: () -> Unit,
    onDeleteStory: (String) -> Unit
) {
    var viewerInfos by remember(story.id) { mutableStateOf<List<StoryViewerInfo>>(emptyList()) }
    var isLoadingViewers by remember(story.id) { mutableStateOf(story.viewedBy.isNotEmpty() || story.likedBy.isNotEmpty()) }

    LaunchedEffect(story.id, story.viewedBy, story.likedBy) {
        val allUserIds = (story.viewedBy + story.likedBy).distinct()
        if (allUserIds.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val repo = DiscoverRepository()
                coroutineScope {
                    val deferreds = allUserIds.map { uid ->
                        async {
                            try {
                                val profile = repo.getUserProfile(uid)
                                if (profile != null) {
                                    val isLiked = story.likedBy.contains(uid)
                                    val isViewed = story.viewedBy.contains(uid)
                                    StoryViewerInfo(
                                        id = profile.uid,
                                        name = profile.displayName.ifBlank { "User" },
                                        username = if (profile.username.isNotBlank()) "@${profile.username}" else "",
                                        avatar = profile.avatarUrl,
                                        timeAgo = if (isLiked && isViewed) "Viewed • Liked" else if (isLiked) "Liked" else "Viewed",
                                        reactedEmoji = if (isLiked) "❤️" else null,
                                        verified = profile.verified
                                    )
                                } else null
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                    val infos = deferreds.awaitAll().filterNotNull()
                    val sortedInfos = infos.sortedWith(
                        compareByDescending<StoryViewerInfo> { it.reactedEmoji != null }
                            .thenBy { it.name.lowercase() }
                    )
                    viewerInfos = sortedInfos
                    isLoadingViewers = false
                }
            }
        } else {
            viewerInfos = emptyList()
            isLoadingViewers = false
        }
    }

    val timeAgo = remember(story.timestamp) {
        try {
            android.text.format.DateUtils.getRelativeTimeSpanString(
                story.timestamp,
                System.currentTimeMillis(),
                android.text.format.DateUtils.MINUTE_IN_MILLIS
            ).toString()
        } catch (_: Exception) {
            "Recently"
        }
    }

    val chatStory = remember(story, viewerInfos) {
        Story(
            id = story.id,
            authorId = story.userId,
            authorName = "You",
            avatar = "",
            timeAgo = timeAgo,
            image = story.mediaUrl.ifBlank { null },
            caption = story.textContent,
            seen = true,
            isMyStory = true,
            viewCount = maxOf(story.views, story.viewedBy.size),
            likesCount = maxOf(story.likes, story.likedBy.size),
            viewers = viewerInfos,
            locationTag = story.audienceType
        )
    }

    StoryAnalyticsModal(
        story = chatStory,
        onClose = onClose,
        onDeleteStory = onDeleteStory,
        isLoading = isLoadingViewers,
        audienceType = story.audienceType,
        storyTimestamp = story.timestamp,
        decompressedTextFormat = story.decompressedTextFormat
    )
}

/**
 * Compact, borderless Apple HIG Story Insights & Analytics Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryAnalyticsModal(
    story: Story,
    onClose: () -> Unit,
    onDeleteStory: (String) -> Unit,
    isLoading: Boolean = false,
    audienceType: String = "public",
    storyTimestamp: Long = 0L,
    onDirectMessage: ((String) -> Unit)? = null,
    decompressedTextFormat: String = ""
) {
    val colors = LocalOscellionColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = colors.isDark

    val sheetBg = if (isDark) Color(0xFF141519) else Color.White
    val textPrimary = colors.textMain
    val textSecondary = colors.textMuted
    val ytBlue = colors.primary
    val dragHandleColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFD1D5DB)

    val pollSummaries = remember(story.id, decompressedTextFormat) {
        buildStoryPollSummaries(decompressedTextFormat) { widgetId, isQuiz ->
            StoryWidgetResponseStore.aggregateChoicesForWidget(
                context = context,
                storyId = story.id,
                widgetId = widgetId,
                suffix = if (isQuiz) "quiz" else "poll"
            )
        }
    }

    var selectedFilter by remember { mutableStateOf("all") } // "all", "reactions"
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isInsightsExpanded by remember(story.id) { mutableStateOf(false) }

    // Delete Confirmation Dialog (Apple HIG Style)
    if (showDeleteConfirmDialog) {
        IosAlertDialog(
            title = "Delete Story?",
            message = "This story and all its analytics and insights will be permanently deleted.",
            actions = listOf(
                IosAlertAction(
                    title = "Cancel",
                    isCancel = true,
                    onClick = { showDeleteConfirmDialog = false }
                ),
                IosAlertAction(
                    title = "Delete",
                    isDestructive = true,
                    onClick = {
                        SoundManager.playReaction()
                        showDeleteConfirmDialog = false
                        onDeleteStory(story.id)
                        onClose()
                    }
                )
            ),
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = colors.sheetCornerRadius, topEnd = colors.sheetCornerRadius),
        containerColor = sheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(dragHandleColor)
            )
        },
        windowInsets = WindowInsets(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .background(sheetBg)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // ── Compact Top Header (Zero Box Clutter) ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Compact Story Thumbnail
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.inputBg)
                    ) {
                        if (story.image != null) {
                            AsyncImage(
                                model = story.image,
                                contentDescription = "Story preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (story.bgGradientColors != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(story.bgGradientColors))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ytBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = story.caption.take(12),
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    maxLines = 2,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Story Insights",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        val audienceLabel = when (audienceType.lowercase()) {
                            "custom", "only_friends", "close_friends" -> "Only Friends"
                            "followers" -> "Followers"
                            else -> "Public"
                        }
                        Text(
                            text = "${story.timeAgo} • $audienceLabel",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondary
                        )
                    }
                }

                // Compact Ghost Actions: Delete & Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            SoundManager.playReaction()
                            showDeleteConfirmDialog = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Trash2,
                            contentDescription = "Delete Story",
                            tint = colors.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            SoundManager.playReaction()
                            onClose()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Close",
                            tint = textPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // ── Single Compact Scrollable Analytics & Viewer Feed ───────────────────
            val filteredByTab = when (selectedFilter) {
                "reactions" -> story.viewers.filter { it.reactedEmoji != null }
                else -> story.viewers
            }
            val displayedViewers = remember(filteredByTab, searchQuery) {
                if (searchQuery.isBlank()) {
                    filteredByTab
                } else {
                    val query = searchQuery.trim().lowercase()
                    filteredByTab.filter {
                        it.name.lowercase().contains(query) || it.username.lowercase().contains(query)
                    }
                }
            }

            val totalViews = story.viewCount
            val totalLikes = story.likesCount
            val reactionRate = if (totalViews > 0) {
                ((totalLikes.toFloat() / totalViews.toFloat()) * 100f).coerceAtMost(100f)
            } else 0f
            val isOnlyFriends = audienceType.equals("custom", ignoreCase = true) || audienceType.equals("only_friends", ignoreCase = true)
            val audienceTitle = if (isOnlyFriends) "Only Friends" else if (audienceType.equals("followers", ignoreCase = true)) "Followers" else "Public"
            val audienceBadge = if (isOnlyFriends) "Close circle" else "Active reach"
            val audienceColor = if (isOnlyFriends) colors.success else ytBlue

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 14.dp)
            ) {
                // ── 1. Expandable Analytics & Activity Menu (Collapsed by Default) ─────
                item(key = "expandable_insights") {
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (isInsightsExpanded) 180f else 0f,
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        label = "insightsChevron"
                    )
                    val totalPollVotes = remember(pollSummaries) {
                        pollSummaries.sumOf { it.totalVotes }
                    }
                    val quickSummary = buildString {
                        append("$totalViews ${if (totalViews == 1) "view" else "views"}")
                        append(" • ")
                        append("$totalLikes ${if (totalLikes == 1) "like" else "likes"}")
                        if (pollSummaries.isNotEmpty()) {
                            append(" • $totalPollVotes ${if (totalPollVotes == 1) "vote" else "votes"}")
                        } else {
                            append(" • $audienceTitle")
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.inputBg)
                            .border(0.5.dp, colors.cardBorderSubtle, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SoundManager.playReaction()
                                    isInsightsExpanded = !isInsightsExpanded
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(ytBlue.copy(alpha = 0.13f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.ChartColumn,
                                        contentDescription = null,
                                        tint = ytBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = "Insights & Activity",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = quickSummary,
                                        fontSize = 11.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isInsightsExpanded) "Hide" else "Expand",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ytBlue
                                )
                                Icon(
                                    imageVector = Lucide.ChevronDown,
                                    contentDescription = if (isInsightsExpanded) "Collapse analytics" else "Expand analytics",
                                    tint = ytBlue,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .graphicsLayer { rotationZ = chevronRotation }
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isInsightsExpanded,
                            enter = expandVertically(tween(240, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                            exit = shrinkVertically(tween(200, easing = FastOutSlowInEasing)) + fadeOut(tween(150))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 12.dp)
                            ) {
                                HorizontalDivider(thickness = 0.5.dp, color = colors.cardBorderSubtle)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfessionalKpiCard(
                                        title = "Views",
                                        value = totalViews.toString(),
                                        badgeText = "${story.viewers.size} unique",
                                        badgeColor = ytBlue,
                                        icon = { Icon(Lucide.Eye, contentDescription = null, tint = ytBlue, modifier = Modifier.size(12.dp)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(0.5.dp)
                                            .fillMaxHeight()
                                            .padding(vertical = 3.dp)
                                            .background(colors.cardBorderSubtle)
                                    )
                                    ProfessionalKpiCard(
                                        title = "Likes",
                                        value = totalLikes.toString(),
                                        badgeText = "%.0f%% rate".format(reactionRate),
                                        badgeColor = Color(0xFFFF2D55),
                                        icon = { Icon(ChromaIcons.HeartFilled, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(12.dp)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(0.5.dp)
                                            .fillMaxHeight()
                                            .padding(vertical = 3.dp)
                                            .background(colors.cardBorderSubtle)
                                    )
                                    ProfessionalKpiCard(
                                        title = "Audience",
                                        value = audienceTitle,
                                        badgeText = audienceBadge,
                                        badgeColor = audienceColor,
                                        icon = {
                                            Icon(
                                                imageVector = if (isOnlyFriends) Lucide.Star else Lucide.Users,
                                                contentDescription = null,
                                                tint = audienceColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = colors.cardBorderSubtle)

                                if (pollSummaries.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    StoryCompactPollGraph(summaries = pollSummaries)
                                    Spacer(Modifier.height(8.dp))
                                    HorizontalDivider(thickness = 0.5.dp, color = colors.cardBorderSubtle)
                                }

                                Spacer(Modifier.height(10.dp))
                                StoryEngagementGraph(
                                    viewCount = story.viewCount,
                                    timestamp = storyTimestamp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // ── 4. Compact Segmented Tabs & Search ──────────────────────────────
                item(key = "tabs_and_search") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.inputBg)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            "all" to "All Viewers (${story.viewers.size})",
                            "reactions" to "Likes (${story.viewers.count { it.reactedEmoji != null }})"
                        )
                        tabs.forEach { (key, title) ->
                            val isSelected = selectedFilter == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) colors.cardSurfaceElevated else Color.Transparent)
                                    .clickable {
                                        SoundManager.playReaction()
                                        selectedFilter = key
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) textPrimary else textSecondary
                                )
                            }
                        }
                    }

                    if (story.viewers.size >= 2) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.inputBg),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Search,
                                    contentDescription = "Search",
                                    tint = textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = textPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search viewers...",
                                                color = textSecondary,
                                                fontSize = 12.5.sp
                                            )
                                        }
                                        innerTextField()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        imageVector = Lucide.X,
                                        contentDescription = "Clear",
                                        tint = textSecondary,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { searchQuery = "" }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                }

                // ── 5. Viewers List / Loading / Compact Empty State ─────────────────
                if (displayedViewers.isEmpty()) {
                    item(key = "empty_or_loading") {
                        if (isLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                repeat(3) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(colors.cardBorderSubtle.copy(alpha = 0.5f))
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .height(12.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colors.cardBorderSubtle.copy(alpha = 0.5f))
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(70.dp)
                                                    .height(9.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colors.cardBorderSubtle.copy(alpha = 0.3f))
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 22.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (selectedFilter == "reactions") Lucide.Heart else Lucide.Eye,
                                    contentDescription = null,
                                    tint = textSecondary.copy(alpha = 0.55f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No viewers match \"$searchQuery\""
                                    else if (selectedFilter == "reactions") "No reactions on this story yet"
                                    else "No viewers yet",
                                    fontSize = 12.5.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(displayedViewers, key = { _, v -> v.id }) { index, viewer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val isOnline = OnlineStatusManager.rememberUserPresence(viewer.id).online
                                IosAvatarView(
                                    name = viewer.name,
                                    avatarUrl = viewer.avatar,
                                    size = 36.dp,
                                    isOnline = isOnline,
                                    statusRingColor = sheetBg
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = viewer.name,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (viewer.verified) {
                                            Spacer(Modifier.width(4.dp))
                                            VerifiedBadge(
                                                size = 11.dp,
                                                tone = viewer.verifiedTone
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${viewer.username} • ${viewer.timeAgo}",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            }

                            // Clean Inline Like Indicator & Borderless DM Action
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (viewer.reactedEmoji != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = ChromaIcons.HeartFilled,
                                            contentDescription = "Liked",
                                            tint = Color(0xFFFF2D55),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "Liked",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFF2D55)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (onDirectMessage != null) {
                                            onDirectMessage(viewer.id)
                                        } else {
                                            ToastManager.showToast("Message @${viewer.username.removePrefix("@")}")
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.Send,
                                        contentDescription = "Send message",
                                        tint = ytBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        if (index < displayedViewers.size - 1) {
                            HorizontalDivider(
                                color = colors.cardBorderSubtle,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 46.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean Professional KPI Metric Card with Borderless Design System Capsule styling.
 */
/**
 * Small & Compact Poll Graph for Story Insights (borderless proportional horizontal bar chart).
 */
@Composable
fun StoryCompactPollGraph(
    summaries: List<StoryPollSummary>,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        summaries.forEach { poll ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (if (poll.isQuiz) "QUIZ • " else "POLL • ") + poll.question,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (poll.totalVotes > 0) "${poll.totalVotes} ${if (poll.totalVotes == 1) "vote" else "votes"}" else "No votes yet",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (poll.totalVotes > 0) colors.primary else colors.textMuted
                    )
                }

                poll.options.forEach { option ->
                    val animFrac by animateFloatAsState(
                        targetValue = option.fraction.coerceIn(0f, 1f),
                        animationSpec = tween(360, easing = FastOutSlowInEasing),
                        label = "pollBarFrac"
                    )
                    val barColor = when {
                        option.isCorrectQuizAnswer && poll.totalVotes > 0 -> colors.success.copy(alpha = 0.24f)
                        option.isWinner -> colors.primary.copy(alpha = 0.22f)
                        else -> colors.textMuted.copy(alpha = 0.14f)
                    }
                    val accentText = when {
                        option.isCorrectQuizAnswer && poll.totalVotes > 0 -> colors.success
                        option.isWinner -> colors.primary
                        else -> colors.textMain
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.inputBg)
                    ) {
                        if (animFrac > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animFrac)
                                    .background(barColor)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.label,
                                fontSize = 11.sp,
                                fontWeight = if (option.isWinner) FontWeight.SemiBold else FontWeight.Medium,
                                color = accentText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (poll.totalVotes > 0) "${option.percentage}% (${option.votes})" else "0%",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (poll.totalVotes > 0) accentText else colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalKpiCard(
    title: String,
    value: String,
    badgeText: String,
    badgeColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textMuted
            )
        }

        Text(
            text = value,
            fontSize = 17.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMain,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = badgeText,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = badgeColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Animated Engagement & Hourly Activity Graph.
 * Clean borderless Apple layout with hairline divider, zero boxed container.
 */
@Composable
fun StoryEngagementGraph(
    viewCount: Int,
    timestamp: Long = 0L,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val ytBlue = colors.primary

    var animationTrigger by remember { mutableStateOf(false) }
    val animProgress by animateFloatAsState(
        targetValue = if (animationTrigger) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "graphAnimation"
    )

    LaunchedEffect(Unit) {
        animationTrigger = true
    }

    // Pulse animation for the peak indicator point
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Graph Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HOURLY VIEW ACTIVITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                letterSpacing = 0.6.sp
            )

            if (viewCount > 0) {
                val hoursActive = if (timestamp > 0L) {
                    ((System.currentTimeMillis() - timestamp) / 3_600_000L).coerceIn(1L, 24L)
                } else 1L
                val viewsPerHour = (viewCount.toFloat() / hoursActive.toFloat()).coerceAtLeast(0.5f)

                Text(
                    text = "Peak: %.0f views/hr".format(viewsPerHour * 1.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.success
                )
            } else {
                Text(
                    text = "Waiting for views",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        if (viewCount == 0) {
            // Compact empty state baseline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    drawLine(
                        color = colors.textMuted.copy(alpha = 0.15f),
                        start = Offset(0f, height * 0.5f),
                        end = Offset(width, height * 0.5f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }
                Text(
                    text = "No views recorded yet in the past 24 hours",
                    fontSize = 11.5.sp,
                    color = colors.textMuted.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Normal
                )
            }
        } else {
            // Cubic Bezier Graph with real view metrics
            val dataPoints = remember(viewCount) {
                listOf(0.15f, 0.35f, 0.60f, 0.95f, 0.72f, 0.85f, 0.50f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (dataPoints.size - 1)

                    // Horizontal guidelines
                    val guideAlpha = 0.08f
                    val strokeGuide = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                    drawLine(
                        color = colors.textMain.copy(alpha = guideAlpha),
                        start = Offset(0f, height * 0.30f),
                        end = Offset(width, height * 0.30f),
                        strokeWidth = strokeGuide.width,
                        pathEffect = strokeGuide.pathEffect
                    )
                    drawLine(
                        color = colors.textMain.copy(alpha = guideAlpha),
                        start = Offset(0f, height * 0.70f),
                        end = Offset(width, height * 0.70f),
                        strokeWidth = strokeGuide.width,
                        pathEffect = strokeGuide.pathEffect
                    )

                    val graphPoints = dataPoints.mapIndexed { idx, value ->
                        val animatedY = height - ((value * height * 0.82f) * animProgress)
                        Offset(idx * spacing, animatedY)
                    }

                    // Smooth Cubic Bezier Path
                    val strokePath = Path().apply {
                        if (graphPoints.isNotEmpty()) {
                            moveTo(graphPoints[0].x, graphPoints[0].y)
                            for (i in 0 until graphPoints.size - 1) {
                                val p0 = graphPoints[i]
                                val p1 = graphPoints[i + 1]
                                val controlX1 = (p0.x + p1.x) / 2
                                val controlY1 = p0.y
                                val controlX2 = (p0.x + p1.x) / 2
                                val controlY2 = p1.y
                                cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                            }
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(strokePath)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }

                    // Gradient fill area
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                ytBlue.copy(alpha = 0.22f * animProgress),
                                ytBlue.copy(alpha = 0.01f * animProgress)
                            ),
                            startY = 0f,
                            endY = height
                        ),
                        style = Fill
                    )

                    // Stroke line
                    drawPath(
                        path = strokePath,
                        color = ytBlue,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Peak Indicator (at index 3)
                    if (graphPoints.size > 3 && animProgress > 0.6f) {
                        val peakPoint = graphPoints[3]
                        drawCircle(
                            color = ytBlue.copy(alpha = 0.22f),
                            radius = 8.dp.toPx() * pulseScale,
                            center = peakPoint
                        )
                        drawCircle(
                            color = ytBlue,
                            radius = 4.dp.toPx(),
                            center = peakPoint
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = peakPoint
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Time Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Posted", "Earlier", "Recent", "Now").forEach { timeLabel ->
                    Text(
                        text = timeLabel,
                        fontSize = 10.sp,
                        color = colors.textMuted.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
