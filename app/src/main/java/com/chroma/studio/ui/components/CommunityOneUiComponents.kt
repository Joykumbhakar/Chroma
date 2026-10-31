@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.chroma.studio.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.components.OneUiGradientStyle
import com.chroma.studio.model.Story
import com.chroma.studio.model.UserStoryGroup
import com.chroma.studio.ui.screens.CommunityFeedTab
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*

/**
 * Samsung One UI 7 Collapsible Header specifically crafted for the Community Feed.
 * Features:
 * - ChromaOneUiHeaderBackdrop mesh gradient
 * - Large 32sp title with dynamic subtitle based on current tab
 * - Compact top action bar (56dp) with crossfade
 * - Integrated + Create Popover, Notifications with unread badge, Refresh, and Slide-Out Search
 */
@Composable
fun CommunityOneUiHeader(
    title: String = "Community",
    subtitle: String,
    collapseProgress: Float,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearchExpand: (Boolean) -> Unit,
    isPlusMenuOpen: Boolean,
    onTogglePlusMenu: () -> Unit,
    hasUnreadNotifications: Boolean,
    onToggleNotificationDrawer: () -> Unit,
    isRefreshing: Boolean,
    refreshRotation: Float,
    onRefreshFeed: () -> Unit,
    createMenuContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences("chat_ui_preferences", Context.MODE_PRIVATE) }
    val gradientStyle = remember {
        val savedName = prefs.getString("one_ui_gradient_style", OneUiGradientStyle.GALAXY_AI.name)
        try {
            OneUiGradientStyle.valueOf(savedName ?: "GALAXY_AI")
        } catch (_: Exception) {
            OneUiGradientStyle.GALAXY_AI
        }
    }

    val screenBgColor = if (!isDark) Color(0xFFF8FAFC) else Color(0xFF000000)
    val textDark = if (!isDark) Color(0xFF0A0A0C) else Color(0xFFF2F2F7)
    val textMuted = if (!isDark) Color(0xFF71717A) else Color(0xFF8E8E93)
    val dividerColor = if (!isDark) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)

    val expandedHeightDp = 88.dp
    val currentExpandedHeightDp = expandedHeightDp * (1f - collapseProgress).coerceIn(0f, 1f)

    val largeTitleAlpha = (1f - (collapseProgress / 0.5f)).coerceIn(0f, 1f)
    val subtitleAlpha = (1f - (collapseProgress / 0.4f)).coerceIn(0f, 1f)
    val compactTitleAlpha = ((collapseProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)
    val gradientAlpha = if (gradientStyle == OneUiGradientStyle.SOLID) 0f else (1f - (collapseProgress / 0.85f)).coerceIn(0f, 1f)
    val toolbarBgAlpha = (collapseProgress * 0.94f).coerceIn(0f, 0.94f)

    Box(modifier = modifier.fillMaxWidth()) {
        // 1. Mesh Gradient Backdrop
        ChromaOneUiHeaderBackdrop(
            expandedHeightDp = expandedHeightDp + 56.dp,
            collapseProgress = collapseProgress,
            gradientAlpha = gradientAlpha,
            isDark = isDark,
            screenBgColor = screenBgColor
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // Status bar spacer
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(screenBgColor.copy(alpha = toolbarBgAlpha))
            )

            // 2. Compact Top Action Bar (56dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(screenBgColor.copy(alpha = toolbarBgAlpha))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Compact Title (Fades in on collapse)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textDark,
                        fontFamily = InterFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer {
                            alpha = compactTitleAlpha
                            translationY = (1f - compactTitleAlpha) * with(density) { 4.dp.toPx() }
                        }
                    )
                }

                // Right Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isSearchExpanded) {
                        // + Create Button
                        Box {
                            val plusRotation by animateFloatAsState(
                                targetValue = if (isPlusMenuOpen) 45f else 0f,
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                                label = "plusRotation"
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPlusMenuOpen) {
                                            if (!isDark) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
                                        } else Color.Transparent
                                    )
                                    .clickable(onClick = onTogglePlusMenu),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Plus,
                                    contentDescription = "Create Drop",
                                    tint = textDark,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(plusRotation)
                                )
                            }
                            createMenuContent()
                        }

                        // Notifications / Heart Button
                        Box(modifier = Modifier.size(36.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .clickable(onClick = onToggleNotificationDrawer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Heart,
                                    contentDescription = "Notifications",
                                    tint = textDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (hasUnreadNotifications) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-4).dp, y = 4.dp)
                                        .background(if (isDark) Color(0xFF16161A) else Color.White, CircleShape)
                                        .padding(1.5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE11D48))
                                )
                            }
                        }

                        // Refresh Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onRefreshFeed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.RotateCw,
                                contentDescription = "Refresh Feed",
                                tint = if (isRefreshing) Color(0xFF2563EB) else textDark,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(refreshRotation)
                            )
                        }
                    }

                    // Global Search Bar
                    ChromaGlobalExpandingSearchBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        isExpanded = isSearchExpanded,
                        onToggleExpand = onToggleSearchExpand,
                        placeholder = "Search drops, creators...",
                        maxExpandedWidth = 320.dp
                    )
                }
            }

            // 3. Expanded Viewing Area (Shrinks and fades out on scroll)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentExpandedHeightDp),
                contentAlignment = Alignment.Center
            ) {
                if (largeTitleAlpha > 0.01f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .graphicsLayer {
                                alpha = largeTitleAlpha
                                translationY = -collapseProgress * with(density) { 16.dp.toPx() }
                                val scale = 1f - (collapseProgress * 0.04f)
                                scaleX = scale
                                scaleY = scale
                            }
                    ) {

                        // Main title
                        Text(
                            text = title,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                            color = textDark,
                            fontFamily = InterFontFamily,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (subtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = subtitle,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = textMuted,
                                fontFamily = InterFontFamily,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
                            )
                        }
                    }
                }
            }

            // Bottom hairline divider when collapsed
            if (collapseProgress > 0.85f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(dividerColor)
                )
            }
        }
    }
}

