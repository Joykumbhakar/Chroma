package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Type

/**
 * Bottom sheet presented when the user clicks the '+' button in the status section.
 * Provides two options:
 * 1. Choose From Gallery (photo/video status upload)
 * 2. Text Story (rich text story/status editor)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryCreationBottomSheet(
    onDismissRequest: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onTextStory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Title Header
            Text(
                text = "Create Story",
                color = colors.textMain,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Share photos or express yourself with text",
                color = colors.textMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(18.dp))

            // iOS Grouped Card Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
            ) {
                // 1. Choose From Gallery
                StoryCreationOptionRow(
                    icon = Lucide.Image,
                    iconGradient = Brush.linearGradient(
                        colors = listOf(Color(0xFF007AFF), Color(0xFF00C6FF))
                    ),
                    title = "Choose From Gallery",
                    subtitle = "Pick photos or videos from your device",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        SoundManager.playReaction()
                        onChooseFromGallery()
                    }
                )

                // Thin divider
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = if (colors.isDark) Color(0xFF38383A) else Color(0xFFE5E5EA),
                    modifier = Modifier.padding(start = 70.dp, end = 12.dp)
                )

                // 2. Text Story
                StoryCreationOptionRow(
                    icon = Lucide.Type,
                    iconGradient = Brush.linearGradient(
                        colors = listOf(Color(0xFFAF52DE), Color(0xFFFF2D55))
                    ),
                    title = "Text Story",
                    subtitle = "Design a status with gradient & typography",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        SoundManager.playReaction()
                        onTextStory()
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cancel Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun StoryCreationOptionRow(
    icon: ImageVector,
    iconGradient: Brush,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vibrant icon badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMain
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textMuted
            )
        }

        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = colors.textMuted.copy(alpha = 0.45f),
            modifier = Modifier.size(18.dp)
        )
    }
}
