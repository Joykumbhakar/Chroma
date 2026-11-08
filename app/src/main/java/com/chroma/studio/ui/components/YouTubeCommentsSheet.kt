package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.model.FlashComment
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.CommentSortOrder
import com.chroma.studio.viewmodel.FlashCommentViewModel
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth

/**
 * Flagship YouTube-Grade Comments Bottom Sheet.
 * Features:
 * - Realtime updates and instant optimistic comment posting
 * - Adaptive light and dark mode palette matching YouTube mobile
 * - Top comments and Newest first sorting with interactive dropdown
 * - Pinned comments with pin indicator & unpin capability
 * - Video/Post Creator author badge pill
 * - Expandable "Read more" / "Show less" on long comments
 * - Thumbs up (Like) with like count & thumbs down (Dislike)
 * - Creator Heart with overlay creator mini avatar
 * - Reply mode with context banner and keyboard focus
 * - Quick emoji selection bar
 * - Threaded nested replies accordion with indent and reply count
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

    // YouTube exact palette tokens
    val sheetBg = if (isDark) Color(0xFF0F0F0F) else Color.White
    val textPrimary = if (isDark) Color(0xFFF1F1F1) else Color(0xFF0F0F0F)
    val textSecondary = if (isDark) Color(0xFFAAAAAA) else Color(0xFF606060)
    val dividerColor = if (isDark) Color(0xFF272727) else Color(0xFFE5E7EB)
    val surfaceBg = if (isDark) Color(0xFF272727) else Color(0xFFF3F4F6)
    val ytBlue = if (isDark) Color(0xFF3EA6FF) else Color(0xFF065FD4)
    val dragHandleColor = if (isDark) Color(0xFF3F3F3F) else Color(0xFFD1D5DB)

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

    LaunchedEffect(targetId) {
        commentVm.loadComments(targetId)
    }

    // Split top-level and replies
    val repliesMap = remember(comments) {
        comments.filter { !it.replyToCommentId.isNullOrBlank() }
            .groupBy { it.replyToCommentId!! }
    }

    val topLevelComments = remember(comments, sortOrder) {
        val parents = comments.filter { it.replyToCommentId.isNullOrBlank() }
        when (sortOrder) {
            CommentSortOrder.TOP_COMMENTS -> {
                parents.sortedWith(
                    compareByDescending<FlashComment> { it.isPinned }
                        .thenByDescending { it.likesCount }
                        .thenByDescending { it.timestamp }
                )
            }
            CommentSortOrder.NEWEST_FIRST -> {
                parents.sortedWith(
                    compareByDescending<FlashComment> { it.isPinned }
                        .thenByDescending { it.timestamp }
                )
            }
        }
    }

    val totalCommentCount = maxOf(targetCommentCount, comments.size)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = dragHandleColor) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(sheetBg)
        ) {
            // YouTube Comments Header: Count, Sort By Dropdown, and Close Button
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
                Text(
                    text = formatCount(totalCommentCount),
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = textSecondary
                )

                Spacer(Modifier.weight(1f))

                // Sort Dropdown Button
                if (!turnOffComments) {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { sortMenuExpanded = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Lucide.SlidersHorizontal,
                                contentDescription = "Sort comments",
                                tint = textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (sortOrder == CommentSortOrder.TOP_COMMENTS) "Top comments" else "Newest first",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier.background(if (isDark) Color(0xFF212121) else Color.White)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (sortOrder == CommentSortOrder.TOP_COMMENTS) {
                                            Icon(Lucide.Check, null, tint = ytBlue, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                        } else {
                                            Spacer(Modifier.width(24.dp))
                                        }
                                        Text(
                                            "Top comments",
                                            fontWeight = if (sortOrder == CommentSortOrder.TOP_COMMENTS) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp,
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
                                            Icon(Lucide.Check, null, tint = ytBlue, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                        } else {
                                            Spacer(Modifier.width(24.dp))
                                        }
                                        Text(
                                            "Newest first",
                                            fontWeight = if (sortOrder == CommentSortOrder.NEWEST_FIRST) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp,
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

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Close",
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
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

            // Bottom YouTube Comment Dock (only if comments enabled)
            if (!turnOffComments) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(sheetBg)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    // Replying Indicator Bar
                    if (replyingToComment != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(surfaceBg)
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
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Quick Reactions Emoji Bar (YouTube Signature)
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
                                    .background(surfaceBg)
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
                        AsyncImage(
                            model = FirebaseAuth.getInstance().currentUser?.photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(surfaceBg)
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
                                focusedContainerColor = surfaceBg,
                                unfocusedContainerColor = surfaceBg,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            maxLines = 4
                        )
                        if (commentText.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (FirebaseAuth.getInstance().currentUser == null || com.chroma.studio.data.SavedAccountManager.isGuest(context)) {
                                        onRequireLogin()
                                        return@IconButton
                                    }
                                    val parentTargetId = replyingToComment?.id
                                    commentVm.addComment(
                                        videoId = targetId,
                                        text = commentText.trim(),
                                        replyToId = parentTargetId
                                    )
                                    onCommentCountChanged(1)
                                    if (parentTargetId != null) {
                                        expandedThreads[parentTargetId] = true
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
 * YouTube-Grade Comment Card with Avatar, Handle, Author Badges, Read More,
 * Thumbs Up, Thumbs Down, Creator Heart, Reply, and Threaded Accordion.
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
    onToggleReplies: () -> Unit = {},
    onReply: (FlashComment) -> Unit,
    onReplyWithFlash: ((String) -> Unit)? = null,
    onToggleLike: (String) -> Unit,
    onToggleDislike: (String) -> Unit,
    onToggleHeart: (FlashComment) -> Unit,
    onTogglePin: (FlashComment) -> Unit,
    onDelete: (FlashComment) -> Unit,
    onRetry: (FlashComment) -> Unit,
    isChildReply: Boolean = false
) {
    val clipboard = LocalClipboardManager.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val textPrimary = if (isDark) Color(0xFFF1F1F1) else Color(0xFF0F0F0F)
    val textSecondary = if (isDark) Color(0xFFAAAAAA) else Color(0xFF606060)
    val ytBlue = if (isDark) Color(0xFF3EA6FF) else Color(0xFF065FD4)
    val creatorBadgeBg = if (isDark) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val creatorBadgeText = if (isDark) Color.White else Color(0xFF1F2937)

    var menuExpanded by remember { mutableStateOf(false) }
    var isTextExpanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    val isLiked = currentUserId != null && comment.likedBy.contains(currentUserId)
    val isDisliked = currentUserId != null && comment.dislikedBy.contains(currentUserId)
    val isVideoAuthor = currentUserId != null && currentUserId == creatorId
    val isCommentAuthor = currentUserId != null && currentUserId == comment.userId
    val isCommentByVideoAuthor = comment.userId == creatorId

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Pinned comment indicator (YouTube Signature)
        if (comment.isPinned) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = if (isChildReply) 0.dp else 44.dp, bottom = 4.dp)
            ) {
                Icon(
                    imageVector = ChromaIcons.PinFilled,
                    contentDescription = "Pinned",
                    tint = textSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Pinned by @${creatorUsername.ifBlank { "creator" }}",
                    color = textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 2. Avatar + Comment Body
        Row(modifier = Modifier.fillMaxWidth()) {
            IosAvatarView(
                name = comment.displayName.ifBlank { comment.username },
                avatarUrl = comment.avatarUrl,
                size = if (isChildReply) 28.dp else 34.dp
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // User & Timestamp Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCommentByVideoAuthor) {
                        // YouTube Creator Author Badge Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
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
                        Text(
                            text = "Sending...",
                            color = ytBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (comment.isError) {
                        Text(
                            text = "Failed • Tap to retry",
                            color = Color(0xFFFF3B30),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onRetry(comment) }
                        )
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

                // Comment Text with Read More / Show Less expansion
                Spacer(Modifier.height(3.dp))
                Text(
                    text = comment.text,
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

                // 3. YouTube Action Bar: ThumbsUp, ThumbsDown, Creator Heart, Reply, 3-dots
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    // Thumbs Up (Like)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onToggleLike(comment.id) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) ChromaIcons.ThumbsUpFilled else Lucide.ThumbsUp,
                            contentDescription = "Like",
                            tint = if (isLiked) (if (isDark) Color.White else Color(0xFF0F0F0F)) else textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        if (comment.likesCount > 0) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = formatCount(comment.likesCount),
                                fontSize = 11.5.sp,
                                fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLiked) textPrimary else textSecondary
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Thumbs Down (Dislike)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onToggleDislike(comment.id) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isDisliked) ChromaIcons.ThumbsDownFilled else Lucide.ThumbsDown,
                            contentDescription = "Dislike",
                            tint = if (isDisliked) (if (isDark) Color.White else Color(0xFF0F0F0F)) else textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Creator Heart (YouTube Signature)
                    val displayHeartAvatar = creatorAvatarUrl.ifBlank { comment.avatarUrl }
                    if (comment.isCreatorHearted) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
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
                                        .border(0.75.dp, if (isDark) Color(0xFF0F0F0F) else Color.White, CircleShape)
                                )
                            }
                        }
                    } else if (isVideoAuthor) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
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

                    // Reply Text Button
                    if (!comment.isPending && !comment.isError) {
                        Text(
                            text = "Reply",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onReply(comment) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )

                        // Video reply shortcut button (if supported)
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
                            modifier = Modifier.background(if (isDark) Color(0xFF212121) else Color.White)
                        ) {
                            if (isVideoAuthor && !isChildReply) {
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

        // 4. Threaded Nested Replies Accordion (YouTube Signature)
        if (replies.isNotEmpty() && !isChildReply) {
            val replyCount = replies.size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 44.dp, top = 2.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggleReplies() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isRepliesExpanded) Lucide.ChevronUp else Lucide.ChevronDown,
                    contentDescription = null,
                    tint = ytBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isRepliesExpanded) "Hide $replyCount ${if (replyCount == 1) "reply" else "replies"}" else "$replyCount ${if (replyCount == 1) "reply" else "replies"}",
                    color = ytBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = isRepliesExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 38.dp, top = 4.dp)
                ) {
                    replies.sortedBy { it.timestamp }.forEach { childReply ->
                        YouTubeCommentCard(
                            comment = childReply,
                            targetId = targetId,
                            creatorId = creatorId,
                            creatorUsername = creatorUsername,
                            creatorAvatarUrl = creatorAvatarUrl,
                            currentUserId = currentUserId,
                            onReply = onReply,
                            onReplyWithFlash = onReplyWithFlash,
                            onToggleLike = onToggleLike,
                            onToggleDislike = onToggleDislike,
                            onToggleHeart = onToggleHeart,
                            onTogglePin = onTogglePin,
                            onDelete = onDelete,
                            onRetry = onRetry,
                            isChildReply = true
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/** Formats integer counts into YouTube notation ("1.2K", "3.4M"). */
private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
