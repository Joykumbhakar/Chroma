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
import androidx.compose.ui.platform.LocalContext
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
    isOnlyFriendStory: Boolean = false
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val haptic = LocalHapticFeedback.current
    val presetBannerRes = remember(profile?.bannerUrl) { getBannerPresetDrawableRes(profile?.bannerUrl) }
    val appleBlue = AppleProfileTokens.accentBlue(isDark)
    val hairline = AppleProfileTokens.hairlineBorder(isDark)

    // Real-time online presence matching the chat / message screen
    val presence = com.chroma.studio.data.OnlineStatusManager.rememberUserPresence(userId)
    val isUserOnline = presence.online && (profile?.showOnlineStatus != false)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        // 1. Clean Edge-to-Edge Apple Profile Cover Banner (preserves Oscellion artwork)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(154.dp)
                .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
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

            // Delicate top scrim for translucent nav controls & subtle bottom edge hairline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(hairline)
            )
        }

        // 2. Natural Overlap Avatar + Below-Banner Compact Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 110.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val avatarInteractionSource = remember { MutableInteractionSource() }
            val isAvatarPressed by avatarInteractionSource.collectIsPressedAsState()
            val avatarScale by animateFloatAsState(
                targetValue = if (isAvatarPressed) 0.96f else 1f,
                animationSpec = tween(140, easing = FastOutSlowInEasing),
                label = "avatarScale"
            )

            // 88dp total container -> 44dp over cover banner, 44dp below cover seam
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .scale(avatarScale),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
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
                    val shouldShowRing = hasActiveStories || isUploading || isUploadFailed
                    val ringBrush = when {
                        isUploadFailed -> Brush.linearGradient(listOf(Color(0xFFFF3B30), Color(0xFFFF453A)))
                        isUploading -> Brush.sweepGradient(
                            listOf(
                                appleBlue,
                                Color(0xFF38BDF8),
                                appleBlue
                            )
                        )
                        hasActiveStories -> getAppleStatusRingBrush(
                            hasStatus = true,
                            isSeen = false,
                            isDark = isDark,
                            isOnlyFriend = isOnlyFriendStory,
                            isVerified = profile?.verified == true
                        )
                        else -> null
                    }
                    val infiniteTransition = rememberInfiniteTransition(label = "profile_upload_ring")
                    val uploadRingRotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "profile_ring_rot"
                    )

                    if (shouldShowRing && ringBrush != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    if (isUploading) rotationZ = uploadRingRotation
                                }
                                .border(width = 2.4.dp, brush = ringBrush, shape = CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(colors.bg)
                        ) {
                            IosAvatarView(
                                name = profile?.displayName ?: profile?.username ?: "",
                                avatarUrl = profile?.avatarUrl.orEmpty(),
                                size = 72.dp,
                                gender = profile?.gender,
                                isOnline = false,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isUploading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else if (isUploadFailed) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFFF3B30).copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.ShieldAlert,
                                        contentDescription = "Failed",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Crisp theme cutout ring + subtle hairline outline (no excessive shadow)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(width = 3.5.dp, color = colors.bg, shape = CircleShape)
                                .padding(3.5.dp)
                                .clip(CircleShape)
                                .border(width = 0.75.dp, color = hairline, shape = CircleShape)
                        ) {
                            IosAvatarView(
                                name = profile?.displayName ?: profile?.username ?: "",
                                avatarUrl = profile?.avatarUrl.orEmpty(),
                                size = 77.dp,
                                gender = profile?.gender,
                                isOnline = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Purposeful, subtle Green Online Dot (no heavy glow/bounce)
                if (isUserOnline && (profile?.showOnlineStatus != false)) {
                    com.chroma.studio.ui.components.OnlineStatusDot(
                        isOnline = true,
                        size = 15.dp,
                        showPulse = false,
                        ringColor = colors.bg,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-3).dp, y = (-3).dp)
                    )
                }
            }

            // 3. Stats Row — Aligned below the banner seam alongside lower half of avatar
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statNumberColor = colors.textMain
                val statLabelColor = AppleProfileTokens.mutedText(isDark)

                // Posts
                val postsInteraction = remember { MutableInteractionSource() }
                val isPostsPressed by postsInteraction.collectIsPressedAsState()
                val postsScale by animateFloatAsState(
                    targetValue = if (isPostsPressed) 0.96f else 1f,
                    animationSpec = tween(120),
                    label = "postsScale"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .scale(postsScale)
                        .clickable(interactionSource = postsInteraction, indication = null) {
                            onPostsClick?.invoke()
                        }
                ) {
                    Text(
                        text = formatCount(worksCount),
                        color = statNumberColor,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.02).em,
                        lineHeight = 19.sp,
                        style = ProfileTightTextStyle.copy(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = "Posts",
                        color = statLabelColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 13.5.sp,
                        style = ProfileTightTextStyle
                    )
                }

                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width(0.5.dp)
                        .background(hairline)
                )

                // Followers
                val followersInteraction = remember { MutableInteractionSource() }
                val isFollowersPressed by followersInteraction.collectIsPressedAsState()
                val followersScale by animateFloatAsState(
                    targetValue = if (isFollowersPressed) 0.96f else 1f,
                    animationSpec = tween(120),
                    label = "followersScale"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .scale(followersScale)
                        .clickable(interactionSource = followersInteraction, indication = null) {
                            if (isRestricted) ToastManager.showToast("This account is private.")
                            else onNavigateToUserList?.invoke(userId, "followers")
                        }
                ) {
                    Text(
                        text = formatCount(profile?.followersCount ?: 0),
                        color = statNumberColor,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.02).em,
                        lineHeight = 19.sp,
                        style = ProfileTightTextStyle.copy(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = "Followers",
                        color = statLabelColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 13.5.sp,
                        style = ProfileTightTextStyle
                    )
                }

                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width(0.5.dp)
                        .background(hairline)
                )

                // Following
                val followingInteraction = remember { MutableInteractionSource() }
                val isFollowingPressed by followingInteraction.collectIsPressedAsState()
                val followingScale by animateFloatAsState(
                    targetValue = if (isFollowingPressed) 0.96f else 1f,
                    animationSpec = tween(120),
                    label = "followingScale"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .scale(followingScale)
                        .clickable(interactionSource = followingInteraction, indication = null) {
                            if (isRestricted) ToastManager.showToast("This account is private.")
                            else onNavigateToUserList?.invoke(userId, "following")
                        }
                ) {
                    Text(
                        text = formatCount(profile?.followingCount ?: 0),
                        color = statNumberColor,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.02).em,
                        lineHeight = 19.sp,
                        style = ProfileTightTextStyle.copy(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = "Following",
                        color = statLabelColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 13.5.sp,
                        style = ProfileTightTextStyle
                    )
                }
            }
        }
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
    onNavigateToProfile: ((String) -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appleBlue = AppleProfileTokens.accentBlue(isDark)
    val mutedText = AppleProfileTokens.mutedText(isDark)
    val hairline = AppleProfileTokens.hairlineBorder(isDark)
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
        // 1. Identity Block: Primary Name + Secondary @username + Copy + Inline Online Status + Quiet QR Pass
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile?.displayName ?: "Unknown",
                        color = colors.textMain,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.025).em,
                        lineHeight = 24.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ProfileTightTextStyle,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile?.verified == true) {
                        Spacer(Modifier.width(5.dp))
                        VerifiedBadge(
                            size = 17.dp,
                            tone = profile.verifiedTone,
                            showGlow = false
                        )
                    }
                }

                // Secondary Row: @username + copy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
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
                        color = mutedText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = ProfileTightTextStyle
                    )
                    Icon(
                        imageVector = Lucide.Copy,
                        contentDescription = "Copy username",
                        tint = mutedText.copy(alpha = 0.72f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Quiet iOS circular QR / Pass trigger with hairline border
            val qrInteraction = remember { MutableInteractionSource() }
            val isQrPressed by qrInteraction.collectIsPressedAsState()
            val qrScale by animateFloatAsState(
                targetValue = if (isQrPressed) 0.93f else 1f,
                animationSpec = tween(120),
                label = "qrScale"
            )
            Box(
                modifier = Modifier
                    .scale(qrScale)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AppleProfileTokens.secondaryFill(isDark))
                    .border(0.5.dp, hairline, CircleShape)
                    .clickable(interactionSource = qrInteraction, indication = null) {
                        com.chroma.studio.chat.sound.SoundManager.playReaction()
                        onShowShareCard()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Lucide.QrCode,
                    contentDescription = "Show Pass",
                    tint = colors.textMain,
                    modifier = Modifier.size(15.5.dp)
                )
            }
        }

        // 2. Profile Information: ONLY Role uses subtle pill background; Location, Year (20XX), & Gender Icon stay completely borderless/backgroundless
        val roleText = profile?.role?.trim()?.takeIf { it.isNotBlank() }
        val genderValue = profile?.gender?.trim()?.takeIf { it.isNotBlank() }
        val genderIcon: ImageVector? = remember(genderVisible, genderValue) {
            if (!genderVisible || genderValue.isNullOrBlank()) null
            else when (genderValue.lowercase()) {
                "male", "man", "boy", "m" -> Icons.Rounded.Male
                "female", "woman", "girl", "f" -> Icons.Rounded.Female
                "prefer not to say", "none", "hidden" -> null
                else -> Icons.Rounded.Transgender
            }
        }

        val metadataItems = remember(locationVisible, office, home, country, dobVisible, profile) {
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
                list.add(Lucide.Calendar to yr)
            }
            list
        }

        if (roleText != null || metadataItems.isNotEmpty() || genderIcon != null) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (roleText != null) {
                    val roleIcon = when (roleText.lowercase()) {
                        "admin", "superadmin", "founder", "ceo", "owner" -> Lucide.Crown
                        "developer" -> Lucide.Terminal
                        "designer", "web designer" -> Lucide.Palette
                        "content creator" -> Lucide.Video
                        "engineer" -> Lucide.Wrench
                        else -> Lucide.Briefcase
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .clip(RoundedCornerShape(8.dp))
                            .background(appleBlue.copy(alpha = 0.11f))
                            .border(0.5.dp, appleBlue.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.5.dp)
                    ) {
                        Icon(
                            imageVector = roleIcon,
                            contentDescription = null,
                            tint = appleBlue,
                            modifier = Modifier.size(12.5.dp)
                        )
                        Text(
                            text = roleText,
                            color = appleBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.01).em,
                            lineHeight = 14.sp,
                            style = ProfileTightTextStyle
                        )
                    }
                }

                metadataItems.forEach { (icon, text) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.5.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = mutedText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = text,
                            color = mutedText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = (-0.01).em,
                            lineHeight = 15.sp,
                            style = ProfileTightTextStyle
                        )
                    }
                }

                if (genderIcon != null) {
                    Icon(
                        imageVector = genderIcon,
                        contentDescription = genderValue,
                        tint = mutedText,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(15.dp)
                    )
                }
            }
        }

        // 3. Icon-Only Socials & Website Favicon Row (zero background containers)
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
        val faviconUrl = remember(normalizedWebUrl) {
            normalizedWebUrl?.let { url ->
                runCatching {
                    val host = Uri.parse(url).host?.trim()?.takeIf { it.isNotBlank() }
                    host?.let { "https://www.google.com/s2/favicons?domain=$it&sz=64" }
                }.getOrNull()
            }
        }
        var showWebsiteSheet by remember { mutableStateOf(false) }

        if (ghUrl != null || igUrl != null || normalizedWebUrl != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                if (normalizedWebUrl != null) {
                    var faviconLoaded by remember(faviconUrl) { mutableStateOf(false) }
                    var faviconError by remember(faviconUrl) { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable {
                                com.chroma.studio.chat.sound.SoundManager.playReaction()
                                showWebsiteSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!faviconLoaded || faviconError || faviconUrl == null) {
                            Icon(
                                imageVector = Lucide.Globe,
                                contentDescription = "Website",
                                tint = colors.textMain,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        if (faviconUrl != null && !faviconError) {
                            AsyncImage(
                                model = faviconUrl,
                                contentDescription = "Website",
                                contentScale = ContentScale.Crop,
                                onSuccess = { faviconLoaded = true },
                                onError = { faviconError = true },
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .graphicsLayer { alpha = if (faviconLoaded) 1f else 0f }
                            )
                        }
                    }
                }

                if (ghUrl != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable {
                                try {
                                    val target = when {
                                        ghUrl.startsWith("http://", true) || ghUrl.startsWith("https://", true) -> ghUrl
                                        ghUrl.contains(".") -> "https://$ghUrl"
                                        else -> "https://github.com/${ghUrl.removePrefix("@")}"
                                    }
                                    com.chroma.studio.ui.components.InAppBrowserManager.openUrl(target)
                                } catch (_: Exception) {
                                    ToastManager.showToast("Could not open GitHub")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_social_github),
                            contentDescription = "GitHub",
                            colorFilter = ColorFilter.tint(colors.textMain),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (igUrl != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable {
                                try {
                                    val target = when {
                                        igUrl.startsWith("http://", true) || igUrl.startsWith("https://", true) -> igUrl
                                        igUrl.contains(".") -> "https://$igUrl"
                                        else -> "https://instagram.com/${igUrl.removePrefix("@")}"
                                    }
                                    com.chroma.studio.ui.components.InAppBrowserManager.openUrl(target)
                                } catch (_: Exception) {
                                    ToastManager.showToast("Could not open Instagram")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_social_instagram),
                            contentDescription = "Instagram",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (showWebsiteSheet && normalizedWebUrl != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showWebsiteSheet = false },
                sheetState = sheetState,
                containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 6.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(colors.textMuted.copy(alpha = 0.28f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = normalizedWebUrl,
                        color = colors.textMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp,
                        style = ProfileTightTextStyle
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(appleBlue)
                            .clickable {
                                showWebsiteSheet = false
                                try {
                                    com.chroma.studio.ui.components.InAppBrowserManager.openUrl(normalizedWebUrl)
                                } catch (_: Exception) {
                                    ToastManager.showToast("Could not open link")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = websiteCta,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = ProfileTightTextStyle
                        )
                    }
                }
            }
        }

        // 4. Clean Editorial Biography with interactive @mentions and Read more / Show less
        val bioText = profile?.bio?.trim() ?: ""
        if (bioText.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            val isBioLong = remember(bioText) {
                bioText.length > 90 || bioText.contains("\n")
            }

            val displayedBio = remember(bioText, isBioExpanded, isBioLong, appleBlue, mutedText) {
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
                            withStyle(SpanStyle(color = appleBlue, fontWeight = FontWeight.SemiBold)) {
                                append(cleanHandle)
                            }
                            pop()
                            if (trailing.isNotEmpty()) append(trailing)
                        } else if (word.startsWith("#") && word.length > 1) {
                            withStyle(SpanStyle(color = appleBlue, fontWeight = FontWeight.Medium)) {
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
                        withStyle(SpanStyle(color = appleBlue, fontWeight = FontWeight.Medium)) {
                            append("Read more")
                        }
                        pop()
                    } else if (isBioLong && isBioExpanded) {
                        pushStringAnnotation(tag = "TOGGLE_BIO", annotation = "collapse")
                        withStyle(SpanStyle(color = appleBlue, fontWeight = FontWeight.Medium)) {
                            append("  Show less")
                        }
                        pop()
                    }
                }
            }

            var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

            Text(
                text = displayedBio,
                color = colors.textMain,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
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
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier
                    .fillMaxWidth()
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

        // 5. Mutual Connections Group
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
    val appleBlue = AppleProfileTokens.accentBlue(isDark)
    val hairline = AppleProfileTokens.hairlineBorder(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Compact Apple Activity / Music Status Indicator
            UserProfileStatusSection(
                profile = profile,
                isCurrentUser = isCurrentUser,
                userId = userId
            )

            Spacer(Modifier.height(12.dp))

            // Symmetric Apple HIG Action Buttons Row (40dp height, 12dp radius)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrentUser) {
                    ProfileActionButton(
                        onClick = onNavigateToEditProfile,
                        isPrimary = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Lucide.Pencil,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.5.dp)
                        )
                        Text(
                            text = "Edit Profile",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.01).em,
                            lineHeight = 16.sp,
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
                            tint = colors.textMain,
                            modifier = Modifier.size(14.5.dp)
                        )
                        Text(
                            text = "Share Card",
                            color = colors.textMain,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.01).em,
                            lineHeight = 16.sp,
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
                                lineHeight = 16.sp,
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
                                lineHeight = 16.sp,
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
                                lineHeight = 16.sp,
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
                            lineHeight = 16.sp,
                            style = ProfileTightTextStyle
                        )
                    }

                    val shareInteraction = remember { MutableInteractionSource() }
                    val isSharePressed by shareInteraction.collectIsPressedAsState()
                    val shareScale by animateFloatAsState(
                        targetValue = if (isSharePressed) 0.96f else 1f,
                        animationSpec = tween(120),
                        label = "shareScale"
                    )

                    Box(
                        modifier = Modifier
                            .scale(shareScale)
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppleProfileTokens.secondaryFill(isDark))
                            .border(0.5.dp, hairline, RoundedCornerShape(12.dp))
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
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

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

        Spacer(Modifier.height(12.dp))

        // Full-width Minimal Icon Profile Content Tab Switcher
        ProfileSeamlessTabBar(
            icons = profileTabIcons,
            selectedIndex = selectedTabIndex,
            onItemSelected = {
                com.chroma.studio.chat.sound.SoundManager.playReaction()
                onTabSelected(it)
            },
            selectedColor = appleBlue,
            unselectedColor = AppleProfileTokens.mutedText(isDark)
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
