package com.chroma.studio.ui.screens

import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.security.SecurityPinManager
import com.chroma.studio.security.applock.AppLockTarget
import com.chroma.studio.security.applock.AppLockerManager
import com.chroma.studio.security.applock.filterAppLockTargets
import com.chroma.studio.security.applock.formatRelockModeDescription
import com.chroma.studio.security.applock.formatRelockModeLabel
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Flagship Oscellion App Locker Configuration Screen.
 * Clean Apple HIG flat list layout with hairline dividers, borderless rows,
 * underline filter tabs, grace-window re-lock controls, anti-tamper protection,
 * failed-attempt alert audit, and confirmation dialogs before destructive actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val dividerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    val accentBlue = Color(0xFF0381FE)

    var isEnabled by remember { mutableStateOf(AppLockerManager.isEnabled(context)) }
    var hasOverlayPermission by remember { mutableStateOf(AppLockerManager.hasOverlayPermission(context)) }
    val hasAccessibilityDeclared = remember { AppLockerManager.hasAccessibilityServiceDeclared(context) }
    var isAccessibilityEnabled by remember {
        mutableStateOf(hasAccessibilityDeclared && AppLockerManager.isAccessibilityServiceEnabled(context))
    }
    var hasUsageStatsPermission by remember { mutableStateOf(AppLockerManager.hasUsageStatsPermission(context)) }
    var hasPinConfigured by remember { mutableStateOf(SecurityPinManager.hasPin(context)) }
    var isBiometricEnabled by remember { mutableStateOf(SecurityPinManager.isBiometricEnabled(context)) }
    var relockMode by remember { mutableStateOf(AppLockerManager.getRelockMode(context)) }
    var protectSettingsEnabled by remember { mutableStateOf(AppLockerManager.isProtectSettingsEnabled(context)) }
    var intruderAlertCount by remember { mutableIntStateOf(SecurityPinManager.getIntruderAlertCount(context)) }
    var lastFailedAttemptAtMs by remember { mutableLongStateOf(SecurityPinManager.getLastFailedAttemptAtMs(context)) }

    var appFilterTab by remember { mutableStateOf("ALL") } // ALL, LOCKED, DOWNLOADED, SYSTEM
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<AppLockTarget>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showRestrictedSettingsGuideModal by remember { mutableStateOf(false) }
    var showTestLockPreview by remember { mutableStateOf(false) }
    var showRelockTimingSheet by remember { mutableStateOf(false) }
    var showUnlockAllConfirm by remember { mutableStateOf(false) }
    var showDisableConfirm by remember { mutableStateOf(false) }
    var showClearIntruderConfirm by remember { mutableStateOf(false) }

    // Refresh permission & audit statuses whenever user returns from system settings or lock shield
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = AppLockerManager.hasOverlayPermission(context)
                isAccessibilityEnabled = hasAccessibilityDeclared && AppLockerManager.isAccessibilityServiceEnabled(context)
                hasUsageStatsPermission = AppLockerManager.hasUsageStatsPermission(context)
                hasPinConfigured = SecurityPinManager.hasPin(context)
                intruderAlertCount = SecurityPinManager.getIntruderAlertCount(context)
                lastFailedAttemptAtMs = SecurityPinManager.getLastFailedAttemptAtMs(context)
                if (isEnabled && !isAccessibilityEnabled && hasUsageStatsPermission) {
                    AppLockerManager.startUsageWatcherIfNeeded(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isEnabled, isAccessibilityEnabled, hasUsageStatsPermission) {
        if (isEnabled && !isAccessibilityEnabled && hasUsageStatsPermission) {
            AppLockerManager.startUsageWatcherIfNeeded(context)
        } else if (!isEnabled || isAccessibilityEnabled) {
            AppLockerManager.stopUsageWatcher()
        }
    }

    LaunchedEffect(Unit) {
        isLoadingApps = true
        val apps = withContext(Dispatchers.IO) {
            AppLockerManager.getInstalledApps(context)
        }
        installedApps = apps
        isLoadingApps = false
    }

    BackHandler(enabled = true) {
        when {
            showTestLockPreview -> showTestLockPreview = false
            showRelockTimingSheet -> showRelockTimingSheet = false
            else -> onBack()
        }
    }

    val lockedCount = remember(installedApps) { installedApps.count { it.isLocked } }
    val filteredApps = remember(installedApps, searchQuery, appFilterTab) {
        filterAppLockTargets(installedApps, appFilterTab, searchQuery)
    }
    val downloadedCount = remember(installedApps) { installedApps.count { !it.isSystem } }
    val systemCount = remember(installedApps) { installedApps.count { it.isSystem } }
    val hasDetectionEngine = isAccessibilityEnabled || hasUsageStatsPermission

    fun applyMasterEnabled(enabled: Boolean) {
        isEnabled = enabled
        AppLockerManager.setEnabled(context, enabled)
        if (enabled && !isAccessibilityEnabled && hasUsageStatsPermission) {
            AppLockerManager.startUsageWatcherIfNeeded(context)
        } else if (!enabled) {
            AppLockerManager.stopUsageWatcher()
        }
    }

    OneUiCollapsingHeaderLayout(
        title = "App Locker",
        subtitle = "$lockedCount apps protected",
        onBack = onBack,
        headerActions = { _ ->
            Text(
                text = "Done",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentBlue,
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
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // =================================================================
            // 1. SECURITY AUDIT BANNER (when failed PIN attempts were recorded)
            // =================================================================
            if (intruderAlertCount > 0) {
                item {
                    val formattedTime = remember(lastFailedAttemptAtMs) {
                        if (lastFailedAttemptAtMs > 0L) {
                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(lastFailedAttemptAtMs))
                        } else "Recently"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.ShieldAlert,
                            contentDescription = "Failed PIN attempts",
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$intruderAlertCount failed unlock ${if (intruderAlertCount == 1) "attempt" else "attempts"}",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF3B30),
                                fontFamily = InterFontFamily
                            )
                            Text(
                                text = "Last incorrect PIN entry: $formattedTime",
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                fontFamily = InterFontFamily
                            )
                        }
                        Text(
                            text = "Clear",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentBlue,
                            fontFamily = InterFontFamily,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    SoundManager.playTap(context)
                                    showClearIntruderConfirm = true
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                }
            }

            // =================================================================
            // 2. PROTECTION STATUS
            // =================================================================
            item {
                FlatSectionHeader("Protection Status", colors.textMuted)
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                FlatLockerToggleRow(
                    title = "Enable App Locker",
                    subtitle = when {
                        !isEnabled -> "Disabled • No apps intercepted"
                        isAccessibilityEnabled -> "Active • Instant Accessibility protection"
                        hasUsageStatsPermission && hasOverlayPermission -> "Active • Usage Access & Overlay shield"
                        hasUsageStatsPermission -> "Active • Usage Access monitoring"
                        else -> "Action needed • Grant detection permissions below"
                    },
                    icon = Lucide.ShieldCheck,
                    iconTint = accentBlue,
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundManager.playReaction()
                        if (checked && !hasPinConfigured) {
                            ToastManager.showToast("Set a 4-digit PIN first")
                            showPinSetupDialog = true
                            return@FlatLockerToggleRow
                        }
                        if (!checked && lockedCount > 0) {
                            showDisableConfirm = true
                        } else {
                            applyMasterEnabled(checked)
                        }
                    }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

                FlatLockerNavRow(
                    title = "Re-lock Timing",
                    subtitle = formatRelockModeDescription(relockMode),
                    icon = Lucide.Clock,
                    iconTint = Color(0xFF5856D6),
                    valueText = formatRelockModeLabel(relockMode),
                    onClick = {
                        SoundManager.playReaction()
                        showRelockTimingSheet = true
                    }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

                FlatLockerToggleRow(
                    title = "Protect Settings & Installer",
                    subtitle = "Require PIN before opening System Settings or Package Installer",
                    icon = Lucide.Shield,
                    iconTint = Color(0xFFFF9500),
                    checked = protectSettingsEnabled,
                    onCheckedChange = { checked ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundManager.playReaction()
                        if (checked && !hasPinConfigured) {
                            ToastManager.showToast("Set a 4-digit PIN first")
                            showPinSetupDialog = true
                            return@FlatLockerToggleRow
                        }
                        protectSettingsEnabled = checked
                        AppLockerManager.setProtectSettingsEnabled(context, checked)
                        ToastManager.showToast(
                            if (checked) "Anti-tamper protection enabled" else "Settings protection disabled"
                        )
                    }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

                FlatLockerToggleRow(
                    title = "Biometric Unlock",
                    subtitle = if (isBiometricEnabled) "Face & Fingerprint unlock active" else "Disabled • 4-digit PIN only",
                    icon = Lucide.Fingerprint,
                    iconTint = Color(0xFF34C759),
                    checked = isBiometricEnabled,
                    onCheckedChange = { checked ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundManager.playReaction()
                        if (!checked && !hasPinConfigured) {
                            ToastManager.showToast("Set up a 4-digit PIN before disabling Biometrics")
                            showPinSetupDialog = true
                        } else {
                            isBiometricEnabled = checked
                            SecurityPinManager.setBiometricEnabled(context, checked)
                            ToastManager.showToast(if (checked) "Biometric unlock enabled" else "Biometrics disabled")
                        }
                    }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

                FlatLockerNavRow(
                    title = "App Lock PIN",
                    subtitle = if (hasPinConfigured) "4-digit PIN configured" else "Required for unlocking protected apps",
                    icon = Lucide.Lock,
                    iconTint = Color(0xFF5E5CE6),
                    valueText = if (hasPinConfigured) "Change" else "Set Up",
                    onClick = {
                        SoundManager.playReaction()
                        showPinSetupDialog = true
                    }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                Text(
                    text = if (isBiometricEnabled) {
                        "Protected apps require Face, Fingerprint, or your 4-digit PIN."
                    } else {
                        "Protected apps require your 4-digit PIN to open."
                    },
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    lineHeight = 16.sp,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // =================================================================
            // 3. REQUIRED SYSTEM PERMISSIONS (when incomplete)
            // =================================================================
            if (!hasDetectionEngine || !hasOverlayPermission) {
                item {
                    FlatSectionHeader("Required System Permissions", colors.textMuted)
                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                    if (hasAccessibilityDeclared && !isAccessibilityEnabled) {
                        FlatLockerNavRow(
                            title = "Accessibility Service",
                            subtitle = if (AppLockerManager.isRestrictedSettingsApplicable()) {
                                "Zero-latency • Tap for sideload guide"
                            } else {
                                "Instant app launch detection"
                            },
                            icon = Lucide.Zap,
                            iconTint = Color(0xFFFF9500),
                            valueText = if (AppLockerManager.isRestrictedSettingsApplicable()) "Guide" else "Grant",
                            valueTextColor = Color(0xFFFF9500),
                            onClick = {
                                SoundManager.playReaction()
                                if (AppLockerManager.isRestrictedSettingsApplicable()) {
                                    showRestrictedSettingsGuideModal = true
                                } else {
                                    AppLockerManager.openAccessibilitySettings(context)
                                }
                            }
                        )
                        HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))
                    }

                    FlatLockerNavRow(
                        title = "Usage Access",
                        subtitle = if (hasUsageStatsPermission) "Granted • App launch detection active" else "Detects when protected apps open",
                        icon = Lucide.Activity,
                        iconTint = Color(0xFF10B981),
                        valueText = if (hasUsageStatsPermission) "Granted" else "Grant",
                        valueTextColor = if (hasUsageStatsPermission) Color(0xFF10B981) else accentBlue,
                        showChevron = !hasUsageStatsPermission,
                        onClick = {
                            SoundManager.playReaction()
                            AppLockerManager.openUsageStatsSettings(context)
                        }
                    )
                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

                    FlatLockerNavRow(
                        title = "Display Over Other Apps",
                        subtitle = if (hasOverlayPermission) "Granted • Lock shield ready" else "Displays the lock screen over protected apps",
                        icon = Lucide.Layers,
                        iconTint = Color(0xFF00C7BE),
                        valueText = if (hasOverlayPermission) "Granted" else "Grant",
                        valueTextColor = if (hasOverlayPermission) Color(0xFF10B981) else accentBlue,
                        showChevron = !hasOverlayPermission,
                        onClick = {
                            SoundManager.playReaction()
                            AppLockerManager.openOverlaySettings(context)
                        }
                    )
                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                    if (hasAccessibilityDeclared && !isAccessibilityEnabled && AppLockerManager.isRestrictedSettingsApplicable()) {
                        RestrictedSettingNoticeSection(
                            isDark = isDark,
                            dividerColor = dividerColor,
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
                                ToastManager.showToast("Copied ADB command")
                            }
                        )
                    }
                }
            }

            // =================================================================
            // 4. QUICK ACTIONS
            // =================================================================
            item {
                FlatSectionHeader("Quick Actions", colors.textMuted)
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                FlatLockerNavRow(
                    title = "Lock Recommended Apps",
                    subtitle = "Messaging, social, gallery, files & finance",
                    icon = Lucide.Sparkles,
                    iconTint = Color(0xFF10B981),
                    valueText = "Protect",
                    valueTextColor = accentBlue,
                    onClick = {
                        SoundManager.playReaction()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val added = AppLockerManager.lockRecommended(context)
                        val lockedSet = AppLockerManager.getLockedPackages(context)
                        installedApps = installedApps.map { it.copy(isLocked = lockedSet.contains(it.packageName)) }
                        ToastManager.showToast("Protected $added recommended apps")
                    }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

                FlatLockerNavRow(
                    title = "Test Lock Screen",
                    subtitle = "Preview biometric, PIN & wallpaper shield",
                    icon = Lucide.Eye,
                    iconTint = accentBlue,
                    valueText = "Preview",
                    onClick = {
                        SoundManager.playReaction()
                        showTestLockPreview = true
                    }
                )

                if (lockedCount > 0) {
                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))
                    FlatLockerNavRow(
                        title = "Unlock All Apps",
                        subtitle = "Remove all $lockedCount protected apps",
                        icon = Lucide.LockOpen,
                        iconTint = Color(0xFFFF3B30),
                        isDestructive = true,
                        valueText = "Clear",
                        valueTextColor = Color(0xFFFF3B30),
                        showChevron = false,
                        onClick = {
                            SoundManager.playReaction()
                            showUnlockAllConfirm = true
                        }
                    )
                }
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
            }

            // =================================================================
            // 5. FILTER TABS & SEARCH
            // =================================================================
            item {
                Spacer(Modifier.height(14.dp))

                // Underline tabs (no pill shapes, no boxes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    listOf(
                        "ALL" to "All (${installedApps.size})",
                        "LOCKED" to "Locked ($lockedCount)",
                        "DOWNLOADED" to "Apps ($downloadedCount)",
                        "SYSTEM" to "System ($systemCount)"
                    ).forEach { (key, label) ->
                        val selected = appFilterTab == key
                        Column(
                            modifier = Modifier
                                .clickable {
                                    SoundManager.playTap(context)
                                    appFilterTab = key
                                }
                                .padding(top = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.5.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) accentBlue else colors.textMuted,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(2.dp)
                                    .background(if (selected) accentBlue else Color.Transparent)
                            )
                        }
                    }
                }
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                // Compact borderless search bar with subtle neutral surface
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.Search,
                        contentDescription = "Search",
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search apps or package names",
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
                            modifier = Modifier.size(28.dp)
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

            // =================================================================
            // 6. INSTALLED APPS FLAT LIST
            // =================================================================
            if (isLoadingApps) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OscellionLoadingSpinner(modifier = Modifier.size(34.dp))
                    }
                }
            } else if (filteredApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "No applications found matching \"$searchQuery\""
                            } else {
                                "No applications in this category"
                            },
                            fontSize = 14.sp,
                            color = colors.textMuted,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            } else {
                item {
                    val unlockedInView = filteredApps.filter { !it.isLocked }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "APPLICATIONS (${filteredApps.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted,
                            letterSpacing = 0.4.sp,
                            fontFamily = InterFontFamily
                        )
                        if (unlockedInView.size in 2..40) {
                            Text(
                                text = "Lock ${unlockedInView.size} shown",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentBlue,
                                fontFamily = InterFontFamily,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        SoundManager.playReaction()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val changed = AppLockerManager.setPackagesLocked(
                                            context,
                                            unlockedInView.map { it.packageName },
                                            true
                                        )
                                        val lockedSet = AppLockerManager.getLockedPackages(context)
                                        installedApps = installedApps.map {
                                            it.copy(isLocked = lockedSet.contains(it.packageName))
                                        }
                                        ToastManager.showToast("Locked $changed apps")
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                }

                itemsIndexed(
                    items = filteredApps,
                    key = { _, target -> target.packageName }
                ) { index, appTarget ->
                    AppLockRowItem(
                        target = appTarget,
                        onToggle = { isChecked ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            SoundManager.playReaction()
                            AppLockerManager.setPackageLocked(context, appTarget.packageName, isChecked)
                            installedApps = installedApps.map {
                                if (it.packageName == appTarget.packageName) it.copy(isLocked = isChecked) else it
                            }
                        }
                    )
                    HorizontalDivider(
                        color = dividerColor,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(
                            start = if (index == filteredApps.lastIndex) 0.dp else 68.dp
                        )
                    )
                }
            }
        }
    }

    // =========================================================================
    // DIALOGS & SHEETS
    // =========================================================================

    if (showRelockTimingSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showRelockTimingSheet = false },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = "Re-lock Timing",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                AppLockerManager.RELOCK_MODES.forEachIndexed { idx, mode ->
                    val selected = relockMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                SoundManager.playReaction()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                relockMode = mode
                                AppLockerManager.setRelockMode(context, mode)
                                showRelockTimingSheet = false
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatRelockModeLabel(mode),
                                fontSize = 15.5.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) accentBlue else colors.textMain,
                                fontFamily = InterFontFamily
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = formatRelockModeDescription(mode),
                                fontSize = 12.sp,
                                color = colors.textMuted,
                                fontFamily = InterFontFamily
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = "Selected",
                                tint = accentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (idx < AppLockerManager.RELOCK_MODES.lastIndex) {
                        HorizontalDivider(
                            color = dividerColor,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showUnlockAllConfirm) {
        IosAlertDialog(
            title = "Unlock All Apps?",
            message = "All $lockedCount protected applications will no longer require biometric or PIN unlock.",
            confirmText = "Unlock All",
            isDestructive = true,
            onConfirm = {
                showUnlockAllConfirm = false
                AppLockerManager.clearAllLockedPackages(context)
                installedApps = installedApps.map { it.copy(isLocked = false) }
                ToastManager.showToast("Unlocked all apps")
            },
            onDismiss = { showUnlockAllConfirm = false }
        )
    }

    if (showDisableConfirm) {
        IosAlertDialog(
            title = "Turn Off App Locker?",
            message = "Your $lockedCount locked apps will open without requiring PIN or biometric verification.",
            confirmText = "Turn Off",
            isDestructive = true,
            onConfirm = {
                showDisableConfirm = false
                applyMasterEnabled(false)
                ToastManager.showToast("App Locker disabled")
            },
            onDismiss = { showDisableConfirm = false }
        )
    }

    if (showClearIntruderConfirm) {
        IosAlertDialog(
            title = "Clear Security Alerts?",
            message = "This will dismiss the failed PIN attempt log.",
            confirmText = "Clear",
            isDestructive = true,
            onConfirm = {
                showClearIntruderConfirm = false
                SecurityPinManager.clearIntruderAlerts(context)
                intruderAlertCount = 0
                lastFailedAttemptAtMs = 0L
                ToastManager.showToast("Security alert log cleared")
            },
            onDismiss = { showClearIntruderConfirm = false }
        )
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
            dividerColor = dividerColor,
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

    if (showTestLockPreview) {
        OscellionLockScreen(
            titleText = "Lock Preview",
            onBack = { showTestLockPreview = false },
            onUnlockSuccess = { showTestLockPreview = false }
        )
    }
}

@Composable
private fun FlatSectionHeader(title: String, mutedColor: Color) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = mutedColor,
        letterSpacing = 0.4.sp,
        fontFamily = InterFontFamily,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun FlatLockerNavRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color,
    valueText: String? = null,
    valueTextColor: Color = Color(0xFF8E8E93),
    isDestructive: Boolean = false,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) Color(0xFFFF3B30) else iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDestructive) Color(0xFFFF3B30) else colors.textMain,
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (!valueText.isNullOrBlank()) {
            Text(
                text = valueText,
                fontSize = 14.sp,
                color = valueTextColor,
                fontFamily = InterFontFamily
            )
        }
        if (showChevron) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun FlatLockerToggleRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textMain,
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        AppleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            activeTrackColor = Color(0xFF0381FE)
        )
    }
}

