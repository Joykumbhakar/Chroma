package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.data.OnlineStatusManager
import com.chroma.studio.model.FlashComment
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.CommentSortOrder
import com.chroma.studio.viewmodel.FlashCommentViewModel
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Flagship YouTube-Grade Comments Bottom Sheet.
 * Features:
 * - Realtime updates and instant optimistic comment posting
 * - Threaded tree connector lines (Root and Branch lines)
 * - Skip partially expanded bottom sheet with design system corner radius
 * - Unified online status dot on avatars via OnlineStatusManager
 * - Sleek borderless compact pills and capsules for badges, sort, and actions
 * - Pinned comments with indicator pill
 * - Video/Post Creator author badge pill
 * - Expandable "Read more" / "Show less" on long comments
 * - Interactive bouncy Thumbs up (Like), Thumbs down (Dislike), Creator Heart
 * - Smart reply mode automatically linking to thread root with @mention prefix
 * - Quick emoji selection bar and edge-to-edge keyboard padding
 * - 3-dots overflow menu (Pin, Delete, Copy text)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeCommentsSheet(
    targetId: String,
    targetCommentCount: Int,
    creatorId: String,
    creatorUsername: String = "",
    creatorAvatarUrl: String = "",
    turnOffComments: Boolean = false,
    commentVm: FlashCommentViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onDismiss: () -> Unit,
    onCommentCountChanged: (Int) -> Unit = {},
    onReplyWithFlash: ((String?) -> Unit)? = null,
    onRequireLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    // Design system tokens
    val sheetBg = if (isDark) Color(0xFF111216) else Color.White
    val textPrimary = colors.textMain
    val textSecondary = colors.textMuted
    val dividerColor = colors.cardBorderSubtle
    val surfacePillBg = if (isDark) Color(0xFF1E212A) else Color(0xFFF1F5F9)
    val ytBlue = colors.primary
    val dragHandleColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFD1D5DB)
    val treeLineColor = if (isDark) Color(0xFF2C303B) else Color(0xFFE2E8F0)

    val comments by commentVm.comments.collectAsState()
    val sortOrder by commentVm.sortOrder.collectAsState()
    val isLoading by commentVm.isLoading.collectAsState()

    var commentText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<FlashComment?>(null) }
    val expandedThreads = remember { mutableStateMapOf<String, Boolean>() }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var commentPendingDelete by remember { mutableStateOf<FlashComment?>(null) }
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(targetId) {
        commentVm.loadComments(targetId)
    }

    // Split top-level and replies
    val repliesMap = remember(comments) {
        comments.filter { !it.replyToCommentId.isNullOrBlank() }
            .groupBy { it.replyToCommentId!! }
    }

    // Sort top-level comments: ALWAYS prioritize pinned first, then pending comments (optimistic UI), then likes/timestamp
    val topLevelComments = remember(comments, sortOrder) {
        val parents = comments.filter { it.replyToCommentId.isNullOrBlank() }
        when (sortOrder) {
            CommentSortOrder.TOP_COMMENTS -> {
                parents.sortedWith(
                    compareByDescending<FlashComment> { it.isPinned }
                        .thenByDescending { it.isPending }
                        .thenByDescending { it.likesCount }
                        .thenByDescending { it.timestamp }
                )
            }
            CommentSortOrder.NEWEST_FIRST -> {
                parents.sortedWith(
                    compareByDescending<FlashComment> { it.isPinned }
                        .thenByDescending { it.isPending }
                        .thenByDescending { it.timestamp }
                )
            }
        }
    }

    val totalCommentCount = maxOf(targetCommentCount, comments.size)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                .fillMaxHeight(0.88f)
                .background(sheetBg)
        ) {
            // Header Bar: "Comments", Count capsule, Sort capsule button, Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comments",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = textPrimary
                )
                Spacer(Modifier.width(8.dp))

                // Compact Count Badge Capsule
                Surface(
                    shape = CircleShape,
                    color = ytBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = formatCount(totalCommentCount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ytBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                // Sort Capsule Button (Borderless compact rounded pill)
                if (!turnOffComments) {
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = surfacePillBg,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { sortMenuExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Lucide.SlidersHorizontal,
                                    contentDescription = "Sort comments",
                                    tint = textPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (sortOrder == CommentSortOrder.TOP_COMMENTS) "Top" else "Newest",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary
                                )
                                Spacer(Modifier.width(3.dp))
                                Icon(
                                    imageVector = Lucide.ChevronDown,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF1E212A) else Color.White)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (sortOrder == CommentSortOrder.TOP_COMMENTS) {
                                            Icon(Lucide.Check, null, tint = ytBlue, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(8.dp))
                                        } else {
                                            Spacer(Modifier.width(23.dp))
                                        }
                                        Text(
                                            "Top comments",
                                            fontWeight = if (sortOrder == CommentSortOrder.TOP_COMMENTS) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.5.sp,
                                            color = textPrimary
                                        )
                                    }
                                },
                                onClick = {
                                    commentVm.setSortOrder(CommentSortOrder.TOP_COMMENTS)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (sortOrder == CommentSortOrder.NEWEST_FIRST) {
                                            Icon(Lucide.Check, null, tint = ytBlue, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(8.dp))
                                        } else {
                                            Spacer(Modifier.width(23.dp))
                                        }
                                        Text(
                                            "Newest first",
                                            fontWeight = if (sortOrder == CommentSortOrder.NEWEST_FIRST) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.5.sp,
                                            color = textPrimary
                                        )
                                    }
                                },
                                onClick = {
                                    commentVm.setSortOrder(CommentSortOrder.NEWEST_FIRST)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Close Button (Compact circular pill)
                Surface(
                    shape = CircleShape,
                    color = surfacePillBg,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() }
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

            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            // Comment List / States
            if (turnOffComments) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Lucide.MessageSquareOff, null, tint = textSecondary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Comments are turned off",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "The creator has turned off comments for this post.",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                }
            } else if (isLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    OscellionLoadingSpinner(color = ytBlue)
                }
            } else if (topLevelComments.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Lucide.MessageCircle, null, tint = textSecondary.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No comments yet",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Say something to start the conversation.",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    items(topLevelComments, key = { it.id }) { comment ->
                        val childReplies = repliesMap[comment.id] ?: emptyList()
                        val isThreadExpanded = expandedThreads[comment.id] ?: false

                        YouTubeCommentCard(
                            comment = comment,
                            targetId = targetId,
                            creatorId = creatorId,
                            creatorUsername = creatorUsername,
                            creatorAvatarUrl = creatorAvatarUrl,
                            currentUserId = currentUserId,
                            replies = childReplies,
                            isRepliesExpanded = isThreadExpanded,
                            treeLineColor = treeLineColor,
                            onToggleReplies = {
                                expandedThreads[comment.id] = !isThreadExpanded
                            },
                            onReply = { target ->
                                replyingToComment = target
                                focusRequester.requestFocus()
                            },
                            onReplyWithFlash = if (onReplyWithFlash != null) { { targetId -> onReplyWithFlash(targetId) } } else null,
                            onToggleLike = { commentVm.toggleLike(it) },
                            onToggleDislike = { commentVm.toggleDislike(it) },
                            onToggleHeart = { commentVm.toggleCreatorHeart(it) },
                            onTogglePin = { commentVm.togglePin(it, targetId) },
                            onDelete = { commentPendingDelete = it },
                            onRetry = { commentVm.retryComment(it) }
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // Bottom Comment Dock (only if comments enabled)
            if (!turnOffComments) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(sheetBg)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    // Replying Context Banner
                    if (replyingToComment != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(surfacePillBg)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Lucide.Reply,
                                contentDescription = null,
                                tint = ytBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Replying to @${replyingToComment?.username}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { replyingToComment = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = "Cancel",
                                    tint = textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Quick Reactions Emoji Bar
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickEmojis = listOf("❤️", "🔥", "👏", "😂", "😍", "🙌", "✨", "💯", "🎉", "🤯")
                        items(quickEmojis) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(surfacePillBg)
                                    .clickable { commentText += emoji }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 16.sp)
                            }
                        }
                    }

                    // Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val currentPresence = OnlineStatusManager.rememberUserPresence(currentUser?.uid)
                        IosAvatarView(
                            name = currentUser?.displayName ?: "User",
                            avatarUrl = currentUser?.photoUrl?.toString().orEmpty(),
                            size = 34.dp,
                            isOnline = currentPresence.online,
                            statusRingColor = sheetBg
                        )

                        Spacer(Modifier.width(10.dp))

                        TextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = {
                                Text(
                                    if (replyingToComment != null) "Add a reply..." else "Add a comment...",
                                    color = textSecondary,
                                    fontSize = 13.5.sp
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = surfacePillBg,
                                unfocusedContainerColor = surfacePillBg,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            maxLines = 4
                        )

                        AnimatedVisibility(
                            visible = commentText.isNotBlank(),
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Row {
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (FirebaseAuth.getInstance().currentUser == null || com.chroma.studio.data.SavedAccountManager.isGuest(context)) {
                                            onRequireLogin()
                                            return@IconButton
                                        }

                                        // Resolve root parent thread ID so child replies remain nested inside the root thread
                                        val rootParentId = if (!replyingToComment?.replyToCommentId.isNullOrBlank()) {
                                            replyingToComment?.replyToCommentId
                                        } else {
                                            replyingToComment?.id
                                        }
                                        val replyUsername = replyingToComment?.username

                                        commentVm.addComment(
                                            videoId = targetId,
                                            text = commentText.trim(),
                                            replyToId = rootParentId,
                                            replyToUsername = replyUsername
                                        )
                                        onCommentCountChanged(1)

                                        if (rootParentId != null) {
                                            expandedThreads[rootParentId] = true
                                        } else {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(0)
                                            }
                                        }

                                        commentText = ""
                                        replyingToComment = null
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(ytBlue)
                                ) {
                                    Icon(
                                        imageVector = Lucide.Send,
                                        contentDescription = "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (commentPendingDelete != null) {
        val comment = commentPendingDelete!!
        IosAlertDialog(
            title = "Delete Comment?",
            message = "Are you sure you want to delete this comment? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                val toDelete = comment
                commentPendingDelete = null
                commentVm.deleteComment(toDelete)
                onCommentCountChanged(-1)
                ToastManager.showToast("Comment deleted")
            },
            dismissText = "Cancel",
            onDismiss = { commentPendingDelete = null }
        )
    }
}

/**
 * Thread Branch Connector Gutter.
 * Draws:
 * - Continuous vertical trunk at [trunkX] from y=0 to y=height (if not last reply)
 * - Smooth curved branch turning 90 degrees right into child avatar at [branchEndX]
 */
@Composable
fun ThreadBranchGutter(
    isLastReply: Boolean,
    trunkX: Dp = 17.dp,
    branchEndX: Dp = 34.dp,
    avatarCenterY: Dp = 14.dp,
    cornerRadius: Dp = 10.dp,
    color: Color,
    strokeWidth: Dp = 1.5.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val trunkXPx = trunkX.toPx()
        val branchEndXPx = branchEndX.toPx()
        val avatarCenterYPx = avatarCenterY.toPx()
        val cornerRadiusPx = cornerRadius.toPx()
        val strokeWidthPx = strokeWidth.toPx()

        val path = Path().apply {
            moveTo(trunkXPx, 0f)
            val curveStartY = (avatarCenterYPx - cornerRadiusPx).coerceAtLeast(0f)

            if (isLastReply) {
                // Descend to curve start
                lineTo(trunkXPx, curveStartY)
                // Smooth curved arc turning right
                quadraticBezierTo(
                    trunkXPx, avatarCenterYPx,
                    trunkXPx + cornerRadiusPx, avatarCenterYPx
                )
                // Horizontal branch into avatar edge
                lineTo(branchEndXPx, avatarCenterYPx)
            } else {
                // Full vertical trunk to bottom of this reply item
                lineTo(trunkXPx, size.height)

                // Branch turning right into avatar
                moveTo(trunkXPx, curveStartY)
                quadraticBezierTo(
                    trunkXPx, avatarCenterYPx,
                    trunkXPx + cornerRadiusPx, avatarCenterYPx
                )
                lineTo(branchEndXPx, avatarCenterYPx)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Top-Level Comment Card with Avatar, Handle, Creator Badge, Thumbs Up/Down,
 * Creator Heart, Reply Pill, and Threaded Tree Accordion.
 */
@Composable
fun YouTubeCommentCard(
    comment: FlashComment,
    targetId: String,
    creatorId: String,
    creatorUsername: String = "",
    creatorAvatarUrl: String = "",
    currentUserId: String?,
    replies: List<FlashComment> = emptyList(),
    isRepliesExpanded: Boolean = false,
    treeLineColor: Color,
    onToggleReplies: () -> Unit = {},
    onReply: (FlashComment) -> Unit,
    onReplyWithFlash: ((String) -> Unit)? = null,
    onToggleLike: (String) -> Unit,
    onToggleDislike: (String) -> Unit,
    onToggleHeart: (FlashComment) -> Unit,
    onTogglePin: (FlashComment) -> Unit,
    onDelete: (FlashComment) -> Unit,
    onRetry: (FlashComment) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val textPrimary = colors.textMain
    val textSecondary = colors.textMuted
    val ytBlue = colors.primary
    val creatorBadgeBg = if (isDark) Color(0xFF1E2433) else Color(0xFFEBF3FF)
    val creatorBadgeText = ytBlue
    val surfacePillBg = if (isDark) Color(0xFF1E212A) else Color(0xFFF1F5F9)
    val sheetBg = if (isDark) Color(0xFF111216) else Color.White

    var menuExpanded by remember { mutableStateOf(false) }
    var isTextExpanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    val isLiked = currentUserId != null && comment.likedBy.contains(currentUserId)
    val isDisliked = currentUserId != null && comment.dislikedBy.contains(currentUserId)
    val isVideoAuthor = currentUserId != null && currentUserId == creatorId
    val isCommentAuthor = currentUserId != null && currentUserId == comment.userId
    val isCommentByVideoAuthor = comment.userId == creatorId

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "likeBounce"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Pinned comment indicator pill
        if (comment.isPinned) {
            Surface(
                shape = CircleShape,
                color = surfacePillBg,
                modifier = Modifier.padding(start = 44.dp, bottom = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = ChromaIcons.PinFilled,
                        contentDescription = "Pinned",
                        tint = ytBlue,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Pinned by @${creatorUsername.ifBlank { "creator" }}",
                        color = ytBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 2. Parent Avatar & Body Row (with Root vertical trunk descending if replies expanded)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left: Avatar + vertical trunk line
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(34.dp)
                    .fillMaxHeight()
            ) {
                val isOnline = OnlineStatusManager.rememberUserPresence(comment.userId).online
                IosAvatarView(
                    name = comment.displayName.ifBlank { comment.username },
                    avatarUrl = comment.avatarUrl,
                    size = 34.dp,
                    isOnline = isOnline,
                    statusRingColor = sheetBg
                )

                // Root vertical trunk extending below avatar when thread is expanded
                if (replies.isNotEmpty() && isRepliesExpanded) {
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .weight(1f)
                            .background(treeLineColor)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Right: Comment body & actions
            Column(modifier = Modifier.weight(1f)) {
                // User & Timestamp Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCommentByVideoAuthor) {
                        Surface(
                            shape = CircleShape,
                            color = creatorBadgeBg,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "@${comment.username}",
                                    color = creatorBadgeText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(3.dp))
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = "Creator",
                                    tint = creatorBadgeText,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "@${comment.username}",
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    if (comment.verified) {
                        VerifiedBadge(size = 11.dp)
                        Spacer(Modifier.width(4.dp))
                    }

                    Spacer(Modifier.width(4.dp))

                    if (comment.isPending) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = ytBlue,
                                strokeWidth = 1.5.dp,
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Posting...",
                                color = ytBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (comment.isError) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onRetry(comment) }
                        ) {
                            Icon(
                                imageVector = Lucide.CircleAlert,
                                contentDescription = null,
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Failed • Tap to retry",
                                color = Color(0xFFFF3B30),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = android.text.format.DateUtils.getRelativeTimeSpanString(
                                comment.timestamp,
                                System.currentTimeMillis(),
                                android.text.format.DateUtils.MINUTE_IN_MILLIS
                            ).toString(),
                            color = textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Comment Text with @mentions highlighting
                Spacer(Modifier.height(3.dp))
                val displayText = remember(comment.text) {
                    val baseText = comment.text
                    buildAnnotatedString {
                        val parts = baseText.split(" ")
                        parts.forEachIndexed { index, part ->
                            if (part.startsWith("@") && part.length > 1) {
                                withStyle(SpanStyle(color = ytBlue, fontWeight = FontWeight.SemiBold)) {
                                    append(part)
                                }
                            } else {
                                append(part)
                            }
                            if (index < parts.size - 1) append(" ")
                        }
                    }
                }

                Text(
                    text = displayText,
                    color = if (comment.isError) Color(0xFF9CA3AF) else textPrimary,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    maxLines = if (isTextExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        if (!isTextExpanded && result.hasVisualOverflow) {
                            hasOverflow = true
                        }
                    }
                )
                if (hasOverflow) {
                    Text(
                        text = if (isTextExpanded) "Show less" else "Read more",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        modifier = Modifier
                            .clickable { isTextExpanded = !isTextExpanded }
                            .padding(vertical = 2.dp)
                    )
                }

                // Action Bar: ThumbsUp, ThumbsDown, Creator Heart, Reply, 3-dots
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    // Thumbs Up (Like)
                    Surface(
                        shape = CircleShape,
                        color = if (isLiked) ytBlue.copy(alpha = 0.12f) else Color.Transparent,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleLike(comment.id)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) ChromaIcons.ThumbsUpFilled else Lucide.ThumbsUp,
                                contentDescription = "Like",
                                tint = if (isLiked) ytBlue else textSecondary,
                                modifier = Modifier
                                    .size(15.dp)
                                    .graphicsLayer {
                                        scaleX = likeScale
                                        scaleY = likeScale
                                    }
                            )
                            if (comment.likesCount > 0) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = formatCount(comment.likesCount),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLiked) ytBlue else textSecondary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Thumbs Down (Dislike)
                    Surface(
                        shape = CircleShape,
                        color = if (isDisliked) textPrimary.copy(alpha = 0.10f) else Color.Transparent,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleDislike(comment.id)
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDisliked) ChromaIcons.ThumbsDownFilled else Lucide.ThumbsDown,
                                contentDescription = "Dislike",
                                tint = if (isDisliked) textPrimary else textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Creator Heart
                    val displayHeartAvatar = creatorAvatarUrl.ifBlank { comment.avatarUrl }
                    if (comment.isCreatorHearted) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .then(
                                    if (isVideoAuthor) Modifier.clickable { onToggleHeart(comment) } else Modifier
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(20.dp)) {
                                Icon(
                                    imageVector = ChromaIcons.HeartFilled,
                                    contentDescription = "Loved by creator",
                                    tint = Color(0xFFFF0033),
                                    modifier = Modifier
                                        .size(15.dp)
                                        .align(Alignment.TopStart)
                                )
                                AsyncImage(
                                    model = displayHeartAvatar,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .align(Alignment.BottomEnd)
                                        .border(0.75.dp, sheetBg, CircleShape)
                                )
                            }
                        }
                    } else if (isVideoAuthor) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onToggleHeart(comment) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = ChromaIcons.HeartFilled,
                                contentDescription = "Heart as creator",
                                tint = textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Reply Capsule Button
                    if (!comment.isPending && !comment.isError) {
                        Surface(
                            shape = CircleShape,
                            color = surfacePillBg,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onReply(comment) }
                        ) {
                            Text(
                                text = "Reply",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (onReplyWithFlash != null) {
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { onReplyWithFlash(comment.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Video,
                                    contentDescription = "Reply with Flash",
                                    tint = textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // 3-dots overflow menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "More actions",
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF1E212A) else Color.White)
                        ) {
                            if (isVideoAuthor) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            ChromaIcons.PinFilled,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = textPrimary
                                        )
                                    },
                                    text = { Text(if (comment.isPinned) "Unpin comment" else "Pin to top", color = textPrimary) },
                                    onClick = {
                                        menuExpanded = false
                                        onTogglePin(comment)
                                    }
                                )
                            }
                            if (isCommentAuthor || isVideoAuthor) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Lucide.Trash2,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFFFF3B30)
                                        )
                                    },
                                    text = { Text("Delete", color = Color(0xFFFF3B30)) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete(comment)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Lucide.Copy,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = textPrimary
                                    )
                                },
                                text = { Text("Copy text", color = textPrimary) },
                                onClick = {
                                    menuExpanded = false
                                    clipboard.setText(AnnotatedString(comment.text))
                                    ToastManager.showToast("Comment copied")
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Threaded Nested Replies Section (Root & Branch lines)
        if (replies.isNotEmpty()) {
            val replyCount = maxOf(replies.size, comment.replyCount)

            // Toggle Button Row with vertical trunk continuing through gutter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 4.dp)
            ) {
                // 34.dp gutter with vertical trunk line centered at 17.dp
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRepliesExpanded) {
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .fillMaxHeight()
                                .background(treeLineColor)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Toggle Button Capsule Pill
                Surface(
                    shape = CircleShape,
                    color = surfacePillBg,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onToggleReplies() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = if (isRepliesExpanded) Lucide.ChevronUp else Lucide.ChevronDown,
                            contentDescription = null,
                            tint = ytBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isRepliesExpanded) "Hide replies" else "View $replyCount ${if (replyCount == 1) "reply" else "replies"}",
                            color = ytBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isRepliesExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val sortedReplies = replies.sortedWith(
                        compareBy<FlashComment> { it.timestamp }
                            .thenBy { it.isPending }
                    )

                    sortedReplies.forEachIndexed { index, childReply ->
                        val isLast = (index == sortedReplies.size - 1)

                        YouTubeChildReplyCard(
                            comment = childReply,
                            targetId = targetId,
                            creatorId = creatorId,
                            creatorUsername = creatorUsername,
                            creatorAvatarUrl = creatorAvatarUrl,
                            currentUserId = currentUserId,
                            isLastReply = isLast,
                            treeLineColor = treeLineColor,
                            onReply = onReply,
                            onReplyWithFlash = onReplyWithFlash,
                            onToggleLike = onToggleLike,
                            onToggleDislike = onToggleDislike,
                            onToggleHeart = onToggleHeart,
                            onTogglePin = onTogglePin,
                            onDelete = onDelete,
                            onRetry = onRetry
                        )
                    }
                }
            }
        }
    }
}

