package com.chroma.studio.ui.screens

import com.chroma.studio.ui.components.VerifiedBadge

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chroma.studio.ui.components.LiquidGlassSecondaryButton
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.R
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.model.Story
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.UserStoryGroup
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.components.MarkdownText
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val StoryViewerJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

@Composable
fun StoryViewerScreen(
    storyGroups: List<UserStoryGroup>,
    initialGroupIndex: Int,
    currentUserId: String,
    storyViewModel: StoryViewModel,
    likedStoryIds: Set<String>,
    myStories: List<Story>,
    currentUserProfile: UserProfile? = null,
    onClose: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onReplySend: ((authorId: String, text: String) -> Unit)? = null
) {
    val allGroups = remember(storyGroups, myStories, currentUserId, currentUserProfile, initialGroupIndex) {
        if (initialGroupIndex < 0 && myStories.isNotEmpty()) {
            listOf(
                UserStoryGroup(
                    userId = currentUserId,
                    userProfile = currentUserProfile?.copy(
                        displayName = currentUserProfile.displayName.ifBlank { "Your Story" }
                    ) ?: UserProfile(uid = currentUserId, displayName = "Your Story"),
                    stories = myStories,
                    allSeen = true
                )
            ) + storyGroups.filter { it.userId != currentUserId }
        } else {
            storyGroups
        }
    }

    var currentGroupIndex by remember { mutableIntStateOf(if (initialGroupIndex < 0) 0 else initialGroupIndex.coerceIn(0, maxOf(0, allGroups.size - 1))) }
    var currentStoryIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var showViewers by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var quickReplyText by remember { mutableStateOf("") }
    var showQuickReactions by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val storyDurationMs = 5000L

    val currentGroup = allGroups.getOrNull(currentGroupIndex)
    val storiesForCurrentGroup = currentGroup?.stories ?: emptyList()
    val currentStory = storiesForCurrentGroup.getOrNull(currentStoryIndex)
    val isOwner = currentStory != null && (currentStory.userId == currentUserId || currentGroup?.userId == currentUserId)
    val isLiked = currentStory != null && likedStoryIds.contains(currentStory.id)

    val progress = remember(currentStoryIndex, currentGroupIndex) { Animatable(0f) }
    val effectivePaused = isPaused || showViewers || showDeleteConfirm

    LaunchedEffect(currentStoryIndex, currentGroupIndex, effectivePaused, storiesForCurrentGroup.size) {
        if (!effectivePaused && storiesForCurrentGroup.isNotEmpty()) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = storyDurationMs.toInt(), easing = LinearEasing)
            )
            goNextStory(
                currentStoryIndex = currentStoryIndex,
                currentGroupIndex = currentGroupIndex,
                storiesInGroup = storiesForCurrentGroup.size,
                totalGroups = allGroups.size,
                onStoryChange = { currentStoryIndex = it },
                onGroupChange = { currentGroupIndex = it; currentStoryIndex = 0 },
                onClose = onClose
            )
        }
    }

    LaunchedEffect(currentStory?.id) {
        if (currentStory != null && !isOwner) {
            storyViewModel.markViewed(currentStory.id)
        }
    }

    val likeScale = remember { Animatable(1f) }
    LaunchedEffect(isLiked) {
        if (isLiked) {
            likeScale.animateTo(1.4f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
            likeScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        LaunchedEffect(view) {
            val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            window?.let {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(it, false)
                androidx.core.view.WindowInsetsControllerCompat(it, view).apply {
                    hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                    systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // ── Story Content ─────────────────────────────────────────────────────────
            if (currentStory != null) {
                val media = currentStory.mediaUrl
                val isTextStory = media.startsWith("text:") || media.startsWith("markdown://")
                val textBody = when {
                    media.startsWith("text:") -> media.removePrefix("text:")
                    media.startsWith("markdown://") -> media.removePrefix("markdown://")
                    else -> ""
                }

                if (isTextStory) {
                    val formatState = try {
                        if (currentStory.textFormat.isNotBlank() && currentStory.textFormat != "{}") {
                            StoryViewerJson.decodeFromString<StatusFormatState>(currentStory.textFormat)
                        } else {
                            StatusFormatState()
                        }
                    } catch (e: Exception) {
                        StatusFormatState()
                    }

                    val gradientTheme = StoryGradientThemes.find { it.id == formatState.gradientPreset }
                    val backgroundBrush = if (gradientTheme != null && formatState.gradientPreset != "None") {
                        Brush.verticalGradient(gradientTheme.colors)
                    } else {
                        val solidBg = try { Color(android.graphics.Color.parseColor(if (currentStory.backgroundColor.isNotBlank()) currentStory.backgroundColor else formatState.backgroundColorHex)) } catch (e: Exception) { Color(0xFF1F1C2C) }
                        Brush.verticalGradient(listOf(solidBg, solidBg))
                    }

                    val textColor = try { Color(android.graphics.Color.parseColor(formatState.textColorHex)) } catch (e: Exception) { Color.White }
                    val highlightColor = if (formatState.isHighlight) {
                        try { Color(android.graphics.Color.parseColor(formatState.highlightColorHex)).copy(alpha = 0.35f) } catch (e: Exception) { textColor.copy(alpha = 0.25f) }
                    } else Color.Transparent

                    val font = when (formatState.fontFamily) {
                        "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                        "Mono" -> com.chroma.studio.ui.theme.MonoFontFamily
                        "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
                        else -> com.chroma.studio.ui.theme.InterFontFamily
                    }

                    val align = when (formatState.textAlign) {
                        "left" -> androidx.compose.ui.text.style.TextAlign.Left
                        "right" -> androidx.compose.ui.text.style.TextAlign.Right
                        else -> androidx.compose.ui.text.style.TextAlign.Center
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundBrush)
                            .padding(horizontal = 24.dp, vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                                .padding(24.dp),
                            horizontalAlignment = if (formatState.textAlign == "left") Alignment.Start else if (formatState.textAlign == "right") Alignment.End else Alignment.CenterHorizontally
                        ) {
                            // Category Tag Badge
                            if (formatState.tag.isNotBlank()) {
                                val catItem = StoryCategories.find { it.name.equals(formatState.tag, ignoreCase = true) }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.5.dp)) {
                                        if (catItem != null) {
                                            Icon(
                                                imageVector = catItem.icon,
                                                contentDescription = null,
                                                tint = textColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = formatState.tag,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textColor
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            // Title if present
                            if (formatState.title.isNotBlank()) {
                                Text(
                                    text = formatState.title,
                                    style = TextStyle(
                                        fontFamily = font,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        textAlign = align
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(10.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(textColor.copy(alpha = 0.15f)))
                                Spacer(Modifier.height(12.dp))
                            }

                            // Body Content / Quote
                            if (currentStory.textContent.isNotBlank()) {
                                val bodyOnly = if (formatState.title.isNotBlank() && currentStory.textContent.startsWith("# ")) {
                                    currentStory.textContent.substringAfter("\n\n").ifBlank { currentStory.textContent }
                                } else {
                                    currentStory.textContent
                                }

                                if (formatState.isQuote) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(textColor.copy(alpha = 0.08f))
                                            .border(2.dp, Brush.verticalGradient(listOf(Color(0xFF007AFF), Color(0xFFFF2D55))), RoundedCornerShape(14.dp))
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("“", fontSize = 28.sp, fontWeight = FontWeight.Black, color = textColor)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = bodyOnly,
                                            style = TextStyle(
                                                fontFamily = font,
                                                fontSize = if (formatState.headingLevel == 2) 22.sp else 16.sp,
                                                fontWeight = if (formatState.isBold) FontWeight.Bold else FontWeight.Normal,
                                                fontStyle = FontStyle.Italic,
                                                color = textColor,
                                                textAlign = align
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = bodyOnly,
                                        style = TextStyle(
                                            fontFamily = font,
                                            fontSize = if (formatState.headingLevel == 2) 24.sp else if (formatState.headingLevel == 3) 19.sp else 16.sp,
                                            fontWeight = if (formatState.isBold) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (formatState.isItalic) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = if (formatState.isUnderline) TextDecoration.Underline else TextDecoration.None,
                                            color = textColor,
                                            textAlign = align,
                                            background = highlightColor,
                                            lineHeight = if (formatState.headingLevel == 2) 32.sp else 23.sp,
                                            drawStyle = if (formatState.isStroke) Stroke(
                                                miter = 8f,
                                                width = 2.5f,
                                                join = StrokeJoin.Round
                                            ) else androidx.compose.ui.graphics.drawscope.Fill
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                // Fallback to legacy markdown if textContent is empty but mediaUrl had markdown
                                MarkdownText(
                                    markdown = textBody,
                                    defaultColor = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Mood pill at bottom
                            if (formatState.mood.isNotBlank()) {
                                val moodItem = StoryMoods.find { it.name.equals(formatState.mood, ignoreCase = true) }
                                Spacer(Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.5.dp)) {
                                        if (moodItem != null) {
                                            Icon(
                                                imageVector = moodItem.icon,
                                                contentDescription = null,
                                                tint = textColor.copy(alpha = 0.9f),
                                                modifier = Modifier.size(11.5.dp)
                                            )
                                        }
                                        Text(
                                            text = formatState.mood,
                                            fontSize = 11.sp,
                                            color = textColor.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(media)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Story Media",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // ── Touch / Gestures Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentStoryIndex, currentGroupIndex, storiesForCurrentGroup.size) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (showViewers || showDeleteConfirm) return@detectTapGestures
                                val totalWidth = size.width
                                if (offset.x < totalWidth * 0.33f) {
                                    if (currentStoryIndex > 0) {
                                        currentStoryIndex--
                                    } else if (currentGroupIndex > 0) {
                                        currentGroupIndex--
                                        val prevCount = allGroups.getOrNull(currentGroupIndex)?.stories?.size ?: 1
                                        currentStoryIndex = maxOf(0, prevCount - 1)
                                    }
                                } else {
                                    goNextStory(
                                        currentStoryIndex = currentStoryIndex,
                                        currentGroupIndex = currentGroupIndex,
                                        storiesInGroup = storiesForCurrentGroup.size,
                                        totalGroups = allGroups.size,
                                        onStoryChange = { currentStoryIndex = it },
                                        onGroupChange = { currentGroupIndex = it; currentStoryIndex = 0 },
                                        onClose = onClose
                                    )
                                }
                            },
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            }
                        )
                    }
            )

            // ── Top Bar ───────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 12.dp, end = 12.dp)
            ) {
                // Segmented Progress Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    storiesForCurrentGroup.forEachIndexed { index, _ ->
                        val segmentProgress = when {
                            index < currentStoryIndex -> 1f
                            index == currentStoryIndex -> progress.value
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.35f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(segmentProgress)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // User Header Row
                currentGroup?.let { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (!isOwner) {
                                        onAuthorClick(group.userId)
                                    }
                                }
                        ) {
                            IosAvatarView(
                                name = group.userProfile.displayName,
                                avatarUrl = group.userProfile.avatarUrl,
                                size = 38.dp,
                                gender = group.userProfile.gender
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isOwner) "Your Story" else group.userProfile.displayName.ifBlank { "User" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (group.userProfile.verified && !isOwner) {
                                    Spacer(Modifier.width(4.dp))
                                    VerifiedBadge(size = 12.dp)
                                }
                            }
                            currentStory?.let {
                                Text(
                                    text = storyFormatTimeAgo(it.timestamp),
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        // Owner Delete Action
                        if (isOwner && currentStory != null) {
                            Icon(
                                Lucide.Trash2,
                                contentDescription = "Delete Story",
                                tint = Color(0xFFFF453A),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { showDeleteConfirm = true }
                            )
                            Spacer(Modifier.width(12.dp))
                        }

                        // Close Button
                        Icon(
                            Lucide.X,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onClose() }
                        )
                    }
                }
            }

            // ── Bottom Controls ───────────────────────────────────────────────────────
            if (currentStory != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    if (isOwner) {
                        // Admin: Viewers & Likes pill button
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                                .clickable { showViewers = true }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Lucide.Eye, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "${currentStory.viewedBy.size} views",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(ChromaIcons.HeartFilled, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "${currentStory.likedBy.size}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(Lucide.ChevronUp, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        }
                    } else {
                        // Viewer: Quick Message & Like Bar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (showQuickReactions) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("🔥", "❤️", "😂", "😍", "😮", "👏").forEach { emoji ->
                                        Text(
                                            text = emoji,
                                            fontSize = 22.sp,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    ToastManager.showToast("Sent $emoji reaction")
                                                    showQuickReactions = false
                                                }
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(Color.White.copy(alpha = 0.18f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BasicTextField(
                                            value = quickReplyText,
                                            onValueChange = { quickReplyText = it },
                                            singleLine = true,
                                            textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                                            cursorBrush = SolidColor(Color.White),
                                            decorationBox = { innerTextField ->
                                                if (quickReplyText.isEmpty()) {
                                                    Text(
                                                        "Send message...",
                                                        color = Color.White.copy(alpha = 0.65f),
                                                        fontSize = 13.5.sp
                                                    )
                                                }
                                                innerTextField()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (quickReplyText.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    val reply = quickReplyText.trim()
                                                    val authorId = currentStory?.userId ?: currentGroup?.userId ?: ""
                                                    if (authorId.isNotBlank() && reply.isNotBlank()) {
                                                        onReplySend?.invoke(authorId, reply)
                                                    }
                                                    ToastManager.showToast("Reply sent!")
                                                    quickReplyText = ""
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Lucide.Send, contentDescription = "Send", tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { showQuickReactions = !showQuickReactions },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Lucide.Smile, contentDescription = "Reactions", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }

                                // Filled Heart Like Button
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .scale(likeScale.value)
                                        .clip(CircleShape)
                                        .background(if (isLiked) Color(0xFFFF2D55).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.18f))
                                        .border(
                                            0.5.dp,
                                            if (isLiked) Color(0xFFFF2D55).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.35f),
                                            CircleShape
                                        )
                                        .clickable {
                                            currentStory.let { storyViewModel.toggleLike(it.id) }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) ChromaIcons.HeartFilled else Lucide.Heart,
                                        contentDescription = if (isLiked) "Unlike" else "Like",
                                        tint = if (isLiked) Color(0xFFFF2D55) else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Admin Viewers Bottom Sheet ────────────────────────────────────────────
            if (showViewers && currentStory != null) {
                AdminStoryViewersSheet(
                    story = currentStory,
                    onDismiss = { showViewers = false },
                    onDelete = {
                        storyViewModel.deleteStory(currentStory.id)
                        showViewers = false
                        ToastManager.showToast("Story deleted")
                        onClose()
                    }
                )
            }

            // ── Delete Confirmation Dialog ────────────────────────────────────────────
            if (showDeleteConfirm && currentStory != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("Delete Story?", fontWeight = FontWeight.Bold) },
                    text = { Text("This will permanently remove this story for all viewers.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                storyViewModel.deleteStory(currentStory.id)
                                showDeleteConfirm = false
                                ToastManager.showToast("Story deleted")
                                onClose()
                            }
                        ) {
                            Text("Delete", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        LiquidGlassSecondaryButton(
                            text = "Cancel",
                            onClick = { showDeleteConfirm = false },
                            isSmall = true
                        )
                    },
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }
    }
}

// ── Admin Viewers Sheet Component ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminStoryViewersSheet(
    story: Story,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var viewerProfiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoadingProfiles by remember { mutableStateOf(true) }

    LaunchedEffect(story.id) {
        if (story.viewedBy.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val repo = DiscoverRepository()
                val profiles = story.viewedBy.mapNotNull { uid ->
                    try { repo.getUserProfile(uid) } catch (e: Exception) { null }
                }
                viewerProfiles = profiles
                isLoadingProfiles = false
            }
        } else {
            isLoadingProfiles = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Story Insights",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${story.viewedBy.size} Viewers · ${story.likedBy.size} Likes",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Lucide.Trash2, contentDescription = "Delete", tint = Color(0xFFFF453A))
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isLoadingProfiles) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF007AFF), modifier = Modifier.size(24.dp))
                }
            } else if (viewerProfiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No viewers yet", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewerProfiles) { profile ->
                        val hasLiked = story.likedBy.contains(profile.uid)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IosAvatarView(
                                    name = profile.displayName,
                                    avatarUrl = profile.avatarUrl,
                                    size = 38.dp,
                                    gender = profile.gender
                                )
                                Column {
                                    Text(profile.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("@${profile.username}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }

                            if (hasLiked) {
                                Icon(
                                    ChromaIcons.HeartFilled,
                                    contentDescription = "Liked",
                                    tint = Color(0xFFFF2D55),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun goNextStory(
    currentStoryIndex: Int,
    currentGroupIndex: Int,
    storiesInGroup: Int,
    totalGroups: Int,
    onStoryChange: (Int) -> Unit,
    onGroupChange: (Int) -> Unit,
    onClose: () -> Unit
) {
    if (currentStoryIndex < storiesInGroup - 1) {
        onStoryChange(currentStoryIndex + 1)
    } else if (currentGroupIndex < totalGroups - 1) {
        onGroupChange(currentGroupIndex + 1)
    } else {
        onClose()
    }
}

private fun storyFormatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "Yesterday"
    }
}
