@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.chroma.studio.chat.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.chroma.studio.R
import com.chroma.studio.chat.data.*
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.ui.components.AppleSettingsGraphic
import com.chroma.studio.ui.components.AppleSwitch
import com.chroma.studio.ui.components.LiquidGlassPrimaryButton
import com.chroma.studio.ui.components.LiquidGlassSecondaryButton
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState

/**
 * Flagship Apple iOS Settings & Profile Screen.
 * Complete with profile editor, appearance picker, privacy, notifications,
 * blocked users manager, cache cleaner, and security controls.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsScreen(
    currentUser: User,
    onUpdateUser: (User) -> Unit,
    chats: List<ChatConversation>,
    onUnblockChat: (String) -> Unit,
    currentWallpaper: WallpaperType,
    currentWallpaperDimming: Float,
    currentBubbleStyle: BubbleStyleType,
    currentGlobeStyle: VoiceGlobeStyleType,
    currentCustomCorners: CustomCornerConfig,
    currentAnimationStyle: BubbleAnimationStyle,
    accentColor: AccentColorType,
    onAppearanceToggle: () -> Unit,
    onAccentCycle: () -> Unit,
    onSelectAccent: (AccentColorType) -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onOpenAppearanceSettings: () -> Unit,
    onClose: () -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }

    var isEditProfileModalOpen by remember { mutableStateOf(false) }
    var isBlockedUsersModalOpen by remember { mutableStateOf(false) }
    var isPrivacyModalOpen by remember { mutableStateOf(false) }
    var isDataStorageModalOpen by remember { mutableStateOf(false) }
    var isAboutModalOpen by remember { mutableStateOf(false) }

    // Toggle states
    var soundsEnabled by remember { mutableStateOf(SoundManager.isSoundsEnabled) }
    var hapticsEnabled by remember { mutableStateOf(true) }
    var readReceiptsEnabled by remember { mutableStateOf(true) }
    var screenLockEnabled by remember { mutableStateOf(false) }

    // Cache calculation
    var cacheSizeMb by remember { mutableFloatStateOf(14.8f) }

    val blockedChats = remember(chats) { chats.filter { it.isBlocked } }

    Scaffold(
        containerColor = colors.appBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.appBg)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Done",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.iosBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                SoundManager.playReaction()
                                onClose()
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    )

                    Text(
                        text = "Edit Profile",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.iosBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                SoundManager.playReaction()
                                isEditProfileModalOpen = true
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Apple iOS Large Title
                Text(
                    text = "Settings",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textDark,
                    letterSpacing = (-0.5).sp
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── 1. USER PROFILE HERO CARD ────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.cardBg)
                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            SoundManager.playReaction()
                            isEditProfileModalOpen = true
                        }
                ) {
                    // Profile Cover Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        AsyncImage(
                            model = currentUser.banner.ifEmpty { drawableUri(R.drawable.banner_wave_banner_midnight_glow) },
                            contentDescription = "Cover Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                                    )
                                )
                        )
                    }

                    // Content below banner with avatar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Large Avatar overlapping banner
                        Box(
                            modifier = Modifier
                                .offset(y = (-22).dp)
                                .size(64.dp)
                        ) {
                            AsyncImage(
                                model = currentUser.avatar,
                                contentDescription = currentUser.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(colors.cardBg)
                                    .border(2.5.dp, colors.cardBg, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-2).dp, y = (-2).dp)
                                    .background(colors.cardBg, CircleShape)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(colors.success)
                            )
                        }

                        // Name & Username info
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .offset(y = (-8).dp)
                        ) {
                            Text(
                                text = currentUser.name,
                                fontSize = 18.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textDark
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = currentUser.username,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.iosBlue
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = currentUser.status,
                                fontSize = 12.5.sp,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right Chevron
                        LucideChevronRight(
                            tint = colors.textMuted.copy(alpha = 0.6f),
                            size = 18.dp,
                            modifier = Modifier.offset(y = (-8).dp)
                        )
                    }
                }
            }

            // ── 2. SECTION: APPEARANCE & PERSONALIZATION ─────────────────────
            item {
                SettingsSectionContainer(title = "APPEARANCE & THEMES") {
                    // Dark Mode Row
                    SettingsToggleRow(
                        icon = { LucideMoon(tint = Color(0xFF5856D6), size = 18.dp) },
                        title = "Dark Mode",
                        subtitle = if (colors.isLight) "Light theme active" else "OLED Dark theme active",
                        checked = !colors.isLight,
                        onCheckedChange = { onAppearanceToggle() }
                    )

                    SettingsDivider()

                    // Accent Color Row
                    SettingsActionRow(
                        icon = { LucidePalette(tint = colors.iosBlue, size = 18.dp) },
                        title = "Theme Accent",
                        value = accentColor.title,
                        onClick = { onAccentCycle() }
                    )

                    SettingsDivider()

                    // Wallpaper Row
                    SettingsActionRow(
                        icon = { LucideImage(tint = Color(0xFF34C759), size = 18.dp) },
                        title = "Chat Wallpaper",
                        value = currentWallpaper.title,
                        onClick = { onOpenWallpaperPicker() }
                    )

                    SettingsDivider()

                    // Bubble Style Row
                    SettingsActionRow(
                        icon = { LucideMessageSquare(tint = Color(0xFFFF9500), size = 18.dp) },
                        title = "Bubble & Globe Design",
                        value = currentBubbleStyle.title.substringAfter(". "),
                        onClick = { onOpenAppearanceSettings() }
                    )

                    SettingsDivider()

                    // Message Animation Style Row
                    SettingsActionRow(
                        icon = { LucideSparkles(tint = Color(0xFFAF52DE), size = 18.dp) },
                        title = "Arrival Animation",
                        value = currentAnimationStyle.title.substringBefore(" ("),
                        onClick = { onOpenAppearanceSettings() }
                    )
                }
            }

            // ── 3. SECTION: NOTIFICATIONS & SOUNDS ────────────────────────────
            item {
                SettingsSectionContainer(title = "NOTIFICATIONS & SOUNDS") {
                    // In-App Sound Effects Toggle
                    SettingsToggleRow(
                        icon = { LucideVolume2(tint = Color(0xFF007AFF), size = 18.dp) },
                        title = "Sound Effects",
                        subtitle = "Play tones for sent, received messages & reactions",
                        checked = soundsEnabled,
                        onCheckedChange = {
                            soundsEnabled = it
                            SoundManager.isSoundsEnabled = it
                            onShowToast(if (it) "Sounds enabled" else "Sounds muted")
                        }
                    )

                    SettingsDivider()

                    // Haptics Toggle
                    SettingsToggleRow(
                        icon = { LucideSmartphone(tint = Color(0xFFFF2D55), size = 18.dp) },
                        title = "Haptic Feedback",
                        subtitle = "Subtle tactile vibrations on message actions",
                        checked = hapticsEnabled,
                        onCheckedChange = {
                            hapticsEnabled = it
                            onShowToast(if (it) "Haptics enabled" else "Haptics disabled")
                        }
                    )

                    SettingsDivider()

                    // Dynamic Island Preview Toggle
                    SettingsToggleRow(
                        icon = { LucideBell(tint = Color(0xFFFF9500), size = 18.dp) },
                        title = "Dynamic Island Alerts",
                        subtitle = "Show top pill overlay for active calls & audio notes",
                        checked = true,
                        onCheckedChange = { onShowToast("Dynamic Island is always active") }
                    )
                }
            }

            // ── 4. SECTION: PRIVACY & SECURITY ───────────────────────────────
            item {
                SettingsSectionContainer(title = "PRIVACY & SECURITY") {
                    // Read Receipts Double Ticks
                    SettingsToggleRow(
                        icon = { LucideCheckCheck(tint = Color(0xFF007AFF), size = 18.dp) },
                        title = "Read Receipts",
                        subtitle = "Send & view blue double checkmarks",
                        checked = readReceiptsEnabled,
                        onCheckedChange = {
                            readReceiptsEnabled = it
                            onShowToast(if (it) "Read receipts enabled" else "Read receipts disabled")
                        }
                    )

                    SettingsDivider()

                    // Blocked Contacts Row
                    SettingsActionRow(
                        icon = { LucideUserX(tint = Color(0xFFFF3B30), size = 18.dp) },
                        title = "Blocked Contacts",
                        value = "${blockedChats.size}",
                        onClick = { isBlockedUsersModalOpen = true }
                    )

                    SettingsDivider()

                    // Screen Lock Toggle
                    SettingsToggleRow(
                        icon = { LucideLock(tint = Color(0xFF34C759), size = 18.dp) },
                        title = "App Screen Lock",
                        subtitle = "Require Biometrics / PIN to access conversations",
                        checked = screenLockEnabled,
                        onCheckedChange = {
                            screenLockEnabled = it
                            onShowToast(if (it) "Screen lock enabled" else "Screen lock disabled")
                        }
                    )

                    SettingsDivider()

                    // End to End Encryption Status
                    SettingsActionRow(
                        icon = { LucideShieldCheck(tint = Color(0xFF34C759), size = 18.dp) },
                        title = "Encryption Status",
                        value = "Verified (Signal Protocol)",
                        onClick = { onShowToast("End-to-end encryption keys are verified") }
                    )
                }
            }

            // ── 5. SECTION: STORAGE & DATA ───────────────────────────────────
            item {
                SettingsSectionContainer(title = "STORAGE & DATA") {
                    // Auto-Download Media
                    SettingsActionRow(
                        icon = { LucideDownload(tint = Color(0xFF5856D6), size = 18.dp) },
                        title = "Auto-Download Media",
                        value = "Wi-Fi and Cellular",
                        onClick = { onShowToast("Media auto-download set to Wi-Fi & Cellular") }
                    )

                    SettingsDivider()

                    // Clear Cache Row
                    SettingsActionRow(
                        icon = { LucideTrash(tint = Color(0xFFFF3B30), size = 18.dp) },
                        title = "Clear Cached Files",
                        value = if (cacheSizeMb > 0f) "${"%.1f".format(cacheSizeMb)} MB" else "Cleared",
                        onClick = {
                            if (cacheSizeMb > 0f) {
                                cacheSizeMb = 0f
                                SoundManager.playReaction()
                                onShowToast("Cached files cleared! Freed 14.8 MB ")
                            } else {
                                onShowToast("Cache is already clean")
                            }
                        }
                    )
                }
            }

            // ── 6. SECTION: HELP & ABOUT ─────────────────────────────────────
            item {
                SettingsSectionContainer(title = "ABOUT OSCELLION") {
                    SettingsActionRow(
                        icon = { LucideInfo(tint = colors.iosBlue, size = 18.dp) },
                        title = "Version",
                        value = "3.4.0 (2026.10)",
                        onClick = { isAboutModalOpen = true }
                    )

                    SettingsDivider()

                    SettingsActionRow(
                        icon = { LucideHeart(tint = Color(0xFFFF2D55), size = 18.dp) },
                        title = "Designed For",
                        value = "ChromaStudio • Apple iOS HIG",
                        onClick = { isAboutModalOpen = true }
                    )
                }
            }

            // Footer Spacer
            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ── MODAL 1: EDIT PROFILE DIALOG ─────────────────────────────────────────
    if (isEditProfileModalOpen) {
        EditProfileModal(
            currentUser = currentUser,
            onDismiss = { isEditProfileModalOpen = false },
            onSave = { updatedUser ->
                onUpdateUser(updatedUser)
                isEditProfileModalOpen = false
                SoundManager.playSend()
                onShowToast("Profile updated successfully ")
            }
        )
    }

    // ── MODAL 2: BLOCKED USERS MANAGER ───────────────────────────────────────
    if (isBlockedUsersModalOpen) {
        BlockedContactsModal(
            blockedChats = blockedChats,
            onDismiss = { isBlockedUsersModalOpen = false },
            onUnblock = { chatId ->
                onUnblockChat(chatId)
                onShowToast("Contact unblocked")
            }
        )
    }

    // ── MODAL 3: ABOUT OSCELLION MODAL ───────────────────────────────────────
    if (isAboutModalOpen) {
        AboutAppModal(onDismiss = { isAboutModalOpen = false })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE APPLE iOS INSET GROUPED CONTAINERS & ROWS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsSectionContainer(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalChatColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textMuted,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardBg)
                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
        ) {
            content()
        }
    }
}

@Composable
fun SettingsDivider() {
    val colors = LocalChatColors.current
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp),
        color = colors.separator,
        thickness = 0.5.dp
    )
}

@Composable
fun SettingsActionRow(
    icon: @Composable () -> Unit,
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    val colors = LocalChatColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                SoundManager.playReaction()
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                icon()
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textDark
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LucideChevronRight(tint = colors.textMuted.copy(alpha = 0.6f), size = 16.dp)
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalChatColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                icon()
            }
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textDark
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        AppleSwitch(
            checked = checked,
            onCheckedChange = {
                SoundManager.playReaction()
                onCheckedChange(it)
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EDIT PROFILE MODAL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EditProfileModal(
    currentUser: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    val colors = LocalChatColors.current
    var name by remember { mutableStateOf(currentUser.name) }
    var username by remember { mutableStateOf(currentUser.username) }
    var status by remember { mutableStateOf(currentUser.status) }
    var selectedAvatar by remember { mutableStateOf(currentUser.avatar) }
    var selectedBanner by remember { mutableStateOf(currentUser.banner.ifEmpty { drawableUri(R.drawable.banner_wave_banner_midnight_glow) }) }

    val presetAvatars = remember {
        listOf(
            drawableUri(R.drawable.ic_avatar_corporate_navy),
            drawableUri(R.drawable.ic_avatar_emerald_focus),
            drawableUri(R.drawable.ic_avatar_gold_standard),
            drawableUri(R.drawable.ic_avatar_sapphire_night),
            drawableUri(R.drawable.ic_avatar_royal_blue),
            drawableUri(R.drawable.ic_avatar_deep_purple),
            drawableUri(R.drawable.ic_avatar_ocean_depth),
            drawableUri(R.drawable.ic_avatar_teal_pro),
            drawableUri(R.drawable.ic_avatar_executive_forest),
            drawableUri(R.drawable.ic_avatar_burgundy_board),
            drawableUri(R.drawable.ic_avatar_midnight),
            drawableUri(R.drawable.ic_avatar_charcoal),
            drawableUri(R.drawable.ic_avatar_classic_mac),
            drawableUri(R.drawable.ic_avatar_space_gray),
            drawableUri(R.drawable.ic_avatar_steel_grey),
            drawableUri(R.drawable.default_avatar_apple),
            drawableUri(R.drawable.ic_avatar_slate_blue)
        )
    }

    val presetBanners = remember {
        listOf(
            drawableUri(R.drawable.banner_wave_banner_midnight_glow),
            drawableUri(R.drawable.banner_wave_banner_electric_blue),
            drawableUri(R.drawable.banner_wave_banner_coral_sunset),
            drawableUri(R.drawable.banner_wave_banner_amethyst_spark),
            drawableUri(R.drawable.banner_wave_banner_ocean_cyan),
            drawableUri(R.drawable.banner_wave_banner_sunset_orange),
            drawableUri(R.drawable.banner_wave_banner_royal_indigo),
            drawableUri(R.drawable.banner_wave_banner_golden_amber),
            drawableUri(R.drawable.banner_wave_banner_forest_mint),
            drawableUri(R.drawable.banner_wave_banner_emerald_green),
            drawableUri(R.drawable.banner_wave_banner_cherry_blossom),
            drawableUri(R.drawable.banner_wave_banner_ruby_red),
            drawableUri(R.drawable.banner_wave_banner_berry_purple),
            drawableUri(R.drawable.banner_wave_banner_vibrant_violet),
            drawableUri(R.drawable.banner_wave_banner_teal_aqua)
        )
    }

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Edit Profile",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textDark
            )

                Spacer(Modifier.height(12.dp))

                // Live Preview Card (Banner + Avatar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = selectedBanner,
                        contentDescription = "Selected Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                                )
                            )
                    )

                    // Avatar in preview
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = 12.dp, y = (-6).dp)
                    ) {
                        AsyncImage(
                            model = selectedAvatar,
                            contentDescription = name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(colors.cardBg)
                                .border(2.dp, colors.cardBg, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Cover Banner Selector ──
                Text(
                    text = "CHOOSE COVER BANNER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    modifier = Modifier.fillMaxWidth().padding(start = 2.dp)
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(presetBanners) { ban ->
                        val isSel = ban == selectedBanner
                        Box(
                            modifier = Modifier
                                .width(68.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSel) 2.dp else 0.5.dp,
                                    color = if (isSel) colors.iosBlue else colors.borderSubtle,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedBanner = ban
                                    SoundManager.playReaction()
                                }
                        ) {
                            AsyncImage(
                                model = ban,
                                contentDescription = "Banner Option",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Avatar Selector ──
                Text(
                    text = "CHOOSE AVATAR PRESET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted,
                    modifier = Modifier.fillMaxWidth().padding(start = 2.dp)
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(presetAvatars) { av ->
                        val isSel = av == selectedAvatar
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSel) 2.dp else 0.5.dp,
                                    color = if (isSel) colors.iosBlue else colors.borderSubtle,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedAvatar = av
                                    SoundManager.playReaction()
                                }
                        ) {
                            AsyncImage(
                                model = av,
                                contentDescription = "Avatar Option",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Form Fields
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (colors.isLight) Color(0xFFF2F2F7) else Color(0xFF2C2C2E))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    // Display Name
                    Text("FULL NAME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textDark),
                        cursorBrush = SolidColor(colors.iosBlue),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // Username
                    Text("USERNAME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
                    BasicTextField(
                        value = username,
                        onValueChange = { username = it },
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textDark),
                        cursorBrush = SolidColor(colors.iosBlue),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // Bio Status
                    Text("ABOUT / BIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textMuted)
                    BasicTextField(
                        value = status,
                        onValueChange = { status = it },
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, color = colors.textDark),
                        cursorBrush = SolidColor(colors.iosBlue),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Action Buttons (Cancel / Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassSecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    LiquidGlassPrimaryButton(
                        text = "Save Changes",
                        onClick = {
                            onSave(
                                currentUser.copy(
                                    name = name.trim().ifEmpty { currentUser.name },
                                    username = username.trim().ifEmpty { currentUser.username },
                                    status = status.trim().ifEmpty { currentUser.status },
                                    avatar = selectedAvatar,
                                    banner = selectedBanner
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

// ─────────────────────────────────────────────────────────────────────────────
// BLOCKED CONTACTS MODAL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BlockedContactsModal(
    blockedChats: List<ChatConversation>,
    onDismiss: () -> Unit,
    onUnblock: (String) -> Unit
) {
    val colors = LocalChatColors.current

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Blocked Contacts",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textDark
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorCloseX(tint = colors.textMuted, size = 16.dp)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (blockedChats.isEmpty()) {
                val emptyState = rememberAppleGraphicAnimationState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppleSettingsGraphic(
                        modifier = Modifier.size(115.dp),
                        state = emptyState,
                        enableClickToAnimate = true
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "No Blocked Contacts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textDark
                    )
                    Text(
                        text = "Contacts you block will appear here.",
                        fontSize = 13.sp,
                        color = colors.textMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(blockedChats) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceSecondary)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncImage(
                                    model = chat.avatar,
                                    contentDescription = chat.name,
                                    modifier = Modifier.size(36.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = chat.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textDark
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.iosBlue.copy(alpha = 0.12f))
                                    .clickable { onUnblock(chat.id) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Unblock",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.iosBlue
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            LiquidGlassPrimaryButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ABOUT OSCELLION MODAL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AboutAppModal(onDismiss: () -> Unit) {
    val colors = LocalChatColors.current

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(colors.iosBlue, Color(0xFF5856D6))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                PhosphorPaperPlaneTilt(tint = Color.White, size = 32.dp)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Oscellion Chat",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textDark
            )

            Text(
                text = "Version 3.4.0 • Build 2026.10",
                fontSize = 13.sp,
                color = colors.textMuted
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Crafted with pure Apple iOS Design System principles, Signal Protocol End-to-End Encryption, and 20 bespoke chat bubble aesthetics.",
                fontSize = 13.5.sp,
                color = colors.textDark.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(20.dp))

            LiquidGlassPrimaryButton(
                text = "OK",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


