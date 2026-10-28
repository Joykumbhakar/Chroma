package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.R
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.theme.LocalOscellionColors
import kotlin.math.abs

/**
 * Model representing a user connected via mutual follows or post likes.
 */
data class ConnectedUser(
    val id: String,
    val name: String,
    val username: String = "",
    val avatarUrl: String = "",
    val initials: String = "",
    val isOnline: Boolean = false
)

fun UserProfile.toConnectedUser(): ConnectedUser {
    val cleanName = when {
        displayName.isNotBlank() -> displayName.trim()
        username.isNotBlank() -> username.trim()
        else -> "User"
    }

    val computedInitials = if (cleanName.contains(" ")) {
        val parts = cleanName.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].first().uppercase()}.${parts[1].first().uppercase()}"
        } else {
            cleanName.take(2).uppercase()
        }
    } else if (cleanName.length >= 2) {
        "${cleanName[0].uppercase()}.${cleanName[1].uppercase()}"
    } else {
        cleanName.uppercase()
    }

    return ConnectedUser(
        id = uid,
        name = cleanName,
        username = username,
        avatarUrl = avatarUrl,
        initials = computedInitials,
        isOnline = online
    )
}

/**
 * Palette for vibrant initials avatar circles matching the design reference.
 */
private val InitialsColorPalette = listOf(
    Color(0xFFE65100), // Rich Warm Orange (like "A.B" in design reference)
    Color(0xFF6A1B9A), // Royal Purple (like Avatar 3)
    Color(0xFF00897B), // Deep Teal (like Avatar 4)
    Color(0xFF007AFF), // Cupertino Blue
    Color(0xFFD81B60), // Crimson Rose
    Color(0xFF1E88E5), // Vivid Sky
    Color(0xFF43A047), // Fresh Green
    Color(0xFFFB8C00)  // Golden Amber
)

private fun getInitialsColor(name: String): Color {
    val hash = abs(name.hashCode())
    return InitialsColorPalette[hash % InitialsColorPalette.size]
}

/**
 * High-fidelity Group of Avatars with overlapping circular faces, +N counter pill,
 * and an interactive Apple-style dropdown popup menu displaying connected users.
 *
 * Implements the exact design reference from Setproduct "Group of Avatars".
 */
@Composable
fun ConnectedUsersAvatarGroup(
    users: List<ConnectedUser>,
    modifier: Modifier = Modifier,
    totalCount: Int = users.size,
    maxVisible: Int = 4,
    avatarSize: Dp = 26.dp,
    overlapOffset: Dp = 7.dp,
    prefixText: String = "Followed by",
    suffixText: String = "",
    showTextSummary: Boolean = true,
    onUserClick: (String) -> Unit = {}
) {
    if (users.isEmpty() && totalCount <= 0) return

    val colors = LocalOscellionColors.current
    var isDropdownOpen by remember { mutableStateOf(false) }

    val visibleUsers = remember(users, maxVisible) {
        users.take(maxVisible)
    }

    val remainingCount = remember(totalCount, visibleUsers.size) {
        (totalCount - visibleUsers.size).coerceAtLeast(0)
    }

    // Separation border ring color matching current theme background
    val ringBorderColor = if (colors.isDark) Color(0xFF121214) else Color.White

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isDropdownOpen = !isDropdownOpen
            }
        ) {
            // 1. Overlapping Avatars Stack with +N Counter
            Row(
                horizontalArrangement = Arrangement.spacedBy(-overlapOffset),
                verticalAlignment = Alignment.CenterVertically
            ) {
                visibleUsers.forEachIndexed { index, user ->
                    ConnectedAvatarCircle(
                        user = user,
                        size = avatarSize,
                        borderColor = ringBorderColor
                    )
                }

                // The +N Count Badge (Light silver circle matching design reference)
                if (remainingCount > 0) {
                    ConnectedRemainingBadge(
                        count = remainingCount,
                        size = avatarSize,
                        borderColor = ringBorderColor,
                        onClick = { isDropdownOpen = !isDropdownOpen }
                    )
                }
            }

            // 2. Instagram/Facebook-Style Connected User Text Summary
            if (showTextSummary) {
                val namesText = remember(users, totalCount, prefixText, suffixText) {
                    buildAnnotatedString {
                        if (prefixText.isNotBlank()) {
                            withStyle(SpanStyle(color = Color(0xFF8E8E93), fontWeight = FontWeight.Normal)) {
                                append("$prefixText ")
                            }
                        }

                        val sampleNames = users.take(2).map { it.name }
                        if (sampleNames.isNotEmpty()) {
                            withStyle(SpanStyle(color = colors.textMain, fontWeight = FontWeight.SemiBold)) {
                                append(sampleNames.joinToString(", "))
                            }
                        }

                        if (totalCount > sampleNames.size) {
                            val others = totalCount - sampleNames.size
                            withStyle(SpanStyle(color = Color(0xFF8E8E93), fontWeight = FontWeight.Normal)) {
                                append(" and ")
                            }
                            withStyle(SpanStyle(color = colors.textMain, fontWeight = FontWeight.SemiBold)) {
                                append("$others other${if (others > 1) "s" else ""}")
                            }
                        }

                        if (suffixText.isNotBlank()) {
                            withStyle(SpanStyle(color = Color(0xFF8E8E93), fontWeight = FontWeight.Normal)) {
                                append(" $suffixText")
                            }
                        }
                    }
                }

                Text(
                    text = namesText,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        // 3. Interactive Floating Connected Users Dropdown Popup Menu
        if (isDropdownOpen) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(x = 10, y = 80),
                onDismissRequest = { isDropdownOpen = false },
                properties = PopupProperties(focusable = true)
            ) {
                ConnectedUsersDropdownCard(
                    users = users,
                    onUserClick = { userId ->
                        isDropdownOpen = false
                        onUserClick(userId)
                    }
                )
            }
        }
    }
}

