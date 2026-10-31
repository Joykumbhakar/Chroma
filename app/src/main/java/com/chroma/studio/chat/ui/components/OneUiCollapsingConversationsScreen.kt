package com.chroma.studio.chat.ui.components

import com.chroma.studio.ui.components.ChromaGlobalExpandingSearchBar
import com.chroma.studio.ui.components.StoriesSection

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Users
import com.composables.icons.lucide.MessageSquare
import com.composables.icons.lucide.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.chroma.studio.chat.data.ChatConversation
import com.chroma.studio.chat.data.Story
import com.chroma.studio.chat.data.User
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.components.ChromaOneUiHeaderBackdrop
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.model.UserStoryGroup
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.components.LiquidGlassCheckbox
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ============================================================================
// SAMSUNG ONE UI COLLAPSING HEADER ENGINE WITH EXPANDED MESH GRADIENT
// ============================================================================

enum class OneUiGradientStyle(
    val label: String,
    val badgeText: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val orbColor: Color
) {
    GALAXY_AI(
        label = "Galaxy Ice",
        badgeText = "One UI 7.0 Pure Ice Blue",
        primaryColor = Color(0xFF2563EB),    // Samsung Royal Blue
        secondaryColor = Color(0xFF60A5FA),  // Crisp Clean Sky Blue
        orbColor = Color(0xFFBAE6FD)        // Pure Ice White-Blue
    ),
    BORA_PURPLE(
        label = "Ocean Sky",
        badgeText = "One UI 7.0 Ocean Sky",
        primaryColor = Color(0xFF0284C7),    // Clean Cerulean Blue
        secondaryColor = Color(0xFF38BDF8),  // Light Sky Blue
        orbColor = Color(0xFFE0F2FE)        // Crisp Ice White
    ),
    EMERALD(
        label = "Nordic Frost",
        badgeText = "One UI 7.0 Nordic Frost",
        primaryColor = Color(0xFF0EA5E9),    // Cyan Sky Blue
        secondaryColor = Color(0xFF7DD3FC),  // Frost Blue
        orbColor = Color(0xFFF0F9FF)        // Glacier White
    ),
    SUNSET(
        label = "Royal Blue",
        badgeText = "One UI 7.0 Royal Sapphire",
        primaryColor = Color(0xFF1D4ED8),    // Deep Royal Sapphire
        secondaryColor = Color(0xFF3B82F6),  // True Blue
        orbColor = Color(0xFFDBEAFE)        // Soft White Blue
    ),
    SOLID(
        label = "Solid",
        badgeText = "One UI Classic Solid",
        primaryColor = Color.Transparent,
        secondaryColor = Color.Transparent,
        orbColor = Color.Transparent
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OneUiCollapsingConversationsScreen(
    title: String,
    currentUser: User,
    stories: List<Story> = emptyList(),
    storyGroups: List<UserStoryGroup> = emptyList(),
    hasMyStory: Boolean = false,
    chats: List<ChatConversation>,
    activeTab: String,
    onTabSelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    gradientStyle: OneUiGradientStyle,
    onCycleGradientStyle: () -> Unit,
    onSwitchToClassicLayout: () -> Unit,
    onMyStoryClick: () -> Unit,
    onAddStoryClick: () -> Unit,
    onStoryClick: (Story) -> Unit,
    onStoryGroupClick: ((Int) -> Unit)? = null,
    onChatSelected: (String) -> Unit,
    onNewMessageClick: () -> Unit,
    onCreateGroupClick: (() -> Unit)? = null,
    onOpenAppearanceSettings: () -> Unit,
    onUserProfileClick: () -> Unit,
    onStartCall: (ChatConversation, String) -> Unit,
    lockedChatIds: Set<String>,
    pinnedChatIds: Set<String>,
    onPinChatToggle: (ChatConversation) -> Unit,
    onMuteChatToggle: (ChatConversation) -> Unit,
    onReadChatToggle: (ChatConversation) -> Unit,
    onLockChatToggle: (ChatConversation) -> Unit,
    onLockMultipleChatsToggle: ((List<String>) -> Unit)? = null,
    onArchiveChat: (ChatConversation) -> Unit,
    onClearChatHistory: (ChatConversation) -> Unit,
    onExportChat: (ChatConversation) -> Unit,
    onDeleteChat: (ChatConversation) -> Unit
) {
    val colors = LocalChatColors.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Multi-Selection Mode States
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedChatIds = remember { mutableStateListOf<String>() }
    var chatPendingDelete by remember { mutableStateOf<ChatConversation?>(null) }
    var showDeleteSelectedChatsDialog by remember { mutableStateOf(false) }
    var showBiometricLockConfirmationDialog by remember { mutableStateOf(false) }
    var selectedChatForActions by remember { mutableStateOf<ChatConversation?>(null) }

    BackHandler(enabled = isSelectionMode) {
        SoundManager.playReaction()
        isSelectionMode = false
        selectedChatIds.clear()
    }

    // Automatically hide main bottom navigation bar when selection mode or any bottom sheet/option appears
    val hasBottomOptionsOrModals = isSelectionMode ||
        selectedChatIds.isNotEmpty() ||
        selectedChatForActions != null ||
        showDeleteSelectedChatsDialog ||
        showBiometricLockConfirmationDialog

    LaunchedEffect(hasBottomOptionsOrModals) {
        com.chroma.studio.ChatStateManager.setBottomOptionsVisible(hasBottomOptionsOrModals)
    }

    DisposableEffect(Unit) {
        onDispose {
            com.chroma.studio.ChatStateManager.setBottomOptionsVisible(false)
        }
    }

    // One UI Expanded Viewing Area (~38% proportion)
    val expandedHeightDp: Dp = 190.dp
    val maxCollapsePx = remember(density) { with(density) { expandedHeightDp.toPx() } }
    var collapseOffsetPx by remember { mutableFloatStateOf(0f) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    var isFabExpanded by remember { mutableStateOf(false) }
    var lastIndex by remember { mutableIntStateOf(0) }
    var lastOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (currentIndex, currentOffset) ->
                if (listState.isScrollInProgress) {
                    val isScrollingUp = when {
                        currentIndex < lastIndex -> true
                        currentIndex > lastIndex -> false
                        else -> currentOffset < lastOffset
                    }
                    isFabExpanded = isScrollingUp
                }
                lastIndex = currentIndex
                lastOffset = currentOffset
            }
    }

    // 0.0f = Expanded, 1.0f = Collapsed
    val collapseProgress = remember(collapseOffsetPx, maxCollapsePx) {
        if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx).coerceIn(0f, 1f)
    }

    // Authentic Samsung One UI Two-Stage Title Crossfade + Expanded Gradient Alpha
    val largeTitleAlpha = (1f - (collapseProgress / 0.45f)).coerceIn(0f, 1f)
    val subtitleAlpha = (1f - (collapseProgress / 0.35f)).coerceIn(0f, 1f)
    val badgeAlpha = (1f - (collapseProgress / 0.30f)).coerceIn(0f, 1f)
    val compactTitleAlpha = ((collapseProgress - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val gradientAlpha = if (gradientStyle == OneUiGradientStyle.SOLID) {
        0f
    } else {
        (1f - (collapseProgress / 0.85f)).coerceIn(0f, 1f)
    }

    fun animateSnapTo(targetFraction: Float) {
        val targetPx = (targetFraction.coerceIn(0f, 1f)) * maxCollapsePx
        coroutineScope.launch {
            animate(
                initialValue = collapseOffsetPx,
                targetValue = targetPx,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { value, _ ->
                collapseOffsetPx = value
            }
        }
    }

    // NestedScrollConnection with One UI Magnetic Auto-Snap Physics
    val nestedScrollConnection = remember(maxCollapsePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                if (deltaY < -8f) {
                    isFabExpanded = false
                } else if (deltaY > 8f) {
                    isFabExpanded = true
                }
                // Scrolling UP (finger moves up -> deltaY < 0): collapse header first
                if (deltaY < 0f && collapseOffsetPx < maxCollapsePx) {
                    val newOffset = (collapseOffsetPx - deltaY).coerceIn(0f, maxCollapsePx)
                    val consumedY = -(newOffset - collapseOffsetPx)
                    collapseOffsetPx = newOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val deltaY = available.y
                if (deltaY < -8f) {
                    isFabExpanded = false
                } else if (deltaY > 8f) {
                    isFabExpanded = true
                }
                // Scrolling DOWN at top of list (deltaY > 0): expand header smoothly
                if (deltaY > 0f && collapseOffsetPx > 0f) {
                    val newOffset = (collapseOffsetPx - deltaY).coerceIn(0f, maxCollapsePx)
                    val consumedY = -(newOffset - collapseOffsetPx)
                    collapseOffsetPx = newOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val progress = if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx)
                if (progress > 0.02f && progress < 0.98f) {
                    val targetPx = when {
                        available.y < -350f -> maxCollapsePx
                        available.y > 350f -> 0f
                        progress >= 0.5f -> maxCollapsePx
                        else -> 0f
                    }
                    animate(
                        initialValue = collapseOffsetPx,
                        targetValue = targetPx,
                        animationSpec = spring(
                            dampingRatio = 0.86f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { value, _ ->
                        collapseOffsetPx = value
                    }
                }
                return Velocity.Zero
            }
        }
    }

    val screenBgColor = if (colors.isLight) Color(0xFFF8FAFC) else Color(0xFF000000)
    val cardBgColor = if (colors.isLight) Color(0xFFFFFFFF) else Color(0xFF17171A)
    val cardBorderColor = if (colors.isLight) Color.Transparent else Color.White.copy(alpha = 0.06f)
    val dividerColor = if (colors.isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.06f)

    val totalUnreadMessages = remember(chats) { chats.sumOf { it.unreadCount } }
    val filteredChats = remember(chats, activeTab, searchQuery, pinnedChatIds, lockedChatIds) {
        val base = chats.filter { chat ->
            val matchesSearch = searchQuery.isBlank() ||
                chat.name.contains(searchQuery, ignoreCase = true) ||
                chat.messages.lastOrNull()?.text?.contains(searchQuery, ignoreCase = true) == true
            val matchesTab = when (activeTab.lowercase()) {
                "unread" -> chat.unreadCount > 0
                "groups", "group" -> chat.type == "group"
                "direct" -> chat.type == "direct"
                "pinned", "favorites" -> chat.id in pinnedChatIds
                "locked" -> chat.id in lockedChatIds
                else -> true
            }
            matchesSearch && matchesTab
        }
        base.sortedByDescending { it.id in pinnedChatIds }
    }

    val currentExpandedHeightDp = with(density) {
        (maxCollapsePx - collapseOffsetPx).coerceAtLeast(0f).toDp()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
            .nestedScroll(nestedScrollConnection)
    ) {
        // 1. Exact Chroma Studio System Gradient Backdrop
        ChromaOneUiHeaderBackdrop(
            expandedHeightDp = expandedHeightDp,
            collapseProgress = collapseProgress,
            gradientAlpha = gradientAlpha,
            isDark = !colors.isLight,
            screenBgColor = screenBgColor
        )

        Column(modifier = Modifier.fillMaxSize()) {
            val toolbarBgAlpha = (collapseProgress * 0.94f).coerceIn(0f, 0.94f)

            // Top Status Bar Spacer that blends smoothly with the toolbar background
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(screenBgColor.copy(alpha = toolbarBgAlpha))
            )

            // 2. EXPANDED VIEWING AREA (Collapses smoothly on scroll: 190dp -> 0dp)
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
                                translationY = -collapseProgress * with(density) { 20.dp.toPx() }
                                val scale = 1f - (collapseProgress * 0.05f)
                                scaleX = scale
                                scaleY = scale
                            }
                    ) {


                        // Large One UI Expanded Title
                        Text(
                            text = title,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-0.5).sp,
                            color = colors.textDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Dynamic Unread & Conversations Subtitle
                        Text(
                            text = if (totalUnreadMessages > 0) {
                                "$totalUnreadMessages unread message${if (totalUnreadMessages > 1) "s" else ""} • ${chats.size} conversations"
                            } else {
                                "All caught up • ${chats.size} conversations"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
                        )
                    }
                }
            }

            // 3. STICKY COMPACT ONE UI TOOLBAR (Always pinned below Expanded Area)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(screenBgColor.copy(alpha = toolbarBgAlpha))
            ) {
                if (isSelectionMode) {
                    // One UI Multi-Selection Action Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        SoundManager.playReaction()
                                        isSelectionMode = false
                                        selectedChatIds.clear()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                LucideCloseX(tint = colors.textDark, size = 18.dp)
                            }

                            Text(
                                text = "${selectedChatIds.size} Selected",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textDark
                            )
                        }

                        val allSelected = filteredChats.isNotEmpty() && selectedChatIds.size >= filteredChats.size
                        Text(
                            text = if (allSelected) "Deselect All" else "Select All",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (gradientStyle == OneUiGradientStyle.SOLID) colors.iosBlue else gradientStyle.primaryColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    SoundManager.playReaction()
                                    if (allSelected) {
                                        selectedChatIds.clear()
                                    } else {
                                        selectedChatIds.clear()
                                        selectedChatIds.addAll(filteredChats.map { it.id })
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Compact Title (Fades in on collapse, no back arrow / chevron for root tab)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.graphicsLayer {
                                    alpha = compactTitleAlpha
                                    translationY = (1f - compactTitleAlpha) * with(density) { 4.dp.toPx() }
                                }
                            )
                        }

                        // Right Action Icons: Gradient Swatch, Edit, Search, Appearance, Profile Avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {


                            // Edit Button (Direct Selection Mode trigger)
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (colors.isLight) Color.White
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .clickable {
                                        SoundManager.playReaction()
                                        isSelectionMode = true
                                    }
                                    .padding(horizontal = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Edit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (gradientStyle == OneUiGradientStyle.SOLID) colors.iosBlue else gradientStyle.primaryColor
                                )
                            }

                        // Global Slide-Out Search Bar (Rounded corners, no border when inactive)
                        ChromaGlobalExpandingSearchBar(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChanged,
                            isExpanded = isSearchExpanded,
                            onToggleExpand = {
                                isSearchExpanded = it
                                if (!it) onSearchQueryChanged("")
                            },
                            placeholder = "Search messages...",
                            maxExpandedWidth = 220.dp
                        )

                        // Appearance Settings Button
                        IconButton(
                            onClick = {
                                SoundManager.playReaction()
                                onOpenAppearanceSettings()
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Canvas(modifier = Modifier.size(18.dp)) {
                                drawCircle(
                                    color = colors.textDark,
                                    radius = size.minDimension * 0.42f,
                                    style = Stroke(width = 1.9.dp.toPx())
                                )
                                drawCircle(
                                    color = colors.iosBlue,
                                    radius = size.minDimension * 0.16f
                                )
                            }
                        }

                        // User Profile Avatar Button (Tap: Profile, Long-press: Switch back to classic layout)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.iosBlue.copy(alpha = 0.18f))
                                .border(1.dp, colors.iosBlue.copy(alpha = 0.4f), CircleShape)
                                .combinedClickable(
                                    onClick = { onUserProfileClick() },
                                    onLongClick = {
                                        SoundManager.playReaction()
                                        onSwitchToClassicLayout()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentUser.avatar.isNotBlank()) {
                                AsyncImage(
                                    model = currentUser.avatar,
                                    contentDescription = currentUser.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = currentUser.name.take(2).uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.iosBlue
                                )
                            }
                        }
                    }
                }



                // Bottom Hairline Divider when Collapsed (> 92%)
                if (collapseProgress > 0.85f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.6.dp)
                            .background(dividerColor)
                    )
                }
            }

            // 4. SCROLLABLE INTERACTION ZONE (Stories + Filter Chips + 26.dp One UI Squircle Cards)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 6.dp,
                    bottom = 135.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stories Strip Card (One UI 26.dp Rounded Squircle)
                // Unified High-Fidelity Stories Section (Shared with Community Tab)
                item(key = "oneui_stories_card") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(cardBgColor)
                            .border(1.dp, cardBorderColor, RoundedCornerShape(26.dp))
                            .padding(vertical = 4.dp)
                    ) {
                        StoriesSection(
                            stories = storyGroups,
                            isLoading = false,
                            onAddStoryClick = onAddStoryClick,
                            hasMyStory = hasMyStory,
                            myStoryAvatarUrl = currentUser.avatar ?: "",
                            myStoryGender = null,
                            myStoriesList = emptyList(),
                            isUploading = false,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            gapColor = cardBgColor,
                            onMyStoryClick = onMyStoryClick,
                            onStoryClick = { groupIdx ->
                                onStoryGroupClick?.invoke(groupIdx)
                            }
                        )
                    }
                }

                item(key = "oneui_filter_tabs") {
                    val tabs = listOf(
                        "all" to "All",
                        "unread" to if (totalUnreadMessages > 0) "Unread ($totalUnreadMessages)" else "Unread",
                        "groups" to "Groups",
                        "pinned" to "Favorites",
                        "locked" to "Locked"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(tabs, key = { it.first }) { (tabKey, tabLabel) ->
                            val isSelected = activeTab.equals(tabKey, ignoreCase = true)
                            val activeChipColor = if (gradientStyle == OneUiGradientStyle.SOLID) {
                                colors.iosBlue
                            } else {
                                gradientStyle.primaryColor
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) activeChipColor else cardBgColor)
                                                                        .clickable {
                                        SoundManager.playReaction()
                                        onTabSelected(tabKey)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabLabel,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else colors.textMuted
                                )
                            }
                        }
                    }
                }

                // Grouped 26.dp One UI Squircle Conversations Container
                if (filteredChats.isEmpty()) {
                    item(key = "oneui_empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .background(cardBgColor)
                                .border(1.dp, cardBorderColor, RoundedCornerShape(26.dp))
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No conversations found",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap the New message button below to start chatting",
                                    fontSize = 13.sp,
                                    color = colors.textMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    item(key = "oneui_conversations_group_card") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .background(cardBgColor)
                                .border(1.dp, cardBorderColor, RoundedCornerShape(26.dp))
                        ) {
                            filteredChats.forEachIndexed { idx, chat ->
                                val isPinned = chat.id in pinnedChatIds
                                val isLocked = chat.id in lockedChatIds
                                val isSelected = chat.id in selectedChatIds
                                val lastMsg = chat.messages.lastOrNull()
                                val snippet = when {
                                    isLocked -> "🔒 Locked conversation"
                                    chat.statusText.contains("typing", ignoreCase = true) ||
                                        chat.statusText.contains("recording", ignoreCase = true) -> chat.statusText
                                    lastMsg != null -> lastMsg.text?.takeIf { it.isNotBlank() }
                                        ?: lastMsg.fileName
                                        ?: "Media attachment"
                                    chat.bio.isNotBlank() -> chat.bio
                                    else -> "Tap to start chatting"
                                }
                                val timeLabel = lastMsg?.time ?: ""

                                OneUiSwipeableConversationItem(
                                    chat = chat,
                                    snippet = snippet,
                                    timeLabel = timeLabel,
                                    gradientStyle = gradientStyle,
                                    cardBgColor = cardBgColor,
                                    dividerColor = dividerColor,
                                    isPinned = isPinned,
                                    isLocked = isLocked,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    showDivider = idx < filteredChats.lastIndex,
                                    onToggleSelect = {
                                        if (isSelected) {
                                            selectedChatIds.remove(chat.id)
                                        } else {
                                            selectedChatIds.add(chat.id)
                                        }
                                    },
                                    onClick = {
                                        if (isSelectionMode) {
                                            if (isSelected) {
                                                selectedChatIds.remove(chat.id)
                                            } else {
                                                selectedChatIds.add(chat.id)
                                            }
                                        } else {
                                            onChatSelected(chat.id)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        SoundManager.playReaction()
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                            if (chat.id !in selectedChatIds) {
                                                selectedChatIds.add(chat.id)
                                            }
                                        }
                                    },
                                    onPinToggle = { onPinChatToggle(chat) },
                                    onMuteToggle = { onMuteChatToggle(chat) },
                                    onDelete = { chatPendingDelete = chat }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // 5. ONE UI FLOATING ACTION BUTTON (Freely Draggable Left to Right, Shrinks on collapse)
        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences("chat_ui_preferences", Context.MODE_PRIVATE) }
        val configuration = LocalConfiguration.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val marginPx = with(density) { 20.dp.toPx() }
        var fabWidthPx by remember { mutableFloatStateOf(0f) }
        val maxLeftOffset = remember(screenWidthPx, fabWidthPx, marginPx) {
            if (fabWidthPx > 0f) -(screenWidthPx - fabWidthPx - (marginPx * 2)).coerceAtLeast(0f) else 0f
        }
        val savedFabRatio = remember { prefs.getFloat("one_ui_fab_x_ratio", 1f) }
        val fabOffsetX = remember { Animatable(0f) }
        var isDraggingFab by remember { mutableStateOf(false) }

        LaunchedEffect(maxLeftOffset) {
            if (maxLeftOffset < 0f) {
                val targetOffset = maxLeftOffset * (1f - savedFabRatio)
                fabOffsetX.snapTo(targetOffset)
            }
        }

        AnimatedVisibility(
            visible = !isSelectionMode,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            val fabColor = if (gradientStyle == OneUiGradientStyle.SOLID) {
                colors.iosBlue
            } else {
                gradientStyle.primaryColor
            }
            val fabElevation by animateDpAsState(
                targetValue = if (isDraggingFab) 18.dp else 10.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "fabElevation"
            )
            val fabScale by animateFloatAsState(
                targetValue = if (isDraggingFab) 1.08f else 1f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                label = "fabScale"
            )

            // Specular shiny glass border (135 degree diagonal specular highlights from top-left white highlight to bottom-right sky rim glow)
            val shinyGlassBorder = remember(fabColor) {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.30f),
                        Color(0xFF93C5FD).copy(alpha = 0.40f),
                        Color(0xFF60A5FA).copy(alpha = 0.85f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            }

            val fabBackground = remember(fabColor) {
                Brush.linearGradient(
                    colors = listOf(
                        fabColor,
                        fabColor.copy(alpha = 0.90f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            }

            val targetFabWidth = if (isFabExpanded) 216.dp else 56.dp
            val fabWidth by animateDpAsState(
                targetValue = targetFabWidth,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "fabWidth"
            )
            val fabCorner = 28.dp
            val fabShape = RoundedCornerShape(fabCorner)

            Box(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(end = 20.dp, bottom = 114.dp)
                    .offset { IntOffset(x = fabOffsetX.value.roundToInt(), y = 0) }
                    .graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                    }
                    .onGloballyPositioned { coordinates ->
                        fabWidthPx = coordinates.size.width.toFloat()
                    }
                    .width(fabWidth)
                    .height(56.dp)
                    .shadow(fabElevation, fabShape, spotColor = fabColor.copy(alpha = 0.5f))
                    .clip(fabShape)
                    .background(fabBackground)
                    .border(1.2.dp, shinyGlassBorder, fabShape)
                    .then(
                        if (!isFabExpanded) {
                            Modifier.pointerInput(maxLeftOffset) {
                                awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalDrag = 0f
                            var hasDragged = false
                            val touchSlop = viewConfiguration.touchSlop

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    if (!hasDragged && !isFabExpanded) {
                                        SoundManager.playReaction()
                                        onNewMessageClick()
                                    } else if (hasDragged) {
                                        isDraggingFab = false
                                        coroutineScope.launch {
                                            val current = fabOffsetX.value
                                            val range = (0f - maxLeftOffset).coerceAtLeast(1f)
                                            val ratio = ((current - maxLeftOffset) / range).coerceIn(0f, 1f)
                                            val target = when {
                                                ratio < 0.15f -> maxLeftOffset
                                                ratio > 0.85f -> 0f
                                                else -> current
                                            }
                                            fabOffsetX.animateTo(
                                                target,
                                                spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                                            )
                                            val finalRatio = ((target - maxLeftOffset) / range).coerceIn(0f, 1f)
                                            prefs.edit().putFloat("one_ui_fab_x_ratio", finalRatio).apply()
                                        }
                                    }
                                    break
                                }

                                val dragAmount = change.positionChange().x
                                totalDrag += kotlin.math.abs(dragAmount)
                                if (totalDrag > touchSlop) {
                                    if (!hasDragged) {
                                        hasDragged = true
                                        isDraggingFab = true
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    change.consume()
                                    coroutineScope.launch {
                                        val newOffset = (fabOffsetX.value + dragAmount).coerceIn(maxLeftOffset, 0f)
                                        fabOffsetX.snapTo(newOffset)
                                    }
                                }
                            }
                                }
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isFabExpanded,
                    transitionSpec = {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                    },
                    label = "fabContent"
                ) { expanded ->
                    if (!expanded) {
                        Icon(
                            imageVector = Lucide.Plus,
                            contentDescription = "New chat",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Create Group
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        SoundManager.playReaction()
                                        onCreateGroupClick?.invoke() ?: onNewMessageClick()
                                    }
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Users,
                                    contentDescription = "Create Group",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Group",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(Color.White.copy(alpha = 0.35f))
                            )

                            // Direct Chat
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        SoundManager.playReaction()
                                        onNewMessageClick()
                                    }
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.MessageSquare,
                                    contentDescription = "Direct Chat",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Direct Chat",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. ONE UI MULTI-SELECTION BOTTOM ACTION MENU BAR
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(99f)
        ) {
            val hasSelection = selectedChatIds.isNotEmpty()
            val allSelectedRead = hasSelection && selectedChatIds.all { id ->
                (chats.find { it.id == id }?.unreadCount ?: 0) == 0
            }
            val allSelectedMuted = hasSelection && selectedChatIds.all { id ->
                chats.find { it.id == id }?.isMuted == true
            }
            val allSelectedPinned = hasSelection && selectedChatIds.all { id ->
                id in pinnedChatIds
            }
            val allSelectedLocked = hasSelection && selectedChatIds.all { id ->
                id in lockedChatIds
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .shadow(16.dp, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .background(cardBgColor)
                    .border(1.dp, cardBorderColor, RoundedCornerShape(26.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Read / Unread
                    OneUiSelectionActionButton(
                        icon = {
                            LucideCheckCheck(
                                tint = if (hasSelection) (gradientStyle.primaryColor.takeIf { gradientStyle != OneUiGradientStyle.SOLID } ?: colors.iosBlue)
                                else colors.textMuted.copy(alpha = 0.38f),
                                size = 20.dp
                            )
                        },
                        label = if (allSelectedRead) "Unread" else "Read",
                        tint = gradientStyle.primaryColor.takeIf { gradientStyle != OneUiGradientStyle.SOLID } ?: colors.iosBlue,
                        enabled = hasSelection,
                        onClick = {
                            val toProcess = selectedChatIds.toList()
                            toProcess.forEach { id ->
                                chats.find { it.id == id }?.let { onReadChatToggle(it) }
                            }
                            isSelectionMode = false
                            selectedChatIds.clear()
                        }
                    )

                    // 2. Mute / Unmute
                    OneUiSelectionActionButton(
                        icon = {
                            if (allSelectedMuted) {
                                LucideBell(
                                    tint = if (hasSelection) (gradientStyle.primaryColor.takeIf { gradientStyle != OneUiGradientStyle.SOLID } ?: colors.iosBlue)
                                    else colors.textMuted.copy(alpha = 0.38f),
                                    size = 20.dp
                                )
                            } else {
                                LucideBellOff(
                                    tint = if (hasSelection) (gradientStyle.primaryColor.takeIf { gradientStyle != OneUiGradientStyle.SOLID } ?: colors.iosBlue)
                                    else colors.textMuted.copy(alpha = 0.38f),
                                    size = 20.dp
                                )
                            }
                        },
                        label = if (allSelectedMuted) "Unmute" else "Mute",
                        tint = gradientStyle.primaryColor.takeIf { gradientStyle != OneUiGradientStyle.SOLID } ?: colors.iosBlue,
                        enabled = hasSelection,
                        onClick = {
                            val toProcess = selectedChatIds.toList()
                            toProcess.forEach { id ->
                                chats.find { it.id == id }?.let { onMuteChatToggle(it) }
                            }
                            isSelectionMode = false
                            selectedChatIds.clear()
                        }
                    )

                    // 3. Pin / Unpin
                    OneUiSelectionActionButton(
                        icon = {
                            LucideStar(
                                tint = if (hasSelection) Color(0xFFF59E0B)
                                else colors.textMuted.copy(alpha = 0.38f),
                                size = 20.dp
                            )
                        },
                        label = if (allSelectedPinned) "Unpin" else "Pin",
                        tint = Color(0xFFF59E0B),
                        enabled = hasSelection,
                        onClick = {
                            val toProcess = selectedChatIds.toList()
                            toProcess.forEach { id ->
                                chats.find { it.id == id }?.let { onPinChatToggle(it) }
                            }
                            isSelectionMode = false
                            selectedChatIds.clear()
                        }
                    )

                    // 4. Lock / Unlock
                    OneUiSelectionActionButton(
                        icon = {
                            LucideLock(
                                tint = if (hasSelection) (gradientStyle.primaryColor.takeIf { gradientStyle != OneUiGradientStyle.SOLID } ?: colors.iosBlue)
                                else colors.textMuted.copy(alpha = 0.38f),
                                size = 20.dp
                            )
                        },
                        label = if (allSelectedLocked) "Unlock" else "Lock",
                        tint = gradientStyle.primaryColor.takeIf { gradientStyle != OneUiGradientStyle.SOLID } ?: colors.iosBlue,
                        enabled = hasSelection,
                        onClick = {
                            showBiometricLockConfirmationDialog = true
                        }
                    )

                    // 5. Delete (Destructive)
                    OneUiSelectionActionButton(
                        icon = {
                            LucideTrash(
                                tint = if (hasSelection) Color(0xFFEF4444)
                                else Color(0xFFEF4444).copy(alpha = 0.38f),
                                size = 20.dp
                            )
                        },
                        label = "Delete",
                        tint = Color(0xFFEF4444),
                        enabled = hasSelection,
                        onClick = {
                            showDeleteSelectedChatsDialog = true
                        }
                    )
                }
            }
        }

        // 7. CONFIRMATION DIALOGS
        if (showDeleteSelectedChatsDialog) {
            val count = selectedChatIds.size
            IosAlertDialog(
                title = "Delete ${if (count == 1) "Conversation" else "$count Conversations"}?",
                message = "The selected ${if (count == 1) "conversation" else "conversations"} and all message history will be permanently deleted from your device.",
                actions = listOf(
                    IosAlertAction(
                        title = "Cancel",
                        isCancel = true,
                        onClick = { showDeleteSelectedChatsDialog = false }
                    ),
                    IosAlertAction(
                        title = "Delete",
                        isDestructive = true,
                        onClick = {
                            showDeleteSelectedChatsDialog = false
                            val idsToDelete = selectedChatIds.toList()
                            idsToDelete.forEach { id ->
                                chats.find { it.id == id }?.let { onDeleteChat(it) }
                            }
                            isSelectionMode = false
                            selectedChatIds.clear()
                        }
                    )
                ),
                onDismiss = { showDeleteSelectedChatsDialog = false }
            )
        }

        if (showBiometricLockConfirmationDialog) {
            val count = selectedChatIds.size
            val allSelectedLocked = selectedChatIds.isNotEmpty() && selectedChatIds.all { it in lockedChatIds }
            val title = if (allSelectedLocked) "Unlock with Biometrics?" else "Biometric Chat Lock"
            val message = if (allSelectedLocked) {
                "Are you sure you want to remove biometric lock from ${if (count == 1) "this conversation" else "$count selected conversations"}?"
            } else {
                "Are you sure you want to lock ${if (count == 1) "this conversation" else "$count selected conversations"} with Biometrics? You will need your biometric authentication to access them."
            }
            val confirmText = if (allSelectedLocked) "Unlock" else "Lock Chats"

            IosAlertDialog(
                title = title,
                message = message,
                actions = listOf(
                    IosAlertAction(
                        title = "Cancel",
                        isCancel = true,
                        onClick = { showBiometricLockConfirmationDialog = false }
                    ),
                    IosAlertAction(
                        title = confirmText,
                        isDestructive = false,
                        onClick = {
                            showBiometricLockConfirmationDialog = false
                            val toLock = selectedChatIds.toList()
                            isSelectionMode = false
                            selectedChatIds.clear()
                            if (onLockMultipleChatsToggle != null) {
                                onLockMultipleChatsToggle(toLock)
                            } else {
                                toLock.forEach { id ->
                                    chats.find { it.id == id }?.let { onLockChatToggle(it) }
                                }
                            }
                        }
                    )
                ),
                onDismiss = { showBiometricLockConfirmationDialog = false }
            )
        }

        chatPendingDelete?.let { chat ->
            IosAlertDialog(
                title = "Delete Chat?",
                message = "Are you sure you want to delete this chat with \"${chat.name}\"? All message history will be removed.",
                actions = listOf(
                    IosAlertAction(
                        title = "Cancel",
                        isCancel = true,
                        onClick = { chatPendingDelete = null }
                    ),
                    IosAlertAction(
                        title = "Delete",
                        isDestructive = true,
                        onClick = {
                            val toDelete = chat
                            chatPendingDelete = null
                            onDeleteChat(toDelete)
                        }
                    )
                ),
                onDismiss = { chatPendingDelete = null }
            )
        }

        // 6. LONG-PRESS CONVERSATION QUICK ACTIONS MODAL SHEET
        selectedChatForActions?.let { targetChat ->
            val isPinned = targetChat.id in pinnedChatIds
            val isLocked = targetChat.id in lockedChatIds
            ModalBottomSheet(
                onDismissRequest = { selectedChatForActions = null },
                containerColor = cardBgColor,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = targetChat.name.ifBlank { "Conversation Options" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OneUiSheetActionRow(
                        label = if (isPinned) "Unpin from favorites" else "Pin to favorites",
                        color = colors.textDark
                    ) {
                        selectedChatForActions = null
                        onPinChatToggle(targetChat)
                    }
                    OneUiSheetActionRow(
                        label = if (targetChat.unreadCount > 0) "Mark as read" else "Mark as unread",
                        color = colors.textDark
                    ) {
                        selectedChatForActions = null
                        onReadChatToggle(targetChat)
                    }
                    OneUiSheetActionRow(
                        label = if (targetChat.isMuted) "Unmute notifications" else "Mute notifications",
                        color = colors.textDark
                    ) {
                        selectedChatForActions = null
                        onMuteChatToggle(targetChat)
                    }
                    OneUiSheetActionRow(
                        label = if (isLocked) "Remove Biometric Lock" else "Lock with Biometrics",
                        color = colors.iosBlue
                    ) {
                        selectedChatForActions = null
                        onLockChatToggle(targetChat)
                    }
                    OneUiSheetActionRow(
                        label = "Start Audio Call",
                        color = colors.textDark
                    ) {
                        selectedChatForActions = null
                        onStartCall(targetChat, "Audio Call")
                    }
                    OneUiSheetActionRow(
                        label = "Export Chat Transcript",
                        color = colors.textDark
                    ) {
                        selectedChatForActions = null
                        onExportChat(targetChat)
                    }
                    OneUiSheetActionRow(
                        label = "Archive Conversation",
                        color = colors.textDark
                    ) {
                        selectedChatForActions = null
                        onArchiveChat(targetChat)
                    }
                    OneUiSheetActionRow(
                        label = "Clear Chat History",
                        color = Color(0xFFFF9500)
                    ) {
                        selectedChatForActions = null
                        onClearChatHistory(targetChat)
                    }
                    OneUiSheetActionRow(
                        label = "Delete Conversation",
                        color = Color(0xFFFF3B30)
                    ) {
                        selectedChatForActions = null
                        onDeleteChat(targetChat)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun OneUiSheetActionRow(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OneUiSwipeableConversationItem(
    chat: ChatConversation,
    snippet: String,
    timeLabel: String,
    gradientStyle: OneUiGradientStyle,
    cardBgColor: Color,
    dividerColor: Color,
    isPinned: Boolean,
    isLocked: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    showDivider: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPinToggle: () -> Unit,
    onMuteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalChatColors.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }

    val pinActionWidthPx = with(density) { 80.dp.toPx() }
    val leftActionsWidthPx = with(density) { 150.dp.toPx() }

    LaunchedEffect(isSelectionMode) {
        if (isSelectionMode && dragOffset.value != 0f) {
            dragOffset.animateTo(0f, spring(dampingRatio = 0.8f))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // 1. Background Swipe Actions
        if (dragOffset.value > 0f) {
            // Pin / Unpin Action (Revealed on Swipe Right)
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                val pinBgColor = if (gradientStyle == OneUiGradientStyle.SOLID) {
                    colors.iosBlue
                } else {
                    gradientStyle.primaryColor
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .background(pinBgColor)
                        .clickable {
                            SoundManager.playReaction()
                            coroutineScope.launch {
                                dragOffset.animateTo(0f, spring(dampingRatio = 0.82f))
                            }
                            onPinToggle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LucidePin(tint = Color.White, size = 20.dp)
                        Text(
                            text = if (isPinned) "Unpin" else "Pin",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        } else if (dragOffset.value < 0f) {
            // Mute & Delete Actions (Revealed on Swipe Left)
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute / Unmute Button
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(75.dp)
                            .background(Color(0xFFF59E0B))
                            .clickable {
                                SoundManager.playReaction()
                                coroutineScope.launch {
                                    dragOffset.animateTo(0f, spring(dampingRatio = 0.82f))
                                }
                                onMuteToggle()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (chat.isMuted) {
                                LucideBell(tint = Color.White, size = 20.dp)
                            } else {
                                LucideBellOff(tint = Color.White, size = 20.dp)
                            }
                            Text(
                                text = if (chat.isMuted) "Unmute" else "Mute",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Delete Button
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(75.dp)
                            .background(Color(0xFFEF4444))
                            .clickable {
                                SoundManager.playReaction()
                                coroutineScope.launch {
                                    dragOffset.animateTo(0f, spring(dampingRatio = 0.82f))
                                }
                                onDelete()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LucideTrash(tint = Color.White, size = 20.dp)
                            Text(
                                text = "Delete",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 2. Foreground Item Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .background(cardBgColor)
                .then(
                    if (!isSelectionMode) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    coroutineScope.launch {
                                        val newOffset = (dragOffset.value + dragAmount)
                                            .coerceIn(-leftActionsWidthPx, pinActionWidthPx)
                                        dragOffset.snapTo(newOffset)
                                    }
                                },
                                onDragEnd = {
                                    coroutineScope.launch {
                                        val current = dragOffset.value
                                        val target = when {
                                            current > pinActionWidthPx / 2f -> pinActionWidthPx
                                            current < -leftActionsWidthPx / 2f -> -leftActionsWidthPx
                                            else -> 0f
                                        }
                                        dragOffset.animateTo(
                                            target,
                                            animationSpec = spring(
                                                dampingRatio = 0.82f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .combinedClickable(
                    onClick = {
                        if (dragOffset.value != 0f) {
                            coroutineScope.launch {
                                dragOffset.animateTo(0f, spring(dampingRatio = 0.82f))
                            }
                        } else {
                            onClick()
                        }
                    },
                    onLongClick = onLongClick
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated Checkbox in Selection Mode
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiquidGlassCheckbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            checkedColor = if (gradientStyle == OneUiGradientStyle.SOLID) colors.iosBlue else gradientStyle.primaryColor,
                            size = 22.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }

                // Avatar + Online Status Dot
                Box(contentAlignment = Alignment.BottomEnd) {
                    IosAvatarView(
                        name = chat.name,
                        avatarUrl = chat.avatar,
                        size = 50.dp
                    )
                    if (chat.online) {
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(2.dp, cardBgColor, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Center Name & Message Preview
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = chat.name.ifBlank { "Chat" },
                            fontSize = 15.5.sp,
                            fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                            color = colors.textDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPinned) {
                            LucideStar(tint = Color(0xFFF59E0B), size = 13.dp)
                        }
                        if (isLocked) {
                            LucideLock(tint = colors.iosBlue, size = 13.dp)
                        }
                        if (chat.isMuted) {
                            Text(
                                text = "🔕",
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = snippet,
                        fontSize = 13.sp,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        color = if (chat.statusText.contains("typing", ignoreCase = true)) {
                            colors.iosBlue
                        } else {
                            colors.textMuted
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Timestamp + Unread Pill
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (timeLabel.isNotBlank()) {
                        Text(
                            text = timeLabel,
                            fontSize = 11.sp,
                            color = if (chat.unreadCount > 0) colors.iosBlue else colors.textMuted
                        )
                    }
                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (gradientStyle == OneUiGradientStyle.SOLID) colors.iosBlue
                                    else gradientStyle.primaryColor
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (showDivider) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = if (isSelectionMode) 114.dp else 80.dp, end = 16.dp)
                        .height(0.7.dp)
                        .background(dividerColor)
                )
            }
        }
    }
}

@Composable
private fun OneUiSelectionActionButton(
    icon: @Composable () -> Unit,
    label: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) {
                SoundManager.playReaction()
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) tint else tint.copy(alpha = 0.38f),
            maxLines = 1
        )
    }
}