package com.chroma.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.Story
import com.chroma.studio.model.UserStoryGroup
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus

@Composable
fun StoriesSection(
    stories: List<UserStoryGroup>,
    isLoading: Boolean,
    onAddStoryClick: () -> Unit = {},
    isCurrentUser: Boolean = true,
    hasMyStory: Boolean = false,
    myStoryAvatarUrl: String = "",
    myStoryGender: String? = null,
    myStoriesList: List<Story> = emptyList(),
    isUploading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    onMyStoryClick: (() -> Unit)? = null,
    onStoryClick: ((groupIndex: Int) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val infiniteTransition = rememberInfiniteTransition(label = "story_skeleton")
    val skeletonAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // "Your story" / "My Story"
        if (isCurrentUser) {
            item(key = "my_story") {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(74.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color.Gray.copy(alpha = skeletonAlpha))
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.Gray.copy(alpha = skeletonAlpha))
                        )
                    }
                } else {
                    val myStoryInteractionSource = remember { MutableInteractionSource() }
                    val isMyStoryPressed by myStoryInteractionSource.collectIsPressedAsState()
                    val myStoryScale by animateFloatAsState(
                        targetValue = if (isMyStoryPressed) 0.92f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "my_story_scale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(74.dp)
                            .graphicsLayer {
                                scaleX = myStoryScale
                                scaleY = myStoryScale
                            }
                            .clickable(
                                interactionSource = myStoryInteractionSource,
                                indication = null
                            ) {
                                if (hasMyStory && onMyStoryClick != null) {
                                    onMyStoryClick()
                                } else {
                                    onAddStoryClick()
                                }
                            }
                    ) {
                        AppleStatusAvatar(
                            name = "You",
                            avatarUrl = myStoryAvatarUrl,
                            size = 68.dp,
                            avatarSize = 56.dp,
                            hasStatus = hasMyStory,
                            storyCount = myStoriesList.size.coerceAtLeast(1),
                            isSeen = false,
                            isUploading = isUploading,
                            showAddBadge = true,
                            gender = myStoryGender,
                            isDark = isDark,
                            gapColor = colors.bg,
                            onAddBadgeClick = { onAddStoryClick() }
                        )

                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = if (isUploading) "Uploading..." else "Your story",
                            color = if (isUploading) Color(0xFF007AFF) else if (hasMyStory) colors.textMain else colors.textMain.copy(alpha = 0.82f),
                            fontSize = 11.5.sp,
                            fontWeight = if (isUploading || hasMyStory) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Friends' / Active Users' Stories
        if (isLoading) {
            items(5) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(74.dp)) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = skeletonAlpha))
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.Gray.copy(alpha = skeletonAlpha))
                    )
                }
            }
        } else {
            items(stories.size, key = { stories[it].userProfile.uid.ifBlank { "story_group_$it" } }) { index ->
                val storyGroup = stories[index]
                val profile = storyGroup.userProfile
                val displayName = profile.displayName.ifBlank { "User" }
                val firstName = displayName.split(Regex("\\s+")).firstOrNull() ?: displayName
                val avatarUrl = profile.avatarUrl
                val isSeen = storyGroup.allSeen
                val isOnline = profile.online
                val hasStories = storyGroup.stories.isNotEmpty()

                val itemInteractionSource = remember { MutableInteractionSource() }
                val isItemPressed by itemInteractionSource.collectIsPressedAsState()
                val itemScale by animateFloatAsState(
                    targetValue = if (isItemPressed) 0.92f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "story_item_scale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(74.dp)
                        .graphicsLayer {
                            scaleX = itemScale
                            scaleY = itemScale
                        }
                        .clickable(
                            interactionSource = itemInteractionSource,
                            indication = null
                        ) { onStoryClick?.invoke(index) }
                ) {
                    AppleStatusAvatar(
                        name = displayName,
                        avatarUrl = avatarUrl,
                        size = 68.dp,
                        avatarSize = 56.dp,
                        hasStatus = hasStories,
                        storyCount = storyGroup.stories.size,
                        isSeen = isSeen,
                        isOnline = isOnline,
                        showAddBadge = false,
                        gender = profile.gender,
                        isDark = isDark,
                        gapColor = colors.bg
                    )

                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = firstName,
                        color = if (!isSeen && hasStories) colors.textMain else colors.textMain.copy(alpha = 0.62f),
                        fontSize = 11.5.sp,
                        fontWeight = if (!isSeen && hasStories) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
