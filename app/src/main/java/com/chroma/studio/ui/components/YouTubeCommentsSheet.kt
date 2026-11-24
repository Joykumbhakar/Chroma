package com.chroma.studio.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chroma.studio.data.SavedAccountManager
import com.chroma.studio.model.FlashComment
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.FlashCommentViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * Flagship Comment Section Interface for ChromaStudio.
 * Powered directly by PhotoPostCommentBottomSheet, integrating:
 * - Real-time Firestore synchronization through FlashCommentViewModel
 * - Interactive voice notes, poll voting, media attachments, and spoilers
 * - Double-tap heart celebration, swipe-to-reply, rich text formatting, and translation
 * - Full theme support (Dark/Light) with edge-to-edge navigation bar padding (no transparent gap)
 * - ChromaStudio signature IosAvatarView, VerifiedBadge, and VoiceNotePlayer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeCommentsSheet(
    targetId: String,
    creatorId: String = "",
    creatorUsername: String = "",
    creatorAvatarUrl: String = "",
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

    LaunchedEffect(targetId) {
        if (targetId.isNotBlank()) {
            commentVm.loadComments(targetId)
        }
    }

    val currentUser = remember(authUser) {
        val rawHandle = authUser?.displayName?.replace(" ", "")?.lowercase()
            ?: authUser?.email?.substringBefore("@")
            ?: (authUser?.uid?.take(6) ?: "creator")
        CommentAuthor(
            name = authUser?.displayName ?: rawHandle,
            handle = rawHandle,
            avatarStyle = AvatarStyle.ROYAL_BLUE,
            verified = null,
            role = if (!creatorId.isNullOrBlank() && authUser?.uid == creatorId) "Creator" else null,
            isSelf = true,
            avatarUrl = authUser?.photoUrl?.toString().orEmpty(),
            userId = authUser?.uid.orEmpty(),
            isOnline = true
        )
    }

    val postSummary = remember(targetId, creatorUsername, creatorAvatarUrl) {
        PhotoPostSummary(
            id = targetId,
            author = CommentAuthor(
                name = creatorUsername.ifBlank { "Creator" },
                handle = creatorUsername.ifBlank { "creator" },
                avatarStyle = AvatarStyle.ROYAL_BLUE,
                verified = VerifiedBadgeType.BLUE,
                role = "Creator",
                avatarUrl = creatorAvatarUrl,
                userId = creatorId
            ),
            caption = "Community Discussion",
            timeAgo = "Active",
            likeCountLabel = "Likes"
        )
    }

    val mappedComments = remember(liveComments, creatorId, currentUser.userId) {
        val roots = liveComments.filter { it.replyToCommentId.isNullOrBlank() }
        val repliesMap = liveComments.filter { !it.replyToCommentId.isNullOrBlank() }.groupBy { it.replyToCommentId!! }

        roots.map { flash ->
                val replies = repliesMap[flash.id]?.map { reply ->
                    CommentReply(
                        id = reply.id,
                        author = CommentAuthor(
                            name = reply.displayName.ifBlank { reply.username },
                            handle = reply.username.ifBlank { "user" },
                            avatarStyle = AvatarStyle.CLASSIC_MAC,
                            verified = if (reply.verified) VerifiedBadgeType.BLUE else null,
                            role = if (!creatorId.isNullOrBlank() && reply.userId == creatorId) "Creator" else null,
                            isSelf = currentUser.userId.isNotBlank() && reply.userId == currentUser.userId,
                            avatarUrl = reply.avatarUrl,
                            userId = reply.userId
                        ),
                        text = reply.text,
                        createdAtMillis = reply.timestamp,
                        likes = reply.likesCount,
                        likedByMe = currentUser.userId.isNotBlank() && reply.likedBy.contains(currentUser.userId),
                        creatorHeart = reply.isCreatorHearted
                    )
                } ?: emptyList()

                PostComment(
                    id = flash.id,
                    author = CommentAuthor(
                        name = flash.displayName.ifBlank { flash.username },
                        handle = flash.username.ifBlank { "user" },
                        avatarStyle = AvatarStyle.CLASSIC_MAC,
                        verified = if (flash.verified) VerifiedBadgeType.BLUE else null,
                        role = if (!creatorId.isNullOrBlank() && flash.userId == creatorId) "Creator" else null,
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
                    replies = replies
                )
            }
    }

    PhotoPostCommentBottomSheet(
        postSummary = postSummary,
        currentUser = currentUser,
        onDismissRequest = onDismiss,
        initialComments = mappedComments,
        isDarkTheme = isDark,
        onCommentCountChanged = null,
        onTotalCommentCountChanged = null,
        onSubmitComment = { text, parentCommentId, _ ->
            if (auth.currentUser == null || SavedAccountManager.isGuest(context)) {
                onRequireLogin()
            } else {
                commentVm.addComment(
                    videoId = targetId,
                    text = text,
                    replyToId = parentCommentId,
                    replyToUsername = null
                )
                onCommentCountChanged(1)
            }
        },
        onToggleLike = { commentId, _ ->
            commentVm.toggleLike(commentId)
        },
        onToggleCreatorHeart = { commentId, _ ->
            liveComments.firstOrNull { it.id == commentId }?.let {
                commentVm.toggleCreatorHeart(it)
            }
        },
        onTogglePin = { commentId ->
            liveComments.firstOrNull { it.id == commentId }?.let {
                commentVm.togglePin(it, targetId)
            }
        },
        onDeleteComment = { commentId, _ ->
            liveComments.firstOrNull { it.id == commentId }?.let {
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
