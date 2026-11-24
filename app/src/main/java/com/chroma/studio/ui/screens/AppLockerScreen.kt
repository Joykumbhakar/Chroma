package com.chroma.studio.ui.screens

import android.content.Context
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.security.SecurityPinManager
import com.chroma.studio.security.applock.AppLockTarget
import com.chroma.studio.security.applock.AppLockerManager
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Flagship Oscellion App Locker Configuration Screen.
 * Provides granular app-by-app locking, permission diagnostics (Accessibility & Overlay),
 * instant search filtering, and one-tap recommended protection.
 */
@Composable
fun AppLockerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    var isEnabled by remember { mutableStateOf(AppLockerManager.isEnabled(context)) }
    var hasOverlayPermission by remember { mutableStateOf(AppLockerManager.hasOverlayPermission(context)) }
    var isAccessibilityEnabled by remember { mutableStateOf(AppLockerManager.isAccessibilityServiceEnabled(context)) }
    var hasUsageStatsPermission by remember { mutableStateOf(AppLockerManager.hasUsageStatsPermission(context)) }
    var hasPinConfigured by remember { mutableStateOf(SecurityPinManager.hasPin(context)) }
    var isBiometricEnabled by remember { mutableStateOf(SecurityPinManager.isBiometricEnabled(context)) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showRestrictedSettingsGuideModal by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<AppLockTarget>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    // Auto-refresh permission statuses whenever user returns from Settings or App Info
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = AppLockerManager.hasOverlayPermission(context)
                isAccessibilityEnabled = AppLockerManager.isAccessibilityServiceEnabled(context)
                hasUsageStatsPermission = AppLockerManager.hasUsageStatsPermission(context)
                if (!isAccessibilityEnabled && hasUsageStatsPermission) {
                    AppLockerManager.startUsageWatcherIfNeeded(context, scope)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isEnabled, isAccessibilityEnabled, hasUsageStatsPermission) {
        if (isEnabled && !isAccessibilityEnabled && hasUsageStatsPermission) {
            AppLockerManager.startUsageWatcherIfNeeded(context, scope)
        } else if (!isEnabled || isAccessibilityEnabled) {
            AppLockerManager.stopUsageWatcher()
        }
    }

    // Load installed applications on background IO dispatcher
    LaunchedEffect(Unit) {
        isLoadingApps = true
        val apps = withContext(Dispatchers.IO) {
            AppLockerManager.getInstalledApps(context)
        }
        installedApps = apps
        isLoadingApps = false
    }

    BackHandler(enabled = true) {
        onBack()
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            val q = searchQuery.trim().lowercase()
            installedApps.filter {
                it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

    val lockedCount = remember(installedApps) {
        installedApps.count { it.isLocked }
    }

    OneUiCollapsingHeaderLayout(
        title = "App Locker",
        subtitle = "$lockedCount apps locked with Biometric & PIN",
        onBack = onBack,
        headerActions = { _ ->
            // Done button in header
            Text(
                text = "Done",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                fontFamily = InterFontFamily,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        SoundManager.playReaction()
                        onBack()
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // =====================================================================
            // 1. MASTER TOGGLE & STATUS
            // =====================================================================
            item {
                AppleSettingsGroup(
                    header = "Protection Status",
                    footer = if (isBiometricEnabled) {
                        "When enabled, protected applications require Face, Fingerprint, or PIN to unlock."
                    } else {
                        "When enabled, protected applications require the 4-digit PIN to unlock."
                    }
                ) {
                    AppleSettingToggleRow(
                        title = "Enable App Locker",
                        subtitle = if (isEnabled) {
                            if (isAccessibilityEnabled) "Active • Zero-latency Accessibility"
                            else if (hasUsageStatsPermission) "Active • Usage Access Fallback"
                            else "Enabled • Grant permission below"
                        } else "Disabled • No apps locked",
                        icon = Lucide.ShieldCheck,
                        iconGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE)),
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundManager.playReaction()
                            isEnabled = checked
                            AppLockerManager.setEnabled(context, checked)
                            if (checked && !isAccessibilityEnabled && hasUsageStatsPermission) {
                                AppLockerManager.startUsageWatcherIfNeeded(context, scope)
                            } else if (!checked) {
                                AppLockerManager.stopUsageWatcher()
                            }
                        }
                    )

                    AppleSettingDivider()

                    AppleSettingToggleRow(
                        title = "Biometric Unlock",
                        subtitle = if (isBiometricEnabled) "Face & Fingerprint unlock active" else "Disabled • Unlock using 4-digit PIN only",
                        icon = Lucide.Fingerprint,
                        iconGradient = listOf(Color(0xFF34C759), Color(0xFF30D158)),
                        checked = isBiometricEnabled,
                        onCheckedChange = { checked ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundManager.playReaction()
                            if (!isEnabled) {
                                ToastManager.showToast("Enable App Locker first")
                                return@AppleSettingToggleRow
                            }
                            if (!checked && !hasPinConfigured) {
                                ToastManager.showToast("Set up a 4-digit PIN before disabling Biometrics")
                                showPinSetupDialog = true
                            } else {
                                isBiometricEnabled = checked
                                SecurityPinManager.setBiometricEnabled(context, checked)
                                ToastManager.showToast(if (checked) "Biometric unlock enabled" else "Biometrics disabled (PIN only)")
                            }
                        }
                    )

                    AppleSettingDivider()

                    AppleSettingNavRow(
                        title = "App Lock PIN",
                        subtitle = if (hasPinConfigured) "4-digit PIN configured" else "Set up a 4-digit PIN for locking apps",
                        icon = Lucide.Lock,
                        iconGradient = listOf(Color(0xFF5856D6), Color(0xFF5E5CE6)),
                        valueText = if (hasPinConfigured) "Change" else "Set Up",
                        onClick = {
                            SoundManager.playReaction()
                            showPinSetupDialog = true
                        }
                    )
                }
            }

            // =====================================================================
            // 2. PERMISSION HEALTH DIAGNOSTICS
            // =====================================================================
            val hasDetectionEngine = isAccessibilityEnabled || hasUsageStatsPermission
            if (!hasDetectionEngine || !hasOverlayPermission) {
                item {
                    AppleSettingsGroup(
                        header = "Required System Permissions",
                        footer = "App Locker requires these settings to detect launched apps and display the security shield."
                    ) {
                        if (!isAccessibilityEnabled) {
                            AppleSettingNavRow(
                                title = "Accessibility Service",
                                subtitle = if (AppLockerManager.isRestrictedSettingsApplicable()) "Zero-latency • Tap for Restricted Setting fix" else "Required for instant zero-latency interception",
                                icon = Lucide.Zap,
                                iconGradient = listOf(Color(0xFFFF9500), Color(0xFFFF5E3A)),
                                valueText = if (AppLockerManager.isRestrictedSettingsApplicable()) "Fix" else "Grant",
                                onClick = {
                                    SoundManager.playReaction()
                                    if (AppLockerManager.isRestrictedSettingsApplicable()) {
                                        showRestrictedSettingsGuideModal = true
                                    } else {
                                        AppLockerManager.openAccessibilitySettings(context)
                                    }
                                }
                            )
                        }

                        if (!hasUsageStatsPermission && !isAccessibilityEnabled) {
                            if (!isAccessibilityEnabled) {
                                AppleSettingDivider()
                            }
                            AppleSettingNavRow(
                                title = "Usage Access (Alternative)",
                                subtitle = "Bypasses Android Restricted Setting lock",
                                icon = Lucide.Activity,
                                iconGradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                                valueText = "Grant",
                                onClick = {
                                    SoundManager.playReaction()
                                    AppLockerManager.openUsageStatsSettings(context)
                                }
                            )
                        }

                        if (!hasOverlayPermission) {
                            if (!isAccessibilityEnabled || !hasUsageStatsPermission) {
                                AppleSettingDivider()
                            }
                            AppleSettingNavRow(
                                title = "Display Over Other Apps",
                                subtitle = "Allows the lock screen to cover locked apps immediately",
                                icon = Lucide.Layers,
                                iconGradient = listOf(Color(0xFF00C7BE), Color(0xFF0082FB)),
                                valueText = "Grant",
                                onClick = {
                                    SoundManager.playReaction()
                                    AppLockerManager.openOverlaySettings(context)
                                }
                            )
                        }
                    }
                }

                // Android 13+ (API 33+) Restricted Settings Helper Card
                if (!isAccessibilityEnabled && AppLockerManager.isRestrictedSettingsApplicable()) {
                    item {
                        RestrictedSettingAlertCard(
                            isDark = isDark,
                            onOpenAppInfo = {
                                SoundManager.playReaction()
                                AppLockerManager.openAppInfo(context)
                            },
                            onOpenAccessibility = {
                                SoundManager.playReaction()
                                AppLockerManager.openAccessibilitySettings(context)
                            },
                            onCopyAdbCommand = {
                                SoundManager.playReaction()
                                val cmd = "adb shell appops set ${context.packageName} ACCESS_RESTRICTED_SETTINGS allow"
                                clipboardManager.setText(AnnotatedString(cmd))
                                ToastManager.showToast("Copied ADB bypass command to clipboard")
                            }
                        )
                    }
                }
            }

            // =====================================================================
            // 3. QUICK ACTIONS
            // =====================================================================
            item {
                AppleSettingsGroup(
                    header = "Quick Configuration"
                ) {
                    AppleSettingNavRow(
                        title = "Lock Recommended Apps",
                        subtitle = "Instantly protects WhatsApp, Photos, Instagram, Banking & Files",
                        icon = Lucide.Sparkles,
                        iconGradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                        valueText = "Protect",
                        onClick = {
                            SoundManager.playReaction()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val added = AppLockerManager.lockRecommended(context)
                            ToastManager.showToast("Protected $added recommended apps")
                            // Refresh list state
                            val updated = installedApps.map {
                                it.copy(isLocked = AppLockerManager.isPackageLocked(context, it.packageName))
                            }
                            installedApps = updated
                        }
                    )
                }
            }

            // =====================================================================
            // 4. SEARCH APPS BAR
            // =====================================================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.cardSurface)
                        .border(
                            width = 0.75.dp,
                            color = colors.cardBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.Search,
                        contentDescription = "Search",
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search installed apps...",
                                fontSize = 14.sp,
                                color = colors.textMuted.copy(alpha = 0.75f),
                                fontFamily = InterFontFamily
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = colors.textMain,
                            unfocusedTextColor = colors.textMain
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Clear search",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // =====================================================================
            // 5. INSTALLED APPS LIST
            // =====================================================================
            if (isLoadingApps) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OscellionLoadingSpinner(
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else if (filteredApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No applications found matching '$searchQuery'" else "No launchable apps found",
                            fontSize = 14.sp,
                            color = colors.textMuted,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "INSTALLED APPLICATIONS (${filteredApps.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }

                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { appTarget ->
                    AppLockRowItem(
                        target = appTarget,
                        isDark = isDark,
                        onToggle = { isChecked ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            SoundManager.playReaction()
                            AppLockerManager.setPackageLocked(context, appTarget.packageName, isChecked)

                            // Update in-memory state
                            installedApps = installedApps.map {
                                if (it.packageName == appTarget.packageName) {
                                    it.copy(isLocked = isChecked)
                                } else it
                            }
                        }
                    )
                }
            }
        }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            isChangingExisting = hasPinConfigured,
            onDismiss = { showPinSetupDialog = false },
            onSuccess = {
                showPinSetupDialog = false
                hasPinConfigured = true
                isBiometricEnabled = SecurityPinManager.isBiometricEnabled(context)
                ToastManager.showToast("PIN configured successfully")
            }
        )
    }

    if (showRestrictedSettingsGuideModal) {
        RestrictedSettingsGuideDialog(
            isDark = isDark,
            onDismiss = { showRestrictedSettingsGuideModal = false },
            onOpenAppInfo = {
                showRestrictedSettingsGuideModal = false
                AppLockerManager.openAppInfo(context)
            },
            onOpenAccessibility = {
                showRestrictedSettingsGuideModal = false
                AppLockerManager.openAccessibilitySettings(context)
            },
            onOpenUsageAccess = {
                showRestrictedSettingsGuideModal = false
                AppLockerManager.openUsageStatsSettings(context)
            }
        )
    }
}

@Composable
private fun AppLockRowItem(
    target: AppLockTarget,
    isDark: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardSurface)
            .border(
                width = 0.5.dp,
                color = colors.cardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onToggle(!target.isLocked) }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.cardSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (target.icon != null) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            setImageDrawable(target.icon)
                        }
                    },
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Icon(
                    imageVector = Lucide.AppWindow,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // App Name and Package
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = target.appName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMain,
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = target.packageName,
                fontSize = 11.sp,
                color = colors.textMuted,
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(12.dp))

        // Sleek Spring Pill Switch
        SpringPillSwitch(
            checked = target.isLocked,
            onCheckedChange = onToggle,
            colors = PillSwitchColors.Brand,
            trackWidth = 52.dp,
            trackHeight = 30.dp
        )
    }
}

