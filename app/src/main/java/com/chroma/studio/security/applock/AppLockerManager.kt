package com.chroma.studio.security.applock

import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

enum class AppLockCategory(val label: String) {
    SOCIAL("Social"),
    FINANCE("Finance"),
    PRODUCTIVITY("Work & Mail"),
    MEDIA("Media & Files"),
    SYSTEM("System"),
    OTHER("Apps")
}

data class AppLockTarget(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isLocked: Boolean,
    val isSystem: Boolean,
    val category: AppLockCategory = if (isSystem) AppLockCategory.SYSTEM else AppLockCategory.OTHER
)

/** Returns grace window duration in milliseconds for timed re-lock policies. */
fun getRelockGraceDurationMs(mode: String): Long = when (mode) {
    AppLockerManager.RELOCK_30_SEC -> 30_000L
    AppLockerManager.RELOCK_1_MIN -> 60_000L
    AppLockerManager.RELOCK_5_MIN -> 300_000L
    else -> 0L
}

/** Human-readable short value label for re-lock policy. */
fun formatRelockModeLabel(mode: String): String = when (mode) {
    AppLockerManager.RELOCK_30_SEC -> "After 30s"
    AppLockerManager.RELOCK_1_MIN -> "After 1 min"
    AppLockerManager.RELOCK_5_MIN -> "After 5 min"
    AppLockerManager.RELOCK_SCREEN_OFF -> "Screen Off"
    else -> "On Exit"
}

/** Human-readable description for re-lock policy. */
fun formatRelockModeDescription(mode: String): String = when (mode) {
    AppLockerManager.RELOCK_30_SEC -> "Stays unlocked for 30 seconds after switching away"
    AppLockerManager.RELOCK_1_MIN -> "Stays unlocked for 1 minute after switching away"
    AppLockerManager.RELOCK_5_MIN -> "Stays unlocked for 5 minutes after switching away"
    AppLockerManager.RELOCK_SCREEN_OFF -> "Stays unlocked until your screen turns off"
    else -> "Locks again as soon as you leave an unlocked app"
}

/** Pure policy evaluation determining whether an unlocked session remains valid. */
fun shouldSessionStayUnlocked(
    unlockedAtMs: Long?,
    leftAtMs: Long?,
    relockMode: String,
    nowMs: Long
): Boolean {
    if (unlockedAtMs == null) return false
    return when (relockMode) {
        AppLockerManager.RELOCK_SCREEN_OFF -> true
        AppLockerManager.RELOCK_IMMEDIATELY -> leftAtMs == null
        AppLockerManager.RELOCK_30_SEC,
        AppLockerManager.RELOCK_1_MIN,
        AppLockerManager.RELOCK_5_MIN -> {
            if (leftAtMs == null) {
                true
            } else {
                val graceMs = getRelockGraceDurationMs(relockMode)
                val elapsed = nowMs - leftAtMs
                elapsed >= 0L && elapsed < graceMs
            }
        }
        else -> leftAtMs == null
    }
}

/** Pure anti-tamper package check for System Settings and Package Installer. */
fun isAntiTamperPackage(packageName: String, resolvedSettingsPackage: String? = null): Boolean {
    val pkg = packageName.lowercase()
    if (!resolvedSettingsPackage.isNullOrBlank() && pkg == resolvedSettingsPackage.lowercase()) {
        return true
    }
    return pkg == "com.android.settings" ||
        pkg == "com.google.android.packageinstaller" ||
        pkg == "com.android.packageinstaller"
}

