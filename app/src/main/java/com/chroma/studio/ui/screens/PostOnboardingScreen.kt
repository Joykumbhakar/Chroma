package com.chroma.studio.ui.screens

import com.chroma.studio.ui.components.VerifiedBadge

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.components.AppleFlashGraphic
import com.chroma.studio.ui.components.AppleSwitch
import com.chroma.studio.ui.components.rememberAppleGraphicAnimationState
import com.chroma.studio.ui.graphics3d.Apple3DGraphicIcon
import com.chroma.studio.ui.graphics3d.Apple3DGraphicId
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch

/**
 * Flagship Chroma Studio Post-Onboarding Personalization Wizard.
 * Features:
 * - 3-Step Guided Setup: Aesthetic & Theme -> Privacy & Collaboration -> Curated Inspiration
 * - Dynamic Live Studio Canvas preview that updates in real-time with selected accent tint
 * - 100% Theme-accurate glassmorphic styling (zero white-on-white contrast bugs)
 * - Tactile Apple-grade micro-interactions with smooth spring feedback and audio cues
 * - Curated creators discovery with instant follow toggles and fallback resilience
 */
@Composable
fun PostOnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleDarkMode: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    // ── Wizard Step State (0: Aesthetic, 1: Privacy, 2: Community) ───
    var currentStep by remember { mutableIntStateOf(0) }

    // ── Studio Accent Palettes ───────────────────────────────────────
    val accentColors = remember {
        listOf(
            StudioAccentOption(Color(0xFF0A84FF), "Electric Blue", "Cupertino & Precision", "Clean, focused, and crisp"),
            StudioAccentOption(Color(0xFF6366F1), "Studio Indigo", "Creative Flow", "Modern, deep, and ambient"),
            StudioAccentOption(Color(0xFF10B981), "Emerald Mint", "Organic Balance", "Fresh, energetic, and vivid"),
            StudioAccentOption(Color(0xFFF59E0B), "Solar Amber", "Cinematic Warmth", "Luminous, golden, and bold"),
            StudioAccentOption(Color(0xFFF43F5E), "Neon Rose", "Cinematic Drama", "Punchy, expressive, and radiant")
        )
    }
    var selectedAccentIndex by remember {
        mutableIntStateOf(prefs.getInt("selected_accent_index", 0).coerceIn(0, accentColors.lastIndex))
    }
    val activeAccent = accentColors[selectedAccentIndex].color

    // ── Theme State ──────────────────────────────────────────────────
    var isDarkMode by remember { mutableStateOf(prefs.getBoolean("is_dark_mode", colors.isDark)) }

    // ── Real-Time & Privacy Preferences ──────────────────────────────
    var isPresenceEnabled by remember { mutableStateOf(prefs.getBoolean("show_online_status", true)) }
    var isReadReceiptsEnabled by remember { mutableStateOf(prefs.getBoolean("read_receipts_enabled", true)) }
    var isTypingBroadcastEnabled by remember { mutableStateOf(prefs.getBoolean("typing_indicator_enabled", true)) }
    var isNotificationsEnabled by remember { mutableStateOf(prefs.getBoolean("push_notifications_dm", true)) }

    // ── Creative Disciplines / Topics ────────────────────────────────
    val creativeTopics = remember {
        listOf(
            "Color Grading",
            "Motion Graphics",
            "Sound Design",
            "Cinematic Reels",
            "3D & VFX",
            "Typography",
            "Analog 35mm",
            "Generative Art"
        )
    }
    val selectedTopics = remember { mutableStateListOf("Color Grading", "Cinematic Reels", "Motion Graphics") }

    // ── Recommended Creators Discovery ───────────────────────────────
    val discoverRepo = remember { com.chroma.studio.data.DiscoverRepository() }
    var featuredCreators by remember { mutableStateOf<List<FeaturedCreatorItem>>(emptyList()) }
    val followedCreatorIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        try {
            val users = discoverRepo.getSuggestedCreators(8)
            if (users.isNotEmpty()) {
                featuredCreators = users.map { u ->
                    FeaturedCreatorItem(
                        id = u.uid,
                        name = u.displayName.ifBlank { u.username.ifBlank { "Visual Creator" } },
                        handle = if (u.username.isNotBlank()) "@${u.username}" else "@creator",
                        tagline = if (u.bio.isNotBlank()) u.bio else "Digital Artist & Visual Storyteller",
                        avatarUrl = u.avatarUrl
                    )
                }
            } else {
                featuredCreators = defaultCuratedCreators
            }
        } catch (_: Exception) {
            featuredCreators = defaultCuratedCreators
        }
    }

    // Helper to persist preferences and finish
    fun savePreferencesAndFinish() {
        SoundManager.playReaction()
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null && followedCreatorIds.isNotEmpty()) {
            for (cId in followedCreatorIds) {
                com.chroma.studio.data.mutation.OptimisticFollowManager.toggleFollow(
                    currentUserId = currentUid,
                    targetUserId = cId,
                    baseFollowing = false
                )
            }
        }
        prefs.edit().apply {
            putBoolean("show_online_status", isPresenceEnabled)
            putBoolean("read_receipts_enabled", isReadReceiptsEnabled)
            putBoolean("typing_indicator_enabled", isTypingBroadcastEnabled)
            putBoolean("push_notifications_dm", isNotificationsEnabled)
            putBoolean("push_notifications_drops", isNotificationsEnabled)
            putInt("selected_accent_index", selectedAccentIndex)
            putInt("studio_accent_color", activeAccent.toArgb())
            putBoolean("is_dark_mode", isDarkMode)
            putBoolean("has_completed_post_onboarding", true)
            apply()
        }
        onFinish()
    }

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            OnboardingTopBar(
                currentStep = currentStep,
                totalSteps = 3,
                activeAccent = activeAccent,
                onStepSelected = { targetStep ->
                    if (targetStep != currentStep) {
                        SoundManager.playReaction()
                        currentStep = targetStep
                    }
                },
                onBack = {
                    if (currentStep > 0) {
                        SoundManager.playReaction()
                        currentStep--
                    }
                },
                onSkip = { savePreferencesAndFinish() }
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                currentStep = currentStep,
                totalSteps = 3,
                activeAccent = activeAccent,
                onBack = {
                    if (currentStep > 0) {
                        SoundManager.playReaction()
                        currentStep--
                    }
                },
                onNext = {
                    SoundManager.playReaction()
                    if (currentStep < 2) {
                        currentStep++
                    } else {
                        savePreferencesAndFinish()
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width / 2 } + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut(animationSpec = tween(240)))
                } else {
                    (slideInHorizontally { width -> -width / 2 } + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutHorizontally { width -> width / 2 } + fadeOut(animationSpec = tween(240)))
                }
            },
            label = "onboarding_step_content",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { step ->
            when (step) {
                0 -> AestheticStepContent(
                    accentColors = accentColors,
                    selectedIndex = selectedAccentIndex,
                    activeAccent = activeAccent,
                    isDarkMode = isDarkMode,
                    onSelectAccent = { index ->
                        SoundManager.playReaction()
                        selectedAccentIndex = index
                    },
                    onToggleDarkMode = { dark ->
                        SoundManager.playReaction()
                        isDarkMode = dark
                        onToggleDarkMode?.invoke(dark)
                        prefs.edit().putBoolean("is_dark_mode", dark).apply()
                    }
                )

                1 -> PrivacyStepContent(
                    activeAccent = activeAccent,
                    isPresenceEnabled = isPresenceEnabled,
                    onPresenceChange = { isPresenceEnabled = it },
                    isReadReceiptsEnabled = isReadReceiptsEnabled,
                    onReadReceiptsChange = { isReadReceiptsEnabled = it },
                    isTypingBroadcastEnabled = isTypingBroadcastEnabled,
                    onTypingChange = { isTypingBroadcastEnabled = it },
                    isNotificationsEnabled = isNotificationsEnabled,
                    onNotificationsChange = { isNotificationsEnabled = it }
                )

                2 -> CommunityStepContent(
                    activeAccent = activeAccent,
                    topics = creativeTopics,
                    selectedTopics = selectedTopics,
                    onToggleTopic = { topic ->
                        SoundManager.playReaction()
                        if (topic in selectedTopics) selectedTopics.remove(topic)
                        else selectedTopics.add(topic)
                    },
                    creators = featuredCreators,
                    followedIds = followedCreatorIds,
                    onToggleFollow = { creatorId ->
                        SoundManager.playReaction()
                        if (creatorId in followedCreatorIds) followedCreatorIds.remove(creatorId)
                        else followedCreatorIds.add(creatorId)
                    }
                )
            }
        }
    }
}

