package com.chroma.studio.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.DefaultGenderAvatar
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.VerifiedBadge
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

// ============================================================================
// DATA MODELS & ENUMS
// ============================================================================

enum class NotificationFilter(val label: String, val icon: ImageVector) {
    ALL("All", Lucide.LayoutGrid),
    LIKES("Likes", Lucide.Heart),
    COMMENTS("Comments", Lucide.MessageCircle),
    FOLLOWS("Follows", Lucide.UserPlus),
    MESSAGES("Messages", Lucide.Send),
    PROJECTS("Projects", Lucide.Layers)
}

typealias NotificationCategory = NotificationFilter

enum class NotificationActionType {
    LIKE,
    COMMENT,
    FOLLOW,
    MESSAGE,
    REMIX,
    MENTION,
    SYSTEM_DROP,
    PROJECT_EXPORT,
    PROJECT_COLLAB
}

data class ChromaNotification(
    val id: String,
    val type: NotificationActionType,
    val actorUid: String,
    val actorName: String,
    val actorHandle: String,
    val actorAvatarUrl: String? = null,
    val actorGender: String? = null,
    val isVerified: Boolean = false,
    val actionText: String = "",
    val detailText: String? = null,
    val targetPreviewUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetId: String? = null,
    val following: Boolean = false,
    val likedComment: Boolean = false,
    val likedBack: Boolean = false
) {
    val effectiveCategory: NotificationFilter
        get() = when (type) {
            NotificationActionType.LIKE -> NotificationFilter.LIKES
            NotificationActionType.COMMENT -> NotificationFilter.COMMENTS
            NotificationActionType.FOLLOW -> NotificationFilter.FOLLOWS
            NotificationActionType.MESSAGE -> NotificationFilter.MESSAGES
            NotificationActionType.REMIX,
            NotificationActionType.PROJECT_EXPORT,
            NotificationActionType.PROJECT_COLLAB -> NotificationFilter.PROJECTS
            NotificationActionType.MENTION -> NotificationFilter.COMMENTS
            NotificationActionType.SYSTEM_DROP -> NotificationFilter.ALL
        }

    val isUnread: Boolean get() = !isRead

    fun toMap(): Map<String, Any?> = mapOf(
        "type" to type.name,
        "actorUid" to actorUid,
        "actorName" to actorName,
        "actorHandle" to actorHandle,
        "actorAvatarUrl" to actorAvatarUrl,
        "actorGender" to actorGender,
        "isVerified" to isVerified,
        "actionText" to actionText,
        "detailText" to detailText,
        "targetPreviewUrl" to targetPreviewUrl,
        "timestamp" to timestamp,
        "isRead" to isRead,
        "targetId" to targetId
    )
}

// ============================================================================
// GLASS HR LINE DIVIDER
// ============================================================================

@Composable
private fun GlassHrLine(
    modifier: Modifier = Modifier,
    color: Color? = null,
    thickness: Dp = 0.5.dp
) {
    val colors = LocalOscellionColors.current
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = thickness,
        color = color ?: colors.glassBorder.copy(alpha = 0.45f)
    )
}

// ============================================================================
// AVATAR WITH BADGE
// ============================================================================

@Composable
private fun NotifAvatarWithBadge(
    notification: ChromaNotification,
    onAvatarClick: (() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val (badgeBgColor, badgeIconColor, badgeIcon) = when (notification.type) {
        NotificationActionType.LIKE        -> Triple(Color(0xFFFFE4E6), Color(0xFFE11D48), Lucide.Heart)
        NotificationActionType.COMMENT     -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Lucide.MessageCircle)
        NotificationActionType.FOLLOW      -> Triple(Color(0xFFD1FAE5), Color(0xFF059669), Lucide.UserPlus)
        NotificationActionType.MESSAGE     -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), Lucide.Send)
        NotificationActionType.REMIX       -> Triple(Color(0xFFEDE9FE), Color(0xFF7C3AED), Lucide.Sparkles)
        NotificationActionType.MENTION     -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), Lucide.AtSign)
        NotificationActionType.SYSTEM_DROP -> Triple(Color(0xFFFEF3C7), Color(0xFFF59E0B), Lucide.Bell)
        NotificationActionType.PROJECT_EXPORT -> Triple(Color(0xFFECFDF5), Color(0xFF0D9488), Lucide.Video)
        NotificationActionType.PROJECT_COLLAB -> Triple(Color(0xFFEEF2FF), Color(0xFF4F46E5), Lucide.Users)
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .then(
                if (onAvatarClick != null) Modifier.clickable { onAvatarClick() }
                else Modifier
            ),
        contentAlignment = Alignment.BottomEnd
    ) {
        DefaultGenderAvatar(
            gender = notification.actorGender,
            avatarUrlOrPreset = notification.actorAvatarUrl,
            name = notification.actorName,
            size = 38.dp
        )

        Box(
            modifier = Modifier
                .size(15.dp)
                .offset(x = 2.dp, y = 2.dp)
                .clip(CircleShape)
                .background(badgeBgColor)
                .border(1.5.dp, colors.bg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = badgeIconColor,
                modifier = Modifier.size(8.dp)
            )
        }
    }
}

