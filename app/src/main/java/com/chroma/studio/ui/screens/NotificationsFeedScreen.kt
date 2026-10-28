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
import androidx.compose.ui.draw.shadow
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
import com.chroma.studio.data.NotificationDispatcher
import com.chroma.studio.data.mutation.OptimisticFollowManager
import com.chroma.studio.model.ChromaNotification
import com.chroma.studio.model.NotificationActionType
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.DefaultGenderAvatar
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.IosAlertDialog
import com.chroma.studio.ui.components.AppleNotificationGraphic
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
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
// FILTER CATEGORIES
// ============================================================================

enum class NotificationFilter(val label: String, val icon: ImageVector) {
    ALL("All", Lucide.LayoutGrid),
    LIKES("Likes", Lucide.Heart),
    COMMENTS("Comments", Lucide.MessageCircle),
    FOLLOWS("Follows", Lucide.UserPlus),
    STORIES("Stories", Lucide.Sparkles),
    MENTIONS("Mentions", Lucide.AtSign),
    MESSAGES("Messages", Lucide.Send),
    PROJECTS("Projects", Lucide.Layers)
}

val ChromaNotification.effectiveCategory: NotificationFilter
    get() = when (type) {
        NotificationActionType.LIKE,
        NotificationActionType.LIKE_COMMENT,
        NotificationActionType.STORY_LIKE -> NotificationFilter.LIKES
        NotificationActionType.COMMENT,
        NotificationActionType.REPLY -> NotificationFilter.COMMENTS
        NotificationActionType.FOLLOW,
        NotificationActionType.FOLLOW_REQUEST -> NotificationFilter.FOLLOWS
        NotificationActionType.STORY_REPLY,
        NotificationActionType.STORY_REACTION -> NotificationFilter.STORIES
        NotificationActionType.MENTION -> NotificationFilter.MENTIONS
        NotificationActionType.MESSAGE -> NotificationFilter.MESSAGES
        NotificationActionType.REMIX,
        NotificationActionType.PROJECT_EXPORT,
        NotificationActionType.PROJECT_COLLAB -> NotificationFilter.PROJECTS
        NotificationActionType.SYSTEM_DROP -> NotificationFilter.ALL
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
        NotificationActionType.LIKE,
        NotificationActionType.LIKE_COMMENT,
        NotificationActionType.STORY_LIKE  -> Triple(Color(0xFFFFE4E6), Color(0xFFE11D48), Lucide.Heart)
        NotificationActionType.COMMENT,
        NotificationActionType.REPLY       -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Lucide.MessageCircle)
        NotificationActionType.FOLLOW      -> Triple(Color(0xFFD1FAE5), Color(0xFF059669), Lucide.UserPlus)
        NotificationActionType.FOLLOW_REQUEST -> Triple(Color(0xFFE0E7FF), Color(0xFF4F46E5), Lucide.UserCheck)
        NotificationActionType.STORY_REPLY -> Triple(Color(0xFFFCE7F3), Color(0xFFDB2777), Lucide.Sparkles)
        NotificationActionType.STORY_REACTION -> Triple(Color(0xFFFEF3C7), Color(0xFFF59E0B), Lucide.Flame)
        NotificationActionType.MESSAGE     -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), Lucide.Send)
        NotificationActionType.REMIX       -> Triple(Color(0xFFEDE9FE), Color(0xFF7C3AED), Lucide.Sparkles)
        NotificationActionType.MENTION     -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), Lucide.AtSign)
        NotificationActionType.SYSTEM_DROP -> Triple(Color(0xFFFEF3C7), Color(0xFFF59E0B), Lucide.Bell)
        NotificationActionType.PROJECT_EXPORT -> Triple(Color(0xFFECFDF5), Color(0xFF0D9488), Lucide.Video)
        NotificationActionType.PROJECT_COLLAB -> Triple(Color(0xFFEEF2FF), Color(0xFF4F46E5), Lucide.Users)
    }

    Box(
        modifier = Modifier
            .size(42.dp)
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
            size = 40.dp
        )

        Box(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-1).dp, y = (-1).dp)
                .clip(CircleShape)
                .background(badgeBgColor)
                .border(1.5.dp, colors.bg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = badgeIconColor,
                modifier = Modifier.size(9.dp)
            )
        }
    }
}

