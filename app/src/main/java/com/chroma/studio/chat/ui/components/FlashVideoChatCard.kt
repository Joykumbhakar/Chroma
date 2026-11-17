package com.chroma.studio.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.data.ChatMessage
import com.chroma.studio.ui.components.VerifiedBadge
import com.chroma.studio.ui.components.studioBevel
import com.composables.icons.lucide.*

/**
 * High-performance, Instagram-style Flash Video Card rendered in Direct Messages and Group Chats.
 * Formatted with 9:16 vertical proportions, micro-beveled borders, creator attribution,
 * solid centered play trigger, duration pill, and one-tap immersion navigation.
 */
@Composable
fun FlashVideoChatCard(
    message: ChatMessage,
    isOutgoing: Boolean,
    onFlashVideoClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val videoId = message.flashVideoId ?: ""
    val rawThumbnail = message.flashVideoThumbnailUrl?.takeIf { it.isNotBlank() }
        ?: message.mediaUrl?.takeIf { it.isNotBlank() }
        ?: ""

    val resolvedThumbnail = remember(rawThumbnail) {
        if (rawThumbnail.isNotBlank()) {
            com.chroma.studio.chat.data.ChatLocalCacheManager.getOfflineSafeMediaUrl(context, rawThumbnail)
        } else {
            ""
        }
    }

    val authorUsername = message.flashVideoAuthorUsername?.takeIf { it.isNotBlank() } ?: "creator"
    val authorAvatar = message.flashVideoAuthorAvatarUrl?.takeIf { it.isNotBlank() } ?: ""
    val caption = message.flashVideoCaption?.takeIf { it.isNotBlank() }
        ?: message.text?.takeIf { it.isNotBlank() && it != "⚡ Shared a Flash video" }
        ?: ""

    val durationMs = message.flashVideoDurationMs ?: 15_000L
    val durationFormatted = remember(durationMs) {
        val totalSec = (durationMs / 1000).coerceAtLeast(1)
        val min = totalSec / 60
        val sec = totalSec % 60
        String.format("%d:%02d", min, sec)
    }

    Box(
        modifier = modifier
            .width(224.dp)
            .height(336.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp))
            .studioBevel(cornerRadius = 18.dp, borderWidth = 0.9.dp)
            .background(Color(0xFF141414))
            .clickable(enabled = onFlashVideoClick != null && videoId.isNotBlank()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onFlashVideoClick?.invoke(videoId)
            }
    ) {
        // ── 1. Video Poster / High-Res Thumbnail ──────────────────────────────
        if (resolvedThumbnail.isNotBlank()) {
            AsyncImage(
                model = resolvedThumbnail,
                contentDescription = "Flash video thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
            )
        } else {
            // Elegant dark gradient placeholder if thumbnail is loading or offline
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1F1C2C), Color(0xFF928DAB))
                        )
                    )
            )
        }

        // ── 2. Top Header Overlay with Scrim ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.78f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Flash / Reel Badge Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .studioBevel(cornerRadius = 10.dp, borderWidth = 0.6.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF007A), Color(0xFFFF5E00))
                            )
                        )
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Zap,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "Flash",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                }

                // Author Attribution Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                ) {
                    if (authorAvatar.isNotBlank()) {
                        AsyncImage(
                            model = authorAvatar,
                            contentDescription = authorUsername,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = authorUsername.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "@$authorUsername",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 95.dp),
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        )
                    )

                    VerifiedBadge(size = 11.dp)
                }
            }
        }

        // ── 3. Centered Tactile Glass Play Button ───────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(52.dp)
                .shadow(elevation = 10.dp, shape = CircleShape)
                .studioBevel(cornerRadius = 26.dp, borderWidth = 1.dp)
                .background(Color.Black.copy(alpha = 0.52f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.Play,
                contentDescription = "Play video",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = 1.5.dp) // Optical centering for play triangle
            )
        }

        // ── 4. Bottom Overlay with Scrim, Caption & Action Pill ────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.60f),
                            Color.Black.copy(alpha = 0.90f)
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Caption Snippet (up to 2 lines)
                if (caption.isNotBlank()) {
                    Text(
                        text = caption,
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                offset = Offset(0f, 1f),
                                blurRadius = 3f
                            )
                        )
                    )
                }

                // Bottom Row: Duration Chip + "Watch Flash" Interactive Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Duration Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Clock,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = durationFormatted,
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // "Watch Flash" Interactive Action Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .studioBevel(cornerRadius = 10.dp, borderWidth = 0.6.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF0381FE), Color(0xFF00C6FF))
                                )
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Watch Flash",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
