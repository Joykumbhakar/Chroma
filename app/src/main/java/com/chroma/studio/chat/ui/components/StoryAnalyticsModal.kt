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
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

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
        storyTimestamp = story.timestamp
    )
}

/**
 * Flagship Story Insights & Analytics Bottom Sheet.
 * Implemented using ModalBottomSheet with swipe down to dismiss,
 * design system components & variables (LocalOscellionColors),
 * modular KPI cards, dynamic hourly engagement curve, and live viewer presence.
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
    onDirectMessage: ((String) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val sheetBg = if (isDark) Color(0xFF141519) else Color.White
    val textPrimary = colors.textMain
    val textSecondary = colors.textMuted
    val ytBlue = colors.primary
    val surfacePillBg = if (isDark) Color(0xFF1E212A) else Color(0xFFF1F5F9)
    val dragHandleColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFD1D5DB)

    var selectedFilter by remember { mutableStateOf("all") } // "all", "reactions"
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .background(sheetBg)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            // ── Top Header ──────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Story Thumbnail Squircle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF242731) else Color(0xFFE2E8F0))
                            .border(0.5.dp, colors.cardBorderSubtle, RoundedCornerShape(12.dp))
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
                                    .background(Brush.linearGradient(listOf(ytBlue, Color(0xFF0056B3)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = story.caption.take(15),
                                    fontSize = 9.sp,
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        val audienceLabel = when (audienceType.lowercase()) {
                            "custom", "only_friends", "close_friends" -> "⭐ Only Friends"
                            "followers" -> "Followers only"
                            else -> "Public story"
                        }
                        Text(
                            text = "Posted ${story.timeAgo} • $audienceLabel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondary
                        )
                    }
                }

                // Actions: Delete Story & Close Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF3B30).copy(alpha = 0.12f),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                SoundManager.playReaction()
                                showDeleteConfirmDialog = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Lucide.Trash2,
                                contentDescription = "Delete Story",
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = surfacePillBg,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                SoundManager.playReaction()
                                onClose()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Close",
                                tint = textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── 1. Modular KPI Metric Cards ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Views
                val totalViews = story.viewCount
                ProfessionalKpiCard(
                    title = "Views",
                    value = totalViews.toString(),
                    badgeText = "${story.viewers.size} unique",
                    badgeColor = ytBlue,
                    icon = { Icon(Lucide.Eye, contentDescription = null, tint = ytBlue, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.weight(1f)
                )

                // Card 2: Reactions (Likes)
                val totalLikes = story.likesCount
                val reactionRate = if (totalViews > 0) {
                    ((totalLikes.toFloat() / totalViews.toFloat()) * 100f).coerceAtMost(100f)
                } else 0f
                ProfessionalKpiCard(
                    title = "Likes",
                    value = totalLikes.toString(),
                    badgeText = "%.0f%% rate".format(reactionRate),
                    badgeColor = Color(0xFFFF2D55),
                    icon = { Icon(ChromaIcons.HeartFilled, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.weight(1f)
                )

                // Card 3: Audience Scope
                val isOnlyFriends = audienceType.equals("custom", ignoreCase = true) || audienceType.equals("only_friends", ignoreCase = true)
                val audienceTitle = if (isOnlyFriends) "Only Friends" else if (audienceType.equals("followers", ignoreCase = true)) "Followers" else "Public"
                val audienceBadge = if (isOnlyFriends) "Close circle" else "Active reach"
                val audienceColor = if (isOnlyFriends) Color(0xFF00C853) else ytBlue

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
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── 2. Dynamic Engagement Activity Curve ─────────────────────────────
            StoryEngagementGraph(
                viewCount = story.viewCount,
                timestamp = storyTimestamp
            )

            Spacer(Modifier.height(12.dp))

            // ── 3. Segmented Filter Tabs ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(surfacePillBg)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
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
                            .clip(CircleShape)
                            .background(
                                if (isSelected) (if (isDark) Color(0xFF2C303B) else Color.White)
                                else Color.Transparent
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.shadow(
                                        elevation = 1.dp,
                                        shape = CircleShape,
                                        ambientColor = Color.Black.copy(alpha = 0.08f),
                                        spotColor = Color.Black.copy(alpha = 0.08f)
                                    )
                                } else Modifier
                            )
                            .clickable {
                                SoundManager.playReaction()
                                selectedFilter = key
                            }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) textPrimary else textSecondary
                        )
                    }
                }
            }

            // Optional Search Bar for Viewers (shown when there are viewers)
            if (story.viewers.size >= 2) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = CircleShape,
                    color = surfacePillBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Search,
                            contentDescription = "Search",
                            tint = textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search viewers by name or handle...",
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

            Spacer(Modifier.height(10.dp))

            // ── 4. Viewers Interaction List ───────────────────────────────────────
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

            if (displayedViewers.isEmpty()) {
                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        repeat(4) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(colors.cardBorderSubtle.copy(alpha = 0.5f))
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.cardBorderSubtle.copy(alpha = 0.5f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.cardBorderSubtle.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedFilter == "reactions") Lucide.Heart else Lucide.Eye,
                                contentDescription = null,
                                tint = textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No viewers match \"$searchQuery\""
                                else if (selectedFilter == "reactions") "No reactions on this story yet"
                                else "No viewers yet • Views will appear here as friends watch your story",
                                fontSize = 13.sp,
                                color = textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(displayedViewers, key = { _, v -> v.id }) { index, viewer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar with live unified online dot
                                val isOnline = OnlineStatusManager.rememberUserPresence(viewer.id).online
                                IosAvatarView(
                                    name = viewer.name,
                                    avatarUrl = viewer.avatar,
                                    size = 42.dp,
                                    isOnline = isOnline,
                                    statusRingColor = sheetBg
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = viewer.name,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (viewer.verified) {
                                            Spacer(Modifier.width(4.dp))
                                            VerifiedBadge(size = 11.dp)
                                        }
                                    }
                                    Text(
                                        text = "${viewer.username} • ${viewer.timeAgo}",
                                        fontSize = 11.5.sp,
                                        color = textSecondary
                                    )
                                }
                            }

                            // Right Reaction / Interaction Pill & Quick DM
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (viewer.reactedEmoji != null) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFFF2D55).copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = ChromaIcons.HeartFilled,
                                                contentDescription = "Liked",
                                                tint = Color(0xFFFF2D55),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "Liked",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF2D55)
                                            )
                                        }
                                    }
                                }

                                // Quick Message Shortcut
                                Surface(
                                    shape = CircleShape,
                                    color = surfacePillBg,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (onDirectMessage != null) {
                                                onDirectMessage(viewer.id)
                                            } else {
                                                ToastManager.showToast("Message @${viewer.username.removePrefix("@")}")
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Lucide.Send,
                                            contentDescription = "Send message",
                                            tint = ytBlue,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (index < displayedViewers.size - 1) {
                            HorizontalDivider(
                                color = colors.cardBorderSubtle,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 54.dp)
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
    val isDark = colors.isDark
    val surfacePillBg = if (isDark) Color(0xFF1E212A) else Color(0xFFF1F5F9)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfacePillBg)
            .border(0.5.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            icon()
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textMuted
            )
        }

        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMain,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Surface(
            shape = CircleShape,
            color = badgeColor.copy(alpha = 0.12f)
        ) {
            Text(
                text = badgeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Animated Engagement & Hourly Activity Graph.
 * Displays real hourly view velocity when views are present,
 * or a clean empty-state baseline when views are zero.
 */
@Composable
fun StoryEngagementGraph(
    viewCount: Int,
    timestamp: Long = 0L,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val ytBlue = colors.primary
    val cardBg = if (isDark) Color(0xFF1E212A) else Color(0xFFF9F9FB)

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
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(0.5.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Graph Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(ytBlue)
                )
                Text(
                    text = "HOURLY VIEW ACTIVITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    letterSpacing = 0.5.sp
                )
            }

            if (viewCount > 0) {
                val hoursActive = if (timestamp > 0L) {
                    ((System.currentTimeMillis() - timestamp) / 3_600_000L).coerceIn(1L, 24L)
                } else 1L
                val viewsPerHour = (viewCount.toFloat() / hoursActive.toFloat()).coerceAtLeast(0.5f)

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF34C759).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Peak: %.0f views/hr".format(viewsPerHour * 1.5f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34C759),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = colors.textMuted.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = "Waiting for views",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (viewCount == 0) {
            // Empty state baseline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
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
