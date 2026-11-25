package com.chroma.studio.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
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
                verified = if (creatorVerified) VerifiedBadgeType.BLUE else VerifiedBadgeType.NONE,
                verifiedTone = creatorVerifiedTone,
                role = "Creator",
                avatarUrl = creatorAvatarUrl,
                userId = creatorId
            ),
            authorHandle = cleanCreatorHandle,
            authorVerified = if (creatorVerified) VerifiedBadgeType.BLUE else VerifiedBadgeType.NONE,
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

        roots.map { flash ->
            val replies = repliesMap[flash.id]?.map { reply ->
                CommentReply(
                    id = reply.id,
                    author = CommentAuthor(
                        name = reply.displayName.ifBlank { reply.username },
                        handle = reply.username.ifBlank { "user" },
                        avatarStyle = AvatarStyle.CLASSIC_MAC,
                        verified = if (reply.verified) VerifiedBadgeType.BLUE else VerifiedBadgeType.NONE,
                        verifiedTone = reply.verifiedTone,
                        role = if (creatorId.isNotBlank() && reply.userId == creatorId) "Creator" else null,
                        isSelf = currentUser.userId.isNotBlank() && reply.userId == currentUser.userId,
                        avatarUrl = reply.avatarUrl,
                        userId = reply.userId
                    ),
                    text = reply.text,
                    createdAtMillis = reply.timestamp,
                    likes = reply.likesCount,
                    likedByMe = currentUser.userId.isNotBlank() && reply.likedBy.contains(currentUser.userId),
                    creatorHeart = reply.isCreatorHearted,
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
                    verified = if (flash.verified) VerifiedBadgeType.BLUE else VerifiedBadgeType.NONE,
                    verifiedTone = flash.verifiedTone,
                    role = if (creatorId.isNotBlank() && flash.userId == creatorId) "Creator" else null,
                    isSelf = currentUser.userId.isNotBlank() && flash.userId == currentUser.userId,
                    avatarUrl = flash.avatarUrl,
                    userId = flash.userId
                ),
                text = flash.text,
                createdAtMillis = flash.timestamp,
                pinned = flash.isPinned,
                likes = flash.likesCount,
                likedByMe = currentUser.userId.isNotBlank() && flash.likedBy.contains(currentUser.userId),
                creatorHeart = flash.isCreatorHearted,
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
        onTogglePin = { commentId ->
            liveComments.firstOrNull { it.id == commentId }?.let {
                commentVm.togglePin(it, targetId)
            }
        },
        onDeleteComment = { commentId, replyId ->
            val targetCommentId = replyId ?: commentId
            liveComments.firstOrNull { it.id == targetCommentId }?.let {
                commentVm.deleteComment(it)
                onCommentCountChanged(-1)
            }
        },
        onVotePoll = { _, _ -> },
        onReportComment = { _, reason ->
            ToastManager.showToast("Report submitted: $reason")
        }
    )
}
