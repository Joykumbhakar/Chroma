package com.chroma.studio.security.applock

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.annotation.WorkerThread
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class AppLockTarget(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isLocked: Boolean,
    val isSystem: Boolean
)

/**
 * Flagship Manager for Oscellion App Locker.
 * Handles persistence of locked packages, real-time in-memory session unlock state,
 * system permission verification (Accessibility & Window Overlay), and application querying.
 */
object AppLockerManager {
    private const val PREFS_NAME = "oscellion_app_locker_prefs"
    private const val KEY_ENABLED = "app_locker_master_enabled"
    private const val KEY_LOCKED_PACKAGES = "app_locker_locked_packages"
    private const val KEY_RELOCK_MODE = "app_locker_relock_mode" // IMMEDIATELY or SCREEN_OFF

    // Session cache of currently unlocked packages (cleared on screen off or app switch)
    private val unlockedSessions = ConcurrentHashMap<String, Long>()

    // Current foreground package tracking
    var currentForegroundPackage: String? = null
        private set

    private val _lockedCountFlow = MutableStateFlow(0)
    val lockedCountFlow: StateFlow<Int> = _lockedCountFlow.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Returns whether App Locker master switch is enabled */
    fun isEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLED, false)
    }

    /** Enables or disables App Locker master switch */
    fun setEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) {
            unlockedSessions.clear()
        }
    }

    /** Returns the set of all locked package names */
    fun getLockedPackages(context: Context): Set<String> {
        val set = getPrefs(context).getStringSet(KEY_LOCKED_PACKAGES, emptySet()) ?: emptySet()
        _lockedCountFlow.value = set.size
        return set
    }

    /** Returns true if the specific package is configured to be locked */
    fun isPackageLocked(context: Context, packageName: String): Boolean {
        if (!isEnabled(context)) return false
        // Never lock our own app through App Locker (our app uses internal OscellionLockScreen)
        if (packageName == context.packageName) return false
        return getLockedPackages(context).contains(packageName)
    }

    /** Toggles lock state for a package */
    fun setPackageLocked(context: Context, packageName: String, locked: Boolean) {
        val current = getLockedPackages(context).toMutableSet()
        if (locked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
            unlockedSessions.remove(packageName)
        }
        getPrefs(context).edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedCountFlow.value = current.size
    }

    /** Returns count of locked packages */
    fun getLockedCount(context: Context): Int {
        return getLockedPackages(context).size
    }

    // =========================================================================
    // SESSION LIFECYCLE MANAGEMENT
    // =========================================================================

    /** Returns true if user has already unlocked this package in current session */
    fun isPackageSessionUnlocked(packageName: String): Boolean {
        return unlockedSessions.containsKey(packageName)
    }

    /** Marks a package as unlocked following PIN or Biometric verification */
    fun markPackageUnlocked(packageName: String) {
        unlockedSessions[packageName] = System.currentTimeMillis()
    }

    /** Revokes unlock state for a specific package */
    fun lockPackage(packageName: String) {
        unlockedSessions.remove(packageName)
    }

    /** Revokes all unlock sessions (e.g., when device screen turns off) */
    fun lockAllSessions() {
        unlockedSessions.clear()
    }

    /** Called when the foreground package changes */
    fun onForegroundPackageChanged(newPackage: String) {
        val old = currentForegroundPackage
        currentForegroundPackage = newPackage
        if (old != null && old != newPackage) {
            // User navigated away from the previous app -> revoke its session if desired
            // Instant relock ensures when they return, they must unlock again
            unlockedSessions.remove(old)
        }
    }

    // =========================================================================
    // PERMISSION CHECKS & SYSTEM SETTINGS INTENTS
    // =========================================================================

    /** Checks whether Display Over Other Apps permission is granted */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /** Opens System Settings for Display Over Other Apps */
    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }

    /** Checks whether OscellionAppLockService Accessibility service is enabled */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
            val runningServices = am?.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            if (runningServices?.any { it.resolveInfo.serviceInfo.packageName == context.packageName } == true) {
                return true
            }
        } catch (_: Exception) {}

        val expectedService = "${context.packageName}/${OscellionAppLockService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(':').any {
            it.equals(expectedService, ignoreCase = true) ||
            (it.contains(context.packageName, ignoreCase = true) && it.contains("OscellionAppLockService", ignoreCase = true))
        }
    }

    /** Opens System Accessibility Settings page */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /** Opens System App Info page (where user can tap 3-dots -> "Allow restricted settings") */
    fun openAppInfo(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_APPLICATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }

    /** Returns true if device is Android 13+ (API 33+) where sideloaded APKs have restricted settings */
    fun isRestrictedSettingsApplicable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /** Checks whether Usage Stats permission is granted */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Opens System Usage Access Settings page */
    fun openUsageStatsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private var usageWatcherJob: kotlinx.coroutines.Job? = null

    /** Starts foreground app monitoring using UsageStatsManager if Accessibility is restricted */
    fun startUsageWatcherIfNeeded(context: Context, scope: kotlinx.coroutines.CoroutineScope) {
        if (!isEnabled(context)) {
            stopUsageWatcher()
            return
        }
        if (isAccessibilityServiceEnabled(context)) {
            // Accessibility service is active; zero latency interception running
            stopUsageWatcher()
            return
        }
        if (!hasUsageStatsPermission(context)) {
            stopUsageWatcher()
            return
        }

        if (usageWatcherJob?.isActive == true) return

        usageWatcherJob = scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager ?: return@launch
            var lastPkg: String? = null
            var lastLaunchTime = 0L

            while (isActive && isEnabled(context) && !isAccessibilityServiceEnabled(context)) {
                try {
                    val now = System.currentTimeMillis()
                    val events = usm.queryEvents(now - 3500, now)
                    val event = android.app.usage.UsageEvents.Event()
                    var foregroundPkg: String? = null

                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED ||
                            event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                            foregroundPkg = event.packageName
                        }
                    }

                    if (foregroundPkg != null &&
                        foregroundPkg != context.packageName &&
                        foregroundPkg != "com.android.systemui" &&
                        !foregroundPkg.contains("launcher") &&
                        !foregroundPkg.contains("home")
                    ) {
                        currentForegroundPackage = foregroundPkg
                        if (isPackageLocked(context, foregroundPkg) && !isPackageSessionUnlocked(foregroundPkg)) {
                            if (foregroundPkg != lastPkg || now - lastLaunchTime > 500L) {
                                lastPkg = foregroundPkg
                                lastLaunchTime = now
                                AppLockActivity.launch(context, foregroundPkg)
                            }
                        }
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(250L)
            }
        }
    }

    fun stopUsageWatcher() {
        usageWatcherJob?.cancel()
        usageWatcherJob = null
    }

    // =========================================================================
    // INSTALLED APPLICATIONS QUERYING
    // =========================================================================

    /**
     * Queries all launchable user apps installed on the device, sorted alphabetically
     * with locked apps prioritized.
     */
    @WorkerThread
    fun getInstalledApps(context: Context): List<AppLockTarget> {
        val pm = context.packageManager
        val lockedSet = getLockedPackages(context)
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
        val targets = mutableListOf<AppLockTarget>()
        val seenPackages = mutableSetOf<String>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                targets.add(
                    AppLockTarget(
                        packageName = pkg,
                        appName = label,
                        icon = icon,
                        isLocked = lockedSet.contains(pkg),
                        isSystem = isSystem
                    )
                )
            } catch (_: Exception) {}
        }

        // Sort: locked first, then alphabetically
        return targets.sortedWith(
            compareByDescending<AppLockTarget> { it.isLocked }
                .thenBy { it.appName.lowercase() }
        )
    }

    /**
     * Locks recommended high-privacy apps (Social, Messaging, Photos, Files, Banking)
     * if found on device.
     */
    fun lockRecommended(context: Context): Int {
        val recommendedKeywords = listOf(
            "whatsapp", "telegram", "signal", "messenger", "instagram", "facebook", "twitter",
            "snapchat", "tiktok", "photos", "gallery", "chrome", "gmail", "bank", "paytm",
            "gpay", "phonepe", "paypal", "files", "vault"
        )
        val installed = getInstalledApps(context)
        val toLock = installed.filter { target ->
            val pkgLower = target.packageName.lowercase()
            val nameLower = target.appName.lowercase()
            recommendedKeywords.any { pkgLower.contains(it) || nameLower.contains(it) }
        }.map { it.packageName }

        val current = getLockedPackages(context).toMutableSet()
        current.addAll(toLock)
        getPrefs(context).edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedCountFlow.value = current.size
        return toLock.size
    }
}
