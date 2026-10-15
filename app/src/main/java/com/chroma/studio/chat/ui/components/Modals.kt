package com.chroma.studio.chat.ui.components
import com.chroma.studio.R

import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.chroma.studio.chat.data.AccentColorType
import com.chroma.studio.chat.data.BubbleAnimationStyle
import com.chroma.studio.chat.data.BubbleStyleType
import com.chroma.studio.chat.data.ChatConversation
import com.chroma.studio.chat.data.CustomCornerConfig
import com.chroma.studio.chat.data.GroupMember
import com.chroma.studio.chat.data.PointyCornerStyle
import com.chroma.studio.chat.data.Story
import com.chroma.studio.chat.data.VoiceGlobeStyleType
import com.chroma.studio.chat.data.WallpaperType
import com.chroma.studio.chat.data.drawableUri
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.ChatWallpaperBackground
import com.chroma.studio.chat.ui.theme.LocalChatColors

/**
 * Apple iOS Frosted Glass Cover Banner Picker Modal.
 * Used for picking rich cover banners for groups or profiles.
 */
@Composable
fun IosCoverBannerPickerModal(
    currentBanner: String,
    onDismiss: () -> Unit,
    onApplyBanner: (String) -> Unit
) {
    val colors = LocalChatColors.current
    var selectedBanner by remember { mutableStateOf(currentBanner) }
    val bannerPresets = remember {
        WallpaperType.values().filter {
            it.drawableRes != null && (it.category == "Vibrant Waves" || it.category == "Geometric & Mesh")
        }
    }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Vibrant Waves", "Geometric & Mesh")

    val displayedBanners = remember(selectedCategory) {
        if (selectedCategory == "All") bannerPresets
        else bannerPresets.filter { it.category == selectedCategory }
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
                ) { onDismiss() }
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (colors.isLight) Color(0xF8FFFFFF) else Color(0xF21C1C1E))
                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Cover Banner",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textDark
                            )
                            Text(
                                text = "Choose a cover style for your group",
                                fontSize = 12.5.sp,
                                color = colors.textMuted
                            )
                        }
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

                    // Category Pill Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceSecondary)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSel = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) colors.cardBg else Color.Transparent)
                                    .clickable {
                                        SoundManager.playReaction()
                                        selectedCategory = cat
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSel) colors.textDark else colors.textMuted
                                )
                            }
                        }
                    }

                    // Banner Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(displayedBanners) { banner ->
                            val uri = drawableUri(banner.drawableRes!!)
                            val isSelected = selectedBanner == uri
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.5.dp,
                                        color = if (isSelected) colors.iosBlue else colors.borderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        SoundManager.playReaction()
                                        selectedBanner = uri
                                    }
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = banner.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Gradient for title text
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                            )
                                        )
                                )
                                Text(
                                    text = banner.title,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-6).dp, y = 6.dp)
                                            .clip(CircleShape)
                                            .background(colors.iosBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LucideCheck(tint = Color.White, size = 12.dp)
                                    }
                                }
                            }
                        }
                    }

                    // Apply Button
                    Button(
                        onClick = {
                            SoundManager.playSend()
                            onApplyBanner(selectedBanner)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.iosBlue)
                    ) {
                        Text("Apply Banner", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// 1. Apple HIG iOS Full Screen Contact Info / Group Info View
@Composable
fun ContactInfoDrawer(
    chat: ChatConversation,
    onClose: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
    onSearchMessages: () -> Unit,
    onMailClick: () -> Unit = {},
    onSendMessage: () -> Unit = {},
    onShareProfile: () -> Unit = {},
    onAddToFavorites: () -> Unit = {},
    onMuteNotifications: () -> Unit = {},
    onReportUser: () -> Unit = {},
    onBlockUser: () -> Unit = {},
    onUpdateGroupAvatar: ((String) -> Unit)? = null,
    onUpdateGroupBanner: ((String) -> Unit)? = null,
    onUpdateGroupName: ((String) -> Unit)? = null,
    onUpdateGroupBio: ((String) -> Unit)? = null,
    onAddParticipants: (() -> Unit)? = null,
    onAddGroupMembers: ((List<GroupMember>) -> Unit)? = null,
    onToggleMemberAdmin: ((GroupMember) -> Unit)? = null,
    onRemoveGroupMember: ((GroupMember) -> Unit)? = null,
    onDirectMessageMember: ((GroupMember) -> Unit)? = null,
    onLeaveGroup: (() -> Unit)? = null,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    val scrollState = rememberScrollState()
    val isGroup = chat.type == "group"

    // Group Management Dialog States
    var isIconStudioOpen by remember { mutableStateOf(false) }
    var isBannerPickerOpen by remember { mutableStateOf(false) }
    var isAddMemberModalOpen by remember { mutableStateOf(false) }
    var selectedMemberForAction by remember { mutableStateOf<GroupMember?>(null) }
    var isPermissionsModalOpen by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var isEditingBio by remember { mutableStateOf(false) }
    var isLeaveGroupSheetOpen by remember { mutableStateOf(false) }
    var isFullGalleryOpen by remember { mutableStateOf(false) }
    var fullGalleryInitialTab by remember { mutableIntStateOf(0) }

    var currentDisplayAvatar by remember(chat.avatar) { mutableStateOf(chat.avatar) }
    var currentDisplayBanner by remember(chat.banner) { mutableStateOf(chat.banner) }
    var mediaTab by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.appBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header spacer for sticky bar
            Spacer(Modifier.height(56.dp).windowInsetsPadding(WindowInsets.statusBars))

            // Main Centered Content Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 580.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cover Banner (strictly for banners)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    AsyncImage(
                        model = currentDisplayBanner.ifEmpty { drawableUri(com.chroma.studio.R.drawable.banner_wave_banner_midnight_glow) },
                        contentDescription = "Cover Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.10f))
                                )
                            )
                    )

                    // Change Cover Banner Pill Button for Groups
                    if (isGroup) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .clickable {
                                    SoundManager.playReaction()
                                    isBannerPickerOpen = true
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PhosphorCameraIcon(tint = Color.White, size = 13.dp)
                                Text(
                                    text = "Cover",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Main Overlapping Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-52).dp)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Avatar with Border & Online Status
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .shadow(4.dp, CircleShape)
                    ) {
                        AsyncImage(
                            model = currentDisplayAvatar,
                            contentDescription = chat.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(3.5.dp, colors.appBg, CircleShape)
                                .clickable {
                                    if (isGroup) {
                                        SoundManager.playReaction()
                                        isIconStudioOpen = true
                                    } else {
                                        onImageClick(chat.avatar)
                                    }
                                },
                            contentScale = ContentScale.Crop
                        )

                        if (isGroup) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-2).dp, y = (-2).dp)
                                    .background(colors.iosBlue, CircleShape)
                                    .border(2.5.dp, colors.appBg, CircleShape)
                                    .clickable {
                                        SoundManager.playReaction()
                                        isIconStudioOpen = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                PhosphorCameraIcon(tint = Color.White, size = 15.dp)
                            }
                        } else if (chat.online) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-3).dp, y = (-2).dp)
                                    .background(colors.success, CircleShape)
                                    .border(3.dp, colors.appBg, CircleShape)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Profile Display Name & Subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = isGroup) {
                                SoundManager.playReaction()
                                isEditingName = true
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = chat.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textDark,
                            textAlign = TextAlign.Center
                        )
                        if (chat.isVerified) {
                            Icon(
                                imageVector = com.composables.icons.lucide.Lucide.BadgeCheck,
                                contentDescription = "Verified Account",
                                tint = colors.iosBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (isGroup) {
                            LucideEdit(tint = colors.iosBlue, size = 16.dp)
                        }
                    }

                    Spacer(Modifier.height(3.dp))
                    val statusSubtitle = when {
                        isGroup -> "Group • ${chat.members?.size ?: 0} members"
                        chat.online -> "Online"
                        chat.statusText.isNotBlank() -> chat.statusText
                        else -> "Offline"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (!isGroup && chat.online) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(colors.success)
                            )
                        }
                        Text(
                            text = statusSubtitle,
                            fontSize = 14.sp,
                            fontWeight = if (!isGroup && chat.online) FontWeight.Medium else FontWeight.Normal,
                            color = if (!isGroup && chat.online) colors.success else colors.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (isGroup) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(colors.iosBlueSubtle)
                                    .clickable {
                                        SoundManager.playReaction()
                                        isIconStudioOpen = true
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    LucidePalette(tint = colors.iosBlue, size = 13.dp)
                                    Text(
                                        text = "Group Icon Studio",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.iosBlue
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(colors.surfaceSecondary)
                                    .clickable {
                                        SoundManager.playReaction()
                                        isBannerPickerOpen = true
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    LucideImage(tint = colors.textDark, size = 13.dp)
                                    Text(
                                        text = "Cover Banner",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textDark
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Action Buttons Row (audio, video, search, mute/mail)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Audio Call Action
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.cardBg)
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                                .clickable { onAudioCall() }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LucidePhone(tint = colors.iosBlue, size = 20.dp)
                            Text("audio", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = colors.iosBlue)
                        }

                        // Video Call Action
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.cardBg)
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                                .clickable { onVideoCall() }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LucideVideo(tint = colors.iosBlue, size = 20.dp)
                            Text("video", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = colors.iosBlue)
                        }

                        // Search Messages Action
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.cardBg)
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                                .clickable { onSearchMessages() }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LucideSearch(tint = colors.iosBlue, size = 20.dp)
                            Text("search", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = colors.iosBlue)
                        }

                        // 4th Action: Mute for Group, Mail for direct contact
                        if (isGroup) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.cardBg)
                                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                                    .clickable { onMuteNotifications() }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (chat.isMuted) {
                                    LucideBellOff(tint = colors.iosBlue, size = 20.dp)
                                    Text("unmute", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = colors.iosBlue)
                                } else {
                                    LucideBell(tint = colors.iosBlue, size = 20.dp)
                                    Text("mute", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = colors.iosBlue)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.cardBg)
                                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                                    .clickable { onMailClick() }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                LucideMail(tint = colors.iosBlue, size = 20.dp)
                                Text("mail", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = colors.iosBlue)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Details Card: Group Info vs User Contact Info ────────
                    if (isGroup) {
                        // Group Details Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBg)
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            // Description Row (Clickable to Edit)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SoundManager.playReaction()
                                        isEditingBio = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("description", fontSize = 12.5.sp, color = colors.textDark, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = if (chat.bio.isNotBlank()) chat.bio else "Team chat for project planning, design feedback, and team sync.",
                                        fontSize = 15.sp,
                                        color = colors.textDark,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                LucideEdit(tint = colors.iosBlue, size = 15.dp, modifier = Modifier.padding(top = 4.dp, start = 8.dp))
                            }

                            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

                            // Group Permissions & Settings Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SoundManager.playReaction()
                                        isPermissionsModalOpen = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(colors.iosBlueSubtle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LucideLock(tint = colors.iosBlue, size = 15.dp)
                                    }
                                    Column {
                                        Text("Group Permissions", fontSize = 15.sp, color = colors.textDark, fontWeight = FontWeight.Medium)
                                        Text("Send messages, edit info, add members", fontSize = 12.sp, color = colors.textMuted)
                                    }
                                }
                                LucideChevronRight(tint = colors.textMuted, size = 18.dp)
                            }

                            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

                            // Group Invite Link Row
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onShareProfile() }
                                    .padding(horizontal = 16.dp, vertical = 11.dp)
                            ) {
                                Text("invite link", fontSize = 12.5.sp, color = colors.textDark, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "https://oscellion.chat/join/${chat.id}",
                                    fontSize = 15.sp,
                                    color = colors.iosBlue,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

                            // Created Info Row
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 11.dp)
                            ) {
                                Text("created", fontSize = 12.5.sp, color = colors.textDark, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "Created by Alex Rivera • ${chat.members?.size ?: 0} members",
                                    fontSize = 14.sp,
                                    color = colors.textMuted,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Group Members Section ───────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "MEMBERS (${chat.members?.size ?: 0})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMuted,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.cardBg)
                                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                            ) {
                                // Add Participants Action Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            SoundManager.playReaction()
                                            if (onAddParticipants != null) {
                                                onAddParticipants()
                                            } else {
                                                isAddMemberModalOpen = true
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(colors.iosBlueSubtle, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.iosBlue)
                                    }
                                    Text(
                                        text = "Add Participants...",
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.iosBlue
                                    )
                                }

                                if (chat.members != null && chat.members.isNotEmpty()) {
                                    HorizontalDivider(
                                        color = colors.separator,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 62.dp)
                                    )

                                    chat.members.forEachIndexed { index, member ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    SoundManager.playReaction()
                                                    selectedMemberForAction = member
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            AsyncImage(
                                                model = member.avatar,
                                                contentDescription = member.name,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .border(0.5.dp, colors.borderSubtle, CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = member.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textDark
                                                )
                                                Text(
                                                    text = member.role,
                                                    fontSize = 12.5.sp,
                                                    color = colors.textMuted
                                                )
                                            }

                                            if (member.role.contains("Admin", ignoreCase = true) || member.role.contains("Owner", ignoreCase = true)) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(colors.iosBlueSubtle)
                                                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                                ) {
                                                    Text(
                                                        text = if (member.role.contains("Owner", ignoreCase = true)) "Owner" else "Admin",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = colors.iosBlue
                                                    )
                                                }
                                            }

                                            LucideChevronRight(tint = colors.textMuted.copy(alpha = 0.6f), size = 16.dp)
                                        }

                                        if (index < chat.members.size - 1) {
                                            HorizontalDivider(
                                                color = colors.separator,
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(start = 64.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    } else {
                        val context = LocalContext.current
                        val clipboardManager = LocalClipboardManager.current
                        val usernameText = "@${chat.name.lowercase().replace(Regex("[^a-zA-Z0-9]"), ".")}"

                        // User Profile Details Card (Username & Phone)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBg)
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            // Username Row (Tap to copy)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(usernameText))
                                        Toast.makeText(context, "Username copied", Toast.LENGTH_SHORT).show()
                                        SoundManager.playReaction()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 11.dp)
                            ) {
                                Text("username", fontSize = 12.5.sp, color = colors.textMuted, fontWeight = FontWeight.Normal)
                                Spacer(Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = usernameText,
                                        fontSize = 16.sp,
                                        color = colors.iosBlue,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Lucide.Copy,
                                        contentDescription = "Copy Username",
                                        tint = colors.textMuted.copy(alpha = 0.6f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

                            // Phone Row (Tap to copy)
                            val displayPhone = if (chat.phone.isNotBlank()) chat.phone else "Not provided"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = chat.phone.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(chat.phone))
                                        Toast.makeText(context, "Phone number copied", Toast.LENGTH_SHORT).show()
                                        SoundManager.playReaction()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 11.dp)
                            ) {
                                Text("phone", fontSize = 12.5.sp, color = colors.textMuted, fontWeight = FontWeight.Normal)
                                Spacer(Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = displayPhone,
                                        fontSize = 16.sp,
                                        color = if (chat.phone.isNotBlank()) colors.textDark else colors.textMuted,
                                        fontWeight = FontWeight.Normal
                                    )
                                    if (chat.phone.isNotBlank()) {
                                        Icon(
                                            imageVector = Lucide.Copy,
                                            contentDescription = "Copy Phone",
                                            tint = colors.textMuted.copy(alpha = 0.6f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Bio Card for User (Tap to copy)
                        val displayBio = if (chat.bio.isNotBlank()) chat.bio else "No bio provided"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBg)
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                                .clickable(enabled = chat.bio.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(chat.bio))
                                    Toast.makeText(context, "Bio copied", Toast.LENGTH_SHORT).show()
                                    SoundManager.playReaction()
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("bio", fontSize = 12.5.sp, color = colors.textMuted, fontWeight = FontWeight.Normal)
                                if (chat.bio.isNotBlank()) {
                                    Icon(
                                        imageVector = Lucide.Copy,
                                        contentDescription = "Copy Bio",
                                        tint = colors.textMuted.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = displayBio,
                                fontSize = 15.5.sp,
                                color = if (chat.bio.isNotBlank()) colors.textDark else colors.textMuted,
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                    }

                    // ── Shared Content Section (Media, Links, Docs) ─────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Section Header with See All
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MEDIA, LINKS & DOCS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMuted
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        SoundManager.playReaction()
                                        fullGalleryInitialTab = mediaTab
                                        isFullGalleryOpen = true
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "See All",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.iosBlue
                                )
                                LucideChevronRight(tint = colors.iosBlue, size = 14.dp)
                            }
                        }

                        // Section Segmented Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceSecondary)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Media", "Links", "Docs").forEachIndexed { tabIdx, tabTitle ->
                                val isSelected = mediaTab == tabIdx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) colors.cardBg else Color.Transparent)
                                        .clickable {
                                            SoundManager.playReaction()
                                            mediaTab = tabIdx
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabTitle,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) colors.textDark else colors.textMuted
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        when (mediaTab) {
                            0 -> {
                                // Media Images Grid
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.cardBg)
                                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val mediaList = listOf(
                                        "https://images.unsplash.com/photo-1558002038-1055907df827?q=80&w=300&auto=format&fit=crop",
                                        "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=300&auto=format&fit=crop",
                                        "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?q=80&w=300&auto=format&fit=crop"
                                    )
                                    mediaList.forEach { url ->
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Shared Media",
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    SoundManager.playReaction()
                                                    fullGalleryInitialTab = 0
                                                    isFullGalleryOpen = true
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                            1 -> {
                                // Shared Links
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.cardBg)
                                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                SoundManager.playReaction()
                                                fullGalleryInitialTab = 2
                                                isFullGalleryOpen = true
                                            }
                                            .padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(colors.iosBlueSubtle, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LucideSearch(tint = colors.iosBlue, size = 16.dp)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "https://figma.com/file/OscellionDesignSystem",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.iosBlue,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text("Figma design tokens & screen specifications", fontSize = 12.sp, color = colors.textMuted)
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Shared Documents
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.cardBg)
                                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                SoundManager.playReaction()
                                                fullGalleryInitialTab = 1
                                                isFullGalleryOpen = true
                                            }
                                            .padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(Color(0xFFFF3B30).copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF3B30))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Product_Roadmap_Q4.pdf",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textDark
                                            )
                                            Text("2.4 MB • Shared yesterday", fontSize = 12.sp, color = colors.textMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Action Groups & Danger Options ──────────────────────
                    if (isGroup) {
                        // Group Action Group
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBg)
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onShareProfile() }
                                    .padding(horizontal = 16.dp, vertical = 13.dp)
                            ) {
                                Text("Share Group Link", fontSize = 16.5.sp, color = colors.iosBlue, fontWeight = FontWeight.Normal)
                            }
                            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMuteNotifications() }
                                    .padding(horizontal = 16.dp, vertical = 13.dp)
                            ) {
                                Text(
                                    text = if (chat.isMuted) "Unmute Group" else "Mute Group Notifications",
                                    fontSize = 16.5.sp,
                                    color = colors.iosBlue,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Group Danger Zone: Leave Group
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBg)
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SoundManager.playReaction()
                                        isLeaveGroupSheetOpen = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 13.dp)
                            ) {
                                Text(
                                    text = "Leave Group",
                                    fontSize = 16.5.sp,
                                    color = Color(0xFFFF3B30),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        // User Profile Actions
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBg)
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSendMessage() }
                                    .padding(horizontal = 16.dp, vertical = 13.dp)
                            ) {
                                Text("Send Message", fontSize = 16.5.sp, color = colors.iosBlue, fontWeight = FontWeight.Normal)
                            }
                            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onShareProfile() }
                                    .padding(horizontal = 16.dp, vertical = 13.dp)
                            ) {
                                Text("Share Contact", fontSize = 16.5.sp, color = colors.iosBlue, fontWeight = FontWeight.Normal)
                            }
                            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAddToFavorites() }
                                    .padding(horizontal = 16.dp, vertical = 13.dp)
                            ) {
                                Text("Add to Favorites", fontSize = 16.5.sp, color = colors.iosBlue, fontWeight = FontWeight.Normal)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // User Danger Options
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.cardBg)
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onReportUser() }
                                    .padding(horizontal = 16.dp, vertical = 13.dp)
                            ) {
                                Text("Report Contact", fontSize = 16.5.sp, color = Color(0xFFFF3B30), fontWeight = FontWeight.Normal)
                            }
                            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onBlockUser() }
                                    .padding(horizontal = 16.dp, vertical = 13.dp)
                            ) {
                                Text(
                                    text = if (chat.isBlocked) "Unblock this Contact" else "Block this Contact",
                                    fontSize = 16.5.sp,
                                    color = Color(0xFFFF3B30),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        // ── Solid Apple iOS Sticky Navigation Bar ───────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(colors.appBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClose() }
                        .padding(end = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    LucideChevronLeft(tint = colors.iosBlue, size = 22.dp)
                    Text(
                        text = "Back",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.iosBlue
                    )
                }

                // Dynamic Navigation Title (visible when scrolled)
                Text(
                    text = if (scrollState.value > 120) chat.name else (if (isGroup) "Group Info" else "Contact Info"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceSecondary)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    LucideX(tint = colors.textMuted, size = 14.dp)
                }
            }

            HorizontalDivider(color = colors.separator, thickness = 0.5.dp)
        }

        // ── Group Modals ─────────────────────────────────────────────────
        if (isIconStudioOpen) {
            GroupIconCreatorModal(
                currentAvatar = currentDisplayAvatar,
                initialGroupName = chat.name,
                onDismiss = { isIconStudioOpen = false },
                onApplyIcon = { newIcon ->
                    currentDisplayAvatar = newIcon
                    onUpdateGroupAvatar?.invoke(newIcon)
                    isIconStudioOpen = false
                }
            )
        }

        if (isBannerPickerOpen) {
            IosCoverBannerPickerModal(
                currentBanner = currentDisplayBanner,
                onDismiss = { isBannerPickerOpen = false },
                onApplyBanner = { newBanner ->
                    currentDisplayBanner = newBanner
                    onUpdateGroupBanner?.invoke(newBanner)
                    isBannerPickerOpen = false
                }
            )
        }

        if (isAddMemberModalOpen) {
            IosAddGroupMemberModal(
                existingMemberNames = (chat.members ?: emptyList()).map { it.name }.toSet(),
                onDismiss = { isAddMemberModalOpen = false },
                onAddMembers = { newContacts ->
                    val addedMembers = newContacts.map {
                        GroupMember(name = it.name, role = it.role, avatar = it.avatar)
                    }
                    onAddGroupMembers?.invoke(addedMembers)
                    isAddMemberModalOpen = false
                }
            )
        }

        selectedMemberForAction?.let { member ->
            IosMemberActionSheetModal(
                member = member,
                onDismiss = { selectedMemberForAction = null },
                onDirectMessage = {
                    selectedMemberForAction = null
                    onDirectMessageMember?.invoke(member)
                },
                onToggleAdmin = {
                    selectedMemberForAction = null
                    onToggleMemberAdmin?.invoke(member)
                },
                onRemoveMember = {
                    selectedMemberForAction = null
                    onRemoveGroupMember?.invoke(member)
                }
            )
        }

        if (isPermissionsModalOpen) {
            IosGroupPermissionsModal(
                groupName = chat.name,
                onDismiss = { isPermissionsModalOpen = false },
                onSave = {
                    isPermissionsModalOpen = false
                }
            )
        }

        if (isEditingName) {
            IosInputDialog(
                title = "Edit Group Subject",
                message = "Enter a new subject for this group.",
                initialValue = chat.name,
                placeholder = "Group Name",
                confirmTitle = "Save",
                onConfirm = { newName ->
                    if (newName.isNotBlank()) {
                        onUpdateGroupName?.invoke(newName.trim())
                    }
                    isEditingName = false
                },
                onDismiss = { isEditingName = false }
            )
        }

        if (isEditingBio) {
            IosInputDialog(
                title = "Edit Group Description",
                message = "Enter a description for group participants.",
                initialValue = chat.bio,
                placeholder = "Group Description",
                confirmTitle = "Save",
                onConfirm = { newBio ->
                    onUpdateGroupBio?.invoke(newBio.trim())
                    isEditingBio = false
                },
                onDismiss = { isEditingBio = false }
            )
        }

        if (isLeaveGroupSheetOpen) {
            IosActionSheet(
                title = "Leave \"${chat.name}\"?",
                message = "You will no longer receive messages from this group unless re-added by an admin.",
                actions = listOf(
                    IosAlertAction(
                        title = "Leave Group",
                        isDestructive = true,
                        onClick = {
                            onLeaveGroup?.invoke()
                        }
                    )
                ),
                onDismiss = { isLeaveGroupSheetOpen = false }
            )
        }

        if (isFullGalleryOpen) {
            CustomSharedGalleryModal(
                chat = chat,
                allChats = listOf(chat),
                initialTab = fullGalleryInitialTab,
                onClose = { isFullGalleryOpen = false },
                onReplyPrivately = { senderName, mediaItem ->
                    isFullGalleryOpen = false
                    val matchingMember = chat.members?.firstOrNull { it.name.equals(senderName, ignoreCase = true) }
                    if (matchingMember != null) {
                        onDirectMessageMember?.invoke(matchingMember)
                    }
                },
                onForwardItems = { _, _ -> },
                onToast = { }
            )
        }
    }
}

