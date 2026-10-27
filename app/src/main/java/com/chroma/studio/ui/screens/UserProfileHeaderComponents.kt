package com.chroma.studio.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.AiWaveSphereIcon
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.R
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.UserStoryGroup
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.StoryViewModel
import com.composables.icons.lucide.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserProfileHeroSection(
    profile: UserProfile?,
    hasActiveStories: Boolean,
    worksCount: Int,
    isRestricted: Boolean,
    userId: String,
    onAvatarClick: () -> Unit,
    onNavigateToUserList: ((String, String) -> Unit)?
) {
    val colors = LocalOscellionColors.current
    val presetBannerRes = remember(profile?.bannerUrl) { getBannerPresetDrawableRes(profile?.bannerUrl) }

    Box(modifier = Modifier.fillMaxWidth()) {
        // 1. Cinematic Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF007AFF), Color(0xFF0A84FF), Color(0xFF32ADE6))
                    )
                )
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
        }

        // 2. Avatar & Top Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Elevated Apple Glass Avatar
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .blur(14.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x55007AFF), Color(0x220A84FF), Color.Transparent)
                            ),
                            CircleShape
                        )
                )

                Box(
                    modifier = Modifier.size(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(10.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.20f))
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(
                                width = if (hasActiveStories) 2.5.dp else 1.5.dp,
                                brush = if (hasActiveStories) {
                                    Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF00C6FF)))
                                } else {
                                    Brush.linearGradient(
                                        if (colors.isDark) listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))
                                        else listOf(Color(0xFFE5E5EA), Color(0xFFF2F2F7))
                                    )
                                },
                                shape = CircleShape
                            )
                            .clickable { onAvatarClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        val presetRes = remember(profile?.avatarUrl) { getAvatarPresetDrawableRes(profile?.avatarUrl) }
                        val defaultAvatarRes = remember(profile?.gender) { getDefaultAvatarDrawableRes(profile?.gender) }
                        val resolvedDrawable = presetRes ?: if (profile?.avatarUrl.isNullOrBlank()) defaultAvatarRes else null

                        if (resolvedDrawable != null) {
                            Image(
                                painter = painterResource(id = resolvedDrawable),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (!profile?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profile?.avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.default_avatar_apple),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    if (profile?.online == true && profile?.showOnlineStatus == true) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = (-2).dp, y = (-2).dp)
                                .background(if (colors.isDark) Color(0xFF16161A) else Color.White, CircleShape)
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759))
                        )
                    }
                }
            }

            // Top Stats beside Avatar
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 50.dp, start = 10.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Posts Count
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = formatCount(worksCount),
                        color = colors.textMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Posts",
                        color = Color(0xFF8E8E93),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Followers Count
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isRestricted) ToastManager.showToast("This account is private.")
                        else onNavigateToUserList?.invoke(userId, "followers")
                    }
                ) {
                    Text(
                        text = formatCount(profile?.followersCount ?: 0),
                        color = colors.textMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Followers",
                        color = Color(0xFF8E8E93),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Following Count
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isRestricted) ToastManager.showToast("This account is private.")
                        else onNavigateToUserList?.invoke(userId, "following")
                    }
                ) {
                    Text(
                        text = formatCount(profile?.followingCount ?: 0),
                        color = colors.textMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Following",
                        color = Color(0xFF8E8E93),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
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
    onNavigateToUserList: ((String, String) -> Unit)?
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
        val roleTransition = rememberInfiniteTransition(label = "profile_role_gradient")
        val roleGradientShift by roleTransition.animateFloat(
            initialValue = 0f,
            targetValue = 600f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "role_gradient_shift"
        )

        val roleAnimatedBrush = remember(roleGradientShift) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF007AFF),
                    Color(0xFF00C6FF),
                    Color(0xFF34C759),
                    Color(0xFF007AFF)
                ),
                start = Offset(roleGradientShift, 0f),
                end = Offset(roleGradientShift + 250f, 0f)
            )
        }

        Spacer(Modifier.height(2.dp))

        // 1. Display Name + Verified Badge + Pass Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = profile?.displayName ?: "Unknown",
                    color = colors.textMain,
                    fontSize = 18.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (profile?.verified == true) {
                    Spacer(Modifier.width(4.dp))
                    VerifiedBadge(size = 17.dp)
                }
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.glassBgHover.copy(alpha = 0.6f))
                    .border(0.75.dp, colors.glassBorder, RoundedCornerShape(6.dp))
                    .clickable { onShowShareCard() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Lucide.QrCode, contentDescription = "Show Pass", tint = Color(0xFF007AFF), modifier = Modifier.size(14.dp))
            }
        }

        // 2. Username
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.5.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("username", "@${profile?.username ?: ""}")
                    clipboard.setPrimaryClip(clip)
                    ToastManager.showToast("Username copied to clipboard")
                }
        ) {
            Text(
                text = "@${profile?.username ?: "unknown"}",
                color = Color(0xFF8E8E93),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Lucide.Copy,
                contentDescription = "Copy username",
                tint = Color(0xFF8E8E93).copy(alpha = 0.5f),
                modifier = Modifier.size(10.5.dp)
            )
        }

        // 3. Role
        val roleText = profile?.role
        if (!roleText.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp, bottom = 1.dp)
            ) {
                AiWaveSphereIcon(
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = roleText,
                    style = TextStyle(
                        brush = roleAnimatedBrush,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // 4. Bio Text
        val bioText = profile?.bio ?: ""
        if (bioText.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            var cutText by remember(bioText) { mutableStateOf<String?>(null) }

            val fullAnnotatedBio = remember(bioText) {
                buildAnnotatedString {
                    val words = bioText.split(Regex("(?<=\\s)|(?=\\s)|\\n"))
                    for (word in words) {
                        if (word.startsWith("@") || word.startsWith("#")) {
                            withStyle(style = SpanStyle(color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)) {
                                append(word)
                            }
                        } else {
                            append(word)
                        }
                    }
                }
            }

            val displayedBio = remember(isBioExpanded, cutText, fullAnnotatedBio) {
                if (isBioExpanded) {
                    buildAnnotatedString {
                        append(fullAnnotatedBio)
                        if (cutText != null) {
                            withStyle(style = SpanStyle(color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)) {
                                append(" less")
                            }
                        }
                    }
                } else if (cutText != null) {
                    buildAnnotatedString {
                        val words = cutText!!.split(Regex("(?<=\\s)|(?=\\s)|\\n"))
                        for (word in words) {
                            if (word.startsWith("@") || word.startsWith("#")) {
                                withStyle(style = SpanStyle(color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)) {
                                append(word)
                            }
                        } else {
                            append(word)
                        }
                    }
                    withStyle(style = SpanStyle(color = Color(0xFF8E8E93))) {
                        append("... ")
                    }
                    withStyle(style = SpanStyle(color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)) {
                        append("more")
                    }
                }
            } else {
                fullAnnotatedBio
            }
        }

        Text(
            text = displayedBio,
            color = colors.textMain,
            fontSize = 13.sp,
            lineHeight = 17.5.sp,
            maxLines = if (isBioExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Clip,
            onTextLayout = { textLayoutResult ->
                if (!isBioExpanded && cutText == null && textLayoutResult.hasVisualOverflow) {
                    val lastLineIndex = 2.coerceAtMost(textLayoutResult.lineCount - 1)
                    val lastCharIndex = textLayoutResult.getLineEnd(lastLineIndex, visibleEnd = true)
                    val adjustedCut = (lastCharIndex - 9).coerceAtLeast(0)
                    if (adjustedCut < bioText.length) {
                        cutText = bioText.substring(0, adjustedCut).trimEnd()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (cutText != null) isBioExpanded = !isBioExpanded
                }
        )
    }

    // Metadata details (Office, Home, Country, DOB, Gender)
    val hasDetails = (locationVisible && (!office.isNullOrBlank() || !home.isNullOrBlank() || !country.isNullOrBlank())) ||
            dobVisible || genderVisible

    if (hasDetails) {
        Spacer(Modifier.height(4.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (locationVisible && !office.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.5.dp)) {
                    Icon(Lucide.Briefcase, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(12.5.dp))
                    Text(office, color = Color(0xFF8E8E93), fontSize = 12.sp)
                }
            }
            if (locationVisible && !home.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.5.dp)) {
                    Icon(Lucide.House, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(12.5.dp))
                    Text(home, color = Color(0xFF8E8E93), fontSize = 12.sp)
                }
            }
            if (locationVisible && !country.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.5.dp)) {
                    Icon(Lucide.Globe, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(12.5.dp))
                    Text(country, color = Color(0xFF8E8E93), fontSize = 12.sp)
                }
            }
            if (dobVisible) {
                val dobStr = profile?.dateOfBirthAsLong?.let {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
                } ?: ""
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.5.dp)) {
                    Icon(Lucide.Calendar, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(12.5.dp))
                    Text("Born $dobStr", color = Color(0xFF8E8E93), fontSize = 12.sp)
                }
            }
            if (genderVisible) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.5.dp)) {
                    Icon(Lucide.User, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(12.5.dp))
                    Text(profile?.gender ?: "", color = Color(0xFF8E8E93), fontSize = 12.sp)
                }
            }
        }
    }

    // Social Links & Content Tags
    val profileLinks = profile?.socialLinks ?: emptyMap()
    val hasContentTags = profile?.contentTags?.isNotEmpty() == true

    if (profileLinks.isNotEmpty() || hasContentTags) {
        Spacer(Modifier.height(5.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            profileLinks.forEach { (platform, url) ->
                val icon = when (platform.lowercase()) {
                    "github", "githubacname" -> Lucide.Github
                    "portfolio", "website" -> Lucide.Globe
                    "twitter", "x" -> Lucide.Twitter
                    "linkedin" -> Lucide.Linkedin
                    "instagram" -> Lucide.Instagram
                    else -> Lucide.Link
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.5.dp),
                    modifier = Modifier.clickable {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: Exception) {}
                    }
                ) {
                    Icon(icon, contentDescription = platform, tint = Color(0xFF007AFF), modifier = Modifier.size(12.dp))
                    Text(
                        platform,
                        color = Color(0xFF007AFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (hasContentTags) {
                profile!!.contentTags.forEach { tag ->
                    Text(
                        text = if (tag.startsWith("#")) tag else "#$tag",
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Mutual Connections
    if (mutualFollowers.isNotEmpty()) {
        Spacer(Modifier.height(5.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onNavigateToUserList?.invoke(userId, "followers") }
        ) {
            Text("Followed by ", color = Color(0xFF8E8E93), fontSize = 11.5.sp)
            Row(horizontalArrangement = Arrangement.spacedBy((-5).dp)) {
                mutualFollowers.take(3).forEach { mutual ->
                    if (mutual.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(mutual.avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(17.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.default_avatar_apple),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(17.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                        )
                    }
                }
            }
            Spacer(Modifier.width(5.dp))
            val names = mutualFollowers.take(2).joinToString(", ") { it.username } + if (mutualFollowers.size > 2) " and ${mutualFollowers.size - 2} others" else ""
            Text(
                names,
                color = colors.textMain,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
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
    storyVm: StoryViewModel
) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(6.dp))

        // Action Buttons Row (Gradient Pill Buttons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isCurrentUser) {
                ChromaPillButton(
                    onClick = { onNavigateToEditProfile() },
                    variant = ChromaPillVariant.Blue,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(Lucide.Pencil, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                ChromaPillButton(
                    onClick = { onShowShareCard() },
                    variant = ChromaPillVariant.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(Lucide.QrCode, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share Card", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                if (isFollowing) {
                    ChromaPillButton(
                        onClick = { onToggleFollow() },
                        variant = ChromaPillVariant.Secondary,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(Lucide.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Following", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    ChromaPillButton(
                        onClick = { onToggleFollow() },
                        variant = ChromaPillVariant.Blue,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Icon(Lucide.UserPlus, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Follow", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                ChromaPillButton(
                    onClick = {
                        onNavigateToChat?.invoke(userId)
                    },
                    variant = ChromaPillVariant.Cyan,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(Lucide.MessageSquare, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Message", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                IconButton(
                    onClick = { onShowShareCard() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Lucide.Share2, contentDescription = "Share", tint = colors.textMain, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Stories Section
        LaunchedEffect(userId, isCurrentUser) {
            if (!isCurrentUser) {
                storyVm.loadUserStories(userId)
            }
        }
        if (isCurrentUser || filteredStories.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            StoriesSection(
                stories = filteredStories,
                isLoading = false,
                isCurrentUser = isCurrentUser,
                hasMyStory = hasMyStory,
                myStoryAvatarUrl = profile?.avatarUrl ?: "",
                myStoryGender = profile?.gender,
                onMyStoryClick = onMyStoryClick,
                onAddStoryClick = onAddStoryClick,
                onStoryClick = onStoryClick,
                contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 2.dp, bottom = 4.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        // Modern Instagram-Style Seamless Flat Tab Switcher (Works, Photos, Flash Videos, Remixes)
        ProfileSeamlessTabBar(
            icons = profileTabIcons,
            selectedIndex = selectedTabIndex,
            onItemSelected = { onTabSelected(it) },
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Spacer(Modifier.height(4.dp))
    }
}
