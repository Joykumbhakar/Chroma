package com.chroma.studio.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun UserProfileHeroSection(
    profile: UserProfile?,
    hasActiveStories: Boolean,
    worksCount: Int,
    isRestricted: Boolean,
    userId: String,
    onAvatarClick: () -> Unit,
    onNavigateToUserList: ((String, String) -> Unit)?,
    onPostsClick: (() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val heroHazeState = remember { HazeState() }
    val presetBannerRes = remember(profile?.bannerUrl) { getBannerPresetDrawableRes(profile?.bannerUrl) }

    Box(modifier = Modifier.fillMaxWidth()) {
        // 1. Cinematic Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .haze(heroHazeState)
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
                .padding(top = 74.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elevated Apple Glass Avatar
            Box(
                modifier = Modifier.offset(y = 10.dp),
                contentAlignment = Alignment.Center
            ) {
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

            // Top Stats beside Avatar in Pure Blur Apple Glass Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .hazeChild(
                        state = heroHazeState,
                        shape = RoundedCornerShape(18.dp),
                        style = HazeDefaults.style(
                            tint = Color.Transparent,
                            blurRadius = 24.dp,
                            noiseFactor = 0f
                        )
                    )
                    .border(
                        0.75.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Posts Count
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.5.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onPostsClick?.invoke()
                            }
                    ) {
                        Text(
                            text = formatCount(worksCount),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.02).em,
                            style = TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.35f),
                                    blurRadius = 4f
                                )
                            )
                        )
                        Text(
                            text = "Posts",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            style = TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.30f),
                                    blurRadius = 3f
                                )
                            )
                        )
                    }

                    // Delicate Glass Vertical Hairline
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(0.5.dp)
                            .background(Color.White.copy(alpha = 0.25f))
                    )

                    // Followers Count
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.5.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isRestricted) ToastManager.showToast("This account is private.")
                                else onNavigateToUserList?.invoke(userId, "followers")
                            }
                    ) {
                        Text(
                            text = formatCount(profile?.followersCount ?: 0),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.02).em,
                            style = TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.35f),
                                    blurRadius = 4f
                                )
                            )
                        )
                        Text(
                            text = "Followers",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            style = TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.30f),
                                    blurRadius = 3f
                                )
                            )
                        )
                    }

                    // Delicate Glass Vertical Hairline
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(0.5.dp)
                            .background(Color.White.copy(alpha = 0.25f))
                    )

                    // Following Count
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.5.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isRestricted) ToastManager.showToast("This account is private.")
                                else onNavigateToUserList?.invoke(userId, "following")
                            }
                    ) {
                        Text(
                            text = formatCount(profile?.followingCount ?: 0),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.02).em,
                            style = TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.35f),
                                    blurRadius = 4f
                                )
                            )
                        )
                        Text(
                            text = "Following",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            style = TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.30f),
                                    blurRadius = 3f
                                )
                            )
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

        Spacer(Modifier.height(14.dp))

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
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).em,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (profile?.verified == true) {
                    Spacer(Modifier.width(5.dp))
                    VerifiedBadge(size = 17.dp, showGlow = true)
                }
            }

            // Quick QR / Pass Apple Glass Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (colors.isDark) Color(0x1A007AFF) else Color(0x10007AFF))
                    .border(
                        0.75.dp,
                        Brush.linearGradient(listOf(Color(0x60007AFF), Color(0x20007AFF))),
                        CircleShape
                    )
                    .clickable { onShowShareCard() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Lucide.QrCode,
                    contentDescription = "Show Pass",
                    tint = LiquidGlassTokens.AppleSystemBlue,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // 2. Username
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
                .padding(vertical = 1.dp)
        ) {
            Text(
                text = "@${profile?.username ?: "unknown"}",
                color = Color(0xFF8E8E93),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Lucide.Copy,
                contentDescription = "Copy username",
                tint = Color(0xFF8E8E93).copy(alpha = 0.6f),
                modifier = Modifier.size(11.dp)
            )
        }

        // 3. Role Glass Badge
        val roleText = profile?.role
        if (!roleText.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(9999.dp),
                color = if (colors.isDark) Color(0x1A007AFF) else Color(0x0F007AFF),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                ) {
                    AiWaveSphereIcon(modifier = Modifier.size(13.dp))
                    Text(
                        text = roleText,
                        style = TextStyle(
                            brush = roleAnimatedBrush,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.01).em
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // 4. Bio Text with ....Read more
        val bioText = profile?.bio?.trim() ?: ""
        if (bioText.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            val isBioLong = remember(bioText) {
                bioText.length > 75 || bioText.count { it == '\n' } >= 1
            }

            val displayedBio = remember(bioText, isBioExpanded, isBioLong) {
                buildAnnotatedString {
                    if (!isBioLong || isBioExpanded) {
                        // Full text with formatted @mentions and #tags
                        val words = bioText.split(Regex("(?<=\\s)|(?=\\s)|\\n"))
                        for (word in words) {
                            if (word.startsWith("@") || word.startsWith("#")) {
                                withStyle(SpanStyle(color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)) {
                                    append(word)
                                }
                            } else {
                                append(word)
                            }
                        }
                        if (isBioLong && isBioExpanded) {
                            withStyle(SpanStyle(color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)) {
                                append(" Show less")
                            }
                        }
                    } else {
                        // Truncated preview for ....Read more
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
                                withStyle(SpanStyle(color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)) {
                                    append(word)
                                }
                            } else {
                                append(word)
                            }
                        }
                        withStyle(SpanStyle(color = Color(0xFF8E8E93))) {
                            append("....")
                        }
                        withStyle(SpanStyle(color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)) {
                            append("Read more")
                        }
                    }
                }
            }

            Text(
                text = displayedBio,
                color = colors.textMain,
                fontSize = 13.sp,
                lineHeight = 17.5.sp,
                maxLines = if (isBioExpanded) Int.MAX_VALUE else 2,
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

    // 5. Metadata Matrix Chips (Replaces grocery list)
    val metadataItems = remember(locationVisible, office, home, country, dobVisible, genderVisible, profile) {
        val list = mutableListOf<Triple<ImageVector, String, Color>>()
        if (locationVisible && !office.isNullOrBlank()) {
            list.add(Triple(Lucide.Briefcase, office, Color(0xFF007AFF)))
        }
        if (locationVisible && !home.isNullOrBlank()) {
            list.add(Triple(Lucide.House, home, Color(0xFF34C759)))
        }
        if (locationVisible && !country.isNullOrBlank()) {
            list.add(Triple(Lucide.Globe, country, Color(0xFF5856D6)))
        }
        if (dobVisible) {
            val dobStr = profile?.dateOfBirthAsLong?.let {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
            }
            if (!dobStr.isNullOrBlank()) {
                list.add(Triple(Lucide.Calendar, "Born $dobStr", Color(0xFFFF9500)))
            }
        }
        if (genderVisible && !profile?.gender.isNullOrBlank()) {
            list.add(Triple(Lucide.User, profile!!.gender, Color(0xFFAF52DE)))
        }
        list
    }

    if (metadataItems.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            metadataItems.forEach { (icon, text, tintColor) ->
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = if (colors.isDark) Color(0x18FFFFFF) else Color(0x0A000000)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.5.dp),
                        modifier = Modifier.padding(horizontal = 8.5.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tintColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(11.5.dp)
                        )
                        Text(
                            text = text,
                            color = colors.textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.01).em
                        )
                    }
                }
            }
        }
    }

    // 6. Social Links Uiverse Squircle Dock (Clean backgroundless row)
    val profileLinks = profile?.socialLinks ?: emptyMap()
    if (profileLinks.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            profileLinks.forEach { (platform, url) ->
                ProfileSquircleSocialButton(
                    platform = platform,
                    url = url,
                    onClick = {
                        try {
                            val targetUri = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUri)))
                        } catch (_: Exception) {
                            ToastManager.showToast("Could not open link")
                        }
                    }
                )
            }
        }
    }

    // 7. Content Tags (Interests with Expand/Collapse)
    val hasContentTags = profile?.contentTags?.isNotEmpty() == true
    if (hasContentTags) {
        Spacer(Modifier.height(6.dp))
        var areTagsExpanded by remember { mutableStateOf(false) }
        val allTags = profile!!.contentTags
        val visibleTags = if (areTagsExpanded || allTags.size <= 4) allTags else allTags.take(4)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            visibleTags.forEach { rawTag ->
                val tag = if (rawTag.startsWith("#")) rawTag else "#$rawTag"
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = if (colors.isDark) Color(0x12FFFFFF) else Color(0x06000000)
                ) {
                    Text(
                        text = tag,
                        color = Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (allTags.size > 4) {
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = Color(0x12007AFF),
                    modifier = Modifier.clickable { areTagsExpanded = !areTagsExpanded }
                ) {
                    Text(
                        text = if (areTagsExpanded) "Show less" else "+${allTags.size - 4} more",
                        color = Color(0xFF007AFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }

    // Mutual Connections
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

        // Action Buttons Row (Apple Liquid Glass Buttons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCurrentUser) {
                LiquidGlassPrimaryButton(
                    onClick = onNavigateToEditProfile,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
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
                        letterSpacing = (-0.01).em
                    )
                }

                LiquidGlassSecondaryButton(
                    onClick = onShowShareCard,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Icon(
                        imageVector = Lucide.QrCode,
                        contentDescription = null,
                        tint = if (colors.isDark) Color.White else Color.Black,
                        modifier = Modifier.size(14.5.dp)
                    )
                    Text(
                        text = "Share Card",
                        color = if (colors.isDark) Color.White else Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        letterSpacing = (-0.01).em
                    )
                }
            } else {
                if (isFollowing) {
                    LiquidGlassSecondaryButton(
                        onClick = onToggleFollow,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = null,
                            tint = if (colors.isDark) Color.White else Color.Black,
                            modifier = Modifier.size(14.5.dp)
                        )
                        Text(
                            text = "Following",
                            color = if (colors.isDark) Color.White else Color.Black,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.01).em
                        )
                    }
                } else {
                    LiquidGlassPrimaryButton(
                        onClick = onToggleFollow,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
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
                            letterSpacing = (-0.01).em
                        )
                    }
                }

                LiquidGlassSecondaryButton(
                    onClick = { onNavigateToChat?.invoke(userId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Icon(
                        imageVector = Lucide.MessageSquare,
                        contentDescription = null,
                        tint = if (colors.isDark) Color.White else Color.Black,
                        modifier = Modifier.size(14.5.dp)
                    )
                    Text(
                        text = "Message",
                        color = if (colors.isDark) Color.White else Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        letterSpacing = (-0.01).em
                    )
                }

                // Apple Glass Share Icon Pill
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (colors.isDark) Color(0x18FFFFFF) else Color(0xFFF1F3F5))
                        .border(
                            1.dp,
                            if (colors.isDark) Brush.verticalGradient(listOf(Color(0x30FFFFFF), Color(0x10FFFFFF)))
                            else LiquidGlassTokens.SurfaceSpecularGlassBorder,
                            CircleShape
                        )
                        .border(0.5.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                        .clickable { onShowShareCard() },
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
