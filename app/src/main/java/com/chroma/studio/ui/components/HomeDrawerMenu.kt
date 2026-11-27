package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

// ============================================================================
// DESIGN TOKENS & THEME PALETTE (Exact Apple-Inspired Light & Dark Surfaces)
// ============================================================================

@Immutable
internal data class HomeDrawerMenuPalette(
    val bodyBackground: Color,
    val drawerBase: Color,
    val drawerSurfaceTop: Color,
    val drawerSurfaceMid: Color,
    val drawerSurfaceBottom: Color,
    val drawerRightBorder: Color,
    val drawerInnerRim: Color,
    val hairlineDivider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val itemHoverBg: Color,
    val activeItemBg: Color,
    val activeItemText: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val treeLine: Color,
    val triggerBg: Color,
    val triggerBorder: Color,
    val triggerBar: Color,
    val footerTopGrad: Color,
    val footerMidGrad: Color,
    val footerBottomGrad: Color,
    val switchOffTrack: Color,
    val switchOnTrack: Color,
    val toastBg: Color,
    val toastText: Color,
    val toastBorder: Color,
    val toastDot: Color,
    val trashTint: Color,
    val trashBg: Color,
    val backdropScrim: Color,
    val waveGlowBottom: Color,
    val waveGlowMid: Color,
    val waveGlowOrb: Color
)

internal val LightHomeDrawerMenuPalette = HomeDrawerMenuPalette(
    bodyBackground = Color(0xFFFFFFFF),
    drawerBase = Color(0xFFFBFBFD),
    drawerSurfaceTop = Color(0xE6FFFFFF),
    drawerSurfaceMid = Color(0x99F8F9FC),
    drawerSurfaceBottom = Color(0x73EEF5FF),
    drawerRightBorder = Color(0x12000000),
    drawerInnerRim = Color(0xBFFFFFFF),
    hairlineDivider = Color(0x0F000000),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF334155),
    textMuted = Color(0xFF64748B),
    itemHoverBg = Color(0x0C000000),
    activeItemBg = Color(0x1F3B82F6),
    activeItemText = Color(0xFF2563EB),
    badgeBg = Color(0x0F000000),
    badgeText = Color(0xFF64748B),
    treeLine = Color(0x14000000),
    triggerBg = Color(0xF2FFFFFF),
    triggerBorder = Color(0x14000000),
    triggerBar = Color(0xFF1E293B),
    footerTopGrad = Color(0x1AFFFFFF),
    footerMidGrad = Color(0x4DFFFFFF),
    footerBottomGrad = Color(0xA6FFFFFF),
    switchOffTrack = Color(0x29000000),
    switchOnTrack = Color(0xFF3B82F6),
    toastBg = Color(0xEB18181B),
    toastText = Color(0xFFFFFFFF),
    toastBorder = Color(0x1AFFFFFF),
    toastDot = Color(0xFF60A5FA),
    trashTint = Color(0xFFE11D48),
    trashBg = Color(0x17F43F5E),
    backdropScrim = Color(0x38020617),
    waveGlowBottom = Color(0x1C0EA5E9),
    waveGlowMid = Color(0x0A60A5FA),
    waveGlowOrb = Color(0x247DD3FC)
)

internal val DarkHomeDrawerMenuPalette = HomeDrawerMenuPalette(
    bodyBackground = Color(0xFF09090B),
    drawerBase = Color(0xFF141416),
    drawerSurfaceTop = Color(0x08FFFFFF),
    drawerSurfaceMid = Color(0x00000000),
    drawerSurfaceBottom = Color(0x730D1424),
    drawerRightBorder = Color(0x17FFFFFF),
    drawerInnerRim = Color(0x0FFFFFFF),
    hairlineDivider = Color(0x12FFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFE4E4E7),
    textMuted = Color(0xFFA1A1AA),
    itemHoverBg = Color(0x11FFFFFF),
    activeItemBg = Color(0x2960A5FA),
    activeItemText = Color(0xFF93C5FD),
    badgeBg = Color(0x1AFFFFFF),
    badgeText = Color(0xFFA1A1AA),
    treeLine = Color(0x1AFFFFFF),
    triggerBg = Color(0xE618181B),
    triggerBorder = Color(0x1CFFFFFF),
    triggerBar = Color(0xFFE4E4E7),
    footerTopGrad = Color(0x26141416),
    footerMidGrad = Color(0x66141416),
    footerBottomGrad = Color(0xBF141416),
    switchOffTrack = Color(0x33FFFFFF),
    switchOnTrack = Color(0xFF3B82F6),
    toastBg = Color(0xF2FFFFFF),
    toastText = Color(0xFF18181B),
    toastBorder = Color(0x1A000000),
    toastDot = Color(0xFF2563EB),
    trashTint = Color(0xFFFB7185),
    trashBg = Color(0x26F43F5E),
    backdropScrim = Color(0x9E000000),
    waveGlowBottom = Color(0x332563EB),
    waveGlowMid = Color(0x0F0EA5E9),
    waveGlowOrb = Color(0x293B82F6)
)

internal fun homeDrawerScrimColor(isDarkMode: Boolean): Color =
    if (isDarkMode) DarkHomeDrawerMenuPalette.backdropScrim else LightHomeDrawerMenuPalette.backdropScrim

private val BrandBlue = Color(0xFF3B82F6)
private val AppleEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

// Shared Gradient Stops for Custom Filled Icons
private val GradBlue = listOf(Color(0xFF38BDF8), Color(0xFF2563EB))
private val GradAmber = listOf(Color(0xFFFCD34D), Color(0xFFF59E0B), Color(0xFFEA580C))
private val GradPurple = listOf(Color(0xFFC084FC), Color(0xFF7C3AED))
private val GradEmerald = listOf(Color(0xFF34D399), Color(0xFF059669))
private val GradIndigo = listOf(Color(0xFF818CF8), Color(0xFF4338CA))
private val GradGold = listOf(Color(0xFFFEF9C3), Color(0xFFFCD34D), Color(0xFFD97706))