// ============================================================================
// STEP 1: WORKSPACE AESTHETIC & THEME
// ============================================================================

@Composable
private fun AestheticStepContent(
    accentColors: List<StudioAccentOption>,
    selectedIndex: Int,
    activeAccent: Color,
    isDarkMode: Boolean,
    onSelectAccent: (Int) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Eyebrow & Title
        OnboardingHeaderBadge(
            icon = Lucide.Palette,
            accentColor = activeAccent,
            text = "STEP 1 OF 3 • STUDIO IDENTITY"
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Define Your Aesthetic",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMain,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.6).sp
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Pick your signature studio accent and display theme. Timelines, audio meters, waveforms, and tools adapt in real-time.",
            fontSize = 14.5.sp,
            lineHeight = 21.sp,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(24.dp))

        // ── Interactive Live Mini Studio Mockup ──
        LiveStudioPreviewCard(
            activeAccent = activeAccent,
            isDarkMode = isDarkMode
        )

        Spacer(Modifier.height(14.dp))

        // ── Pro Video Engine Hardware Pipeline Card ──
        ProVideoEngineBannerCard(
            activeAccent = activeAccent
        )

        Spacer(Modifier.height(24.dp))

        // ── Accent Color Swatches ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SIGNATURE ACCENT",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 1.sp
            )

            Text(
                text = accentColors[selectedIndex].title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = activeAccent
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            accentColors.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                val animatedScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "swatch_scale"
                )

                Surface(
                    onClick = { onSelectAccent(index) },
                    shape = RoundedCornerShape(10.dp),
                    color = colors.glassBg,
                    border = BorderStroke(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) option.color else colors.glassBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Colored Core
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 34.dp else 30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            option.color,
                                            option.color.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                                .shadow(
                                    elevation = if (isSelected) 8.dp else 2.dp,
                                    shape = RoundedCornerShape(10.dp),
                                    spotColor = option.color
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Studio Theme Mode (Dark / Light) ──
        Text(
            text = "WORKSPACE APPEARANCE",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMuted,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dark Mode Card
            ThemeSelectionCard(
                title = "Obsidian Dark",
                subtitle = "Deep cinema contrast",
                icon = Lucide.Moon,
                isSelected = isDarkMode,
                accentColor = activeAccent,
                onClick = { onToggleDarkMode(true) },
                modifier = Modifier.weight(1f)
            )

            // Light Mode Card
            ThemeSelectionCard(
                title = "Pure Light",
                subtitle = "Crisp gallery canvas",
                icon = Lucide.Sun,
                isSelected = !isDarkMode,
                accentColor = activeAccent,
                onClick = { onToggleDarkMode(false) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

/**
 * Interactive Live Mini Studio Preview Card.
 * Renders an animated miniature Chroma Studio workspace reflecting the chosen accent color,
 * featuring an articulated AppleFlashGraphic director clapperboard and 35mm filmstrip reel styling.
 */
@Composable
private fun LiveStudioPreviewCard(
    activeAccent: Color,
    isDarkMode: Boolean
) {
    val colors = LocalOscellionColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_preview")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.glassBg,
        border = BorderStroke(1.dp, colors.glassBorder),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Ambient Radial Accent Glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                activeAccent.copy(alpha = if (isDarkMode) 0.16f else 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Top Mini Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(activeAccent)
                        )
                        Text(
                            text = "CHROMA TIMELINE 4K",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Mini Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = activeAccent.copy(alpha = 0.14f),
                            border = BorderStroke(0.5.dp, activeAccent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "PRORES 422",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = activeAccent,
                            modifier = Modifier.height(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Share2,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Export",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Mini Viewport Preview Screen with Articulated Clapperboard & Reel Notches
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    activeAccent.copy(alpha = 0.20f),
                                    colors.glassBgHover
                                )
                            )
                        )
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                ) {
                    // 35mm Filmstrip Reel Perforation Notches (Top & Bottom)
                    FilmstripNotches(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        color = colors.glassBorder
                    )
                    FilmstripNotches(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        color = colors.glassBorder
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Articulated Director Clapperboard Graphic
                        val clapperState = rememberAppleGraphicAnimationState()
                        AppleFlashGraphic(
                            modifier = Modifier.size(62.dp),
                            state = clapperState,
                            enableClickToAnimate = true
                        )

                        // Studio Video Monitor Overlay HUD
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3B30))
                                )
                                Text(
                                    text = "REC • 4K DCI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "00:00:18:24 • 24 FPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activeAccent
                            )
                            Text(
                                text = "SCENE 01 • TAKE 04",
                                fontSize = 9.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Mini Multi-Track Timeline with Playhead
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.bg)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Video Track Block
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(activeAccent.copy(alpha = 0.28f))
                            .border(1.dp, activeAccent.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.dp)
                                .background(activeAccent)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "CLIP_A001.MOV",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeAccent
                        )
                    }

                    // Audio Waveform Track
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(10.dp)
                            .align(Alignment.BottomStart),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val heights = listOf(0.4f, 0.9f, 0.6f, 0.3f, 0.7f, 1f, 0.5f, 0.8f, 0.4f, 0.9f, 0.7f, 0.5f, 0.8f, 1f, 0.6f)
                        heights.forEach { h ->
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .fillMaxHeight(h)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(activeAccent.copy(alpha = pulseAlpha))
                            )
                        }
                    }

                    // Glowing Animated Playhead Line
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .offset(x = 90.dp)
                            .background(activeAccent)
                    )
                }
            }
        }
    }
}

