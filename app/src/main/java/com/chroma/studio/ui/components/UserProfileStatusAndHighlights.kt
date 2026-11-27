package com.chroma.studio.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import com.chroma.studio.model.StoryHighlight
import com.chroma.studio.viewmodel.StoryViewModel
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

@Suppress("DEPRECATION")
private val StatusTightPlatformStyle = PlatformTextStyle(includeFontPadding = false)

private val StatusLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both
)

private val StatusTightTextStyle = TextStyle(
    platformStyle = StatusTightPlatformStyle,
    lineHeightStyle = StatusLineHeightStyle
)

data class ProfileHighlightItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val emoji: String = "✨",
    val coverUrl: String? = null,
    val gradientColorIndex: Int = 0,
    val storyIds: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Dedicated Designed Status Section for User Profile:
 * - Shows current status emoji & status phrase
 * - Live online indicator dot with subtle breathing glow
 * - Edit / update status dialog for current user
 */
@Composable
fun UserProfileStatusSection(
    profile: UserProfile?,
    isCurrentUser: Boolean,
    userId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val prefs = remember { context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE) }

    var statusEmoji by remember(userId, profile?.statusEmoji) {
        mutableStateOf(prefs.getString("status_emoji_$userId", profile?.statusEmoji.orEmpty()) ?: profile?.statusEmoji.orEmpty())
    }
    var statusText by remember(userId, profile?.statusText) {
        mutableStateOf(prefs.getString("status_text_$userId", profile?.statusText.orEmpty()) ?: profile?.statusText.orEmpty())
    }
    var showEditDialog by remember { mutableStateOf(false) }

    val statusCardBg = if (isDark) Color(0xFF181B22) else Color(0xFFF3F6FA)

    if (statusText.isNotBlank() || isCurrentUser) {
        val statusLower = statusText.lowercase()
        val isMusicActivity = statusEmoji in listOf("🎧", "🎵", "🎶", "🎼") ||
            statusLower.contains("listening") ||
            statusLower.contains("playing") ||
            statusLower.contains("spotify") ||
            statusLower.contains("music")

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (statusText.isNotBlank()) {
                val pillBg = if (isDark) Color(0xFF1B2231) else Color(0xFFEFF1F6)
                val pillBorder = if (isDark) Color(0xFF283247) else Color(0xFFE2E8F0)
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(pillBg)
                        .border(0.75.dp, pillBorder, CircleShape)
                        .clickable(enabled = isCurrentUser) {
                            SoundManager.playReaction()
                            showEditDialog = true
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    if (isMusicActivity) {
                        Icon(
                            imageVector = Lucide.Headphones,
                            contentDescription = "Music activity",
                            tint = Color(0xFF0064FA),
                            modifier = Modifier.size(13.dp)
                        )
                    } else if (statusEmoji.isBlank()) {
                        Icon(
                            imageVector = Lucide.Sparkles,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(12.5.dp)
                        )
                    }
                    Text(
                        text = if (statusEmoji.isNotBlank()) "$statusEmoji $statusText" else statusText,
                        fontSize = 12.5.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMain.copy(alpha = 0.88f),
                        letterSpacing = (-0.01).em,
                        lineHeight = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = StatusTightTextStyle,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrentUser) {
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = "Edit status",
                            tint = colors.textMuted.copy(alpha = 0.55f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isCurrentUser) {
                            SoundManager.playReaction()
                            showEditDialog = true
                        }
                        .padding(vertical = 3.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Sparkles,
                        contentDescription = null,
                        tint = colors.textMuted.copy(alpha = 0.68f),
                        modifier = Modifier.size(12.5.dp)
                    )
                    Text(
                        text = "Add status or listening activity",
                        fontSize = 12.5.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        color = colors.textMuted.copy(alpha = 0.72f),
                        letterSpacing = (-0.01).em,
                        lineHeight = 15.sp,
                        style = StatusTightTextStyle
                    )
                }
            }
        }
    }

    // Edit Status Dialog
    if (showEditDialog) {
        var tempEmoji by remember { mutableStateOf(statusEmoji) }
        var tempText by remember { mutableStateOf(statusText) }
        val emojis = listOf("✨", "🎨", "🚀", "💡", "☕", "🔥", "🎧", "🌿", "📸", "⚡")

        IosAlertDialog(
            title = "Set Profile Status",
            message = null,
            confirmText = "Save",
            onConfirm = {
                statusEmoji = tempEmoji
                statusText = tempText
                prefs.edit()
                    .putString("status_emoji_$userId", tempEmoji)
                    .putString("status_text_$userId", tempText)
                    .apply()
                try {
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                        .set(mapOf("statusEmoji" to tempEmoji, "statusText" to tempText), com.google.firebase.firestore.SetOptions.merge())
                    com.chroma.studio.data.DiscoverRepository.getInstance().invalidateProfile(userId)
                } catch (_: Exception) {}
                showEditDialog = false
            },
            dismissText = "Cancel",
            onDismiss = { showEditDialog = false },
            customContent = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Quick emoji selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        emojis.take(5).forEach { em ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (tempEmoji == em) colors.primary.copy(alpha = 0.2f) else if (isDark) Color(0xFF1E1E22) else Color(0xFFEFF2F6))
                                    .border(
                                        width = if (tempEmoji == em) 1.5.dp else 0.dp,
                                        color = if (tempEmoji == em) colors.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { tempEmoji = em },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = em, fontSize = 20.sp)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        emojis.drop(5).take(5).forEach { em ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (tempEmoji == em) colors.primary.copy(alpha = 0.2f) else if (isDark) Color(0xFF1E1E22) else Color(0xFFEFF2F6))
                                    .border(
                                        width = if (tempEmoji == em) 1.5.dp else 0.dp,
                                        color = if (tempEmoji == em) colors.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { tempEmoji = em },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = em, fontSize = 20.sp)
                            }
                        }
                    }

                    // Status text input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1E1E22) else Color(0xFFEFF2F6))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = tempText,
                            onValueChange = { tempText = it.take(60) },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                fontFamily = InterFontFamily,
                                color = colors.textMain,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (tempText.isEmpty()) {
                                    Text(
                                        text = "What's on your mind?",
                                        fontSize = 14.sp,
                                        fontFamily = InterFontFamily,
                                        color = colors.textMuted
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }
            }
        )
    }
}

