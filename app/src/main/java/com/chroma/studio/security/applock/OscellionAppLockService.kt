package com.chroma.studio.security.applock

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent

/**
 * Flagship Accessibility Engine for Oscellion App Locker.
 * Provides true zero-latency (<5ms) window state monitoring to intercept
 * target applications before their UI or splash screen can leak sensitive data.
 */
class OscellionAppLockService : AccessibilityService() {

    private var lastInterceptionTime = 0L
    private var lastInterceptedPackage: String? = null

    companion object {
        @Volatile
        var instance: OscellionAppLockService? = null
            private set

        fun performHome(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        }

        fun resetInterception() {
            instance?.lastInterceptedPackage = null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    // Broadcast receiver to instantly revoke all unlocked sessions when device locks
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                AppLockerManager.lockAllSessions()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            return
        }

        val targetPackage = event.packageName?.toString() ?: return

        // If user returned to home or launcher, reset interception cache so re-launch is instant
        if (targetPackage.contains("launcher", ignoreCase = true) ||
            targetPackage.contains("home", ignoreCase = true) ||
            targetPackage == "com.android.systemui") {
            lastInterceptedPackage = null
            AppLockerManager.onForegroundPackageChanged(targetPackage)
            return
        }

        // Ignore our own package and system UI components
        if (targetPackage == packageName ||
            targetPackage.contains("inputmethod") ||
            targetPackage.contains("keyboard")) {
            return
        }

        // Notify manager of foreground transition
        AppLockerManager.onForegroundPackageChanged(targetPackage)

        // Check if package is locked
        if (!AppLockerManager.isPackageLocked(this, targetPackage)) {
            lastInterceptedPackage = null
            return
        }

        // Check if currently unlocked in active session
        if (AppLockerManager.isPackageSessionUnlocked(targetPackage)) {
            return
        }

        // Debounce launches within 350ms for identical package
        val now = System.currentTimeMillis()
        if (targetPackage == lastInterceptedPackage && now - lastInterceptionTime < 350L) {
            return
        }
        lastInterceptionTime = now
        lastInterceptedPackage = targetPackage

        // Launch instant Shield Activity
        AppLockActivity.launch(this, targetPackage)
    }

    override fun onInterrupt() {
        // Required callback for AccessibilityService lifecycle
    }
}
