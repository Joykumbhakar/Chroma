package com.chroma.studio.ui.screens

import com.chroma.studio.ui.components.UserProfileStatusSection
import com.chroma.studio.ui.components.UserProfileHighlightsSection
import com.chroma.studio.ui.components.ProfileSeamlessTabBar
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.chroma.studio.chat.sound.SoundManager
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.Male
import androidx.compose.material.icons.rounded.Transgender
import com.composables.icons.lucide.ShieldAlert
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.components.getAppleStatusRingBrush
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.AiWaveSphereIcon
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.R
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.UserStoryGroup
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import kotlinx.coroutines.launch

private object AppleProfileTokens {
    fun accentBlue(isDark: Boolean) = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    fun secondaryFill(isDark: Boolean) = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    fun secondaryPressedFill(isDark: Boolean) = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    fun hairlineBorder(isDark: Boolean) = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
    fun mutedText(isDark: Boolean) = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)
    val OnlineGreen = Color(0xFF34C759)
}

@Suppress("DEPRECATION")
val ProfileTightPlatformStyle = PlatformTextStyle(includeFontPadding = false)

val ProfileLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both
)

val ProfileTightTextStyle = TextStyle(
    platformStyle = ProfileTightPlatformStyle,
    lineHeightStyle = ProfileLineHeightStyle
)