// ============================================================================
// SECTION HEADER ROW (Clean flat row architecture)
// ============================================================================

@Composable
private fun SectionHeaderRow(
    title: String,
    count: Int,
    isAccent: Boolean
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isAccent) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = if (isAccent) colors.primary else colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textMuted,
            maxLines = 1
        )
    }
}

// ============================================================================
// SWIPEABLE NOTIFICATION ROW (Flat HR Line Architecture — Icon-Only Actions)
// ============================================================================

@Composable
private fun SwipeableNotificationCard(
    notification: ChromaNotification,
    isOpenLeftTray: Boolean,
    onOpenSwipeChanged: (String?) -> Unit,
    onCardClick: (ChromaNotification) -> Unit,
    onAvatarClick: (ChromaNotification) -> Unit,
    onSwipeRightRemove: (ChromaNotification) -> Unit,
    onPrimaryAction: (ChromaNotification) -> Unit,
    onToggleRead: (ChromaNotification) -> Unit
) {
    val colors = LocalOscellionColors.current
    val density = LocalDensity.current
    val snapLeftPx = with(density) { (-100).dp.toPx() }
    val removeThresholdPx = with(density) { 90.dp.toPx() }
    val fullLeftThresholdPx = with(density) { (-120).dp.toPx() }
    val partialLeftThresholdPx = with(density) { (-40).dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isOpenLeftTray) {
        val target = if (isOpenLeftTray) snapLeftPx else 0f
        if (offsetX.value != target) {
            offsetX.animateTo(target, spring(dampingRatio = 0.8f, stiffness = 400f))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
    ) {
        // SWIPE RIGHT tray — Remove Action
        if (offsetX.value > 4f) {
            val rightAlpha = (offsetX.value / (removeThresholdPx * 0.7f)).coerceIn(0f, 1f)
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = rightAlpha }
                    .background(colors.error)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Lucide.Trash2,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // SWIPE LEFT tray — Contextual Actions (ONLY ICONS)
        if (offsetX.value < -4f || isOpenLeftTray) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .background(colors.glassBgHover)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                val primaryIcon = when (notification.type) {
                    NotificationActionType.MESSAGE -> Lucide.Send
                    NotificationActionType.FOLLOW  -> if (notification.following) Lucide.UserCheck else Lucide.UserPlus
                    NotificationActionType.COMMENT -> Lucide.Heart
                    else -> Lucide.Heart
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                        .clickable { onPrimaryAction(notification) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = primaryIcon,
                        contentDescription = "Action",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.glassBg)
                        .border(1.dp, colors.glassBorder, CircleShape)
                        .clickable { onToggleRead(notification) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (notification.isUnread) Lucide.CheckCheck else Lucide.Mail,
                        contentDescription = "Toggle Read",
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // FOREGROUND ROW (Flat continuous row separated by HR lines — no card box)
        Row(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(
                    if (notification.isUnread) colors.glassBgHover.copy(alpha = 0.5f)
                    else Color.Transparent
                )
                .pointerInput(notification.id, isOpenLeftTray) {
                    detectHorizontalDragGestures(
                        onDragStart = { if (!isOpenLeftTray) onOpenSwipeChanged(null) },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val raw = offsetX.value + dragAmount
                                val damped = when {
                                    raw > 180.dp.toPx() -> 180.dp.toPx() + (raw - 180.dp.toPx()) * 0.2f
                                    raw < (-140).dp.toPx() -> (-140).dp.toPx() + (raw + 140.dp.toPx()) * 0.2f
                                    else -> raw
                                }
                                offsetX.snapTo(damped)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value > removeThresholdPx -> {
                                        offsetX.animateTo(1000f, tween(200, easing = FastOutSlowInEasing))
                                        onSwipeRightRemove(notification)
                                    }
                                    offsetX.value < fullLeftThresholdPx -> {
                                        onOpenSwipeChanged(null)
                                        offsetX.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 400f))
                                        onPrimaryAction(notification)
                                    }
                                    offsetX.value < partialLeftThresholdPx -> {
                                        onOpenSwipeChanged(notification.id)
                                        offsetX.animateTo(snapLeftPx, spring(dampingRatio = 0.8f, stiffness = 400f))
                                    }
                                    else -> {
                                        onOpenSwipeChanged(null)
                                        offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                onOpenSwipeChanged(null)
                                offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                            }
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isOpenLeftTray) onOpenSwipeChanged(null)
                    else onCardClick(notification)
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Avatar + Details
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NotifAvatarWithBadge(
                    notification = notification,
                    onAvatarClick = { onAvatarClick(notification) }
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Line 1: User name, Verification, Dot, Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = notification.actorName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (notification.isVerified) {
                            VerifiedBadge(size = 12.dp)
                        }
                        if (notification.isUnread) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                            )
                        }
                        Text(
                            text = "• ${formatRelativeTime(notification.timestamp)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }

                    // Line 2: Action text + Snippet with ellipsis (...)
                    val combinedText = remember(notification.actionText, notification.detailText) {
                        if (!notification.detailText.isNullOrBlank()) {
                            "${notification.actionText}: ${notification.detailText}"
                        } else {
                            notification.actionText
                        }
                    }

                    Text(
                        text = combinedText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Right: Contextual Action (ONLY ICONS)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (notification.type) {
                    NotificationActionType.FOLLOW -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (notification.following) colors.glassBgHover else colors.primary
                                )
                                .border(
                                    1.dp,
                                    if (notification.following) colors.glassBorder else colors.primary,
                                    CircleShape
                                )
                                .clickable { onPrimaryAction(notification) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (notification.following) Lucide.UserCheck else Lucide.UserPlus,
                                contentDescription = if (notification.following) "Following" else "Follow",
                                tint = if (notification.following) colors.textMain else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    NotificationActionType.MESSAGE -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.glassBg)
                                .border(1.dp, colors.glassBorder, CircleShape)
                                .clickable { onPrimaryAction(notification) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Send,
                                contentDescription = "Reply",
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    NotificationActionType.COMMENT -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.glassBg)
                                .border(1.dp, colors.glassBorder, CircleShape)
                                .clickable { onPrimaryAction(notification) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Heart,
                                contentDescription = "Like",
                                tint = if (notification.likedComment) colors.primary else colors.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    else -> {
                        if (!notification.targetPreviewUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = notification.targetPreviewUrl,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(6.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SHIMMER SKELETON
// ============================================================================

@Composable
private fun FeedSkeletonList(rowCount: Int = 5) {
    val colors = LocalOscellionColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX = transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "shimmerX"
    )
    val shimmer = Brush.linearGradient(
        colors = listOf(
            colors.glassBg,
            colors.glassBgHover,
            colors.glassBg
        ),
        start = Offset(shimmerX.value, 0f),
        end = Offset(shimmerX.value + 350f, 150f)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(rowCount) { idx ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(shimmer)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (idx % 2 == 0) 0.55f else 0.4f)
                            .height(11.dp)
                            .clip(CircleShape)
                            .background(shimmer)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(9.dp)
                            .clip(CircleShape)
                            .background(shimmer)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(shimmer)
                )
            }
            GlassHrLine()
        }
    }
}

// ============================================================================
// CLEAN COMPACT EMPTY STATE
// ============================================================================

@Composable
private fun NotificationEmptyState(
    filterLabel: String,
    onRefresh: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.glassBg)
                .border(1.dp, colors.glassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.BellOff,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "All Caught Up",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMain,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "No $filterLabel notifications right now",
            fontSize = 12.5.sp,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.glassBg)
                .border(1.dp, colors.glassBorder, CircleShape)
                .clickable { onRefresh() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Lucide.RefreshCw,
                    contentDescription = "Refresh",
                    tint = colors.primary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Refresh",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary
                )
            }
        }
    }
}

// ============================================================================
// NOTIFICATIONS FEED SCREEN — MAIN COMPOSABLE
// ============================================================================

@Composable
fun NotificationsFeedScreen(
    onBack: () -> Unit,
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToProfile: ((userId: String) -> Unit)? = null,
    onNavigateToChat: ((roomId: String) -> Unit)? = null,
    onNavigateToPost: ((postId: String, authorId: String?) -> Unit)? = null,
    onNavigateToProject: ((draftId: String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val colors = LocalOscellionColors.current

    // Firebase Data
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid }
    var notificationsList by remember { mutableStateOf(defaultCommunityNotifications) }
    val discoverRepo = remember { DiscoverRepository() }

    // UI States
    var selectedFilter by remember { mutableStateOf(NotificationFilter.ALL) }
    var unreadOnly by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isFeedLoading by remember { mutableStateOf(true) }
    var isManualRefreshing by remember { mutableStateOf(false) }
    var openSwipeCardId by remember { mutableStateOf<String?>(null) }
    var lastRemovedBackup by remember { mutableStateOf<Pair<ChromaNotification, Int>?>(null) }

    // Toast
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastShowUndo by remember { mutableStateOf(false) }
    var toastJob by remember { mutableStateOf<Job?>(null) }

    fun showToast(msg: String, showUndo: Boolean = false) {
        toastJob?.cancel()
        toastMessage = msg
        toastShowUndo = showUndo
        toastJob = scope.launch {
            delay(if (showUndo) 3600L else 2200L)
            toastMessage = null
            toastShowUndo = false
        }
    }

    // Realtime Firestore Snapshot Listener
    DisposableEffect(currentUid) {
        if (currentUid == null) {
            notificationsList = defaultCommunityNotifications
            isFeedLoading = false
            return@DisposableEffect onDispose {}
        }

        val registration = FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(80)
            .addSnapshotListener { snapshot, error ->
                isFeedLoading = false
                isManualRefreshing = false

                if (error != null) {
                    if (notificationsList.isEmpty()) {
                        notificationsList = defaultCommunityNotifications
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val fetched = snapshot.documents.mapNotNull { doc ->
                        try {
                            ChromaNotification(
                                id = doc.id,
                                type = runCatching {
                                    NotificationActionType.valueOf(doc.getString("type") ?: "LIKE")
                                }.getOrDefault(NotificationActionType.LIKE),
                                actorUid = doc.getString("actorUid") ?: "",
                                actorName = doc.getString("actorName") ?: "Unknown",
                                actorHandle = doc.getString("actorHandle") ?: "",
                                actorAvatarUrl = doc.getString("actorAvatarUrl"),
                                actorGender = doc.getString("actorGender"),
                                isVerified = doc.getBoolean("isVerified") ?: false,
                                actionText = doc.getString("actionText") ?: "",
                                detailText = doc.getString("detailText"),
                                targetPreviewUrl = doc.getString("targetPreviewUrl"),
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                isRead = doc.getBoolean("isRead") ?: false,
                                targetId = doc.getString("targetId")
                            )
                        } catch (_: Exception) { null }
                    }
                    if (fetched.isNotEmpty()) {
                        notificationsList = fetched
                    }
                } else if (snapshot != null && snapshot.isEmpty) {
                    if (notificationsList.isEmpty()) {
                        notificationsList = defaultCommunityNotifications
                    }
                }
            }

        onDispose {
            registration.remove()
        }
    }

    // Filtered Feed List
    val filteredNotifications = remember(notificationsList, selectedFilter, unreadOnly, searchQuery) {
        val q = searchQuery.trim().lowercase()
        notificationsList.filter { item ->
            if (selectedFilter != NotificationFilter.ALL && item.effectiveCategory != selectedFilter) return@filter false
            if (unreadOnly && item.isRead) return@filter false
            if (q.isNotEmpty()) {
                item.actorName.lowercase().contains(q) ||
                    item.actorHandle.lowercase().contains(q) ||
                    (item.detailText?.lowercase()?.contains(q) == true) ||
                    item.actionText.lowercase().contains(q)
            } else true
        }
    }

    // Mutation Operations
    fun markAsRead(notifId: String) {
        val target = notificationsList.find { it.id == notifId }
        if (target == null || target.isRead) return

        notificationsList = notificationsList.map { if (it.id == notifId) it.copy(isRead = true) else it }
        if (currentUid != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(currentUid)
                        .collection("notifications").document(notifId)
                        .update("isRead", true)
                } catch (_: Exception) {}
            }
        }
    }

    fun markAllAsRead() {
        val unread = notificationsList.filter { !it.isRead }
        if (unread.isEmpty()) return

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        SoundManager.playReaction()
        notificationsList = notificationsList.map { it.copy(isRead = true) }
        showToast("All marked as read")

        if (currentUid != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    val col = db.collection("users").document(currentUid).collection("notifications")
                    unread.chunked(500).forEach { chunk ->
                        val batch = db.batch()
                        chunk.forEach { n -> batch.update(col.document(n.id), "isRead", true) }
                        batch.commit().await()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun toggleRead(notification: ChromaNotification) {
        val nextRead = !notification.isRead
        openSwipeCardId = null
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        notificationsList = notificationsList.map {
            if (it.id == notification.id) it.copy(isRead = nextRead) else it
        }
        showToast(if (!nextRead) "Marked as unread" else "Marked as read")

        if (currentUid != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(currentUid)
                        .collection("notifications").document(notification.id)
                        .update("isRead", nextRead)
                } catch (_: Exception) {}
            }
        }
    }

    fun removeNotification(removed: ChromaNotification) {
        val idx = notificationsList.indexOfFirst { it.id == removed.id }
        if (idx != -1) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            lastRemovedBackup = Pair(removed, idx)
            notificationsList = notificationsList.filterNot { it.id == removed.id }
            if (openSwipeCardId == removed.id) openSwipeCardId = null
            showToast("Notification removed", showUndo = true)

            if (currentUid != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        FirebaseFirestore.getInstance()
                            .collection("users").document(currentUid)
                            .collection("notifications").document(removed.id)
                            .delete()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun undoRemove() {
        val backup = lastRemovedBackup ?: return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val mutable = notificationsList.toMutableList()
        mutable.add(backup.second.coerceIn(0, mutable.size), backup.first)
        notificationsList = mutable
        lastRemovedBackup = null
        showToast("Notification restored")

        if (currentUid != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(currentUid)
                        .collection("notifications").document(backup.first.id)
                        .set(backup.first.toMap())
                } catch (_: Exception) {}
            }
        }
    }

    fun handlePrimaryAction(item: ChromaNotification) {
        openSwipeCardId = null
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        markAsRead(item.id)

        when (item.type) {
            NotificationActionType.MESSAGE -> {
                if (item.actorUid.isNotBlank()) {
                    val roomId = if (currentUid != null && currentUid < item.actorUid) {
                        "${currentUid}_${item.actorUid}"
                    } else if (currentUid != null) {
                        "${item.actorUid}_${currentUid}"
                    } else item.actorUid

                    if (onNavigateToChat != null) {
                        onNavigateToChat(roomId)
                    } else {
                        showToast("Opening chat with ${item.actorHandle}")
                    }
                }
            }
            NotificationActionType.FOLLOW -> {
                val next = !item.following
                notificationsList = notificationsList.map { if (it.id == item.id) it.copy(following = next, isRead = true) else it }
                SoundManager.playTap(context)
                showToast(if (next) "Following ${item.actorHandle}" else "Unfollowed ${item.actorHandle}")
                if (currentUid != null && item.actorUid.isNotBlank()) {
                    scope.launch { try { discoverRepo.toggleFollow(currentUid, item.actorUid) } catch (_: Exception) {} }
                }
            }
            NotificationActionType.COMMENT -> {
                val next = !item.likedComment
                notificationsList = notificationsList.map { if (it.id == item.id) it.copy(likedComment = next, isRead = true) else it }
                SoundManager.playPop()
                showToast(if (next) "Liked comment" else "Removed like")
            }
            else -> {
                val next = !item.likedBack
                notificationsList = notificationsList.map { if (it.id == item.id) it.copy(likedBack = next, isRead = true) else it }
                SoundManager.playPop()
                showToast(if (next) "Sent thanks to ${item.actorHandle}" else "Removed reaction")
            }
        }
    }

    fun handleCardClick(clicked: ChromaNotification) {
        markAsRead(clicked.id)
        when (clicked.type) {
            NotificationActionType.MESSAGE -> {
                if (clicked.actorUid.isNotBlank()) {
                    val roomId = if (currentUid != null && currentUid < clicked.actorUid) {
                        "${currentUid}_${clicked.actorUid}"
                    } else if (currentUid != null) {
                        "${clicked.actorUid}_${currentUid}"
                    } else clicked.actorUid

                    if (onNavigateToChat != null) {
                        onNavigateToChat(roomId)
                    } else {
                        showToast("Opening chat with ${clicked.actorHandle}")
                    }
                }
            }
            NotificationActionType.FOLLOW -> {
                if (clicked.actorUid.isNotBlank() && onNavigateToProfile != null) {
                    onNavigateToProfile(clicked.actorUid)
                } else {
                    showToast("Viewed ${clicked.actorName}")
                }
            }
            NotificationActionType.LIKE,
            NotificationActionType.COMMENT,
            NotificationActionType.MENTION -> {
                if (!clicked.targetId.isNullOrBlank() || clicked.actorUid.isNotBlank()) {
                    if (onNavigateToPost != null) {
                        onNavigateToPost(clicked.targetId ?: "", clicked.actorUid.ifBlank { null })
                    } else {
                        showToast("Viewed post by ${clicked.actorName}")
                    }
                } else {
                    showToast("Viewed ${clicked.actorName}")
                }
            }
            NotificationActionType.PROJECT_EXPORT,
            NotificationActionType.PROJECT_COLLAB,
            NotificationActionType.REMIX -> {
                if (onNavigateToProject != null) {
                    onNavigateToProject(clicked.targetId)
                } else {
                    showToast("Opening project ${clicked.detailText ?: ""}")
                }
            }
            NotificationActionType.SYSTEM_DROP -> {
                showToast("System announcement: ${clicked.actionText}")
            }
        }
    }

    fun triggerManualRefresh() {
        if (isManualRefreshing) return
        isManualRefreshing = true
        isFeedLoading = true
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        if (currentUid == null) {
            notificationsList = defaultCommunityNotifications
            scope.launch {
                delay(300L)
                isFeedLoading = false
                isManualRefreshing = false
                showToast("Notifications refreshed")
            }
            return
        }

        scope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUid)
                    .collection("notifications")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(80)
                    .get()
                    .await()
                    .let { snap ->
                        val fetched = snap.documents.mapNotNull { doc ->
                            try {
                                ChromaNotification(
                                    id = doc.id,
                                    type = runCatching {
                                        NotificationActionType.valueOf(doc.getString("type") ?: "LIKE")
                                    }.getOrDefault(NotificationActionType.LIKE),
                                    actorUid = doc.getString("actorUid") ?: "",
                                    actorName = doc.getString("actorName") ?: "Unknown",
                                    actorHandle = doc.getString("actorHandle") ?: "",
                                    actorAvatarUrl = doc.getString("actorAvatarUrl"),
                                    actorGender = doc.getString("actorGender"),
                                    isVerified = doc.getBoolean("isVerified") ?: false,
                                    actionText = doc.getString("actionText") ?: "",
                                    detailText = doc.getString("detailText"),
                                    targetPreviewUrl = doc.getString("targetPreviewUrl"),
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                    isRead = doc.getBoolean("isRead") ?: false,
                                    targetId = doc.getString("targetId")
                                )
                            } catch (_: Exception) { null }
                        }
                        if (fetched.isNotEmpty()) notificationsList = fetched
                    }
            } catch (_: Exception) {
            } finally {
                delay(200L)
                isFeedLoading = false
                isManualRefreshing = false
                showToast("Notifications refreshed")
            }
        }
    }

    val unreadCount = notificationsList.count { !it.isRead }

    val refreshRotation = remember { Animatable(0f) }
    LaunchedEffect(isManualRefreshing) {
        if (isManualRefreshing) {
            refreshRotation.animateTo(
                targetValue = refreshRotation.value + 360f,
                animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing))
            )
        } else {
            refreshRotation.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header with design system ChromaScreenHeader
            ChromaScreenHeader(
                title = "Activity",
                badgeText = if (unreadCount > 0) "$unreadCount" else null,
                showBackButton = true,
                circularBackButton = true,
                onBack = onBack,
                isSearchExpanded = searchOpen,
                searchContent = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(colors.glassBg)
                            .border(1.dp, colors.glassBorder, CircleShape)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Search,
                            contentDescription = "Search",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                color = colors.textMain,
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search notifications...",
                                        fontSize = 13.sp,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Clear",
                                tint = colors.textMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable { searchQuery = "" }
                            )
                        }
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = "Done",
                            tint = colors.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { searchOpen = false }
                        )
                    }
                },
                actions = {
                    // Search toggle
                    GlassIconButton(
                        onClick = { searchOpen = !searchOpen },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Search,
                            contentDescription = "Search",
                            tint = if (searchOpen) colors.primary else colors.textMain,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Mark all read
                    GlassIconButton(
                        onClick = { markAllAsRead() },
                        modifier = Modifier.size(34.dp),
                        enabled = unreadCount > 0
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Lucide.CheckCheck,
                                contentDescription = "Mark all read",
                                tint = if (unreadCount > 0) colors.primary else colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .offset(x = 2.dp, y = (-2).dp)
                                        .clip(CircleShape)
                                        .background(colors.primary)
                                )
                            }
                        }
                    }

                    // Refresh
                    GlassIconButton(
                        onClick = { triggerManualRefresh() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.RefreshCw,
                            contentDescription = "Refresh",
                            tint = colors.textMain,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(refreshRotation.value)
                        )
                    }

                    // Settings
                    if (onNavigateToSettings != null) {
                        GlassIconButton(
                            onClick = { onNavigateToSettings() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.SlidersHorizontal,
                                contentDescription = "Settings",
                                tint = colors.textMain,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            )

            // Category Filter Icon Pills (ONLY LUCIDE ICONS INSTEAD OF TEXT)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = listOf(
                    NotificationFilter.ALL to Lucide.LayoutGrid,
                    NotificationFilter.LIKES to Lucide.Heart,
                    NotificationFilter.COMMENTS to Lucide.MessageCircle,
                    NotificationFilter.FOLLOWS to Lucide.UserPlus,
                    NotificationFilter.MESSAGES to Lucide.Send,
                    NotificationFilter.PROJECTS to Lucide.Layers
                )

                filters.forEach { (filter, icon) ->
                    val isSelected = selectedFilter == filter
                    val unreadForFilter = notificationsList.count {
                        (filter == NotificationFilter.ALL || it.effectiveCategory == filter) && it.isUnread
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) colors.primary else colors.glassBg
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) colors.primary else colors.glassBorder,
                                shape = CircleShape
                            )
                            .clickable {
                                if (selectedFilter != filter) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedFilter = filter
                                    openSwipeCardId = null
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = filter.label,
                            tint = if (isSelected) Color.White else colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )

                        if (unreadForFilter > 0 && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                            )
                        }
                    }
                }

                // Unread filter toggle as an icon button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (unreadOnly) colors.primary else colors.glassBg)
                        .border(
                            width = if (unreadOnly) 1.5.dp else 1.dp,
                            color = if (unreadOnly) colors.primary else colors.glassBorder,
                            shape = CircleShape
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            unreadOnly = !unreadOnly
                            openSwipeCardId = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Mail,
                        contentDescription = "Unread only",
                        tint = if (unreadOnly) Color.White else colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            GlassHrLine()

            // Feed Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isFeedLoading -> {
                        FeedSkeletonList(rowCount = 5)
                    }
                    filteredNotifications.isEmpty() -> {
                        NotificationEmptyState(
                            filterLabel = selectedFilter.label,
                            onRefresh = { triggerManualRefresh() }
                        )
                    }
                    else -> {
                        val newItems = filteredNotifications.filter { it.isUnread }
                        val earlierItems = filteredNotifications.filter { !it.isUnread }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 72.dp)
                        ) {
                            if (newItems.isNotEmpty()) {
                                item(key = "header_new") {
                                    SectionHeaderRow(
                                        title = "NEW",
                                        count = newItems.size,
                                        isAccent = true
                                    )
                                    GlassHrLine()
                                }

                                items(newItems, key = { it.id }) { item ->
                                    SwipeableNotificationCard(
                                        notification = item,
                                        isOpenLeftTray = openSwipeCardId == item.id,
                                        onOpenSwipeChanged = { openSwipeCardId = it },
                                        onCardClick = { handleCardClick(it) },
                                        onAvatarClick = { clicked ->
                                            if (clicked.actorUid.isNotBlank() && onNavigateToProfile != null) {
                                                onNavigateToProfile(clicked.actorUid)
                                            }
                                        },
                                        onSwipeRightRemove = { removeNotification(it) },
                                        onPrimaryAction = { handlePrimaryAction(it) },
                                        onToggleRead = { toggleRead(it) }
                                    )
                                    GlassHrLine()
                                }
                            }

                            if (earlierItems.isNotEmpty()) {
                                item(key = "header_earlier") {
                                    SectionHeaderRow(
                                        title = "EARLIER",
                                        count = earlierItems.size,
                                        isAccent = false
                                    )
                                    GlassHrLine()
                                }

                                items(earlierItems, key = { it.id }) { item ->
                                    SwipeableNotificationCard(
                                        notification = item,
                                        isOpenLeftTray = openSwipeCardId == item.id,
                                        onOpenSwipeChanged = { openSwipeCardId = it },
                                        onCardClick = { handleCardClick(it) },
                                        onAvatarClick = { clicked ->
                                            if (clicked.actorUid.isNotBlank() && onNavigateToProfile != null) {
                                                onNavigateToProfile(clicked.actorUid)
                                            }
                                        },
                                        onSwipeRightRemove = { removeNotification(it) },
                                        onPrimaryAction = { handlePrimaryAction(it) },
                                        onToggleRead = { toggleRead(it) }
                                    )
                                    GlassHrLine()
                                }
                            }
                        }
                    }
                }
            }
        }

        // Toast Banner with Undo
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(tween(160)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(160)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.textMain.copy(alpha = 0.92f))
                    .border(1.dp, colors.glassBorder, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = toastMessage ?: "",
                    color = colors.bg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (toastShowUndo && lastRemovedBackup != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.primary)
                            .clickable { undoRemove() }
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Undo",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// RELATIVE TIME FORMATTER
// ============================================================================

private fun formatRelativeTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1  -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24   -> "${hours}h"
        days < 7     -> "${days}d"
        else         -> "${days / 7}w"
    }
}