// ============================================================================
// SLIDE-OUT DRAWER CONTENT (Header, Scrollable Sections, Vector Waves, Footer)
// ============================================================================

@Composable
fun HomeNavigationDrawerContent(
    isDarkMode: Boolean,
    isDrawerVisible: Boolean,
    activeItem: String?,
    displayName: String,
    username: String,
    avatarUrl: String,
    bannerUrl: String = "",
    isVerified: Boolean = false,
    verifiedTone: String? = null,
    isOnline: Boolean = true,
    draftsBadgeCount: String? = null,
    onCloseClick: () -> Unit,
    onProfileClick: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onSelectItem: (label: String, markActive: Boolean) -> Unit
) {
    val palette = if (isDarkMode) DarkHomeDrawerMenuPalette else LightHomeDrawerMenuPalette
    var isStoryExpanded by remember { mutableStateOf(false) }
    var isDraftsExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.drawerBase)
            .drawBehind {
                // Vertical Apple-style clean surface luminance
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to palette.drawerSurfaceTop,
                        0.55f to palette.drawerSurfaceMid,
                        1f to palette.drawerSurfaceBottom
                    )
                )
                // Crisp inner glass rim along left/top and hairline divider on right edge
                drawLine(
                    color = palette.drawerRightBorder,
                    start = Offset(size.width - 0.5.dp.toPx(), 0f),
                    end = Offset(size.width - 0.5.dp.toPx(), size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        // 1. Live Parametric Wireframe Wave Vector Graphics anchored at the bottom
        ParametricWaveBackground(
            palette = palette,
            isAnimating = isDrawerVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(340.dp)
        )

        // 2. Main Foreground Drawer Column
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Banner + Half-Overlapping User Profile Avatar
            DrawerHeaderWithProfile(
                palette = palette,
                displayName = displayName,
                username = username,
                avatarUrl = avatarUrl,
                bannerUrl = bannerUrl,
                isVerified = isVerified,
                verifiedTone = verifiedTone,
                isOnline = isOnline,
                onCloseClick = onCloseClick,
                onProfileClick = onProfileClick
            )

            // Scrollable Navigation Sections (Tight Spacing & Minimal Padding)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // SECTION 1: CREATE NEW*
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SectionHeaderLabel(title = "CREATE NEW", palette = palette)

                    MenuItemRow(
                        label = "New Post",
                        isActive = activeItem == "New Post",
                        palette = palette,
                        icon = { NewPostFilledIcon(Modifier.size(17.dp)) },
                        onClick = { onSelectItem("New Post", true) }
                    )

                    MenuItemRow(
                        label = "New Flash",
                        isActive = activeItem == "New Flash",
                        palette = palette,
                        icon = { FlashFilledIcon(Modifier.size(17.dp)) },
                        onClick = { onSelectItem("New Flash", true) }
                    )

                    // New Story > Expandable Dropdown
                    ExpandableMenuGroup(
                        label = "New Story",
                        expanded = isStoryExpanded,
                        palette = palette,
                        icon = { NewStoryFilledIcon(Modifier.size(17.dp)) },
                        onToggle = { isStoryExpanded = !isStoryExpanded }
                    ) {
                        SubMenuItemRow(
                            label = "Image story",
                            isActive = activeItem == "New Story → Image Story",
                            palette = palette,
                            icon = { ImageStoryFilledIcon(Modifier.size(15.dp)) },
                            onClick = { onSelectItem("New Story → Image Story", true) }
                        )
                        SubMenuItemRow(
                            label = "Text story",
                            isActive = activeItem == "New Story → Text Story",
                            palette = palette,
                            icon = { TextStoryFilledIcon(Modifier.size(15.dp)) },
                            onClick = { onSelectItem("New Story → Text Story", true) }
                        )
                    }

                    MenuItemRow(
                        label = "New Work",
                        isActive = activeItem == "New Work",
                        palette = palette,
                        icon = { NewWorkFilledIcon(Modifier.size(17.dp)) },
                        onClick = { onSelectItem("New Work", true) }
                    )
                }

                // Hairline Section Divider
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    thickness = 0.8.dp,
                    color = palette.hairlineDivider
                )

                // SECTION 2: OTHER OPTIONS*
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SectionHeaderLabel(title = "OTHER OPTIONS", palette = palette)

                    MenuItemRow(
                        label = "Backgrounds",
                        isActive = activeItem == "Backgrounds",
                        palette = palette,
                        icon = { BackgroundsFilledIcon(Modifier.size(17.dp)) },
                        onClick = { onSelectItem("Backgrounds", true) }
                    )

                    // Drafts Expandable Dropdown with count badge
                    ExpandableMenuGroup(
                        label = "Drafts",
                        expanded = isDraftsExpanded,
                        badgeCount = draftsBadgeCount,
                        palette = palette,
                        icon = {
                            DraftsFilledIcon(
                                color = if (isDarkMode) Color(0xFFD4D4D8) else Color(0xFF475569),
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        onToggle = { isDraftsExpanded = !isDraftsExpanded }
                    ) {
                        SubMenuItemRow(
                            label = "Flash editor Draft",
                            isActive = activeItem == "Drafts → Flash editor Draft",
                            palette = palette,
                            icon = { FlashFilledIcon(Modifier.size(15.dp)) },
                            onClick = { onSelectItem("Drafts → Flash editor Draft", true) }
                        )
                        SubMenuItemRow(
                            label = "Photo editor Drafts",
                            isActive = activeItem == "Drafts → Photo editor Drafts",
                            palette = palette,
                            icon = { PhotoEditorFilledIcon(Modifier.size(16.dp)) },
                            onClick = { onSelectItem("Drafts → Photo editor Drafts", true) }
                        )
                    }
                }
            }

            // 3. Bottom Pinned Footer (Clear Translucent Gradient so Vector Strands Stay Sharp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to palette.footerTopGrad,
                            0.55f to palette.footerMidGrad,
                            1f to palette.footerBottomGrad
                        )
                    )
                    .drawBehind {
                        drawLine(
                            color = palette.hairlineDivider,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .navigationBarsPadding()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Luxury Royal-Amethyst Purple "Upgrade to Pro" Badge (Zero Outer Glow, Glossy Shine)
                UpgradeToProLuxuryBadge(
                    onClick = { onSelectItem("Upgrade to Pro", false) },
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Settings
                MenuItemRow(
                    label = "Settings",
                    isActive = activeItem == "Settings",
                    palette = palette,
                    icon = {
                        SettingsFilledIcon(
                            color = if (isDarkMode) Color(0xFFA1A1AA) else Color(0xFF475569),
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    onClick = { onSelectItem("Settings", true) }
                )

                // Trash
                MenuItemRow(
                    label = "Trash",
                    isActive = activeItem == "Trash",
                    isDestructive = true,
                    palette = palette,
                    icon = {
                        TrashFilledIcon(
                            color = if (activeItem == "Trash") {
                                palette.trashTint
                            } else if (isDarkMode) {
                                Color(0xFFA1A1AA)
                            } else {
                                Color(0xFF475569)
                            },
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    onClick = { onSelectItem("Trash", true) }
                )

                // Dark Mode Row with Apple iOS-Style Toggle Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleDarkMode() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isDarkMode) {
                            SunFilledIcon(Modifier.size(17.dp))
                        } else {
                            MoonFilledIcon(
                                color = Color(0xFF475569),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Text(
                            text = "Dark mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary.copy(alpha = 0.9f)
                        )
                    }

                    AppleStyleToggleSwitch(
                        checked = isDarkMode,
                        palette = palette,
                        onCheckedChange = { onToggleDarkMode() }
                    )
                }
            }
        }
    }
}

// ============================================================================
// HEADER BANNER & HALF-OVERLAPPING USER PROFILE
// ============================================================================

@Composable
private fun DrawerHeaderWithProfile(
    palette: HomeDrawerMenuPalette,
    displayName: String,
    username: String,
    avatarUrl: String,
    bannerUrl: String,
    isVerified: Boolean,
    verifiedTone: String? = null,
    isOnline: Boolean,
    onCloseClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val bannerShape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    val bannerHeight = 112.dp
    val avatarSize = 70.dp

    val effectiveName = displayName.ifBlank { username.ifBlank { "Creator" } }
    val handleText = if (username.isNotBlank()) "@$username" else "@guest"

    // Pulsing online indicator ring
    val pulseTransition = rememberInfiniteTransition(label = "OnlinePulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OnlinePulseScale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OnlinePulseAlpha"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 1. Rounded-Bottom Header Banner Backdrop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
                    .shadow(
                        elevation = 8.dp,
                        shape = bannerShape,
                        ambientColor = Color(0x332563EB),
                        spotColor = Color(0x332563EB)
                    )
                    .clip(bannerShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4F46E5),
                                Color(0xFF2563EB),
                                Color(0xFF06B6D4)
                            ),
                            start = Offset(0f, 320f),
                            end = Offset(780f, 0f)
                        )
                    )
            ) {
                if (bannerUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(bannerUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Abstract silky ribbon highlights & subtle editorial vignette inside the banner
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (bannerUrl.isBlank()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x55FFFFFF), Color.Transparent),
                                center = Offset(size.width * 0.2f, size.height * 0.15f),
                                radius = size.width * 0.65f
                            ),
                            radius = size.width * 0.65f,
                            center = Offset(size.width * 0.2f, size.height * 0.15f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x40A5F3FC), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.8f),
                                radius = size.width * 0.55f
                            ),
                            radius = size.width * 0.55f,
                            center = Offset(size.width * 0.85f, size.height * 0.8f)
                        )
                    }
                    // Subtle editorial vignette + crisp glass bottom hairline
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color(0x26000000),
                            0.5f to Color.Transparent,
                            1f to Color(0x59000000)
                        )
                    )
                    drawLine(
                        color = Color(0x4DFFFFFF),
                        start = Offset(0f, size.height - 0.5.dp.toPx()),
                        end = Offset(size.width, size.height - 0.5.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Close Drawer Button (Top-Right of Header Banner)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 10.dp, end = 10.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x40000000))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Close Navigation Menu",
                            onClick = onCloseClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CloseXIcon(
                        color = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // 2. Overlapping User Profile Avatar (Exact 50% ON Banner, 50% BELOW Banner)
            Box(
                modifier = Modifier
                    .padding(start = 14.dp, top = bannerHeight - (avatarSize / 2))
            ) {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .shadow(8.dp, CircleShape, ambientColor = Color(0x26000000))
                        .clip(CircleShape)
                        .background(palette.drawerBase)
                        .border(1.dp, palette.hairlineDivider, CircleShape)
                        .padding(2.5.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    IosAvatarView(
                        name = effectiveName,
                        avatarUrl = avatarUrl,
                        size = avatarSize - 5.dp
                    )
                }

                // Online Status Indicator with soft pulsing ring (single-source OnlineStatusDot)
                if (isOnline && com.chroma.studio.data.OnlineStatusManager.isSelfStatusVisible()) {
                    OnlineStatusDot(
                        isOnline = true,
                        size = 15.dp,
                        showPulse = true,
                        ringColor = palette.drawerBase,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                    )
                }
            }
        }

        // 3. User Name + Custom Filled Verified Badge + Handle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onProfileClick)
                .padding(horizontal = 14.dp)
                .padding(top = 5.dp, bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = effectiveName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isVerified) {
                    VerifiedBadge(
                        size = 16.dp,
                        tone = verifiedTone,
                        userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    )
                }
            }
            Text(
                text = handleText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 14.dp),
            thickness = 0.8.dp,
            color = palette.hairlineDivider
        )
    }
}

