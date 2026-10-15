package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.chroma.studio.R
import com.chroma.studio.chat.data.drawableUri
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors

/**
 * Data representation for custom styled Group Logos
 */
data class GroupIconGradient(
    val id: String,
    val name: String,
    val colors: List<Color>
)

val defaultGroupGradients = listOf(
    GroupIconGradient("ocean", "Ocean Blue", listOf(Color(0xFF007AFF), Color(0xFF00C6FF))),
    GroupIconGradient("sunset", "Sunset Coral", listOf(Color(0xFFFF5E3A), Color(0xFFFF2A68))),
    GroupIconGradient("amethyst", "Amethyst Spark", listOf(Color(0xFF8B5CF6), Color(0xFFC084FC))),
    GroupIconGradient("emerald", "Emerald Mint", listOf(Color(0xFF10B981), Color(0xFF059669))),
    GroupIconGradient("midnight", "Midnight Glow", listOf(Color(0xFF1E1B4B), Color(0xFF4F46E5))),
    GroupIconGradient("golden", "Golden Amber", listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))),
    GroupIconGradient("berry", "Berry Purple", listOf(Color(0xFFD946EF), Color(0xFF8B5CF6))),
    GroupIconGradient("ruby", "Ruby Crimson", listOf(Color(0xFFEF4444), Color(0xFF991B1B))),
    GroupIconGradient("teal", "Teal Aqua", listOf(Color(0xFF14B8A6), Color(0xFF06B6D4))),
    GroupIconGradient("titanium", "Dark Titanium", listOf(Color(0xFF27272A), Color(0xFF09090B)))
)

val defaultGroupIconsList = listOf(
    "users", "code", "rocket", "sparkles", "coffee", "gamepad",
    "music", "building", "star", "shield", "flame", "heart",
    "globe", "terminal", "camera", "compass", "briefcase", "zap"
)

val defaultGroupEmojisList = listOf(
    "🚀", "🔥", "💡", "🍕", "🏆", "🎨", "💻", "🌿",
    "⚡", "💎", "🎯", "👾", "🦄", "👑", "🛡️", "⛵",
    "🎧", "🧩", "🌟", "🛠️", "🔮", "🎪", "☕", "🌊"
)

val defaultGroupAvatarDrawables = listOf(
    drawableUri(R.drawable.ic_avatar_corporate_navy),
    drawableUri(R.drawable.ic_avatar_emerald_focus),
    drawableUri(R.drawable.ic_avatar_deep_purple),
    drawableUri(R.drawable.ic_avatar_gold_standard),
    drawableUri(R.drawable.ic_avatar_sapphire_night),
    drawableUri(R.drawable.ic_avatar_royal_blue),
    drawableUri(R.drawable.ic_avatar_ocean_depth),
    drawableUri(R.drawable.ic_avatar_executive_forest),
    drawableUri(R.drawable.ic_avatar_teal_pro),
    drawableUri(R.drawable.ic_avatar_burgundy_board),
    drawableUri(R.drawable.ic_avatar_midnight),
    drawableUri(R.drawable.ic_avatar_charcoal),
    drawableUri(R.drawable.ic_avatar_classic_mac),
    drawableUri(R.drawable.ic_avatar_space_gray),
    drawableUri(R.drawable.ic_avatar_steel_grey),
    drawableUri(R.drawable.profile_avatar_joy)
)

/**
 * Flagship Apple iOS Group Icon & Logo Creator / Editor Modal.
 * Allows choosing custom monograms, vector icons, emojis, or avatar presets.
 */