// ============================================================================
// DEFAULT SEED NOTIFICATIONS (Firestore fallback)
// ============================================================================

private val _now = System.currentTimeMillis()

private val defaultCommunityNotifications = listOf(
    ChromaNotification(
        id = "notif_dm_sora",
        type = NotificationActionType.MESSAGE,
        actorUid = "creator_sora",
        actorName = "Sora Takahashi",
        actorHandle = "@sora_motion",
        actorAvatarUrl = null,
        actorGender = "female",
        isVerified = true,
        actionText = "Sent messages in Studio Chat",
        detailText = "Checked the stems and remix layers for the live set",
        timestamp = _now - (2 * 60 * 1000),
        isRead = false
    ),
    ChromaNotification(
        id = "notif_dm_marcus",
        type = NotificationActionType.MESSAGE,
        actorUid = "creator_marcus",
        actorName = "Marcus Vance",
        actorHandle = "@mvance_vfx",
        actorAvatarUrl = null,
        actorGender = "male",
        isVerified = true,
        actionText = "Shared render preview",
        detailText = "Render pass preview 4K_Pass_Final.png",
        timestamp = _now - (9 * 60 * 1000),
        isRead = false
    ),
    ChromaNotification(
        id = "notif_like_elena",
        type = NotificationActionType.LIKE,
        actorUid = "creator_elena",
        actorName = "Elena Rostova",
        actorHandle = "@elena_arch",
        actorAvatarUrl = null,
        actorGender = "female",
        isVerified = true,
        actionText = "Liked your post",
        detailText = "Neo-Brutalist Monolith Study #04",
        timestamp = _now - (18 * 60 * 1000),
        isRead = false
    ),
    ChromaNotification(
        id = "notif_remix_devon",
        type = NotificationActionType.REMIX,
        actorUid = "creator_devon",
        actorName = "Devon Brooks",
        actorHandle = "@devon_3d",
        actorAvatarUrl = null,
        actorGender = "male",
        isVerified = true,
        actionText = "Forked your remix",
        detailText = "Kinetic Typography Reel derivative composition",
        timestamp = _now - (32 * 60 * 1000),
        isRead = false
    ),
    ChromaNotification(
        id = "notif_comment_aria",
        type = NotificationActionType.COMMENT,
        actorUid = "creator_aria",
        actorName = "Aria Chen",
        actorHandle = "@ariachen_ui",
        actorAvatarUrl = null,
        actorGender = "female",
        isVerified = true,
        actionText = "Commented on video",
        detailText = "The camera transition at 0:08 is pure perfection!",
        timestamp = _now - (60 * 60 * 1000),
        isRead = false
    ),
    ChromaNotification(
        id = "notif_follow_kaito",
        type = NotificationActionType.FOLLOW,
        actorUid = "creator_kaito",
        actorName = "Kaito Nakamura",
        actorHandle = "@kaito_type",
        actorAvatarUrl = null,
        actorGender = "male",
        isVerified = true,
        actionText = "Started following you",
        detailText = "You both follow @sora_motion",
        timestamp = _now - (3 * 3600 * 1000),
        isRead = true
    ),
    ChromaNotification(
        id = "notif_mention_joy",
        type = NotificationActionType.MENTION,
        actorUid = "thejoykumbhakar",
        actorName = "Joy Kumbhakar",
        actorHandle = "@joykumbhakar101",
        actorAvatarUrl = null,
        actorGender = "male",
        isVerified = true,
        actionText = "Mentioned you in showcase",
        detailText = "Check out the latest shader pipeline in ChromaStudio.",
        timestamp = _now - (24 * 3600 * 1000),
        isRead = true
    ),
    ChromaNotification(
        id = "notif_system_drop",
        type = NotificationActionType.SYSTEM_DROP,
        actorUid = "chroma_studio",
        actorName = "Chroma Studio",
        actorHandle = "@chromastudio",
        actorAvatarUrl = null,
        isVerified = true,
        actionText = "Released 12 new LUT Presets",
        detailText = "Available now in Video Editor and Image Studio",
        timestamp = _now - (36 * 3600 * 1000),
        isRead = true
    )
)