/**
 * Modern Samsung One UI Category Filter Pills for Community Feed.
 * Prominently visible right below the Stories card.
 */
@Composable
fun CommunityCategoryPills(
    selectedTab: CommunityFeedTab,
    onSelectTab: (CommunityFeedTab) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF2563EB)
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current

    val cardBgColor = if (!isDark) Color(0xFFFFFFFF) else Color(0xFF17171A)
    val cardBorderColor = if (!isDark) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)
    val textMain = if (!isDark) Color(0xFF0A0A0C) else Color(0xFFF2F2F7)
    val textMuted = if (!isDark) Color(0xFF71717A) else Color(0xFF8E8E93)

    val tabs = listOf(
        CommunityFeedTab.ALL to "Latest",
        CommunityFeedTab.TRENDING to "Trending",
        CommunityFeedTab.FLASH to "Flash",
        CommunityFeedTab.FOLLOWING to "Following",
        CommunityFeedTab.SAVED to "Saved"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (tab, label) ->
            val isSelected = (selectedTab == tab)
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                label = "pillScale"
            )

            val pillShape = RoundedCornerShape(50)

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .height(36.dp)
                    .clip(pillShape)
                    .background(if (isSelected) activeColor else cardBgColor)
                    .then(
                        if (isSelected) {
                            Modifier.shadow(4.dp, pillShape, spotColor = activeColor.copy(alpha = 0.35f))
                        } else Modifier
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        SoundManager.playReaction()
                        onSelectTab(tab)
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else textMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else textMain,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

/**
 * Unified 26dp Squircle Card container for the Community Stories Section.
 * Matches the exact styling of the Messages Tab Stories Card.
 */
@Composable
fun CommunityStoriesCard(
    stories: List<UserStoryGroup>,
    isLoading: Boolean,
    hasMyStory: Boolean,
    myStoryAvatarUrl: String,
    myStoryGender: String?,
    myStoriesList: List<Story>,
    isUploading: Boolean,
    onMyStoryClick: () -> Unit,
    onAddStoryClick: () -> Unit,
    onStoryClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val cardBgColor = if (!isDark) Color(0xFFFFFFFF) else Color(0xFF17171A)
    val cardBorderColor = if (!isDark) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)
    val squircleShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .shadow(4.dp, squircleShape, spotColor = Color.Black.copy(alpha = 0.06f))
            .clip(squircleShape)
            .background(cardBgColor)
            .padding(vertical = 4.dp)
    ) {
        StoriesSection(
            stories = stories,
            isLoading = isLoading,
            hasMyStory = hasMyStory,
            myStoryAvatarUrl = myStoryAvatarUrl,
            myStoryGender = myStoryGender,
            myStoriesList = myStoriesList,
            isUploading = isUploading,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            gapColor = cardBgColor,
            onMyStoryClick = onMyStoryClick,
            onAddStoryClick = onAddStoryClick,
            onStoryClick = onStoryClick
        )
    }
}

