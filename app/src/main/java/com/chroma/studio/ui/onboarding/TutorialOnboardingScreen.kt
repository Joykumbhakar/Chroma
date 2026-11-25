package com.chroma.studio.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import com.chroma.studio.ui.theme.LocalOscellionColors
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.chroma.studio.R
import com.chroma.studio.chat.sound.SoundManager
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch

// ── Obsidian Studio Design Tokens ───────────────────────────────────────────
private val ObsidianCanvas = Color(0xFF09090B)
private val SurfaceCard = Color(0xFF13131A)
private val SurfaceSubtle = Color(0xFF111116)
private val SurfaceGlass = Color(0xCC181820)
private val HairlineBorder = Color.White.copy(alpha = 0.09f)
private val TextPrimary = Color(0xFFFAFAFA)
private val TextSecondary = Color(0xFFA1A1AA)
private val TextMuted = Color(0xFF71717A)
private val StudioAzure = Color(0xFF0381FE)
private val StudioIndigo = Color(0xFF6366F1)
private val StudioCyan = Color(0xFF38BDF8)
private val AccentHeart = Color(0xFFF43F5E)

/**
 * Editorial First-Time Studio Onboarding built around the React Bits <GridMotion /> engine.
 *
 * Slides:
 * - Page 1: Full-bleed kinetic diagonal <GridMotion /> gallery wall + optical reticle focal framing
 * - Page 2: Ambient dark <GridMotion /> backdrop with real editorial creator feed cards & remix metrics
 * - Page 3: Edge-bled tilted <GridMotion /> showcase + Studio Dark / Daylight workspace selector
 * - Page 4: Real frosted notification banner stack + granular benefit rows + POST_NOTIFICATIONS flow
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialOnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val appColors = LocalOscellionColors.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    // Onboarding renders on an obsidian dark canvas regardless of current app theme:
    // force light (white) status & navigation bar icons while active, then restore.
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.isAppearanceLightStatusBars = false
            controller?.isAppearanceLightNavigationBars = false
        }
        DisposableEffect(view, appColors.isDark) {
            val window = (view.context as? android.app.Activity)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.isAppearanceLightStatusBars = false
            controller?.isAppearanceLightNavigationBars = false
            onDispose {
                controller?.isAppearanceLightStatusBars = !appColors.isDark
                controller?.isAppearanceLightNavigationBars = !appColors.isDark
            }
        }
    }

    val totalPages = 4
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })
    var preferDarkWorkspace by remember { mutableStateOf(prefs.getBoolean("is_dark_mode", true)) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onFinish()
    }

    fun finishTutorial(requestNotifications: Boolean = false) {
        prefs.edit().putBoolean("is_dark_mode", preferDarkWorkspace).apply()
        if (requestNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        onFinish()
    }

    val studioGalleryTiles = remember {
        listOf(
            GridMotionTile.ImageRes(R.drawable.onboard_art_botanical, "Cobalt botanical macro"),
            GridMotionTile.ImageRes(R.drawable.wallpaper_hd_3, "Amber & slate horizon"),
            GridMotionTile.ImageRes(R.drawable.onboard_art_prism, "Optical light prism"),
            GridMotionTile.ImageRes(R.drawable.wallpapers, "Atlantic twilight cliff"),
            GridMotionTile.ImageRes(R.drawable.onboard_art_architecture, "Golden hour concrete"),
            GridMotionTile.ImageRes(R.drawable.wallpaper_macbook, "Sculpted azure ribs"),
            GridMotionTile.ImageRes(R.drawable.wallpaper_stock, "Midnight cobalt mesh"),
            GridMotionTile.ImageRes(R.drawable.wallpaper_hd_4, "Emerald glass curve"),
            GridMotionTile.ImageRes(R.drawable.wallpaper, "Aegean coast daylight"),
            GridMotionTile.ImageRes(R.drawable.bg_lock_screen, "Anodized dark silk"),
            GridMotionTile.ImageRes(R.drawable.wallpaper_macbook_alt, "Refractive wave"),
            GridMotionTile.ImageRes(R.drawable.wallpaper_windows, "Obsidian sapphire fold")
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
    ) {
        AmbientStudioGlow(currentPage = pagerState.currentPage)

        if (pagerState.currentPage == 0) {
            // Full-bleed React Bits Showcase GridMotion wall running edge-to-edge under top chrome & copy
            GridMotion(
                items = studioGalleryTiles,
                modifier = Modifier.fillMaxSize(),
                rows = 6,
                aspectRatio = 1.33f,
                gap = 14.dp,
                radius = 14.dp,
                angle = -22f,
                tilt = 32f,
                speedDpPerSec = 52f,
                direction = GridMotionDirection.Alternate,
                parallax = 0.5f,
                spotlight = 0.65f,
                dim = 0.18f,
                fade = 0.30f
            )
            // Top editorial scrim so status bar & "Oscellion / Skip" stay crisp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.22f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            0.0f to ObsidianCanvas.copy(alpha = 0.92f),
                            0.55f to ObsidianCanvas.copy(alpha = 0.45f),
                            1.0f to Color.Transparent
                        )
                    )
            )
            // Bottom editorial legibility fade into obsidian dark surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.54f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.35f to ObsidianCanvas.copy(alpha = 0.82f),
                            0.65f to ObsidianCanvas.copy(alpha = 0.96f),
                            1.0f to ObsidianCanvas
                        )
                    )
            )
        } else if (pagerState.currentPage == 1) {
            // Full-bleed 3D Isometric Vertical Marquee showcase right after Slide 1 GridMotion
            Marquee3DTestimonials(
                modifier = Modifier.fillMaxSize(),
                fadeColor = ObsidianCanvas
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.50f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.38f to ObsidianCanvas.copy(alpha = 0.88f),
                            0.68f to ObsidianCanvas.copy(alpha = 0.97f),
                            1.0f to ObsidianCanvas
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopOnboardingBar(
                currentPage = pagerState.currentPage,
                totalPages = totalPages,
                onBack = {
                    SoundManager.playReaction()
                    scope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                },
                onSkipOrClose = {
                    SoundManager.playReaction()
                    finishTutorial(requestNotifications = false)
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> SlideOneGridMotionHero()
                    1 -> SlideTwoCommunityFeed()
                    2 -> SlideThreeWorkspaceShowcase(
                        tiles = studioGalleryTiles,
                        preferDarkWorkspace = preferDarkWorkspace,
                        onSelectWorkspace = { isDark ->
                            SoundManager.playReaction()
                            preferDarkWorkspace = isDark
                        }
                    )
                    3 -> SlideFourNotifications()
                }
            }

            BottomActionFooter(
                currentPage = pagerState.currentPage,
                totalPages = totalPages,
                onPrimaryClick = {
                    SoundManager.playReaction()
                    if (pagerState.currentPage >= totalPages - 1) {
                        finishTutorial(requestNotifications = true)
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = pagerState.currentPage + 1,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioNoBouncy
                                )
                            )
                        }
                    }
                },
                onSecondaryClick = {
                    SoundManager.playReaction()
                    if (pagerState.currentPage == 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    } else {
                        finishTutorial(requestNotifications = false)
                    }
                }
            )
        }
    }
}

// ============================================================================
// TOP NAVIGATION CHROME
// ============================================================================

@Composable
private fun TopOnboardingBar(
    currentPage: Int,
    totalPages: Int,
    onBack: () -> Unit,
    onSkipOrClose: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = (currentPage + 1).toFloat() / totalPages.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "top_progress"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (currentPage == 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_oscellion_app_icon),
                    contentDescription = "Oscellion",
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .border(0.75.dp, HairlineBorder, RoundedCornerShape(7.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "Oscellion",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SurfaceGlass)
                    .border(0.8.dp, HairlineBorder, CircleShape)
                    .clickable {
                        if (currentPage == totalPages - 1) onSkipOrClose() else onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (currentPage == totalPages - 1) Lucide.X else Lucide.ArrowLeft,
                    contentDescription = if (currentPage == totalPages - 1) "Close" else "Back",
                    tint = TextPrimary.copy(alpha = 0.88f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // Minimalist Capsule Step Progress
        Box(
            modifier = Modifier
                .width(88.dp)
                .height(3.5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.14f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(listOf(StudioAzure, StudioCyan))
                    )
            )
        }

        if (currentPage < totalPages - 1) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(SurfaceGlass)
                    .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                    .clickable { onSkipOrClose() }
                    .padding(horizontal = 13.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Skip",
                    color = TextPrimary.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Spacer(modifier = Modifier.size(34.dp))
        }
    }
}

// ============================================================================
// SLIDE 1 — React Bits Showcase Copy over Full-Bleed 3D GridMotion Wall
// ============================================================================

@Composable
private fun SlideOneGridMotionHero() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "A wall of craft\nthat moves with you",
            color = TextPrimary,
            fontSize = 31.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 35.sp,
            letterSpacing = (-0.8).sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Oscellion brings cinema 3D LUTs, multi-layer shaders, and optical depth directly to your creative workflow.",
            color = TextSecondary,
            fontSize = 14.5.sp,
            lineHeight = 20.5.sp
        )
    }
}

// ============================================================================
// SLIDE 2 — 3D Isometric Vertical Marquee Creator Network Copy (Full-Bleed Behind)
// ============================================================================

@Composable
private fun SlideTwoCommunityFeed() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Share drops with\na creator network",
            color = TextPrimary,
            fontSize = 31.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 35.sp,
            letterSpacing = (-0.8).sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Publish live shaders, remix creators' 16-bit pipelines, and connect inside verified Close Friends story circles.",
            color = TextSecondary,
            fontSize = 14.5.sp,
            lineHeight = 20.5.sp
        )
    }
}

// ============================================================================
// SLIDE 3 — Edge-Bled <GridMotion /> Showcase + Workspace Mode Choice
// ============================================================================

@Composable
private fun SlideThreeWorkspaceShowcase(
    tiles: List<GridMotionTile>,
    preferDarkWorkspace: Boolean,
    onSelectWorkspace: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Upper 42%: Live GridMotion Showcase bleeding into dark obsidian surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f)
        ) {
            GridMotion(
                items = tiles,
                modifier = Modifier.fillMaxSize(),
                rows = 5,
                aspectRatio = 1.33f,
                gap = 12.dp,
                radius = 12.dp,
                angle = -22f,
                tilt = 38f,
                speedDpPerSec = 46f,
                direction = GridMotionDirection.Alternate,
                parallax = 0.5f,
                spotlight = 0.55f,
                dim = 0.22f,
                fade = 0.45f
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to ObsidianCanvas
                        )
                    )
            )

            // Crisp App Identity Mark anchored over the fade boundary
            Image(
                painter = painterResource(id = R.drawable.ic_oscellion_app_icon),
                contentDescription = "Oscellion",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 6.dp)
                    .size(46.dp)
                    .shadow(14.dp, RoundedCornerShape(13.dp), spotColor = StudioAzure.copy(alpha = 0.45f))
                    .clip(RoundedCornerShape(13.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(13.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.58f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Everything built for creators",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            ChecklistRow("Lossless 3D LUTs, curves & optical blur")
            Spacer(modifier = Modifier.height(7.dp))
            ChecklistRow("End-to-end encrypted voice & direct chat")
            Spacer(modifier = Modifier.height(7.dp))
            ChecklistRow("Verified Gold & Close Friends circles")

            Spacer(modifier = Modifier.height(16.dp))

            WorkspaceChoiceCard(
                title = "Studio Dark",
                badgeText = "Recommended",
                subtitle = "OLED-deep canvas for accurate grading",
                selected = preferDarkWorkspace,
                onClick = { onSelectWorkspace(true) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            WorkspaceChoiceCard(
                title = "Daylight Studio",
                badgeText = null,
                subtitle = "Crisp neutral surface for daytime editing",
                selected = !preferDarkWorkspace,
                onClick = { onSelectWorkspace(false) }
            )
        }
    }
}

// ============================================================================
// SLIDE 4 — Real Frosted Glass Notification Feed + Benefits
// ============================================================================

@Composable
private fun SlideFourNotifications() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.38f)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0.0f to Color.Black,
                            0.72f to Color.Black,
                            0.99f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Back depth notification card
            RealNotificationBannerCard(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .offset(x = 14.dp, y = 26.dp),
                alpha = 0.35f,
                title = "Trending Spotlight",
                body = "Your shader reached #1 on Explore",
                time = "12m",
                thumbRes = null
            )

            // Mid notification card
            RealNotificationBannerCard(
                modifier = Modifier
                    .fillMaxWidth(0.83f)
                    .offset(x = 6.dp, y = 13.dp),
                alpha = 0.62f,
                title = "Close Friends • Voice Note",
                body = "Sandip Pal sent a 0:18 studio note",
                time = "2m",
                thumbRes = null
            )

            // Front focal notification banner
            RealNotificationBannerCard(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .offset(x = 0.dp, y = 0.dp),
                alpha = 1f,
                title = "Sandip Pal remixed Cobalt Prism",
                body = "Opened your 16-bit LUT & layered optical blur",
                time = "now",
                thumbRes = R.drawable.onboard_art_prism
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.62f)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Get notified about\ndrops and replies",
                color = TextPrimary,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            NotificationBenefitRow(
                icon = Lucide.TrendingUp,
                iconTint = StudioCyan,
                prefix = "Get ",
                boldPart = "live reach insights ",
                middle = "when creators view or ",
                secondBold = "remix your work",
                suffix = ""
            )

            Spacer(modifier = Modifier.height(18.dp))

            NotificationBenefitRow(
                icon = Lucide.BadgeCheck,
                iconTint = Color(0xFF818CF8),
                prefix = "",
                boldPart = "Story drops & highlights ",
                middle = "from your ",
                secondBold = "Close Friends circle",
                suffix = ""
            )

            Spacer(modifier = Modifier.height(18.dp))

            NotificationBenefitRow(
                icon = Lucide.Bell,
                iconTint = StudioAzure,
                prefix = "Know instantly when an ",
                boldPart = "encrypted voice note ",
                middle = "or ",
                secondBold = "direct reply",
                suffix = " arrives"
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "You can change this anytime in Settings.",
                color = TextMuted,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun RealNotificationBannerCard(
    modifier: Modifier = Modifier,
    alpha: Float,
    title: String,
    body: String,
    time: String,
    @DrawableRes thumbRes: Int?
) {
    Row(
        modifier = modifier
            .height(76.dp)
            .alpha(alpha)
            .shadow(16.dp, RoundedCornerShape(18.dp), spotColor = StudioAzure.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF191C28), Color(0xFF13151E)))
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_oscellion_app_icon),
            contentDescription = "Oscellion",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp)),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = time,
                    color = TextMuted,
                    fontSize = 10.5.sp
                )
            }
            Text(
                text = body,
                color = TextSecondary,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (thumbRes != null) {
            Image(
                painter = painterResource(id = thumbRes),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .border(0.75.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(9.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// ============================================================================
// BOTTOM ACTION FOOTER (React Bits High-Contrast Editorial Rhythm)
// ============================================================================

@Composable
private fun BottomActionFooter(
    currentPage: Int,
    totalPages: Int,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit
) {
    val isLast = currentPage == totalPages - 1
    val isFirst = currentPage == 0
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cta_press_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 18.dp, top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Primary High-Contrast Flagship Pill (React Bits clean white / Oscellion Azure on Permission)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = if (isLast) StudioAzure.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(26.dp))
                .background(
                    if (isLast) Brush.horizontalGradient(listOf(StudioAzure, StudioIndigo))
                    else Brush.horizontalGradient(listOf(Color(0xFFFAFAFA), Color(0xFFE4E4E7)))
                )
                .clickable(interactionSource = interaction, indication = null) { onPrimaryClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = when {
                        isFirst -> "Start creating"
                        isLast -> "Allow notifications"
                        else -> "Continue"
                    },
                    color = if (isLast) Color.White else Color(0xFF09090B),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
                if (!isLast) {
                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF09090B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (isFirst || isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceCard)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(24.dp))
                    .clickable { onSecondaryClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFirst) "Choose workspace theme" else "Not now",
                    color = TextPrimary.copy(alpha = 0.88f),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============================================================================
// SUPPORTING ROWS & AMBIENT GLOW
// ============================================================================

@Composable
private fun ChecklistRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Lucide.Check,
            contentDescription = null,
            tint = StudioCyan,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            color = TextPrimary.copy(alpha = 0.92f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WorkspaceChoiceCard(
    title: String,
    badgeText: String?,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) StudioAzure else HairlineBorder,
        animationSpec = tween(200),
        label = "choice_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF101929) else SurfaceSubtle,
        animationSpec = tween(200),
        label = "choice_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.3.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(StudioAzure.copy(alpha = 0.18f))
                            .border(0.8.dp, StudioAzure.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = StudioCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.5.sp
            )
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) StudioAzure else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (selected) StudioAzure else Color.White.copy(alpha = 0.25f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationBenefitRow(
    icon: ImageVector,
    iconTint: Color,
    prefix: String,
    boldPart: String,
    middle: String,
    secondBold: String,
    suffix: String
) {
    val styledText = remember(prefix, boldPart, middle, secondBold, suffix) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = TextSecondary, fontWeight = FontWeight.Normal)) {
                append(prefix)
            }
            withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.SemiBold)) {
                append(boldPart)
            }
            withStyle(SpanStyle(color = TextSecondary, fontWeight = FontWeight.Normal)) {
                append(middle)
            }
            withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.SemiBold)) {
                append(secondBold)
            }
            withStyle(SpanStyle(color = TextSecondary, fontWeight = FontWeight.Normal)) {
                append(suffix)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.14f))
                .border(0.75.dp, iconTint.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = styledText,
            fontSize = 14.5.sp,
            lineHeight = 20.5.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AmbientStudioGlow(currentPage: Int) {
    val glowColor by animateColorAsState(
        targetValue = when (currentPage) {
            0 -> StudioAzure
            1 -> StudioCyan
            2 -> StudioIndigo
            else -> StudioAzure
        },
        animationSpec = tween(550),
        label = "spotlight_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(95.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.16f), Color.Transparent),
                    radius = size.width * 0.78f,
                    center = Offset(size.width * 0.5f, size.height * 0.26f)
                )
            )
        }
    }
}
