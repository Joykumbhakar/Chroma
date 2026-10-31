package com.chroma.studio.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.components.OneUiGradientStyle
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.components.ChromaOneUiHeaderBackdrop
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AccountCenterScreen(
    onNavigateToProfileIdentity: (UserProfile) -> Unit,
    onNavigateToSecurity: (UserProfile) -> Unit,
    onNavigateToPrivacy: (UserProfile) -> Unit,
    onNavigateToNotifications: (UserProfile) -> Unit,
    onNavigateToArchives: (UserProfile) -> Unit,
    onNavigateToActivityHistory: (UserProfile) -> Unit,
    onNavigateToPrivacyTerms: () -> Unit = {},
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // One UI Theme Synchronization
    val prefs = remember { context.getSharedPreferences("chat_ui_preferences", Context.MODE_PRIVATE) }
    var gradientStyle by remember {
        val savedName = prefs.getString("one_ui_gradient_style", OneUiGradientStyle.GALAXY_AI.name)
        mutableStateOf(
            try {
                OneUiGradientStyle.valueOf(savedName ?: "GALAXY_AI")
            } catch (_: Exception) {
                OneUiGradientStyle.GALAXY_AI
            }
        )
    }

    fun cycleGradientStyle() {
        val styles = OneUiGradientStyle.entries
        val next = styles[(styles.indexOf(gradientStyle) + 1) % styles.size]
        gradientStyle = next
        prefs.edit().putString("one_ui_gradient_style", next.name).apply()
        SoundManager.playReaction()
    }

    // One UI Collapsing Engine
    val expandedHeightDp: Dp = 190.dp
    val maxCollapsePx = remember(density) { with(density) { expandedHeightDp.toPx() } }
    var collapseOffsetPx by remember { mutableFloatStateOf(0f) }

    val collapseProgress = remember(collapseOffsetPx, maxCollapsePx) {
        if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx).coerceIn(0f, 1f)
    }

    val largeTitleAlpha = (1f - (collapseProgress / 0.45f)).coerceIn(0f, 1f)
    val subtitleAlpha = (1f - (collapseProgress / 0.35f)).coerceIn(0f, 1f)
    val badgeAlpha = (1f - (collapseProgress / 0.30f)).coerceIn(0f, 1f)
    val compactTitleAlpha = ((collapseProgress - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val gradientAlpha = if (gradientStyle == OneUiGradientStyle.SOLID) 0f else (1f - (collapseProgress / 0.85f)).coerceIn(0f, 1f)

    fun animateSnapTo(targetFraction: Float) {
        val targetPx = (targetFraction.coerceIn(0f, 1f)) * maxCollapsePx
        coroutineScope.launch {
            animate(
                initialValue = collapseOffsetPx,
                targetValue = targetPx,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { value, _ ->
                collapseOffsetPx = value
            }
        }
    }

    val nestedScrollConnection = remember(maxCollapsePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                if (deltaY < 0f && collapseOffsetPx < maxCollapsePx) {
                    val newOffset = (collapseOffsetPx - deltaY).coerceIn(0f, maxCollapsePx)
                    val consumedY = -(newOffset - collapseOffsetPx)
                    collapseOffsetPx = newOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val deltaY = available.y
                if (deltaY > 0f && collapseOffsetPx > 0f) {
                    val newOffset = (collapseOffsetPx - deltaY).coerceIn(0f, maxCollapsePx)
                    val consumedY = -(newOffset - collapseOffsetPx)
                    collapseOffsetPx = newOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val progress = if (maxCollapsePx <= 0f) 0f else (collapseOffsetPx / maxCollapsePx)
                if (progress > 0.02f && progress < 0.98f) {
                    val targetPx = when {
                        available.y < -350f -> maxCollapsePx
                        available.y > 350f -> 0f
                        progress >= 0.5f -> maxCollapsePx
                        else -> 0f
                    }
                    animate(
                        initialValue = collapseOffsetPx,
                        targetValue = targetPx,
                        animationSpec = spring(
                            dampingRatio = 0.86f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { value, _ ->
                        collapseOffsetPx = value
                    }
                }
                return Velocity.Zero
            }
        }
    }

    val screenBgColor = if (!isDark) Color(0xFFF8FAFC) else Color(0xFF000000)
    val cardBgColor = if (!isDark) Color(0xFFFFFFFF) else Color(0xFF17171A)
    val cardBorderColor = if (!isDark) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.07f)
    val dividerColor = if (!isDark) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)
    val textDark = if (!isDark) Color(0xFF0A0A0C) else Color(0xFFF2F2F7)
    val textMuted = if (!isDark) Color(0xFF71717A) else Color(0xFF8E8E93)
    val activeAccent = if (gradientStyle == OneUiGradientStyle.SOLID) Color(0xFF007AFF) else gradientStyle.primaryColor

    val currentExpandedHeightDp = with(density) {
        (maxCollapsePx - collapseOffsetPx).coerceAtLeast(0f).toDp()
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snap = db.collection("users").document(currentUserId).get().await()
                profile = snap.toObject(UserProfile::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
            .nestedScroll(nestedScrollConnection)
    ) {
        // 1. Exact Chroma Studio System Gradient Backdrop
        ChromaOneUiHeaderBackdrop(
            expandedHeightDp = expandedHeightDp,
            collapseProgress = collapseProgress,
            gradientAlpha = gradientAlpha,
            isDark = isDark,
            screenBgColor = screenBgColor
        )

        Column(modifier = Modifier.fillMaxSize()) {
            val toolbarBgAlpha = (collapseProgress * 0.94f).coerceIn(0f, 0.94f)

            // Top Status Bar Spacer
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(screenBgColor.copy(alpha = toolbarBgAlpha))
            )

            // 2. Expanded Viewing Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentExpandedHeightDp),
                contentAlignment = Alignment.Center
            ) {
                if (largeTitleAlpha > 0.01f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .graphicsLayer {
                                alpha = largeTitleAlpha
                                translationY = -collapseProgress * with(density) { 20.dp.toPx() }
                                val scale = 1f - (collapseProgress * 0.05f)
                                scaleX = scale
                                scaleY = scale
                            }
                    ) {
                        // Badge Pill
                        // Large One UI Title
                        Text(
                            text = "Settings",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-0.5).sp,
                            color = textDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Dynamic Subtitle
                        Text(
                            text = profile?.displayName?.ifBlank { "Preferences & Account" } ?: "Preferences & Account",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
                        )
                    }
                }
            }

            // 3. Sticky Compact One UI Toolbar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(screenBgColor.copy(alpha = toolbarBgAlpha))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Back Chevron + Compact Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    SoundManager.playReaction()
                                    onBack()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.ArrowLeft,
                                contentDescription = "Back",
                                tint = textDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "Settings",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer {
                                alpha = compactTitleAlpha
                                translationY = (1f - compactTitleAlpha) * with(density) { 4.dp.toPx() }
                            }
                        )
                    }

                    // Right: Theme cycle pill + Done
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (!isDark) Color.Black.copy(0.06f) else Color.White.copy(0.10f))
                                .clickable { cycleGradientStyle() }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (gradientStyle == OneUiGradientStyle.SOLID) Color(0xFF007AFF)
                                            else gradientStyle.orbColor
                                        )
                                )
                                Text(
                                    text = gradientStyle.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textDark
                                )
                            }
                        }

                        Text(
                            text = "Done",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    SoundManager.playReaction()
                                    onBack()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                if (collapseProgress > 0.85f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.6.dp)
                            .background(dividerColor)
                    )
                }
            }

            // 4. Content Area
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    OscellionLoadingSpinner(color = activeAccent)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // One UI Squircle Search Bar
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (!isDark) Color(0xFFE9E9EE) else Color(0xFF1E1E22))
                                .border(1.dp, cardBorderColor, RoundedCornerShape(22.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Lucide.Search,
                                    contentDescription = null,
                                    tint = textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search settings...",
                                            color = textMuted,
                                            fontSize = 14.5.sp,
                                            fontFamily = InterFontFamily
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = textDark,
                                            fontSize = 14.5.sp,
                                            fontFamily = InterFontFamily
                                        ),
                                        cursorBrush = SolidColor(activeAccent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        imageVector = Lucide.X,
                                        contentDescription = "Clear",
                                        tint = textMuted,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { searchQuery = "" }
                                    )
                                }
                            }
                        }
                    }

                    // Profile Card (One UI 26.dp Squircle)
                    item {
                        val userEmail = FirebaseAuth.getInstance().currentUser?.email
                        OneUiSettingsProfileCard(
                            displayName = profile?.displayName?.ifBlank { "Oscellion Creator" } ?: "Oscellion Creator",
                            handleOrEmail = if (!profile?.username.isNullOrBlank()) "@${profile?.username}" else (userEmail ?: "Oscellion Account"),
                            avatarUrl = profile?.avatarUrl,
                            accentColor = activeAccent,
                            cardBgColor = cardBgColor,
                            cardBorderColor = cardBorderColor,
                            textDark = textDark,
                            textMuted = textMuted,
                            onClick = { profile?.let { onNavigateToProfileIdentity(it) } }
                        )
                    }

                    // Section 1: Core Preferences & Account
                    item {
                        OneUiSettingsCardGroup(
                            title = "Core Preferences & Privacy",
                            cardBgColor = cardBgColor,
                            cardBorderColor = cardBorderColor,
                            dividerColor = dividerColor,
                            textMuted = textMuted
                        ) {
                            OneUiSettingsRow(
                                title = "Profile & Identity",
                                subtitle = "Name, avatar, bio, and social handles",
                                icon = Lucide.User,
                                iconTint = activeAccent,
                                valueText = profile?.displayName?.takeIf { it.isNotBlank() },
                                textDark = textDark,
                                textMuted = textMuted,
                                onClick = { profile?.let { onNavigateToProfileIdentity(it) } }
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                            OneUiSettingsRow(
                                title = "Security & Access",
                                subtitle = "Password, logins, and recovery alerts",
                                icon = Lucide.ShieldCheck,
                                iconTint = Color(0xFF10B981),
                                textDark = textDark,
                                textMuted = textMuted,
                                onClick = { profile?.let { onNavigateToSecurity(it) } }
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                            OneUiSettingsRow(
                                title = "Privacy & Interaction",
                                subtitle = "Account visibility, mentions, and blocking",
                                icon = Lucide.Lock,
                                iconTint = Color(0xFF8B5CF6),
                                valueText = if (profile?.isPrivateAccount == true) "Private" else "Public",
                                textDark = textDark,
                                textMuted = textMuted,
                                onClick = { profile?.let { onNavigateToPrivacy(it) } }
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                            OneUiSettingsRow(
                                title = "Notifications & Alerts",
                                subtitle = "Push notifications, tones, and vibration",
                                icon = Lucide.Bell,
                                iconTint = Color(0xFFF59E0B),
                                valueText = if (profile?.pushNotificationsEnabled == true) "On" else "Off",
                                textDark = textDark,
                                textMuted = textMuted,
                                onClick = { profile?.let { onNavigateToNotifications(it) } }
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                            OneUiSettingsRow(
                                title = "Archived Creatives",
                                subtitle = "View and restore your archived creations",
                                icon = Lucide.Archive,
                                iconTint = Color(0xFF06B6D4),
                                textDark = textDark,
                                textMuted = textMuted,
                                onClick = { profile?.let { onNavigateToArchives(it) } }
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                            OneUiSettingsRow(
                                title = "Your Activity",
                                subtitle = "Liked, saved, and shared posts history",
                                icon = Lucide.History,
                                iconTint = Color(0xFFEC4899),
                                textDark = textDark,
                                textMuted = textMuted,
                                onClick = { profile?.let { onNavigateToActivityHistory(it) } }
                            )
                        }
                    }

                    // Section 2: Data & Account
                    item {
                        OneUiSettingsCardGroup(
                            title = "Data & Account",
                            cardBgColor = cardBgColor,
                            cardBorderColor = cardBorderColor,
                            dividerColor = dividerColor,
                            textMuted = textMuted
                        ) {
                            OneUiSettingsRow(
                                title = "Download My Information",
                                subtitle = "Export personal JSON archive",
                                icon = Lucide.Download,
                                iconTint = Color(0xFF3B82F6),
                                textDark = textDark,
                                textMuted = textMuted,
                                onClick = {
                                    profile?.let {
                                        val json = GsonBuilder().setPrettyPrinting().create().toJson(it)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "My Oscellion Profile Data")
                                            putExtra(Intent.EXTRA_TEXT, json)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Download Data"))
                                    }
                                }
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                            OneUiSettingsRow(
                                title = "Privacy Policy and Terms",
                                subtitle = "Legal agreements, terms, and data safety",
                                icon = Lucide.FileText,
                                iconTint = Color(0xFF6B7280),
                                textDark = textDark,
                                textMuted = textMuted,
                                onClick = onNavigateToPrivacyTerms
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                            OneUiSettingsRow(
                                title = "Sign Out",
                                subtitle = "Log out from this device",
                                icon = Lucide.LogOut,
                                iconTint = Color(0xFFEF4444),
                                isDestructive = true,
                                textDark = textDark,
                                textMuted = textMuted,
                                onClick = onLogout
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// ── One UI Profile Card ───────────────────────────────────────────────────────
@Composable
private fun OneUiSettingsProfileCard(
    displayName: String,
    handleOrEmail: String,
    avatarUrl: String?,
    accentColor: Color,
    cardBgColor: Color,
    cardBorderColor: Color,
    textDark: Color,
    textMuted: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(cardBgColor)
            .border(1.dp, cardBorderColor, RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                IosAvatarView(
                    name = displayName,
                    avatarUrl = avatarUrl ?: "",
                    size = 54.dp
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = displayName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textDark,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        text = handleOrEmail,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = textMuted,
                        fontFamily = InterFontFamily
                    )
                }
            }

            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── One UI Settings Card Group Container ──────────────────────────────────────
@Composable
private fun OneUiSettingsCardGroup(
    title: String,
    cardBgColor: Color,
    cardBorderColor: Color,
    dividerColor: Color,
    textMuted: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = textMuted,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 14.dp, bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(cardBgColor)
                .border(1.dp, cardBorderColor, RoundedCornerShape(26.dp)),
            content = content
        )
    }
}

// ── One UI Settings Row Item ──────────────────────────────────────────────────
@Composable
private fun OneUiSettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    valueText: String? = null,
    isDestructive: Boolean = false,
    textDark: Color,
    textMuted: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                SoundManager.playReaction()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) Color(0xFFEF4444) else textDark,
                    fontFamily = InterFontFamily
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = textMuted,
                    fontFamily = InterFontFamily
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (valueText != null) {
                Text(
                    text = valueText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMuted,
                    fontFamily = InterFontFamily
                )
            }
            if (!isDestructive) {
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
