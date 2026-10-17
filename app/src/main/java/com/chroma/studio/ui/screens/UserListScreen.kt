package com.chroma.studio.ui.screens
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.GlassButton
import com.chroma.studio.ui.components.LiquidGlassPrimaryButton
import com.chroma.studio.ui.components.LiquidGlassSecondaryButton
import androidx.compose.ui.composed
import com.airbnb.lottie.compose.animateLottieCompositionAsState


import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.model.UserProfile
import com.chroma.studio.viewmodel.DiscoverViewModel
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloat
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.chroma.studio.R
import com.chroma.studio.ui.components.AppleHeartHandshakeGraphic
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    userId: String,
    type: String, // "followers" or "following"
    discoverVm: DiscoverViewModel,
    onNavigateToUser: (String) -> Unit,
    onBack: () -> Unit
) {
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var currentUserFollowingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val colors = LocalOscellionColors.current

    LaunchedEffect(userId, type) {
        isLoading = true
        // Fetch the list of users to display
        val list = if (type == "followers") {
            discoverVm.repository.getFollowers(userId)
        } else {
            discoverVm.repository.getFollowing(userId)
        }
        users = list

        // Fetch current user's following list to show correct Follow/Following buttons
        if (currentUserId != null) {
            val followingList = discoverVm.repository.getFollowing(currentUserId)
            currentUserFollowingIds = followingList.map { it.uid }.toSet()
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            com.chroma.studio.ui.components.ChromaScreenHeader(
                title = "Oscellion",
                badgeText = if (type == "followers") "Followers" else "Following",
                showBackButton = true,
                onBack = onBack
            )
        },
        containerColor = colors.bg
    ) { padding ->
        if (isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(10) {
                    SkeletonUserListItem()
                }
            }
        } else {
            if (users.isEmpty()) {
                val emptyState = rememberAppleGraphicAnimationState()
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppleHeartHandshakeGraphic(
                            modifier = Modifier.size(130.dp),
                            state = emptyState,
                            enableClickToAnimate = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No Users Found", color = colors.textMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Try searching for another creator or username.", color = colors.textMuted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(users, key = { it.uid }) { user ->
                        val isFollowingUser = currentUserFollowingIds.contains(user.uid)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToUser(user.uid) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(colors.glassBgHover.copy(alpha=0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!user.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Lucide.User, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Name & Username
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.displayName,
                                        color = colors.textMain,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (user.verified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Image(
                                            painter = painterResource(id = R.drawable.verification_badge),
                                            contentDescription = "Verified",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "@${user.username}",
                                    color = colors.textMuted,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Action Button
                            if (currentUserId != null && currentUserId != user.uid) {
                                if (isFollowingUser) {
                                    LiquidGlassSecondaryButton(
                                        text = "Following",
                                        onClick = {
                                            scope.launch {
                                                val success = discoverVm.repository.toggleFollow(currentUserId, user.uid)
                                                if (success) {
                                                    currentUserFollowingIds = currentUserFollowingIds - user.uid
                                                }
                                            }
                                        },
                                        isSmall = true
                                    )
                                } else {
                                    LiquidGlassPrimaryButton(
                                        text = "Follow",
                                        onClick = {
                                            scope.launch {
                                                val success = discoverVm.repository.toggleFollow(currentUserId, user.uid)
                                                if (success) {
                                                    currentUserFollowingIds = currentUserFollowingIds + user.uid
                                                }
                                            }
                                        },
                                        isSmall = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkeletonUserListItem() {
    val colors = LocalOscellionColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .shimmerEffectUserList()
        )
        Spacer(modifier = Modifier.width(12.dp))

        // Name and username
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.width(120.dp).height(16.dp).clip(RoundedCornerShape(8.dp)).shimmerEffectUserList())
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(8.dp)).shimmerEffectUserList())
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Button
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .shimmerEffectUserList()
        )
    }
}

fun Modifier.shimmerEffectUserList(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000)
        ),
        label = "shimmer_anim"
    )
    val colors = com.chroma.studio.ui.theme.LocalOscellionColors.current

    background(
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(
                colors.textMuted.copy(alpha = 0.12f),
                colors.textMuted.copy(alpha = 0.28f),
                colors.textMuted.copy(alpha = 0.12f),
            ),
            start = androidx.compose.ui.geometry.Offset(startOffsetX, 0f),
            end = androidx.compose.ui.geometry.Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned { size = it.size }
}