@Composable
private fun AppLockRowItem(
    target: AppLockTarget,
    onToggle: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!target.isLocked) }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (target.icon != null) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(target.icon)
                    }
                },
                update = { view -> view.setImageDrawable(target.icon) },
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
            )
        } else {
            Icon(
                imageVector = Lucide.AppWindow,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = target.appName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textMain,
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${target.category.label} • ${target.packageName}",
                fontSize = 11.5.sp,
                color = colors.textMuted,
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(12.dp))

        AppleSwitch(
            checked = target.isLocked,
            onCheckedChange = onToggle,
            activeTrackColor = Color(0xFF0381FE)
        )
    }
}

/**
 * Clean borderless expandable section for Android 13+ "Restricted Settings" sideload setup.
 */
@Composable
private fun RestrictedSettingNoticeSection(
    isDark: Boolean,
    dividerColor: Color,
    onOpenAppInfo: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onCopyAdbCommand: () -> Unit
) {
    val colors = LocalOscellionColors.current
    var isExpanded by remember { mutableStateOf(false) }
    val amber = if (isDark) Color(0xFFFF9F0A) else Color(0xFFD97706)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Lucide.ShieldAlert,
                contentDescription = "Restricted Setting Help",
                tint = amber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Android \"Restricted Setting\" Help",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = amber,
                    fontFamily = InterFontFamily
                )
                Text(
                    text = "Android 13+ sideload unlock steps",
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    fontFamily = InterFontFamily
                )
            }
            Icon(
                imageVector = if (isExpanded) Lucide.ChevronUp else Lucide.ChevronDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 50.dp, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RestrictedStepItem("1.", "Trigger prompt: ", "Tap Accessibility below and select Oscellion once.", amber)
                RestrictedStepItem("2.", "Allow in App Info: ", "Open App Info, tap top-right menu (⋮) -> Allow restricted settings.", amber)
                RestrictedStepItem("3.", "Turn ON: ", "Return to Accessibility and toggle Oscellion ON.", amber)

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenAppInfo,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0381FE),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "App Info",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily
                        )
                    }

                    TextButton(
                        onClick = onOpenAccessibility,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Accessibility",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0381FE),
                            fontFamily = InterFontFamily
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopyAdbCommand() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.Terminal,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Copy ADB restricted-settings command",
                        fontSize = 12.sp,
                        color = Color(0xFF0381FE),
                        fontFamily = InterFontFamily,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Lucide.Copy,
                        contentDescription = "Copy command",
                        tint = Color(0xFF0381FE),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
    }
}