/** Categorizes an installed app for clean grouping and quick filtering. */
fun categorizeAppTarget(packageName: String, appName: String, isSystem: Boolean): AppLockCategory {
    val pkg = packageName.lowercase()
    val name = appName.lowercase()
    val financeKeys = listOf("bank", "paytm", "gpay", "phonepe", "paypal", "wallet", "upi", "paisa", "finance", "cred")
    val socialKeys = listOf("whatsapp", "telegram", "signal", "messenger", "instagram", "facebook", "twitter", "snapchat", "tiktok", "discord", "threads", "reddit", "line", "viber")
    val productivityKeys = listOf("gmail", "mail", "outlook", "slack", "teams", "notes", "drive", "docs", "chrome", "browser")
    val mediaKeys = listOf("photos", "gallery", "files", "vault", "camera", "video", "youtube", "spotify")

    return when {
        financeKeys.any { pkg.contains(it) || name.contains(it) } -> AppLockCategory.FINANCE
        socialKeys.any { pkg.contains(it) || name.contains(it) } -> AppLockCategory.SOCIAL
        productivityKeys.any { pkg.contains(it) || name.contains(it) } -> AppLockCategory.PRODUCTIVITY
        mediaKeys.any { pkg.contains(it) || name.contains(it) } -> AppLockCategory.MEDIA
        isSystem -> AppLockCategory.SYSTEM
        else -> AppLockCategory.OTHER
    }
}

/** Returns true if an app matches recommended high-privacy protection heuristics. */
fun isRecommendedPrivacyPackage(packageName: String, appName: String): Boolean {
    val pkgLower = packageName.lowercase()
    val nameLower = appName.lowercase()
    val recommendedKeywords = listOf(
        "whatsapp", "telegram", "signal", "messenger", "instagram", "facebook", "twitter",
        "snapchat", "tiktok", "photos", "gallery", "chrome", "gmail", "bank", "paytm",
        "gpay", "phonepe", "paypal", "files", "vault", "upi", "discord", "cred"
    )
    return recommendedKeywords.any { pkgLower.contains(it) || nameLower.contains(it) }
}

/** Filters app targets by tab (ALL, LOCKED, DOWNLOADED, SYSTEM) and search query. */
fun filterAppLockTargets(
    apps: List<AppLockTarget>,
    filterTab: String,
    searchQuery: String
): List<AppLockTarget> {
    val tabFiltered = when (filterTab) {
        "LOCKED" -> apps.filter { it.isLocked }
        "DOWNLOADED" -> apps.filter { !it.isSystem }
        "SYSTEM" -> apps.filter { it.isSystem }
        else -> apps
    }
    if (searchQuery.isBlank()) return tabFiltered
    val q = searchQuery.trim().lowercase()
    return tabFiltered.filter {
        it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
    }
}

/**
 * Flagship Manager for Oscellion App Locker.
 * Handles persistence of locked packages, real-time in-memory session unlock state,
 * grace-window re-locking, anti-tamper protection, permission verification, and app querying.
 */
object AppLockerManager {
    private const val PREFS_NAME = "oscellion_app_locker_prefs"
    private const val KEY_ENABLED = "app_locker_master_enabled"
    private const val KEY_LOCKED_PACKAGES = "app_locker_locked_packages"
    private const val KEY_RELOCK_MODE = "app_locker_relock_mode"
    private const val KEY_PROTECT_SETTINGS = "app_locker_protect_settings"

    const val RELOCK_IMMEDIATELY = "IMMEDIATELY"
    const val RELOCK_30_SEC = "30_SEC"
    const val RELOCK_1_MIN = "1_MIN"
    const val RELOCK_5_MIN = "5_MIN"
    const val RELOCK_SCREEN_OFF = "SCREEN_OFF"

    val RELOCK_MODES = listOf(
        RELOCK_IMMEDIATELY,
        RELOCK_30_SEC,
        RELOCK_1_MIN,
        RELOCK_5_MIN,
        RELOCK_SCREEN_OFF
    )

    // Persistent background scope for the usage watcher (survives navigation away from AppLockerScreen)
    private val lockerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var screenOffReceiverRegistered = false

    @Volatile
    private var settingsBypassUntilMs: Long = 0L

    // Session cache of currently unlocked packages -> unlock timestamp
    private val unlockedSessions = ConcurrentHashMap<String, Long>()