// 2. Complete Story Viewer Modal (Apple iOS Frosted Glass & Dual Mode)
data class UserStoryGroup(
    val authorName: String,
    val avatar: String,
    val isMyStory: Boolean,
    val stories: List<Story>
)

// 2. Complete Story Viewer Modal (Apple iOS 3D Cube Carousel & Per-User Sequences)
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StoryViewerModal(
    stories: List<Story>,
    initialStoryIndex: Int = 0,
    onClose: () -> Unit,
    onReaction: (String) -> Unit,
    onReplySend: (String) -> Unit,
    onOpenAnalytics: (Story) -> Unit,
    onDeleteStory: (String) -> Unit
) {
    if (stories.isEmpty()) return

    // Group stories by author so each user has their own distinct story sequence
    val storyGroups = remember(stories) {
        stories.groupBy { it.authorName }.map { (author, userStories) ->
            UserStoryGroup(
                authorName = author,
                avatar = userStories.first().avatar,
                isMyStory = userStories.any { it.isMyStory },
                stories = userStories
            )
        }
    }

    val activeInitialStory = stories.getOrNull(initialStoryIndex)
    val initialGroupIndex = remember(activeInitialStory) {
        if (activeInitialStory != null) {
            storyGroups.indexOfFirst { group -> group.stories.any { it.id == activeInitialStory.id } }.coerceAtLeast(0)
        } else 0
    }
    val initialStoryIndexInGroup = remember(activeInitialStory) {
        if (activeInitialStory != null) {
            val group = storyGroups.getOrNull(initialGroupIndex)
            group?.stories?.indexOfFirst { it.id == activeInitialStory.id }?.coerceAtLeast(0) ?: 0
        } else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialGroupIndex,
        pageCount = { storyGroups.size }
    )
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { pageIndex -> storyGroups.getOrNull(pageIndex)?.authorName ?: pageIndex }
        ) { pageIndex ->
            val group = storyGroups.getOrNull(pageIndex) ?: return@HorizontalPager

            // 3D Cube Perspective transformation calculation
            val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
            val isCurrentPage = pagerState.currentPage == pageIndex && !pagerState.isScrollInProgress

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 3D Rotation along Y-axis
                        val rotation = (pageOffset * 90f).coerceIn(-90f, 90f)
                        rotationY = rotation
                        cameraDistance = 14f * density

                        // Pivot point on the edge of the 3D cube
                        transformOrigin = TransformOrigin(
                            pivotFractionX = if (pageOffset <= 0f) 1f else 0f,
                            pivotFractionY = 0.5f
                        )

                        // 3D perspective depth scale & lighting shadow
                        val absOffset = kotlin.math.abs(pageOffset)
                        val scale = (1f - 0.20f * absOffset).coerceIn(0.80f, 1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - 0.35f * absOffset).coerceIn(0.1f, 1f)
                    }
                    .background(Color.Black)
            ) {
                UserStoryPageView(
                    userGroup = group,
                    isActive = isCurrentPage,
                    initialStoryIndex = if (pageIndex == initialGroupIndex) initialStoryIndexInGroup else 0,
                    onNextUser = {
                        if (pageIndex < storyGroups.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pageIndex + 1)
                            }
                        } else {
                            onClose()
                        }
                    },
                    onPreviousUser = {
                        if (pageIndex > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pageIndex - 1)
                            }
                        }
                    },
                    onClose = onClose,
                    onReaction = onReaction,
                    onReplySend = onReplySend,
                    onOpenAnalytics = onOpenAnalytics,
                    onDeleteStory = onDeleteStory
                )
            }
        }
    }
}