// ============================================================================
// MENU ROWS, SECTION HEADERS & SMOOTH ACCORDION DROPDOWNS
// ============================================================================

@Composable
private fun SectionHeaderLabel(
    title: String,
    palette: HomeDrawerMenuPalette
) {
    Text(
        text = buildAnnotatedString {
            append(title)
            withStyle(SpanStyle(color = BrandBlue)) {
                append("*")
            }
        },
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.7.sp,
        color = palette.textMuted.copy(alpha = 0.85f),
        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun MenuItemRow(
    label: String,
    isActive: Boolean,
    palette: HomeDrawerMenuPalette,
    isDestructive: Boolean = false,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val bgColor = when {
        isActive && isDestructive -> palette.trashBg
        isActive -> palette.activeItemBg
        else -> Color.Transparent
    }
    val textColor = when {
        isActive && isDestructive -> palette.trashTint
        isActive -> palette.activeItemText
        else -> palette.textSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        icon()
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun ExpandableMenuGroup(
    label: String,
    expanded: Boolean,
    palette: HomeDrawerMenuPalette,
    badgeCount: String? = null,
    icon: @Composable () -> Unit,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "ChevronRotation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                icon()
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (badgeCount != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.badgeBg)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeCount,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.badgeText
                        )
                    }
                }
                ChevronRightIcon(
                    color = palette.textMuted.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(13.dp)
                        .rotate(chevronRotation)
                )
            }
        }

        // Smooth Accordion Expansion with Left Tree Hairline
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(180, easing = FastOutSlowInEasing)) + fadeOut(tween(150))
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, top = 2.dp, bottom = 2.dp, end = 4.dp)
                    .drawBehind {
                        drawLine(
                            color = palette.treeLine,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SubMenuItemRow(
    label: String,
    isActive: Boolean,
    palette: HomeDrawerMenuPalette,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) palette.activeItemBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon()
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isActive) palette.activeItemText else palette.textSecondary.copy(alpha = 0.88f)
        )
    }
}