/**
 * Interactive One UI Empty State for the Community Feed.
 * Features an inspiring card with Apple graphics, action button, and suggested styles.
 */
@Composable
fun CommunityInteractiveEmptyState(
    selectedTab: CommunityFeedTab,
    searchQuery: String,
    onCreatePostClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val photoState = rememberAppleGraphicAnimationState()

    val cardBgColor = if (!isDark) Color(0xFFFFFFFF) else Color(0xFF17171A)
    val cardBorderColor = if (!isDark) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)
    val textMain = if (!isDark) Color(0xFF0A0A0C) else Color(0xFFF2F2F7)
    val textMuted = if (!isDark) Color(0xFF71717A) else Color(0xFF8E8E93)
    val activeBlue = Color(0xFF2563EB)

    val (title, subtitle) = remember(selectedTab, searchQuery) {
        when {
            searchQuery.isNotBlank() -> "No Matching Drops" to "We couldn't find any creations matching \"$searchQuery\". Try exploring popular tags below."
            selectedTab == CommunityFeedTab.FOLLOWING -> "No Followed Creators" to "You aren't following anyone yet. Discover creative artists in the Latest or Trending feed!"
            selectedTab == CommunityFeedTab.SAVED -> "No Saved Creations" to "Bookmark inspiring drops with the save button to build your private collection."
            selectedTab == CommunityFeedTab.FLASH -> "No Flash Videos Yet" to "Be the first to publish a cinematic vertical video or remix to this community."
            selectedTab == CommunityFeedTab.TRENDING -> "No Trending Drops" to "Drops with active likes, comments, and remixes will be featured as Trending Masterworks."
            else -> "Community Feed is Quiet" to "Be the first to share your stunning color creations and inspire artists worldwide."
        }
    }

    val squircleShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(6.dp, squircleShape, spotColor = Color.Black.copy(alpha = 0.06f))
            .clip(squircleShape)
            .background(cardBgColor)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Apple Photos 3D Graphic Animation
            ApplePhotosGraphic(
                modifier = Modifier.size(92.dp),
                state = photoState,
                enableClickToAnimate = true
            )

            // Title and Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMain,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = textMuted,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Primary Action Button: Create Drop
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF2563EB), Color(0xFF3B82F6))
                        )
                    )
                    .clickable {
                        SoundManager.playReaction()
                        onCreatePostClick()
                    }
                    .padding(horizontal = 22.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Plus,
                        contentDescription = "Create",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Create New Drop",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily
                    )
                }
            }

            // Suggested Topics / Tags row
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "POPULAR STYLES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMuted.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )

                val sampleTags = listOf("AuroraMesh", "Glassmorphism", "Cyberpunk", "Cinematic", "ColorPalette")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    sampleTags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (!isDark) Color(0xFFF1F5F9)
                                    else Color(0xFF202024)
                                )
                                .border(0.8.dp, cardBorderColor, RoundedCornerShape(50))
                                .clickable {
                                    SoundManager.playReaction()
                                    onTopicClick(tag)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.5.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = activeBlue,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}
