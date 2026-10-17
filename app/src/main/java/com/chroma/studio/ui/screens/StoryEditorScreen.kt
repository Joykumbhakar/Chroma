package com.chroma.studio.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.components.LiquidGlassPrimaryButton
import com.chroma.studio.ui.components.LiquidGlassSecondaryButton
import com.chroma.studio.ui.components.AiWaveSphereIcon
import com.chroma.studio.ui.components.AiWaveSphereVector
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.ui.theme.MonoFontFamily
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Story Gradient Preset definitions for rich aesthetic blog backgrounds.
 */
data class StoryGradientTheme(
    val id: String,
    val name: String,
    val colors: List<Color>,
    val defaultTextColor: Color = Color.White
)

val StoryGradientThemes = listOf(
    StoryGradientTheme(
        id = "midnight",
        name = "Midnight Luxe",
        colors = listOf(Color(0xFF0F0C20), Color(0xFF1E1035), Color(0xFF2E0854)),
        defaultTextColor = Color.White
    ),
    StoryGradientTheme(
        id = "sunset",
        name = "Sunset Ember",
        colors = listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2), Color(0xFFFF416C)),
        defaultTextColor = Color.White
    ),
    StoryGradientTheme(
        id = "cyberpunk",
        name = "Neon Cyber",
        colors = listOf(Color(0xFF0D0221), Color(0xFF0F084B), Color(0xFF26408B), Color(0xFF00B4D8)),
        defaultTextColor = Color.White
    ),
    StoryGradientTheme(
        id = "emerald",
        name = "Emerald Deep",
        colors = listOf(Color(0xFF051910), Color(0xFF0A3A2A), Color(0xFF0E6251)),
        defaultTextColor = Color.White
    ),
    StoryGradientTheme(
        id = "oceanic",
        name = "Deep Abyss",
        colors = listOf(Color(0xFF03001E), Color(0xFF7303C0), Color(0xFFEC38BC), Color(0xFFFDEFF9)),
        defaultTextColor = Color.White
    ),
    StoryGradientTheme(
        id = "obsidian",
        name = "Obsidian Dark",
        colors = listOf(Color(0xFF121212), Color(0xFF1C1C1E), Color(0xFF2C2C2E)),
        defaultTextColor = Color.White
    ),
    StoryGradientTheme(
        id = "aurora",
        name = "Aurora Borealis",
        colors = listOf(Color(0xFF0B1021), Color(0xFF144552), Color(0xFF05A8AA), Color(0xFFB8D5B8)),
        defaultTextColor = Color.White
    ),
    StoryGradientTheme(
        id = "velvet",
        name = "Crimson Velvet",
        colors = listOf(Color(0xFF2B0920), Color(0xFF5F0A38), Color(0xFF8B0000)),
        defaultTextColor = Color.White
    ),
    StoryGradientTheme(
        id = "peach",
        name = "Warm Honey",
        colors = listOf(Color(0xFF3A1C28), Color(0xFF7B334C), Color(0xFFE07A5F)),
        defaultTextColor = Color.White
    ),
    StoryGradientTheme(
        id = "clean_paper",
        name = "Editorial Paper",
        colors = listOf(Color(0xFFF9F9FB), Color(0xFFF0F0F3), Color(0xFFE8E8EC)),
        defaultTextColor = Color(0xFF111827)
    ),
    StoryGradientTheme(
        id = "minimal_white",
        name = "Pure Minimal",
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC), Color(0xFFF1F5F9)),
        defaultTextColor = Color(0xFF111827)
    )
)

data class StoryCategoryItem(
    val id: String,
    val name: String,
    val icon: ImageVector
)