// ============================================================================
// LUXURY ROYAL-AMETHYST PURPLE "UPGRADE TO PRO" BADGE (ZERO OUTER GLOW + SHINE)
// ============================================================================

@Composable
private fun UpgradeToProLuxuryBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeShape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(140),
        label = "ProBadgeScale"
    )

    // Continuous diagonal metallic shine sweep (active in first 42% of 3.6s cycle)
    val shineTransition = rememberInfiniteTransition(label = "ProShineTransition")
    val shineProgress by shineTransition.animateFloat(
        initialValue = -0.55f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3600
                -0.55f at 0 using AppleEaseOut
                1.65f at 1520 using LinearEasing
                1.65f at 3600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "ProShineProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(badgeShape)
            .background(
                // Deep multi-stop Imperial Royal-Amethyst Purple Gradient (~112 deg)
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFF1C0A38),
                        0.30f to Color(0xFF3F157A),
                        0.62f to Color(0xFF6B21B8),
                        0.84f to Color(0xFF8B35E8),
                        1.00f to Color(0xFF4B1387)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(720f, 220f)
                )
            )
            .border(1.dp, Color(0x40FFFFFF), badgeShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Satin highlight, glossy upper glass reflection, specular champagne top rim & animated shine sweep
        Canvas(modifier = Modifier.matchParentSize()) {
            // Soft satin top-left radial highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x36F0ABFC), Color.Transparent),
                    center = Offset(size.width * 0.16f, 0f),
                    radius = size.width * 0.42f
                ),
                radius = size.width * 0.42f,
                center = Offset(size.width * 0.16f, 0f)
            )

            // Glossy upper-half glass reflection
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x30FFFFFF), Color(0x0AFFFFFF), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.52f
                ),
                size = Size(size.width, size.height * 0.52f)
            )

            // Crisp champagne-white specular top bevel
            drawLine(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to Color(0xD9F3E8FF),
                    1f to Color.Transparent
                ),
                start = Offset(10.dp.toPx(), 0.5.dp.toPx()),
                end = Offset(size.width - 10.dp.toPx(), 0.5.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )

            // Animated diagonal metallic light sweep
            val bandWidth = 56.dp.toPx()
            val centerX = size.width * shineProgress
            val skewOffset = size.height * 0.45f
            val sweepPath = Path().apply {
                moveTo(centerX - bandWidth / 2 + skewOffset, 0f)
                lineTo(centerX + bandWidth / 2 + skewOffset, 0f)
                lineTo(centerX + bandWidth / 2 - skewOffset, size.height)
                lineTo(centerX - bandWidth / 2 - skewOffset, size.height)
                close()
            }
            drawPath(
                path = sweepPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x59FFFFFF),
                        Color.Transparent
                    ),
                    startX = centerX - bandWidth / 2,
                    endX = centerX + bandWidth / 2
                )
            )
        }

        // Foreground Badge Content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            FluentDiamondGemIcon(modifier = Modifier.size(18.dp))
            Text(
                text = "Upgrade to Pro",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.15).sp,
                color = Color(0xF5FFFFFF)
            )
        }
    }
}

// ============================================================================
// APPLE iOS-STYLE ACCESSIBLE DARK MODE TOGGLE SWITCH
// ============================================================================