/**
 * Individual User Story Page with per-user progress segments and localized navigation.
 */
@Composable
private fun UserStoryPageView(
    userGroup: UserStoryGroup,
    isActive: Boolean,
    initialStoryIndex: Int = 0,
    onNextUser: () -> Unit,
    onPreviousUser: () -> Unit,
    onClose: () -> Unit,
    onReaction: (String) -> Unit,
    onReplySend: (String) -> Unit,
    onOpenAnalytics: (Story) -> Unit,
    onDeleteStory: (String) -> Unit
) {
    val colors = LocalChatColors.current
    val stories = userGroup.stories
    var currentIndex by remember { mutableIntStateOf(initialStoryIndex.coerceIn(0, (stories.size - 1).coerceAtLeast(0))) }
    val currentStory = if (stories.isNotEmpty() && currentIndex in stories.indices) stories[currentIndex] else stories.firstOrNull()

    var replyText by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    // Auto-Advance Timer for this user's story sequence
    LaunchedEffect(currentStory?.id, isPaused, isActive) {
        if (!isActive || currentStory == null) {
            return@LaunchedEffect
        }
        val duration = 5000L
        val interval = 40L
        val step = interval.toFloat() / duration
        progress = 0f
        while (progress < 1f && !isPaused && isActive) {
            kotlinx.coroutines.delay(interval)
            progress += step
        }
        if (progress >= 1f && !isPaused && isActive) {
            if (currentIndex < stories.size - 1) {
                currentIndex++
                progress = 0f
            } else {
                onNextUser()
            }
        }
    }

    if (currentStory == null) return

    val fontFamilies = listOf(
        androidx.compose.ui.text.font.FontFamily.Default,
        androidx.compose.ui.text.font.FontFamily.Serif,
        androidx.compose.ui.text.font.FontFamily.Monospace,
        androidx.compose.ui.text.font.FontFamily.Cursive
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- 1. STORY BACKGROUND LAYER ---
        if (currentStory.image != null) {
            AsyncImage(
                model = currentStory.image,
                contentDescription = "Story Media",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (currentStory.bgGradientColors != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(currentStory.bgGradientColors!!))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF141414))
            )
        }

        // --- 2. TOP & BOTTOM VIGNETTE / SCRIM GRADIENTS ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.70f),
                        0.6f to Color.Black.copy(alpha = 0.25f),
                        1.0f to Color.Transparent
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.4f to Color.Black.copy(alpha = 0.40f),
                        1.0f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )

        // --- 3. TAP ZONES (Next/Prev & Long Press within User & Across Users) ---
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = {
                                if (currentIndex > 0) {
                                    currentIndex--
                                    progress = 0f
                                } else {
                                    onPreviousUser()
                                }
                            }
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = {
                                if (currentIndex < stories.size - 1) {
                                    currentIndex++
                                    progress = 0f
                                } else {
                                    onNextUser()
                                }
                            }
                        )
                    }
            )
        }

        // --- 4. DYNAMIC STORY CONTENT (Centered) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 120.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Text Caption
            if (currentStory.caption.isNotBlank()) {
                val highlightBg = when (currentStory.textHighlightMode) {
                    "TRANSLUCENT_BLACK" -> Color.Black.copy(alpha = 0.65f)
                    "SOLID_WHITE" -> Color.White
                    "ACCENT_BLUE" -> colors.iosBlue
                    else -> Color.Transparent
                }
                val textColor = if (currentStory.textHighlightMode == "SOLID_WHITE") Color.Black else Color.White
                val align = when (currentStory.textAlignStr) {
                    "Start" -> TextAlign.Start
                    "End" -> TextAlign.End
                    else -> TextAlign.Center
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (highlightBg != Color.Transparent) {
                                Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(highlightBg)
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            } else Modifier.padding(horizontal = 8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentStory.caption,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilies.getOrElse(currentStory.fontIndex) { fontFamilies[0] },
                        color = textColor,
                        textAlign = align,
                        lineHeight = 33.sp,
                        style = if (highlightBg == Color.Transparent) {
                            TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.75f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 8f
                                )
                            )
                        } else TextStyle.Default
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Interactive Poll
            if (currentStory.pollData != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LucideBarChart2(tint = colors.iosBlue, size = 18.dp)
                            Text(currentStory.pollData.question, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        if (currentStory.isMyStory) {
                            Text("Admin Results", fontSize = 11.sp, color = colors.textMuted, fontWeight = FontWeight.SemiBold)
                            currentStory.pollData.options.forEach { opt ->
                                val pct = if (currentStory.pollData.totalVotersCount > 0) (opt.votes.toFloat() / currentStory.pollData.totalVotersCount) else 0f
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(opt.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                        Text("${opt.votes} votes", fontSize = 12.sp, color = colors.textMuted)
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.appBg)) {
                                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(pct).background(colors.iosBlue))
                                    }
                                }
                            }
                        } else {
                            currentStory.pollData.options.forEach { opt ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.appBg)
                                        .clickable { SoundManager.playReaction(); onReaction("Voted") }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(opt.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.iosBlue)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. TOP & BOTTOM HUD WITH SAFE INSETS ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Group: ONLY THIS USER's stories count!
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { index, _ ->
                        val segmentProgress = when {
                            index < currentIndex -> 1f
                            index == currentIndex -> progress
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color.White.copy(alpha = 0.35f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(segmentProgress)
                                    .background(Color.White)
                            )
                        }
                    }
                }

                // Author Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.size(40.dp)) {
                            AsyncImage(
                                model = userGroup.avatar,
                                contentDescription = userGroup.authorName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = userGroup.authorName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.60f),
                                            offset = Offset(0f, 1f),
                                            blurRadius = 4f
                                        )
                                    )
                                )
                                if (userGroup.isMyStory) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.iosBlue)
                                            .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = "You",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(
                                text = currentStory.timeAgo,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.80f),
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.60f),
                                        offset = Offset(0f, 1f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                    }

                    // Top Action Buttons (Clean floating icons with dark shadow)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (userGroup.isMyStory) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        SoundManager.playReaction()
                                        isPaused = true
                                        onOpenAnalytics(currentStory)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.offset(x = 0.5.dp, y = 1.dp)) {
                                    LucidePoll(tint = Color.Black.copy(alpha = 0.65f), size = 20.dp)
                                }
                                LucidePoll(tint = Color.White, size = 20.dp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    SoundManager.playReaction()
                                    onClose()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.offset(x = 0.5.dp, y = 1.dp)) {
                                PhosphorCloseX(tint = Color.Black.copy(alpha = 0.65f), size = 19.dp)
                            }
                            PhosphorCloseX(tint = Color.White, size = 19.dp)
                        }
                    }
                }
            }

            // Footer Group
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (currentStory.locationTag != null || currentStory.musicTrack != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStory.locationTag != null) {
                            Row(
                                modifier = Modifier
                                    .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.30f))
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White)
                                    .border(0.5.dp, Color(0xFFE5E5EA), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                LucideMapPin(tint = Color(0xFFFF3B30), size = 13.dp)
                                Text(
                                    text = currentStory.locationTag,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E)
                                )
                            }
                        }
                        if (currentStory.musicTrack != null) {
                            Row(
                                modifier = Modifier
                                    .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.30f))
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White)
                                    .border(0.5.dp, Color(0xFFE5E5EA), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PhosphorWaveform(tint = Color(0xFFFF2D55), size = 14.dp)
                                Text(
                                    text = currentStory.musicTrack,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E)
                                )
                            }
                        }
                    }
                }

                if (userGroup.isMyStory) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFF1C1C1E).copy(alpha = 0.92f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(26.dp))
                            .clickable {
                                SoundManager.playReaction()
                                isPaused = true
                                onOpenAnalytics(currentStory)
                            }
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LucideEye(tint = Color.White, size = 16.dp)
                                Text(
                                    text = "${currentStory.viewCount} Views",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text("•", color = Color.White.copy(alpha = 0.40f), fontSize = 13.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LucideHeart(tint = Color(0xFFFF3B30), isFilled = true, size = 16.dp)
                                Text(
                                    text = "${currentStory.likesCount} Likes",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Story Analytics",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.iosBlue
                            )
                            LucideChevronRight(tint = colors.iosBlue, size = 14.dp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1C1C1E).copy(alpha = 0.88f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (replyText.isEmpty()) {
                                        Text(
                                            text = "Send a reply...",
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.55f)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    SoundManager.playReaction()
                                    onReaction("❤️")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.offset(x = 0.5.dp, y = 1.2.dp)) {
                                LucideHeart(tint = Color.Black.copy(alpha = 0.65f), isFilled = true, size = 26.dp)
                            }
                            LucideHeart(tint = Color(0xFFFF3B30), isFilled = true, size = 26.dp)
                        }

                        if (replyText.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(colors.iosBlue)
                                    .clickable {
                                        SoundManager.playSend()
                                        onReplySend(replyText)
                                        replyText = ""
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                PhosphorSendArrowIcon(tint = Color.White, size = 20.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun FullCallModal(
    chat: ChatConversation,
    callType: String,
    callDurationSec: Int,
    onToggleMic: () -> Unit,
    onToggleCam: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onHangup: () -> Unit
) {
    val min = callDurationSec / 60
    val sec = callDurationSec % 60
    val timerStr = "$min:${sec.toString().padStart(2, '0')}"
    var isMicMuted by remember { mutableStateOf(false) }
    var isCamOn by remember { mutableStateOf(callType.contains("Video")) }
    var isSpeakerOn by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Apple In-Call Header Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Text(
                    text = if (isCamOn) "FaceTime Video" else "FaceTime Audio",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = chat.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = timerStr,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF34C759)
                )
            }

            // In-Call Center Avatar / Video Poster
            if (isCamOn) {
                Box(
                    modifier = Modifier
                        .size(280.dp, 360.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1C1C1E))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                ) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=600&q=80",
                        contentDescription = "Video feed",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${chat.name} (Live)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = chat.avatar,
                        contentDescription = chat.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Apple 6-Button Call Grid + End Call Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Row 1: Mute, Keypad, Speaker
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppleCallIconButton(
                        icon = {
                            if (isMicMuted) LucideMicOff(tint = Color.Black, size = 26.dp)
                            else LucideMic(tint = Color.White, size = 26.dp)
                        },
                        label = "mute",
                        isActive = isMicMuted,
                        onClick = { isMicMuted = !isMicMuted; onToggleMic() }
                    )
                    AppleCallIconButton(
                        icon = { PhosphorKeypadIcon(tint = Color.White, size = 26.dp) },
                        label = "keypad",
                        isActive = false,
                        onClick = {}
                    )
                    AppleCallIconButton(
                        icon = {
                            if (isSpeakerOn) LucideVolume2(tint = Color.Black, size = 26.dp)
                            else LucideVolume2(tint = Color.White, size = 26.dp)
                        },
                        label = "audio",
                        isActive = isSpeakerOn,
                        onClick = { isSpeakerOn = !isSpeakerOn; onToggleSpeaker() }
                    )
                }

                // Row 2: Add Call, FaceTime, Contacts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppleCallIconButton(
                        icon = { PhosphorPlusIcon(tint = Color.White, size = 24.dp) },
                        label = "add call",
                        isActive = false,
                        onClick = {}
                    )
                    AppleCallIconButton(
                        icon = {
                            if (isCamOn) LucideVideo(tint = Color.Black, size = 26.dp)
                            else LucideVideoOff(tint = Color.White, size = 26.dp)
                        },
                        label = "FaceTime",
                        isActive = isCamOn,
                        onClick = { isCamOn = !isCamOn; onToggleCam() }
                    )
                    AppleCallIconButton(
                        icon = { PhosphorUserIcon(tint = Color.White, size = 26.dp) },
                        label = "contacts",
                        isActive = false,
                        onClick = {}
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Bottom End Call Button (Apple Red 72dp Circle)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                        .clickable { onHangup() },
                    contentAlignment = Alignment.Center
                ) {
                    LucideCallEnd(tint = Color.White, size = 32.dp)
                }
            }
        }
    }
}