/**
 * Saved Story Highlights Showcase for User Profile (Instagram Style):
 * - Circular highlight bubbles with custom titles
 * - '+ New' button for adding highlights from saved stories
 * - Tap to view highlight reel / story
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserProfileHighlightsSection(
    userId: String,
    isCurrentUser: Boolean,
    onHighlightClick: (ProfileHighlightItem) -> Unit = {},
    storyViewModel: StoryViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val prefs = remember { context.getSharedPreferences("user_highlights_prefs", Context.MODE_PRIVATE) }

    var showArchivePicker by remember { mutableStateOf(false) }
    var editingHighlight by remember { mutableStateOf<StoryHighlight?>(null) }

    fun loadHighlights(): List<ProfileHighlightItem> {
        val raw = prefs.getString("highlights_$userId", null)
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                val list = mutableListOf<ProfileHighlightItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val sArr = obj.optJSONArray("storyIds")
                    val sIds = buildList {
                        if (sArr != null) {
                            for (j in 0 until sArr.length()) {
                                val idStr = sArr.optString(j)
                                if (idStr.isNotBlank()) add(idStr)
                            }
                        }
                    }
                    list.add(
                        ProfileHighlightItem(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            title = obj.optString("title", "Highlight"),
                            emoji = obj.optString("emoji", "✨"),
                            coverUrl = obj.optString("coverUrl").takeIf { !it.isNullOrBlank() && it != "text:story" },
                            gradientColorIndex = obj.optInt("gradientIndex", 0),
                            storyIds = sIds,
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                return list
            } catch (_: Exception) {}
        }
        return emptyList()
    }

    fun saveHighlights(items: List<ProfileHighlightItem>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("emoji", item.emoji)
            obj.put("coverUrl", item.coverUrl ?: "")
            obj.put("gradientIndex", item.gradientColorIndex)
            obj.put("storyIds", JSONArray(item.storyIds))
            obj.put("updatedAt", item.updatedAt)
            arr.put(obj)
        }
        prefs.edit().putString("highlights_$userId", arr.toString()).apply()
    }

    var highlightsList by remember(userId) { mutableStateOf(loadHighlights()) }
    var showNewHighlightDialog by remember { mutableStateOf(false) }

    val vmHighlights by (storyViewModel?.highlights?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val vmHighlightsOwnerId by (storyViewModel?.highlightsOwnerId?.collectAsState() ?: remember { mutableStateOf<String?>(null) })

    LaunchedEffect(userId, storyViewModel) {
        if (storyViewModel != null && userId.isNotBlank()) {
            storyViewModel.loadHighlights(userId)
        }
    }

    // Keep cached SharedPreferences aligned when ViewModel finishes loading or deleting highlights
    LaunchedEffect(vmHighlights, vmHighlightsOwnerId, userId) {
        if (vmHighlightsOwnerId == userId) {
            val mapped = vmHighlights.mapIndexed { idx, hl ->
                ProfileHighlightItem(
                    id = hl.id,
                    title = hl.title,
                    emoji = "✨",
                    coverUrl = hl.coverUrl.takeIf { it.isNotBlank() && it != "text:story" },
                    gradientColorIndex = idx % 5,
                    storyIds = hl.storyIds,
                    updatedAt = hl.updatedAt
                )
            }
            highlightsList = mapped
            saveHighlights(mapped)
        }
    }

    // Sync real highlights from Firestore (also used when storyViewModel is null)
    LaunchedEffect(userId) {
        try {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("highlights")
                .get()
                .addOnSuccessListener { snapshot ->
                    val serverItems = snapshot.documents.mapNotNull { doc ->
                        @Suppress("UNCHECKED_CAST")
                        val sIds = (doc.get("storyIds") as? List<String>) ?: emptyList()
                        val uAt = doc.getLong("updatedAt") ?: doc.getLong("createdAt") ?: System.currentTimeMillis()
                        ProfileHighlightItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "Highlight",
                            emoji = doc.getString("emoji") ?: "✨",
                            coverUrl = doc.getString("coverUrl").takeIf { !it.isNullOrBlank() && it != "text:story" },
                            gradientColorIndex = (doc.getLong("gradientIndex") ?: 0L).toInt(),
                            storyIds = sIds,
                            updatedAt = uAt
                        )
                    }.sortedByDescending { it.updatedAt }
                    highlightsList = serverItems
                    saveHighlights(serverItems)
                }
        } catch (_: Exception) {}
    }

    val displayHighlights = remember(vmHighlights, vmHighlightsOwnerId, highlightsList, userId) {
        if (vmHighlightsOwnerId == userId) {
            vmHighlights.mapIndexed { idx, hl ->
                ProfileHighlightItem(
                    id = hl.id,
                    title = hl.title,
                    emoji = "✨",
                    coverUrl = hl.coverUrl.takeIf { it.isNotBlank() && it != "text:story" },
                    gradientColorIndex = idx % 5,
                    storyIds = hl.storyIds,
                    updatedAt = hl.updatedAt
                )
            }
        } else {
            highlightsList
        }
    }

    val highlightGradients = remember {
        listOf(
            listOf(Color(0xFF0381FE), Color(0xFF0052B4)),
            listOf(Color(0xFFFF2D55), Color(0xFFFF9500)),
            listOf(Color(0xFF34C759), Color(0xFF0381FE)),
            listOf(Color(0xFF0284C7), Color(0xFF38BDF8)),
            listOf(Color(0xFF0072DE), Color(0xFF00C7BE))
        )
    }

    if (!isCurrentUser && displayHighlights.isEmpty()) {
        return
    }

    val itemBorderColor = if (isDark) Color(0xFF283247) else Color(0xFFDCE5F4)
    val sheetBg = if (isDark) Color(0xFF0D1017) else Color.White
    val newHighlightBg = if (isDark) Color(0xFF182030) else Color(0xFFF4F7FD)

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.04f to Color.Black,
                            0.94f to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            // + New Highlight (if current user)
            if (isCurrentUser) {
                item(key = "new_highlight_btn") {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.94f else 1f,
                        animationSpec = tween(120, easing = FastOutSlowInEasing),
                        label = "newHlScale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(58.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clickable(interactionSource = interactionSource, indication = null) {
                                SoundManager.playReaction()
                                if (storyViewModel != null) {
                                    showArchivePicker = true
                                } else {
                                    showNewHighlightDialog = true
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(1.dp, itemBorderColor, CircleShape)
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(newHighlightBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Plus,
                                contentDescription = "New Highlight",
                                tint = Color(0xFF0064FA),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "New",
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFD0D9EA) else Color(0xFF374151),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 13.5.sp,
                            style = StatusTightTextStyle
                        )
                    }
                }
            }

            // Saved Highlight Bubbles (Lives Forever)
            items(displayHighlights, key = { it.id }) { hl ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1f,
                    animationSpec = tween(120, easing = FastOutSlowInEasing),
                    label = "hlScale"
                )
                val gradientColors = highlightGradients[hl.gradientColorIndex % highlightGradients.size]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(58.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                SoundManager.playReaction()
                                onHighlightClick(hl)
                            },
                            onLongClick = {
                                if (isCurrentUser && storyViewModel != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    editingHighlight = StoryHighlight(
                                        id = hl.id,
                                        userId = userId,
                                        title = hl.title,
                                        coverUrl = hl.coverUrl ?: "",
                                        storyIds = hl.storyIds,
                                        updatedAt = hl.updatedAt
                                    )
                                }
                            }
                        )
                ) {
                    val titleLower = hl.title.lowercase()
                    val pastelBg = when (titleLower) {
                        "projects", "builds" -> listOf(Color(0xFFF0F7FF), Color(0xFFD6E8FF))
                        "android" -> listOf(Color(0xFFECFDF5), Color(0xFFCCF5E4))
                        "studio", "setup" -> listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))
                        "travel" -> listOf(Color(0xFFFFF7ED), Color(0xFFFDE4C8))
                        "artworks", "clicks" -> listOf(Color(0xFFFDF2F8), Color(0xFFFBD5EA))
                        else -> gradientColors
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(sheetBg, CircleShape)
                            .border(1.dp, itemBorderColor, CircleShape)
                            .padding(2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Brush.linearGradient(pastelBg)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!hl.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = hl.coverUrl,
                                    contentDescription = hl.title,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                when (titleLower) {
                                    "projects", "builds" -> Icon(Lucide.Code, contentDescription = null, tint = Color(0xFF0064FA), modifier = Modifier.size(19.dp))
                                    "android" -> Icon(Lucide.Smartphone, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(19.dp))
                                    "studio", "setup" -> Icon(Lucide.Monitor, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(19.dp))
                                    "travel" -> Icon(Lucide.Compass, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(19.dp))
                                    "artworks", "clicks" -> Icon(Lucide.Camera, contentDescription = null, tint = Color(0xFFDB2777), modifier = Modifier.size(19.dp))
                                    else -> Text(
                                        text = hl.emoji,
                                        fontSize = 19.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = hl.title,
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFD0D9EA) else Color(0xFF374151),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        lineHeight = 13.5.sp,
                        style = StatusTightTextStyle
                    )
                }
            }
        }
    }

    // New Highlight Creator Dialog
    if (showNewHighlightDialog) {
        var highlightTitle by remember { mutableStateOf("") }
        var selectedEmoji by remember { mutableStateOf("✨") }
        val emojis = listOf("✨", "🎨", "🚀", "💡", "📸", "🎧", "🔥", "💎")

        IosAlertDialog(
            title = "New Highlight",
            message = "Choose an icon & give your highlight a name:",
            confirmText = "Add",
            confirmEnabled = highlightTitle.isNotBlank(),
            onConfirm = {
                if (highlightTitle.isNotBlank()) {
                    val newItem = ProfileHighlightItem(
                        title = highlightTitle.trim(),
                        emoji = selectedEmoji,
                        gradientColorIndex = highlightsList.size % 5
                    )
                    val updated = highlightsList + newItem
                    highlightsList = updated
                    saveHighlights(updated)
                    try {
                        FirebaseFirestore.getInstance().collection("users").document(userId)
                            .collection("highlights").document(newItem.id)
                            .set(mapOf(
                                "id" to newItem.id,
                                "title" to newItem.title,
                                "emoji" to newItem.emoji,
                                "coverUrl" to (newItem.coverUrl ?: ""),
                                "gradientIndex" to newItem.gradientColorIndex,
                                "createdAt" to System.currentTimeMillis()
                            ))
                    } catch (_: Exception) {}
                    showNewHighlightDialog = false
                }
            },
            dismissText = "Cancel",
            onDismiss = { showNewHighlightDialog = false },
            customContent = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        emojis.forEach { em ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedEmoji == em) colors.primary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { selectedEmoji = em },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = em, fontSize = 18.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1E1E22) else Color(0xFFEFF2F6))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = highlightTitle,
                            onValueChange = { highlightTitle = it.take(20) },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                fontFamily = InterFontFamily,
                                color = colors.textMain,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (highlightTitle.isEmpty()) {
                                    Text(
                                        text = "Highlight Name (e.g. Artworks)",
                                        fontSize = 13.5.sp,
                                        fontFamily = InterFontFamily,
                                        color = colors.textMuted
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }
            }
        )
    }

    if (showArchivePicker && storyViewModel != null) {
        CreateHighlightFromArchiveSheet(
            storyViewModel = storyViewModel,
            onDismiss = { showArchivePicker = false },
            onHighlightCreated = {
                storyViewModel.loadHighlights(userId)
            }
        )
    }

    if (editingHighlight != null && storyViewModel != null) {
        EditHighlightSheet(
            highlight = editingHighlight!!,
            storyViewModel = storyViewModel,
            onDismiss = { editingHighlight = null },
            onHighlightUpdated = {
                storyViewModel.loadHighlights(userId)
            },
            onHighlightDeleted = {
                storyViewModel.loadHighlights(userId)
            }
        )
    }
}