@Composable
private fun AppleStyleToggleSwitch(
    checked: Boolean,
    palette: HomeDrawerMenuPalette,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) palette.switchOnTrack else palette.switchOffTrack,
        animationSpec = tween(200),
        label = "SwitchTrackColor"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 0.dp,
        animationSpec = tween(210, easing = AppleEaseOut),
        label = "SwitchThumbOffset"
    )

    Box(
        modifier = Modifier
            .width(40.dp)
            .height(22.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(18.dp)
                .shadow(3.dp, CircleShape, ambientColor = Color(0x47000000))
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// ============================================================================
// LIVE PARAMETRIC WIREFRAME WAVE VECTOR GRAPHIC (BOTTOM SECTION)
// ============================================================================

@Composable
private fun ParametricWaveBackground(
    palette: HomeDrawerMenuPalette,
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    var phase by remember { mutableFloatStateOf(0f) }

    // Pause animation automatically when drawer is closed
    LaunchedEffect(isAnimating) {
        if (!isAnimating) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val deltaSec = ((frameNanos - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                    phase += deltaSec * 0.48f
                }
                lastNanos = frameNanos
            }
        }
    }

    val reusablePath = remember { Path() }
    val silkPath = remember { Path() }
    val primaryPts = remember { FloatArray(14) }

    Box(modifier = modifier) {
        // Atmospheric blue glow layer behind wireframe strands
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to palette.waveGlowMid,
                    1f to palette.waveGlowBottom
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.waveGlowOrb, Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.72f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.5f, size.height * 0.72f)
            )
        }

        // Parametric Blend Wireframe Ribbons with smooth top fade mask
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            val vbW = 360f
            val vbH = 320f
            val scale = max(size.width / vbW, size.height / vbH)
            val offsetX = (size.width - vbW * scale) / 2f
            val offsetY = size.height - vbH * scale

            val s1 = sin(phase) * 8f
            val c1 = cos(phase * 0.82f) * 7.5f
            val s2 = sin(phase * 0.68f + 1.3f) * 6.5f

            val primaryStart = floatArrayOf(
                -30f, 175f + s1 * 0.4f,
                48f, 285f + c1,
                122f, 92f - s1,
                202f, 168f + s2,
                268f, 232f - c1,
                320f, 118f + s1,
                390f, 162f - s2 * 0.5f
            )
            val primaryEnd = floatArrayOf(
                -30f, 238f - c1 * 0.4f,
                66f, 152f + s2,
                144f, 290f + c1,
                226f, 212f - s1,
                286f, 128f + s2,
                335f, 256f - s1,
                390f, 218f + c1 * 0.5f
            )
            val secondaryStart = floatArrayOf(
                -30f, 282f + s2 * 0.35f,
                72f, 298f - c1,
                146f, 172f + s1,
                234f, 248f - s2,
                294f, 292f + c1,
                340f, 198f - s2,
                390f, 250f + s1 * 0.4f
            )
            val secondaryEnd = floatArrayOf(
                -30f, 226f - s1 * 0.35f,
                86f, 198f + s2,
                166f, 292f - c1,
                248f, 224f + s1,
                310f, 166f - s2,
                350f, 266f + c1,
                390f, 192f - s2 * 0.4f
            )

            val silkBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color(0x0538BDF8),
                    0.40f to Color(0x112563EB),
                    0.72f to Color(0x0E0EA5E9),
                    1.00f to Color(0x0360A5FA)
                ),
                start = Offset(0f, vbH * 0.35f),
                end = Offset(vbW, vbH * 0.70f)
            )
            val primaryBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color(0x2E38BDF8),
                    0.18f to Color(0xB80284C7),
                    0.50f to Color(0xEB2563EB),
                    0.82f to Color(0xBD0EA5E9),
                    1.00f to Color(0x2E60A5FA)
                ),
                start = Offset(0f, vbH * 0.60f),
                end = Offset(vbW, vbH * 0.25f)
            )
            val secondaryBrush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color(0x1F38BDF8),
                    0.28f to Color(0x940EA5E9),
                    0.65f to Color(0xB81D4ED8),
                    1.00f to Color(0x2460A5FA)
                ),
                start = Offset(vbW, vbH * 0.20f),
                end = Offset(0f, vbH * 0.80f)
            )
            val highlightBrush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.30f to Color(0xD10284C7),
                    0.65f to Color(0xF22563EB),
                    1.00f to Color.Transparent
                ),
                startX = 0f,
                endX = vbW
            )

            withTransform({
                translate(left = offsetX, top = offsetY)
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            }) {
                // 1. Silky translucent volume ribbon polygon
                silkPath.reset()
                silkPath.moveTo(primaryStart[0], primaryStart[1])
                silkPath.cubicTo(
                    primaryStart[2], primaryStart[3],
                    primaryStart[4], primaryStart[5],
                    primaryStart[6], primaryStart[7]
                )
                silkPath.cubicTo(
                    primaryStart[8], primaryStart[9],
                    primaryStart[10], primaryStart[11],
                    primaryStart[12], primaryStart[13]
                )
                silkPath.lineTo(primaryEnd[12], primaryEnd[13])
                silkPath.cubicTo(
                    primaryEnd[10], primaryEnd[11],
                    primaryEnd[8], primaryEnd[9],
                    primaryEnd[6], primaryEnd[7]
                )
                silkPath.cubicTo(
                    primaryEnd[4], primaryEnd[5],
                    primaryEnd[2], primaryEnd[3],
                    primaryEnd[0], primaryEnd[1]
                )
                silkPath.close()
                drawPath(path = silkPath, brush = silkBrush)

                // 2. Secondary Counter-Flowing Mesh (16 Strands)
                val secondaryCount = 16
                for (i in 0 until secondaryCount) {
                    val t = i.toFloat() / (secondaryCount - 1)
                    val easedT = easeStrandT(t)
                    val envelope = sin(t * PI).toFloat()
                    val alpha = 0.12f + envelope.pow(0.70f) * 0.52f
                    val strokeW = 0.68f * (0.65f + envelope * 0.48f)

                    interpolatePoints(secondaryStart, secondaryEnd, easedT, primaryPts)
                    fillCubicPath(reusablePath, primaryPts)
                    drawPath(
                        path = reusablePath,
                        brush = secondaryBrush,
                        alpha = alpha,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // 3. Primary Sculptural Twisting Ribbon (26 Strands)
                val primaryCount = 26
                for (i in 0 until primaryCount) {
                    val t = i.toFloat() / (primaryCount - 1)
                    val easedT = easeStrandT(t)
                    val envelope = sin(t * PI).toFloat()
                    val alpha = 0.16f + envelope.pow(0.65f) * 0.72f
                    val strokeW = 0.85f * (0.65f + envelope * 0.55f)

                    interpolatePoints(primaryStart, primaryEnd, easedT, primaryPts)
                    fillCubicPath(reusablePath, primaryPts)
                    drawPath(
                        path = reusablePath,
                        brush = primaryBrush,
                        alpha = alpha,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // 4. Crisp Focal Spine Strands
                val highlightRatios = floatArrayOf(0.36f, 0.50f, 0.64f)
                highlightRatios.forEachIndexed { idx, ratio ->
                    interpolatePoints(primaryStart, primaryEnd, ratio, primaryPts)
                    fillCubicPath(reusablePath, primaryPts)
                    drawPath(
                        path = reusablePath,
                        brush = highlightBrush,
                        alpha = if (idx == 1) 0.72f else 0.42f,
                        style = Stroke(
                            width = if (idx == 1) 1.15f else 0.80f,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // Smooth vertical top fade mask so waves emerge cleanly beneath navigation links
            drawRect(
                brush = Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.22f to Color(0xF2FFFFFF),
                    1.00f to Color.White
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }
}

private fun easeStrandT(t: Float): Float {
    return if (t < 0.5f) {
        2f * t * t * 0.32f + t * 0.68f
    } else {
        (1f - (-2f * t + 2f).pow(2) / 2f) * 0.32f + t * 0.68f
    }
}

private fun interpolatePoints(
    start: FloatArray,
    end: FloatArray,
    t: Float,
    out: FloatArray
) {
    for (i in out.indices) {
        out[i] = start[i] + (end[i] - start[i]) * t
    }
}

private fun fillCubicPath(path: Path, pts: FloatArray) {
    path.reset()
    path.moveTo(pts[0], pts[1])
    path.cubicTo(pts[2], pts[3], pts[4], pts[5], pts[6], pts[7])
    path.cubicTo(pts[8], pts[9], pts[10], pts[11], pts[12], pts[13])
}

// ============================================================================
// CUSTOM FILLED VECTOR ICONS (Zero Icon Backgrounds, Pure Compose Canvas)
// ============================================================================

private fun DrawScope.diagBrush(colors: List<Color>, alpha: Float = 1f): Brush {
    return Brush.linearGradient(
        colors = colors.map { it.copy(alpha = it.alpha * alpha) },
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
}

/** Custom Filled Verified Badge (Blue Gradient Rosette + Crisp White Checkmark) */
@Composable
private fun VerifiedBadgeFilledIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val cx = 12f * s
        val cy = 12f * s
        val badgePath = Path()
        val lobes = 8
        val steps = 96
        for (i in 0..steps) {
            val angle = (i.toDouble() / steps) * 2.0 * PI
            val r = (9.15 + 1.05 * cos(lobes * angle)).toFloat() * s
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) badgePath.moveTo(x, y) else badgePath.lineTo(x, y)
        }
        badgePath.close()

        drawPath(path = badgePath, brush = diagBrush(GradBlue))

        val checkPath = Path().apply {
            moveTo(8.7f * s, 12.2f * s)
            lineTo(11.0f * s, 14.5f * s)
            lineTo(15.5f * s, 9.6f * s)
        }
        drawPath(
            path = checkPath,
            color = Color.White,
            style = Stroke(width = 2.0f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** New Post — Filled Gradient Squircle with White Plus */
@Composable
private fun NewPostFilledIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        drawRoundRect(
            brush = diagBrush(GradBlue),
            topLeft = Offset(2.5f * s, 2.5f * s),
            size = Size(19f * s, 19f * s),
            cornerRadius = CornerRadius(5.5f * s, 5.5f * s)
        )
        drawLine(
            color = Color.White,
            start = Offset(12f * s, 7.75f * s),
            end = Offset(12f * s, 16.25f * s),
            strokeWidth = 2.3f * s,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(7.75f * s, 12f * s),
            end = Offset(16.25f * s, 12f * s),
            strokeWidth = 2.3f * s,
            cap = StrokeCap.Round
        )
    }
}

/** New Flash & Flash Editor Draft — Sculpted Filled Gradient Lightning Bolt */
@Composable
private fun FlashFilledIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val bolt = Path().apply {
            moveTo(13.8f * s, 2.2f * s)
            lineTo(12.9f * s, 9.3f * s)
            lineTo(18.7f * s, 9.3f * s)
            lineTo(10.2f * s, 21.8f * s)
            lineTo(11.1f * s, 14.7f * s)
            lineTo(5.3f * s, 14.7f * s)
            close()
        }
        drawPath(path = bolt, brush = diagBrush(GradAmber))
    }
}

/** New Story — Duotone Outer Halo + Filled Purple Circle + White Plus */
@Composable
private fun NewStoryFilledIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val center = Offset(12f * s, 12f * s)
        drawCircle(brush = diagBrush(GradPurple, alpha = 0.22f), radius = 9.75f * s, center = center)
        drawCircle(brush = diagBrush(GradPurple), radius = 7.5f * s, center = center)
        drawLine(
            color = Color.White,
            start = Offset(12f * s, 8.75f * s),
            end = Offset(12f * s, 15.25f * s),
            strokeWidth = 2.1f * s,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(8.75f * s, 12f * s),
            end = Offset(15.25f * s, 12f * s),
            strokeWidth = 2.1f * s,
            cap = StrokeCap.Round
        )
    }
}

/** Image Story — Filled Purple Landscape Photo Card */
@Composable
private fun ImageStoryFilledIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val framePath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(2.5f * s, 3.5f * s, 21.5f * s, 20.5f * s),
                    cornerRadius = CornerRadius(4f * s, 4f * s)
                )
            )
        }
        drawPath(path = framePath, brush = diagBrush(GradPurple, alpha = 0.26f))
        drawCircle(
            brush = diagBrush(GradPurple),
            radius = 1.8f * s,
            center = Offset(8.4f * s, 8.7f * s)
        )
        clipPath(framePath) {
            val hills = Path().apply {
                moveTo(2.5f * s, 20.5f * s)
                lineTo(2.5f * s, 17.5f * s)
                lineTo(9.1f * s, 12.2f * s)
                lineTo(12.2f * s, 15.2f * s)
                lineTo(15.9f * s, 11.4f * s)
                lineTo(21.5f * s, 17.2f * s)
                lineTo(21.5f * s, 20.5f * s)
                close()
            }
            drawPath(path = hills, brush = diagBrush(GradPurple))
        }
    }
}