/**
 * Individual Avatar Circle with circular photo or vibrant initials fallback.
 */
@Composable
private fun ConnectedAvatarCircle(
    user: ConnectedUser,
    size: Dp,
    borderColor: Color
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (user.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            // Distinct initials fallback with brand gradient/color
            val bgColor = remember(user.name) { getInitialsColor(user.name) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.initials.ifBlank { user.name.take(1).uppercase() },
                    color = Color.White,
                    fontSize = (size.value * 0.38f).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
        }
    }
}

/**
 * The +N Counter Badge Circle matching the design reference light silver chip.
 */
@Composable
private fun ConnectedRemainingBadge(
    count: Int,
    size: Dp,
    borderColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "plusScale"
    )

    val badgeBg = if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)
    val textColor = if (colors.isDark) Color(0xFFF3F4F6) else Color(0xFF1F2937)

    Box(
        modifier = Modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .background(badgeBg)
            .border(2.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+$count",
            color = textColor,
            fontSize = (size.value * 0.40f).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )
    }
}

/**
 * Floating Dropdown Popover Card presenting the list of connected users
 * with profile photos, online status dots, and names.
 */
@Composable
private fun ConnectedUsersDropdownCard(
    users: List<ConnectedUser>,
    onUserClick: (String) -> Unit
) {
    val colors = LocalOscellionColors.current
    val cardBg = if (colors.isDark) Color(0xFF1C1C1E) else Color.White
    val cardBorder = if (colors.isDark) Color(0x30FFFFFF) else Color(0x14000000)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = 14.dp,
        modifier = Modifier
            .width(250.dp)
            .heightIn(max = 280.dp)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            users.forEach { user ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUserClick(user.id) }
                        .padding(horizontal = 14.dp, vertical = 7.5.dp)
                ) {
                    // Avatar with online status indicator
                    Box(modifier = Modifier.size(34.dp)) {
                        if (user.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(user.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = user.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            val bgColor = remember(user.name) { getInitialsColor(user.name) }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.initials.ifBlank { user.name.take(1).uppercase() },
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Green online status dot (matching design reference)
                        if (user.isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(9.5.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34C759))
                                    .border(1.5.dp, cardBg, CircleShape)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = user.name,
                        color = colors.textMain,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
