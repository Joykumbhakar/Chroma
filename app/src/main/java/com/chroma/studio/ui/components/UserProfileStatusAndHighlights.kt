package com.chroma.studio.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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
    val gradientColorIndex: Int = 0
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

    // Breathing glow animation for active dot
    val infiniteTransition = rememberInfiniteTransition(label = "onlinePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    if (statusText.isNotBlank() || isCurrentUser) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .clickable(enabled = isCurrentUser) {
                    SoundManager.playReaction()
                    showEditDialog = true
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF029248).copy(alpha = pulseAlpha))
            )
            Text(
                text = if (statusText.isNotBlank()) {
                    if (statusEmoji.isNotBlank()) "$statusEmoji $statusText" else statusText
                } else "+ Set a status",
                fontSize = 13.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                color = if (statusText.isNotBlank()) colors.textMuted else colors.textMuted.copy(alpha = 0.6f),
                letterSpacing = (-0.01).em,
                lineHeight = 16.sp,
                style = StatusTightTextStyle
            )
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
@Composable
fun UserProfileHighlightsSection(
    userId: String,
    isCurrentUser: Boolean,
    onHighlightClick: (ProfileHighlightItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val prefs = remember { context.getSharedPreferences("user_highlights_prefs", Context.MODE_PRIVATE) }

    fun loadHighlights(): List<ProfileHighlightItem> {
        val raw = prefs.getString("highlights_$userId", null)
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                val list = mutableListOf<ProfileHighlightItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        ProfileHighlightItem(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            title = obj.optString("title", "Highlight"),
                            emoji = obj.optString("emoji", "✨"),
                            coverUrl = obj.optString("coverUrl").takeIf { !it.isNullOrBlank() },
                            gradientColorIndex = obj.optInt("gradientIndex", 0)
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
            arr.put(obj)
        }
        prefs.edit().putString("highlights_$userId", arr.toString()).apply()
    }

    var highlightsList by remember(userId) { mutableStateOf(loadHighlights()) }
    var showNewHighlightDialog by remember { mutableStateOf(false) }

    // Sync real highlights from Firestore
    LaunchedEffect(userId) {
        try {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("highlights")
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        val serverItems = snapshot.documents.mapNotNull { doc ->
                            ProfileHighlightItem(
                                id = doc.id,
                                title = doc.getString("title") ?: "Highlight",
                                emoji = doc.getString("emoji") ?: "✨",
                                coverUrl = doc.getString("coverUrl").takeIf { !it.isNullOrBlank() },
                                gradientColorIndex = (doc.getLong("gradientIndex") ?: 0L).toInt()
                            )
                        }
                        if (serverItems.isNotEmpty()) {
                            highlightsList = serverItems
                            saveHighlights(serverItems)
                        }
                    }
                }
        } catch (_: Exception) {}
    }

    val highlightGradients = remember {
        listOf(
            listOf(Color(0xFF0A84FF), Color(0xFF5856D6)),
            listOf(Color(0xFFFF2D55), Color(0xFFFF9500)),
            listOf(Color(0xFF34C759), Color(0xFF0A84FF)),
            listOf(Color(0xFFAF52DE), Color(0xFFFF2D55)),
            listOf(Color(0xFF5856D6), Color(0xFF00C7BE))
        )
    }

    if (!isCurrentUser && highlightsList.isEmpty()) {
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // + New Highlight (if current user)
            if (isCurrentUser) {
                item(key = "new_highlight_btn") {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f, label = "newHlScale")

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(56.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clickable(interactionSource = interactionSource, indication = null) {
                                SoundManager.playReaction()
                                showNewHighlightDialog = true
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E222A) else Color(0xFFEFF2F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Plus,
                                contentDescription = "New Highlight",
                                tint = colors.textMain,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "New",
                            fontSize = 11.5.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMain,
                            textAlign = TextAlign.Center,
                            lineHeight = 13.5.sp,
                            style = StatusTightTextStyle
                        )
                    }
                }
            }

            // Saved Highlight Bubbles
            items(highlightsList, key = { it.id }) { hl ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f, label = "hlScale")
                val gradientColors = highlightGradients[hl.gradientColorIndex % highlightGradients.size]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(56.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clickable(interactionSource = interactionSource, indication = null) {
                            SoundManager.playReaction()
                            onHighlightClick(hl)
                        }
                ) {
                    // Border-less Highlight Circle
                    val titleLower = hl.title.lowercase()
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                when (titleLower) {
                                    "projects" -> Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF0056C6)))
                                    "studio" -> Brush.linearGradient(listOf(Color(0xFF0B1930), Color(0xFF040E1E)))
                                    "artworks" -> Brush.sweepGradient(listOf(Color(0xFFFF9500), Color(0xFFFF2D55), Color(0xFFAF52DE), Color(0xFF0A84FF), Color(0xFFFF9500)))
                                    "travel" -> Brush.linearGradient(listOf(Color(0xFF34C759), Color(0xFF007AFF)))
                                    else -> Brush.linearGradient(gradientColors)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!hl.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = hl.coverUrl,
                                contentDescription = hl.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            when (titleLower) {
                                "projects" -> Icon(Lucide.Code, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                "studio" -> Icon(Lucide.Camera, contentDescription = null, tint = Color(0xFF3892F6), modifier = Modifier.size(20.dp))
                                else -> Text(
                                    text = hl.emoji,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = hl.title,
                        fontSize = 11.5.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMain,
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
}
