package com.chroma.studio.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.geometry.Offset
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
import com.chroma.studio.chat.ui.components.OneUiGradientStyle
import com.chroma.studio.ui.components.OneUiIconBadge
import com.chroma.studio.ui.components.OneUiSettingsGroup
import com.chroma.studio.ui.components.VerifiedBadge
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch

/**
 * Flagship Chroma Studio Post-Onboarding Personalization Wizard.
 * Fully redesigned following Samsung One UI 7.0 design principles:
 * - One UI 7.0 Viewing Area with light 34.sp typography
 * - Smooth 26.dp squircle group containers (OneUiSettingsGroup)
 * - One UI Pill segmented progress bar and Galaxy accent options
 * - Native One UI Material 3 switches and tactile feedback
 * - Interactive Studio Canvas monitor and curated creator network
 */
@Composable
fun PostOnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleDarkMode: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val isSysDark = isSystemInDarkTheme()
    val colors = LocalOscellionColors.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    // Wizard Step State (0: Studio Identity, 1: Privacy & Presence, 2: Creative Network)
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 3

    // Studio Accent Palettes (One UI & Galaxy Colors)
    val accentColors = remember {
        listOf(
            StudioAccentOption(Color(0xFF2563EB), "Galaxy Blue", "Galaxy Signature", "Vivid, focused, and futuristic"),
            StudioAccentOption(Color(0xFF6366F1), "One UI Indigo", "Fluid Flow", "Modern, ambient, and deep"),
            StudioAccentOption(Color(0xFF10B981), "Phantom Mint", "Natural Balance", "Crisp, organic, and energetic"),
            StudioAccentOption(Color(0xFFF59E0B), "Amber Glow", "Cinematic Warmth", "Luminous, golden, and bold"),
            StudioAccentOption(Color(0xFFF43F5E), "Ruby Rose", "Expressive Hue", "Vibrant, dynamic, and punchy")
        )
    }
    var selectedAccentIndex by remember {
        mutableIntStateOf(prefs.getInt("selected_accent_index", 0).coerceIn(0, accentColors.lastIndex))
    }
    val activeAccent = accentColors[selectedAccentIndex].color

    // Theme Mode State
    var isDarkMode by remember { mutableStateOf(prefs.getBoolean("is_dark_mode", colors.isDark)) }

    // Privacy & Presence States
    var isPresenceEnabled by remember { mutableStateOf(prefs.getBoolean("show_online_status", true)) }
    var isReadReceiptsEnabled by remember { mutableStateOf(prefs.getBoolean("read_receipts_enabled", true)) }
    var isTypingBroadcastEnabled by remember { mutableStateOf(prefs.getBoolean("typing_indicator_enabled", true)) }
    var isNotificationsEnabled by remember { mutableStateOf(prefs.getBoolean("push_notifications_dm", true)) }

    // Topics of Interest
    val creativeTopics = remember {
        listOf(
            "Color Grading",
            "Motion Graphics",
            "Sound Design",
            "Cinematic Reels",
            "3D & VFX",
            "Typography",
            "Analog 35mm",
            "Generative Art",
            "Portrait Lighting",
            "Spatial Audio"
        )
    }
    val selectedTopics = remember { mutableStateListOf("Color Grading", "Cinematic Reels", "Motion Graphics") }

    // Curated Creators
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

    // Persist and Finish
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

    val screenBg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF2F4F7)

    Scaffold(
        containerColor = screenBg,
        topBar = {
            OneUiOnboardingTopBar(
                currentStep = currentStep,
                totalSteps = totalSteps,
                activeAccent = activeAccent,
                isDark = isDarkMode,
                onBack = {
                    if (currentStep > 0) {
                        SoundManager.playReaction()
                        currentStep--
                    }
                },
                onSkip = { savePreferencesAndFinish() },
                onStepSelected = { target ->
                    if (target != currentStep) {
                        SoundManager.playReaction()
                        currentStep = target
                    }
                }
            )
        },
        bottomBar = {
            OneUiOnboardingBottomBar(
                currentStep = currentStep,
                totalSteps = totalSteps,
                activeAccent = activeAccent,
                isDark = isDarkMode,
                onBack = {
                    if (currentStep > 0) {
                        SoundManager.playReaction()
                        currentStep--
                    }
                },
                onNext = {
                    SoundManager.playReaction()
                    if (currentStep < totalSteps - 1) {
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
                    (slideInHorizontally(tween(320)) { width -> width / 3 } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(tween(300)) { width -> -width / 3 } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally(tween(320)) { width -> -width / 3 } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(tween(300)) { width -> width / 3 } + fadeOut(tween(200)))
                }
            },
            label = "one_ui_onboarding_content",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { step ->
            when (step) {
                0 -> OneUiAestheticStep(
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

                1 -> OneUiPrivacyStep(
                    activeAccent = activeAccent,
                    isDarkMode = isDarkMode,
                    isPresenceEnabled = isPresenceEnabled,
                    onPresenceChange = { isPresenceEnabled = it },
                    isReadReceiptsEnabled = isReadReceiptsEnabled,
                    onReadReceiptsChange = { isReadReceiptsEnabled = it },
                    isTypingBroadcastEnabled = isTypingBroadcastEnabled,
                    onTypingChange = { isTypingBroadcastEnabled = it },
                    isNotificationsEnabled = isNotificationsEnabled,
                    onNotificationsChange = { isNotificationsEnabled = it }
                )

                2 -> OneUiCommunityStep(
                    activeAccent = activeAccent,
                    isDarkMode = isDarkMode,
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
// STEP 1: ONE UI WORKSPACE AESTHETIC & THEME
// ============================================================================

@Composable
private fun OneUiAestheticStep(
    accentColors: List<StudioAccentOption>,
    selectedIndex: Int,
    activeAccent: Color,
    isDarkMode: Boolean,
    onSelectAccent: (Int) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // One UI 7.0 Viewing Area
        OneUiStepViewingArea(
            title = "Studio Identity",
            subtitle = "Personalize your workspace accent, theme, and 4K studio pipeline.",
            activeAccent = activeAccent,
            isDark = isDarkMode
        )

        // Live Studio Monitor Card (One UI 26dp Squircle)
        OneUiSettingsGroup(title = "Live Studio Monitor") {
            OneUiLiveStudioCanvas(
                activeAccent = activeAccent,
                isDarkMode = isDarkMode
            )
        }

        // Signature Accent Options (One UI 26dp Squircle)
        OneUiSettingsGroup(title = "Signature Accent") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                accentColors.forEachIndexed { index, option ->
                    val isSelected = index == selectedIndex
                    val itemBg = if (isSelected) {
                        option.color.copy(alpha = if (isDarkMode) 0.16f else 0.10f)
                    } else {
                        if (isDarkMode) Color(0xFF161A22) else Color.White
                    }
                    val itemBorder = if (isSelected) {
                        option.color.copy(alpha = 0.55f)
                    } else {
                        if (isDarkMode) Color(0xFF232A38) else Color.Transparent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(itemBg)
                            .border(1.dp, itemBorder, RoundedCornerShape(18.dp))
                            .clickable { onSelectAccent(index) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(option.color, option.color.copy(alpha = 0.8f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = option.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    fontFamily = InterFontFamily
                                )
                                Text(
                                    text = "${option.subtitle} • ${option.mood}",
                                    fontSize = 12.sp,
                                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontFamily = InterFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectAccent(index) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = option.color,
                                unselectedColor = if (isDarkMode) Color(0xFF475569) else Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }
        }

        // Workspace Appearance (Dark / Light)
        OneUiSettingsGroup(title = "Appearance") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dark Mode Card
                OneUiThemeOptionCard(
                    title = "Dark Mode",
                    subtitle = "AMOLED Cinema",
                    icon = Lucide.Moon,
                    isSelected = isDarkMode,
                    activeAccent = activeAccent,
                    isDark = isDarkMode,
                    onClick = { onToggleDarkMode(true) },
                    modifier = Modifier.weight(1f)
                )

                // Light Mode Card
                OneUiThemeOptionCard(
                    title = "Light Mode",
                    subtitle = "Crisp Canvas",
                    icon = Lucide.Sun,
                    isSelected = !isDarkMode,
                    activeAccent = activeAccent,
                    isDark = isDarkMode,
                    onClick = { onToggleDarkMode(false) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Samsung One UI 7.0 Theme Option Selection Card
 */
@Composable
private fun OneUiThemeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeAccent: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isSelected) {
        activeAccent.copy(alpha = if (isDark) 0.16f else 0.10f)
    } else {
        if (isDark) Color(0xFF161A22) else Color(0xFFF8FAFC)
    }
    val cardBorder = if (isSelected) {
        activeAccent.copy(alpha = 0.55f)
    } else {
        if (isDark) Color(0xFF232A38) else Color(0xFFE2E8F0)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier.height(86.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OneUiIconBadge(
                icon = icon,
                iconColor = if (isSelected) activeAccent else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontFamily = InterFontFamily
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

/**
 * Interactive Live Studio Canvas Monitor styled for Samsung One UI 7.0.
 */
@Composable
private fun OneUiLiveStudioCanvas(
    activeAccent: Color,
    isDarkMode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_anim")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
        label = "w3"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Monitor Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDarkMode) Color(0xFF0A0D14) else Color(0xFF0F172A))
                .border(1.dp, activeAccent.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            // Top HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Text(
                        text = "REC • 4K DCI 60FPS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        fontFamily = InterFontFamily
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = activeAccent.copy(alpha = 0.22f),
                    border = BorderStroke(0.5.dp, activeAccent.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "PRORES 422 HQ",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontFamily = InterFontFamily
                    )
                }
            }

            // Center Waveform & Timecode
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "00:00:18:24",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    letterSpacing = 1.sp,
                    fontFamily = InterFontFamily
                )

                // Animated Audio Bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bars = listOf(wave1, wave2, wave3, wave1 * 0.7f, wave2 * 1.1f, wave3 * 0.8f, wave1 * 0.9f)
                    bars.forEach { hRatio ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((hRatio.coerceIn(0.15f, 1f) * 22).dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(activeAccent)
                        )
                    }
                }
            }

            // Bottom Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timeline Master",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = InterFontFamily
                )
                Text(
                    text = "24-bit / 48kHz Stereo",
                    fontSize = 10.sp,
                    color = activeAccent,
                    fontFamily = InterFontFamily
                )
            }
        }

        // Hardware Specs Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("10-bit HDR", "Dual-Exo 4K", "Audio Tempo Sync").forEach { badge ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkMode) Color(0xFF161A22) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF232A38) else Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 7.dp),
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

// ============================================================================
// STEP 2: ONE UI COLLABORATION & PRIVACY
// ============================================================================

@Composable
private fun OneUiPrivacyStep(
    activeAccent: Color,
    isDarkMode: Boolean,
    isPresenceEnabled: Boolean,
    onPresenceChange: (Boolean) -> Unit,
    isReadReceiptsEnabled: Boolean,
    onReadReceiptsChange: (Boolean) -> Unit,
    isTypingBroadcastEnabled: Boolean,
    onTypingChange: (Boolean) -> Unit,
    isNotificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // One UI Viewing Area
        OneUiStepViewingArea(
            title = "Collaboration",
            subtitle = "Manage real-time presence, chat receipts, and hardware security.",
            activeAccent = activeAccent,
            isDark = isDarkMode
        )

        // Real-Time Switches Group (One UI 26dp Squircle)
        OneUiSettingsGroup(title = "Real-Time Collaboration") {
            OneUiSwitchRow(
                title = "Live Creative Presence",
                subtitle = "Show collaborators when you are active and editing in Studio",
                icon = Lucide.Globe,
                activeAccent = activeAccent,
                isDark = isDarkMode,
                checked = isPresenceEnabled,
                onCheckedChange = {
                    SoundManager.playReaction()
                    onPresenceChange(it)
                }
            )

            HorizontalDivider(color = if (isDarkMode) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)

            OneUiSwitchRow(
                title = "Read & Delivery Receipts",
                subtitle = "Display sent, delivered, and seen marks in direct chats",
                icon = Lucide.CheckCheck,
                activeAccent = activeAccent,
                isDark = isDarkMode,
                checked = isReadReceiptsEnabled,
                onCheckedChange = {
                    SoundManager.playReaction()
                    onReadReceiptsChange(it)
                }
            )

            HorizontalDivider(color = if (isDarkMode) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)

            OneUiSwitchRow(
                title = "Voice & Typing Broadcast",
                subtitle = "Broadcast real-time recording waveform and typing state",
                icon = Lucide.Mic,
                activeAccent = activeAccent,
                isDark = isDarkMode,
                checked = isTypingBroadcastEnabled,
                onCheckedChange = {
                    SoundManager.playReaction()
                    onTypingChange(it)
                }
            )

            HorizontalDivider(color = if (isDarkMode) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)

            OneUiSwitchRow(
                title = "Push Notifications",
                subtitle = "Instant alerts for mentions, direct messages, and project drops",
                icon = Lucide.Bell,
                activeAccent = activeAccent,
                isDark = isDarkMode,
                checked = isNotificationsEnabled,
                onCheckedChange = {
                    SoundManager.playReaction()
                    onNotificationsChange(it)
                }
            )
        }

        // Hardware Security Group
        OneUiSettingsGroup(title = "Hardware Security") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OneUiIconBadge(
                    icon = Lucide.ShieldCheck,
                    iconColor = Color(0xFF10B981)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Android Keystore Encryption",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                            fontFamily = InterFontFamily
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "ENCLAVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                    Text(
                        text = "Local drafts, API keys, and cache are hardware-isolated with Knox-grade protection.",
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontFamily = InterFontFamily
                    )
                }
            }

            HorizontalDivider(color = if (isDarkMode) Color(0xFF1F2430) else Color(0xFFE2E8F0), thickness = 0.5.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OneUiIconBadge(
                    icon = Lucide.HardDrive,
                    iconColor = activeAccent
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sandboxed Studio Scratchpad",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                        fontFamily = InterFontFamily
                    )
                    Text(
                        text = "Timeline video cache, frames, and audio waveforms remain strictly on-device.",
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontFamily = InterFontFamily
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Samsung One UI Settings Switch Row
 */
@Composable
private fun OneUiSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    activeAccent: Color,
    isDark: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 13.dp),
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
            OneUiIconBadge(
                icon = icon,
                iconColor = activeAccent
            )

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontFamily = InterFontFamily
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontFamily = InterFontFamily
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeAccent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

// ============================================================================
// STEP 3: ONE UI CURATED COMMUNITY & NETWORK
// ============================================================================

@Composable
private fun OneUiCommunityStep(
    activeAccent: Color,
    isDarkMode: Boolean,
    topics: List<String>,
    selectedTopics: List<String>,
    onToggleTopic: (String) -> Unit,
    creators: List<FeaturedCreatorItem>,
    followedIds: List<String>,
    onToggleFollow: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // One UI Viewing Area
        OneUiStepViewingArea(
            title = "Creative Network",
            subtitle = "Pick your artistic disciplines and follow featured visual creators.",
            activeAccent = activeAccent,
            isDark = isDarkMode
        )

        // Creative Interests (One UI 26dp Squircle)
        OneUiSettingsGroup(title = "Creative Topics") {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                topics.forEach { topic ->
                    val isSelected = topic in selectedTopics
                    val chipBg = if (isSelected) {
                        activeAccent.copy(alpha = if (isDarkMode) 0.18f else 0.12f)
                    } else {
                        if (isDarkMode) Color(0xFF161A22) else Color(0xFFF1F5F9)
                    }
                    val chipBorder = if (isSelected) {
                        activeAccent
                    } else {
                        if (isDarkMode) Color(0xFF232A38) else Color(0xFFE2E8F0)
                    }

                    Surface(
                        onClick = { onToggleTopic(topic) },
                        shape = RoundedCornerShape(14.dp),
                        color = chipBg,
                        border = BorderStroke(1.dp, chipBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = topic,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) activeAccent else (if (isDarkMode) Color.White else Color(0xFF0F172A)),
                                fontFamily = InterFontFamily
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
        }

        // Featured Artists (One UI 26dp Squircle)
        OneUiSettingsGroup(title = "Featured Artists") {
            Column(modifier = Modifier.fillMaxWidth()) {
                val avatarGradients = remember {
                    listOf(
                        listOf(Color(0xFF2563EB), Color(0xFF6366F1)),
                        listOf(Color(0xFF6366F1), Color(0xFFA855F7)),
                        listOf(Color(0xFF06B6D4), Color(0xFF10B981)),
                        listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                    )
                }

                creators.forEachIndexed { index, creator ->
                    val isFollowed = creator.id in followedIds

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Squircle Avatar (One UI 14dp)
                        if (creator.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = creator.avatarUrl,
                                contentDescription = creator.name,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, if (isDarkMode) Color(0xFF232A38) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            val grad = avatarGradients[index % avatarGradients.size]
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(grad)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = creator.name.take(1).uppercase(),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }

                        // Creator Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = creator.name,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = InterFontFamily
                                )
                                VerifiedBadge(size = 13.dp)
                            }
                            Text(
                                text = "${creator.handle} • ${creator.tagline}",
                                fontSize = 12.sp,
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = InterFontFamily
                            )
                        }

                        // One UI Follow Pill Button
                        Surface(
                            onClick = { onToggleFollow(creator.id) },
                            shape = RoundedCornerShape(999.dp),
                            color = if (isFollowed) {
                                if (isDarkMode) Color(0xFF1E2430) else Color.White
                            } else {
                                activeAccent
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isFollowed) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = null,
                                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = if (isFollowed) "Following" else "Follow",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isFollowed) {
                                        if (isDarkMode) Color.White else Color(0xFF0F172A)
                                    } else {
                                        Color.White
                                    },
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }

                    if (index < creators.lastIndex) {
                        HorizontalDivider(
                            color = if (isDarkMode) Color(0xFF1F2430) else Color(0xFFE2E8F0),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 72.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ============================================================================
// ONE UI STEP VIEWING AREA (LIGHT 34SP TYPOGRAPHY)
// ============================================================================

@Composable
private fun OneUiStepViewingArea(
    title: String,
    subtitle: String,
    activeAccent: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                letterSpacing = (-0.5).sp,
                fontFamily = InterFontFamily
            )

            Text(
                text = subtitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontFamily = InterFontFamily,
                lineHeight = 19.sp
            )
        }
    }
}

// ============================================================================
// ONE UI TOP & BOTTOM NAVIGATION BARS
// ============================================================================

@Composable
private fun OneUiOnboardingTopBar(
    currentStep: Int,
    totalSteps: Int,
    activeAccent: Color,
    isDark: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onStepSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Action / Back Button
            if (currentStep > 0) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Lucide.ChevronLeft,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(activeAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Sparkles,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "CHROMA STUDIO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        letterSpacing = 0.8.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }

            // Step Indicator Text
            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontFamily = InterFontFamily
            )

            // Right Skip Action
            TextButton(
                onClick = onSkip,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Skip",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontFamily = InterFontFamily
                )
            }
        }

        // Segmented Pill Progress Indicator (Samsung One UI 7.0 style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until totalSteps) {
                val isCompletedOrActive = i <= currentStep
                val barColor = if (isCompletedOrActive) activeAccent else (if (isDark) Color(0xFF1E2430) else Color(0xFFE2E8F0))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(barColor)
                        .clickable(enabled = i < currentStep) {
                            onStepSelected(i)
                        }
                )
            }
        }
    }
}

@Composable
private fun OneUiOnboardingBottomBar(
    currentStep: Int,
    totalSteps: Int,
    activeAccent: Color,
    isDark: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val isLastStep = currentStep == totalSteps - 1
    val nextStepNames = listOf("Collaboration", "Creative Network", "Launch Studio")
    val nextLabel = nextStepNames.getOrElse(currentStep) { "Continue" }

    val barBg = if (isDark) Color(0xFF000000).copy(alpha = 0.95f) else Color(0xFFF2F4F7).copy(alpha = 0.96f)

    Surface(
        color = barBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentStep > 0) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(26.dp),
                    color = if (isDark) Color(0xFF161A22) else Color(0xFFE2E8F0),
                    modifier = Modifier
                        .weight(0.32f)
                        .height(50.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Back",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }

            // Primary Next CTA Button (Samsung One UI 26dp Squircle)
            Surface(
                onClick = onNext,
                shape = RoundedCornerShape(26.dp),
                color = activeAccent,
                modifier = Modifier
                    .weight(if (currentStep > 0) 0.68f else 1f)
                    .height(50.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isLastStep) "Launch Studio Workspace" else "Next: $nextLabel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontFamily = InterFontFamily
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isLastStep) Lucide.Rocket else Lucide.ArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// DATA MODELS & DEFAULT CREATORS
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
        tagline = "Cinema Colorist • DaVinci & ARRI 35mm LUTs",
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