/** Text Story — Filled Purple Typography Card */
@Composable
private fun TextStoryFilledIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        drawRoundRect(
            brush = diagBrush(GradPurple),
            topLeft = Offset(2.5f * s, 3.5f * s),
            size = Size(19f * s, 17f * s),
            cornerRadius = CornerRadius(4f * s, 4f * s)
        )
        drawLine(Color.White, Offset(8f * s, 8.5f * s), Offset(16f * s, 8.5f * s), 2f * s, StrokeCap.Round)
        drawLine(Color.White, Offset(12f * s, 8.5f * s), Offset(12f * s, 16.2f * s), 2f * s, StrokeCap.Round)
        drawLine(Color.White, Offset(10f * s, 16.2f * s), Offset(14f * s, 16.2f * s), 2f * s, StrokeCap.Round)
    }
}

/** New Work — Filled Emerald Briefcase */
@Composable
private fun NewWorkFilledIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val handle = Path().apply {
            moveTo(8.5f * s, 6.8f * s)
            lineTo(8.5f * s, 5.0f * s)
            quadraticBezierTo(8.5f * s, 3.1f * s, 10.5f * s, 3.1f * s)
            lineTo(13.5f * s, 3.1f * s)
            quadraticBezierTo(15.5f * s, 3.1f * s, 15.5f * s, 5.0f * s)
            lineTo(15.5f * s, 6.8f * s)
        }
        drawPath(
            path = handle,
            brush = diagBrush(GradEmerald),
            style = Stroke(width = 2.2f * s, cap = StrokeCap.Round)
        )
        drawRoundRect(
            brush = diagBrush(GradEmerald),
            topLeft = Offset(2.5f * s, 6.5f * s),
            size = Size(19f * s, 14f * s),
            cornerRadius = CornerRadius(4f * s, 4f * s)
        )
        val flap = Path().apply {
            moveTo(2.6f * s, 11.5f * s)
            quadraticBezierTo(12f * s, 13.8f * s, 21.4f * s, 11.5f * s)
        }
        drawPath(
            path = flap,
            color = Color(0x59FFFFFF),
            style = Stroke(width = 1.5f * s, cap = StrokeCap.Round)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(10.25f * s, 11f * s),
            size = Size(3.5f * s, 2.4f * s),
            cornerRadius = CornerRadius(1f * s, 1f * s)
        )
    }
}

