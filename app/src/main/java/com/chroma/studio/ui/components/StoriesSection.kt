package com.chroma.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    onMyStoryClick: (() -> Unit)? = null,
    onStoryClick: ((groupIndex: Int) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val infiniteTransition = rememberInfiniteTransition(label = "story_skeleton")
    val skeletonAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // "Your story" / "My Story"
        if (isCurrentUser) {
            item(key = "my_story") {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(72.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Gray.copy(alpha = skeletonAlpha))
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Gray.copy(alpha = skeletonAlpha))
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
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
                            size = 64.dp,
                            avatarSize = 54.dp,
                            hasStatus = hasMyStory,
                            isSeen = false,
                            showAddBadge = !hasMyStory,
                            gender = myStoryGender,
                            isDark = isDark,
                            gapColor = if (isDark) Color(0xFF121212) else Color.White,
                            onAddBadgeClick = { onAddStoryClick() }
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your story",
                            color = colors.textMain,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = skeletonAlpha))
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Gray.copy(alpha = skeletonAlpha))
                    )
                }
            }
        } else {
            items(stories.size, key = { stories[it].userProfile.uid }) { index ->
                val storyGroup = stories[index]
                val profile = storyGroup.userProfile
                val displayName = profile.displayName.ifBlank { "User" }
                val firstName = displayName.split(Regex("\\s+")).firstOrNull() ?: displayName
                val avatarUrl = profile.avatarUrl
                val isSeen = storyGroup.allSeen
                val isOnline = profile.online
                val hasStories = storyGroup.stories.isNotEmpty()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onStoryClick?.invoke(index) }
                ) {
                    AppleStatusAvatar(
                        name = displayName,
                        avatarUrl = avatarUrl,
                        size = 64.dp,
                        avatarSize = 54.dp,
                        hasStatus = hasStories,
                        isSeen = isSeen,
                        isOnline = isOnline,
                        showAddBadge = false,
                        gender = profile.gender,
                        isDark = isDark,
                        gapColor = if (isDark) Color(0xFF121212) else Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = firstName,
                        color = colors.textMain,
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
