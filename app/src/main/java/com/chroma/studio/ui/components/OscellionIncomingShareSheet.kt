package com.chroma.studio.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.model.SharedIncomingData
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*

enum class SharedTargetAction {
    POST_CREATION,
    CHAT_MESSAGE,
    VIDEO_EDITOR,
    AUDIO_STUDIO
}

/**
 * Flagship Samsung One UI 7 / iOS styled Incoming Share Modal.
 * Prompts the user with contextual studio actions whenever files, media, or text
 * are shared into Oscellion from external apps (Gallery, Files, Browser, etc.).
 */
@Composable
fun OscellionIncomingShareSheet(
    sharedData: SharedIncomingData,
    onSelectAction: (SharedTargetAction) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val sheetBg = if (isDark) Color(0xFF141416) else Color(0xFFFFFFFF)
    val cardBg = if (isDark) Color(0xFF1F1F24) else Color(0xFFF2F4F7)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val textMain = if (isDark) Color(0xFFF2F2F7) else Color(0xFF0A0A0C)
    val textMuted = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)
    val brandBlue = Color(0xFF0381FE)

    // Scrim overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                SoundManager.playReaction()
                onDismiss()
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bottom Sheet Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {} // Prevent closing when tapping inside
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(sheetBg)
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.06f),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isDark) Color.White.copy(0.25f) else Color.Black.copy(0.18f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Brand Icon Badge
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0381FE), Color(0xFF0051D5))
                                    )
                                )
                                .border(0.75.dp, Color.White.copy(0.35f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Share2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Share to Oscellion",
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                color = textMain
                            )
                            Text(
                                text = sharedData.displayHeadline,
                                fontSize = 12.5.sp,
                                fontFamily = InterFontFamily,
                                color = brandBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.06f))
                            .clickable {
                                SoundManager.playReaction()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Close",
                            tint = textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Media Preview Box
                if (sharedData.hasUris) {
                    if (sharedData.uris.size == 1) {
                        // Single Media Preview
                        val uri = sharedData.uris.first()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (sharedData.isAudio) {
                                // Audio Track Card
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(Color(0xFFEC4899), Color(0xFFBE185D))
                                                )
                                            )
                                            .border(1.5.dp, Color.White.copy(0.35f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Music,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Audio Recording / Music",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = uri.lastPathSegment ?: "audio_track.mp3",
                                            fontSize = 12.sp,
                                            color = textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                // Image / Video Preview
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Shared Media",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (sharedData.isVideo) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(0.60f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Play,
                                            contentDescription = "Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Multi-Item Carousel
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${sharedData.uris.size} items selected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textMuted,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(sharedData.uris) { uri ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 110.dp, height = 110.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(cardBg)
                                            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (sharedData.hasText) {
                    // Text / URL Snippet Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "\"${sharedData.text?.trim()}\"",
                            fontSize = 13.5.sp,
                            color = textMain,
                            fontFamily = InterFontFamily,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(22.dp)),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Action 1: Create Post / Story
                    ShareActionRow(
                        title = "Create Community Post / Story",
                        subtitle = "Publish to your public portfolio or stories",
                        icon = LucideFilled.User,
                        iconTint = brandBlue,
                        textMain = textMain,
                        textMuted = textMuted,
                        onClick = {
                            SoundManager.playReaction()
                            onSelectAction(SharedTargetAction.POST_CREATION)
                        }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = cardBorder)

                    // Action 2: Send in Chat
                    ShareActionRow(
                        title = "Send in Chat / DM",
                        subtitle = "Send directly to a friend or group room",
                        icon = Lucide.MessageCircle,
                        iconTint = Color(0xFF06B6D4),
                        textMain = textMain,
                        textMuted = textMuted,
                        onClick = {
                            SoundManager.playReaction()
                            onSelectAction(SharedTargetAction.CHAT_MESSAGE)
                        }
                    )

                    // Action 3: Studio Video Editor (if video or photos)
                    if (sharedData.isVideo || sharedData.isImage || !sharedData.hasUris) {
                        HorizontalDivider(thickness = 0.5.dp, color = cardBorder)
                        ShareActionRow(
                            title = "Studio Video & Motion Editor",
                            subtitle = "Color grade, trim, layer effects & music",
                            icon = Lucide.Video,
                            iconTint = Color(0xFF8B5CF6),
                            textMain = textMain,
                            textMuted = textMuted,
                            onClick = {
                                SoundManager.playReaction()
                                onSelectAction(SharedTargetAction.VIDEO_EDITOR)
                            }
                        )
                    }

                    // Action 4: Audio Studio (if audio)
                    if (sharedData.isAudio) {
                        HorizontalDivider(thickness = 0.5.dp, color = cardBorder)
                        ShareActionRow(
                            title = "Audio Studio & Waveform Trimmer",
                            subtitle = "Inspect frequencies, trim beat, or remix",
                            icon = Lucide.Music,
                            iconTint = Color(0xFFEC4899),
                            textMain = textMain,
                            textMuted = textMuted,
                            onClick = {
                                SoundManager.playReaction()
                                onSelectAction(SharedTargetAction.AUDIO_STUDIO)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun ShareActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    textMain: Color,
    textMuted: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.weight(1f)
        ) {
            OneUiIconBadge(
                icon = icon,
                iconColor = iconTint
            )

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textMain,
                    fontFamily = InterFontFamily
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = textMuted,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = textMuted,
            modifier = Modifier.size(17.dp)
        )
    }
}