val StoryCategories = listOf(
    StoryCategoryItem("thought", "Thought", Lucide.MessageSquare),
    StoryCategoryItem("design", "Design", Lucide.Palette),
    StoryCategoryItem("tech", "Tech", Lucide.Code),
    StoryCategoryItem("daily", "Daily Life", Lucide.Coffee),
    StoryCategoryItem("idea", "Idea", Lucide.Lightbulb),
    StoryCategoryItem("vibe", "Vibe", Lucide.Headphones),
    StoryCategoryItem("hot_take", "Hot Take", Lucide.Flame),
    StoryCategoryItem("micro_blog", "Article", Lucide.BookOpen),
    StoryCategoryItem("spark", "Spark", Lucide.Zap),
    StoryCategoryItem("reflection", "Reflection", Lucide.Compass)
)

data class StoryMoodItem(
    val id: String,
    val name: String,
    val icon: ImageVector
)

val StoryMoods = listOf(
    StoryMoodItem("inspired", "Inspired", Lucide.Sun),
    StoryMoodItem("coffee", "Caffeinated", Lucide.Coffee),
    StoryMoodItem("night", "Night Owl", Lucide.Moon),
    StoryMoodItem("chill", "Chill Mode", Lucide.Smile),
    StoryMoodItem("building", "Building", Lucide.Hammer),
    StoryMoodItem("music", "In the Zone", Lucide.Headphones),
    StoryMoodItem("deep", "Deep Thought", Lucide.Lightbulb),
    StoryMoodItem("focused", "Focused", Lucide.Flame)
)

data class StoryPromptItem(
    val title: String,
    val promptBody: String,
    val icon: ImageVector
)

val StoryPrompts = listOf(
    StoryPromptItem("Daily Reflection", "One important lesson I learned today...", Lucide.BookOpen),
    StoryPromptItem("Design Philosophy", "Good design is as little design as possible because...", Lucide.Palette),
    StoryPromptItem("Dev Log", "Currently building something exciting in Compose...", Lucide.Code),
    StoryPromptItem("Hot Take", "Unpopular opinion in tech and design...", Lucide.Flame),
    StoryPromptItem("Quote of the Day", "\"Simplicity is the ultimate sophistication.\"", Lucide.Quote),
    StoryPromptItem("Quick Thought", "What if we reimagined how stories work...", Lucide.Lightbulb),
    StoryPromptItem("Reading Note", "Key takeaway from what I read today...", Lucide.BookOpen)
)

private val StoryFormatJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

enum class StoryEditorToolbarTab {
    TYPOGRAPHY,
    COLORS,
    STRUCTURE,
    AUDIENCE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryEditorScreen(
    storyViewModel: StoryViewModel,
    onNavigateBack: () -> Unit,
    onStoryPosted: () -> Unit
) {
    val themeColors = LocalOscellionColors.current
    val isDark = themeColors.isDark
    val focusManager = LocalFocusManager.current

    // Document State
    var headlineText by remember { mutableStateOf("") }
    var bodyContent by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("") }
    var audienceType by remember { mutableStateOf("public") } // "public", "followers", "custom"

    // Formatting State
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var isStrikethrough by remember { mutableStateOf(false) }
    var isHighlight by remember { mutableStateOf(false) }
    var isStroke by remember { mutableStateOf(false) }
    var isQuoteMode by remember { mutableStateOf(false) }
    var headingLevel by remember { mutableIntStateOf(1) } // 1 = Normal Body, 2 = H1 Large, 3 = H2 Medium
    var textAlign by remember { mutableStateOf("left") } // "left", "center", "right"
    var selectedFontFamilyName by remember { mutableStateOf("Sans") } // "Sans", "Serif", "Mono", "Cursive"

    // Visual Theme State - defaults to light theme gradient if in light mode, or midnight if in dark mode
    val defaultGradientId = if (isDark) "midnight" else "clean_paper"
    var selectedGradientId by remember { mutableStateOf(defaultGradientId) }
    var customBgColorHex by remember { mutableStateOf(if (isDark) "#FF121212" else "#FFF8FAFC") }
    var isCustomSolidBg by remember { mutableStateOf(false) }
    var textColorHex by remember { mutableStateOf(if (isDark) "#FFFFFF" else "#111827") }
    var highlightColorHex by remember { mutableStateOf("#FFD60A") }

