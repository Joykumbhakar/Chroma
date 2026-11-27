package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chroma.studio.data.SavedAccountManager
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.FlashCommentViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * Unified Studio Comment Sheet wrapper for ChromaStudio / Oscellion.
 * Connects PhotoPostCommentBottomSheet to live Firestore state via FlashCommentViewModel:
 * - Resolves current user avatar & handle from SavedAccountManager + FirebaseAuth
 * - Propagates loading shimmer, pending/error retry states, and comments-disabled mode
 * - Preserves thread author mention directory and optional Reply-with-Flash trigger
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeCommentsSheet(
    targetId: String,
    creatorId: String = "",
    creatorUsername: String = "",
    creatorAvatarUrl: String = "",
    creatorVerified: Boolean = false,
    creatorVerifiedTone: String? = null,
    postCaption: String = "",
    postThumbnailUrl: String = "",
    targetCommentCount: Int = 0,
    turnOffComments: Boolean = false,
    commentVm: FlashCommentViewModel = viewModel(),
    onDismiss: () -> Unit,
    onReplyWithFlash: ((String?) -> Unit)? = null,
    onRequireLogin: () -> Unit = {},
    onCommentCountChanged: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = LocalOscellionColors.current.isDark
    val auth = FirebaseAuth.getInstance()
    val authUser = auth.currentUser

    val liveComments by commentVm.comments.collectAsState()
    val isLoading by commentVm.isLoading.collectAsState()

    LaunchedEffect(targetId) {
        if (targetId.isNotBlank()) {
            commentVm.loadComments(targetId)
        }
    }

    val currentUser = remember(authUser, creatorId, context) {
        val savedAccount = authUser?.uid?.let { uid ->
            SavedAccountManager.getSavedAccounts(context).firstOrNull { it.uid == uid }
        }
        val rawHandle = savedAccount?.username?.takeIf { it.isNotBlank() }
            ?: authUser?.displayName?.replace(" ", "")?.lowercase()?.takeIf { it.isNotBlank() }
            ?: authUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: (authUser?.uid?.take(6) ?: "creator")
        val resolvedName = savedAccount?.displayName?.takeIf { it.isNotBlank() }
            ?: authUser?.displayName?.takeIf { it.isNotBlank() }
            ?: rawHandle
        val resolvedAvatar = savedAccount?.avatarUrl?.takeIf { it.isNotBlank() }
            ?: authUser?.photoUrl?.toString().orEmpty()

        CommentAuthor(
            name = resolvedName,
            handle = rawHandle,
            avatarStyle = AvatarStyle.ROYAL_BLUE,
            verified = null,
            role = if (creatorId.isNotBlank() && authUser?.uid == creatorId) "Creator" else null,
            isSelf = true,
            avatarUrl = resolvedAvatar,
            userId = authUser?.uid.orEmpty(),
            isOnline = true
        )
    }

    val postSummary = remember(
        targetId,
        creatorId,
        creatorUsername,
        creatorAvatarUrl,
        creatorVerified,
        postCaption,
        postThumbnailUrl
    ) {
        val cleanCreatorHandle = creatorUsername.ifBlank { "creator" }
        PhotoPostSummary(
            id = targetId,
            author = CommentAuthor(
                name = cleanCreatorHandle,
                handle = cleanCreatorHandle,
                avatarStyle = AvatarStyle.ROYAL_BLUE,
                verified = resolveVerifiedBadgeType(creatorVerified, creatorVerifiedTone),
                verifiedTone = creatorVerifiedTone,
                role = "Creator",
                avatarUrl = creatorAvatarUrl,
                userId = creatorId
            ),
            authorHandle = cleanCreatorHandle,
            authorVerified = resolveVerifiedBadgeType(creatorVerified, creatorVerifiedTone),
            authorVerifiedTone = creatorVerifiedTone,
            caption = postCaption.trim(),
            thumbnailUrl = postThumbnailUrl.trim(),
            relativeTime = "",
            likeCountLabel = ""
        )
    }

    val mappedComments = remember(liveComments, creatorId, currentUser.userId) {
        val roots = liveComments.filter { it.replyToCommentId.isNullOrBlank() }
        val repliesMap = liveComments
            .filter { !it.replyToCommentId.isNullOrBlank() }
            .groupBy { it.replyToCommentId!! }

        fun mapReactions(raw: Map<String, List<String>>): Map<String, ReactionData> =
            raw.mapNotNull { (emoji, uids) ->
                if (uids.isEmpty()) null
                else emoji to ReactionData(
                    count = uids.size,
                    activeByMe = currentUser.userId.isNotBlank() && uids.contains(currentUser.userId)
                )
            }.toMap()

        roots.map { flash ->
            val replies = repliesMap[flash.id]?.map { reply ->
                CommentReply(
                    id = reply.id,
                    author = CommentAuthor(
                        name = reply.displayName.ifBlank { reply.username },
                        handle = reply.username.ifBlank { "user" },
                        avatarStyle = AvatarStyle.CLASSIC_MAC,
                        verified = resolveVerifiedBadgeType(reply.verified, reply.verifiedTone),
                        verifiedTone = reply.verifiedTone,
                        role = if (creatorId.isNotBlank() && reply.userId == creatorId) "Creator" else null,
                        isSelf = currentUser.userId.isNotBlank() && reply.userId == currentUser.userId,
                        avatarUrl = reply.avatarUrl,
                        userId = reply.userId
                    ),
                    text = reply.text,
                    createdAtMillis = reply.timestamp,
                    edited = reply.edited,
                    likes = reply.likesCount,
                    likedByMe = currentUser.userId.isNotBlank() && reply.likedBy.contains(currentUser.userId),
                    creatorHeart = reply.isCreatorHearted,
                    reactions = mapReactions(reply.reactions),
                    replyFlashVideoUrl = reply.replyFlashVideoUrl,
                    isPending = reply.isPending,
                    isError = reply.isError
                )
            } ?: emptyList()

            PostComment(
                id = flash.id,
                author = CommentAuthor(
                    name = flash.displayName.ifBlank { flash.username },
                    handle = flash.username.ifBlank { "user" },
                    avatarStyle = AvatarStyle.CLASSIC_MAC,
                    verified = resolveVerifiedBadgeType(flash.verified, flash.verifiedTone),
                    verifiedTone = flash.verifiedTone,
                    role = if (creatorId.isNotBlank() && flash.userId == creatorId) "Creator" else null,
                    isSelf = currentUser.userId.isNotBlank() && flash.userId == currentUser.userId,
                    avatarUrl = flash.avatarUrl,
                    userId = flash.userId
                ),
                text = flash.text,
                createdAtMillis = flash.timestamp,
                edited = flash.edited,
                pinned = flash.isPinned,
                likes = flash.likesCount,
                likedByMe = currentUser.userId.isNotBlank() && flash.likedBy.contains(currentUser.userId),
                creatorHeart = flash.isCreatorHearted,
                reactions = mapReactions(flash.reactions),
                isPending = flash.isPending,
                isError = flash.isError,
                replies = replies
            )
        }
    }

    val threadMentionDirectory = remember(mappedComments, postSummary.author) {
        buildList {
            postSummary.author?.let { if (it.handle.isNotBlank()) add(it) }
            mappedComments.forEach { comment ->
                add(comment.author)
                comment.replies.forEach { reply -> add(reply.author) }
            }
        }
            .filter { it.handle.isNotBlank() }
            .distinctBy { it.handle.lowercase() }
    }

    var activeReplyFlashUrl by remember { mutableStateOf<String?>(null) }

    PhotoPostCommentBottomSheet(
        postSummary = postSummary,
        currentUser = currentUser,
        onDismissRequest = onDismiss,
        initialComments = mappedComments,
        mentionDirectory = threadMentionDirectory,
        isDarkTheme = isDark,
        isLoading = isLoading,
        commentsDisabled = turnOffComments,
        expectedCommentCount = targetCommentCount,
        enableLocalRichAttachments = false,
        onReplyWithFlash = if (onReplyWithFlash != null) {
            { parentCommentId ->
                if (auth.currentUser == null || SavedAccountManager.isGuest(context)) {
                    onRequireLogin()
                } else {
                    onReplyWithFlash.invoke(parentCommentId)
                }
            }
        } else null,
        onCommentCountChanged = null,
        onTotalCommentCountChanged = null,
        onBeforeSubmitCheck = {
            if (auth.currentUser == null || SavedAccountManager.isGuest(context)) {
                onRequireLogin()
                false
            } else {
                true
            }
        },
        onSubmitComment = { text, parentCommentId, _ ->
            if (auth.currentUser == null || SavedAccountManager.isGuest(context)) {
                onRequireLogin()
            } else {
                val replyUsername = if (!parentCommentId.isNullOrBlank()) {
                    liveComments.firstOrNull { it.id == parentCommentId }?.username
                } else null
                commentVm.addComment(
                    videoId = targetId,
                    text = text,
                    replyToId = parentCommentId,
                    replyToUsername = replyUsername
                )
                onCommentCountChanged(1)
            }
        },
        onRetryComment = { commentId ->
            liveComments.firstOrNull { it.id == commentId }?.let {
                commentVm.retryComment(it)
            }
        },
        onEditComment = { commentId, replyId, newText ->
            if (auth.currentUser == null || SavedAccountManager.isGuest(context)) {
                onRequireLogin()
            } else {
                commentVm.editComment(replyId ?: commentId, newText)
            }
        },
        onToggleLike = { commentId, replyId ->
            if (auth.currentUser == null || SavedAccountManager.isGuest(context)) {
                onRequireLogin()
            } else {
                commentVm.toggleLike(replyId ?: commentId)
            }
        },
        onToggleCreatorHeart = { commentId, replyId ->
            liveComments.firstOrNull { it.id == (replyId ?: commentId) }?.let {
                commentVm.toggleCreatorHeart(it)
            }
        },
        onToggleReaction = { commentId, replyId, emoji ->
            if (auth.currentUser == null || SavedAccountManager.isGuest(context)) {
                onRequireLogin()
            } else {
                commentVm.toggleReaction(replyId ?: commentId, emoji)
            }
        },
        onOpenReplyFlashVideo = { url -> activeReplyFlashUrl = url },
        onTogglePin = { commentId ->
            liveComments.firstOrNull { it.id == commentId }?.let {
                commentVm.togglePin(it, targetId)
            }
        },
        onDeleteComment = { commentId, replyId ->
            val targetCommentId = replyId ?: commentId
            val childCount = if (replyId == null) {
                liveComments.count { it.replyToCommentId == commentId }
            } else 0
            liveComments.firstOrNull { it.id == targetCommentId }?.let {
                commentVm.deleteComment(it)
                onCommentCountChanged(-(1 + childCount))
            }
        },
        onVotePoll = { _, _ -> },
        onReportComment = { commentId, reason ->
            if (auth.currentUser == null || SavedAccountManager.isGuest(context)) {
                onRequireLogin()
            } else {
                commentVm.reportComment(commentId, targetId, reason)
            }
        }
    )

    activeReplyFlashUrl?.let { flashUrl ->
        ReplyFlashPreviewDialog(
            videoUrl = flashUrl,
            isDark = isDark,
            onDismiss = { activeReplyFlashUrl = null }
        )
    }
}

@Composable
private fun ReplyFlashPreviewDialog(
    videoUrl: String,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val bgColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF141418) else androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    val textColor = if (isDark) androidx.compose.ui.graphics.Color(0xFFFAFAFA) else androidx.compose.ui.graphics.Color(0xFF111114)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(bgColor)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                androidx.compose.material3.Text(
                    text = "Flash Reply",
                    color = textColor,
                    fontSize = androidx.compose.ui.unit.TextUnit(15f, androidx.compose.ui.unit.TextUnitType.Sp),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text("Close")
                }
            }
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.widget.VideoView(ctx).apply {
                        setVideoURI(android.net.Uri.parse(videoUrl))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                        }
                    }
                },
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 14f)
            )
        }
    }
}