// ============================================================================
// SECTION HEADER ROW
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
            .background(colors.bg.copy(alpha = 0.88f))
            .padding(horizontal = 16.dp, vertical = 7.dp),
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
// SWIPEABLE NOTIFICATION ROW (Refined Apple/Instagram Card Architecture)
// ============================================================================

@Composable
private fun SwipeableNotificationCard(
    notification: ChromaNotification,
    isFollowingUser: Boolean,
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
    val snapLeftPx = with(density) { (-96).dp.toPx() }
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
                    .background(Color(0xFFFF3B30))
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

        // SWIPE LEFT tray — Contextual Actions (Sleek rounded rectangular glass buttons)
        if (offsetX.value < -4f || isOpenLeftTray) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .background(colors.glassBgHover)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Read toggle button
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.glassBg)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                        .clickable { onToggleRead(notification) }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (notification.isUnread) Lucide.CheckCheck else Lucide.Mail,
                        contentDescription = "Toggle Read",
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Delete action button
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF3B30).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { onSwipeRightRemove(notification) }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Trash2,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // FOREGROUND ROW
        Row(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(
                    if (notification.isUnread) colors.glassBgHover.copy(alpha = 0.45f)
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
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Avatar + Details
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NotifAvatarWithBadge(
                    notification = notification,
                    onAvatarClick = { onAvatarClick(notification) }
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Line 1: User name, Verification, Unread dot, Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = notification.actorName,
                            fontSize = 13.5.sp,
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
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }

                    // Line 2: Action text + Snippet
                    val combinedText = remember(notification.actionText, notification.detailText) {
                        if (!notification.detailText.isNullOrBlank()) {
                            "${notification.actionText}: ${notification.detailText}"
                        } else {
                            notification.actionText
                        }
                    }

                    Text(
                        text = combinedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Right: Authentic Contextual Action Buttons (NO circle backgrounds)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (notification.type) {
                    NotificationActionType.FOLLOW -> {
                        // High-grade social follow button (Follow / Following)
                        if (!isFollowingUser) {
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.primary)
                                    .clickable { onPrimaryAction(notification) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Follow",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.glassBg)
                                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                    .clickable { onPrimaryAction(notification) }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Following",
                                    color = colors.textMain,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    NotificationActionType.FOLLOW_REQUEST -> {
                        // Instagram-style Confirm & Delete buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.primary)
                                    .clickable { onPrimaryAction(notification) }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Confirm",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.glassBg)
                                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                    .clickable { onSwipeRightRemove(notification) }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Delete",
                                    color = colors.textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    NotificationActionType.STORY_REACTION -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!notification.detailText.isNullOrBlank()) {
                                Text(
                                    text = notification.detailText,
                                    fontSize = 19.sp
                                )
                            }
                            if (!notification.targetPreviewUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = notification.targetPreviewUrl,
                                    contentDescription = "Story Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                        .clickable { onCardClick(notification) }
                                )
                            }
                        }
                    }
                    NotificationActionType.STORY_LIKE,
                    NotificationActionType.STORY_REPLY -> {
                        if (!notification.targetPreviewUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = notification.targetPreviewUrl,
                                contentDescription = "Story Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                    .clickable { onCardClick(notification) }
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.Sparkles,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    NotificationActionType.MESSAGE -> {
                        // Clean Reply button
                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.glassBg)
                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                .clickable { onPrimaryAction(notification) }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Send,
                                    contentDescription = "Reply",
                                    tint = colors.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Reply",
                                    color = colors.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    NotificationActionType.COMMENT,
                    NotificationActionType.REPLY,
                    NotificationActionType.LIKE_COMMENT -> {
                        if (!notification.targetPreviewUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = notification.targetPreviewUrl,
                                contentDescription = "Post Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                    .clickable { onCardClick(notification) }
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.Heart,
                                contentDescription = "Like Comment",
                                tint = if (notification.likedComment) colors.primary else colors.textMuted,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onPrimaryAction(notification) }
                                    .padding(2.dp)
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                    .clickable { onCardClick(notification) }
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
private fun FeedSkeletonList(rowCount: Int = 6) {
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmer)
                )
            }
            GlassHrLine()
        }
    }
}

// ============================================================================
// CLEAN EMPTY STATE (Exact Apple HIG 3D Notification Empty State Studio Design)
// Matches e:\Design Assets\apple_hig_3d_notification_empty_state_studio.html
// ============================================================================

@Composable
private fun NotificationEmptyState(
    filterLabel: String,
    onRefresh: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val graphicState = rememberAppleGraphicAnimationState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Clean 3D Cupertino Bell without check badge or floor shadow glow
        AppleNotificationGraphic(
            modifier = Modifier.size(175.dp),
            state = graphicState,
            enableClickToAnimate = true,
            showSonicWaves = true,
            showCheckBadge = false,
            showFloorGlow = false,
            onClick = null
        )

        Spacer(Modifier.height(16.dp))

        // 3. Apple Typography Headline
        Text(
            text = if (filterLabel == "All") "No Notifications Yet" else "No $filterLabel Yet",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMain,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        // 4. Apple Subtitle Copy
        Text(
            text = if (filterLabel == "All") {
                "You’re all caught up. When people like, comment, mention, or message you, your notifications will appear right here."
            } else {
                "No notifications matching the \"$filterLabel\" category at the moment."
            },
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth(0.86f)
        )

        Spacer(Modifier.height(20.dp))

        // 5. Clean Action Button with Rounded Corners (RoundedCornerShape(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1A8CFF),
                            Color(0xFF0062CC)
                        )
                    )
                )
                .clickable {
                    coroutineScope.launch {
                        graphicState.triggerBellRing()
                    }
                    onRefresh()
                }
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Lucide.RefreshCw,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Refresh Activity",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
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
    var notificationsList by remember { mutableStateOf<List<ChromaNotification>>(emptyList()) }

    // Real-time follow states from single source of truth
    val followingUserIds by OptimisticFollowManager.followingUserIds.collectAsState()

    // UI States
    var selectedFilter by remember { mutableStateOf(NotificationFilter.ALL) }
    var unreadOnly by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isFeedLoading by remember { mutableStateOf(true) }
    var isManualRefreshing by remember { mutableStateOf(false) }
    var openSwipeCardId by remember { mutableStateOf<String?>(null) }
    var lastRemovedBackup by remember { mutableStateOf<Pair<ChromaNotification, Int>?>(null) }
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Toast Banner
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
            notificationsList = emptyList()
            isFeedLoading = false
            return@DisposableEffect onDispose {}
        }

        val registration = FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                isFeedLoading = false
                isManualRefreshing = false

                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val fetched = snapshot.documents.mapNotNull { doc ->
                        try {
                            ChromaNotification(
                                id = doc.id,
                                type = runCatching {
                                    NotificationActionType.valueOf(doc.getString("type") ?: "LIKE")
                                }.getOrDefault(NotificationActionType.LIKE),
                                actorUid = doc.getString("actorUid") ?: "",
                                actorName = doc.getString("actorName") ?: "Chroma Creator",
                                actorHandle = doc.getString("actorHandle") ?: "@user",
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
                    notificationsList = fetched
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

    val unreadCount = remember(notificationsList) { notificationsList.count { it.isUnread } }

    // Mutation Operations
    fun markAsRead(notifId: String) {
        val target = notificationsList.find { it.id == notifId }
        if (target == null || target.isRead) return

        notificationsList = notificationsList.map { if (it.id == notifId) it.copy(isRead = true) else it }
        if (currentUid != null) {
            scope.launch {
                NotificationDispatcher.updateNotificationRead(currentUid, notifId, true)
            }
        }
    }

    fun markAllAsRead() {
        val unread = notificationsList.filter { it.isUnread }
        if (unread.isEmpty()) return

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        SoundManager.playReaction()
        notificationsList = notificationsList.map { it.copy(isRead = true) }
        showToast("All marked as read")

        if (currentUid != null) {
            scope.launch {
                NotificationDispatcher.markAllAsRead(currentUid)
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
            scope.launch {
                NotificationDispatcher.updateNotificationRead(currentUid, notification.id, nextRead)
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
                scope.launch {
                    NotificationDispatcher.deleteNotification(currentUid, removed.id)
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

    fun clearAllNotifications() {
        if (notificationsList.isEmpty()) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        notificationsList = emptyList()
        openSwipeCardId = null
        showToast("All notifications cleared")

        if (currentUid != null) {
            scope.launch {
                NotificationDispatcher.clearAllNotifications(currentUid)
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
                if (currentUid != null && item.actorUid.isNotBlank()) {
                    val currentlyFollowing = followingUserIds.contains(item.actorUid) || item.following
                    val next = !currentlyFollowing
                    SoundManager.playTap(context)
                    showToast(if (next) "Following ${item.actorHandle}" else "Unfollowed ${item.actorHandle}")

                    OptimisticFollowManager.toggleFollow(
                        currentUserId = currentUid,
                        targetUserId = item.actorUid,
                        baseFollowing = currentlyFollowing,
                        onError = {
                            showToast("Could not update follow status.")
                        }
                    )
                }
            }
            NotificationActionType.FOLLOW_REQUEST -> {
                if (currentUid != null && item.actorUid.isNotBlank()) {
                    SoundManager.playTap(context)
                    showToast("Approved follow request from ${item.actorHandle}")
                    removeNotification(item)
                    scope.launch {
                        com.chroma.studio.data.DiscoverRepository.getInstance().setFollow(
                            currentUserId = item.actorUid,
                            targetUserId = currentUid,
                            targetFollow = true
                        )
                    }
                }
            }
            NotificationActionType.COMMENT,
            NotificationActionType.LIKE_COMMENT -> {
                val next = !item.likedComment
                notificationsList = notificationsList.map { if (it.id == item.id) it.copy(likedComment = next, isRead = true) else it }
                SoundManager.playPop()
                showToast(if (next) "Liked comment" else "Removed like")
            }
            else -> {
                val next = !item.likedBack
                notificationsList = notificationsList.map { if (it.id == item.id) it.copy(likedBack = next, isRead = true) else it }
                SoundManager.playPop()
                showToast(if (next) "Sent reaction to ${item.actorHandle}" else "Removed reaction")
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
            NotificationActionType.FOLLOW,
            NotificationActionType.FOLLOW_REQUEST -> {
                if (clicked.actorUid.isNotBlank() && onNavigateToProfile != null) {
                    onNavigateToProfile(clicked.actorUid)
                } else {
                    showToast("Viewed ${clicked.actorName}")
                }
            }
            NotificationActionType.STORY_LIKE,
            NotificationActionType.STORY_REPLY,
            NotificationActionType.STORY_REACTION -> {
                if (clicked.actorUid.isNotBlank() && onNavigateToProfile != null) {
                    onNavigateToProfile(clicked.actorUid)
                } else {
                    showToast("Story interaction from ${clicked.actorName}")
                }
            }
            NotificationActionType.LIKE,
            NotificationActionType.COMMENT,
            NotificationActionType.REPLY,
            NotificationActionType.LIKE_COMMENT,
            NotificationActionType.MENTION -> {
                if (!clicked.targetId.isNullOrBlank() || clicked.actorUid.isNotBlank()) {
                    if (onNavigateToPost != null) {
                        onNavigateToPost(clicked.targetId ?: "", clicked.actorUid.ifBlank { null })
                    } else {
                        showToast("Opening post...")
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
                } else if (!clicked.targetId.isNullOrBlank() && onNavigateToPost != null) {
                    onNavigateToPost(clicked.targetId, clicked.actorUid.ifBlank { null })
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
                    .limit(100)
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
                                    actorName = doc.getString("actorName") ?: "Chroma Creator",
                                    actorHandle = doc.getString("actorHandle") ?: "@user",
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
                        notificationsList = fetched
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
            // Header with design system ChromaScreenHeader and Overflow Menu
            ChromaScreenHeader(
                title = "Activity",
                badgeText = if (unreadCount > 0) "$unreadCount" else null,
                showBackButton = true,
                circularBackButton = false,
                onBack = onBack,
                isSearchExpanded = searchOpen,
                searchContent = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassBg)
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
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
                                fontSize = 13.5.sp,
                                color = colors.textMain,
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search notifications...",
                                        fontSize = 13.5.sp,
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
                                    .clickable { searchQuery = "" }
                            )
                        }
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = "Done",
                            tint = colors.primary,
                            modifier = Modifier
                                .size(18.dp)
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

                    // Native Dropdown Menu ("use menus")
                    Box {
                        GlassIconButton(
                            onClick = { isMoreMenuOpen = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "More Options",
                                tint = colors.textMain,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isMoreMenuOpen,
                            onDismissRequest = { isMoreMenuOpen = false },
                            modifier = Modifier
                                .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.25f))
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.glassBg, RoundedCornerShape(20.dp))
                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (unreadCount > 0) "Mark all as read ($unreadCount)" else "Mark all as read",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (unreadCount > 0) colors.textMain else colors.textMuted
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Lucide.CheckCheck,
                                        contentDescription = null,
                                        tint = if (unreadCount > 0) colors.primary else colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                enabled = unreadCount > 0,
                                onClick = {
                                    isMoreMenuOpen = false
                                    markAllAsRead()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (unreadOnly) "Show all notifications" else "Only unread notifications",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textMain
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (unreadOnly) Lucide.LayoutGrid else Lucide.Filter,
                                        contentDescription = null,
                                        tint = colors.textMain,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    unreadOnly = !unreadOnly
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Refresh Activity",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textMain
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Lucide.RefreshCw,
                                        contentDescription = null,
                                        tint = colors.textMain,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    isMoreMenuOpen = false
                                    triggerManualRefresh()
                                }
                            )

                            if (onNavigateToSettings != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Notification Settings",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textMain
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Lucide.SlidersHorizontal,
                                            contentDescription = null,
                                            tint = colors.textMain,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        isMoreMenuOpen = false
                                        onNavigateToSettings()
                                    }
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                thickness = 0.5.dp,
                                color = colors.glassBorder
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Clear all notifications",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF3B30)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Lucide.Trash2,
                                        contentDescription = null,
                                        tint = Color(0xFFFF3B30),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                enabled = notificationsList.isNotEmpty(),
                                onClick = {
                                    isMoreMenuOpen = false
                                    showClearConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            )

            // Segmented Category Filter Tabs (Proper text labels + clean rounded rectangles)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = listOf(
                    NotificationFilter.ALL to "All",
                    NotificationFilter.LIKES to "Likes",
                    NotificationFilter.COMMENTS to "Comments",
                    NotificationFilter.FOLLOWS to "Follows",
                    NotificationFilter.MESSAGES to "Messages",
                    NotificationFilter.PROJECTS to "Projects"
                )

                filters.forEach { (filter, label) ->
                    val isSelected = selectedFilter == filter
                    val unreadCountForTab = notificationsList.count {
                        (filter == NotificationFilter.ALL || it.effectiveCategory == filter) && it.isUnread
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) colors.primary.copy(alpha = 0.15f)
                                else colors.glassBg
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) colors.primary.copy(alpha = 0.6f) else colors.glassBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                if (selectedFilter != filter) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedFilter = filter
                                    openSwipeCardId = null
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) colors.primary else colors.textMuted
                            )
                            if (unreadCountForTab > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) colors.primary else colors.primary.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (unreadCountForTab > 99) "99+" else "$unreadCountForTab",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else colors.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    isFeedLoading && notificationsList.isEmpty() -> {
                        FeedSkeletonList(rowCount = 6)
                    }
                    filteredNotifications.isEmpty() -> {
                        NotificationEmptyState(
                            filterLabel = selectedFilter.label,
                            onRefresh = { triggerManualRefresh() }
                        )
                    }
                    else -> {
                        val followRequests = filteredNotifications.filter { it.type == NotificationActionType.FOLLOW_REQUEST }
                        val standardNotifications = filteredNotifications.filter { it.type != NotificationActionType.FOLLOW_REQUEST }

                        val instagramTimeSections = listOf("Today", "Yesterday", "This week", "This month", "Earlier")
                        val groupedByTime = standardNotifications.groupBy { formatInstagramSection(it.timestamp) }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 72.dp)
                        ) {
                            if (followRequests.isNotEmpty()) {
                                item(key = "header_follow_requests") {
                                    SectionHeaderRow(
                                        title = "FOLLOW REQUESTS",
                                        count = followRequests.size,
                                        isAccent = true
                                    )
                                    GlassHrLine()
                                }

                                items(followRequests, key = { it.id }) { item ->
                                    val isFollowing = followingUserIds.contains(item.actorUid) || item.following
                                    SwipeableNotificationCard(
                                        notification = item,
                                        isFollowingUser = isFollowing,
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

                            instagramTimeSections.forEach { sectionTitle ->
                                val sectionItems = groupedByTime[sectionTitle] ?: emptyList()
                                if (sectionItems.isNotEmpty()) {
                                    item(key = "header_${sectionTitle.lowercase().replace(" ", "_")}") {
                                        SectionHeaderRow(
                                            title = sectionTitle.uppercase(),
                                            count = sectionItems.size,
                                            isAccent = sectionTitle == "Today"
                                        )
                                        GlassHrLine()
                                    }

                                    items(sectionItems, key = { it.id }) { item ->
                                        val isFollowing = followingUserIds.contains(item.actorUid) || item.following
                                        SwipeableNotificationCard(
                                            notification = item,
                                            isFollowingUser = isFollowing,
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
        }

        // Confirmation Dialog for Clear All
        if (showClearConfirmDialog) {
            IosAlertDialog(
                title = "Clear All Notifications?",
                message = "This will permanently remove all activity notifications from your feed.",
                confirmText = "Clear All",
                isDestructive = true,
                onConfirm = {
                    showClearConfirmDialog = false
                    clearAllNotifications()
                },
                dismissText = "Cancel",
                onDismiss = { showClearConfirmDialog = false }
            )
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.textMain.copy(alpha = 0.92f))
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 9.dp),
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
                            .clip(RoundedCornerShape(6.dp))
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

private fun formatInstagramSection(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - timestamp).coerceAtLeast(0L)
    val oneDayMs = 24 * 60 * 60 * 1000L

    val calNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
    val calItem = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }

    val isSameYear = calNow.get(java.util.Calendar.YEAR) == calItem.get(java.util.Calendar.YEAR)
    val dayDiff = calNow.get(java.util.Calendar.DAY_OF_YEAR) - calItem.get(java.util.Calendar.DAY_OF_YEAR)

    return when {
        isSameYear && dayDiff == 0 -> "Today"
        isSameYear && dayDiff == 1 -> "Yesterday"
        diff < 7 * oneDayMs -> "This week"
        diff < 30 * oneDayMs -> "This month"
        else -> "Earlier"
    }
}

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
