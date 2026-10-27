package com.chroma.studio.chat.ui.components

import com.chroma.studio.ui.components.VerifiedBadge

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.chat.data.AccentColorType
import com.chroma.studio.chat.data.ChatConversation
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.ui.components.AiWaveSphereIcon

@Composable
fun ChatHeader(
    activeChat: ChatConversation,
    unreadBadgeCount: Int,
    isSearchOpen: Boolean,
    searchInsideQuery: String,
    searchMatchCount: Int,
    isCallActive: Boolean,
    callType: String,
    callDurationSec: Int,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onAppearanceToggle: () -> Unit,
    onAccentCycle: () -> Unit,
    onWallpaperCycle: () -> Unit,
    onOpenAppearanceSettings: () -> Unit = {},
    onOpenSharedGallery: () -> Unit = {},
    onCreatePollClick: () -> Unit,
    onMuteToggle: () -> Unit,
    onExportTranscript: () -> Unit,
    onClearChat: () -> Unit,
    onBlockUser: () -> Unit,
    onUnpinMessage: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = colors.headerBg,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Main Navigation Bar ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back + Profile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Back button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onBackClick() }
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            Icon(Lucide.ChevronLeft, contentDescription = "Back", tint = colors.iosBlue, modifier = Modifier.size(28.dp))
                            if (unreadBadgeCount > 0) {
                                Text(
                                    text = unreadBadgeCount.toString(),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = colors.iosBlue,
                                    modifier = Modifier.offset(x = (-4).dp)
                                )
                            }
                        }
                    }

                    // Profile trigger
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onProfileClick() }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Avatar with online pulsing indicator
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IosAvatarView(
                                name = activeChat.name,
                                avatarUrl = activeChat.avatar,
                                size = 40.dp,
                                isGroup = activeChat.type == "group",
                                isOnline = false
                            )
                            if (activeChat.online) {
                                val infiniteTransition = rememberInfiniteTransition(label = "headerPulse")
                                val pulseScale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.45f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "pulseScale"
                                )
                                val pulseAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.55f,
                                    targetValue = 0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "pulseAlpha"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = pulseScale
                                                scaleY = pulseScale
                                                alpha = pulseAlpha
                                            }
                                            .background(colors.success, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(colors.success, CircleShape)
                                            .border(2.dp, colors.headerBg, CircleShape)
                                    )
                                }
                            }
                        }

                        // Name + status
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = activeChat.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (activeChat.isVerified) {
                                    VerifiedBadge(size = 16.dp)
                                }
                                if (activeChat.isMuted) {
                                    LucideBellOff(tint = colors.textMuted, size = 13.dp)
                                }
                            }
                            val isActivity = activeChat.statusText.contains("typing", ignoreCase = true) || activeChat.statusText.contains("recording", ignoreCase = true)
                            val isRecording = activeChat.statusText.contains("recording", ignoreCase = true)
                            val headerStatusText = when {
                                activeChat.isBlocked -> "Blocked"
                                isActivity -> activeChat.statusText
                                activeChat.online -> "Online"
                                activeChat.statusText.isNotBlank() -> activeChat.statusText
                                else -> "Offline"
                            }
                            val headerStatusColor = when {
                                activeChat.isBlocked -> colors.textMuted
                                isActivity -> colors.iosBlue
                                activeChat.online -> colors.success
                                else -> colors.textMuted
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (isRecording) {
                                    LucideMic(tint = colors.iosBlue, size = 11.dp)
                                }
                                Text(
                                    text = headerStatusText,
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = headerStatusColor,
                                    fontWeight = if (activeChat.online || isActivity) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Right: Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-2).dp)
                ) {
                    // Search
                    IconButton(onClick = onSearchToggle, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Lucide.Search,
                            contentDescription = "Search",
                            tint = colors.iosBlue,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    // Audio call
                    IconButton(onClick = onAudioCallClick, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Lucide.Phone,
                            contentDescription = "Call",
                            tint = colors.iosBlue,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    // Video call
                    IconButton(onClick = onVideoCallClick, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Lucide.Video,
                            contentDescription = "Video",
                            tint = colors.iosBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Overflow menu
                    Box {
                        IconButton(
                            onClick = { showMenu = !showMenu },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "More Options",
                                tint = colors.iosBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .width(245.dp)
                                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.15f))
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (colors.isLight) Color(0xF8FFFFFF)
                                    else Color(0xF21C1C1E)
                                )
                                .border(
                                    width = 0.85.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = if (colors.isLight) 0.55f else 0.35f),
                                            Color.White.copy(alpha = 0.08f),
                                            Color.White.copy(alpha = if (colors.isLight) 0.25f else 0.18f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(vertical = 4.dp)
                        ) {
                            val isGroup = activeChat.type == "group"

                            MenuDivider(label = if (isGroup) "Group" else "Contact")
                            AppleMenuItem(
                                title = if (isGroup) "Group Info" else "Contact Info",
                                icon = {
                                    Icon(
                                        imageVector = Lucide.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                iconBgColor = Color(0xFF007AFF).copy(alpha = 0.14f),
                                onClick = { showMenu = false; onProfileClick() }
                            )
                            AppleMenuItem(
                                title = "Mute Notifications",
                                icon = {
                                    Icon(
                                        imageVector = if (activeChat.isMuted) Lucide.Bell else Lucide.BellOff,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9500),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                iconBgColor = Color(0xFFFF9500).copy(alpha = 0.14f),
                                trailingContent = {
                                    Text(
                                        text = if (activeChat.isMuted) "On" else "Off",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (activeChat.isMuted) colors.iosBlue else colors.textMuted
                                    )
                                },
                                onClick = { showMenu = false; onMuteToggle() }
                            )

                            HorizontalDivider(
                                color = colors.separator,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)
                            )
                            MenuDivider(label = "Appearance")
                            AppleMenuItem(
                                title = "Chat Themes",
                                icon = {
                                    Icon(
                                        imageVector = Lucide.Palette,
                                        contentDescription = null,
                                        tint = Color(0xFFAF52DE),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                iconBgColor = Color(0xFFAF52DE).copy(alpha = 0.14f),
                                trailingContent = {
                                    AiWaveSphereIcon(modifier = Modifier.size(15.dp))
                                },
                                onClick = { showMenu = false; onOpenAppearanceSettings() }
                            )
                            AppleMenuItem(
                                title = "Color Mode",
                                icon = {
                                    Icon(
                                        imageVector = Lucide.Moon,
                                        contentDescription = null,
                                        tint = Color(0xFF5856D6),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                iconBgColor = Color(0xFF5856D6).copy(alpha = 0.14f),
                                trailingContent = {
                                    Text(
                                        text = if (colors.isLight) "Light" else "Dark",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textMuted
                                    )
                                },
                                onClick = { showMenu = false; onAppearanceToggle() }
                            )

                            HorizontalDivider(
                                color = colors.separator,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)
                            )
                            MenuDivider(label = "Chat & Media")
                            AppleMenuItem(
                                title = "Media, Links & Docs",
                                icon = {
                                    Icon(
                                        imageVector = Lucide.Image,
                                        contentDescription = null,
                                        tint = Color(0xFF34C759),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                iconBgColor = Color(0xFF34C759).copy(alpha = 0.14f),
                                onClick = { showMenu = false; onOpenSharedGallery() }
                            )
                            AppleMenuItem(
                                title = "Create Poll",
                                icon = {
                                    Icon(
                                        imageVector = Lucide.ChartColumn,
                                        contentDescription = null,
                                        tint = Color(0xFF32ADE6),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                iconBgColor = Color(0xFF32ADE6).copy(alpha = 0.14f),
                                onClick = { showMenu = false; onCreatePollClick() }
                            )
                            AppleMenuItem(
                                title = "Export Transcript",
                                icon = {
                                    Icon(
                                        imageVector = Lucide.Download,
                                        contentDescription = null,
                                        tint = Color(0xFF30B0C7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                iconBgColor = Color(0xFF30B0C7).copy(alpha = 0.14f),
                                onClick = { showMenu = false; onExportTranscript() }
                            )
                            AppleMenuItem(
                                title = "Clear Chat",
                                icon = {
                                    Icon(
                                        imageVector = Lucide.Trash2,
                                        contentDescription = null,
                                        tint = Color(0xFF8E8E93),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                iconBgColor = Color(0xFF8E8E93).copy(alpha = 0.14f),
                                onClick = { showMenu = false; onClearChat() }
                            )

                            HorizontalDivider(
                                color = colors.separator,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)
                            )
                            AppleMenuItem(
                                title = if (isGroup) "Leave Group"
                                       else (if (activeChat.isBlocked) "Unblock Contact" else "Block Contact"),
                                icon = {
                                    Icon(
                                        imageVector = Lucide.Ban,
                                        contentDescription = null,
                                        tint = Color(0xFFFF3B30),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                iconBgColor = Color(0xFFFF3B30).copy(alpha = 0.14f),
                                isDestructive = true,
                                onClick = { showMenu = false; onBlockUser() }
                            )
                        }
                    }
                }
            }

            // ── Pinned Message Banner ──────────────────────────────────
            AnimatedVisibility(
                visible = activeChat.pinnedMessage != null,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
            ) {
                if (activeChat.pinnedMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceSecondary)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(24.dp)
                                    .background(colors.iosBlue, RoundedCornerShape(2.dp))
                            )
                            Column {
                                Text(
                                    text = "Pinned Message",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.iosBlue,
                                    lineHeight = 13.sp
                                )
                                Text(
                                    text = activeChat.pinnedMessage,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = colors.textDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onUnpinMessage() },
                            contentAlignment = Alignment.Center
                        ) {
                            PhosphorCloseX(tint = colors.textMuted, size = 12.dp)
                        }
                    }
                }
            }

            // ── In-Chat Search Bar ──────────────────────────────────
            AnimatedVisibility(
                visible = isSearchOpen,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceSecondary)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PhosphorMagnifyingGlass(tint = colors.textMuted, size = 15.dp)
                    BasicTextField(
                        value = searchInsideQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        cursorBrush = SolidColor(colors.iosBlue),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = colors.textDark,
                            fontWeight = FontWeight.Normal
                        ),
                        decorationBox = { inner ->
                            if (searchInsideQuery.isEmpty()) {
                                Text(
                                    "Search in Conversation",
                                    fontSize = 15.sp,
                                    color = colors.textMuted
                                )
                            }
                            inner()
                        }
                    )
                    if (searchInsideQuery.isNotEmpty()) {
                        Text(
                            text = "$searchMatchCount match${if (searchMatchCount == 1) "" else "es"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onSearchToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorCloseX(tint = colors.textMuted, size = 12.dp)
                    }
                }
            }

            // ── Active Call Banner ──────────────────────────────────
            AnimatedVisibility(visible = isCallActive) {
                val min = callDurationSec / 60
                val sec = callDurationSec % 60
                val durationStr = "$min:${sec.toString().padStart(2, '0')}"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF34C759))
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BlinkingDot(Color.White)
                        Text(
                            text = "$callType · ${activeChat.name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = durationStr,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Button(
                        onClick = onEndCall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            "End",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // ── Hairline Bottom Separator ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.separator)
            )
        }
    }
}

// ── Helper Composables ──────────────────────────────────

@Composable
private fun MenuDivider(label: String) {
    val colors = LocalChatColors.current
    Box(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMuted.copy(alpha = 0.85f),
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun AppleMenuItem(
    title: String,
    icon: @Composable () -> Unit,
    iconBgColor: Color,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
    isDestructive: Boolean = false
) {
    val colors = LocalChatColors.current
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = if (isDestructive) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isDestructive) Color(0xFFFF3B30) else colors.textDark
                )
                trailingContent?.invoke()
            }
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
    )
}

@Composable
private fun MenuItemText(
    text: String,
    color: Color,
    isBold: Boolean = false
) {
    Text(
        text = text,
        fontSize = 14.5.sp,
        fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
        color = color
    )
}