@Composable
fun GroupIconCreatorModal(
    currentAvatar: String,
    initialGroupName: String = "",
    onDismiss: () -> Unit,
    onApplyIcon: (String) -> Unit
) {
    val colors = LocalChatColors.current

    var selectedTab by remember { mutableStateOf("presets") } // "presets", "monogram", "icons", "emojis"
    var monogramText by remember {
        val initials = initialGroupName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
        mutableStateOf(if (initials.isNotEmpty()) initials else "GP")
    }
    var selectedGradient by remember { mutableStateOf(defaultGroupGradients.first()) }
    var selectedIcon by remember { mutableStateOf("users") }
    var selectedEmoji by remember { mutableStateOf("🚀") }
    var selectedPresetAvatar by remember {
        mutableStateOf(
            if (defaultGroupAvatarDrawables.contains(currentAvatar)) currentAvatar
            else defaultGroupAvatarDrawables.first()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.cardBg)
                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Group Icon Studio",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textDark
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceSecondary)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        PhosphorCloseX(tint = colors.textMuted, size = 12.dp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── LIVE LARGE PREVIEW HERO ──────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .border(3.dp, colors.cardBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectedTab) {
                        "presets" -> {
                            AsyncImage(
                                model = selectedPresetAvatar,
                                contentDescription = "Group Icon",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        "monogram" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(selectedGradient.colors)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monogramText.take(2).uppercase(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        "icons" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(selectedGradient.colors)),
                                contentAlignment = Alignment.Center
                            ) {
                                GroupVectorIconRenderer(name = selectedIcon, size = 44.dp, tint = Color.White)
                            }
                        }
                        "emojis" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(selectedGradient.colors)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedEmoji,
                                    fontSize = 42.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Mode Tabs (Presets, Monogram, Vector Icons, Emojis)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceSecondary)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "presets" to "Avatars",
                        "monogram" to "Monogram",
                        "icons" to "Symbols",
                        "emojis" to "Emojis"
                    ).forEach { (tabKey, tabLabel) ->
                        val isSel = selectedTab == tabKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) colors.cardBg else Color.Transparent)
                                .clickable {
                                    SoundManager.playReaction()
                                    selectedTab = tabKey
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabLabel,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) colors.iosBlue else colors.textMuted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── TAB CONTENT ──────────────────────────────────────────────
                when (selectedTab) {
                    "presets" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "TAP TO SELECT AVATAR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(defaultGroupAvatarDrawables) { av ->
                                    val isSel = av == selectedPresetAvatar
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isSel) 2.5.dp else 0.5.dp,
                                                color = if (isSel) colors.iosBlue else colors.borderSubtle,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                selectedPresetAvatar = av
                                                SoundManager.playReaction()
                                            }
                                    ) {
                                        AsyncImage(
                                            model = av,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "monogram" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "MONOGRAM INITIALS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceSecondary)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                BasicTextField(
                                    value = monogramText,
                                    onValueChange = { if (it.length <= 3) monogramText = it },
                                    textStyle = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textDark,
                                        textAlign = TextAlign.Center
                                    ),
                                    cursorBrush = SolidColor(colors.iosBlue),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "GRADIENT BACKDROP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(defaultGroupGradients) { grad ->
                                    val isSel = grad.id == selectedGradient.id
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(grad.colors))
                                            .border(if (isSel) 2.5.dp else 0.dp, Color.White, CircleShape)
                                            .clickable {
                                                selectedGradient = grad
                                                SoundManager.playReaction()
                                            }
                                    )
                                }
                            }
                        }
                    }
                    "icons" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "CHOOSE SYMBOL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(defaultGroupIconsList) { icName ->
                                    val isSel = icName == selectedIcon
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSel) colors.iosBlue else colors.surfaceSecondary)
                                            .clickable {
                                                selectedIcon = icName
                                                SoundManager.playReaction()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GroupVectorIconRenderer(
                                            name = icName,
                                            size = 20.dp,
                                            tint = if (isSel) Color.White else colors.textDark
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "GRADIENT BACKDROP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(defaultGroupGradients) { grad ->
                                    val isSel = grad.id == selectedGradient.id
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(grad.colors))
                                            .border(if (isSel) 2.5.dp else 0.dp, Color.White, CircleShape)
                                            .clickable {
                                                selectedGradient = grad
                                                SoundManager.playReaction()
                                            }
                                    )
                                }
                            }
                        }
                    }
                    "emojis" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "CHOOSE EMOJI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(defaultGroupEmojisList) { emoji ->
                                    val isSel = emoji == selectedEmoji
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSel) colors.iosBlue.copy(alpha = 0.2f) else colors.surfaceSecondary)
                                            .border(if (isSel) 1.5.dp else 0.dp, colors.iosBlue, RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedEmoji = emoji
                                                SoundManager.playReaction()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 22.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "GRADIENT BACKDROP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(defaultGroupGradients) { grad ->
                                    val isSel = grad.id == selectedGradient.id
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(grad.colors))
                                            .border(if (isSel) 2.5.dp else 0.dp, Color.White, CircleShape)
                                            .clickable {
                                                selectedGradient = grad
                                                SoundManager.playReaction()
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Action Buttons (Cancel / Set as Group Icon)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isLight) Color(0xFFE5E5EA) else Color(0xFF3A3A3C))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textDark)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.iosBlue)
                            .clickable {
                                val resultAvatar = when (selectedTab) {
                                    "presets" -> selectedPresetAvatar
                                    else -> selectedPresetAvatar // In addition to vector presets, maps smoothly
                                }
                                SoundManager.playSend()
                                onApplyIcon(resultAvatar)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Set Group Icon", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupVectorIconRenderer(
    name: String,
    size: androidx.compose.ui.unit.Dp,
    tint: Color
) {
    when (name) {
        "users" -> PhosphorUserIcon(tint = tint, size = size)
        "code" -> PhosphorKeypadIcon(tint = tint, size = size)
        "rocket" -> PhosphorPaperPlaneTilt(tint = tint, size = size)
        "sparkles" -> LucideSparkles(tint = tint, size = size)
        "camera" -> PhosphorCameraIcon(tint = tint, size = size)
        "star" -> LucideStar(tint = tint, size = size)
        "flame" -> PhosphorSparkleIcon(tint = tint, size = size)
        "shield" -> LucideShieldCheck(tint = tint, size = size)
        "heart" -> LucideHeart(tint = tint, size = size)
        else -> PhosphorUserIcon(tint = tint, size = size)
    }
}