/**
 * Child Reply Card rendered with Thread Branch connector line directly into the avatar.
 */
@Composable
fun YouTubeChildReplyCard(
    comment: FlashComment,
    targetId: String,
    creatorId: String,
    creatorUsername: String = "",
    creatorAvatarUrl: String = "",
    currentUserId: String?,
    isLastReply: Boolean,
    treeLineColor: Color,
    onReply: (FlashComment) -> Unit,
    onReplyWithFlash: ((String) -> Unit)? = null,
    onToggleLike: (String) -> Unit,
    onToggleDislike: (String) -> Unit,
    onToggleHeart: (FlashComment) -> Unit,
    onTogglePin: (FlashComment) -> Unit,
    onDelete: (FlashComment) -> Unit,
    onRetry: (FlashComment) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val textPrimary = colors.textMain
    val textSecondary = colors.textMuted
    val ytBlue = colors.primary
    val creatorBadgeBg = if (isDark) Color(0xFF1E2433) else Color(0xFFEBF3FF)
    val creatorBadgeText = ytBlue
    val surfacePillBg = if (isDark) Color(0xFF1E212A) else Color(0xFFF1F5F9)
    val sheetBg = if (isDark) Color(0xFF111216) else Color.White

    var menuExpanded by remember { mutableStateOf(false) }
    var isTextExpanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    val isLiked = currentUserId != null && comment.likedBy.contains(currentUserId)
    val isDisliked = currentUserId != null && comment.dislikedBy.contains(currentUserId)
    val isVideoAuthor = currentUserId != null && currentUserId == creatorId
    val isCommentAuthor = currentUserId != null && currentUserId == comment.userId
    val isCommentByVideoAuthor = comment.userId == creatorId

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "childLikeBounce"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // 1. Thread Branch Connector Gutter (Trunk at x=17.dp, branch curving to x=34.dp)
        ThreadBranchGutter(
            isLastReply = isLastReply,
            trunkX = 17.dp,
            branchEndX = 34.dp,
            avatarCenterY = 14.dp,
            cornerRadius = 10.dp,
            color = treeLineColor,
            strokeWidth = 1.5.dp,
            modifier = Modifier
                .width(34.dp)
                .fillMaxHeight()
        )

        // 2. Child Avatar (28.dp, starting at x=34.dp)
        val isOnline = OnlineStatusManager.rememberUserPresence(comment.userId).online
        IosAvatarView(
            name = comment.displayName.ifBlank { comment.username },
            avatarUrl = comment.avatarUrl,
            size = 28.dp,
            isOnline = isOnline,
            statusRingColor = sheetBg
        )

        Spacer(Modifier.width(8.dp))

        // 3. Child Comment Body & Actions
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
        ) {
            // User & Timestamp Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCommentByVideoAuthor) {
                    Surface(
                        shape = CircleShape,
                        color = creatorBadgeBg,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "@${comment.username}",
                                color = creatorBadgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(3.dp))
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = "Creator",
                                tint = creatorBadgeText,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "@${comment.username}",
                        color = textPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                if (comment.verified) {
                    VerifiedBadge(size = 10.5.dp)
                    Spacer(Modifier.width(4.dp))
                }

                Spacer(Modifier.width(4.dp))

                if (comment.isPending) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = ytBlue,
                            strokeWidth = 1.5.dp,
                            modifier = Modifier.size(9.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Posting...",
                            color = ytBlue,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (comment.isError) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onRetry(comment) }
                    ) {
                        Icon(
                            imageVector = Lucide.CircleAlert,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "Failed • Tap to retry",
                            color = Color(0xFFFF3B30),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = android.text.format.DateUtils.getRelativeTimeSpanString(
                            comment.timestamp,
                            System.currentTimeMillis(),
                            android.text.format.DateUtils.MINUTE_IN_MILLIS
                        ).toString(),
                        color = textSecondary,
                        fontSize = 10.5.sp
                    )
                }
            }

            // Comment Text with @mention highlighting
            Spacer(Modifier.height(3.dp))
            val displayText = remember(comment.text, comment.replyToUsername) {
                val baseText = comment.text
                if (!comment.replyToUsername.isNullOrBlank() && !baseText.startsWith("@${comment.replyToUsername}")) {
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = ytBlue, fontWeight = FontWeight.SemiBold)) {
                            append("@${comment.replyToUsername} ")
                        }
                        append(baseText)
                    }
                } else {
                    buildAnnotatedString {
                        val parts = baseText.split(" ")
                        parts.forEachIndexed { index, part ->
                            if (part.startsWith("@") && part.length > 1) {
                                withStyle(SpanStyle(color = ytBlue, fontWeight = FontWeight.SemiBold)) {
                                    append(part)
                                }
                            } else {
                                append(part)
                            }
                            if (index < parts.size - 1) append(" ")
                        }
                    }
                }
            }

            Text(
                text = displayText,
                color = if (comment.isError) Color(0xFF9CA3AF) else textPrimary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = if (isTextExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    if (!isTextExpanded && result.hasVisualOverflow) {
                        hasOverflow = true
                    }
                }
            )
            if (hasOverflow) {
                Text(
                    text = if (isTextExpanded) "Show less" else "Read more",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier
                        .clickable { isTextExpanded = !isTextExpanded }
                        .padding(vertical = 2.dp)
                )
            }

            // Action Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp)
            ) {
                // Thumbs Up (Like)
                Surface(
                    shape = CircleShape,
                    color = if (isLiked) ytBlue.copy(alpha = 0.12f) else Color.Transparent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleLike(comment.id)
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) ChromaIcons.ThumbsUpFilled else Lucide.ThumbsUp,
                            contentDescription = "Like",
                            tint = if (isLiked) ytBlue else textSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .graphicsLayer {
                                    scaleX = likeScale
                                    scaleY = likeScale
                                }
                        )
                        if (comment.likesCount > 0) {
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = formatCount(comment.likesCount),
                                fontSize = 11.sp,
                                fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLiked) ytBlue else textSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Thumbs Down (Dislike)
                Surface(
                    shape = CircleShape,
                    color = if (isDisliked) textPrimary.copy(alpha = 0.10f) else Color.Transparent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleDislike(comment.id)
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDisliked) ChromaIcons.ThumbsDownFilled else Lucide.ThumbsDown,
                            contentDescription = "Dislike",
                            tint = if (isDisliked) textPrimary else textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Creator Heart
                val displayHeartAvatar = creatorAvatarUrl.ifBlank { comment.avatarUrl }
                if (comment.isCreatorHearted) {
                    Spacer(Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(
                                if (isVideoAuthor) Modifier.clickable { onToggleHeart(comment) } else Modifier
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(modifier = Modifier.size(19.dp)) {
                            Icon(
                                imageVector = ChromaIcons.HeartFilled,
                                contentDescription = "Loved by creator",
                                tint = Color(0xFFFF0033),
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.TopStart)
                            )
                            AsyncImage(
                                model = displayHeartAvatar,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(10.5.dp)
                                    .clip(CircleShape)
                                    .align(Alignment.BottomEnd)
                                    .border(0.75.dp, sheetBg, CircleShape)
                            )
                        }
                    }
                } else if (isVideoAuthor) {
                    Spacer(Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onToggleHeart(comment) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = ChromaIcons.HeartFilled,
                            contentDescription = "Heart as creator",
                            tint = textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                // Reply Capsule Button
                if (!comment.isPending && !comment.isError) {
                    Surface(
                        shape = CircleShape,
                        color = surfacePillBg,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onReply(comment) }
                    ) {
                        Text(
                            text = "Reply",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }

                    if (onReplyWithFlash != null) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = { onReplyWithFlash(comment.id) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Video,
                                contentDescription = "Reply with Flash",
                                tint = textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // 3-dots overflow menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.EllipsisVertical,
                            contentDescription = "More actions",
                            tint = textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(if (isDark) Color(0xFF1E212A) else Color.White)
                    ) {
                        if (isCommentAuthor || isVideoAuthor) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Lucide.Trash2,
                                        null,
                                        modifier = Modifier.size(15.dp),
                                        tint = Color(0xFFFF3B30)
                                    )
                                },
                                text = { Text("Delete", color = Color(0xFFFF3B30), fontSize = 13.sp) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete(comment)
                                }
                            )
                        }
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    Lucide.Copy,
                                    null,
                                    modifier = Modifier.size(15.dp),
                                    tint = textPrimary
                                )
                            },
                            text = { Text("Copy text", color = textPrimary, fontSize = 13.sp) },
                            onClick = {
                                menuExpanded = false
                                clipboard.setText(AnnotatedString(comment.text))
                                ToastManager.showToast("Comment copied")
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Formats integer counts into standard notation ("1.2K", "3.4M"). */
private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