    // UI Sheets & Modes
    var activeToolbarTab by remember { mutableStateOf(StoryEditorToolbarTab.TYPOGRAPHY) }
    var showLivePreview by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }

    val currentGradient = StoryGradientThemes.find { it.id == selectedGradientId } ?: StoryGradientThemes.first()

    // Calculate background brush / solid color
    val canvasBackgroundBrush = remember(isCustomSolidBg, selectedGradientId, customBgColorHex) {
        if (isCustomSolidBg) {
            val color = try { Color(AndroidColor.parseColor(customBgColorHex)) } catch (e: Exception) { if (isDark) Color(0xFF121212) else Color(0xFFF8FAFC) }
            Brush.verticalGradient(listOf(color, color))
        } else {
            Brush.verticalGradient(currentGradient.colors)
        }
    }

    val currentEffectiveTextColor = remember(textColorHex) {
        try { Color(AndroidColor.parseColor(textColorHex)) } catch (e: Exception) { if (isDark) Color.White else Color(0xFF111827) }
    }

    val currentHighlightColor = remember(isHighlight, highlightColorHex) {
        if (isHighlight) {
            try { Color(AndroidColor.parseColor(highlightColorHex)).copy(alpha = 0.35f) } catch (e: Exception) { Color(0xFFFFD60A).copy(alpha = 0.35f) }
        } else {
            Color.Transparent
        }
    }

    val selectedFont = when (selectedFontFamilyName) {
        "Serif" -> FontFamily.Serif
        "Mono" -> MonoFontFamily
        "Cursive" -> FontFamily.Cursive
        else -> InterFontFamily
    }

    val selectedTextAlign = when (textAlign) {
        "center" -> TextAlign.Center
        "right" -> TextAlign.Right
        else -> TextAlign.Left
    }

    val headlineStyle = TextStyle(
        fontFamily = selectedFont,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = currentEffectiveTextColor,
        textAlign = selectedTextAlign
    )

    val bodyTextStyle = TextStyle(
        fontFamily = selectedFont,
        fontSize = when (headingLevel) {
            2 -> 20.sp
            3 -> 17.sp
            else -> 15.sp
        },
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = when {
            isUnderline && isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
            isUnderline -> TextDecoration.Underline
            isStrikethrough -> TextDecoration.LineThrough
            else -> TextDecoration.None
        },
        color = currentEffectiveTextColor,
        textAlign = selectedTextAlign,
        background = currentHighlightColor,
        lineHeight = when (headingLevel) {
            2 -> 28.sp
            3 -> 24.sp
            else -> 22.sp
        },
        drawStyle = if (isStroke) Stroke(
            miter = 8f,
            width = 2.5f,
            join = StrokeJoin.Round
        ) else androidx.compose.ui.graphics.drawscope.Fill
    )

    // Calculate word & character metrics
    val totalCharacters = (headlineText.length + bodyContent.length)
    val wordCount = remember(bodyContent, headlineText) {
        val allText = "$headlineText $bodyContent".trim()
        if (allText.isEmpty()) 0 else allText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val estimatedReadTime = maxOf(1, (wordCount / 40))

    // Handle Publish Action
    val onPublishAction: () -> Unit = {
        if (bodyContent.isNotBlank() || headlineText.isNotBlank()) {
            isPublishing = true
            val fullContent = buildString {
                if (headlineText.isNotBlank()) {
                    append("# ").append(headlineText.trim()).append("\n\n")
                }
                append(bodyContent.trim())
            }

            val formatState = StatusFormatState(
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                isStroke = isStroke,
                isHighlight = isHighlight,
                headingLevel = headingLevel,
                textColorHex = textColorHex,
                backgroundColorHex = if (isCustomSolidBg) customBgColorHex else String.format("#%08X", (0xFFFFFFFF and currentGradient.colors.first().value.toLong())),
                title = headlineText.trim(),
                tag = selectedCategory,
                mood = selectedMood,
                fontFamily = selectedFontFamilyName,
                textAlign = textAlign,
                isQuote = isQuoteMode,
                gradientPreset = if (!isCustomSolidBg) selectedGradientId else "None",
                highlightColorHex = highlightColorHex
            )

            val jsonFormat = try {
                StoryFormatJson.encodeToString(formatState)
            } catch (e: Exception) {
                "{}"
            }

            storyViewModel.publishTextStory(
                textContent = fullContent,
                textFormat = jsonFormat,
                backgroundColor = formatState.backgroundColorHex,
                audienceType = audienceType,
                customAudienceIds = emptyList()
            )

            isPublishing = false
            ToastManager.showToast("Story published! ✨")
            onStoryPosted()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColors.bg)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clean Close icon button (NO background)
                IconButton(
                    onClick = {
                        if (bodyContent.isNotBlank() || headlineText.isNotBlank()) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Close",
                        tint = themeColors.textMain,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Story Writer",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textMain,
                        maxLines = 1
                    )
                    Text(
                        text = "${wordCount} words • ${estimatedReadTime}m read",
                        fontSize = 11.sp,
                        color = themeColors.textMuted,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.width(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Templates (NO background)
                    IconButton(
                        onClick = { showTemplatesSheet = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        AiWaveSphereIcon(
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Preview Toggle (NO background)
                    IconButton(
                        onClick = { showLivePreview = !showLivePreview },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Eye,
                            contentDescription = "Preview",
                            tint = if (showLivePreview) themeColors.primary else themeColors.textMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // Primary Liquid Glass Publish Button
                    LiquidGlassPrimaryButton(
                        onClick = onPublishAction,
                        enabled = (bodyContent.isNotBlank() || headlineText.isNotBlank()) && !isPublishing,
                        isSmall = true
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Publish", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                            Icon(Lucide.Send, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        },
        containerColor = themeColors.bg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Category & Mood Selector Bar (Compact, clean outline chips, no secondary cards)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Topic Chips with clean outline style
                StoryCategories.forEach { cat ->
                    val isSelected = selectedCategory == cat.name
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) themeColors.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .border(
                                width = 0.5.dp,
                                color = if (isSelected) themeColors.primary else themeColors.glassBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedCategory = if (isSelected) "" else cat.name
                            }
                            .padding(horizontal = 8.dp, vertical = 4.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = cat.name,
                            tint = if (isSelected) themeColors.primary else themeColors.textMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = cat.name,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) themeColors.primary else themeColors.textMain
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(themeColors.glassBorder)
                )

                // Mood Chips with clean outline style
                StoryMoods.forEach { mood ->
                    val isSelected = selectedMood == mood.name
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) themeColors.success.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = 0.5.dp,
                                color = if (isSelected) themeColors.success else themeColors.glassBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedMood = if (isSelected) "" else mood.name
                            }
                            .padding(horizontal = 8.dp, vertical = 4.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = mood.icon,
                            contentDescription = mood.name,
                            tint = if (isSelected) themeColors.success else themeColors.textMuted,
                            modifier = Modifier.size(11.5.dp)
                        )
                        Text(
                            text = mood.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) themeColors.success else themeColors.textMuted
                        )
                    }
                }
            }

            // The Hero Story Canvas (Flat, clean, NO shadows, compact padding)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(canvasBackgroundBrush)
                    .border(0.5.dp, themeColors.glassBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Meta: Category & Mood Badges inside Canvas
                    if (selectedCategory.isNotBlank() || selectedMood.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedCatItem = StoryCategories.find { it.name == selectedCategory }
                            if (selectedCatItem != null) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(currentEffectiveTextColor.copy(alpha = 0.12f))
                                        .clickable { selectedCategory = "" }
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = selectedCatItem.icon,
                                        contentDescription = null,
                                        tint = currentEffectiveTextColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = selectedCatItem.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = currentEffectiveTextColor
                                    )
                                    Icon(
                                        Lucide.X,
                                        contentDescription = "Remove",
                                        tint = currentEffectiveTextColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }

                            val selectedMoodItem = StoryMoods.find { it.name == selectedMood }
                            if (selectedMoodItem != null) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(currentEffectiveTextColor.copy(alpha = 0.12f))
                                        .clickable { selectedMood = "" }
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = selectedMoodItem.icon,
                                        contentDescription = null,
                                        tint = currentEffectiveTextColor.copy(alpha = 0.85f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = selectedMoodItem.name,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = currentEffectiveTextColor.copy(alpha = 0.85f)
                                    )
                                    Icon(
                                        Lucide.X,
                                        contentDescription = "Remove",
                                        tint = currentEffectiveTextColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Headline / Title Field
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (headlineText.isEmpty()) {
                            Text(
                                text = "Catchy Title / Headline...",
                                style = headlineStyle.copy(color = currentEffectiveTextColor.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        BasicTextField(
                            value = headlineText,
                            onValueChange = { if (it.length <= 100) headlineText = it },
                            textStyle = headlineStyle,
                            cursorBrush = SolidColor(currentEffectiveTextColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Hairline divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(currentEffectiveTextColor.copy(alpha = 0.12f))
                    )

                    Spacer(Modifier.height(6.dp))

                    // Scrollable Body Text Canvas Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (isQuoteMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(currentEffectiveTextColor.copy(alpha = 0.05f))
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(themeColors.primary, themeColors.primaryHover)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "\"",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = currentEffectiveTextColor.copy(alpha = 0.6f),
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(end = 5.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    if (bodyContent.isEmpty()) {
                                        Text(
                                            text = "Type your memorable quote or thought...",
                                            style = bodyTextStyle.copy(
                                                fontStyle = FontStyle.Italic,
                                                color = currentEffectiveTextColor.copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    BasicTextField(
                                        value = bodyContent,
                                        onValueChange = { if (it.length <= 600) bodyContent = it },
                                        textStyle = bodyTextStyle.copy(fontStyle = FontStyle.Italic),
                                        cursorBrush = SolidColor(currentEffectiveTextColor),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                ) {
                                    if (bodyContent.isEmpty()) {
                                        Text(
                                            text = "Start writing your story or article here...\n\nUse the toolbar below to customize typography, highlight ideas, change gradients, and add mood tags.",
                                            style = bodyTextStyle.copy(color = currentEffectiveTextColor.copy(alpha = 0.35f)),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    BasicTextField(
                                        value = bodyContent,
                                        onValueChange = { if (it.length <= 600) bodyContent = it },
                                        textStyle = bodyTextStyle,
                                        cursorBrush = SolidColor(currentEffectiveTextColor),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Footer Meta inside canvas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val user = FirebaseAuth.getInstance().currentUser
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IosAvatarView(
                                name = user?.displayName ?: "Author",
                                avatarUrl = user?.photoUrl?.toString() ?: "",
                                size = 18.dp
                            )
                            Text(
                                text = user?.displayName?.ifBlank { "You" } ?: "You",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = currentEffectiveTextColor.copy(alpha = 0.75f)
                            )
                        }

                        Text(
                            text = "${bodyContent.length + headlineText.length}/600",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = currentEffectiveTextColor.copy(alpha = 0.55f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Clean Compact Dock Toolbar (Flat, NO shadows, NO nested card clutter) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColors.glassBg)
                    .border(0.5.dp, themeColors.glassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Toolbar Segmented Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0x15FFFFFF) else Color(0x0A000000))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StoryEditorToolbarTab.values().forEach { tab ->
                        val isSelected = activeToolbarTab == tab
                        val (icon, label) = when (tab) {
                            StoryEditorToolbarTab.TYPOGRAPHY -> Lucide.Type to "Format"
                            StoryEditorToolbarTab.COLORS -> Lucide.Palette to "Themes"
                            StoryEditorToolbarTab.STRUCTURE -> Lucide.LayoutTemplate to "Style"
                            StoryEditorToolbarTab.AUDIENCE -> Lucide.Globe to "Audience"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) themeColors.primary else Color.Transparent)
                                .clickable { activeToolbarTab = tab }
                                .padding(vertical = 4.5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color.White else themeColors.textMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else themeColors.textMuted
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(5.dp))

                // Toolbar Sub-controls according to active tab (NO icon backgrounds)
                when (activeToolbarTab) {
                    StoryEditorToolbarTab.TYPOGRAPHY -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bold, Italic, Underline, Strikethrough (NO backgrounds, pure clean icons)
                            ToolbarCleanIconButton(icon = Lucide.Bold, isActive = isBold, contentDescription = "Bold") { isBold = !isBold }
                            ToolbarCleanIconButton(icon = Lucide.Italic, isActive = isItalic, contentDescription = "Italic") { isItalic = !isItalic }
                            ToolbarCleanIconButton(icon = Lucide.Underline, isActive = isUnderline, contentDescription = "Underline") { isUnderline = !isUnderline }
                            ToolbarCleanIconButton(icon = Lucide.Strikethrough, isActive = isStrikethrough, contentDescription = "Strikethrough") { isStrikethrough = !isStrikethrough }

                            VerticalLineDivider()

                            // Headings
                            ToolbarCleanTextButton(label = "H1", isActive = headingLevel == 2) {
                                headingLevel = if (headingLevel == 2) 1 else 2
                            }
                            ToolbarCleanTextButton(label = "H2", isActive = headingLevel == 3) {
                                headingLevel = if (headingLevel == 3) 1 else 3
                            }

                            VerticalLineDivider()

                            // Highlight with swatches
                            ToolbarCleanIconButton(icon = Lucide.Highlighter, isActive = isHighlight, contentDescription = "Highlight") {
                                isHighlight = !isHighlight
                            }

                            if (isHighlight) {
                                listOf("#FFD60A", "#34C759", "#5AC8FA", "#FF2D55", "#AF52DE").forEach { hex ->
                                    HighlightColorDot(hex = hex, isSelected = highlightColorHex == hex) {
                                        highlightColorHex = hex
                                    }
                                }
                            }

                            VerticalLineDivider()

                            // Font Picker Chips
                            listOf("Sans" to "Inter", "Serif" to "Serif", "Mono" to "Mono", "Cursive" to "Script").forEach { (key, display) ->
                                ToolbarCleanChip(label = display, isSelected = selectedFontFamilyName == key) {
                                    selectedFontFamilyName = key
                                }
                            }
                        }
                    }

                    StoryEditorToolbarTab.COLORS -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Gradient Themes Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StoryGradientThemes.forEach { theme ->
                                    val isSelected = !isCustomSolidBg && selectedGradientId == theme.id
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Brush.linearGradient(theme.colors))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) themeColors.primary else themeColors.glassBorder,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                isCustomSolidBg = false
                                                selectedGradientId = theme.id
                                                textColorHex = String.format("#%08X", (0xFFFFFFFF and theme.defaultTextColor.value.toLong()))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Lucide.Check, null, tint = theme.defaultTextColor, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }

                            // Text Color Swatches Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Color:", fontSize = 10.5.sp, color = themeColors.textMuted)
                                listOf("#FFFFFF", "#111827", "#FF3B30", "#34C759", "#007AFF", "#FFCC00", "#FF2D55", "#AF52DE", "#5AC8FA").forEach { hex ->
                                    val color = try { Color(AndroidColor.parseColor(hex)) } catch (e: Exception) { Color.White }
                                    Box(
                                        modifier = Modifier
                                            .size(19.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(color)
                                            .border(
                                                width = if (textColorHex == hex) 1.5.dp else 0.5.dp,
                                                color = if (textColorHex == hex) themeColors.primary else themeColors.glassBorder,
                                                shape = RoundedCornerShape(5.dp)
                                            )
                                            .clickable { textColorHex = hex }
                                    )
                                }
                            }
                        }
                    }

                    StoryEditorToolbarTab.STRUCTURE -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quote toggle
                            ToolbarCleanToggle(icon = Lucide.Quote, label = "Quote", isActive = isQuoteMode) {
                                isQuoteMode = !isQuoteMode
                            }

                            VerticalLineDivider()

                            // Text Alignments (NO icon backgrounds)
                            ToolbarCleanIconButton(icon = Lucide.AlignLeft, isActive = textAlign == "left", contentDescription = "Align Left") {
                                textAlign = "left"
                            }
                            ToolbarCleanIconButton(icon = Lucide.AlignCenter, isActive = textAlign == "center", contentDescription = "Align Center") {
                                textAlign = "center"
                            }
                            ToolbarCleanIconButton(icon = Lucide.AlignRight, isActive = textAlign == "right", contentDescription = "Align Right") {
                                textAlign = "right"
                            }

                            VerticalLineDivider()

                            // Outline / Stroke
                            ToolbarCleanToggle(icon = Lucide.PenTool, label = "Outline", isActive = isStroke) {
                                isStroke = !isStroke
                            }
                        }
                    }

                    StoryEditorToolbarTab.AUDIENCE -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AudienceCleanOptionChip(
                                icon = Lucide.Globe,
                                label = "Public",
                                isSelected = audienceType == "public"
                            ) { audienceType = "public" }

                            AudienceCleanOptionChip(
                                icon = Lucide.Users,
                                label = "Followers",
                                isSelected = audienceType == "followers"
                            ) { audienceType = "followers" }

                            AudienceCleanOptionChip(
                                icon = Lucide.Lock,
                                label = "Close Friends",
                                isSelected = audienceType == "custom"
                            ) { audienceType = "custom" }
                        }
                    }
                }
            }
        }
    }

    // ── Live Story Preview Dialog (Flat, NO shadows) ─────────────────────────
    if (showLivePreview) {
        Dialog(
            onDismissRequest = { showLivePreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            val user = FirebaseAuth.getInstance().currentUser
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showLivePreview = false },
                contentAlignment = Alignment.Center
            ) {
                // Phone Frame Card (Flat, clean, NO shadows)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(canvasBackgroundBrush)
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Story Progress Bars Simulation
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                        }

                        // Top User Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IosAvatarView(
                                    name = user?.displayName ?: "You",
                                    avatarUrl = user?.photoUrl?.toString() ?: "",
                                    size = 32.dp
                                )
                                Column {
                                    Text(
                                        text = user?.displayName?.ifBlank { "Your Story" } ?: "Your Story",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = currentEffectiveTextColor
                                    )
                                    Text(
                                        text = "Just now • Preview",
                                        fontSize = 11.sp,
                                        color = currentEffectiveTextColor.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showLivePreview = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Lucide.X, contentDescription = "Close Preview", tint = currentEffectiveTextColor, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Render Content
                        val selectedCatItem = StoryCategories.find { it.name == selectedCategory }
                        if (selectedCatItem != null) {
                            Row(
                                modifier = Modifier
                                    .align(if (textAlign == "center") Alignment.CenterHorizontally else Alignment.Start)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(currentEffectiveTextColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = selectedCatItem.icon,
                                    contentDescription = null,
                                    tint = currentEffectiveTextColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = selectedCatItem.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentEffectiveTextColor
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        if (headlineText.isNotBlank()) {
                            Text(
                                text = headlineText,
                                style = headlineStyle,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        if (isQuoteMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(currentEffectiveTextColor.copy(alpha = 0.08f))
                                    .border(
                                        1.dp,
                                        Brush.verticalGradient(listOf(themeColors.primary, themeColors.primaryHover)),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("\"", fontSize = 24.sp, fontWeight = FontWeight.Black, color = currentEffectiveTextColor)
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = bodyContent.ifBlank { "Your quote will appear here." },
                                    style = bodyTextStyle.copy(fontStyle = FontStyle.Italic),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Text(
                                text = bodyContent.ifBlank { "Your story content will appear here." },
                                style = bodyTextStyle,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        val selectedMoodItem = StoryMoods.find { it.name == selectedMood }
                        if (selectedMoodItem != null) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .align(if (textAlign == "center") Alignment.CenterHorizontally else Alignment.Start)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(currentEffectiveTextColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = selectedMoodItem.icon,
                                    contentDescription = null,
                                    tint = currentEffectiveTextColor.copy(alpha = 0.85f),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = selectedMoodItem.name,
                                    fontSize = 10.5.sp,
                                    color = currentEffectiveTextColor.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Bottom Action Simulation Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Send a message...", fontSize = 12.sp, color = currentEffectiveTextColor.copy(alpha = 0.6f))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Lucide.Heart, null, tint = currentEffectiveTextColor, modifier = Modifier.size(16.dp))
                                Icon(Lucide.Send, null, tint = currentEffectiveTextColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Templates & Inspiration Sheet (Flat, clean, NO shadows) ───────────────
    if (showTemplatesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTemplatesSheet = false },
            containerColor = themeColors.glassBg,
            dragHandle = { BottomSheetDefaults.DragHandle(color = themeColors.glassBorder) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AiWaveSphereIcon(modifier = Modifier.size(18.dp))
                        Text(
                            text = "Prompts & Templates",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.textMain
                        )
                    }
                    IconButton(
                        onClick = { showTemplatesSheet = false },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Lucide.X, null, tint = themeColors.textMain, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                StoryPrompts.forEach { prompt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(themeColors.glassBgHover)
                            .border(0.5.dp, themeColors.glassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                headlineText = prompt.title
                                bodyContent = prompt.promptBody
                                showTemplatesSheet = false
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(prompt.icon, null, tint = themeColors.primary, modifier = Modifier.size(15.dp).padding(top = 1.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prompt.title, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = themeColors.textMain)
                            Spacer(Modifier.height(2.dp))
                            Text(prompt.promptBody, fontSize = 11.5.sp, color = themeColors.textMuted)
                        }
                    }
                }
            }
        }
    }

    // ── Discard Changes Confirmation Dialog ────────────────────────────────
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = themeColors.glassBg,
            shape = RoundedCornerShape(14.dp),
            title = {
                Text("Discard story draft?", color = themeColors.textMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text("If you go back now, your current writing and styling will be lost.", color = themeColors.textMuted, fontSize = 13.sp)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = themeColors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                LiquidGlassSecondaryButton(
                    text = "Keep Editing",
                    onClick = { showDiscardDialog = false },
                    isSmall = true
                )
            }
        )
    }
}

// ── Clean Toolbar Sub-components (NO backgrounds for icons, Flat) ────────────

@Composable
private fun ToolbarCleanIconButton(
    icon: ImageVector,
    isActive: Boolean,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    val themeColors = LocalOscellionColors.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) themeColors.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) themeColors.primary else themeColors.textMuted,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun ToolbarCleanTextButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val themeColors = LocalOscellionColors.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) themeColors.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) themeColors.primary else themeColors.textMuted
        )
    }
}

@Composable
private fun ToolbarCleanToggle(
    icon: ImageVector? = null,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val themeColors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) themeColors.primary.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 0.5.dp,
                color = if (isActive) themeColors.primary else themeColors.glassBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) themeColors.primary else themeColors.textMuted,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) themeColors.primary else themeColors.textMuted
        )
    }
}

@Composable
private fun ToolbarCleanChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeColors = LocalOscellionColors.current
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) themeColors.primary.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 0.5.dp,
                color = if (isSelected) themeColors.primary else themeColors.glassBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) themeColors.primary else themeColors.textMuted
        )
    }
}

@Composable
private fun HighlightColorDot(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeColors = LocalOscellionColors.current
    val color = try { Color(AndroidColor.parseColor(hex)) } catch (e: Exception) { Color.Yellow }
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) themeColors.primary else themeColors.glassBorder,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun AudienceCleanOptionChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeColors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) themeColors.primary.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 0.5.dp,
                color = if (isSelected) themeColors.primary else themeColors.glassBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) themeColors.primary else themeColors.textMuted,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) themeColors.primary else themeColors.textMuted
        )
    }
}

@Composable
private fun VerticalLineDivider() {
    val themeColors = LocalOscellionColors.current
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .height(14.dp)
            .background(themeColors.glassBorder)
    )
}
