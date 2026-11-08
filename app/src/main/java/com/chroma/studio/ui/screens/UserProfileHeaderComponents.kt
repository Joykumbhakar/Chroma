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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.components.AppleStatusVibrantGradient
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
    onPostsClick: (() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val heroHazeState = remember { HazeState() }
    val presetBannerRes = remember(profile?.bannerUrl) { getBannerPresetDrawableRes(profile?.bannerUrl) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // 1. Cinematic Banner with subtle bottom corner rounding & top scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
                .haze(heroHazeState)
                .background(if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
        ) {
            if (presetBannerRes != null) {
                Image(
                    painter = painterResource(id = presetBannerRes),
                    contentDescription = "Profile Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (!profile?.bannerUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profile?.bannerUrl,
                    contentDescription = "Profile Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.banner_wave_banner_midnight_glow),
                    contentDescription = "Default Profile Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Top Ambient Gradient Scrim to ensure status bar & action icons always pop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
            )
        }

        // 2. Avatar & Stats Row — Straddling the banner seam with pixel perfection
        // Avatar is 84dp: 42dp over banner, 42dp over profile body
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
                .padding(top = 80.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Elevated Avatar with Crisp Cut-out Ring & Tactile Spring Scale
            val avatarInteractionSource = remember { MutableInteractionSource() }
            val isAvatarPressed by avatarInteractionSource.collectIsPressedAsState()
            val avatarScale by animateFloatAsState(
                targetValue = if (isAvatarPressed) 0.94f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "avatarScale"
            )

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(avatarScale),
                contentAlignment = Alignment.Center
            ) {
                // Avatar Container with Theme Background Separation Mask & Clean Native Elevation
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            clip = false,
                            spotColor = Color.Black.copy(alpha = 0.20f),
                            ambientColor = Color.Black.copy(alpha = 0.10f)
                        )
                        .clip(CircleShape)
                        .background(colors.bg)
                        .combinedClickable(
                            interactionSource = avatarInteractionSource,
                            indication = null,
                            onClick = { onAvatarClick() },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                SoundManager.playReaction()
                                onAvatarLongClick?.invoke()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasActiveStories) {
                        // Instagram-style Story Ring with inner theme gap
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 2.8.dp,
                                    brush = Brush.sweepGradient(AppleStatusVibrantGradient),
                                    shape = CircleShape
                                )
                                .padding(4.5.dp) // Gap separator in theme background color
                                .clip(CircleShape)
                                .background(colors.bg)
                        ) {
                            IosAvatarView(
                                name = profile?.displayName ?: profile?.username ?: "",
                                avatarUrl = profile?.avatarUrl.orEmpty(),
                                size = 74.dp,
                                gender = profile?.gender,
                                isOnline = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Standard Profile Avatar with crisp theme border
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 3.5.dp,
                                    color = colors.bg,
                                    shape = CircleShape
                                )
                                .padding(1.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 0.8.dp,
                                    brush = Brush.linearGradient(
                                        if (colors.isDark) listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))
                                        else listOf(Color(0xFFE5E5EA), Color(0xFFF2F2F7))
                                    ),
                                    shape = CircleShape
                                )
                        ) {
                            IosAvatarView(
                                name = profile?.displayName ?: profile?.username ?: "",
                                avatarUrl = profile?.avatarUrl.orEmpty(),
                                size = 78.dp,
                                gender = profile?.gender,
                                isOnline = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Online Status Indicator with Theme Border
                if (profile?.online == true && profile?.showOnlineStatus == true) {
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .background(colors.bg, CircleShape)
                            .padding(2.5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C759))
                    )
                }
            }

            // 3. Stats Card — Apple / One UI Frosted Blur Background Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .hazeChild(
                        state = heroHazeState,
                        shape = RoundedCornerShape(16.dp),
                        style = HazeDefaults.style(
                            tint = Color.Transparent,
                            blurRadius = 24.dp,
                            noiseFactor = 0f
                        )
                    )
                    .background(
                        if (colors.isDark) Color(0xFF1C1C1E).copy(alpha = 0.55f)
                        else Color.White.copy(alpha = 0.70f)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statNumberColor = colors.textMain
                    val statLabelColor = colors.textMuted

                    // Posts Count
                    val postsInteraction = remember { MutableInteractionSource() }
                    val isPostsPressed by postsInteraction.collectIsPressedAsState()
                    val postsScale by animateFloatAsState(
                        targetValue = if (isPostsPressed) 0.94f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "postsScale"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier
                            .weight(1f)
                            .scale(postsScale)
                            .clickable(
                                interactionSource = postsInteraction,
                                indication = null
                            ) {
                                onPostsClick?.invoke()
                            }
                    ) {
                        Text(
                            text = formatCount(worksCount),
                            color = statNumberColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.02).em,
                            lineHeight = 19.sp,
                            style = ProfileTightTextStyle
                        )
                        Text(
                            text = "Posts",
                            color = statLabelColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 13.5.sp,
                            style = ProfileTightTextStyle
                        )
                    }

                    // Delicate Hairline Divider
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(0.5.dp)
                            .background(if (colors.isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.15f))
                    )

                    // Followers Count
                    val followersInteraction = remember { MutableInteractionSource() }
                    val isFollowersPressed by followersInteraction.collectIsPressedAsState()
                    val followersScale by animateFloatAsState(
                        targetValue = if (isFollowersPressed) 0.94f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "followersScale"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier
                            .weight(1f)
                            .scale(followersScale)
                            .clickable(
                                interactionSource = followersInteraction,
                                indication = null
                            ) {
                                if (isRestricted) ToastManager.showToast("This account is private.")
                                else onNavigateToUserList?.invoke(userId, "followers")
                            }
                    ) {
                        Text(
                            text = formatCount(profile?.followersCount ?: 0),
                            color = statNumberColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.02).em,
                            lineHeight = 19.sp,
                            style = ProfileTightTextStyle
                        )
                        Text(
                            text = "Followers",
                            color = statLabelColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 13.5.sp,
                            style = ProfileTightTextStyle
                        )
                    }

                    // Delicate Hairline Divider
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(0.5.dp)
                            .background(if (colors.isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.15f))
                    )

                    // Following Count
                    val followingInteraction = remember { MutableInteractionSource() }
                    val isFollowingPressed by followingInteraction.collectIsPressedAsState()
                    val followingScale by animateFloatAsState(
                        targetValue = if (isFollowingPressed) 0.94f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "followingScale"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier
                            .weight(1f)
                            .scale(followingScale)
                            .clickable(
                                interactionSource = followingInteraction,
                                indication = null
                            ) {
                                if (isRestricted) ToastManager.showToast("This account is private.")
                                else onNavigateToUserList?.invoke(userId, "following")
                            }
                    ) {
                        Text(
                            text = formatCount(profile?.followingCount ?: 0),
                            color = statNumberColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.02).em,
                            lineHeight = 19.sp,
                            style = ProfileTightTextStyle
                        )
                        Text(
                            text = "Following",
                            color = statLabelColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 13.5.sp,
                            style = ProfileTightTextStyle
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileBioDetailsSection(
    profile: UserProfile?,
    isCurrentUser: Boolean,
    isFollowing: Boolean,
    mutualFollowers: List<UserProfile>,
    userId: String,
    onShowShareCard: () -> Unit,
    onNavigateToUserList: ((String, String) -> Unit)?,
    onNavigateToProfile: ((String) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val context = LocalContext.current
    var isBioExpanded by remember { mutableStateOf(false) }

    val vis = profile?.dataVisibility
    val locationVisible = isFieldVisible("location", vis, isCurrentUser, isFollowing)
    val office = profile?.officeLocation?.takeIf { it.isNotBlank() }
    val home = profile?.homeLocation?.takeIf { it.isNotBlank() }
    val country = profile?.countryLocation?.takeIf { it.isNotBlank() }
    val dobVisible = isFieldVisible("dob", vis, isCurrentUser, isFollowing) && profile?.dateOfBirthAsLong != null
    val genderVisible = isFieldVisible("gender", vis, isCurrentUser, isFollowing) && !profile?.gender.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // 1. Identity Block: Display Name & @username tightly paired, with Sleek QR Pass Icon Button on the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile?.displayName ?: "Unknown",
                        color = colors.textMain,
                        fontSize = 20.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.02).em,
                        lineHeight = 23.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ProfileTightTextStyle
                    )
                    if (profile?.verified == true) {
                        Spacer(Modifier.width(6.dp))
                        VerifiedBadge(size = 18.dp, showGlow = true)
                    }
                }

                // Subtitle Row: @username
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("username", "@${profile?.username ?: ""}")
                            clipboard.setPrimaryClip(clip)
                            ToastManager.showToast("Username copied to clipboard")
                        }
                        .padding(horizontal = 2.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "@${profile?.username ?: "unknown"}",
                        color = colors.textMuted,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.5.sp,
                        style = ProfileTightTextStyle
                    )
                    Icon(
                        imageVector = Lucide.Copy,
                        contentDescription = "Copy username",
                        tint = colors.textMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.5.dp)
                    )
                }
            }

            // Sleek border-less Apple icon button for QR pass with spring touch feedback
            val qrInteraction = remember { MutableInteractionSource() }
            val isQrPressed by qrInteraction.collectIsPressedAsState()
            val qrScale by animateFloatAsState(
                targetValue = if (isQrPressed) 0.90f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "qrScale"
            )
            Box(
                modifier = Modifier
                    .scale(qrScale)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (colors.isDark) Color(0xFF1E222A) else Color(0xFFECEFF4))
                    .clickable(
                        interactionSource = qrInteraction,
                        indication = null
                    ) {
                        com.chroma.studio.chat.sound.SoundManager.playReaction()
                        onShowShareCard()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Lucide.QrCode,
                    contentDescription = "Show Pass",
                    tint = colors.textMain,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 3. Combined Role & Metadata Pills Row (FlowRow, Border-less)
        val roleText = profile?.role?.trim()?.takeIf { it.isNotBlank() }
        val metadataItems = remember(locationVisible, office, home, country, dobVisible, genderVisible, profile) {
            val list = mutableListOf<Pair<ImageVector, String>>()

            if (locationVisible) {
                val locText = when {
                    !office.isNullOrBlank() && !country.isNullOrBlank() -> "$office, $country"
                    !office.isNullOrBlank() -> office
                    !country.isNullOrBlank() -> country
                    !home.isNullOrBlank() -> home
                    else -> null
                }
                if (!locText.isNullOrBlank()) {
                    list.add(Lucide.MapPin to locText)
                }
            }

            val dob = profile?.dateOfBirthAsLong
            if (dobVisible && dob != null && dob > 0L) {
                val yr = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(dob))
                list.add(Lucide.Calendar to "Born $yr")
            }

            if (genderVisible && !profile?.gender.isNullOrBlank()) {
                list.add(Lucide.User to profile.gender.trim())
            }

            list
        }

        val hasPills = roleText != null || metadataItems.isNotEmpty()
        if (hasPills) {
            Spacer(Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Role Pill
                if (roleText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (colors.isDark) Color(0xFF0381FE).copy(alpha = 0.18f) else Color(0xFFF1F5FE))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Code,
                                contentDescription = null,
                                tint = Color(0xFF0062FF),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = roleText,
                                color = Color(0xFF0062FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.01).em,
                                lineHeight = 14.sp,
                                style = ProfileTightTextStyle
                            )
                        }
                    }
                }

                // Other Metadata Pills
                metadataItems.forEach { (icon, text) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (colors.isDark) Color(0xFF1E222A) else Color(0xFFEFF4F9))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.5.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (colors.isDark) Color(0xFFA0A5B0) else Color(0xFF20314A),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = text,
                                color = if (colors.isDark) Color(0xFFA0A5B0) else Color(0xFF20314A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.01).em,
                                lineHeight = 14.sp,
                                style = ProfileTightTextStyle
                            )
                        }
                    }
                }
            }
        }

        // 4. Sleek Website Link Chip & Social Handles (Fixing lone icon into cohesive chip)
        val socialLinksMap = profile?.socialLinks ?: emptyMap()
        val ghUrl = socialLinksMap["github"]?.trim()?.takeIf { it.isNotBlank() }
        val igUrl = socialLinksMap["instagram"]?.trim()?.takeIf { it.isNotBlank() }
        val webUrl = socialLinksMap["website"]?.trim()?.takeIf { it.isNotBlank() }

        val hasSocials = ghUrl != null || igUrl != null || webUrl != null
        if (hasSocials) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Website Link Chip
                if (webUrl != null) {
                    val displayDomain = webUrl
                        .removePrefix("https://")
                        .removePrefix("http://")
                        .removePrefix("www.")
                        .removeSuffix("/")

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (colors.isDark) Color(0xFF0381FE).copy(alpha = 0.15f) else Color(0xFFEFF5FF))
                            .clickable {
                                try {
                                    val target = if (webUrl.startsWith("http://") || webUrl.startsWith("https://")) webUrl else "https://$webUrl"
                                    com.chroma.studio.ui.components.InAppBrowserManager.openUrl(target)
                                } catch (_: Exception) {
                                    ToastManager.showToast("Could not open link")
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Link2,
                                contentDescription = "Website",
                                tint = Color(0xFF0062FF),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = displayDomain,
                                color = Color(0xFF0062FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.01).em,
                                lineHeight = 14.sp,
                                style = ProfileTightTextStyle
                            )
                            Icon(
                                imageVector = Lucide.ArrowUpRight,
                                contentDescription = null,
                                tint = Color(0xFF0062FF).copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // GitHub Chip
                if (ghUrl != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (colors.isDark) Color(0xFF1E222A) else Color(0xFFEFF4F9))
                            .clickable {
                                try {
                                    val target = if (ghUrl.startsWith("http://") || ghUrl.startsWith("https://")) ghUrl else "https://$ghUrl"
                                    com.chroma.studio.ui.components.InAppBrowserManager.openUrl(target)
                                } catch (_: Exception) {
                                    ToastManager.showToast("Could not open GitHub")
                                }
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_social_github),
                                contentDescription = "GitHub",
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "GitHub",
                                color = if (colors.isDark) Color(0xFFA0A5B0) else Color(0xFF20314A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.sp,
                                style = ProfileTightTextStyle
                            )
                        }
                    }
                }

                // Instagram Chip
                if (igUrl != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (colors.isDark) Color(0xFF1E222A) else Color(0xFFEFF4F9))
                            .clickable {
                                try {
                                    val target = if (igUrl.startsWith("http://") || igUrl.startsWith("https://")) igUrl else "https://$igUrl"
                                    com.chroma.studio.ui.components.InAppBrowserManager.openUrl(target)
                                } catch (_: Exception) {
                                    ToastManager.showToast("Could not open Instagram")
                                }
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_social_instagram),
                                contentDescription = "Instagram",
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Instagram",
                                color = if (colors.isDark) Color(0xFFA0A5B0) else Color(0xFF20314A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.sp,
                                style = ProfileTightTextStyle
                            )
                        }
                    }
                }
            }
        }

        // 5. Bio Text with smooth Read more / Show less & clickable @mentions
        val bioText = profile?.bio?.trim() ?: ""
        if (bioText.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            val isBioLong = remember(bioText) {
                bioText.length > 75 || bioText.contains("\n")
            }

            val displayedBio = remember(bioText, isBioExpanded, isBioLong) {
                buildAnnotatedString {
                    if (!isBioLong || isBioExpanded) {
                        val words = bioText.split(Regex("(?<=\\s)|(?=\\s)|\\n"))
                        for (word in words) {
                            if (word.startsWith("@") || word.startsWith("#")) {
                                withStyle(SpanStyle(color = Color(0xFF0062FF), fontWeight = FontWeight.SemiBold)) {
                                    append(word)
                                }
                            } else {
                                append(word)
                            }
                        }
                        if (isBioLong && isBioExpanded) {
                            withStyle(SpanStyle(color = Color(0xFF0062FF), fontWeight = FontWeight.Bold)) {
                                append(" Show less")
                            }
                        }
                    } else {
                        val firstNewline = bioText.indexOf('\n')
                        val cutIndex = when {
                            firstNewline in 1..75 -> firstNewline
                            bioText.length > 75 -> {
                                val spaceIdx = bioText.lastIndexOf(' ', 75)
                                if (spaceIdx > 40) spaceIdx else 75
                            }
                            else -> bioText.length
                        }
                        val preview = bioText.take(cutIndex).trimEnd()
                        val words = preview.split(Regex("(?<=\\s)|(?=\\s)|\\n"))
                        for (word in words) {
                            if (word.startsWith("@") || word.startsWith("#")) {
                                withStyle(SpanStyle(color = Color(0xFF0062FF), fontWeight = FontWeight.SemiBold)) {
                                    append(word)
                                }
                            } else {
                                append(word)
                            }
                        }
                        withStyle(SpanStyle(color = colors.textMuted)) {
                            append("... ")
                        }
                        withStyle(SpanStyle(color = Color(0xFF0062FF), fontWeight = FontWeight.Bold)) {
                            append("Read more")
                        }
                    }
                }
            }

            Text(
                text = displayedBio,
                color = colors.textMain,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                letterSpacing = (-0.01).em,
                style = TextStyle(
                    platformStyle = ProfileTightPlatformStyle,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None
                    )
                ),
                maxLines = if (isBioExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isBioLong) isBioExpanded = !isBioExpanded
                    }
            )
        }



        // 7. Mutual Connections Group
        if (mutualFollowers.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            val connectedUsers = remember(mutualFollowers) {
                mutualFollowers.map { it.toConnectedUser() }
            }
            ConnectedUsersAvatarGroup(
                users = connectedUsers,
                totalCount = mutualFollowers.size,
                maxVisible = 4,
                avatarSize = 26.dp,
                overlapOffset = 7.dp,
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "profileBtnScale"
    )

    val shape = RoundedCornerShape(999.dp)
    val background = if (isPrimary) {
        if (isPressed) Color(0xFF0056D6) else Color(0xFF0062FF)
    } else {
        if (isPressed) {
            if (colors.isDark) Color(0xFF262A34) else Color(0xFFDFE7FD)
        } else {
            if (colors.isDark) Color(0xFF1E222A) else Color(0xFFECF1FE)
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(38.dp)
            .clip(shape)
            .background(background)
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
    isFollowRequested: Boolean = false
) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(6.dp))

        // Dedicated Designed Status Section
        UserProfileStatusSection(
            profile = profile,
            isCurrentUser = isCurrentUser,
            userId = userId
        )
        Spacer(Modifier.height(12.dp))

        // Action Buttons Row (Apple iOS Squircle Buttons, 12dp radius, border-less!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCurrentUser) {
                val secondaryBtnColor = if (colors.isDark) Color(0xFF3892F6) else Color(0xFF0062FF)
                ProfileActionButton(
                    onClick = onNavigateToEditProfile,
                    isPrimary = true,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Lucide.Pencil,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Edit Profile",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        letterSpacing = (-0.01).em,
                        lineHeight = 15.sp,
                        style = ProfileTightTextStyle
                    )
                }

                ProfileActionButton(
                    onClick = onShowShareCard,
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Lucide.Share2,
                        contentDescription = null,
                        tint = secondaryBtnColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Share Card",
                        color = secondaryBtnColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        letterSpacing = (-0.01).em,
                        lineHeight = 15.sp,
                        style = ProfileTightTextStyle
                    )
                }
            } else {
                if (isFollowing) {
                    ProfileActionButton(
                        onClick = onToggleFollow,
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = null,
                            tint = colors.textMain,
                            modifier = Modifier.size(14.5.dp)
                        )
                        Text(
                            text = "Following",
                            color = colors.textMain,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.01).em,
                            lineHeight = 15.5.sp,
                            style = ProfileTightTextStyle
                        )
                    }
                } else if (isFollowRequested) {
                    ProfileActionButton(
                        onClick = onToggleFollow,
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Lucide.Clock,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(14.5.dp)
                        )
                        Text(
                            text = "Requested",
                            color = colors.textMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.01).em,
                            lineHeight = 15.5.sp,
                            style = ProfileTightTextStyle
                        )
                    }
                } else {
                    ProfileActionButton(
                        onClick = onToggleFollow,
                        isPrimary = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Lucide.UserPlus,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.5.dp)
                        )
                        Text(
                            text = "Follow",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.01).em,
                            lineHeight = 15.5.sp,
                            style = ProfileTightTextStyle
                        )
                    }
                }

                ProfileActionButton(
                    onClick = { onNavigateToChat?.invoke(userId) },
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Lucide.MessageSquare,
                        contentDescription = null,
                        tint = colors.textMain,
                        modifier = Modifier.size(14.5.dp)
                    )
                    Text(
                        text = "Message",
                        color = colors.textMain,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        letterSpacing = (-0.01).em,
                        lineHeight = 15.5.sp,
                        style = ProfileTightTextStyle
                    )
                }

                // One UI 7.0 Share Icon Button
                val shareInteraction = remember { MutableInteractionSource() }
                val isSharePressed by shareInteraction.collectIsPressedAsState()
                val shareScale by animateFloatAsState(
                    targetValue = if (isSharePressed) 0.94f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "shareScale"
                )

                Box(
                    modifier = Modifier
                        .scale(shareScale)
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (colors.isDark) Color(0xFF1E222A) else Color(0xFFEFF2F6))
                        .clickable(
                            interactionSource = shareInteraction,
                            indication = null,
                            onClick = {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                onShowShareCard()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Share2,
                        contentDescription = "Share",
                        tint = colors.textMain,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Saved Story Highlights Showcase (Instagram-style)
        UserProfileHighlightsSection(
            userId = userId,
            isCurrentUser = isCurrentUser,
            onHighlightClick = { hl ->
                if (onMyStoryClick != null && isCurrentUser) {
                    onMyStoryClick()
                } else if (onStoryClick != null && filteredStories.isNotEmpty()) {
                    onStoryClick(0)
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        // Seamless Tab Switcher (Works, Posts, Flash Videos, Remixes, Saved)
        ProfileSeamlessTabBar(
            icons = profileTabIcons,
            selectedIndex = selectedTabIndex,
            onItemSelected = {
                com.chroma.studio.chat.sound.SoundManager.playReaction()
                onTabSelected(it)
            },
            selectedColor = Color(0xFF0062FF),
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Spacer(Modifier.height(4.dp))
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
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 2.dp,
                                    brush = Brush.sweepGradient(AppleStatusVibrantGradient),
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (hasActiveStories) Brush.sweepGradient(AppleStatusVibrantGradient)
                                else Brush.linearGradient(
                                    listOf(
                                        if (colors.isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6),
                                        if (colors.isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6)
                                    )
                                )
                            ),
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
                                .background(Brush.linearGradient(listOf(Color(0xFFFF2D55), Color(0xFFAF52DE))))
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
