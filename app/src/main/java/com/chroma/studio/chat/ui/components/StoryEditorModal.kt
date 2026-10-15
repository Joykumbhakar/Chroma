package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.data.PollData
import com.chroma.studio.chat.data.PollOption
import com.chroma.studio.chat.data.Story
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors

data class StoryGradientPreset(
    val title: String,
    val colors: List<Color>
)

enum class TextHighlightMode {
    NONE,
    TRANSLUCENT_BLACK,
    SOLID_WHITE,
    ACCENT_BLUE
}

/**
 * Flagship Apple iOS 18 & Instagram Stories Creator & Editor.
 * Includes interactive text formatting, typography engine, floating stickers,
 * smooth gradient backdrops, and safe area window padding.
 */
@Composable
fun StoryEditorModal(
    onDismiss: () -> Unit,
    onPublishStory: (Story) -> Unit
) {
    val colors = LocalChatColors.current

    // Gradient Background Presets
    val gradients = remember {
        listOf(
            StoryGradientPreset("Ocean Blue", listOf(Color(0xFF007AFF), Color(0xFF005ECB), Color(0xFF003882))),
            StoryGradientPreset("Sunset Glow", listOf(Color(0xFFFF5E3A), Color(0xFFFF2A68), Color(0xFF9B51E0))),
            StoryGradientPreset("Cyberpunk Neon", listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF3B82F6))),
            StoryGradientPreset("Midnight Nebula", listOf(Color(0xFF181B34), Color(0xFF241C48), Color(0xFF0F172A))),
            StoryGradientPreset("Emerald Focus", listOf(Color(0xFF059669), Color(0xFF10B981), Color(0xFF047857))),
            StoryGradientPreset("Rose Gold", listOf(Color(0xFFFB7185), Color(0xFFF43F5E), Color(0xFF881337))),
            StoryGradientPreset("Warm Amber", listOf(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFF7C2D12))),
            StoryGradientPreset("Obsidian Pure Dark", listOf(Color(0xFF1C1C1E), Color(0xFF141416), Color(0xFF000000)))
        )
    }

    val sampleImages = remember {
        listOf(
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1518791841217-8f162f1e1131?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=800&q=80"
        )
    }

    var selectedGradientIndex by remember { mutableIntStateOf(0) }
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var storyCaption by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<String?>("San Francisco, CA") }
    var selectedMusic by remember { mutableStateOf<String?>("Odesza • A Moment Apart") }
    var storyPoll by remember { mutableStateOf<PollData?>(null) }
    var isAddingSticker by remember { mutableStateOf(false) }

    // Typography & Formatting State
    var fontIndex by remember { mutableIntStateOf(0) }
    val fontFamilies = listOf(FontFamily.Default, FontFamily.Serif, FontFamily.Monospace, FontFamily.Cursive)
    val fontNames = listOf("Classic", "Serif", "Code", "Script")
    var highlightMode by remember { mutableStateOf(TextHighlightMode.NONE) }
    var textAlign by remember { mutableStateOf(TextAlign.Center) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── 1. Story Canvas Background (Photo or Multi-Stop Gradient) ───────
        if (selectedPhotoUrl != null) {
            AsyncImage(
                model = selectedPhotoUrl,
                contentDescription = "Selected Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.30f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(gradients[selectedGradientIndex].colors)
                    )
            )
        }

        // ── 2. Top and Bottom Vignette Scrims ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.60f),
                        0.6f to Color.Black.copy(alpha = 0.20f),
                        1.0f to Color.Transparent
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.4f to Color.Black.copy(alpha = 0.40f),
                        1.0f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )

        // ── 3. Center Text & Interactive Canvas Content ────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 100.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Floating Apple Music Sticker Pill (Light Theme)
            if (selectedMusic != null) {
                Row(
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.30f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(0.5.dp, Color(0xFFE5E5EA), RoundedCornerShape(20.dp))
                        .clickable {
                            SoundManager.playReaction()
                            selectedMusic = null
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PhosphorWaveform(tint = Color(0xFFFF2D55), size = 15.dp)
                    Text(
                        text = selectedMusic ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    PhosphorCloseX(tint = Color(0xFF8E8E93), size = 11.dp)
                }
                Spacer(Modifier.height(10.dp))
            }

            // Floating Location Sticker Pill (Light Theme)
            if (selectedLocation != null) {
                Row(
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.30f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(0.5.dp, Color(0xFFE5E5EA), RoundedCornerShape(20.dp))
                        .clickable {
                            SoundManager.playReaction()
                            selectedLocation = null
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LucideMapPin(tint = Color(0xFFFF3B30), size = 14.dp)
                    Text(
                        text = selectedLocation ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    PhosphorCloseX(tint = Color(0xFF8E8E93), size = 11.dp)
                }
                Spacer(Modifier.height(10.dp))
            }

            // Floating Poll Sticker Widget (Light Theme)
            if (storyPoll != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.25f))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(0.5.dp, Color(0xFFE5E5EA), RoundedCornerShape(18.dp))
                        .clickable {
                            SoundManager.playReaction()
                            storyPoll = null
                        }
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LucideBarChart2(tint = colors.iosBlue, size = 16.dp)
                                Text(
                                    text = storyPoll?.question ?: "Poll",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E)
                                )
                            }
                            PhosphorCloseX(tint = Color(0xFF8E8E93), size = 12.dp)
                        }

                        storyPoll?.options?.forEach { opt ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF2F2F7))
                                    .padding(vertical = 8.dp, horizontal = 10.dp)
                            ) {
                                Text(
                                    text = opt.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF007AFF)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Text Input Box with Dynamic Highlight Container
            val textContainerModifier = when (highlightMode) {
                TextHighlightMode.NONE -> Modifier.fillMaxWidth()
                TextHighlightMode.TRANSLUCENT_BLACK -> Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                TextHighlightMode.SOLID_WHITE -> Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                TextHighlightMode.ACCENT_BLUE -> Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.iosBlue)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            }

            val captionTextColor = when (highlightMode) {
                TextHighlightMode.SOLID_WHITE -> Color.Black
                else -> Color.White
            }

            Box(
                modifier = textContainerModifier,
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = storyCaption,
                    onValueChange = { storyCaption = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilies[fontIndex],
                        color = captionTextColor,
                        textAlign = textAlign,
                        lineHeight = 33.sp,
                        shadow = if (highlightMode == TextHighlightMode.NONE) {
                            Shadow(
                                color = Color.Black.copy(alpha = 0.75f),
                                offset = Offset(0f, 2f),
                                blurRadius = 8f
                            )
                        } else null
                    ),
                    cursorBrush = SolidColor(if (highlightMode == TextHighlightMode.SOLID_WHITE) Color.Black else Color.White),
                    decorationBox = { inner ->
                        if (storyCaption.isEmpty()) {
                            Text(
                                text = "Tap to type a thought...",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = fontFamilies[fontIndex],
                                color = if (highlightMode == TextHighlightMode.SOLID_WHITE) {
                                    Color.Black.copy(alpha = 0.4f)
                                } else {
                                    Color.White.copy(alpha = 0.70f)
                                },
                                textAlign = textAlign,
                                modifier = Modifier.fillMaxWidth(),
                                style = TextStyle(
                                    shadow = if (highlightMode == TextHighlightMode.NONE) {
                                        Shadow(
                                            color = Color.Black.copy(alpha = 0.75f),
                                            offset = Offset(0f, 2f),
                                            blurRadius = 8f
                                        )
                                    } else null
                                )
                            )
                        }
                        inner()
                    }
                )
            }
        }

        // ── 4. Apple iOS Top Toolbar (Frosted Glass Tools) ─────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        SoundManager.playReaction()
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.offset(x = 0.5.dp, y = 1.2.dp)) {
                    PhosphorCloseX(tint = Color.Black.copy(alpha = 0.70f), size = 20.dp)
                }
                PhosphorCloseX(tint = Color.White, size = 20.dp)
            }

            // Right Action Tools
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Typography Font Selector Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            SoundManager.playReaction()
                            fontIndex = (fontIndex + 1) % fontFamilies.size
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aa",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilies[fontIndex],
                        color = Color.White,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.70f),
                                offset = Offset(0.5f, 1.2f),
                                blurRadius = 6f
                            )
                        )
                    )
                }

                // Text Alignment Toggle Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            SoundManager.playReaction()
                            textAlign = when (textAlign) {
                                TextAlign.Center -> TextAlign.Start
                                TextAlign.Start -> TextAlign.End
                                else -> TextAlign.Center
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Shadow layer
                    Column(
                        horizontalAlignment = when (textAlign) {
                            TextAlign.Start -> Alignment.Start
                            TextAlign.End -> Alignment.End
                            else -> Alignment.CenterHorizontally
                        },
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.width(17.dp).offset(x = 0.5.dp, y = 1.2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black.copy(alpha = 0.70f), RoundedCornerShape(1.dp)))
                        Box(modifier = Modifier.fillMaxWidth(0.65f).height(2.dp).background(Color.Black.copy(alpha = 0.70f), RoundedCornerShape(1.dp)))
                        Box(modifier = Modifier.fillMaxWidth(0.85f).height(2.dp).background(Color.Black.copy(alpha = 0.70f), RoundedCornerShape(1.dp)))
                    }
                    // Foreground layer
                    Column(
                        horizontalAlignment = when (textAlign) {
                            TextAlign.Start -> Alignment.Start
                            TextAlign.End -> Alignment.End
                            else -> Alignment.CenterHorizontally
                        },
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.width(17.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.White, RoundedCornerShape(1.dp)))
                        Box(modifier = Modifier.fillMaxWidth(0.65f).height(2.dp).background(Color.White, RoundedCornerShape(1.dp)))
                        Box(modifier = Modifier.fillMaxWidth(0.85f).height(2.dp).background(Color.White, RoundedCornerShape(1.dp)))
                    }
                }

                // Text Highlight Mode Button (A box icon)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            SoundManager.playReaction()
                            highlightMode = when (highlightMode) {
                                TextHighlightMode.NONE -> TextHighlightMode.TRANSLUCENT_BLACK
                                TextHighlightMode.TRANSLUCENT_BLACK -> TextHighlightMode.SOLID_WHITE
                                TextHighlightMode.SOLID_WHITE -> TextHighlightMode.ACCENT_BLUE
                                TextHighlightMode.ACCENT_BLUE -> TextHighlightMode.NONE
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (highlightMode != TextHighlightMode.NONE) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (highlightMode) {
                                        TextHighlightMode.SOLID_WHITE -> Color.White
                                        TextHighlightMode.ACCENT_BLUE -> colors.iosBlue
                                        else -> Color.Black.copy(alpha = 0.70f)
                                    }
                                )
                                .border(1.dp, Color.White, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (highlightMode == TextHighlightMode.SOLID_WHITE) Color.Black else Color.White
                            )
                        }
                    } else {
                        Text(
                            text = "A",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.70f),
                                    offset = Offset(0.5f, 1.2f),
                                    blurRadius = 6f
                                )
                            )
                        )
                    }
                }

                // Add Sticker Button (Music, Location, Poll, Emojis)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            SoundManager.playReaction()
                            isAddingSticker = !isAddingSticker
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.offset(x = 0.5.dp, y = 1.2.dp)) {
                        PhosphorSmileyIcon(tint = Color.Black.copy(alpha = 0.70f), size = 23.dp)
                    }
                    PhosphorSmileyIcon(
                        tint = if (isAddingSticker) colors.iosBlue else Color.White,
                        size = 23.dp
                    )
                }

                // Photo / Gallery Toggle Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            SoundManager.playReaction()
                            selectedPhotoUrl = if (selectedPhotoUrl == null) sampleImages.firstOrNull() else null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.offset(x = 0.5.dp, y = 1.2.dp)) {
                        PhosphorPhotoIcon(tint = Color.Black.copy(alpha = 0.70f), size = 23.dp)
                    }
                    PhosphorPhotoIcon(
                        tint = if (selectedPhotoUrl != null) colors.iosBlue else Color.White,
                        size = 23.dp
                    )
                }
            }
        }

        // ── 5. Interactive Stickers & Widgets Light Bottom Sheet ─────────
        if (isAddingSticker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isAddingSticker = false }
            )
        }

        AnimatedVisibility(
            visible = isAddingSticker,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp, start = 14.dp, end = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White)
                    .border(0.5.dp, Color(0xFFE5E5EA), RoundedCornerShape(26.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Stickers & Widgets",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF2F2F7))
                                .clickable { isAddingSticker = false },
                            contentAlignment = Alignment.Center
                        ) {
                            PhosphorCloseX(tint = Color(0xFF3C3C43), size = 13.dp)
                        }
                    }

                    // Main 3 Widget Actions (Light Theme Cards)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Location Sticker Tag
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF2F2F7))
                                .clickable {
                                    SoundManager.playReaction()
                                    selectedLocation = "San Francisco, CA"
                                    isAddingSticker = false
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LucideMapPin(tint = Color(0xFFFF3B30), size = 16.dp)
                                Text("Location", fontSize = 13.sp, color = Color(0xFF1C1C1E), fontWeight = FontWeight.Bold)
                            }
                        }

                        // Music Sticker Tag
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF2F2F7))
                                .clickable {
                                    SoundManager.playReaction()
                                    selectedMusic = "Odesza • A Moment Apart"
                                    isAddingSticker = false
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PhosphorWaveform(tint = Color(0xFFFF2D55), size = 16.dp)
                                Text("Music", fontSize = 13.sp, color = Color(0xFF1C1C1E), fontWeight = FontWeight.Bold)
                            }
                        }

                        // Poll Sticker Tag
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF2F2F7))
                                .clickable {
                                    SoundManager.playReaction()
                                    storyPoll = PollData(
                                        question = "What should we build next?",
                                        options = listOf(
                                            PollOption("id1", "Smart Glasses", 0, false, emptyList()),
                                            PollOption("id2", "Hologram UI", 0, false, emptyList())
                                        )
                                    )
                                    isAddingSticker = false
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LucideBarChart2(tint = Color(0xFF007AFF), size = 16.dp)
                                Text("Poll", fontSize = 13.sp, color = Color(0xFF1C1C1E), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Quick Emojis Row
                    Text(
                        text = "Quick Reactions",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8E8E93)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("🔥", "❤️", "✨", "🚀", "🎉", "💯", "👏", "😍").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 26.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        SoundManager.playReaction()
                                        storyCaption = if (storyCaption.isEmpty()) emoji else "$storyCaption $emoji"
                                        isAddingSticker = false
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Photo Library Backdrop Thumbnails
                    Text(
                        text = "Backdrop Photo Presets",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8E8E93)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sampleImages) { url ->
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (selectedPhotoUrl == url) 2.5.dp else 0.5.dp,
                                        color = if (selectedPhotoUrl == url) colors.iosBlue else Color(0xFFE5E5EA),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        SoundManager.playReaction()
                                        selectedPhotoUrl = url
                                        isAddingSticker = false
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Photo Preset",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 6. Bottom Color Palette Swatches & Share Action ───────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circular Gradient / Color Swatches Carousel (Frosted Light Glass)
            if (selectedPhotoUrl == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.20f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.88f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.60f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(gradients) { index, preset ->
                            val isSelected = selectedGradientIndex == index

                            val targetSize = if (isSelected) 34.dp else 28.dp
                            val animatedSize by animateDpAsState(
                                targetValue = targetSize,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "swatchSize"
                            )

                            Box(
                                modifier = Modifier
                                    .size(animatedSize)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(preset.colors))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF1C1C1E) else Color.White.copy(alpha = 0.70f),
                                        shape = CircleShape
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.shadow(6.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.35f))
                                        } else Modifier
                                    )
                                    .clickable {
                                        SoundManager.playReaction()
                                        selectedGradientIndex = index
                                    }
                            )
                        }
                    }
                }
            }

            // Full-Width Primary Apple Share Button
            Button(
                onClick = {
                    SoundManager.playSend()
                    val finalImage = selectedPhotoUrl
                    val finalCaption = if (storyCaption.isNotBlank()) storyCaption else ""
                    val newStory = Story(
                        id = "s_my_${System.currentTimeMillis()}",
                        authorName = "Your Story",
                        avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80",
                        timeAgo = "Just now",
                        image = finalImage,
                        caption = finalCaption,
                        seen = false,
                        isMyStory = true,
                        viewCount = 1,
                        likesCount = 0,
                        musicTrack = selectedMusic,
                        locationTag = selectedLocation,
                        viewers = emptyList(),
                        bgGradientColors = if (finalImage == null) gradients[selectedGradientIndex].colors else null,
                        textHighlightMode = highlightMode.name,
                        fontIndex = fontIndex,
                        textAlignStr = when (textAlign) {
                            TextAlign.Start -> "Start"
                            TextAlign.End -> "End"
                            else -> "Center"
                        },
                        pollData = storyPoll
                    )
                    onPublishStory(newStory)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(10.dp, RoundedCornerShape(26.dp), spotColor = colors.iosBlue),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.iosBlue),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PhosphorSendArrowIcon(tint = Color.White, size = 18.dp)
                    Text(
                        text = "Share to Story",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
