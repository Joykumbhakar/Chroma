package com.chroma.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.data.OnlineStatusManager
import com.chroma.studio.ui.theme.LocalOscellionColors

/**
 * Universal Atomic Online Status Dot.
 * Adheres to single-source design tokens from LocalOscellionColors.current.onlineStatus / offlineStatus.
 * Features an optional gentle live pulse when online.
 */
@Composable
fun OnlineStatusDot(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    showPulse: Boolean = true,
    ringColor: Color? = null
) {
    val colors = LocalOscellionColors.current
    val targetColor = if (isOnline) colors.onlineStatus else colors.offlineStatus

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by if (isOnline && showPulse) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.38f,
            animationSpec = infiniteRepeatable(
                animation = tween(1250, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotPulse"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }
    val pulseAlpha by if (isOnline && showPulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1250, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotPulseAlpha"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (isOnline && showPulse) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(targetColor.copy(alpha = pulseAlpha))
            )
        }
        val ring = ringColor ?: colors.statusRingBorder
        if (ring != Color.Transparent) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(ring)
                    .padding((size.value * 0.15f).coerceIn(1.2f, 2.5f).dp)
                    .clip(CircleShape)
                    .background(targetColor)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(targetColor)
            )
        }
    }
}

/**
 * Universal Atomic Online Status Badge (e.g. "Online" with pulsing dot or "Last seen ...").
 * Single source of truth across user profile bio, search results, and chat screens.
 */
@Composable
fun OnlineStatusBadge(
    isOnline: Boolean,
    statusText: String? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = LocalOscellionColors.current
    val displayText = statusText ?: if (isOnline) "Online" else "Offline"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
    ) {
        if (isOnline) {
            OnlineStatusDot(
                isOnline = true,
                size = if (compact) 6.5.dp else 8.dp,
                showPulse = true,
                ringColor = Color.Transparent
            )
        }
        Text(
            text = displayText,
            color = if (isOnline) colors.onlineStatus else colors.textMuted.copy(alpha = 0.85f),
            fontSize = if (compact) 11.5.sp else 12.5.sp,
            fontWeight = if (isOnline) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Live User Presence Indicator that automatically reacts to real-time Firebase changes
 * using OnlineStatusManager and the central design system tokens.
 */
@Composable
fun LiveUserPresenceIndicator(
    userId: String?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val presence = OnlineStatusManager.rememberUserPresence(userId)
    val isOnline = presence.online
    val statusText = if (isOnline) {
        "Online"
    } else if (presence.lastOnline != null && presence.lastOnline > 0L) {
        OnlineStatusManager.formatWhatsAppLastSeen(presence.lastOnline)
    } else {
        "Offline"
    }

    OnlineStatusBadge(
        isOnline = isOnline,
        statusText = statusText,
        modifier = modifier,
        compact = compact
    )
}
