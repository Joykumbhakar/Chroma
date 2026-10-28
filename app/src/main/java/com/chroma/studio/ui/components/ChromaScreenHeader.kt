package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide

/**
 * Standard Cupertino Frosted Glass Top Navigation Header used consistently across all screens
 * (matching the Community tab header).
 */
@Composable
fun ChromaScreenHeader(
    title: String,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFF007AFF),
    showBackButton: Boolean = false,
    circularBackButton: Boolean = true,
    onBack: (() -> Unit)? = null,
    isSearchExpanded: Boolean = false,
    searchContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg.copy(alpha = 0.94f))
            .border(0.5.dp, colors.glassBorder)
            .padding(top = statusBarTop + 6.dp, bottom = 10.dp, start = 16.dp, end = 16.dp)
    ) {
        if (isSearchExpanded && searchContent != null) {
            searchContent()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button + Brand Title & Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showBackButton && onBack != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.ChevronLeft,
                                contentDescription = "Back",
                                tint = colors.textMain,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textMain,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = ".",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor
                        )

                        if (!badgeText.isNullOrBlank()) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(badgeColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = badgeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Right: Action items
                if (actions != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions()
                    }
                }
            }
        }
    }
}

/**
 * Circular Glass Action Button for ChromaScreenHeader
 */
@Composable
fun ChromaHeaderActionButton(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color? = null,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: colors.textMain,
            modifier = Modifier.size(20.dp)
        )
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .size(if (badgeCount > 9) 16.dp else 8.5.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-3.5).dp, y = 3.5.dp)
                    .background(if (colors.isDark) Color(0xFF16161A) else Color.White, CircleShape)
                    .padding(1.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30)),
                contentAlignment = Alignment.Center
            ) {
                if (badgeCount > 9) {
                    Text(
                        text = if (badgeCount > 99) "99+" else "$badgeCount",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Cupertino Pill Primary Action Button for ChromaScreenHeader
 */
@Composable
fun ChromaHeaderActionButton(
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accentColor: Color = Color(0xFF007AFF),
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val thinAppleBorder = remember {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.42f),
                Color.White.copy(alpha = 0.12f)
            )
        )
    }
    val btnShape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(btnShape)
            .background(if (enabled && !isLoading) accentColor else Color.Gray.copy(alpha = 0.35f))
            .border(
                width = 0.5.dp,
                brush = if (enabled && !isLoading) thinAppleBorder else SolidColor(Color.White.copy(alpha = 0.12f)),
                shape = btnShape
            )
            .clickable(enabled = enabled && !isLoading) { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            OscellionLoadingSpinner(
                modifier = Modifier.size(16.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