@OptIn(ExperimentalFoundationApi::class)
private fun lerpFloat(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileHeroSection(
    profile: UserProfile?,
    hasActiveStories: Boolean,
    worksCount: Int,
    isRestricted: Boolean,
    userId: String,
    onAvatarClick: () -> Unit,
    onAvatarLongClick: (() -> Unit)? = null,
    onNavigateToUserList: ((String, String) -> Unit)?,
    onPostsClick: (() -> Unit)? = null,
    isUploading: Boolean = false,
    isUploadFailed: Boolean = false,
    isOnlyFriendStory: Boolean = false,
    bannerProgress: Float = 0f,
    isStorySeen: Boolean = false,
    isCurrentUser: Boolean = false,
    showBackButton: Boolean = true,
    onBack: () -> Unit = {},
    onQrClick: (() -> Unit)? = null,
    onEditBannerClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    onBannerTap: (() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val presetBannerRes = remember(profile?.bannerUrl) { getBannerPresetDrawableRes(profile?.bannerUrl) }
    val sheetBg = OscellionProfilePalette.sheetBg(isDark)
    val inkPrimary = OscellionProfilePalette.inkPrimary(isDark)
    val inkMuted = OscellionProfilePalette.inkMuted(isDark)
    val hairline = OscellionProfilePalette.hairline(isDark)
    val statsDivider = OscellionProfilePalette.statsDivider(isDark)

    // Real-time online presence matching the chat / message screen
    val presence = com.chroma.studio.data.OnlineStatusManager.rememberUserPresence(userId)
    val isUserOnline = presence.online && (profile?.showOnlineStatus != false)

    val ease = smoothStepFraction(bannerProgress)
    val density = LocalDensity.current
    val baseBannerDp = 176f
    val bannerHeight = (baseBannerDp + 114f * bannerProgress.coerceIn(0f, 1.35f)).dp
    val avatarSizeDp = 96.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        // 1. Pull-to-Expand Signature Liquid-Wave or Custom Profile Cover Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .then(
                    if (onBannerTap != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBannerTap
                        )
                    } else Modifier
                )
        ) {
            val cleanBanner = profile?.bannerUrl?.trim().orEmpty()
            val useCustomBanner = cleanBanner.isNotBlank() &&
                !cleanBanner.contains("midnight_glow", ignoreCase = true) &&
                !cleanBanner.contains("default_banner", ignoreCase = true)

            when {
                presetBannerRes != null && useCustomBanner -> {
                    Image(
                        painter = painterResource(id = presetBannerRes),
                        contentDescription = "Profile Banner",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val zoom = 1f + ease * 0.05f
                                scaleX = zoom
                                scaleY = zoom
                            },
                        contentScale = ContentScale.Crop
                    )
                }
                useCustomBanner -> {
                    AsyncImage(
                        model = cleanBanner,
                        contentDescription = "Profile Banner",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val zoom = 1f + ease * 0.05f
                                scaleX = zoom
                                scaleY = zoom
                            },
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    LiquidWaveBannerCanvas(
                        isDark = isDark,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val zoom = 1f + ease * 0.05f
                                scaleX = zoom
                                scaleY = zoom
                            }
                    )
                }
            }

            // Pull-to-Expand Visual Cue Pill
            val cueAlpha = (1f - bannerProgress * 2.2f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(
                        (if (isDark) Color.White else OscellionProfilePalette.BlueAccent)
                            .copy(alpha = 0.22f * cueAlpha)
                    )
            )

            // Frosted Circular Banner Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    BannerGlassCircleButton(
                        isDark = isDark,
                        onClick = {
                            com.chroma.studio.chat.sound.SoundManager.playReaction()
                            onBack()
                        }
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronLeft,
                            contentDescription = "Back",
                            tint = inkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onQrClick != null) {
                        BannerGlassCircleButton(
                            isDark = isDark,
                            onClick = {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                onQrClick()
                            }
                        ) {
                            Icon(
                                imageVector = Lucide.QrCode,
                                contentDescription = "Share Card",
                                tint = inkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (isCurrentUser && onEditBannerClick != null) {
                        BannerGlassCircleButton(
                            isDark = isDark,
                            onClick = {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                onEditBannerClick()
                            }
                        ) {
                            Icon(
                                imageVector = Lucide.Pencil,
                                contentDescription = "Edit Cover",
                                tint = inkPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    if (isCurrentUser && onSettingsClick != null) {
                        BannerGlassCircleButton(
                            isDark = isDark,
                            onClick = {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                onSettingsClick()
                            }
                        ) {
                            Icon(
                                imageVector = Lucide.Settings,
                                contentDescription = "Settings",
                                tint = inkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else if (!isCurrentUser && onMoreClick != null) {
                        BannerGlassCircleButton(
                            isDark = isDark,
                            onClick = {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                onMoreClick()
                            }
                        ) {
                            Icon(
                                imageVector = Lucide.EllipsisVertical,
                                contentDescription = "More Options",
                                tint = inkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Rounded Sheet Overlapping Banner Bottom + Continuous Center-Shift Choreography
        // Note: Do NOT .clip() this Column vertically so the overlapping avatar & story ring render uncropped above the seam
        val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = (bannerHeight - 28.dp).coerceAtLeast(100.dp))
                .shadow(
                    elevation = 8.dp,
                    shape = sheetShape,
                    ambientColor = Color(0x180A183E),
                    spotColor = Color(0x180A183E),
                    clip = false
                )
                .background(sheetBg, sheetShape)
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp)
        ) {
            val roleText = profile?.role?.trim()?.takeIf { it.isNotBlank() }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val innerMaxWidth = this@BoxWithConstraints.maxWidth
                val innerWidthPx = with(density) { innerMaxWidth.toPx() }
                val avatarSizePx = with(density) { avatarSizeDp.toPx() }
                val avatarDx = ((innerWidthPx - avatarSizePx) / 2f) * ease
                val avatarYOffset = lerpFloat(-46f, -42f, ease).dp

                var nameRowWidthPx by remember { mutableFloatStateOf(0f) }
                var usernameWidthPx by remember { mutableFloatStateOf(0f) }
                var statsWidthPx by remember { mutableFloatStateOf(0f) }

                val nameDx = maxOf(0f, (innerWidthPx - nameRowWidthPx) / 2f) * ease
                val userDx = maxOf(0f, (innerWidthPx - usernameWidthPx) / 2f) * ease

                val statsInitialLeft = (innerWidthPx - statsWidthPx).coerceAtLeast(0f)
                val statsTargetLeft = ((innerWidthPx - statsWidthPx) / 2f).coerceAtLeast(0f)
                val statsDx = (statsTargetLeft - statsInitialLeft) * ease
                val statsDy = with(density) { 86.dp.toPx() } * ease

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Row: Avatar + Right-Aligned Tabular Stats
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                    ) {
                        OscellionProfileAvatar(
                            profile = profile,
                            avatarUrl = profile?.avatarUrl.orEmpty(),
                            isDark = isDark,
                            isOnline = isUserOnline,
                            hasActiveStories = hasActiveStories,
                            isStorySeen = isStorySeen,
                            isOnlyFriendStory = isOnlyFriendStory,
                            isVerified = profile?.verified == true,
                            isUploading = isUploading,
                            isUploadFailed = isUploadFailed,
                            size = avatarSizeDp,
                            onTap = { onAvatarClick() },
                            onLongTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                SoundManager.playReaction()
                                onAvatarLongClick?.invoke()
                            },
                            modifier = Modifier
                                .offset(y = avatarYOffset)
                                .graphicsLayer {
                                    translationX = avatarDx
                                    val zoom = 1f + 0.09f * ease
                                    scaleX = zoom
                                    scaleY = zoom
                                }
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 4.dp, end = 4.dp)
                                .onGloballyPositioned { statsWidthPx = it.size.width.toFloat() }
                                .graphicsLayer {
                                    translationX = statsDx
                                    translationY = statsDy
                                }
                        ) {
                            ProfileStatItem(
                                value = formatCount(worksCount),
                                label = "Posts",
                                inkColor = inkPrimary,
                                mutedColor = inkMuted,
                                onClick = { onPostsClick?.invoke() }
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(statsDivider)
                            )
                            ProfileStatItem(
                                value = formatCount(profile?.followersCount ?: 0),
                                label = "Followers",
                                inkColor = inkPrimary,
                                mutedColor = inkMuted,
                                onClick = {
                                    if (isRestricted) ToastManager.showToast("This account is private.")
                                    else onNavigateToUserList?.invoke(userId, "followers")
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(statsDivider)
                            )
                            ProfileStatItem(
                                value = formatCount(profile?.followingCount ?: 0),
                                label = "Following",
                                inkColor = inkPrimary,
                                mutedColor = inkMuted,
                                onClick = {
                                    if (isRestricted) ToastManager.showToast("This account is private.")
                                    else onNavigateToUserList?.invoke(userId, "following")
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Display Name + Multi-Tier Verified Badge + Role Capsule Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier
                            .widthIn(max = innerMaxWidth)
                            .onGloballyPositioned { nameRowWidthPx = it.size.width.toFloat() }
                            .graphicsLayer { translationX = nameDx }
                    ) {
                        Text(
                            text = profile?.displayName ?: "Unknown",
                            color = inkPrimary,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                            lineHeight = 25.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ProfileTightTextStyle,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (profile?.verified == true) {
                            VerifiedBadge(
                                size = 19.dp,
                                tone = profile.verifiedTone,
                                userId = profile.uid,
                                showGlow = false
                            )
                        }

                        if (roleText != null) {
                            OscellionRoleCapsuleBadge(
                                role = roleText,
                                isDark = isDark
                            )
                        }
                    }

                    // @username with subtle copy affordance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .widthIn(max = innerMaxWidth)
                            .onGloballyPositioned { usernameWidthPx = it.size.width.toFloat() }
                            .graphicsLayer { translationX = userDx }
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("username", "@${profile?.username ?: ""}")
                                clipboard.setPrimaryClip(clip)
                                ToastManager.showToast("Username copied to clipboard")
                            }
                            .padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = "@${profile?.username ?: "unknown"}",
                            color = inkMuted,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = ProfileTightTextStyle,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Lucide.Copy,
                            contentDescription = "Copy username",
                            tint = inkMuted.copy(alpha = 0.72f),
                            modifier = Modifier.size(11.5.dp)
                        )
                    }

                    // Smoothly opening slot for centered stats when banner expands
                    Spacer(Modifier.height(56.dp * ease))
                }
            }
        }
    }
}

@Composable
private fun BannerGlassCircleButton(
    isDark: Boolean,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = tween(110),
        label = "bannerBtnScale"
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(scale)
            .shadow(2.dp, CircleShape, ambientColor = Color(0x1E070B18), spotColor = Color(0x1E070B18))
            .clip(CircleShape)
            .background(OscellionProfilePalette.topIconBg(isDark))
            .border(1.dp, OscellionProfilePalette.topIconBorder(isDark), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun ProfileStatItem(
    value: String,
    label: String,
    inkColor: Color,
    mutedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(110),
        label = "statScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Text(
            text = value,
            color = inkColor,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.02).em,
            lineHeight = 18.sp,
            style = ProfileTightTextStyle.copy(fontFeatureSettings = "tnum")
        )
        Text(
            text = label,
            color = mutedColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 13.sp,
            style = ProfileTightTextStyle
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserProfileBioDetailsSection(
    profile: UserProfile?,
    isCurrentUser: Boolean,
    isFollowing: Boolean,
    mutualFollowers: List<UserProfile>,
    userId: String,
    onShowShareCard: () -> Unit,
    onNavigateToUserList: ((String, String) -> Unit)?,
    onNavigateToProfile: ((String) -> Unit)? = null,
    onOpenWebsiteSheet: ((url: String, cta: String) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val scope = rememberCoroutineScope()
    val blueAccent = OscellionProfilePalette.BlueAccent
    val bioInk = OscellionProfilePalette.inkSecondary(isDark)
    val mutedText = OscellionProfilePalette.inkMuted(isDark)
    var isBioExpanded by remember { mutableStateOf(false) }

    val vis = profile?.dataVisibility
    val locationVisible = isFieldVisible("location", vis, isCurrentUser, isFollowing)
    val office = profile?.officeLocation?.takeIf { it.isNotBlank() }
    val home = profile?.homeLocation?.takeIf { it.isNotBlank() }
    val country = profile?.countryLocation?.takeIf { it.isNotBlank() }
    val dobVisible = isFieldVisible("dob", vis, isCurrentUser, isFollowing) && profile?.dateOfBirthAsLong != null
    val genderVisible = isFieldVisible("gender", vis, isCurrentUser, isFollowing) && !profile?.gender.isNullOrBlank()

    var fallbackWebsiteSheetUrl by remember { mutableStateOf<String?>(null) }
    var fallbackWebsiteCta by remember { mutableStateOf("Open") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OscellionProfilePalette.sheetBg(isDark))
            .padding(horizontal = 20.dp)
    ) {
        // 1. Clean Editorial Biography with interactive @mentions and Read more / Show less
        val bioText = profile?.bio?.trim() ?: ""
        if (bioText.isNotBlank()) {
            val isBioLong = remember(bioText) {
                bioText.length > 90 || bioText.contains("\n")
            }

            val displayedBio = remember(bioText, isBioExpanded, isBioLong, blueAccent, mutedText) {
                buildAnnotatedString {
                    val source = if (!isBioLong || isBioExpanded) {
                        bioText
                    } else {
                        val firstNewline = bioText.indexOf('\n')
                        val cutIndex = when {
                            firstNewline in 1..90 -> firstNewline
                            bioText.length > 90 -> {
                                val spaceIdx = bioText.lastIndexOf(' ', 90)
                                if (spaceIdx > 48) spaceIdx else 90
                            }
                            else -> bioText.length
                        }
                        bioText.take(cutIndex).trimEnd()
                    }

                    val words = source.split(Regex("(?<=\\s)|(?=\\s)|\\n"))
                    for (word in words) {
                        if (word.startsWith("@") && word.length > 1) {
                            val cleanHandle = word.trimEnd('.', ',', '!', '?', ':', ';', ')')
                            val trailing = word.removePrefix(cleanHandle)
                            pushStringAnnotation(tag = "MENTION", annotation = cleanHandle.removePrefix("@"))
                            withStyle(SpanStyle(color = blueAccent, fontWeight = FontWeight.SemiBold)) {
                                append(cleanHandle)
                            }
                            pop()
                            if (trailing.isNotEmpty()) append(trailing)
                        } else if (word.startsWith("#") && word.length > 1) {
                            withStyle(SpanStyle(color = blueAccent, fontWeight = FontWeight.Medium)) {
                                append(word)
                            }
                        } else {
                            append(word)
                        }
                    }

                    if (isBioLong && !isBioExpanded) {
                        withStyle(SpanStyle(color = mutedText)) {
                            append("… ")
                        }
                        pushStringAnnotation(tag = "TOGGLE_BIO", annotation = "expand")
                        withStyle(SpanStyle(color = blueAccent, fontWeight = FontWeight.SemiBold)) {
                            append("Read more")
                        }
                        pop()
                    } else if (isBioLong && isBioExpanded) {
                        pushStringAnnotation(tag = "TOGGLE_BIO", annotation = "collapse")
                        withStyle(SpanStyle(color = blueAccent, fontWeight = FontWeight.SemiBold)) {
                            append("  Show less")
                        }
                        pop()
                    }
                }
            }

            var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

            Text(
                text = displayedBio,
                color = bioInk,
                fontSize = 13.5.sp,
                lineHeight = 21.5.sp,
                style = TextStyle(
                    platformStyle = ProfileTightPlatformStyle,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None
                    )
                ),
                maxLines = if (isBioExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Clip,
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
                    .pointerInput(displayedBio, isBioLong) {
                        detectTapGestures { offset ->
                            val layout = textLayoutResult ?: return@detectTapGestures
                            val charOffset = layout.getOffsetForPosition(offset)
                            val mentionAnn = displayedBio.getStringAnnotations("MENTION", charOffset, charOffset).firstOrNull()
                            if (mentionAnn != null) {
                                val handle = mentionAnn.item
                                scope.launch {
                                    try {
                                        val mentionedProfile = com.chroma.studio.data.DiscoverRepository.getInstance()
                                            .getUserByUsername(handle)
                                        if (mentionedProfile != null && onNavigateToProfile != null) {
                                            onNavigateToProfile(mentionedProfile.uid)
                                        } else {
                                            ToastManager.showToast("@$handle")
                                        }
                                    } catch (_: Exception) {
                                        ToastManager.showToast("@$handle")
                                    }
                                }
                                return@detectTapGestures
                            }
                            val toggleAnn = displayedBio.getStringAnnotations("TOGGLE_BIO", charOffset, charOffset).firstOrNull()
                            if (toggleAnn != null || isBioLong) {
                                isBioExpanded = !isBioExpanded
                            }
                        }
                    }
            )
        }

        // 2. Borderless Compact Metadata Row: Location · [Calendar] 20XX · [Gender Symbol Icon ONLY]
        val locationLabel = remember(locationVisible, office, home, country) {
            if (!locationVisible) null
            else when {
                !office.isNullOrBlank() && !country.isNullOrBlank() -> "$office, $country"
                !office.isNullOrBlank() -> office
                !country.isNullOrBlank() -> country
                !home.isNullOrBlank() -> home
                else -> null
            }
        }
        val birthYearLabel = remember(dobVisible, profile?.dateOfBirthAsLong) {
            val dob = profile?.dateOfBirthAsLong
            if (dobVisible && dob != null && dob > 0L) {
                SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(dob))
            } else null
        }
        val genderValue = profile?.gender?.trim()?.takeIf {
            genderVisible && it.isNotBlank() && it.lowercase() !in setOf("prefer not to say", "none", "hidden")
        }

        if (locationLabel != null || birthYearLabel != null || genderValue != null) {
            Spacer(Modifier.height(12.dp))
            val sepColor = if (isDark) Color(0xFF3B475C) else Color(0xFFCFD8E6)
            val metaIconTint = OscellionProfilePalette.inkMuted(isDark)
            val metaTextInk = OscellionProfilePalette.metaInk(isDark)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (locationLabel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        LocationPinVectorIcon(tint = metaIconTint, modifier = Modifier.size(14.dp))
                        Text(
                            text = locationLabel,
                            color = metaTextInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 15.sp,
                            style = ProfileTightTextStyle
                        )
                    }
                }
                if (locationLabel != null && (birthYearLabel != null || genderValue != null)) {
                    Text(
                        text = "·",
                        color = sepColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                if (birthYearLabel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        CalendarYearVectorIcon(tint = metaIconTint, modifier = Modifier.size(14.dp))
                        Text(
                            text = birthYearLabel,
                            color = metaTextInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 15.sp,
                            style = ProfileTightTextStyle.copy(fontFeatureSettings = "tnum")
                        )
                    }
                }
                if (birthYearLabel != null && genderValue != null) {
                    Text(
                        text = "·",
                        color = sepColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
                if (genderValue != null) {
                    GenderSymbolVectorIcon(
                        gender = genderValue,
                        tint = metaIconTint,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(14.dp)
                    )
                }
            }
        }

        // 3. 3D Floating + Sheen Borderless Social Links Row (GitHub, Instagram, Website)
        val socialLinksMap = profile?.socialLinks ?: emptyMap()
        val ghUrl = socialLinksMap["github"]?.trim()?.takeIf { it.isNotBlank() }
        val igUrl = socialLinksMap["instagram"]?.trim()?.takeIf { it.isNotBlank() }
        val webUrl = socialLinksMap["website"]?.trim()?.takeIf { it.isNotBlank() }
        val websiteCta = socialLinksMap["website_cta"]?.trim()?.takeIf { it.isNotBlank() }
            ?: socialLinksMap["website_label"]?.trim()?.takeIf { it.isNotBlank() }
            ?: "Open"

        val normalizedWebUrl = remember(webUrl) {
            webUrl?.let { raw ->
                if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) raw
                else "https://$raw"
            }
        }

        if (ghUrl != null || igUrl != null || normalizedWebUrl != null) {
            Spacer(Modifier.height(14.dp))
            Social3DLinksRow(
                showGithub = ghUrl != null,
                showInstagram = igUrl != null,
                showWebsite = normalizedWebUrl != null,
                onGithubClick = {
                    val raw = ghUrl ?: return@Social3DLinksRow
                    try {
                        val target = when {
                            raw.startsWith("http://", true) || raw.startsWith("https://", true) -> raw
                            raw.contains(".") -> "https://$raw"
                            else -> "https://github.com/${raw.removePrefix("@")}"
                        }
                        com.chroma.studio.ui.components.InAppBrowserManager.openUrl(target)
                    } catch (_: Exception) {
                        ToastManager.showToast("Could not open GitHub")
                    }
                },
                onInstagramClick = {
                    val raw = igUrl ?: return@Social3DLinksRow
                    try {
                        val target = when {
                            raw.startsWith("http://", true) || raw.startsWith("https://", true) -> raw
                            raw.contains(".") -> "https://$raw"
                            else -> "https://instagram.com/${raw.removePrefix("@")}"
                        }
                        com.chroma.studio.ui.components.InAppBrowserManager.openUrl(target)
                    } catch (_: Exception) {
                        ToastManager.showToast("Could not open Instagram")
                    }
                },
                onWebsiteClick = {
                    val targetUrl = normalizedWebUrl ?: return@Social3DLinksRow
                    com.chroma.studio.chat.sound.SoundManager.playReaction()
                    if (onOpenWebsiteSheet != null) {
                        onOpenWebsiteSheet(targetUrl, websiteCta)
                    } else {
                        fallbackWebsiteCta = websiteCta
                        fallbackWebsiteSheetUrl = targetUrl
                    }
                }
            )
        }

        if (fallbackWebsiteSheetUrl != null) {
            val activeUrl = fallbackWebsiteSheetUrl!!
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { fallbackWebsiteSheetUrl = null },
                sheetState = sheetState,
                containerColor = OscellionProfilePalette.elevatedCard(isDark),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 6.dp)
                            .width(38.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF3A4458) else Color(0xFFCBD5E1))
                    )
                },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                windowInsets = WindowInsets(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = activeUrl,
                        color = OscellionProfilePalette.inkPrimary(isDark),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp,
                        style = ProfileTightTextStyle
                    )

                    OscellionPrimaryGradientButton(
                        onClick = {
                            fallbackWebsiteSheetUrl = null
                            try {
                                com.chroma.studio.ui.components.InAppBrowserManager.openUrl(activeUrl)
                            } catch (_: Exception) {
                                ToastManager.showToast("Could not open link")
                            }
                        },
                        height = 46.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = fallbackWebsiteCta,
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            style = ProfileTightTextStyle
                        )
                    }
                }
            }
        }

        // 4. Mutual Connections Group
        if (mutualFollowers.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            val connectedUsers = remember(mutualFollowers) {
                mutualFollowers.map { it.toConnectedUser() }
            }
            ConnectedUsersAvatarGroup(
                users = connectedUsers,
                totalCount = mutualFollowers.size,
                maxVisible = 4,
                avatarSize = 24.dp,
                overlapOffset = 6.dp,
                prefixText = "Followed by",
                suffixText = "you follow",
                showTextSummary = true,
                onUserClick = { clickedUid ->
                    if (onNavigateToProfile != null) {
                        onNavigateToProfile(clickedUid)
                    } else {
                        onNavigateToUserList?.invoke(clickedUid, "followers")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileSquircleSocialButton(
    platform: String,
    url: String = "",
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "squircleScale"
    )

    val cleanPlatform = platform.lowercase().trim()
    val cleanUrl = url.lowercase().trim()

    val imageRes = when {
        cleanPlatform.contains("github") || cleanUrl.contains("github.com") -> R.drawable.ic_social_github
        cleanPlatform.contains("instagram") || cleanPlatform == "ig" || cleanPlatform == "insta" || cleanUrl.contains("instagram.com") -> R.drawable.ic_social_instagram
        cleanPlatform.contains("portfolio") || cleanPlatform.contains("website") || cleanPlatform == "web" || cleanPlatform == "site" || cleanPlatform == "works" || cleanPlatform == "dribbble" || cleanPlatform == "behance" || cleanUrl.contains("dribbble.com") || cleanUrl.contains("behance.net") -> R.drawable.ic_social_portfolio
        cleanPlatform.contains("camera") || cleanPlatform.contains("photo") || cleanPlatform == "vsco" || cleanPlatform == "flickr" || cleanPlatform == "unsplash" || cleanPlatform == "gallery" || cleanUrl.contains("vsco.co") || cleanUrl.contains("flickr.com") || cleanUrl.contains("unsplash.com") -> R.drawable.ic_social_camera
        cleanPlatform.contains("fingerprint") || cleanPlatform.contains("ident") || cleanPlatform.contains("bio") || cleanPlatform.contains("secur") || cleanPlatform.contains("key") || cleanPlatform.contains("verify") || cleanPlatform.contains("pass") -> R.drawable.ic_social_fingerprint
        else -> null
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .size(34.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = platform,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // Border-less fallback for unmapped platforms (e.g. twitter/x, linkedin, link)
            val (icon, gradient) = when (cleanPlatform) {
                "twitter", "x" -> Lucide.Twitter to Brush.linearGradient(listOf(Color(0xFF1DA1F2), Color(0xFF0C7ABF)))
                "linkedin" -> Lucide.Linkedin to Brush.linearGradient(listOf(Color(0xFF0077B5), Color(0xFF004182)))
                else -> Lucide.Link to Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
            }
            val shape = RoundedCornerShape(9.5.dp)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(shape)
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = platform,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileActionButton(
    onClick: () -> Unit,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "profileBtnScale"
    )

    val shape = RoundedCornerShape(12.dp)
    val appleBlue = AppleProfileTokens.accentBlue(isDark)
    val background = if (isPrimary) {
        if (isPressed) appleBlue.copy(alpha = 0.86f) else appleBlue
    } else {
        if (isPressed) AppleProfileTokens.secondaryPressedFill(isDark)
        else AppleProfileTokens.secondaryFill(isDark)
    }
    val hairline = if (isPrimary) Color.Transparent else AppleProfileTokens.hairlineBorder(isDark)

    Box(
        modifier = modifier
            .scale(scale)
            .height(40.dp)
            .clip(shape)
            .background(background)
            .then(
                if (!isPrimary) Modifier.border(0.5.dp, hairline, shape) else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    com.chroma.studio.chat.sound.SoundManager.playReaction()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}


@Composable
fun UserProfileActionsStoriesSection(
    isCurrentUser: Boolean,
    isFollowing: Boolean,
    userId: String,
    currentUserId: String?,
    profile: UserProfile?,
    filteredStories: List<UserStoryGroup>,
    hasMyStory: Boolean = false,
    profileTabIcons: List<ImageVector>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onToggleFollow: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onShowShareCard: () -> Unit,
    onNavigateToChat: ((String) -> Unit)?,
    onMyStoryClick: (() -> Unit)? = null,
    onAddStoryClick: () -> Unit = {},
    onStoryClick: ((groupIndex: Int) -> Unit)? = null,
    storyVm: StoryViewModel,
    isFollowRequested: Boolean = false,
    onHighlightClick: ((com.chroma.studio.ui.components.ProfileHighlightItem) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val sheetBg = OscellionProfilePalette.sheetBg(isDark)
    val inkPrimary = OscellionProfilePalette.inkPrimary(isDark)
    val mutedInk = OscellionProfilePalette.inkMuted(isDark)
    val softPill = OscellionProfilePalette.softPill(isDark)
    val hairline = OscellionProfilePalette.hairline(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(sheetBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Compact Apple Activity / Music Status Indicator
            UserProfileStatusSection(
                profile = profile,
                isCurrentUser = isCurrentUser,
                userId = userId
            )

            Spacer(Modifier.height(14.dp))

            // Symmetric Oscellion Action Pill Buttons Row (43dp CircleShape pills)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrentUser) {
                    OscellionPrimaryGradientButton(
                        onClick = {
                            com.chroma.studio.chat.sound.SoundManager.playReaction()
                            onNavigateToEditProfile()
                        },
                        height = 43.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Lucide.Pencil,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Edit Profile",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.01).em,
                            lineHeight = 16.sp,
                            style = ProfileTightTextStyle
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(43.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF182030) else Color(0xFFF4F7FD))
                            .border(1.dp, if (isDark) Color(0xFF283247) else Color(0xFFE6EDF8), CircleShape)
                            .clickable {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                onShowShareCard()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Share,
                                contentDescription = null,
                                tint = OscellionProfilePalette.BlueAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Share Card",
                                color = inkPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                letterSpacing = (-0.01).em,
                                lineHeight = 16.sp,
                                style = ProfileTightTextStyle
                            )
                        }
                    }
                } else {
                    if (isFollowing) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(43.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF182030) else Color(0xFFF4F7FD))
                                .border(1.dp, if (isDark) Color(0xFF283247) else Color(0xFFDCE5F4), CircleShape)
                                .clickable {
                                    com.chroma.studio.chat.sound.SoundManager.playReaction()
                                    onToggleFollow()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = null,
                                    tint = inkPrimary,
                                    modifier = Modifier.size(14.5.dp)
                                )
                                Text(
                                    text = "Following",
                                    color = inkPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    style = ProfileTightTextStyle
                                )
                            }
                        }
                    } else if (isFollowRequested) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(43.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF182030) else Color(0xFFF4F7FD))
                                .border(1.dp, if (isDark) Color(0xFF283247) else Color(0xFFDCE5F4), CircleShape)
                                .clickable {
                                    com.chroma.studio.chat.sound.SoundManager.playReaction()
                                    onToggleFollow()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Clock,
                                    contentDescription = null,
                                    tint = mutedInk,
                                    modifier = Modifier.size(14.5.dp)
                                )
                                Text(
                                    text = "Requested",
                                    color = mutedInk,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    style = ProfileTightTextStyle
                                )
                            }
                        }
                    } else {
                        OscellionPrimaryGradientButton(
                            onClick = {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                onToggleFollow()
                            },
                            height = 43.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Lucide.UserPlus,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.5.dp)
                            )
                            Text(
                                text = "Follow",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                style = ProfileTightTextStyle
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(43.dp)
                            .clip(CircleShape)
                            .background(OscellionProfilePalette.elevatedCard(isDark))
                            .border(1.dp, OscellionProfilePalette.ringInactive(isDark), CircleShape)
                            .clickable {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                onNavigateToChat?.invoke(userId)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.MessageCircle,
                                contentDescription = null,
                                tint = OscellionProfilePalette.BlueAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Message",
                                color = inkPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                style = ProfileTightTextStyle
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(43.dp)
                            .clip(CircleShape)
                            .background(OscellionProfilePalette.pillSurface(isDark))
                            .border(1.dp, if (isDark) Color(0xFF283247) else Color(0xFFE6EDF8), CircleShape)
                            .clickable {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                onShowShareCard()
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Share,
                                contentDescription = "Share Profile Card",
                                tint = OscellionProfilePalette.subInk(isDark),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Share",
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF374151),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                style = ProfileTightTextStyle
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Saved Story Collections / Highlights
            UserProfileHighlightsSection(
                userId = userId,
                isCurrentUser = isCurrentUser,
                storyViewModel = storyVm,
                onHighlightClick = { hl ->
                    if (onHighlightClick != null) {
                        onHighlightClick(hl)
                    } else if (onMyStoryClick != null && isCurrentUser) {
                        onMyStoryClick()
                    } else if (onStoryClick != null && filteredStories.isNotEmpty()) {
                        onStoryClick(0)
                    }
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Labeled Profile Content Tab Switcher with Custom Heart-Handshake Remix Icon & Sliding Pill Indicator
        val tabLabels = remember(isCurrentUser, profileTabIcons.size) {
            if (isCurrentUser || profileTabIcons.size >= 5) {
                listOf("All", "Posts", "Flash", "Remixes", "Saved")
            } else {
                listOf("All", "Posts", "Flash", "Remixes")
            }
        }
        OscellionProfileTabsBar(
            tabs = tabLabels,
            selectedIndex = selectedTabIndex,
            isDark = isDark,
            onSelectTab = {
                com.chroma.studio.chat.sound.SoundManager.playReaction()
                onTabSelected(it)
            }
        )

        Spacer(Modifier.height(2.dp))
    }
}

/**
 * Instagram-style Long-Press Avatar Action Sheet
 * Allows user to choose: "View Story" or "View Profile Photo" (and "Change Profile Photo" for current user)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAvatarActionBottomSheet(
    profile: UserProfile?,
    hasActiveStories: Boolean,
    storyCount: Int,
    isCurrentUser: Boolean,
    onViewStory: () -> Unit,
    onViewProfilePhoto: () -> Unit,
    onChangePhoto: (() -> Unit)? = null,
    isOnlyFriendStory: Boolean = false,
    onDismissRequest: () -> Unit
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
        },
        windowInsets = WindowInsets(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Profile Preview Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mini Avatar with Story Ring if active
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasActiveStories) {
                        val ringBrush = getAppleStatusRingBrush(
                            hasStatus = true,
                            isSeen = false,
                            isDark = colors.isDark,
                            isOnlyFriend = isOnlyFriendStory,
                            isVerified = profile?.verified == true
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 2.dp,
                                    brush = ringBrush,
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                        ) {
                            IosAvatarView(
                                name = profile?.displayName ?: profile?.username ?: "",
                                avatarUrl = profile?.avatarUrl.orEmpty(),
                                size = 46.dp,
                                gender = profile?.gender,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(
                                    width = 0.8.dp,
                                    color = if (colors.isDark) Color(0xFF38383A) else Color(0xFFE5E5EA),
                                    shape = CircleShape
                                )
                        ) {
                            IosAvatarView(
                                name = profile?.displayName ?: profile?.username ?: "",
                                avatarUrl = profile?.avatarUrl.orEmpty(),
                                size = 52.dp,
                                gender = profile?.gender,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = profile?.displayName.orEmpty().ifBlank { profile?.username ?: "User" },
                        color = colors.textMain,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        lineHeight = 19.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ProfileTightTextStyle
                    )
                    Text(
                        text = "@${profile?.username.orEmpty().ifBlank { "user" }}",
                        color = colors.textMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ProfileTightTextStyle
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // iOS / Samsung Grouped Card Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
            ) {
                // 1. View Story
                val storyInteractionSource = remember { MutableInteractionSource() }
                val isStoryPressed by storyInteractionSource.collectIsPressedAsState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isStoryPressed) {
                                if (colors.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                            } else Color.Transparent
                        )
                        .clickable(
                            interactionSource = storyInteractionSource,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                SoundManager.playReaction()
                                if (hasActiveStories) {
                                    onDismissRequest()
                                    onViewStory()
                                } else {
                                    ToastManager.showToast("No active stories right now")
                                }
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val playBgBrush = if (hasActiveStories) {
                        getAppleStatusRingBrush(
                            hasStatus = true,
                            isSeen = false,
                            isDark = colors.isDark,
                            isOnlyFriend = isOnlyFriendStory,
                            isVerified = profile?.verified == true
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                if (colors.isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6),
                                if (colors.isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6)
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(playBgBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Play,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "View Story",
                            color = if (hasActiveStories) colors.textMain else colors.textMuted,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp,
                            style = ProfileTightTextStyle
                        )
                        Text(
                            text = if (hasActiveStories) {
                                if (storyCount > 0) "$storyCount active ${if (storyCount == 1) "story" else "stories"}"
                                else "Watch active stories"
                            } else "No active stories",
                            color = colors.textMuted,
                            fontSize = 12.5.sp,
                            lineHeight = 15.sp,
                            style = ProfileTightTextStyle
                        )
                    }

                    if (hasActiveStories) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF0381FE), Color(0xFF0072DE))))
                                .padding(horizontal = 9.dp, vertical = 3.5.dp)
                        ) {
                            Text(
                                text = "Active",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            tint = colors.textMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = if (colors.isDark) Color(0xFF38383A) else Color(0xFFE5E5EA),
                    modifier = Modifier.padding(start = 70.dp, end = 16.dp)
                )

                // 2. View Profile Photo
                val photoInteractionSource = remember { MutableInteractionSource() }
                val isPhotoPressed by photoInteractionSource.collectIsPressedAsState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isPhotoPressed) {
                                if (colors.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                            } else Color.Transparent
                        )
                        .clickable(
                            interactionSource = photoInteractionSource,
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                SoundManager.playReaction()
                                onDismissRequest()
                                onViewProfilePhoto()
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0381FE)), // In-App Brand Blue
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Eye,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "View Profile Photo",
                            color = colors.textMain,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp,
                            style = ProfileTightTextStyle
                        )
                        Text(
                            text = "See full-size profile picture",
                            color = colors.textMuted,
                            fontSize = 12.5.sp,
                            lineHeight = 15.sp,
                            style = ProfileTightTextStyle
                        )
                    }

                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 3. Change Profile Photo (Current User only)
                if (isCurrentUser && onChangePhoto != null) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = if (colors.isDark) Color(0xFF38383A) else Color(0xFFE5E5EA),
                        modifier = Modifier.padding(start = 70.dp, end = 16.dp)
                    )

                    val changeInteractionSource = remember { MutableInteractionSource() }
                    val isChangePressed by changeInteractionSource.collectIsPressedAsState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isChangePressed) {
                                    if (colors.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                                } else Color.Transparent
                            )
                            .clickable(
                                interactionSource = changeInteractionSource,
                                indication = null,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    SoundManager.playReaction()
                                    onDismissRequest()
                                    onChangePhoto()
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF5856D6)), // Apple / One UI Purple
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Camera,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = "Change Profile Photo",
                                color = colors.textMain,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp,
                                style = ProfileTightTextStyle
                            )
                            Text(
                                text = "Upload new avatar from device",
                                color = colors.textMuted,
                                fontSize = 12.5.sp,
                                lineHeight = 15.sp,
                                style = ProfileTightTextStyle
                            )
                        }

                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Cancel Button
            val cancelInteractionSource = remember { MutableInteractionSource() }
            val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isCancelPressed) {
                            if (colors.isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)
                        } else {
                            if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                        }
                    )
                    .clickable(
                        interactionSource = cancelInteractionSource,
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismissRequest()
                        }
                    ),
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