/** Backgrounds — Depth-Layered Filled Indigo Stack */
@Composable
private fun BackgroundsFilledIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val topLayer = Path().apply {
            moveTo(12f * s, 2.7f * s)
            lineTo(20.9f * s, 7.6f * s)
            lineTo(12f * s, 12.5f * s)
            lineTo(3.1f * s, 7.6f * s)
            close()
        }
        drawPath(path = topLayer, brush = diagBrush(GradIndigo))

        val midLayer = Path().apply {
            moveTo(3.2f * s, 12.3f * s)
            lineTo(12f * s, 16.9f * s)
            lineTo(20.8f * s, 12.3f * s)
        }
        drawPath(
            path = midLayer,
            brush = diagBrush(GradIndigo, alpha = 0.65f),
            style = Stroke(width = 2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val bottomLayer = Path().apply {
            moveTo(3.2f * s, 16.7f * s)
            lineTo(12f * s, 21.3f * s)
            lineTo(20.8f * s, 16.7f * s)
        }
        drawPath(
            path = bottomLayer,
            brush = diagBrush(GradIndigo, alpha = 0.38f),
            style = Stroke(width = 2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** Drafts — Duotone Folded-Corner Document + Edit Nib */
@Composable
private fun DraftsFilledIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val doc = Path().apply {
            moveTo(6.5f * s, 2.5f * s)
            lineTo(14f * s, 2.5f * s)
            lineTo(20.5f * s, 9f * s)
            lineTo(20.5f * s, 18.5f * s)
            quadraticBezierTo(20.5f * s, 21.5f * s, 17.5f * s, 21.5f * s)
            lineTo(6.5f * s, 21.5f * s)
            quadraticBezierTo(3.5f * s, 21.5f * s, 3.5f * s, 18.5f * s)
            lineTo(3.5f * s, 5.5f * s)
            quadraticBezierTo(3.5f * s, 2.5f * s, 6.5f * s, 2.5f * s)
            close()
        }
        drawPath(path = doc, color = color.copy(alpha = 0.24f))

        val fold = Path().apply {
            moveTo(14f * s, 2.5f * s)
            lineTo(14f * s, 7.2f * s)
            quadraticBezierTo(14f * s, 9f * s, 15.8f * s, 9f * s)
            lineTo(20.5f * s, 9f * s)
            close()
        }
        drawPath(path = fold, color = color.copy(alpha = 0.55f))

        drawLine(color, Offset(7.5f * s, 12.2f * s), Offset(13.2f * s, 12.2f * s), 2f * s, StrokeCap.Round)
        drawLine(color, Offset(7.5f * s, 15.8f * s), Offset(11.2f * s, 15.8f * s), 2f * s, StrokeCap.Round)

        val pencil = Path().apply {
            moveTo(17.7f * s, 12.0f * s)
            lineTo(19.3f * s, 13.6f * s)
            lineTo(14.9f * s, 18.0f * s)
            lineTo(13.0f * s, 18.3f * s)
            lineTo(13.3f * s, 16.4f * s)
            close()
        }
        drawPath(path = pencil, color = color)
    }
}

/** Photo Editor Drafts — Filled Photo Frame (Sun + Mountains) + Precision Angled Edit Pencil */
@Composable
private fun PhotoEditorFilledIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val photoBase = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(2.5f * s, 3f * s, 19f * s, 18f * s),
                    cornerRadius = CornerRadius(3.2f * s, 3.2f * s)
                )
            )
        }
        val cornerCutout = Path().apply {
            moveTo(19.5f * s, 8.6f * s)
            lineTo(19.5f * s, 18.5f * s)
            lineTo(9.6f * s, 18.5f * s)
            close()
        }
        val croppedPhoto = Path.combine(PathOperation.Difference, photoBase, cornerCutout)
        drawPath(path = croppedPhoto, brush = diagBrush(GradBlue))

        clipPath(croppedPhoto) {
            drawCircle(
                color = Color.White,
                radius = 1.6f * s,
                center = Offset(7.4f * s, 7.6f * s)
            )
            val peaks = Path().apply {
                moveTo(3.5f * s, 17f * s)
                lineTo(8.3f * s, 11.4f * s)
                lineTo(10.4f * s, 13.5f * s)
                lineTo(12.3f * s, 11.6f * s)
                lineTo(15.2f * s, 15.2f * s)
                lineTo(15.2f * s, 18f * s)
                lineTo(3.5f * s, 18f * s)
                close()
            }
            drawPath(path = peaks, color = Color(0xF0FFFFFF))
        }

        val pencil = Path().apply {
            moveTo(18.6f * s, 11.5f * s)
            lineTo(21.1f * s, 14.0f * s)
            lineTo(15.5f * s, 19.6f * s)
            lineTo(12.2f * s, 20.3f * s)
            lineTo(12.9f * s, 17.0f * s)
            close()
        }
        drawPath(path = pencil, brush = diagBrush(GradIndigo))
    }
}

