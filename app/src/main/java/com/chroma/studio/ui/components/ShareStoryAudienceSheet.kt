package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.Star

/**
 * Instagram-style Dual Story Publishing Selector.
 * Allows instant publishing to "Your Story" (Public) or "Only Friend" (Close Friends),
 * along with one-tap access to curate the Only Friend list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareStoryAudienceSheet(
    onDismissRequest: () -> Unit,
    onSharePublic: () -> Unit,
    onShareOnlyFriends: () -> Unit,
    onEditOnlyFriends: () -> Unit,
    onlyFriendsCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = if (colors.isDark) Color(0xFF1C1C1E) else Color.White,
        contentColor = colors.textMain,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(if (colors.isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.18f))
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Share Story",
                color = colors.textMain,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Choose who can view this story for the next 24 hours",
                color = colors.textMuted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Container Group
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
            ) {
                // 1. Your Story (Public)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundManager.playSend()
                            onSharePublic()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(AppleStatusVibrantGradient)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (colors.isDark) Color(0xFF1C1C1E) else Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Globe,
                                    contentDescription = null,
                                    tint = if (colors.isDark) Color.White else Color(0xFF007AFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Your Story",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMain
                            )
                            Text(
                                text = "Anyone on Chroma can see your story",
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF007AFF))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Share",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = if (colors.isDark) Color(0xFF38383A) else Color(0xFFE5E5EA),
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp)
                )

                // 2. Only Friend (Emerald Neon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundManager.playSend()
                            onShareOnlyFriends()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(OnlyFriendEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Only Friend",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMain
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(OnlyFriendEmerald.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "$onlyFriendsCount",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnlyFriendEmerald
                                    )
                                }
                            }
                            Text(
                                text = "Shared only with your Only Friend list",
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Edit list gear / settings button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(if (colors.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA))
                                .clickable {
                                    SoundManager.playReaction()
                                    onEditOnlyFriends()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Settings2,
                                contentDescription = "Edit Only Friends",
                                tint = colors.textMain,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(OnlyFriendEmerald)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Share",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cancel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (colors.isDark) Color(0xFF2C2C2E).copy(alpha = 0.6f) else Color(0xFFE5E5EA).copy(alpha = 0.7f))
                    .clickable { onDismissRequest() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    color = colors.textMain,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
