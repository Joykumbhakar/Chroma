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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

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
    val isDark = isSystemInDarkTheme()
    val prefs = remember { context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE) }

    var statusEmoji by remember(userId) {
        mutableStateOf(prefs.getString("status_emoji_$userId", profile?.statusEmoji ?: "✨") ?: "✨")
    }
    var statusText by remember(userId) {
        mutableStateOf(prefs.getString("status_text_$userId", profile?.statusText ?: "Living in color & gradients") ?: "Living in color & gradients")
    }
    var showEditDialog by remember { mutableStateOf(false) }

    val statusCardBg = if (isDark) Color(0xFF141416) else Color(0xFFF1F5F9)
    val statusBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(statusCardBg)
            .border(1.dp, statusBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = isCurrentUser) {
                SoundManager.playReaction()
                showEditDialog = true
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Emoji Status Bubble
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E1E22) else Color.White)
                        .border(0.5.dp, statusBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusEmoji,
                        fontSize = 18.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759).copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = if (profile?.online == true) "Active Status" else "Current Mood",
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = statusText.ifBlank { "Set your status..." },
                        fontSize = 13.5.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isCurrentUser) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Pencil,
                        contentDescription = "Edit Status",
                        tint = colors.textMuted,
                        modifier = Modifier.size(13.dp)
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

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Set Profile Status",
                    fontSize = 17.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
            },
            text = {
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        statusEmoji = tempEmoji
                        statusText = tempText
                        prefs.edit()
                            .putString("status_emoji_$userId", tempEmoji)
                            .putString("status_text_$userId", tempText)
                            .apply()
                        try {
                            FirebaseFirestore.getInstance().collection("users").document(userId)
                                .update(mapOf("statusEmoji" to tempEmoji, "statusText" to tempText))
                        } catch (_: Exception) {}
                        showEditDialog = false
                    }
                ) {
                    Text("Save", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            },
            containerColor = colors.bg,
            shape = RoundedCornerShape(20.dp)
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
    val isDark = isSystemInDarkTheme()
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
        // Defaults
        return listOf(
            ProfileHighlightItem(title = "Artworks", emoji = "🎨", gradientColorIndex = 0),
            ProfileHighlightItem(title = "Projects", emoji = "🚀", gradientColorIndex = 1),
            ProfileHighlightItem(title = "Studio", emoji = "✨", gradientColorIndex = 2),
            ProfileHighlightItem(title = "Favorites", emoji = "💡", gradientColorIndex = 3)
        )
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

    val highlightGradients = remember {
        listOf(
            listOf(Color(0xFF007AFF), Color(0xFF5856D6)),
            listOf(Color(0xFFFF2D55), Color(0xFFFF9500)),
            listOf(Color(0xFF34C759), Color(0xFF007AFF)),
            listOf(Color(0xFFAF52DE), Color(0xFFFF2D55)),
            listOf(Color(0xFF5856D6), Color(0xFF00C7BE))
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                            .width(66.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clickable(interactionSource = interactionSource, indication = null) {
                                SoundManager.playReaction()
                                showNewHighlightDialog = true
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.2.dp,
                                    color = if (isDark) Color.White.copy(0.20f) else Color.Black.copy(0.12f),
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1A1A1E) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Plus,
                                contentDescription = "New Highlight",
                                tint = colors.textMain,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = "New",
                            fontSize = 11.5.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMain,
                            textAlign = TextAlign.Center
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
                        .width(66.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clickable(interactionSource = interactionSource, indication = null) {
                            SoundManager.playReaction()
                            onHighlightClick(hl)
                        }
                ) {
                    // Instagram-style Outer Status Ring
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.8.dp,
                                brush = Brush.linearGradient(gradientColors),
                                shape = CircleShape
                            )
                            .padding(3.5.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF16161A) else Color(0xFFF8FAFC)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!hl.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = hl.coverUrl,
                                contentDescription = hl.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = hl.emoji,
                                fontSize = 24.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = hl.title,
                        fontSize = 11.5.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
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

        AlertDialog(
            onDismissRequest = { showNewHighlightDialog = false },
            title = {
                Text(
                    text = "New Highlight",
                    fontSize = 17.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose an icon & give your highlight a name:",
                        fontSize = 13.sp,
                        color = colors.textMuted
                    )
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
            },
            confirmButton = {
                TextButton(
                    enabled = highlightTitle.isNotBlank(),
                    onClick = {
                        val newItem = ProfileHighlightItem(
                            title = highlightTitle.trim(),
                            emoji = selectedEmoji,
                            gradientColorIndex = highlightsList.size % 5
                        )
                        val updated = highlightsList + newItem
                        highlightsList = updated
                        saveHighlights(updated)
                        showNewHighlightDialog = false
                    }
                ) {
                    Text("Add", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewHighlightDialog = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            },
            containerColor = colors.bg,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