    // Tracks when user navigated away from an unlocked package -> leave timestamp
    private val leftAppTimestamps = ConcurrentHashMap<String, Long>()

    @Volatile
    private var cachedRelockMode: String = RELOCK_IMMEDIATELY

    // Current foreground package tracking
    var currentForegroundPackage: String? = null
        private set

    // Active shield package currently being locked/displayed on screen
    @Volatile
    var shieldPackage: String? = null

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
            leftAppTimestamps.clear()
        }
    }

    /** Returns whether System Settings & Package Installer anti-tamper lock is enabled */
    fun isProtectSettingsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PROTECT_SETTINGS, false)
    }

    /** Enables or disables anti-tamper protection on System Settings & Package Installer */
    fun setProtectSettingsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PROTECT_SETTINGS, enabled).apply()
    }

    /** Temporarily exempts System Settings when launched from inside App Locker permissions setup */
    fun grantSettingsBypassWindow(durationMs: Long = 15_000L) {
        settingsBypassUntilMs = System.currentTimeMillis() + durationMs
    }

    fun isSettingsBypassActive(nowMs: Long = System.currentTimeMillis()): Boolean {
        return nowMs < settingsBypassUntilMs
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
        if (getLockedPackages(context).contains(packageName)) return true
        if (isProtectSettingsEnabled(context) && !isSettingsBypassActive()) {
            val resolvedSettingsPkg = resolveSettingsPackageName(context)
            if (isAntiTamperPackage(packageName, resolvedSettingsPkg)) {
                return true
            }
        }
        return false
    }

    private fun resolveSettingsPackageName(context: Context): String? {
        return try {
            context.packageManager
                .resolveActivity(Intent(Settings.ACTION_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        } catch (_: Exception) {
            null
        }
    }

    /** Toggles lock state for a package */
    fun setPackageLocked(context: Context, packageName: String, locked: Boolean) {
        val current = getLockedPackages(context).toMutableSet()
        if (locked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
            unlockedSessions.remove(packageName)
            leftAppTimestamps.remove(packageName)
        }
        getPrefs(context).edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedCountFlow.value = current.size
    }

    /** Bulk lock/unlock helper for filtered selections. Returns net count updated. */
    fun setPackagesLocked(context: Context, packageNames: Collection<String>, locked: Boolean): Int {
        val current = getLockedPackages(context).toMutableSet()
        var changed = 0
        for (pkg in packageNames) {
            if (pkg == context.packageName) continue
            if (locked) {
                if (current.add(pkg)) changed++
            } else {
                if (current.remove(pkg)) changed++
                unlockedSessions.remove(pkg)
                leftAppTimestamps.remove(pkg)
            }
        }
        getPrefs(context).edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedCountFlow.value = current.size
        return changed
    }

    /** Removes all apps from the lock list */
    fun clearAllLockedPackages(context: Context) {
        getPrefs(context).edit().putStringSet(KEY_LOCKED_PACKAGES, emptySet()).apply()
        unlockedSessions.clear()
        leftAppTimestamps.clear()
        _lockedCountFlow.value = 0
    }

    /** Returns re-lock policy (IMMEDIATELY, 30_SEC, 1_MIN, 5_MIN, or SCREEN_OFF) */
    fun getRelockMode(context: Context): String {
        val mode = getPrefs(context).getString(KEY_RELOCK_MODE, RELOCK_IMMEDIATELY) ?: RELOCK_IMMEDIATELY
        cachedRelockMode = mode
        return mode
    }

    /** Sets re-lock policy */
    fun setRelockMode(context: Context, mode: String) {
        cachedRelockMode = mode
        getPrefs(context).edit().putString(KEY_RELOCK_MODE, mode).apply()
        if (mode == RELOCK_IMMEDIATELY) {
            leftAppTimestamps.clear()
        }
    }

    /** Returns count of locked packages */
    fun getLockedCount(context: Context): Int {
        return getLockedPackages(context).size
    }

    // =========================================================================
    // SESSION LIFECYCLE MANAGEMENT
    // =========================================================================

    /** Returns true if user has already unlocked this package and session hasn't expired */
    fun isPackageSessionUnlocked(packageName: String): Boolean {
        return isPackageSessionUnlockedInternal(packageName, cachedRelockMode, System.currentTimeMillis())
    }

    /** Context-aware check that ensures persisted re-lock policy is respected even after service restart */
    fun isPackageSessionUnlocked(context: Context, packageName: String): Boolean {
        return isPackageSessionUnlockedInternal(packageName, getRelockMode(context), System.currentTimeMillis())
    }

    private fun isPackageSessionUnlockedInternal(
        packageName: String,
        relockMode: String,
        nowMs: Long
    ): Boolean {
        val unlockedAt = unlockedSessions[packageName] ?: return false
        val leftAt = leftAppTimestamps[packageName]
        val valid = shouldSessionStayUnlocked(
            unlockedAtMs = unlockedAt,
            leftAtMs = leftAt,
            relockMode = relockMode,
            nowMs = nowMs
        )
        if (!valid) {
            unlockedSessions.remove(packageName)
            leftAppTimestamps.remove(packageName)
        }
        return valid
    }

    /** Marks a package as unlocked following PIN or Biometric verification */
    fun markPackageUnlocked(packageName: String) {
        unlockedSessions[packageName] = System.currentTimeMillis()
        leftAppTimestamps.remove(packageName)
    }

    /** Revokes unlock state for a specific package */
    fun lockPackage(packageName: String) {
        unlockedSessions.remove(packageName)
        leftAppTimestamps.remove(packageName)
    }

    /** Revokes all unlock sessions (e.g., when device screen turns off) */
    fun lockAllSessions() {
        unlockedSessions.clear()
        leftAppTimestamps.clear()
    }

    /** Called when the foreground package changes (backward-compatible boolean overload) */
    fun onForegroundPackageChanged(newPackage: String, relockImmediately: Boolean = true) {
        val mode = if (relockImmediately) RELOCK_IMMEDIATELY else RELOCK_SCREEN_OFF
        onForegroundPackageChangedWithMode(newPackage, mode)
    }

    /** Called when the foreground package changes using persisted re-lock mode */
    fun onForegroundPackageChanged(context: Context, newPackage: String) {
        onForegroundPackageChangedWithMode(newPackage, getRelockMode(context))
    }

    private fun onForegroundPackageChangedWithMode(newPackage: String, relockMode: String) {
        val now = System.currentTimeMillis()
        val old = currentForegroundPackage
        currentForegroundPackage = newPackage

        if (unlockedSessions.containsKey(newPackage)) {
            val leftAt = leftAppTimestamps.remove(newPackage)
            if (!shouldSessionStayUnlocked(unlockedSessions[newPackage], leftAt, relockMode, now)) {
                unlockedSessions.remove(newPackage)
            }
        }

        if (old != null && old != newPackage && unlockedSessions.containsKey(old)) {
            when (relockMode) {
                RELOCK_IMMEDIATELY -> {
                    unlockedSessions.remove(old)
                    leftAppTimestamps.remove(old)
                }
                RELOCK_30_SEC, RELOCK_1_MIN, RELOCK_5_MIN -> {
                    leftAppTimestamps[old] = now
                }
                RELOCK_SCREEN_OFF -> Unit
            }
        }
    }

    private fun ensureScreenOffReceiver(context: Context) {
        if (screenOffReceiverRegistered) return
        synchronized(this) {
            if (screenOffReceiverRegistered) return
            try {
                val appCtx = context.applicationContext
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                            lockAllSessions()
                        }
                    }
                }
                val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    appCtx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    appCtx.registerReceiver(receiver, filter)
                }
                screenOffReceiverRegistered = true
            } catch (_: Exception) {}
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
        grantSettingsBypassWindow()
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

    /** Returns true only if this build actually declares an AccessibilityService in its manifest */
    fun hasAccessibilityServiceDeclared(context: Context): Boolean {
        return try {
            val intent = Intent("android.accessibilityservice.AccessibilityService").setPackage(context.packageName)
            context.packageManager.queryIntentServices(intent, 0).isNotEmpty()
        } catch (_: Exception) {
            false
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
        grantSettingsBypassWindow()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /** Opens System App Info page (where user can tap 3-dots -> "Allow restricted settings") */
    fun openAppInfo(context: Context) {
        grantSettingsBypassWindow()
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
        grantSettingsBypassWindow()
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private var usageWatcherJob: kotlinx.coroutines.Job? = null

    /** Starts foreground app monitoring using UsageStatsManager */
    fun startUsageWatcherIfNeeded(
        context: Context,
        @Suppress("UNUSED_PARAMETER") scope: kotlinx.coroutines.CoroutineScope? = null
    ) {
        val appCtx = context.applicationContext
        ensureScreenOffReceiver(appCtx)
        if (!isEnabled(appCtx)) {
            stopUsageWatcher()
            return
        }
        if (isAccessibilityServiceEnabled(appCtx)) {
            // Accessibility service is active; zero latency interception running
            stopUsageWatcher()
            return
        }
        if (!hasUsageStatsPermission(appCtx)) {
            stopUsageWatcher()
            return
        }

        if (usageWatcherJob?.isActive == true) return

        usageWatcherJob = lockerScope.launch {
            val usm = appCtx.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager ?: return@launch
            var lastPkg: String? = null
            var lastLaunchTime = 0L

            while (isActive && isEnabled(appCtx) && !isAccessibilityServiceEnabled(appCtx)) {
                try {
                    val now = System.currentTimeMillis()
                    val events = usm.queryEvents(now - 3500, now)
                    val event = android.app.usage.UsageEvents.Event()
                    var foregroundPkg: String? = null

                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        @Suppress("DEPRECATION")
                        val isForegroundEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
                        } else {
                            event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
                        }
                        if (isForegroundEvent) {
                            foregroundPkg = event.packageName
                        }
                    }

                    if (foregroundPkg != null && foregroundPkg != appCtx.packageName) {
                        onForegroundPackageChanged(appCtx, foregroundPkg)

                        val isSystemChrome = foregroundPkg == "com.android.systemui" ||
                            foregroundPkg.contains("launcher", ignoreCase = true) ||
                            foregroundPkg.contains("home", ignoreCase = true)

                        if (isSystemChrome) {
                            shieldPackage = null
                        } else if (isPackageLocked(appCtx, foregroundPkg) &&
                            !isPackageSessionUnlocked(appCtx, foregroundPkg) &&
                            shieldPackage != foregroundPkg
                        ) {
                            if (foregroundPkg != lastPkg || now - lastLaunchTime > 650L) {
                                lastPkg = foregroundPkg
                                lastLaunchTime = now
                                withContext(Dispatchers.Main) {
                                    AppLockActivity.launch(appCtx, foregroundPkg)
                                }
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

        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val homePkg = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName

        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
        val targets = mutableListOf<AppLockTarget>()
        val seenPackages = mutableSetOf<String>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || pkg == homePkg || seenPackages.contains(pkg)) continue
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
                        isSystem = isSystem,
                        category = categorizeAppTarget(pkg, label, isSystem)
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
        val installed = getInstalledApps(context)
        val toLock = installed.filter { target ->
            isRecommendedPrivacyPackage(target.packageName, target.appName)
        }.map { it.packageName }

        val current = getLockedPackages(context).toMutableSet()
        current.addAll(toLock)
        getPrefs(context).edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
        _lockedCountFlow.value = current.size
        return toLock.size
    }
}
