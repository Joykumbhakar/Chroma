package com.chroma.studio.ui.screens

import android.content.Context
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import com.chroma.studio.security.applock.AppLockTarget
import com.chroma.studio.security.applock.AppLockerManager
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.InterFontFamily
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

    var isEnabled by remember { mutableStateOf(AppLockerManager.isEnabled(context)) }
    var hasOverlayPermission by remember { mutableStateOf(AppLockerManager.hasOverlayPermission(context)) }
    var isAccessibilityEnabled by remember { mutableStateOf(AppLockerManager.isAccessibilityServiceEnabled(context)) }

    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<AppLockTarget>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    // Auto-refresh permission statuses whenever user returns from Settings or App Info
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = AppLockerManager.hasOverlayPermission(context)
                isAccessibilityEnabled = AppLockerManager.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                color = Color(0xFF0381FE),
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
                    footer = "When enabled, protected applications are locked immediately upon launch until verified with Face, Fingerprint, or PIN."
                ) {
                    AppleSettingToggleRow(
                        title = "Enable App Locker",
                        subtitle = if (isEnabled) "Active • Intercepting locked apps" else "Disabled • No apps locked",
                        icon = Lucide.ShieldCheck,
                        iconGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE)),
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundManager.playReaction()
                            isEnabled = checked
                            AppLockerManager.setEnabled(context, checked)
                        }
                    )
                }
            }

            // =====================================================================
            // 2. PERMISSION HEALTH DIAGNOSTICS
            // =====================================================================
            if (!isAccessibilityEnabled || !hasOverlayPermission) {
                item {
                    AppleSettingsGroup(
                        header = "Required System Permissions",
                        footer = "App Locker requires these system settings to detect launched apps instantly without latency or screen flicker."
                    ) {
                        if (!isAccessibilityEnabled) {
                            AppleSettingNavRow(
                                title = "Accessibility Service",
                                subtitle = "Required for instant zero-latency interception",
                                icon = Lucide.Zap,
                                iconGradient = listOf(Color(0xFFFF9500), Color(0xFFFF5E3A)),
                                valueText = "Grant",
                                onClick = {
                                    SoundManager.playReaction()
                                    AppLockerManager.openAccessibilitySettings(context)
                                }
                            )
                        }

                        if (!hasOverlayPermission) {
                            if (!isAccessibilityEnabled) {
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
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.60f),
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
                                color = Color.White.copy(alpha = 0.40f),
                                fontFamily = InterFontFamily
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
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
                                tint = Color.White.copy(alpha = 0.70f),
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
                        CircularProgressIndicator(
                            color = Color(0xFF0381FE),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
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
                            color = Color.White.copy(alpha = 0.50f),
                            fontFamily = InterFontFamily
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "INSTALLED APPLICATIONS (${filteredApps.size})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.50f),
                        letterSpacing = 0.8.sp,
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
}

@Composable
private fun AppLockRowItem(
    target: AppLockTarget,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF14171F).copy(alpha = 0.65f))
            .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable { onToggle(!target.isLocked) }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f)),
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
                    tint = Color.White.copy(alpha = 0.70f),
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
                color = Color.White,
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = target.packageName,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.45f),
                fontFamily = InterFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(12.dp))

        // Lock Switch
        Switch(
            checked = target.isLocked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0381FE),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
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
    onOpenAppInfo: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onCopyAdbCommand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1608))
            .border(1.dp, Color(0xFFFF9F0A).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        // Warning Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
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
                    contentDescription = "Restricted Setting Warning",
                    tint = Color(0xFFFF9F0A),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Android \"Restricted Setting\" Fix",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB340),
                    fontFamily = InterFontFamily
                )
                Text(
                    text = "Android 13, 14 & 15 sideload security check",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.60f),
                    fontFamily = InterFontFamily
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "If Android grays out the toggle and says \"Restricted setting: For your security, this setting is currently unavailable\", unlock it in 3 steps:",
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            color = Color.White.copy(alpha = 0.85f),
            fontFamily = InterFontFamily
        )

        Spacer(Modifier.height(12.dp))

        // Steps
        RestrictedStepItem(
            stepNumber = "1",
            boldText = "Trigger prompt: ",
            normalText = "Tap '2. Accessibility' below and tap ChromaStudio once so Android registers the restriction."
        )
        Spacer(Modifier.height(8.dp))
        RestrictedStepItem(
            stepNumber = "2",
            boldText = "Allow in App Info: ",
            normalText = "Tap '1. App Info' below, tap the three dots (⋮) in the top-right corner, and choose 'Allow restricted settings'."
        )
        Spacer(Modifier.height(8.dp))
        RestrictedStepItem(
            stepNumber = "3",
            boldText = "Turn ON: ",
            normalText = "Return here, tap '2. Accessibility' again, and turn the toggle ON."
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
                    containerColor = Color(0xFFFF9F0A),
                    contentColor = Color.Black
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
                    containerColor = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Lucide.Zap,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = Color(0xFFFF9F0A)
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
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { onCopyAdbCommand() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.Terminal,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.50f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Developer ADB bypass command",
                fontSize = 11.5.sp,
                color = Color.White.copy(alpha = 0.65f),
                fontFamily = InterFontFamily,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Lucide.Copy,
                contentDescription = "Copy",
                tint = Color(0xFFFF9F0A),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun RestrictedStepItem(
    stepNumber: String,
    boldText: String,
    normalText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF9F0A).copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9F0A),
                fontFamily = InterFontFamily
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = buildAnnotatedString {
                append(boldText)
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, color = Color.White),
                    0,
                    boldText.length
                )
                append(normalText)
            },
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = Color.White.copy(alpha = 0.75f),
            fontFamily = InterFontFamily,
            modifier = Modifier.weight(1f)
        )
    }
}