/**
 * Visual guide and 1-tap navigation for Android 13+ (API 33+) "Restricted Settings".
 * When an APK is sideloaded outside Google Play, Android blocks accessibility toggles
 * until enabled via App Info -> 3-dots -> "Allow restricted settings".
 */
@Composable
private fun RestrictedSettingAlertCard(
    isDark: Boolean,
    onOpenAppInfo: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onCopyAdbCommand: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color(0xFF1E1608) else Color(0xFFFFFBEB))
            .border(
                width = 1.dp,
                color = if (isDark) Color(0xFFFF9F0A).copy(alpha = 0.35f) else Color(0xFFFDE68A),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        // Warning Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFFFF9F0A).copy(alpha = 0.16f) else Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.ShieldAlert,
                    contentDescription = "Restricted Setting Warning",
                    tint = if (isDark) Color(0xFFFF9F0A) else Color(0xFFD97706),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Android \"Restricted Setting\" Fix",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFFFB340) else Color(0xFF92400E),
                    fontFamily = InterFontFamily
                )
                Text(
                    text = "Android 13, 14 & 15 sideload security check",
                    fontSize = 12.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.60f) else Color(0xFFB45309),
                    fontFamily = InterFontFamily
                )
            }
            Icon(
                imageVector = if (isExpanded) Lucide.ChevronUp else Lucide.ChevronDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = if (isDark) Color(0xFFFF9F0A) else Color(0xFFD97706),
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                Spacer(Modifier.height(14.dp))

                Text(
                    text = "If Android grays out the toggle and says \"Restricted setting: For your security, this setting is currently unavailable\", unlock it in 3 steps:",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF78350F),
                    fontFamily = InterFontFamily
                )

                Spacer(Modifier.height(12.dp))

                // Steps
                RestrictedStepItem(
                    stepNumber = "1",
                    boldText = "Trigger prompt: ",
                    normalText = "Tap '2. Accessibility' below and tap ChromaStudio once so Android registers the restriction.",
                    isDark = isDark
                )
                Spacer(Modifier.height(8.dp))
                RestrictedStepItem(
                    stepNumber = "2",
                    boldText = "Allow in App Info: ",
                    normalText = "Tap '1. App Info' below, tap the three dots (⋮) in the top-right corner, and choose 'Allow restricted settings'.",
                    isDark = isDark
                )
                Spacer(Modifier.height(8.dp))
                RestrictedStepItem(
                    stepNumber = "3",
                    boldText = "Turn ON: ",
                    normalText = "Return here, tap '2. Accessibility' again, and turn the toggle ON.",
                    isDark = isDark
                )

                Spacer(Modifier.height(16.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenAppInfo,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFFFF9F0A) else Color(0xFFF59E0B),
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Lucide.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "1. App Info (⋮)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onOpenAccessibility,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFFEF3C7),
                            contentColor = if (isDark) Color.White else Color(0xFF92400E)
                        ),
                        border = if (!isDark) BorderStroke(1.dp, Color(0xFFFDE68A)) else null,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Lucide.Zap,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isDark) Color(0xFFFF9F0A) else Color(0xFFD97706)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "2. Accessibility",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ADB Bypass button for developers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFFFEF3C7))
                        .border(
                            width = 0.5.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFFDE68A),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onCopyAdbCommand() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.Terminal,
                        contentDescription = null,
                        tint = if (isDark) Color.White.copy(alpha = 0.50f) else Color(0xFF92400E),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Developer ADB bypass command",
                        fontSize = 11.5.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF92400E),
                        fontFamily = InterFontFamily,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Lucide.Copy,
                        contentDescription = "Copy command",
                        tint = if (isDark) Color.White.copy(alpha = 0.50f) else Color(0xFF92400E),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RestrictedStepItem(
    stepNumber: String,
    boldText: String,
    normalText: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFFFF9F0A) else Color(0xFFF59E0B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.Black else Color.White,
                fontFamily = InterFontFamily
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = buildAnnotatedString {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFFFB340) else Color(0xFF92400E)))
                append(boldText)
                pop()
                pushStyle(SpanStyle(fontWeight = FontWeight.Normal, color = if (isDark) Color.White.copy(alpha = 0.80f) else Color(0xFF78350F)))
                append(normalText)
                pop()
            },
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontFamily = InterFontFamily,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RestrictedSettingsGuideDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenUsageAccess: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9F0A).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.ShieldAlert,
                        contentDescription = null,
                        tint = Color(0xFFFF9F0A),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Fix Restricted Setting",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF111827),
                    fontFamily = InterFontFamily
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Android 13+ automatically displays 'App was denied access' for sideloaded APKs. You can unblock it in 2 taps:",
                    fontSize = 13.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF4B5563),
                    fontFamily = InterFontFamily
                )

                // Step 1
                Surface(
                    onClick = onOpenAppInfo,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFFFF9F0A).copy(alpha = 0.15f) else Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFF9F0A).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF9F0A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open App Info",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (isDark) Color(0xFFFFB340) else Color(0xFF92400E)
                            )
                            Text(
                                text = "Tap 3 dots (⋮) in top-right -> 'Allow restricted settings'",
                                fontSize = 11.5.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.70f) else Color(0xFF78350F)
                            )
                        }
                        Icon(Lucide.ChevronRight, contentDescription = null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(16.dp))
                    }
                }

                // Step 2
                Surface(
                    onClick = onOpenAccessibility,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFF3F4F6),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0381FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Turn ON in Accessibility",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (isDark) Color.White else Color(0xFF111827)
                            )
                            Text(
                                text = "Tap 'Installed apps' -> 'Oscellion' -> toggle ON",
                                fontSize = 11.5.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.60f) else Color(0xFF6B7280)
                            )
                        }
                        Icon(Lucide.ChevronRight, contentDescription = null, tint = Color(0xFF0381FE), modifier = Modifier.size(16.dp))
                    }
                }

                // Alternative: Usage Access
                Surface(
                    onClick = onOpenUsageAccess,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Lucide.ShieldCheck, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Or Use Usage Access (No Restriction)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF065F46)
                            )
                            Text(
                                text = "Android never restricts Usage Access for APKs",
                                fontSize = 11.5.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF047857)
                            )
                        }
                        Icon(Lucide.ChevronRight, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It", fontWeight = FontWeight.Bold, color = Color(0xFF0381FE))
            }
        }
    )
}