/** Settings — Precision Filled 8-Tooth Gear with True Center Cutout */
@Composable
private fun SettingsFilledIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val cx = 12f * s
        val cy = 12f * s

        val outerPath = Path()
        val teeth = 8
        val steps = 96
        for (i in 0..steps) {
            val angle = (i.toDouble() / steps) * 2.0 * PI
            val wave = cos(teeth * angle)
            val shaped = if (wave > 0) wave.pow(0.45) else -(-wave).pow(0.45)
            val r = (8.3 + 1.85 * shaped).toFloat() * s
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) outerPath.moveTo(x, y) else outerPath.lineTo(x, y)
        }
        outerPath.close()

        val innerHole = Path().apply {
            addOval(Rect(center = Offset(cx, cy), radius = 3.25f * s))
        }
        val gear = Path.combine(PathOperation.Difference, outerPath, innerHole)
        drawPath(path = gear, color = color)
        drawCircle(color = color.copy(alpha = 0.35f), radius = 1.45f * s, center = Offset(cx, cy))
    }
}

/** Trash — Layered Filled Bin with True Vertical Slit Cutouts */
@Composable
private fun TrashFilledIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        drawRoundRect(
            color = color,
            topLeft = Offset(9f * s, 2.2f * s),
            size = Size(6f * s, 2.8f * s),
            cornerRadius = CornerRadius(1.4f * s, 1.4f * s)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(3.5f * s, 4.5f * s),
            size = Size(17f * s, 2.2f * s),
            cornerRadius = CornerRadius(1.1f * s, 1.1f * s)
        )

        val body = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(5.5f * s, 8f * s, 18.5f * s, 21.8f * s),
                    topLeft = CornerRadius(1f * s, 1f * s),
                    topRight = CornerRadius(1f * s, 1f * s),
                    bottomLeft = CornerRadius(3.4f * s, 3.4f * s),
                    bottomRight = CornerRadius(3.4f * s, 3.4f * s)
                )
            )
        }
        val slits = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(9.2f * s, 10.8f * s, 10.9f * s, 18.6f * s),
                    cornerRadius = CornerRadius(0.85f * s, 0.85f * s)
                )
            )
            addRoundRect(
                RoundRect(
                    rect = Rect(13.1f * s, 10.8f * s, 14.8f * s, 18.6f * s),
                    cornerRadius = CornerRadius(0.85f * s, 0.85f * s)
                )
            )
        }
        val bin = Path.combine(PathOperation.Difference, body, slits)
        drawPath(path = bin, color = color.copy(alpha = 0.84f))
    }
}

/** Dark Mode (Off) — Filled Crescent Moon */
@Composable
private fun MoonFilledIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val full = Path().apply {
            addOval(Rect(center = Offset(12f * s, 12.2f * s), radius = 8.6f * s))
        }
        val cutout = Path().apply {
            addOval(Rect(center = Offset(16.2f * s, 8.2f * s), radius = 7.4f * s))
        }
        val crescent = Path.combine(PathOperation.Difference, full, cutout)
        drawPath(path = crescent, color = color)
    }
}

/** Dark Mode (On) — Filled Amber Sun with 8 Rays */
@Composable
private fun SunFilledIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val center = Offset(12f * s, 12f * s)
        val brush = diagBrush(GradAmber)
        drawCircle(brush = brush, radius = 4.7f * s, center = center)
        for (i in 0 until 8) {
            rotate(degrees = i * 45f, pivot = center) {
                drawLine(
                    brush = brush,
                    start = Offset(12f * s, 2.5f * s),
                    end = Offset(12f * s, 4.9f * s),
                    strokeWidth = 2.1f * s,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/** Auxiliary Icons (Close X, Chevron Right) */
@Composable
private fun CloseXIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        drawLine(color, Offset(6f * s, 6f * s), Offset(18f * s, 18f * s), 2.3f * s, StrokeCap.Round)
        drawLine(color, Offset(18f * s, 6f * s), Offset(6f * s, 18f * s), 2.3f * s, StrokeCap.Round)
    }
}

@Composable
private fun ChevronRightIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 24f
        val path = Path().apply {
            moveTo(9f * s, 5.5f * s)
            lineTo(15.5f * s, 12f * s)
            lineTo(9f * s, 18.5f * s)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