@Composable
private fun RestrictedStepItem(
    stepNumber: String,
    boldText: String,
    normalText: String,
    accentColor: Color
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = stepNumber,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            fontFamily = InterFontFamily,
            modifier = Modifier.width(20.dp)
        )
        Text(
            text = buildAnnotatedString {
                pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.textMain))
                append(boldText)
                pop()
                pushStyle(SpanStyle(fontWeight = FontWeight.Normal, color = colors.textMuted))
                append(normalText)
                pop()
            },
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            fontFamily = InterFontFamily,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RestrictedSettingsGuideDialog(
    isDark: Boolean,
    dividerColor: Color,
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenUsageAccess: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val amber = if (isDark) Color(0xFFFF9F0A) else Color(0xFFD97706)
    val blue = Color(0xFF0381FE)
    val green = Color(0xFF10B981)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
                .padding(top = 18.dp, bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Icon(
                    imageVector = Lucide.ShieldAlert,
                    contentDescription = null,
                    tint = amber,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Restricted Setting Fix",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Android 13+ blocks sideloaded Accessibility toggles until allowed in App Info, or use Usage Access instead.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = colors.textMuted,
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            GuideActionRow(
                step = "1.",
                stepColor = amber,
                title = "Open App Info",
                subtitle = "Tap 3 dots (⋮) -> Allow restricted settings",
                onClick = onOpenAppInfo
            )
            HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

            GuideActionRow(
                step = "2.",
                stepColor = blue,
                title = "Enable in Accessibility",
                subtitle = "Installed apps -> Oscellion -> Toggle ON",
                onClick = onOpenAccessibility
            )
            HorizontalDivider(color = dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))

            GuideActionRow(
                step = "Alt",
                stepColor = green,
                title = "Use Usage Access Instead",
                subtitle = "Never restricted by Android",
                onClick = onOpenUsageAccess
            )
            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Done",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = blue,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun GuideActionRow(
    step: String,
    stepColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = step,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = stepColor,
            fontFamily = InterFontFamily,
            modifier = Modifier.width(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textMain,
                fontFamily = InterFontFamily
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colors.textMuted,
                fontFamily = InterFontFamily
            )
        }
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC),
            modifier = Modifier.size(16.dp)
        )
    }
}