/**
 * 35mm Filmstrip Reel Perforation Notches.
 */
@Composable
private fun FilmstripNotches(
    modifier: Modifier = Modifier,
    color: Color
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(12) {
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = 2.5.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color.copy(alpha = 0.45f))
            )
        }
    }
}

/**
 * Pro Video Engine Hardware Pipeline Card showcasing ChromaStudio's video prowess.
 */
@Composable
private fun ProVideoEngineBannerCard(
    activeAccent: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.glassBg,
        border = BorderStroke(1.dp, colors.glassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(activeAccent.copy(alpha = 0.12f))
                    .border(1.dp, activeAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Apple3DGraphicIcon(
                    id = Apple3DGraphicId.FLASH_VIDEO,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CHROMA HARDWARE VIDEO PIPELINE",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeAccent,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Dual-Exo 4K Timeline & ProRes 422",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "25 cinematic video transitions rendered with 10-bit color accuracy and live audio tempo sync.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("4K 60FPS", "10-bit HDR", "ProRes 422 HQ").forEach { badge ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.bg,
                            border = BorderStroke(0.5.dp, colors.glassBorder)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = colors.glassBg,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else colors.glassBorder
        ),
        modifier = modifier.height(84.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) accentColor.copy(alpha = 0.16f) else colors.bg)
                    .border(
                        1.dp,
                        if (isSelected) accentColor.copy(alpha = 0.4f) else colors.glassBorder,
                        RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) accentColor else colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}

// ============================================================================
// STEP 2: REAL-TIME PRESENCE & PRIVACY
// ============================================================================

@Composable
private fun PrivacyStepContent(
    activeAccent: Color,
    isPresenceEnabled: Boolean,
    onPresenceChange: (Boolean) -> Unit,
    isReadReceiptsEnabled: Boolean,
    onReadReceiptsChange: (Boolean) -> Unit,
    isTypingBroadcastEnabled: Boolean,
    onTypingChange: (Boolean) -> Unit,
    isNotificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Eyebrow & Title
        OnboardingHeaderBadge(
            icon = Lucide.Shield,
            accentColor = activeAccent,
            text = "STEP 2 OF 3 • COLLABORATION & PRIVACY"
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Presence & Security",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMain,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.6).sp
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Fine-tune how you collaborate with peers. Activity updates synchronize instantly across low-latency Firebase channels.",
            fontSize = 14.5.sp,
            lineHeight = 21.sp,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(26.dp))

        // ── 4 Dedicated Clean Switch Cards (Zero White-on-White Bugs) ──
        StudioToggleCard(
            title = "Live Creative Presence",
            subtitle = "Show collaborators when you are active and editing in Studio",
            graphicId = Apple3DGraphicId.NETWORK_GLOBE,
            accentColor = activeAccent,
            checked = isPresenceEnabled,
            onCheckedChange = {
                SoundManager.playReaction()
                onPresenceChange(it)
            }
        )

        Spacer(Modifier.height(12.dp))

        StudioToggleCard(
            title = "Read & Delivery Receipts",
            subtitle = "Display sent, delivered, and seen marks in direct chats",
            graphicId = Apple3DGraphicId.CHAT_BUBBLE,
            accentColor = activeAccent,
            checked = isReadReceiptsEnabled,
            onCheckedChange = {
                SoundManager.playReaction()
                onReadReceiptsChange(it)
            }
        )

        Spacer(Modifier.height(12.dp))

        StudioToggleCard(
            title = "Voice & Typing Broadcast",
            subtitle = "Broadcast real-time recording waveform and typing state",
            graphicId = Apple3DGraphicId.STUDIO_MIC,
            accentColor = activeAccent,
            checked = isTypingBroadcastEnabled,
            onCheckedChange = {
                SoundManager.playReaction()
                onTypingChange(it)
            }
        )

        Spacer(Modifier.height(12.dp))

        StudioToggleCard(
            title = "Smart Push Notifications",
            subtitle = "Alerts for mentions, direct messages, and creator project drops",
            graphicId = Apple3DGraphicId.SPATIAL_BELL,
            accentColor = activeAccent,
            checked = isNotificationsEnabled,
            onCheckedChange = {
                SoundManager.playReaction()
                onNotificationsChange(it)
            }
        )

        Spacer(Modifier.height(16.dp))

        // ── Hardware Encryption Chip ──
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.glassBg,
            border = BorderStroke(1.dp, colors.success.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.success.copy(alpha = 0.14f))
                        .border(1.dp, colors.success.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Apple3DGraphicIcon(
                        id = Apple3DGraphicId.PROTECTION_SHIELD,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Android Keystore Encryption",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMain
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = colors.success.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, colors.success.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "ENCLAVE",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.success,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Drafts, API tokens, and local cache are secured with hardware-isolated cryptography.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = colors.textMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Sandboxed Video Scratch Storage Card ──
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.glassBg,
            border = BorderStroke(1.dp, colors.glassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(activeAccent.copy(alpha = 0.12f))
                        .border(1.dp, activeAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Apple3DGraphicIcon(
                        id = Apple3DGraphicId.NVME_DRIVE,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Private Video Scratch Storage",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Timeline video frames, audio waveforms, and temporary export buffers remain strictly on-device in sandboxed cache.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = colors.textMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun StudioToggleCard(
    title: String,
    subtitle: String,
    graphicId: Apple3DGraphicId? = null,
    icon: ImageVector? = null,
    accentColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current

    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(10.dp),
        color = colors.glassBg,
        border = BorderStroke(1.dp, colors.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.16f),
                                    accentColor.copy(alpha = 0.06f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            accentColor.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (graphicId != null) {
                        Apple3DGraphicIcon(
                            id = graphicId,
                            modifier = Modifier.size(32.dp)
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                        color = colors.textMuted
                    )
                }
            }

            AppleSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                activeTrackColor = accentColor
            )
        }
    }
}

// ============================================================================
// STEP 3: DISCOVER & CREATIVE NETWORK
// ============================================================================

@Composable
private fun CommunityStepContent(
    activeAccent: Color,
    topics: List<String>,
    selectedTopics: List<String>,
    onToggleTopic: (String) -> Unit,
    creators: List<FeaturedCreatorItem>,
    followedIds: List<String>,
    onToggleFollow: (String) -> Unit
) {
    val colors = LocalOscellionColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Eyebrow & Title
        OnboardingHeaderBadge(
            icon = Lucide.Sparkles,
            accentColor = activeAccent,
            text = "STEP 3 OF 3 • CURATED INSPIRATION"
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Curate Your Network",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMain,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.6).sp
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Select your creative interests and connect with visual artists across the Chroma community.",
            fontSize = 14.5.sp,
            lineHeight = 21.sp,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(20.dp))

        // ── Featured Director Clapperboard & Reel Spotlight ──
        FeaturedDirectorShowcaseBanner(activeAccent = activeAccent)

        Spacer(Modifier.height(22.dp))

        // ── Creative Disciplines Topic Tags ──
        Text(
            text = "CREATIVE INTERESTS",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMuted,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topics.forEach { topic ->
                val isSelected = topic in selectedTopics
                Surface(
                    onClick = { onToggleTopic(topic) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) activeAccent.copy(alpha = 0.16f) else colors.glassBg,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) activeAccent else colors.glassBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = topic,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) activeAccent else colors.textMain
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                tint = activeAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        // ── Suggested Creators List ──
        Text(
            text = "FEATURED CREATORS",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMuted,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            creators.forEach { creator ->
                val isFollowed = creator.id in followedIds

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.glassBg,
                    border = BorderStroke(1.dp, colors.glassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar with Fallback
                        if (creator.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = creator.avatarUrl,
                                contentDescription = creator.name,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(colors.bg),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.User,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Creator Name, Handle & Tagline
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = creator.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMain,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                VerifiedBadge(size = 13.dp)
                            }

                            Text(
                                text = "${creator.handle} • ${creator.tagline}",
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(4.dp))

                            // Mini Video Reel Chip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = activeAccent.copy(alpha = 0.10f),
                                border = BorderStroke(0.5.dp, activeAccent.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.Film,
                                        contentDescription = null,
                                        tint = activeAccent,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "${creator.featuredReelTitle} • ${creator.reelDuration}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = activeAccent
                                    )
                                }
                            }
                        }

                        // Follow Action Button
                        val btnBg by animateColorAsState(
                            targetValue = if (isFollowed) colors.bg else activeAccent,
                            animationSpec = tween(180),
                            label = "btnBg"
                        )
                        val btnBorderColor = if (isFollowed) colors.glassBorder else Color.Transparent
                        val btnTextColor = if (isFollowed) colors.textMain else Color.White

                        Surface(
                            onClick = { onToggleFollow(creator.id) },
                            shape = RoundedCornerShape(10.dp),
                            color = btnBg,
                            border = BorderStroke(1.dp, btnBorderColor),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isFollowed) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = null,
                                        tint = btnTextColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = if (isFollowed) "Following" else "Follow",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = btnTextColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

// ============================================================================
// TOP & BOTTOM BARS WITH SAFE INSETS & LABELED CAPSULES
// ============================================================================

@Composable
private fun OnboardingTopBar(
    currentStep: Int,
    totalSteps: Int,
    activeAccent: Color,
    onStepSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val stepNames = listOf("Aesthetic", "Privacy", "Creators")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Navigation Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Action / Back Button or Logo
            if (currentStep > 0) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp),
                    color = colors.glassBg,
                    border = BorderStroke(1.dp, colors.glassBorder),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Lucide.ChevronLeft,
                            contentDescription = "Back",
                            tint = colors.textMain,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Film,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "CHROMA STUDIO",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        letterSpacing = 1.1.sp
                    )
                }
            }

            // Step Progress Badge in Center
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = activeAccent.copy(alpha = 0.10f),
                border = BorderStroke(0.5.dp, activeAccent.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "STEP ${currentStep + 1} OF $totalSteps",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeAccent,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Right "Skip" Action
            TextButton(
                onClick = onSkip,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Skip",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted
                )
            }
        }

        // Labeled Step Indicator Capsules / Segments Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until totalSteps) {
                val isActive = i == currentStep
                val isCompleted = i < currentStep
                val isClickable = i <= currentStep

                val segmentBorderColor by animateColorAsState(
                    targetValue = when {
                        isActive -> activeAccent.copy(alpha = 0.8f)
                        isCompleted -> colors.glassBorder
                        else -> colors.glassBorder.copy(alpha = 0.4f)
                    },
                    animationSpec = tween(240),
                    label = "seg_border"
                )

                val segmentBgColor by animateColorAsState(
                    targetValue = when {
                        isActive -> activeAccent.copy(alpha = 0.14f)
                        isCompleted -> colors.glassBg
                        else -> colors.glassBg.copy(alpha = 0.45f)
                    },
                    animationSpec = tween(240),
                    label = "seg_bg"
                )

                Surface(
                    onClick = { if (isClickable) onStepSelected(i) },
                    enabled = isClickable,
                    shape = RoundedCornerShape(12.dp),
                    color = segmentBgColor,
                    border = BorderStroke(if (isActive) 1.5.dp else 1.dp, segmentBorderColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                tint = activeAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isActive) activeAccent else colors.glassBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${i + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else colors.textMuted
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                        }

                        Text(
                            text = stepNames.getOrElse(i) { "Step ${i + 1}" },
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isActive -> activeAccent
                                isCompleted -> colors.textMain
                                else -> colors.textMuted
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    currentStep: Int,
    totalSteps: Int,
    activeAccent: Color,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val isLastStep = currentStep == totalSteps - 1
    val nextStepNames = listOf("Privacy & Presence", "Curated Community", "Launch Studio")
    val nextLabel = nextStepNames.getOrElse(currentStep) { "Continue" }

    Surface(
        color = colors.glassBg.copy(alpha = 0.96f),
        border = BorderStroke(0.5.dp, colors.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 0) {
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(16.dp),
                        color = colors.glassBg,
                        border = BorderStroke(1.dp, colors.glassBorder),
                        modifier = Modifier
                            .weight(0.32f)
                            .height(52.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.ChevronLeft,
                                    contentDescription = null,
                                    tint = colors.textMain,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Back",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMain
                                )
                            }
                        }
                    }
                }

                // Primary Next / Launch CTA Button
                Surface(
                    onClick = onNext,
                    shape = RoundedCornerShape(16.dp),
                    color = activeAccent,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .weight(if (currentStep > 0) 0.68f else 1f)
                        .height(52.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        activeAccent,
                                        activeAccent.copy(alpha = 0.85f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isLastStep) "Launch Studio Workspace" else "Next: $nextLabel",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isLastStep) Lucide.Rocket else Lucide.ArrowRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Community Reel Spotlight Banner showcasing director reels.
 */
@Composable
private fun FeaturedDirectorShowcaseBanner(
    activeAccent: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val clapperState = rememberAppleGraphicAnimationState()

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.glassBg,
        border = BorderStroke(1.dp, colors.glassBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppleFlashGraphic(
                modifier = Modifier.size(68.dp),
                state = clapperState,
                enableClickToAnimate = true
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "COMMUNITY REEL SPOTLIGHT",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeAccent,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Trending Director Reels & LUTs",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Connect with cinema colorists and visual directors sharing ProRes presets and open timeline clips.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}

@Composable
private fun OnboardingHeaderBadge(
    icon: ImageVector? = null,
    accentColor: Color,
    text: String
) {
    // Professional clean uppercase eyebrow (No Pill Capsule, No Icon, Clean Letter Spacing)
    Text(
        text = text,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        color = accentColor,
        letterSpacing = 1.2.sp
    )
}

// ============================================================================
// DATA MODELS & DEFAULT FALLBACK CREATORS
// ============================================================================

private data class StudioAccentOption(
    val color: Color,
    val title: String,
    val subtitle: String,
    val mood: String
)

data class FeaturedCreatorItem(
    val id: String,
    val name: String,
    val handle: String,
    val tagline: String,
    val avatarUrl: String,
    val featuredReelTitle: String = "4K Cinematic Reel",
    val reelDuration: String = "0:30"
)

private val defaultCuratedCreators = listOf(
    FeaturedCreatorItem(
        id = "creator_elena",
        name = "Elena Rostova",
        handle = "@elena_color",
        tagline = "Cinema Colorist • ARRI & DaVinci LUTs",
        avatarUrl = "",
        featuredReelTitle = "Teal & Orange ARRI 35mm LUT",
        reelDuration = "0:45"
    ),
    FeaturedCreatorItem(
        id = "creator_marcus",
        name = "Marcus Vance",
        handle = "@marcusvfx",
        tagline = "3D Lighting & Procedural Motion",
        avatarUrl = "",
        featuredReelTitle = "Procedural Kinetic Spark FX",
        reelDuration = "0:30"
    ),
    FeaturedCreatorItem(
        id = "creator_aria",
        name = "Aria Tanaka",
        handle = "@ariatimeline",
        tagline = "Visual Storyteller & Editorial Director",
        avatarUrl = "",
        featuredReelTitle = "Tokyo Neon Night Editorial",
        reelDuration = "0:52"
    ),
    FeaturedCreatorItem(
        id = "creator_devon",
        name = "Devon Reed",
        handle = "@devonaudio",
        tagline = "Cinematic Soundscapes & Spatial Foley",
        avatarUrl = "",
        featuredReelTitle = "Dolby Atmos Spatial Foley Mix",
        reelDuration = "1:14"
    )
)