@Composable
private fun AppleCallIconButton(
    icon: @Composable () -> Unit,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.White else Color.White.copy(alpha = 0.16f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

// 4. Authentic Apple HIG Inset Grouped Modal: Poll Creator
@Composable
fun PollCreatorModal(
    onDismiss: () -> Unit,
    onPostPoll: (String, List<String>) -> Unit
) {
    val colors = LocalChatColors.current
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("Option 1", "Option 2") }
    var isAnonymous by remember { mutableStateOf(true) }
    var allowMultiple by remember { mutableStateOf(false) }

    val isValid = question.isNotBlank() && options.filter { it.isNotBlank() }.size >= 2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (colors.isLight) Color(0xFFF2F2F7) else Color(0xFF1C1C1E))
                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // iOS Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (colors.isLight) Color(0xFFFFFFFF) else Color(0xFF2C2C2E))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.iosBlue,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                SoundManager.playReaction()
                                onDismiss()
                            }
                    )

                    Text(
                        text = "New Poll",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textDark
                    )

                    Text(
                        text = "Create",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) colors.iosBlue else colors.textMuted.copy(alpha = 0.4f),
                        modifier = Modifier
                            .clickable(
                                enabled = isValid,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                val validOptions = options.filter { it.isNotBlank() }
                                if (isValid) {
                                    SoundManager.playReaction()
                                    onPostPoll(question, validOptions)
                                    onDismiss()
                                }
                            }
                    )
                }

                HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

                // Form Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: Question Inset Group
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "QUESTION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (colors.isLight) Color.White else Color(0xFF2C2C2E))
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = question,
                                onValueChange = { question = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontSize = 16.sp, color = colors.textDark),
                                cursorBrush = SolidColor(colors.iosBlue),
                                decorationBox = { inner ->
                                    if (question.isEmpty()) {
                                        Text("Ask a question...", fontSize = 16.sp, color = colors.textMuted.copy(alpha = 0.7f))
                                    }
                                    inner()
                                }
                            )
                        }
                    }

                    // Section 2: Options Inset Group
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OPTIONS (${options.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMuted
                            )
                            if (options.size < 8) {
                                Text(
                                    text = "+ Add Option",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.iosBlue,
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            SoundManager.playReaction()
                                            options.add("")
                                        }
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (colors.isLight) Color.White else Color(0xFF2C2C2E))
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                        ) {
                            options.forEachIndexed { idx, opt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colors.iosBlue)
                                    )
                                    BasicTextField(
                                        value = opt,
                                        onValueChange = { options[idx] = it },
                                        modifier = Modifier.weight(1f),
                                        cursorBrush = SolidColor(colors.iosBlue),
                                        textStyle = TextStyle(fontSize = 15.sp, color = colors.textDark),
                                        decorationBox = { inner ->
                                            if (opt.isEmpty()) {
                                                Text("Option ${idx + 1}", fontSize = 15.sp, color = colors.textMuted.copy(alpha = 0.7f))
                                            }
                                            inner()
                                        }
                                    )

                                    if (options.size > 2) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (colors.isLight) Color(0xFFF2F2F7) else Color(0xFF3A3A3C))
                                                .clickable {
                                                    SoundManager.playReaction()
                                                    options.removeAt(idx)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            PhosphorCloseX(tint = colors.textMuted, size = 11.dp)
                                        }
                                    }
                                }

                                if (idx < options.size - 1) {
                                    HorizontalDivider(
                                        color = colors.separator,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 32.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Section 3: Settings Inset Group
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SETTINGS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted,
                            modifier = Modifier.padding(start = 12.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (colors.isLight) Color.White else Color(0xFF2C2C2E))
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                        ) {
                            // Anonymous Voting Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isAnonymous = !isAnonymous }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Anonymous Voting", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textDark)
                                    Text("Voters are kept private", fontSize = 12.sp, color = colors.textMuted)
                                }
                                Switch(
                                    checked = isAnonymous,
                                    onCheckedChange = { isAnonymous = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.iosBlue,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = if (colors.isLight) Color(0xFFE5E5EA) else Color(0xFF39393D)
                                    )
                                )
                            }

                            HorizontalDivider(
                                color = colors.separator,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 14.dp)
                            )

                            // Multiple Answers Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { allowMultiple = !allowMultiple }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Multiple Answers", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textDark)
                                    Text("Allow selecting multiple choices", fontSize = 12.sp, color = colors.textMuted)
                                }
                                Switch(
                                    checked = allowMultiple,
                                    onCheckedChange = { allowMultiple = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.iosBlue,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = if (colors.isLight) Color(0xFFE5E5EA) else Color(0xFF39393D)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. Apple Photos Grade Lightbox Modal (Gesture Zoom & Actions)
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LightboxModal(
    imageUrl: String? = null,
    imageUrls: List<String> = emptyList(),
    initialIndex: Int = 0,
    senderName: String? = null,
    subtitle: String? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val effectiveImages = remember(imageUrl, imageUrls) {
        if (imageUrls.isNotEmpty()) imageUrls
        else listOfNotNull(imageUrl)
    }
    val count = effectiveImages.size
    val safeInitialIndex = initialIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = safeInitialIndex) { count }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Reset zoom when swiping across pages
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "lightboxScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.98f))
    ) {
        // Main Horizontal Pager for swipeable photo viewing
        if (effectiveImages.isNotEmpty()) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                userScrollEnabled = scale <= 1.05f,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 56.dp)
            ) { pageIndex ->
                val pageUrl = effectiveImages[pageIndex]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pageIndex) {
                            detectTapGestures(
                                onDoubleTap = {
                                    scale = if (scale > 1.2f) 1f else 2.5f
                                    if (scale == 1f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                },
                                onTap = {
                                    if (scale > 1.2f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = pageUrl,
                        contentDescription = "Full Preview $pageIndex",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp)
                            .graphicsLayer(
                                scaleX = if (pagerState.currentPage == pageIndex) animatedScale else 1f,
                                scaleY = if (pagerState.currentPage == pageIndex) animatedScale else 1f,
                                translationX = if (pagerState.currentPage == pageIndex) offsetX else 0f,
                                translationY = if (pagerState.currentPage == pageIndex) offsetY else 0f
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Top Navigation Header (Matching Screenshot 2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    SoundManager.playReaction()
                    onClose()
                },
                modifier = Modifier.size(40.dp)
            ) {
                PhosphorChevronLeft(tint = Color.White, size = 24.dp)
            }

            Spacer(Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = senderName ?: "Photo",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = if (count > 1) "$count photos • ${subtitle ?: "Yesterday"}" else (subtitle ?: "Photo"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            // Zoom Reset Indicator or Spacer
            if (scale > 1.2f) {
                Text(
                    text = "Reset Zoom",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF007AFF),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Top Centered Pagination Dots (Matching Screenshot 2)
        if (count > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(count) { idx ->
                    val isCurrent = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (isCurrent) 6.5.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (isCurrent) Color.White else Color.White.copy(alpha = 0.35f))
                    )
                }
            }
        }

        // Bottom Action Bar
        val currentPhotoUrl = effectiveImages.getOrElse(pagerState.currentPage) { imageUrl ?: "" }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Share
            IconButton(
                onClick = {
                    SoundManager.playReaction()
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, currentPhotoUrl)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Photo"))
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                LucideShare2(tint = Color.White, size = 20.dp)
            }

            // Save
            IconButton(
                onClick = {
                    SoundManager.playReaction()
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                LucideDownload(tint = Color.White, size = 20.dp)
            }

            // Link
            IconButton(
                onClick = {
                    SoundManager.playReaction()
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                LucideLink(tint = Color.White, size = 20.dp)
            }
        }
    }
}

// 6. Apple Floating Capsule Toast Notification
@Composable
fun ToastNotificationPill(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        if (message != null) {
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xE61C1C1E))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhosphorGreenCheck(size = 15.dp)
                Text(message, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
        }
    }
}

// 7. Authentic Apple Action Sheet: Delete Message Modal
@Composable
fun DeleteMessageModal(
    isOutgoing: Boolean,
    onDeleteForMe: () -> Unit,
    onDeleteForAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val actions = buildList {
        if (isOutgoing) {
            add(
                IosAlertAction(
                    title = "Delete for Everyone",
                    isDestructive = true,
                    onClick = onDeleteForAll
                )
            )
        }
        add(
            IosAlertAction(
                title = "Delete for Me",
                isDestructive = true,
                onClick = onDeleteForMe
            )
        )
    }

    IosActionSheet(
        title = "Delete Message?",
        message = "This message will be removed from your chat.",
        actions = actions,
        cancelAction = IosAlertAction("Cancel", isCancel = true, onClick = onDismiss),
        onDismiss = onDismiss
    )
}

// 8. Wallpaper Picker & Settings Modal (No Pill Shapes, 0.5dp borders)
@Composable
fun WallpaperPickerModal(
    currentWallpaper: WallpaperType,
    currentDimmingAlpha: Float,
    onSelectWallpaper: (WallpaperType, Float) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalChatColors.current
    var selectedWallpaper by remember { mutableStateOf(currentWallpaper) }
    var dimmingAlpha by remember { mutableFloatStateOf(currentDimmingAlpha) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "HD Wallpapers", "Vibrant Waves", "Geometric & Mesh", "Patterns", "Gradients", "Clean")
    val allWallpapers = WallpaperType.values().toList()
    val filteredWallpapers = if (selectedCategory == "All") allWallpapers else allWallpapers.filter { it.category == selectedCategory }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {}
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(colors.cardBg)
                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Drag Indicator Handle (Sharp rectangle, NO PILL SHAPE)
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.borderSubtle)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(12.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Chat Wallpaper",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textDark
                    )
                    Text(
                        text = "Select a background for your conversations",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (colors.isLight) Color(0xFFF1F5F9) else Color(0xFF334155))
                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorCloseX(tint = colors.textDark, size = 14.dp)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Category Filter Tabs (Sharp rectangles, NO PILL SHAPE)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) colors.iosBlue else if (colors.isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B))
                            .border(
                                width = 0.5.dp,
                                color = if (isSelected) colors.iosBlue else colors.borderSubtle,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else colors.textDark
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Live Preview Card with Mock Chat Bubble
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
            ) {
                // Background Wallpaper Live Render
                ChatWallpaperBackground(
                    wallpaperType = selectedWallpaper,
                    dimmingAlpha = dimmingAlpha,
                    modifier = Modifier.fillMaxSize()
                )

                // Mock Chat Bubbles on Top
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Incoming mock bubble
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp, 8.dp, 8.dp, 3.dp))
                            .background(colors.cardBg)
                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(8.dp, 8.dp, 8.dp, 3.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("How does the new wallpaper look? ✨", fontSize = 12.sp, color = colors.textDark, fontWeight = FontWeight.Medium)
                    }

                    // Outgoing mock bubble
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(8.dp, 8.dp, 3.dp, 8.dp))
                            .background(colors.iosBlue)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Crisp and luxury! 👌", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        MessageStatusTick(status = "read", isOutgoing = true, readTint = Color(0xFF38BDF8), size = 12.dp)
                    }
                }

                // Live Preview Badge (Top Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("LIVE PREVIEW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Dimming / Overlay Slider Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Dimming Overlay",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted
                )
                LiquidJellySlider(
                    value = dimmingAlpha,
                    onValueChange = { dimmingAlpha = it },
                    valueRange = 0f..0.6f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(dimmingAlpha * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textDark,
                    modifier = Modifier.width(32.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Wallpapers Grid (Sharp cards, NO PILL SHAPES)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredWallpapers) { wp ->
                    val isSelected = selectedWallpaper == wp

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (colors.isLight) Color(0xFFF8FAFC) else Color(0xFF1E293B))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) colors.iosBlue else colors.borderSubtle,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedWallpaper = wp }
                            .padding(6.dp)
                    ) {
                        // Wallpaper Thumbnail Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(74.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
                        ) {
                            if (wp.drawableRes != null) {
                                Image(
                                    painter = painterResource(id = wp.drawableRes),
                                    contentDescription = wp.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                ChatWallpaperBackground(
                                    wallpaperType = wp,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Selected Green Check Badge
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.iosBlue)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PhosphorGreenCheck(size = 12.dp)
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // Title Label
                        Text(
                            text = wp.title,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.iosBlue else colors.textDark,
                            maxLines = 1,
                            lineHeight = 13.sp
                        )
                        Text(
                            text = wp.category,
                            fontSize = 9.5.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons Row (Sharp geometric buttons, NO PILL SHAPE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cancel Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (colors.isLight) Color(0xFFF1F5F9) else Color(0xFF334155))
                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                        .clickable { onClose() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textDark
                    )
                }

                // Apply Button
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(8.dp),
                            ambientColor = Color(0x20000000),
                            spotColor = colors.iosBlue.copy(alpha = 0.35f)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.iosBlue)
                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .clickable {
                            onSelectWallpaper(selectedWallpaper, dimmingAlpha)
                            onClose()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LucideCheck(tint = Color.White, size = 15.dp)
                        Text(
                            text = "Set as Wallpaper",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// 9. Apple HIG Appearance & Customization Modal (20 Bubble Styles + 4 Corner Sliders + Pointy Notch + 7 Animations + Voice Globe + Colors)
@Composable
fun AppearanceSettingsModal(
    currentBubbleStyle: BubbleStyleType,
    currentGlobeStyle: VoiceGlobeStyleType,
    accentColor: AccentColorType,
    currentCustomCorners: CustomCornerConfig,
    onUpdateCustomCorners: (CustomCornerConfig) -> Unit,
    currentAnimationStyle: BubbleAnimationStyle,
    onSelectAnimationStyle: (BubbleAnimationStyle) -> Unit,
    onSelectBubbleStyle: (BubbleStyleType) -> Unit,
    onSelectGlobeStyle: (VoiceGlobeStyleType) -> Unit,
    onToggleAppearance: () -> Unit,
    onAccentCycle: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bubbles, 1: Corners & Notch, 2: Animations, 3: Voice Globe, 4: Theme
    var bubbleCategoryFilter by remember { mutableStateOf("All") }

    val bubbleCategories = listOf("All", "Modern UI", "Retro & Geek", "Playful & Art", "Bold & 3D")

    val filteredBubbleStyles = remember(bubbleCategoryFilter) {
        if (bubbleCategoryFilter == "All") BubbleStyleType.values().toList()
        else BubbleStyleType.values().filter { it.category.equals(bubbleCategoryFilter, ignoreCase = true) }
    }

    // Animation Test Key to trigger live replay
    var animTestKey by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { /* Consume */ }
                .shadow(16.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(colors.cardBg)
                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            // Drag Handle & Top Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.headerBg)
                    .border(0.5.dp, colors.borderSubtle)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.textMuted.copy(alpha = 0.4f))
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Appearance & Styles",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textDark
                        )
                        Text(
                            text = "Customize bubble corners, send animations, orb, and themes",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.bubbleGray)
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        LucideX(tint = colors.textMuted, size = 15.dp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Apple iOS Segmented Control Header
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (colors.isLight) Color(0x1F767680) else Color(0x3D767680))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val tabs = listOf(
                        "Bubbles (20)",
                        "Corners & Notch",
                        "Animations (7)",
                        "Voice Globe (8)",
                        "Theme & Colors"
                    )
                    items(tabs.size) { index ->
                        val isSelected = selectedTab == index
                        val tabItem = tabs[index]
                        val iconTint = if (isSelected) colors.textDark else colors.textMuted
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) (if (colors.isLight) Color.White else Color(0xFF636366)) else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                when (index) {
                                    0 -> LucideMessageSquare(tint = iconTint, size = 14.dp)
                                    1 -> LucideSliders(tint = iconTint, size = 14.dp)
                                    2 -> PhosphorSparkleIcon(tint = iconTint, size = 14.dp)
                                    3 -> LucideMic(tint = iconTint, size = 14.dp)
                                    else -> PhosphorSun(tint = iconTint, size = 14.dp)
                                }
                                Text(
                                    text = tabItem,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) colors.textDark else colors.textMuted
                                )
                            }
                        }
                    }
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    // TAB 0: 20 CHAT BUBBLE STYLES
                    0 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Category Filter Chips
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(bubbleCategories) { cat ->
                                    val isSelected = bubbleCategoryFilter.equals(cat, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) colors.iosBlue else (if (colors.isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B)))
                                            .border(
                                                0.5.dp,
                                                if (isSelected) colors.iosBlue else colors.borderSubtle,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { bubbleCategoryFilter = cat }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else colors.textMuted
                                        )
                                    }
                                }
                            }

                            // 20 Bubble Style Cards Grid
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
                            ) {
                                items(filteredBubbleStyles) { style ->
                                    val isSelected = style == currentBubbleStyle
                                    val outVisual = resolveBubbleVisual(style, isOutgoing = true, isConsecutive = false, colors = colors, customCorners = currentCustomCorners)
                                    val inVisual = resolveBubbleVisual(style, isOutgoing = false, isConsecutive = false, colors = colors, customCorners = currentCustomCorners)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) colors.iosBlueSubtle else colors.cardBg)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) colors.iosBlue else colors.borderSubtle,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onSelectBubbleStyle(style) }
                                            .padding(12.dp)
                                    ) {
                                        // Header Row: Title + Category + Active Check
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = style.title,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textDark
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(colors.borderSubtle)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = style.category,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = colors.textMuted
                                                    )
                                                }
                                            }

                                            if (isSelected) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(colors.iosBlue)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    LucideCheck(tint = Color.White, size = 12.dp)
                                                    Text(
                                                        text = "Active",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = style.description,
                                            fontSize = 12.sp,
                                            color = colors.textMuted,
                                            lineHeight = 16.sp
                                        )

                                        Spacer(Modifier.height(10.dp))

                                        // Mini Live Preview Sandbox for this Bubble Style
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (colors.isLight) Color(0xFFF8FAFC) else Color(0xFF0B0F17))
                                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Incoming Sample
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Start
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .widthIn(max = 240.dp)
                                                        .shadow(inVisual.elevation, inVisual.shape)
                                                        .clip(inVisual.shape)
                                                        .background(brush = inVisual.background)
                                                        .border(inVisual.borderWidth, inVisual.borderColor, inVisual.shape)
                                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                                ) {
                                                    Text(
                                                        text = style.incomingPreview,
                                                        fontSize = 12.sp,
                                                        color = inVisual.textColor,
                                                        fontFamily = inVisual.fontFamily,
                                                        fontWeight = inVisual.fontWeight
                                                    )
                                                }
                                            }

                                            // Outgoing Sample
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .widthIn(max = 240.dp)
                                                        .shadow(outVisual.elevation, outVisual.shape)
                                                        .clip(outVisual.shape)
                                                        .background(brush = outVisual.background)
                                                        .border(outVisual.borderWidth, outVisual.borderColor, outVisual.shape)
                                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                                ) {
                                                    Text(
                                                        text = style.outgoingPreview,
                                                        fontSize = 12.sp,
                                                        color = outVisual.textColor,
                                                        fontFamily = outVisual.fontFamily,
                                                        fontWeight = outVisual.fontWeight
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: CORNERS & NOTCH CUSTOMIZATION (4 CORNERS + POINTY CORNER STYLE)
                    1 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            // Section 1: Custom Corner Enable Switch
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.cardBg)
                                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Custom Corner Geometry",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textDark
                                            )
                                            Text(
                                                text = if (currentCustomCorners.customEnabled) "Overriding bubble shape with custom radii" else "Using default preset shape for current bubble style",
                                                fontSize = 12.sp,
                                                color = colors.textMuted
                                            )
                                        }
                                        LiquidGlassSwitch(
                                            checked = currentCustomCorners.customEnabled,
                                            onCheckedChange = { enabled ->
                                                onUpdateCustomCorners(currentCustomCorners.copy(customEnabled = enabled))
                                            }
                                        )
                                    }
                                }
                            }

                            // Section 2: Live Interactive Bubble Preview Card
                            item {
                                val outVisual = resolveBubbleVisual(currentBubbleStyle, isOutgoing = true, isConsecutive = false, colors = colors, customCorners = currentCustomCorners)
                                val inVisual = resolveBubbleVisual(currentBubbleStyle, isOutgoing = false, isConsecutive = false, colors = colors, customCorners = currentCustomCorners)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (colors.isLight) Color(0xFFF8FAFC) else Color(0xFF0F172A))
                                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "LIVE SHAPE PREVIEW",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMuted,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = currentBubbleStyle.title,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.iosBlue
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))

                                    // Incoming Sample
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                        Box(
                                            modifier = Modifier
                                                .widthIn(max = 240.dp)
                                                .shadow(inVisual.elevation, inVisual.shape)
                                                .clip(inVisual.shape)
                                                .background(brush = inVisual.background)
                                                .border(inVisual.borderWidth, inVisual.borderColor, inVisual.shape)
                                                .padding(horizontal = 11.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = "Incoming message bubble",
                                                fontSize = 13.sp,
                                                color = inVisual.textColor,
                                                fontFamily = inVisual.fontFamily,
                                                fontWeight = inVisual.fontWeight
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))

                                    // Outgoing Sample
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Box(
                                            modifier = Modifier
                                                .widthIn(max = 240.dp)
                                                .shadow(outVisual.elevation, outVisual.shape)
                                                .clip(outVisual.shape)
                                                .background(brush = outVisual.background)
                                                .border(outVisual.borderWidth, outVisual.borderColor, outVisual.shape)
                                                .padding(horizontal = 11.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = "Outgoing custom radii bubble 🚀",
                                                fontSize = 13.sp,
                                                color = outVisual.textColor,
                                                fontFamily = outVisual.fontFamily,
                                                fontWeight = outVisual.fontWeight
                                            )
                                        }
                                    }
                                }
                            }

                            // Section 3: 4 Individual Corner Sliders
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.cardBg)
                                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "INDIVIDUAL 4-CORNER RADII",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMuted,
                                        letterSpacing = 0.5.sp
                                    )

                                    // Top-Start (Top-Left)
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Top-Left Radius", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textDark)
                                            Text("${currentCustomCorners.topStart.toInt()} dp", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.iosBlue)
                                        }
                                        LiquidJellySlider(
                                            value = currentCustomCorners.topStart,
                                            onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(topStart = it, customEnabled = true)) },
                                            valueRange = 0f..24f,
                                            steps = 23,
                                            
                                        )
                                    }

                                    // Top-End (Top-Right)
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Top-Right Radius", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textDark)
                                            Text("${currentCustomCorners.topEnd.toInt()} dp", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.iosBlue)
                                        }
                                        LiquidJellySlider(
                                            value = currentCustomCorners.topEnd,
                                            onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(topEnd = it, customEnabled = true)) },
                                            valueRange = 0f..24f,
                                            steps = 23,
                                            
                                        )
                                    }

                                    // Bottom-Start (Bottom-Left)
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Bottom-Left Radius", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textDark)
                                            Text("${currentCustomCorners.bottomStart.toInt()} dp", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.iosBlue)
                                        }
                                        LiquidJellySlider(
                                            value = currentCustomCorners.bottomStart,
                                            onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(bottomStart = it, customEnabled = true)) },
                                            valueRange = 0f..24f,
                                            steps = 23,
                                            
                                        )
                                    }

                                    // Bottom-End (Bottom-Right)
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Bottom-Right Radius", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textDark)
                                            Text("${currentCustomCorners.bottomEnd.toInt()} dp", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.iosBlue)
                                        }
                                        LiquidJellySlider(
                                            value = currentCustomCorners.bottomEnd,
                                            onValueChange = { onUpdateCustomCorners(currentCustomCorners.copy(bottomEnd = it, customEnabled = true)) },
                                            valueRange = 0f..24f,
                                            steps = 23,
                                            
                                        )
                                    }
                                }
                            }

                            // Section 4: Pointy Corner Notch Style
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.cardBg)
                                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "POINTY CORNER NOTCH STYLE",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMuted,
                                        letterSpacing = 0.5.sp
                                    )

                                    PointyCornerStyle.values().forEach { pStyle ->
                                        val isSelected = currentCustomCorners.pointyCornerStyle == pStyle
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) colors.iosBlueSubtle else Color.Transparent)
                                                .border(
                                                    width = if (isSelected) 1.dp else 0.5.dp,
                                                    color = if (isSelected) colors.iosBlue else colors.borderSubtle,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    onUpdateCustomCorners(currentCustomCorners.copy(pointyCornerStyle = pStyle, customEnabled = true))
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = pStyle.title,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = colors.textDark
                                                )
                                                Text(
                                                    text = pStyle.description,
                                                    fontSize = 11.sp,
                                                    color = colors.textMuted
                                                )
                                            }
                                            if (isSelected) {
                                                LucideCheck(tint = colors.iosBlue, size = 16.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: ANIMATION STYLES (7 STYLES WITH LIVE INTERACTIVE REPLAY)
                    2 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            item {
                                Text(
                                    text = "Chat Message Send Animation",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textDark
                                )
                                Text(
                                    text = "Choose your favorite physics entrance transition when sending or receiving messages",
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                            }

                            items(BubbleAnimationStyle.values()) { animStyle ->
                                val isSelected = animStyle == currentAnimationStyle

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) colors.iosBlueSubtle else colors.cardBg)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) colors.iosBlue else colors.borderSubtle,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onSelectAnimationStyle(animStyle) }
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = animStyle.title,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textDark
                                                )
                                                if (animStyle == BubbleAnimationStyle.POP_ZOOM) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(colors.iosBlue)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Default", fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = animStyle.description,
                                                fontSize = 11.5.sp,
                                                color = colors.textMuted
                                            )
                                        }

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.iosBlue),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                LucideCheck(tint = Color.White, size = 14.dp)
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // Mini Live Sandbox Box to test and replay this animation
                                    var triggerReplay by remember { mutableIntStateOf(0) }
                                    val replayScale = remember { Animatable(1f) }
                                    val replayAlpha = remember { Animatable(1f) }
                                    val replayOffsetY = remember { Animatable(0f) }
                                    val replayOffsetX = remember { Animatable(0f) }
                                    val replayRotationX = remember { Animatable(0f) }

                                    LaunchedEffect(triggerReplay, animTestKey) {
                                        when (animStyle) {
                                            BubbleAnimationStyle.POP_ZOOM -> {
                                                replayScale.snapTo(0.45f)
                                                replayAlpha.snapTo(0.4f)
                                                replayScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow))
                                                replayAlpha.animateTo(1f, animationSpec = tween(150))
                                            }
                                            BubbleAnimationStyle.SLIDE_UP_FADE -> {
                                                replayOffsetY.snapTo(35f)
                                                replayAlpha.snapTo(0f)
                                                replayOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow))
                                                replayAlpha.animateTo(1f, animationSpec = tween(200))
                                            }
                                            BubbleAnimationStyle.ELASTIC_SPRING -> {
                                                replayScale.snapTo(0.2f)
                                                replayAlpha.snapTo(0f)
                                                replayScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessLow))
                                                replayAlpha.animateTo(1f, animationSpec = tween(120))
                                            }
                                            BubbleAnimationStyle.FLIP_IN_3D -> {
                                                replayRotationX.snapTo(35f)
                                                replayAlpha.snapTo(0f)
                                                replayRotationX.animateTo(0f, animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
                                                replayAlpha.animateTo(1f, animationSpec = tween(220))
                                            }
                                            BubbleAnimationStyle.SLIDE_IN_SIDE -> {
                                                replayOffsetX.snapTo(70f)
                                                replayAlpha.snapTo(0f)
                                                replayOffsetX.animateTo(0f, animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
                                                replayAlpha.animateTo(1f, animationSpec = tween(180))
                                            }
                                            BubbleAnimationStyle.EXPAND_REVEAL -> {
                                                replayScale.snapTo(0.7f)
                                                replayOffsetY.snapTo(15f)
                                                replayAlpha.snapTo(0f)
                                                replayScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium))
                                                replayOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium))
                                                replayAlpha.animateTo(1f, animationSpec = tween(160))
                                            }
                                            BubbleAnimationStyle.GENTLE_FADE -> {
                                                replayAlpha.snapTo(0f)
                                                replayAlpha.animateTo(1f, animationSpec = tween(250, easing = LinearOutSlowInEasing))
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (colors.isLight) Color(0xFFF1F5F9) else Color(0xFF0F172A))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    scaleX = replayScale.value
                                                    scaleY = replayScale.value
                                                    alpha = replayAlpha.value
                                                    translationX = replayOffsetX.value
                                                    translationY = replayOffsetY.value
                                                    rotationX = replayRotationX.value
                                                    transformOrigin = TransformOrigin(0.9f, 0.5f)
                                                }
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.iosBlue)
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Pop-up animation preview ✨",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.cardBg)
                                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
                                                .clickable { triggerReplay++ }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Replay 🔁", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.iosBlue)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 3: 8 VOICE GLOBE THEMES WITH LIVE INTERACTIVE PREVIEW
                    3 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Section Header
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "3D Interactive Voice Globe Styles",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textDark
                                    )
                                    Text(
                                        text = "Custom liquid orb shaders & dynamic audio spectrum for Dynamic Island & Voice Notes",
                                        fontSize = 12.sp,
                                        color = colors.textMuted
                                    )
                                }
                            }

                            // Live Interactive Hero Preview Card
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF000000))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                                        .padding(vertical = 20.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0x22FFFFFF))
                                                .border(0.5.dp, Color(0x44FFFFFF), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(currentGlobeStyle.previewGradient.firstOrNull() ?: Color(0xFF007AFF))
                                                )
                                                Text(
                                                    text = "ACTIVE PREVIEW • ${currentGlobeStyle.title.uppercase()}",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        // 3D Liquid AI Globe Live Component
                                        DynamicIslandLiquidGlobe(
                                            globeStyle = currentGlobeStyle,
                                            size = 120.dp
                                        )

                                        Text(
                                            text = currentGlobeStyle.description,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // All 8 Theme Options
                            items(VoiceGlobeStyleType.values()) { globeStyle ->
                                val isSelected = globeStyle == currentGlobeStyle

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) (if (colors.isLight) Color(0x15007AFF) else Color(0x250A84FF)) else colors.cardBg)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) colors.iosBlue else colors.borderSubtle,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { 
                                            SoundManager.playReaction()
                                            onSelectGlobeStyle(globeStyle) 
                                        }
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Orb Swatch Icon
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(globeStyle.previewGradient)
                                                    )
                                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                PhosphorWaveform(tint = Color.White, size = 18.dp)
                                            }

                                            Column {
                                                Text(
                                                    text = globeStyle.title,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textDark
                                                )
                                                Text(
                                                    text = globeStyle.description,
                                                    fontSize = 11.5.sp,
                                                    color = colors.textMuted
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.iosBlue),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                LucideCheck(tint = Color.White, size = 14.dp)
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // Visual Palette Bar Preview
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF090D16))
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            globeStyle.previewGradient.forEach { c ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(c)
                                                )
                                            }
                                        }
                                        Text(
                                            text = when (globeStyle) {
                                                VoiceGlobeStyleType.ORIGINAL_BLUE -> "Liquid Orb • Cyan Frequency EQ"
                                                VoiceGlobeStyleType.CHROMATIC_3D -> "Cosmic Nebula • Rotating Plasma Orb"
                                                VoiceGlobeStyleType.AURORA_PULSE -> "Neon Aurora • Breathing Ultraviolet Glow"
                                                VoiceGlobeStyleType.CYBER_GRID -> "Cyber Matrix • Laser Waveform Sphere"
                                                VoiceGlobeStyleType.SOLAR_FLARE -> "Solar Flare • Molten Golden Sun"
                                                VoiceGlobeStyleType.DARK_MATTER -> "Obsidian Core • Quantum Indigo Pulse"
                                                VoiceGlobeStyleType.ROSE_QUARTZ -> "Sakura Quartz • Ethereal Blossom Glow"
                                                VoiceGlobeStyleType.ELECTRIC_CYAN -> "Aquamarine Prism • Arctic Cyan Wave"
                                            },
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TAB 4: THEME & ACCENTS
                    4 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section 1: Color Scheme Mode (Light / Dark)
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.cardBg)
                                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "COLOR SCHEME",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMuted,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Light Mode Card
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (colors.isLight) colors.iosBlueSubtle else Color(0xFFF8FAFC))
                                                .border(
                                                    width = if (colors.isLight) 1.5.dp else 0.5.dp,
                                                    color = if (colors.isLight) colors.iosBlue else Color(0xFFE2E8F0),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { if (!colors.isLight) onToggleAppearance() }
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            PhosphorSun(tint = if (colors.isLight) colors.iosBlue else Color(0xFF64748B), size = 24.dp)
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = "Light Mode",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (colors.isLight) colors.iosBlue else Color(0xFF334155)
                                            )
                                        }

                                        // Dark Mode Card
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (!colors.isLight) colors.iosBlueSubtle else Color(0xFF0F172A))
                                                .border(
                                                    width = if (!colors.isLight) 1.5.dp else 0.5.dp,
                                                    color = if (!colors.isLight) colors.iosBlue else Color(0xFF334155),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { if (colors.isLight) onToggleAppearance() }
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            PhosphorMoon(tint = if (!colors.isLight) colors.iosBlue else Color(0xFF94A3B8), size = 24.dp)
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = "Dark Mode",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!colors.isLight) colors.iosBlue else Color(0xFFF1F5F9)
                                            )
                                        }
                                    }
                                }
                            }

                            // Section 2: Accent Color
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.cardBg)
                                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ACCENT COLOR",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMuted,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Tap to cycle",
                                            fontSize = 11.5.sp,
                                            color = colors.iosBlue,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onAccentCycle() },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val palette = listOf(
                                            Color(0xFF0078FE), // iOS Blue
                                            Color(0xFF8B5CF6), // Purple
                                            Color(0xFF10B981), // Emerald
                                            Color(0xFFF59E0B), // Amber
                                            Color(0xFFF43F5E), // Rose
                                            Color(0xFF06B6D4)  // Cyan
                                        )
                                        palette.forEach { c ->
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(c)
                                                    .border(
                                                        width = if (c == colors.iosBlue) 2.5.dp else 0.5.dp,
                                                        color = if (c == colors.iosBlue) (if (colors.isLight) Color.Black else Color.White) else Color.White.copy(0.3f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (c == colors.iosBlue) {
                                                    LucideCheck(tint = Color.White, size = 16.dp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Section 3: Wallpaper Launcher Shortcut
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.cardBg)
                                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                                        .clickable {
                                            onClose()
                                            onOpenWallpaperPicker()
                                        }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colors.iosBlueSubtle),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LucideWallpaper(tint = colors.iosBlue, size = 18.dp)
                                        }
                                        Column {
                                            Text(
                                                text = "Chat Wallpaper Gallery",
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textDark
                                            )
                                            Text(
                                                text = "12 HD Apple & Aesthetic Wallpapers with dimming",
                                                fontSize = 11.5.sp,
                                                color = colors.textMuted
                                            )
                                        }
                                    }
                                    LucideChevronRight(tint = colors.textMuted, size = 18.dp)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Done Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.headerBg)
                    .border(0.5.dp, colors.borderSubtle)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.iosBlue)
                        .clickable { onClose() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Apply & Done",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Apple iOS Full Emoji Reaction Picker Modal.
 * Categorized emoji grids, real-time search, and smooth spring feedback.
 */
@Composable
fun EmojiReactionPickerModal(
    onDismiss: () -> Unit,
    onSelectEmoji: (String) -> Unit
) {
    val colors = LocalChatColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Popular", "Smileys", "Gestures", "Hearts", "Party", "Objects")

    val popularEmojis = listOf("❤️", "👍", "👎", "🔥", "😂", "😍", "🎉", "🙏", "👏", "💯", "✨", "🚀", "🥰", "🥳", "🤩", "🙌")
    val smileyEmojis = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃", "😉", "😊",
        "😇", "🥰", "😍", "🤩", "😘", "😗", "😚", "😙", "😋", "😛", "😜", "🤪",
        "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐", "🤨", "😐", "😑", "😶", "😏",
        "😒", "🙄", "😬", "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕",
        "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "😎", "🤓",
        "🧐", "😕", "😟", "🙁", "😮", "😯", "😲", "😳", "🥺", "😦", "😧", "😨",
        "😰", "😥", "😢", "😭", "😱", "😖", "😣", "😞", "😓", "😩", "😫", "🥱"
    )
    val gestureEmojis = listOf(
        "👍", "👎", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️", "💪", "👊", "✊",
        "🤛", "🤜", "🤞", "✌️", "🤟", "🤘", "👌", "🤌", "🤏", "👈", "👉", "👆",
        "👇", "☝️", "👋", "🤚", "🖐️", "✋", "🖖", "🤙", "🤝", "💅", "🤳", "💃"
    )
    val heartEmojis = listOf(
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕",
        "💞", "💓", "💗", "💖", "💘", "💝", "💟", "🔥", "✨", "⭐", "🌟", "💫",
        "⚡", "💥", "💯", "💢", "💬", "🗨️", "🗯️", "💌", "💐", "🌹", "🌸", "🌺"
    )
    val partyEmojis = listOf(
        "🎉", "🎊", "🎈", "🎁", "🏆", "🥇", "🥈", "🥉", "👑", "💎", "🎯", "🚀",
        "🍿", "🥂", "🍻", "🍾", "🎂", "🍰", "🧁", "🍩", "🍪", "🍕", "🍔", "🍟"
    )
    val objectEmojis = listOf(
        "💡", "📌", "📍", "📎", "🔑", "🔒", "🔓", "📱", "💻", "⌚", "📷", "📸",
        "🎥", "🎙️", "🎧", "📻", "🎵", "🎶", "🔔", "📣", "📢", "📬", "📮", "📦"
    )

    val displayedEmojis = remember(selectedCategory, searchQuery) {
        val baseList = when (selectedCategory) {
            "Popular" -> popularEmojis
            "Smileys" -> smileyEmojis
            "Gestures" -> gestureEmojis
            "Hearts" -> heartEmojis
            "Party" -> partyEmojis
            "Objects" -> objectEmojis
            else -> popularEmojis + smileyEmojis + gestureEmojis + heartEmojis + partyEmojis + objectEmojis
        }.distinct()

        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter { emoji ->
                emoji.contains(q) || when (emoji) {
                    "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖" -> "heart love red".contains(q)
                    "👍" -> "thumbs up like good yes".contains(q)
                    "👎" -> "thumbs down dislike bad no".contains(q)
                    "🔥" -> "fire hot lit flame".contains(q)
                    "😂", "🤣" -> "laugh funny lol haha cry".contains(q)
                    "😍", "🥰" -> "love heart eyes smile happy".contains(q)
                    "🎉", "🎊" -> "party celebrate celebration tada".contains(q)
                    "🙏" -> "pray thank please praise hope".contains(q)
                    "👏" -> "clap applause bravo".contains(q)
                    "💯" -> "hundred perfect score 100".contains(q)
                    "✨", "⭐", "🌟" -> "sparkle star shine glow".contains(q)
                    "🚀" -> "rocket space moon launch".contains(q)
                    "💡" -> "idea bulb light lamp".contains(q)
                    "📌", "📍" -> "pin note location point".contains(q)
                    else -> true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { /* Consume taps */ }
                .shadow(16.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(colors.cardBg)
                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textMuted.copy(alpha = 0.35f))
                    .align(Alignment.CenterHorizontally)
            )

            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "React with Emoji",
                    fontSize = 17.sp,
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
                    LucideX(tint = colors.textMuted, size = 15.dp)
                }
            }

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (colors.isLight) Color(0xFFF2F2F7) else Color(0xFF2C2C2E))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LucideSearch(tint = colors.textMuted, size = 15.dp)
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            fontSize = 14.5.sp,
                            color = colors.textDark,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(colors.iosBlue),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search emoji...",
                                    fontSize = 14.5.sp,
                                    color = colors.textMuted
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(colors.textMuted.copy(alpha = 0.3f))
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            LucideX(tint = colors.textDark, size = 10.dp)
                        }
                    }
                }
            }

            // Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.iosBlue else (if (colors.isLight) Color(0xFFF2F2F7) else Color(0xFF2C2C2E)))
                            .border(0.5.dp, if (isSelected) colors.iosBlue else colors.borderSubtle, RoundedCornerShape(8.dp))
                            .clickable {
                                SoundManager.playPop()
                                selectedCategory = cat
                            }
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else colors.textMuted
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

            // Emoji Grid
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(displayedEmojis.size) { idx ->
                    val emoji = displayedEmojis[idx]
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .clickable {
                                onSelectEmoji(emoji)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 25.sp
                        )
                    }
                }
            }
        }
    }
}













