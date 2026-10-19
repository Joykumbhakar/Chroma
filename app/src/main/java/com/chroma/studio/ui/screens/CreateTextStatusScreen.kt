package com.chroma.studio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.IosSegmentedControl
import com.chroma.studio.ui.components.MarkdownText
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.ChromaHeaderActionButton
import com.chroma.studio.ui.components.LiquidGlassCard
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*

private val COLOR_PRESETS = listOf(
    Color(0xFF007AFF), // Blue
    Color(0xFFFF2D55), // Pink/Red
    Color(0xFF34C759), // Green
    Color(0xFFFF9500), // Orange
    Color(0xFFAF52DE), // Purple
    Color(0xFF5856D6), // Indigo
    Color(0xFF00C7BE), // Teal
    Color(0xFFFF3B30)  // Coral
)

@Composable
fun CreateTextStatusScreen(
    onDismiss: () -> Unit,
    onPost: (String, String) -> Unit // (markdownContent, privacy)
) {
    val colors = LocalOscellionColors.current
    var textValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Edit, 1 = Preview
    var privacy by remember { mutableStateOf("Followers") }
    var showPrivacyMenu by remember { mutableStateOf(false) }
    var showColorPalette by remember { mutableStateOf(false) }

    fun insertFormat(prefix: String, suffix: String) {
        val start = textValue.selection.start
        val end = textValue.selection.end
        val text = textValue.text

        val selectedPart = if (start != end) text.substring(start, end) else ""
        val newText = text.substring(0, start) + prefix + selectedPart + suffix + text.substring(end)
        val newCursor = if (start == end) start + prefix.length else start + prefix.length + selectedPart.length + suffix.length
        textValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor, newCursor)
        )
    }

    val canPost = textValue.text.isNotBlank()

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            ChromaScreenHeader(
                title = "Oscellion",
                badgeText = "New Status",
                showBackButton = true,
                onBack = onDismiss,
                actions = {
                    // Privacy Pill Dropdown
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.glassBgHover)
                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showPrivacyMenu = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (privacy == "Followers") Lucide.Users else Lucide.Globe,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = privacy,
                                color = colors.textMain,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Lucide.ChevronDown,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showPrivacyMenu,
                            onDismissRequest = { showPrivacyMenu = false },
                            modifier = Modifier
                                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.15f))
                                .clip(RoundedCornerShape(24.dp))
                                .background(colors.glassBg)
                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(24.dp))
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Lucide.Users, null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
                                },
                                text = { Text("Followers Only", color = colors.textMain, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    privacy = "Followers"
                                    showPrivacyMenu = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Lucide.Globe, null, tint = Color(0xFF34C759), modifier = Modifier.size(18.dp))
                                },
                                text = { Text("Public", color = colors.textMain, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    privacy = "Public"
                                    showPrivacyMenu = false
                                }
                            )
                        }
                    }

                    // Post Button
                    ChromaHeaderActionButton(
                        text = "Post",
                        icon = Lucide.Send,
                        enabled = canPost,
                        onClick = {
                            if (canPost) {
                                onPost(textValue.text.trim(), privacy)
                                onDismiss()
                            }
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Edit / Preview Segmented Control
            IosSegmentedControl(
                items = listOf("Edit", "Preview"),
                selectedIndex = selectedTab,
                onItemSelected = { selectedTab = it },
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Main Editor / Preview Card
            LiquidGlassCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = colors.glassBgHover,
                borderColor = colors.glassBorder,
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    if (selectedTab == 1) {
                        // Preview Mode
                        if (textValue.text.isBlank()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Nothing to preview yet",
                                    color = colors.textMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            MarkdownText(
                                markdown = textValue.text,
                                modifier = Modifier.fillMaxSize(),
                                defaultColor = colors.textMain
                            )
                        }
                    } else {
                        // Edit Mode
                        BasicTextField(
                            value = textValue,
                            onValueChange = { if (it.text.length <= 700) textValue = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 24.dp),
                            textStyle = TextStyle(
                                color = colors.textMain,
                                fontSize = 18.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color(0xFF007AFF)),
                            decorationBox = { innerTextField ->
                                if (textValue.text.isEmpty()) {
                                    Text(
                                        "What's on your mind?",
                                        color = colors.textMuted.copy(alpha = 0.55f),
                                        fontSize = 18.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        // Word / Character Counter
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.bg.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${textValue.text.length}/700",
                                color = if (textValue.text.length > 650) Color(0xFFFF3B30) else colors.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Quick Color Presets Row (when palette toggled)
            AnimatedVisibility(
                visible = showColorPalette && selectedTab == 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = colors.glassBgHover,
                    borderColor = colors.glassBorder,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(COLOR_PRESETS) { color ->
                            val hex = String.format("#%06X", (0xFFFFFF and color.value.toInt()))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                    .clickable {
                                        insertFormat("[color:$hex]", "[/color]")
                                        showColorPalette = false
                                    }
                            )
                        }
                    }
                }
            }

            // Rich Formatting Toolbar (Edit Mode Only)
            if (selectedTab == 0) {
                Spacer(Modifier.height(10.dp))
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = colors.glassBgHover,
                    borderColor = colors.glassBorder,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FormatToolbarButton(
                            icon = Lucide.Bold,
                            tooltip = "Bold",
                            colors = colors,
                            onClick = { insertFormat("**", "**") }
                        )
                        FormatToolbarButton(
                            icon = Lucide.Italic,
                            tooltip = "Italic",
                            colors = colors,
                            onClick = { insertFormat("*", "*") }
                        )
                        FormatToolbarButton(
                            icon = Lucide.Underline,
                            tooltip = "Underline",
                            colors = colors,
                            onClick = { insertFormat("__", "__") }
                        )
                        FormatToolbarButton(
                            icon = Lucide.Highlighter,
                            tooltip = "Highlight",
                            colors = colors,
                            onClick = { insertFormat("==", "==") }
                        )
                        FormatToolbarButton(
                            icon = Lucide.Heading,
                            tooltip = "Heading",
                            colors = colors,
                            onClick = { insertFormat("# ", "") }
                        )
                        FormatToolbarButton(
                            icon = Lucide.Quote,
                            tooltip = "Quote",
                            colors = colors,
                            onClick = { insertFormat("> ", "") }
                        )
                        FormatToolbarButton(
                            icon = Lucide.Palette,
                            tooltip = "Color",
                            colors = colors,
                            tintOverride = if (showColorPalette) Color(0xFF007AFF) else null,
                            onClick = { showColorPalette = !showColorPalette }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tooltip: String,
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    tintOverride: Color? = null,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = tintOverride ?: colors.textMain,
            modifier = Modifier.size(18.dp)
        )
    }
}
